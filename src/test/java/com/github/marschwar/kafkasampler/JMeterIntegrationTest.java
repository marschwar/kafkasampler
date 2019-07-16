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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.github.marschwar.kafkasampler.KafkaMessageSampler.CHARSET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(KafkaExtension.class)
class JMeterIntegrationTest {

    private static final String FAKE_JMETER_DIR = Paths.get("./fake-jmeter").toAbsolutePath().toString();

    private EmbeddedKafkaBroker kafkaEmbedded;

    private Consumer<String, String> consumer;

    @BeforeAll
    public static void initJMeter() throws Exception {

        JMeterUtils.setJMeterHome(FAKE_JMETER_DIR.toString());
        final Path propsPath = Paths.get(FAKE_JMETER_DIR, "bin", "jmeter.properties");
        JMeterUtils.loadJMeterProperties(propsPath.toString());

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

    @Test
    public void singleMessageWithHeaders() {
        ConsumerRecord<String, String> firstMessage = receiveNumberOfRecordsOrFail(1).get(0);
        ;
        assertThat(firstMessage.key()).isEqualTo("key");
        assertThat(firstMessage.headers()).isNotEmpty();
        assertThat(headerValueAsString(firstMessage.headers().lastHeader("Header1"))).isEqualTo("foo");
        assertThat(headerValueAsString(firstMessage.headers().lastHeader("Header2"))).isEqualTo("bar");
    }

    @Test
    public void fiveMessagesFromSameThreadGroup() {

        final int expectedMessageCount = 5;

        receiveNumberOfRecordsOrFail(expectedMessageCount);
    }

    @Test
    public void fiveMessagesFromDifferentThreadGroups() {

        final int expectedMessageCount = 5;

        receiveNumberOfRecordsOrFail(expectedMessageCount);
    }

    private List<ConsumerRecord<String, String>> receiveNumberOfRecordsOrFail(int expectedMessageCount) {
        final List<ConsumerRecord<String, String>> allRecords = new ArrayList<>(expectedMessageCount);
        while (allRecords.size() < expectedMessageCount) {
            ConsumerRecords<String, String> records = poll();
            if (records.isEmpty()) {
                fail("No more message returned by polling. Received: " + allRecords.size());
            }
            records.forEach(record -> allRecords.add(record));
        }
        return allRecords;
    }

    private ConsumerRecords<String, String> poll() {
        return consumer.poll(Duration.ofSeconds(5));
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
