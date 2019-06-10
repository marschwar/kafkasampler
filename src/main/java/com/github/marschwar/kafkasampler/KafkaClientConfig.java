package com.github.marschwar.kafkasampler;

public interface KafkaClientConfig {
    String PREFIX = "KafkaClientConfig.";

    String BOOTSTRAP_SERVERS = PREFIX + "BootstrapServers";
    String SECURITY_PROTOCOL = PREFIX + "SecurityProtocol";
    String USE_SSL = PREFIX + "UseSsl";
    String SSL_ENDPOINT_IDENTIFICATION = PREFIX + "SslEndpointIdentification";
    String KEYSTORE_TYPE = PREFIX + "KeystoreType";
    String KEYSTORE_LOCATION = PREFIX + "KeystoreLocation";
    String KEYSTORE_PASSWORD = PREFIX + "KeystorePassword";
    String TRUSTSTORE_LOCATION = PREFIX + "TruststoreLocation";
    String TRUSTSTORE_PASSWORD = PREFIX + "TruststorePassword";
    String SASL_MECHANISM = PREFIX + "SaslMechanism";
    String SASL_JAAS_CONFIG = PREFIX + "SaslJaasConfig";
}
