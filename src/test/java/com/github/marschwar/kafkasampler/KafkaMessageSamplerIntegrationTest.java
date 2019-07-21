package com.github.marschwar.kafkasampler;

import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


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
        subject.setHeaders(Collections.singletonList(new Header("key", "value")));

        final Consumer<String, String> consumer = createAndListenToTopic(topic);
        subject.sample(new Entry());

        List<ConsumerRecord<String, String>> records = receiveNumberOfRecordsOrFail(consumer, 1);
        ConsumerRecord<String, String> record = records.get(0);

        assertThat(record.key()).isEqualTo(messageKey);
        assertThat(record.value()).isEqualTo(payload);
        assertThat(record.headers()).hasSize(1);
        org.apache.kafka.common.header.Header actualHeader = record.headers().toArray()[0];
        assertThat(actualHeader.key()).isEqualTo("key");
        assertThat(new String(actualHeader.value(), Charset.forName("UTF-8"))).isEqualTo("value");
    }

    @Test
    void testSendMultipleMessages(TestInfo testInfo) {

        KafkaMessageSampler baseSubject = new KafkaMessageSampler();
        baseSubject.setProperty(KafkaClientConfig.BOOTSTRAP_SERVERS, kafkaEmbedded.getBrokersAsString());

        String topic = testInfo.getTestMethod().get().getName();
        final Consumer<String, String> consumer = createAndListenToTopic(topic);

        final int MESSAGE_COUNT = 10;
        final List<SampleResult> results = new ArrayList<>();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String messageKey = "messageKey" + i;
            String payload = "some message" + i;

            final KafkaMessageSampler subject = (KafkaMessageSampler) baseSubject.clone();

            subject.setTopic(topic);
            subject.setKey(messageKey);
            subject.setPayload(payload);

            results.add(subject.sample(new Entry()));
        }

        assertThat(results).hasSize(MESSAGE_COUNT);
        final List<SampleResult> withErrors = results
            .stream()
            .filter(r -> !r.isSuccessful())
            .collect(Collectors.toList());
        assertThat(withErrors).isEmpty();

        receiveNumberOfRecordsOrFail(consumer, 10);
    }

    private List<ConsumerRecord<String, String>> receiveNumberOfRecordsOrFail(Consumer<String, String> consumer, int expectedMessageCount) {
        final List<ConsumerRecord<String, String>> allRecords = new ArrayList<>(expectedMessageCount);
        while (allRecords.size() < expectedMessageCount) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            if (records.isEmpty()) {
                fail("No more message returned by polling. Received: " + allRecords.size());
            }
            records.forEach(allRecords::add);
        }
        return allRecords;
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
