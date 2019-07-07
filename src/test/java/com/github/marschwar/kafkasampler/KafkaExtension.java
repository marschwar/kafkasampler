package com.github.marschwar.kafkasampler;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.Arrays;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class KafkaExtension implements BeforeAllCallback, BeforeEachCallback, ExtensionContext.Store.CloseableResource {

    private static boolean started = false;

    private static final Logger log = LoggerFactory.getLogger(KafkaExtension.class);

    private static EmbeddedKafkaBroker broker;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (broker == null) {
            broker = new EmbeddedKafkaBroker(1);
            broker.afterPropertiesSet();
            context.getRoot().getStore(GLOBAL).put("kafkaextension", this);
        }
    }

    @Override
    public void close() {
        if (broker != null) {
            broker.destroy();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getTestInstance().ifPresent(testinstance ->
            Arrays.stream(testinstance.getClass().getDeclaredFields())
                .filter(field -> field.getType() == EmbeddedKafkaBroker.class)
                .findFirst().ifPresent(field -> {
                    field.setAccessible(true);
                    try {
                        field.set(testinstance, broker);
                    } catch (IllegalAccessException e) {
                        log.error("Unable to set broker", e);
                    }
                }
            ));
    }
}
