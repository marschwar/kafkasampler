package com.github.marschwar.kafkasampler;

import org.apache.jmeter.samplers.Entry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(KafkaExtension.class)
class KafkaMessageSamplerTest {

    private EmbeddedKafkaBroker kafkaEmbedded;

    @Test
    void sample() {

        String topic = "foo";
        KafkaMessageSampler subject = new KafkaMessageSampler();
        subject.setProperty(KafkaClientConfig.BOOTSTRAP_SERVERS, kafkaEmbedded.getBrokersAsString());

        final Consumer<String, String> consumer = createAndListenToTopic(topic);
        subject.sample(new Entry());

        final ConsumerRecord<String, String> record = consumer.poll(Duration.of(2, ChronoUnit.SECONDS)).iterator().next();
        assertThat(record.value()).isEqualTo("value");
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
