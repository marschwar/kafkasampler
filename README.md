# kafkasampler - Kafka JMeter Extension

This extension provides two components:

* Kafka Producer Sampler: sends messages to Kafka
* Kafka Client Config: Allows configuration of kafka clients that are used by the sampler.

## Install

Build the extension:

    gradlew build

Install the extension into `$JMETER_HOME/lib/ext`:

    cp build/libs/kafkasampler-x.y.z.jar $JMETER_HOME/lib/ext
