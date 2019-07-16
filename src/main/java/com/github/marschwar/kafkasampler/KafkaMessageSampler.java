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
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
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
    static final Charset CHARSET = Charset.forName("UTF-8");

    private KafkaProducerBuilder<String, byte[]> producerBuilder = new KafkaProducerBuilderImpl<>();

    public KafkaMessageSampler() {
        setProperty(new CollectionProperty("_headers", new ArrayList<>()));
    }

    @Override
    public SampleResult sample(Entry entry) {

        final String topic = getTopic();
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("topic must be set.");
        }
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, getKey(), getPayload().getBytes(CHARSET));
        getHeaders().forEach(header -> record.headers().add(header.key, header.value.getBytes(CHARSET)));

        final SampleResult res = new SampleResult();
        res.setSampleLabel("Kafka Message");
        res.setRequestHeaders(getHeadersAsDisplayString());
        res.setSamplerData(getPayload());
        res.setDataEncoding(CHARSET.displayName());

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

        return producerBuilder
            .bootstrapServers(getPropertyAsString(BOOTSTRAP_SERVERS))
            .securityProtocol(getSecurityProtocol())
            .ssl(getPropertyAsBoolean(USE_SSL, false))
            .keystoreLocation(getPropertyAsString(KEYSTORE_LOCATION, ""))
            .keystorePassword(getPropertyAsString(KEYSTORE_PASSWORD))
            .truststoreLocation(getPropertyAsString(TRUSTSTORE_LOCATION, ""))
            .truststorePassword(getPropertyAsString(TRUSTSTORE_PASSWORD))
            .sslIEndpointIdentification(getPropertyAsString(SSL_ENDPOINT_IDENTIFICATION))
            .saslJaasConfig(getPropertyAsString(SASL_JAAS_CONFIG))
            .saslMechanism(getPropertyAsString(SASL_MECHANISM))
            .build();
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
            final Header header = Header.fromString(it.next().getStringValue());
            if (header != null) {
                headers.add(header);
            }
        }
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        ArrayList<Header> elements = new ArrayList<>();
        if (headers != null) {
            elements.addAll(headers);
        }
        getHeadersProp().setCollection(elements);
    }

    private CollectionProperty getHeadersProp() {
        return (CollectionProperty) getProperty("_headers");
    }

    private String getHeadersAsDisplayString() {
        return getHeaders().stream().map(h -> h.key + ": " + h.value).collect(Collectors.joining("\n"));
    }
}
