package com.github.marschwar.kafkasampler;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.io.File;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static com.github.marschwar.kafkasampler.KafkaMessageSampler.CHARSET;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(KafkaExtension.class)
class JMeterIntegrationTest {

    private EmbeddedKafkaBroker kafkaEmbedded;

    private Consumer<String, String> consumer;

    @BeforeAll
    public static void initJMeter() throws Exception {

        JMeterUtils.setJMeterHome("/home/markus/git/kafkasampler/fake-jmeter");
        JMeterUtils.loadJMeterProperties("/home/markus/git/kafkasampler/fake-jmeter/bin/jmeter.properties");

        SaveService.loadProperties();
    }

    @BeforeEach
    public void prepareKafkaAndRunTestplan(TestInfo info) throws Exception {

        final String testName = info.getTestMethod().get().getName();
        final String topic = testName;

        System.setProperty("KAFKA_BROKERS", kafkaEmbedded.getBrokersAsString());
        System.setProperty("KAFKA_TEST_TOPIC", topic);

        kafkaEmbedded.addTopics(topic);
        consumer = createConsumerForTopic(topic);

        final File testPlan = new File(getClass().getResource("/testplans/" + testName + ".jmx").getFile());
        HashTree testPlanTree = SaveService.loadTree(testPlan);

        final StandardJMeterEngine jmeter = new StandardJMeterEngine();
        jmeter.configure(testPlanTree);
        jmeter.run();
    }

    private ConsumerRecords<String, String> poll() {
        return consumer.poll(Duration.ofSeconds(10));
    }

    @Test
    public void singleMessageWithHeaders() {
        ConsumerRecords<String, String> records = poll();

        assertThat(records.count()).isOne();
        ConsumerRecord<String, String> firstMessage = records.iterator().next();
        assertThat(firstMessage.key()).isEqualTo("key");
        assertThat(firstMessage.headers()).isNotEmpty();
        assertThat(headerValueAsString(firstMessage.headers().lastHeader("Header1"))).isEqualTo("foo");
        assertThat(headerValueAsString(firstMessage.headers().lastHeader("Header2"))).isEqualTo("bar");
    }

    private Consumer<String, String> createConsumerForTopic(String topic) {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaEmbedded.getBrokersAsString());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, String> consumer = new KafkaConsumer<>(props);

        kafkaEmbedded.consumeFromAnEmbeddedTopic(consumer, topic);

        return consumer;
    }

    private static String headerValueAsString(org.apache.kafka.common.header.Header header) {
        return new String(header.value(), CHARSET);
    }

}
