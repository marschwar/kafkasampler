# Fake JMeter

This contains the default JMeter property files as well as the expected file structure in order to run jmeter inside
an integration test.

The file `fake-jmeter/lib/ext/ApacheJMeter_functions-5.1.1.jar` is actually empty. It needs to be there so the
class path scan picks up the functions during initialization.
