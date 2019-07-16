package com.github.marschwar.kafkasampler;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.Serializable;
import java.util.Properties;

public class KafkaProducerBuilderImpl<K, V> implements KafkaProducerBuilder<K, V>, Serializable {

    private String bootstrapServers;
    private String securityProtocol;
    private boolean useSsl;
    private String endpointIdentification;
    private String keystoreLocation;
    private String keystorePassword;
    private String truststoreLocation;
    private String truststorePassword;
    private String saslJaasConfig;
    private String saslMechanism;

    @Override
    public KafkaProducer<K, V> build() {
        final Properties props = createProps();
        return new KafkaProducer<>(props);
    }

    @Override
    public KafkaProducerBuilder<K, V> bootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        return this;
    }

    @Override
    public KafkaProducerBuilder<K, V> securityProtocol(String protocol) {
        this.securityProtocol = protocol;
        return this;
    }

    @Override
    public KafkaProducerBuilder<K, V> ssl(boolean useSsl) {
        this.useSsl = useSsl;
        return this;
    }

    @Override
    public KafkaProducerBuilder<K, V> sslIEndpointIdentification(String algorithm) {
        this.endpointIdentification = algorithm;
        return this;
    }

    @Override
    public KafkaProducerBuilder<K, V> keystoreLocation(String location) {
        this.keystoreLocation = location;
        return this;
    }

    @Override
    public KafkaProducerBuilder<K, V> keystorePassword(String password) {
        this.keystorePassword = password;
        return this;
    }

    @Override
    public KafkaProducerBuilder<K, V> truststoreLocation(String location) {
        this.truststoreLocation = location;
        return this;
    }

    @Override
    public KafkaProducerBuilder<K, V> truststorePassword(String password) {
        this.truststorePassword = password;
        return this;
    }

    @Override
    public KafkaProducerBuilder<K, V> saslJaasConfig(String config) {
        this.saslJaasConfig = config;
        return this;
    }

    @Override
    public KafkaProducerBuilder<K, V> saslMechanism(String mechanism) {
        this.saslMechanism = mechanism;
        return this;
    }

    private Properties createProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, "3");
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);

        if (useSsl) {
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, endpointIdentification);
            if (StringUtils.isNotBlank(keystoreLocation)) {
                props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation);
                props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
            }

            if (StringUtils.isNotBlank(truststoreLocation)) {
                props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
                props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
            }
        }

        props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);

        return props;
    }
}
