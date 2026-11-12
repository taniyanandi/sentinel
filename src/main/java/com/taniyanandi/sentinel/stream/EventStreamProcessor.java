package com.taniyanandi.sentinel.stream;

import com.taniyanandi.sentinel.domain.AnomalyAlert;
import com.taniyanandi.sentinel.domain.DeadLetterEvent;
import com.taniyanandi.sentinel.domain.DetectionState;
import com.taniyanandi.sentinel.domain.TelemetryEvent;
import com.taniyanandi.sentinel.metrics.SentinelMetrics;
import com.taniyanandi.sentinel.service.AnomalyDetector;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.kstream.Transformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@Configuration
public class EventStreamProcessor {
    private static final Logger log = LoggerFactory.getLogger(EventStreamProcessor.class);

    private final AnomalyDetector detector;
    private final SentinelMetrics metrics;
    private final String eventsTopic;
    private final String alertsTopic;
    private final String dlqTopic;

    public EventStreamProcessor(AnomalyDetector detector,
                                SentinelMetrics metrics,
                                @Value("${sentinel.kafka.events-topic:sentinel.events}") String eventsTopic,
                                @Value("${sentinel.kafka.alerts-topic:sentinel.alerts}") String alertsTopic,
                                @Value("${sentinel.kafka.dlq-topic:sentinel.events.dlq}") String dlqTopic) {
        this.detector = detector;
        this.metrics = metrics;
        this.eventsTopic = eventsTopic;
        this.alertsTopic = alertsTopic;
        this.dlqTopic = dlqTopic;
    }

    @Bean
    KStream<String, TelemetryEvent> topology(StreamsBuilder builder) {
        JsonSerde<TelemetryEvent> eventSerde = new JsonSerde<>(TelemetryEvent.class);
        JsonSerde<AnomalyAlert> alertSerde = new JsonSerde<>(AnomalyAlert.class);
        JsonSerde<DeadLetterEvent> deadLetterSerde = new JsonSerde<>(DeadLetterEvent.class);
        JsonSerde<DetectionState> stateSerde = new JsonSerde<>(DetectionState.class);

        StoreBuilder<KeyValueStore<String, DetectionState>> stateStore = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("baseline-store"), Serdes.String(), stateSerde);
        builder.addStateStore(stateStore);

        KStream<String, TelemetryEvent> events = builder.stream(eventsTopic,
                Consumed.with(Serdes.String(), eventSerde));

        KStream<String, DetectionOutcome> outcomes = events
                .selectKey((key, event) -> event.entityId() + "|" + event.metric())
                .transform(() -> new Transformer<String, TelemetryEvent, KeyValue<String, DetectionOutcome>>() {
                    private KeyValueStore<String, DetectionState> store;

                    @SuppressWarnings("unchecked")
                    @Override
                    public void init(ProcessorContext context) {
                        store = (KeyValueStore<String, DetectionState>) context.getStateStore("baseline-store");
                    }

                    @Override
                    public KeyValue<String, DetectionOutcome> transform(String key, TelemetryEvent event) {
                        DetectionState state = store.get(key);
                        if (state == null) state = new DetectionState();
                        try {
                            AnomalyAlert alert = detector.evaluate(event, state);
                            store.put(key, state);
                            return KeyValue.pair(key, DetectionOutcome.alert(alert));
                        } catch (Exception exception) {
                            log.warn("Dead-lettering event {} for {}|{} after detection failure",
                                    event.eventId(), event.entityId(), event.metric(), exception);
                            metrics.eventDeadLettered();
                            DeadLetterEvent deadLetterEvent = new DeadLetterEvent(
                                    event,
                                    exception.getClass().getName(),
                                    exception.getMessage(),
                                    Instant.now());
                            return KeyValue.pair(key, DetectionOutcome.deadLetter(deadLetterEvent));
                        }
                    }

                    @Override
                    public void close() {}
                }, Named.as("anomaly-detector"), "baseline-store");

        @SuppressWarnings("unchecked")
        KStream<String, DetectionOutcome>[] branches = outcomes.branch(
                (key, outcome) -> outcome.alert() != null,
                (key, outcome) -> outcome.deadLetter() != null);
        KStream<String, AnomalyAlert> alerts = branches[0].mapValues(DetectionOutcome::alert);
        alerts.to(alertsTopic, Produced.with(Serdes.String(), alertSerde));
        branches[1].mapValues(DetectionOutcome::deadLetter)
                .to(dlqTopic, Produced.with(Serdes.String(), deadLetterSerde));
        return events;
    }

    private record DetectionOutcome(AnomalyAlert alert, DeadLetterEvent deadLetter) {
        private static DetectionOutcome alert(AnomalyAlert alert) {
            return new DetectionOutcome(alert, null);
        }

        private static DetectionOutcome deadLetter(DeadLetterEvent deadLetter) {
            return new DetectionOutcome(null, deadLetter);
        }
    }
}
