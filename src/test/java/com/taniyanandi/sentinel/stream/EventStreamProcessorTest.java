package com.taniyanandi.sentinel.stream;

import com.taniyanandi.sentinel.domain.DeadLetterEvent;
import com.taniyanandi.sentinel.domain.TelemetryEvent;
import com.taniyanandi.sentinel.metrics.SentinelMetrics;
import com.taniyanandi.sentinel.service.AnomalyDetector;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class EventStreamProcessorTest {
    private static final String EVENTS_TOPIC = "events";
    private static final String ALERTS_TOPIC = "alerts";
    private static final String DLQ_TOPIC = "events.dlq";

    @Test
    void routesDetectionFailuresToDeadLetterTopic() {
        AnomalyDetector detector = Mockito.mock(AnomalyDetector.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SentinelMetrics metrics = new SentinelMetrics(registry);
        TelemetryEvent event = event("failed-event", 42.0);
        when(detector.evaluate(Mockito.any(), Mockito.any()))
                .thenThrow(new IllegalArgumentException("value cannot be processed"));

        try (TopologyTestDriver driver = newDriver(detector, metrics)) {
            TestInputTopic<String, TelemetryEvent> input = driver.createInputTopic(
                    EVENTS_TOPIC, Serdes.String().serializer(), eventSerde().serializer());
            TestOutputTopic<String, DeadLetterEvent> dlq = driver.createOutputTopic(
                    DLQ_TOPIC, Serdes.String().deserializer(), deadLetterSerde().deserializer());

            input.pipeInput(event.eventId(), event);

            DeadLetterEvent deadLetter = dlq.readValue();
            assertThat(deadLetter.event()).isEqualTo(event);
            assertThat(deadLetter.errorType()).isEqualTo(IllegalArgumentException.class.getName());
            assertThat(deadLetter.errorMessage()).isEqualTo("value cannot be processed");
            assertThat(deadLetter.failedAt()).isNotNull();
            assertThat(registry.get("sentinel.events.dlq").counter().count()).isEqualTo(1.0);
        }
    }

    @Test
    void doesNotDeadLetterSuccessfulDetection() {
        AnomalyDetector detector = Mockito.mock(AnomalyDetector.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SentinelMetrics metrics = new SentinelMetrics(registry);
        when(detector.evaluate(Mockito.any(), Mockito.any())).thenReturn(null);

        try (TopologyTestDriver driver = newDriver(detector, metrics)) {
            TestInputTopic<String, TelemetryEvent> input = driver.createInputTopic(
                    EVENTS_TOPIC, Serdes.String().serializer(), eventSerde().serializer());
            TestOutputTopic<String, DeadLetterEvent> dlq = driver.createOutputTopic(
                    DLQ_TOPIC, Serdes.String().deserializer(), deadLetterSerde().deserializer());

            input.pipeInput("normal-event", event("normal-event", 10.0));

            assertThat(dlq.isEmpty()).isTrue();
            assertThat(registry.get("sentinel.events.dlq").counter().count()).isZero();
        }
    }

    private TopologyTestDriver newDriver(AnomalyDetector detector, SentinelMetrics metrics) {
        StreamsBuilder builder = new StreamsBuilder();
        new EventStreamProcessor(detector, metrics, EVENTS_TOPIC, ALERTS_TOPIC, DLQ_TOPIC).topology(builder);
        Properties properties = new Properties();
        properties.put("application.id", "event-stream-processor-test");
        properties.put("bootstrap.servers", "dummy:9092");
        properties.put("default.key.serde", Serdes.String().getClass());
        properties.put("default.value.serde", Serdes.ByteArray().getClass());
        return new TopologyTestDriver(builder.build(), properties);
    }

    private JsonSerde<TelemetryEvent> eventSerde() {
        return new JsonSerde<>(TelemetryEvent.class);
    }

    private JsonSerde<DeadLetterEvent> deadLetterSerde() {
        return new JsonSerde<>(DeadLetterEvent.class);
    }

    private TelemetryEvent event(String eventId, double value) {
        return new TelemetryEvent(eventId, "test", "entity-1", "latency_ms", value,
                Instant.parse("2026-11-12T00:00:00Z"), Map.of());
    }

}
