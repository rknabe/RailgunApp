package com.rkade;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Objects;
import java.util.logging.Logger;

public class MainForm extends BaseForm implements DeviceListener, ActionListener, FocusListener, ChangeListener {
    private final static Logger logger = Logger.getLogger(MainForm.class.getName());
    private final AxisPanel axisPanel = new AxisPanel();
    private final DefaultComboBoxModel<Device> deviceListModel = new DefaultComboBoxModel<>();
    private JSpinner spPlayerNum;
    private JPanel mainPanel;
    private JPanel bottomPanel;
    private JLabel statusLabel;
    private JLabel firmwareLabel;
    private JButton defaultsButton;
    private JButton loadButton;
    private JButton saveButton;
    private ButtonsPanel buttonsPanel;
    private JPanel axisPanelParent;
    private JButton btnCalibrate;
    private JPanel northPanel;
    private JPanel settingsPanel;
    private JCheckBox cbAutoRecoil;
    private JSpinner spAutoTriggerSpeed;
    private JLabel lblAutoTrigger;
    private JSpinner spTriggerHold;
    private JLabel lblTriggerHold;
    private JComboBox<Device> deviceList;
    private JButton btnConnect;
    private JPanel devicePanel;
    private JLabel lblPlayerNum;
    private JLabel lblRecoilStrength;
    private JRadioButton rbFull;
    private JRadioButton rbMed;
    private JButton btnUpdate;
    private ButtonGroup recoilStrengthGroup = new ButtonGroup();
    private Device device = null;
    private volatile boolean isCalibrating = false;
    private SettingsDataReport lastSettings = null;
    private DeviceManager deviceManager;

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    public MainForm() {
        controls = java.util.List.of(btnCalibrate, defaultsButton, saveButton, loadButton, cbAutoRecoil,
                spAutoTriggerSpeed, spTriggerHold, deviceList, btnConnect, spPlayerNum, rbFull, rbMed, btnUpdate);

        SpinnerNumberModel triggerSpeedModel = new SpinnerNumberModel(100, 0, 3000, 10);
        spAutoTriggerSpeed.setModel(triggerSpeedModel);
        JSpinner.NumberEditor triggerSpeedEditor = new JSpinner.NumberEditor(spAutoTriggerSpeed, "#");
        JFormattedTextField triggerSpeedField = triggerSpeedEditor.getTextField();
        DefaultFormatter triggerSpeedFormatter = (DefaultFormatter) triggerSpeedField.getFormatter();
        triggerSpeedFormatter.setCommitsOnValidEdit(true);
        spAutoTriggerSpeed.setEditor(triggerSpeedEditor);

        SpinnerNumberModel triggerHoldModel = new SpinnerNumberModel(500, 0, 5000, 10);
        spTriggerHold.setModel(triggerHoldModel);
        JSpinner.NumberEditor triggerHoldEditor = new JSpinner.NumberEditor(spTriggerHold, "#");
        JFormattedTextField triggerHoldField = triggerHoldEditor.getTextField();
        DefaultFormatter triggerHoldFormatter = (DefaultFormatter) triggerHoldField.getFormatter();
        triggerHoldFormatter.setCommitsOnValidEdit(true);
        spTriggerHold.setEditor(triggerHoldEditor);

        SpinnerNumberModel playerNumModel = new SpinnerNumberModel(1, 1, 32, 1);
        spPlayerNum.setModel(playerNumModel);
        JSpinner.NumberEditor playerNumEditor = new JSpinner.NumberEditor(spPlayerNum, "#");
        JFormattedTextField playerNumField = playerNumEditor.getTextField();
        DefaultFormatter playerNumFormatter = (DefaultFormatter) playerNumField.getFormatter();
        playerNumFormatter.setCommitsOnValidEdit(true);
        spPlayerNum.setEditor(playerNumEditor);

        recoilStrengthGroup.add(rbFull);
        recoilStrengthGroup.add(rbMed);

        deviceList.setModel(deviceListModel);

        axisPanelParent.add(axisPanel);
        setupControlListener();
        setPanelEnabled(false);
        devicePanel.setEnabled(true);//these should always be enabled
        btnConnect.setEnabled(true);
        deviceList.setEnabled(true);
    }

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean status = handleAction(e);
        if (!status) {
            logger.warning("Action failed for:" + e.getActionCommand());
        }
    }

    private boolean handleAction(ActionEvent e) {
        if (device != null) {
            if (Objects.equals(e.getActionCommand(), btnCalibrate.getActionCommand())) {
                if (!isCalibrating) {
                    btnCalibrate.setEnabled(false);
                    isCalibrating = true;
                    //set limits to max before calibrating
                    device.setAxisLimits(Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MIN_VALUE, Byte.MAX_VALUE);
                    btnCalibrate.setText("Calibrating...");
                    axisPanel.setCalibrating(true);
                    JLabel validator = new JLabel("<html><body>Please move the crosshair to each corner." +
                            "<br>Click 'Save Settings' afterward to make changes permanent.</br></body></html>");
                    JOptionPane pane = new JOptionPane(validator, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                            null, new Object[]{}, null);
                    final JDialog dialog = pane.createDialog(mainPanel, "Calibrating");
                    dialog.setModal(true);
                    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    final Timer timer = getTimer(dialog, 12000);
                    timer.start();
                    dialog.setVisible(true);
                }
            } else if (e.getActionCommand().equals(saveButton.getActionCommand())) {
                saveButton.setEnabled(false);
                boolean status = device.saveSettings();
                saveButton.setEnabled(true);
                return status;
            } else if (e.getActionCommand().equals(defaultsButton.getActionCommand())) {
                defaultsButton.setEnabled(false);
                boolean status = device.loadDefaults();
                defaultsButton.setEnabled(true);
                return status;
            } else if (e.getActionCommand().equals(loadButton.getActionCommand())) {
                loadButton.setEnabled(false);
                boolean status = device.loadFromEeprom();
                loadButton.setEnabled(true);
                return status;
            } else if (e.getActionCommand().equals(cbAutoRecoil.getActionCommand())) {
                return device.setAutoRecoil(cbAutoRecoil.isSelected());
            } else if (e.getActionCommand().equals(rbFull.getActionCommand()) || e.getActionCommand().equals(rbMed.getActionCommand())) {
                return device.setRecoilStrength(rbFull.isSelected() ? 255 : 180);
            } else if (e.getActionCommand().equals(btnUpdate.getActionCommand())) {
                FirmwareDialog dialog = new FirmwareDialog(deviceManager.getConnectedDevice());
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                return true;
            }
        }
        if (e.getActionCommand().equals(btnConnect.getActionCommand())) {
            return deviceManager.connectDevice((Device) deviceList.getSelectedItem());
        } else if (e.getActionCommand().equals(deviceList.getActionCommand())) {
            btnConnect.setEnabled(deviceList.getSelectedItem() != null);
        }
        return true;
    }

    private Timer getTimer(JDialog dialog, int delay) {
        final Timer timer = new Timer(delay, null);
        ActionListener taskPerformer = e1 -> {
            isCalibrating = false;
            axisPanel.setCalibrating(false);
            dialog.setVisible(false);
            btnCalibrate.setText("Calibrate");
            btnCalibrate.setEnabled(true);
            updateCalibrationSettings();
            timer.stop();
        };
        timer.addActionListener(taskPerformer);
        return timer;
    }

    private void updateCalibrationSettings() {
        short xMin = axisPanel.getXAxisMinimum();
        short xMax = axisPanel.getXAxisMaximum();
        short yMin = axisPanel.getYAxisMinimum();
        short yMax = axisPanel.getYAxisMaximum();

        short nxMin = (short) axisPanel.normalize(xMin, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MIN_VALUE, Byte.MAX_VALUE);
        short nxMax = (short) axisPanel.normalize(xMax, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MIN_VALUE, Byte.MAX_VALUE);
        short nyMin = (short) axisPanel.normalize(yMin, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MIN_VALUE, Byte.MAX_VALUE);
        short nyMax = (short) axisPanel.normalize(yMax, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MIN_VALUE, Byte.MAX_VALUE);

        boolean sent = device.setAxisLimits(nxMin, nxMax, nyMin, nyMax);
        if (!sent) {
            logger.warning("failed to send calibration settings");
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (device != null) {
            if (e.getSource() == spPlayerNum) {
                Number ival = (Number) spPlayerNum.getValue();
                if (ival == null) {
                    ival = 1;
                }
                short newValue = ival.shortValue();
                if (lastSettings == null || newValue != lastSettings.getPlayerNumber()) {
                    device.setPlayerNumber(ival.shortValue());
                }
            }
        }
    }

    @Override
    public void deviceAttached(Device device) {
        if (deviceListModel.getIndexOf(device) < 0) {
            deviceListModel.addElement(device);
            statusLabel.setText("Found, ready to connect...");
        }
        devicePanel.setEnabled(true);//these should always be enabled
        btnConnect.setEnabled(true);
        deviceList.setEnabled(true);
    }

    @Override
    public void deviceDetached(Device device) {
        deviceListModel.removeElement(device);
    }

    @Override
    public void deviceConnected(Device device) {
        this.device = device;
        firmwareLabel.setText(device.getFirmwareType() + ":" + device.getFirmwareVersion());
        setPanelEnabled(true);
        statusLabel.setText("Connected");
        buttonsPanel.deviceConnected(device);
        devicePanel.setEnabled(true);//these should always be enabled
        btnConnect.setEnabled(true);
        deviceList.setEnabled(true);
    }

    @Override
    public void deviceDisconnected(Device device) {
        firmwareLabel.setText("");
        this.device = null;
        setPanelEnabled(false);
        statusLabel.setText("Detached");
        buttonsPanel.deviceDisconnected(device);
        devicePanel.setEnabled(true);//these should always be enabled
        btnConnect.setEnabled(true);
        deviceList.setEnabled(true);
    }

    @Override
    public void deviceUpdated(Device device, String status, DataReport report) {
        SwingUtilities.invokeLater(() -> {
            if (status != null) {
                statusLabel.setText(status);
            }

            if (report != null) {
                switch (report) {
                    case ButtonsDataReport buttonsData -> buttonsPanel.deviceUpdated(device, status, buttonsData);
                    case SettingsDataReport settings -> {
                        lastSettings = settings;
                        firmwareLabel.setText(settings.getDeviceType() + ":" + settings.getDeviceVersion());
                        axisPanel.setXAxisMinimum(settings.getXAxisMinimum());
                        axisPanel.setXAxisMaximum(settings.getXAxisMaximum());
                        axisPanel.setYAxisMinimum(settings.getYAxisMinimum());
                        axisPanel.setYAxisMaximum(settings.getYAxisMaximum());
                        cbAutoRecoil.setSelected(settings.isAutoRecoil());
                        spAutoTriggerSpeed.getModel().setValue(settings.getTriggerRepeatDelay());
                        spTriggerHold.getModel().setValue(settings.getTriggerHoldTime());
                        spPlayerNum.getModel().setValue(settings.getPlayerNumber());
                        if (settings.getRecoilStrength() < 255) {
                            rbMed.setSelected(true);
                        } else {
                            rbFull.setSelected(true);
                        }
                    }
                    case AxisDataReport axisData -> axisPanel.deviceUpdated(device, status, axisData);
                    default -> {
                    }
                }
            }
        });
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (device != null) {
            boolean status = true;
            if (e.getSource() == spAutoTriggerSpeed) {
                Number ival = (Number) spAutoTriggerSpeed.getValue();
                if (ival == null) {
                    ival = 1000;
                }
                short newValue = ival.shortValue();
                if (lastSettings == null || newValue != lastSettings.getTriggerRepeatDelay()) {
                    status = device.setTriggerRepeatRate(ival.shortValue());
                }
            } else if (e.getSource() == spTriggerHold) {
                Number ival = (Number) spTriggerHold.getValue();
                if (ival == null) {
                    ival = 1000;
                }
                short newValue = ival.shortValue();
                if (lastSettings == null || newValue != lastSettings.getTriggerHoldTime()) {
                    status = device.setTriggerHoldTime(ival.shortValue());
                }
            } else if (e.getSource() == spPlayerNum) {
                Number ival = (Number) spPlayerNum.getValue();
                if (ival == null) {
                    ival = 1;
                }
                short newValue = ival.shortValue();
                if (lastSettings == null || newValue != lastSettings.getPlayerNumber()) {
                    status = device.setPlayerNumber(ival.shortValue());
                }
            }
            if (!status) {
                logger.warning("State Changed, failed for:" + e.getSource());
            }
        }
        if (e.getSource() == deviceList) {
            btnConnect.setEnabled(deviceList.getSelectedItem() != null);
        }
    }

    public JComponent getRootComponent() {
        return mainPanel;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setMinimumSize(new Dimension(1060, 400));
        mainPanel.setPreferredSize(new Dimension(1060, 800));
        buttonsPanel = new ButtonsPanel();
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        bottomPanel.setAutoscrolls(false);
        bottomPanel.setMinimumSize(new Dimension(1060, 75));
        bottomPanel.setPreferredSize(new Dimension(1065, 75));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(bottomPanel, gbc);
        bottomPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        panel1.setPreferredSize(new Dimension(445, 32));
        bottomPanel.add(panel1);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        firmwareLabel = new JLabel();
        firmwareLabel.setEnabled(true);
        firmwareLabel.setFocusable(false);
        firmwareLabel.setHorizontalAlignment(4);
        firmwareLabel.setMaximumSize(new Dimension(70, 17));
        firmwareLabel.setMinimumSize(new Dimension(60, 17));
        firmwareLabel.setPreferredSize(new Dimension(70, 20));
        firmwareLabel.setText("Firmware:");
        panel1.add(firmwareLabel);
        statusLabel = new JLabel();
        statusLabel.setEnabled(true);
        statusLabel.setFocusable(false);
        statusLabel.setHorizontalAlignment(2);
        statusLabel.setMinimumSize(new Dimension(130, 20));
        statusLabel.setPreferredSize(new Dimension(130, 20));
        statusLabel.setRequestFocusEnabled(true);
        statusLabel.setText("Device Not Found...");
        panel1.add(statusLabel);
        defaultsButton = new JButton();
        defaultsButton.setActionCommand("resetDefaults");
        defaultsButton.setMaximumSize(new Dimension(196, 30));
        defaultsButton.setMinimumSize(new Dimension(196, 30));
        defaultsButton.setPreferredSize(new Dimension(196, 30));
        defaultsButton.setText("Reset Settings to Defaults");
        bottomPanel.add(defaultsButton);
        loadButton = new JButton();
        loadButton.setActionCommand("loadEEPROM");
        loadButton.setMaximumSize(new Dimension(196, 30));
        loadButton.setMinimumSize(new Dimension(196, 30));
        loadButton.setPreferredSize(new Dimension(196, 30));
        loadButton.setText("Load Settings From EEPROM");
        bottomPanel.add(loadButton);
        saveButton = new JButton();
        saveButton.setActionCommand("saveSettings");
        saveButton.setHorizontalAlignment(0);
        saveButton.setMaximumSize(new Dimension(196, 30));
        saveButton.setMinimumSize(new Dimension(196, 30));
        saveButton.setPreferredSize(new Dimension(196, 30));
        saveButton.setText("Save Settings to EEPROM");
        bottomPanel.add(saveButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}