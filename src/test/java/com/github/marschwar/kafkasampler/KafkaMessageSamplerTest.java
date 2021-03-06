package com.github.marschwar.kafkasampler;

import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class KafkaMessageSamplerTest {
    private static final RecordMetadata ANY_METADATA = new RecordMetadata(new TopicPartition("topic", 1), 0L, 0L, 0L, 0L, 0, 0);

    @InjectMocks
    KafkaMessageSampler subject;

    @Mock
    private KafkaProducerBuilder producerBuilder;


    void setupProducerBuilderMock() {
        when(producerBuilder.bootstrapServers(any())).thenReturn(producerBuilder);
        when(producerBuilder.securityProtocol(any())).thenReturn(producerBuilder);
        when(producerBuilder.ssl(anyBoolean())).thenReturn(producerBuilder);
        when(producerBuilder.keystoreLocation(any())).thenReturn(producerBuilder);
        when(producerBuilder.keystorePassword(any())).thenReturn(producerBuilder);
        when(producerBuilder.truststoreLocation(any())).thenReturn(producerBuilder);
        when(producerBuilder.truststorePassword(any())).thenReturn(producerBuilder);
        when(producerBuilder.sslIEndpointIdentification(any())).thenReturn(producerBuilder);
        when(producerBuilder.saslJaasConfig(any())).thenReturn(producerBuilder);
        when(producerBuilder.saslMechanism(any())).thenReturn(producerBuilder);

    }

    @Test
    void testFailWithoutTopic() {
        assertThrows(IllegalArgumentException.class, () -> {
            subject.setProperty(KafkaClientConfig.BOOTSTRAP_SERVERS, "localhost:9092");
            subject.setTopic(" ");
            subject.setKey("foo");
            subject.setPayload("bar");

            subject.sample(new Entry());
        });
        verifyZeroInteractions(producerBuilder);
    }

    @Test
    void testHandleFailureDuringSend() {
        setupProducerBuilderMock();

        KafkaProducer producer = mock(KafkaProducer.class);
        when(producer.send(any(), any())).thenAnswer(ctx -> {
            final Callback callback = ctx.getArgument(1);
            final RecordMetadata meta = new RecordMetadata(new TopicPartition("topic", 1), 0L, 0L, 0L, 0L, 0, 0);
            callback.onCompletion(meta, new KafkaException("some error"));
            return CompletableFuture.completedFuture(null);
        });
        when(producerBuilder.build()).thenReturn(producer);

        subject.setProperty(KafkaClientConfig.BOOTSTRAP_SERVERS, "localhost:9092");
        subject.setTopic("any");
        subject.setKey("foo");
        subject.setPayload("bar");

        SampleResult result = subject.sample(new Entry());

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getResponseMessage()).isEqualTo("some error");

        verify(producer).close();
    }

    @Test
    void ensureProducerIsClosedOnSuccess() {
        setupProducerBuilderMock();

        KafkaProducer producer = mock(KafkaProducer.class);
        when(producer.send(any(), any())).thenAnswer(ctx -> {
            final Callback callback = ctx.getArgument(1);
            callback.onCompletion(ANY_METADATA, null);
            return CompletableFuture.completedFuture(null);
        });
        when(producerBuilder.build()).thenReturn(producer);

        subject.setProperty(KafkaClientConfig.BOOTSTRAP_SERVERS, "localhost:9092");
        subject.setTopic("any");
        subject.setKey("foo");
        subject.setPayload("bar");

        SampleResult result = subject.sample(new Entry());

        assertThat(result.isSuccessful()).isTrue();

        verify(producer).close();
    }

    @Test
    void whenNoHeadersAreAdded() {
        subject.setHeaders(null);
        assertThat(subject.getHeaders()).isEmpty();
    }

    @Test
    void whenASingleHeaderIsAdded() {
        Header aHeader = new Header("key", "value");
        subject.setHeaders(Collections.singletonList(aHeader));

        assertThat(subject.getHeaders()).containsExactly(aHeader);
    }

    @Test
    void whenMultipleHeadersAreAdded() {
        Header aHeader = new Header("key", "value");
        Header anotherHeader = new Header("key2", "value2");
        subject.setHeaders(Arrays.asList(aHeader, anotherHeader));

        assertThat(subject.getHeaders()).containsExactly(aHeader, anotherHeader);
    }
}
