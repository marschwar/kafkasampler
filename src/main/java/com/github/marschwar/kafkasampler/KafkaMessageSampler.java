package com.github.marschwar.kafkasampler;

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.marschwar.kafkasampler.KafkaClientConfig.*;

public class KafkaMessageSampler extends AbstractSampler implements Interruptible {
    private static final Logger log = LoggerFactory.getLogger(KafkaMessageSampler.class);

    private static final Set<String> APPLIABLE_CONFIG_CLASSES = new HashSet<>(
        Arrays.asList(
            "com.github.marschwar.kafkasampler.KafkaClientConfigGui",
            "org.apache.jmeter.config.gui.SimpleConfigGui"
        )
    );

    private Producer producer;

    public KafkaMessageSampler() {
        setProperty(new CollectionProperty("_headers", new ArrayList<>()));
    }

    @Override
    public boolean interrupt() {
        if (producer != null) {
            producer.close();
            return true;
        }
        return false;
    }

    @Override
    public SampleResult sample(Entry e) {
        SampleResult res = new SampleResult();
        res.setSampleLabel("Kafka Message");
        res.setRequestHeaders(getHeadersAsDisplayString());
        res.setSamplerData(getPayload());
        res.setResponseCode("212");
        res.setSuccessful(true);
        res.sampleStart();
        res.sampleEnd();

        return res;
    }

    private Producer createProducer() {
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

        return new KafkaProducer(props);
    }

    private String getSecurityProtocol() {
        return getPropertyAsString(SECURITY_PROTOCOL, CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL);
    }

    @Override
    public boolean applies(ConfigTestElement configElement) {

        log.warn("Check applicablae " + configElement);
        final String guiClass = configElement.getProperty(TestElement.GUI_CLASS).getStringValue();
        return APPLIABLE_CONFIG_CLASSES.contains(guiClass);
    }

    public String getKey() {
        return getPropertyAsString("key");
    }

    public void setKey(String key) {
        setProperty("key", key);
    }

    public String getPayload() {
        return getPropertyAsString("payload");
    }

    public void setPayload(String key) {
        setProperty("payload", key);
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
        return getHeaders().stream().map(h -> h.key + " = " + h.value).collect(Collectors.joining("\n"));
    }
}
