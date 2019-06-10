package com.github.marschwar.kafkasampler;

import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class KafkaMessageSamplerGui extends AbstractSamplerGui {

    private JTextField tfKey;
    private JTextArea taPayload;
    private HeaderTableModel headerTableModel;

    public KafkaMessageSamplerGui() {
        init();
    }

    @Override
    public String getLabelResource() {
        return "kafka_message_sampler";
    }

    @Override
    public String getStaticLabel() {
        return "Kafka Message";
    }

    @Override
    public TestElement createTestElement() {
        KafkaMessageSampler sampler = new KafkaMessageSampler();
        modifyTestElement(sampler);
        return sampler;
    }

    @Override
    public void modifyTestElement(TestElement element) {
        final KafkaMessageSampler sampler = (KafkaMessageSampler) element;
        super.configureTestElement(sampler);

        sampler.setKey(tfKey.getText());
        sampler.setPayload(taPayload.getText());
        sampler.setHeaders(headerTableModel.getData());
    }

    @Override
    public void configure(TestElement element) {
        final KafkaMessageSampler sampler = (KafkaMessageSampler) element;
        super.configure(element);

        tfKey.setText(sampler.getKey());
        taPayload.setText(sampler.getPayload());
        headerTableModel.setData(sampler.getHeaders());
    }

    @Override
    public void clearGui() {
        tfKey.setText("");
        taPayload.setText("");
        super.clearGui();
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        JPanel basicOptionsPanel = createDataPanel();

        JPanel northPanel = new VerticalPanel();
        northPanel.add(makeTitlePanel());
        northPanel.add(basicOptionsPanel);

        taPayload = new JTextArea(20, 40);
        JScrollPane payloadScrollpane = new JScrollPane(taPayload);
        payloadScrollpane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Message Payload"));

        add(northPanel, BorderLayout.NORTH);
        add(payloadScrollpane, BorderLayout.CENTER);
        add(createHeaderPanel(), BorderLayout.SOUTH);
    }

    private JPanel createDataPanel() {
        VerticalPanel panel = new VerticalPanel();

        JPanel row1 = new HorizontalPanel();
        panel.add(row1);
        tfKey = new JTextField(10);
        JLabel lbKey = new JLabel("Message Key");
        lbKey.setLabelFor(tfKey);
        row1.add(lbKey);
        row1.add(tfKey);

        return panel;
    }

    private JPanel createHeaderPanel() {
        VerticalPanel panel = new VerticalPanel();

        JTable tblHeader = new JTable(2, 2);
        headerTableModel = new HeaderTableModel();
        tblHeader.setModel(headerTableModel);
        JScrollPane headerScrollPane = new JScrollPane(tblHeader);
        headerScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Message Headers"));
        panel.add(headerScrollPane);

        return panel;
    }

    private static class HeaderTableModel extends AbstractTableModel {

        private List<Header> data = Collections.emptyList();

        public List<Header> getData() {
            return data;
        }

        public void setData(List<Header> data) {
            this.data = data;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return data.size() + 1;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int i) {
            switch (i) {
                case 0:
                    return "Header Name";
                case 1:
                    return "Header Value";
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isCellEditable(int i, int i1) {
            return true;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= data.size()) {
                return "";
            }
            final Header header = data.get(row);
            switch (col) {
                case 0:
                    return header.key;
                case 1:
                    return header.value;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public void setValueAt(Object o, int row, int col) {
            final Header header = (row < data.size()) ? data.get(row) : new Header();

            switch (col) {
                case 0: {
                    header.key = o.toString();
                    break;
                }
                case 1: {
                    header.value = o.toString();
                    break;
                }
                default:
                    throw new IllegalArgumentException();
            }

            if (row >= data.size()) {
                data.add(header);
            }
        }
    }
}
