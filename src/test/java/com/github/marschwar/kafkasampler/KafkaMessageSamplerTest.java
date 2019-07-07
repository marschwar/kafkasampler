package com.github.marschwar.kafkasampler;

import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class KafkaMessageSamplerTest {

    @InjectMocks
    KafkaMessageSampler subject;

    @Mock
    private Producer<String, String> producer;

    @Test
    void testFailWithoutTopic() {
        assertThrows(IllegalArgumentException.class, () -> {
            subject.setProperty(KafkaClientConfig.BOOTSTRAP_SERVERS, "localhost:9092");
            subject.setTopic(" ");
            subject.setKey("foo");
            subject.setPayload("bar");

            subject.sample(new Entry());
        });
    }

    @Test
    void testHandleFailureDuringSend() {

        when(producer.send(any(), any())).thenAnswer(ctx -> {
            final Callback callback = ctx.getArgument(1);
            final RecordMetadata meta = new RecordMetadata(new TopicPartition("topic", 1), 0L, 0L, 0L, 0L, 0, 0);
            callback.onCompletion(meta, new KafkaException("some error"));
            return CompletableFuture.completedFuture(null);
        });

        subject.setProperty(KafkaClientConfig.BOOTSTRAP_SERVERS, "localhost:9092");
        subject.setTopic("any");
        subject.setKey("foo");
        subject.setPayload("bar");

        SampleResult result = subject.sample(new Entry());

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getResponseMessage()).isEqualTo("some error");
    }
}
