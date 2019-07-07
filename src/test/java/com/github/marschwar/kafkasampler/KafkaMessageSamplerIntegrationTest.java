package com.github.marschwar.kafkasampler;

import org.apache.jmeter.samplers.Entry;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(KafkaExtension.class)
class KafkaMessageSamplerIntegrationTest {

    private EmbeddedKafkaBroker kafkaEmbedded;

    @Test
    void testSendSimpleMessage(TestInfo testInfo) {

        String topic = testInfo.getTestMethod().get().getName();
        String messageKey = "messageKey";
        String payload = "some message";

        KafkaMessageSampler subject = new KafkaMessageSampler();
        subject.setProperty(KafkaClientConfig.BOOTSTRAP_SERVERS, kafkaEmbedded.getBrokersAsString());
        subject.setTopic(topic);
        subject.setKey(messageKey);
        subject.setPayload(payload);

        final Consumer<String, String> consumer = createAndListenToTopic(topic);
        subject.sample(new Entry());

        final ConsumerRecord<String, String> record = consumer.poll(Duration.of(2, ChronoUnit.SECONDS)).iterator().next();
        assertThat(record.key()).isEqualTo(messageKey);
        assertThat(record.value()).isEqualTo(payload);
    }

    @Test
    void testSendMultipleMessages(TestInfo testInfo) {

        KafkaMessageSampler baseSubject = new KafkaMessageSampler();
        baseSubject.setProperty(KafkaClientConfig.BOOTSTRAP_SERVERS, kafkaEmbedded.getBrokersAsString());

        String topic = testInfo.getTestMethod().get().getName();
        final Consumer<String, String> consumer = createAndListenToTopic(topic);

        final int MESSAGE_COUNT = 10;
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String messageKey = "messageKey" + i;
            String payload = "some message" + i;

            final KafkaMessageSampler subject = (KafkaMessageSampler) baseSubject.clone();

            subject.setTopic(topic);
            subject.setKey(messageKey);
            subject.setPayload(payload);

            subject.sample(new Entry());
        }

        ConsumerRecords<String, String> records;
        int messageCount = 0;
        do {
            records = consumer.poll(Duration.of(2, ChronoUnit.SECONDS));
            messageCount += records.count();
        } while (records.count() > 0);

        assertThat(messageCount).isEqualTo(MESSAGE_COUNT);
    }

    private Consumer<String, String> createAndListenToTopic(String topic) {
        kafkaEmbedded.addTopics(topic);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaEmbedded.getBrokersAsString());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        Consumer<String, String> consumer = new KafkaConsumer<>(props);

        kafkaEmbedded.consumeFromAnEmbeddedTopic(consumer, topic);

        return consumer;
    }
}
