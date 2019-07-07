package com.github.marschwar.kafkasampler;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.marschwar.kafkasampler.KafkaClientConfig.*;

public class KafkaMessageSampler extends AbstractSampler {
    private static final Logger log = LoggerFactory.getLogger(KafkaMessageSampler.class);

    private static final Set<String> APPLIABLE_CONFIG_CLASSES = new HashSet<>(
        Arrays.asList(
            "com.github.marschwar.kafkasampler.KafkaClientConfigGui",
            "org.apache.jmeter.config.gui.SimpleConfigGui"
        )
    );

    private static final String KEY_TOPIC = "topic";
    private static final String KEY_MESSAGE_KEY = "key";
    private static final String KEY_MESSAGE_PAYLOAD = "payload";

    public KafkaMessageSampler() {
        setProperty(new CollectionProperty("_headers", new ArrayList<>()));
    }

    @Override
    public SampleResult sample(Entry entry) {

        final String topic = getTopic();
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("topic must be set.");
        }
        final Charset UTF8 = Charset.forName("UTF-8");
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, getKey(), getPayload().getBytes(UTF8));
        getHeaders().forEach(header -> record.headers().add(header.key, header.value.getBytes(UTF8)));

        final SampleResult res = new SampleResult();
        res.setSampleLabel("Kafka Message");
        res.setRequestHeaders(getHeadersAsDisplayString());
        res.setSamplerData(getPayload());
        res.setDataEncoding(UTF8.displayName());

        try (Producer<String, byte[]> producer = createProducer()) {
            res.sampleStart();
            producer.send(record, (metadata, exception) -> {
                res.sampleEnd();
                if (exception != null) {
                    log.error("Error sending message", exception);
                    res.setSuccessful(false);
                    res.setResponseMessage(exception.getMessage());
                } else {
                    res.setSuccessful(true);
                    res.setResponseHeaders(
                        "Topic: " + metadata.topic() +
                            "\nPartition: " + metadata.partition() +
                            "\nOffset: " + metadata.offset() +
                            "\nTimestamp: " + metadata.timestamp()
                    );
                }
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error sending message", e);
            res.setSuccessful(false);
            res.setResponseMessage(e.getMessage());
        }
        return res;
    }

    private Producer<String, byte[]> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getPropertyAsString(BOOTSTRAP_SERVERS));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, "3");
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, getSecurityProtocol());

        final boolean useSsl = getPropertyAsBoolean(USE_SSL, false);

        if (useSsl) {
            final String keystoreLocation = getPropertyAsString(KEYSTORE_LOCATION, "").trim();
            if (!keystoreLocation.isEmpty()) {
                props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation);
                props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, getPropertyAsString(KEYSTORE_PASSWORD));
            }

            final String truststoreLocation = getPropertyAsString(TRUSTSTORE_LOCATION, "").trim();
            if (!truststoreLocation.isEmpty()) {
                props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
                props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, getPropertyAsString(TRUSTSTORE_PASSWORD));
            }
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, getPropertyAsString(SSL_ENDPOINT_IDENTIFICATION));
        }

        props.put(SaslConfigs.SASL_JAAS_CONFIG, getPropertyAsString(SASL_JAAS_CONFIG));
        props.put(SaslConfigs.SASL_MECHANISM, getPropertyAsString(SASL_MECHANISM));

        return new KafkaProducer<>(props);
    }

    private String getSecurityProtocol() {
        return getPropertyAsString(SECURITY_PROTOCOL, CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL);
    }

    @Override
    public boolean applies(ConfigTestElement configElement) {
        final String guiClass = configElement.getProperty(TestElement.GUI_CLASS).getStringValue();
        return APPLIABLE_CONFIG_CLASSES.contains(guiClass);
    }

    public String getTopic() {
        return getPropertyAsString(KEY_TOPIC);
    }

    public String getKey() {
        return getPropertyAsString(KEY_MESSAGE_KEY);
    }

    public void setTopic(String topic) {
        setProperty(KEY_TOPIC, topic);
    }

    public void setKey(String key) {
        setProperty(KEY_MESSAGE_KEY, key);
    }

    public String getPayload() {
        return getPropertyAsString(KEY_MESSAGE_PAYLOAD);
    }

    public void setPayload(String key) {
        setProperty(KEY_MESSAGE_PAYLOAD, key);
    }

    public List<Header> getHeaders() {
        final PropertyIterator it = getHeadersProp().iterator();
        final List<Header> headers = new ArrayList<>();

        while (it.hasNext()) {
            headers.add(Header.fromString(it.next().getStringValue()));
        }
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        getHeadersProp().setCollection(headers == null ? new ArrayList<>() : headers);
    }

    private CollectionProperty getHeadersProp() {
        return (CollectionProperty) getProperty("_headers");
    }

    private String getHeadersAsDisplayString() {
        return getHeaders().stream().map(h -> h.key + ": " + h.value).collect(Collectors.joining("\n"));
    }
}
