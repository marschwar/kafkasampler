package com.github.marschwar.kafkasampler;

import org.apache.kafka.clients.producer.KafkaProducer;

public interface KafkaProducerBuilder<K, V> {

    KafkaProducer<K, V> build();

    KafkaProducerBuilder<K, V> bootstrapServers(String bootstrapServers);

    KafkaProducerBuilder<K, V> securityProtocol(String protocol);

    KafkaProducerBuilder<K, V> ssl(boolean useSsl);

    KafkaProducerBuilder<K, V> sslIEndpointIdentification(String algorithm);

    KafkaProducerBuilder<K, V> keystoreLocation(String location);

    KafkaProducerBuilder<K, V> keystorePassword(String password);

    KafkaProducerBuilder<K, V> truststoreLocation(String location);

    KafkaProducerBuilder<K, V> truststorePassword(String password);

    KafkaProducerBuilder<K, V> saslJaasConfig(String config);

    KafkaProducerBuilder<K, V> saslMechanism(String mechanism);
}
