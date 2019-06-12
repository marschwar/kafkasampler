package com.github.marschwar.kafkasampler;

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.gui.util.CheckBoxPanel;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.JLabeledTextField;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;

import javax.swing.*;
import java.awt.*;

import static com.github.marschwar.kafkasampler.KafkaClientConfig.*;

public class KafkaClientConfigGui extends AbstractConfigGui {

    private JLabeledTextField tfBootstrapServers;
    private JLabeledTextField tfSecurityProtocol;
    private JCheckBox cbSsl;
    private JLabeledTextField tfSslEndpointIdentification;
    private JLabeledTextField tfKeystoreType;
    private JLabeledTextField tfKeystoreLocation;
    private JLabeledTextField tfKeystorePassword;
    private JLabeledTextField tfTruststoreLocation;
    private JLabeledTextField tfTruststorePassword;
    private JLabeledTextField tfSaslMechanism;
    private JLabeledTextField tfSaslJaasConfig;

    public KafkaClientConfigGui() {
        init();
    }

    @Override
    public String getLabelResource() {
        return "kafka_config_title"; // $NON-NLS-1$
    }

    @Override
    public String getStaticLabel() {
        return "Kafka Client Config";
    }

    @Override
    public TestElement createTestElement() {
        ConfigTestElement config = new ConfigTestElement();
        modifyTestElement(config);
        return config;
    }

    /**
     * Modifies a given TestElement to mirror the data in the gui components.
     *
     * @see org.apache.jmeter.gui.JMeterGUIComponent#modifyTestElement(TestElement)
     */
    @Override
    public void modifyTestElement(TestElement config) {
        ConfigTestElement cfg = (ConfigTestElement) config;
        cfg.clear();
        super.configureTestElement(config);

        config.setProperty(BOOTSTRAP_SERVERS, tfBootstrapServers.getText());
        config.setProperty(SECURITY_PROTOCOL, tfSecurityProtocol.getText());
        config.setProperty(USE_SSL, cbSsl.isSelected());
        if (cbSsl.isSelected()) {
            config.setProperty(SSL_ENDPOINT_IDENTIFICATION, tfSslEndpointIdentification.getText());
            config.setProperty(KEYSTORE_TYPE, tfKeystoreType.getText());
            config.setProperty(KEYSTORE_LOCATION, tfKeystoreLocation.getText());
            config.setProperty(KEYSTORE_PASSWORD, tfKeystorePassword.getText());
            config.setProperty(TRUSTSTORE_LOCATION, tfTruststoreLocation.getText());
            config.setProperty(TRUSTSTORE_PASSWORD, tfTruststorePassword.getText());
        }

        config.setProperty(SASL_MECHANISM, tfSaslMechanism.getText());
        config.setProperty(SASL_JAAS_CONFIG, tfSaslJaasConfig.getText());

    }

    @Override
    public void clearGui() {
        super.clearGui();
        tfBootstrapServers.setText("");
        tfSecurityProtocol.setText(CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL);
        cbSsl.setSelected(true);
        tfSslEndpointIdentification.setText("");
        tfKeystoreType.setText("");
        tfKeystoreLocation.setText("");
        tfKeystorePassword.setText("");
        tfTruststoreLocation.setText("");
        tfTruststorePassword.setText("");
        tfSaslMechanism.setText(SaslConfigs.DEFAULT_SASL_MECHANISM);
        tfSaslJaasConfig.setText("");
    }

    @Override
    public void configure(TestElement el) {
        super.configure(el);
        AbstractTestElement samplerBase = (AbstractTestElement) el;

        tfBootstrapServers.setText(samplerBase.getPropertyAsString(BOOTSTRAP_SERVERS));
        tfSecurityProtocol.setText(samplerBase.getPropertyAsString(SECURITY_PROTOCOL));
        boolean useSsl = samplerBase.getPropertyAsBoolean(USE_SSL);
        cbSsl.setSelected(useSsl);
        if (useSsl) {
            tfKeystoreType.setText(samplerBase.getPropertyAsString(KEYSTORE_TYPE));
            tfKeystoreLocation.setText(samplerBase.getPropertyAsString(KEYSTORE_LOCATION));
            tfKeystorePassword.setText(samplerBase.getPropertyAsString(KEYSTORE_PASSWORD));
            tfTruststoreLocation.setText(samplerBase.getPropertyAsString(TRUSTSTORE_LOCATION));
            tfTruststorePassword.setText(samplerBase.getPropertyAsString(TRUSTSTORE_PASSWORD));
            tfSslEndpointIdentification.setText(samplerBase.getPropertyAsString(SSL_ENDPOINT_IDENTIFICATION));
        }

        tfSaslMechanism.setText(samplerBase.getPropertyAsString(SASL_MECHANISM));
        tfSaslJaasConfig.setText(samplerBase.getPropertyAsString(SASL_JAAS_CONFIG));
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        JPanel basicOptionsPanel = createBasicOptionsPanel();

        JPanel northPanel = new VerticalPanel();
        northPanel.add(makeTitlePanel());
        northPanel.add(basicOptionsPanel);

        JTabbedPane centerPanel = new JTabbedPane();
        centerPanel.add("SSL", createSslOptionsPanel());
        centerPanel.add("SASL", createSaslOptionsPanel());

        add(northPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createBasicOptionsPanel() {
        JPanel panel = new VerticalPanel();

        JPanel row1 = new HorizontalPanel();
        panel.add(row1);
        tfBootstrapServers = new JLabeledTextField("Bootstrap Servers:", 10);
        row1.add(tfBootstrapServers);

        JPanel row2 = new HorizontalPanel();
        panel.add(row2);
        tfSecurityProtocol = new JLabeledTextField("Security Protocol:", 5);
        row2.add(tfSecurityProtocol);

        return panel;
    }

    private JPanel createSslOptionsPanel() {
        JPanel panel = new VerticalPanel();

        cbSsl = new JCheckBox("Use SSL");
        panel.add(CheckBoxPanel.wrap(cbSsl));

        JPanel keystorePanel = new HorizontalPanel();
        keystorePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Keystore"));
        panel.add(keystorePanel);
        tfKeystoreType = new JLabeledTextField("Type:", 10);
        keystorePanel.add(tfKeystoreType);
        tfKeystoreLocation = new JLabeledTextField("Location:", 10);
        keystorePanel.add(tfKeystoreLocation);
        tfKeystorePassword = new JLabeledTextField("Password:", 5);
        keystorePanel.add(tfKeystorePassword);

        JPanel row2 = new HorizontalPanel();
        panel.add(row2);
        tfTruststoreLocation = new JLabeledTextField("Truststore Location:", 10);
        row2.add(tfTruststoreLocation);
        tfTruststorePassword = new JLabeledTextField("Truststore Password:", 5);
        row2.add(tfTruststorePassword);

        JPanel row3 = new HorizontalPanel();
        panel.add(row3);
        tfSslEndpointIdentification = new JLabeledTextField("Endpoint Identification:", 5);
        row3.add(tfSslEndpointIdentification);

        cbSsl.addChangeListener((changeEvent) -> {
            boolean selected = ((JCheckBox) changeEvent.getSource()).isSelected();
            tfKeystoreType.setEnabled(selected);
            tfKeystoreLocation.setEnabled(selected);
            tfKeystorePassword.setEnabled(selected);
            tfTruststoreLocation.setEnabled(selected);
            tfTruststorePassword.setEnabled(selected);
            tfSslEndpointIdentification.setEnabled(selected);
        });

        return panel;
    }

    private JPanel createSaslOptionsPanel() {
        JPanel panel = new VerticalPanel();

        JPanel row1 = new HorizontalPanel();
        panel.add(row1);
        tfSaslMechanism = new JLabeledTextField("Mechanism:", 10);
        row1.add(tfSaslMechanism);

        JPanel row2 = new HorizontalPanel();
        panel.add(row2);
        tfSaslJaasConfig = new JLabeledTextField("JAAS Config:", 10);
        row2.add(tfSaslJaasConfig);

        return panel;
    }

}
