package com.rkade;

import com.fazecast.jSerialComm.SerialPort;
import org.hid4java.HidDevice;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class Device {
    public static final byte CMD_GET_FEATURE = 2;
    public static final byte CMD_GET_STATE = 3;
    public static final byte CMD_VENDOR = 6;
    public static final byte CMD_SET_AXIS_LIMITS = 2;
    public static final byte CMD_SET_AUTO_RECOIL = 3;
    public static final byte CMD_SET_TRIGGER_RATE = 4;
    public static final byte CMD_SET_TRIGGER_HOLD = 5;
    public static final byte CMD_SET_UNIQUE_ID = 6;
    public static final byte CMD_SET_PLAYER_NUMBER = 11;
    public static final byte CMD_SET_RECOIL_STRENGTH = 12;
    public static final byte CMD_SET_INVERT_AXIS = 13;
    public static final byte CMD_EESAVE = 16;
    public static final byte CMD_EELOAD = 17;
    public static final byte CMD_DEFAULT = 18;
    public static final byte CMD_RECOIL = 19;
    public static final String SERIAL_CMD_GET_UNIQUE_ID = "getUniqueId!";
    public static final String FIRMWARE_TYPE = "RKADE-GUN";
    public static final int ESP32_VID = 0x303A;
    public static final int ESP32_PID = 0x8234;
    public static final int ESP32_JTAG_PID = 0x1001;
    private static final Logger logger = Logger.getLogger(Device.class.getName());
    private final HidDevice hidDevice;
    private String name;
    private String firmwareType;
    private String firmwareVersion;
    private SerialPort port;

    public Device(HidDevice hidDevice) {
        this.hidDevice = hidDevice;
        this.name = hidDevice.getProduct();
    }

    public static synchronized String readUniqueId(SerialPort port) {
        try {
            boolean isOpen = port.isOpen();
            if (!isOpen) {
                port.setBaudRate(115200);
                port.setParity(0);
                port.setNumStopBits(1);
                port.setNumDataBits(8);
                isOpen = port.openPort(500);
            }
            if (isOpen) {
                byte[] value = SERIAL_CMD_GET_UNIQUE_ID.getBytes(StandardCharsets.US_ASCII);
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 100);
                int ret = port.writeBytes(value, value.length);
                if (ret > 0) {
                    InputStream is = port.getInputStream();
                    InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
                    BufferedReader bufferedReader = new BufferedReader(streamReader);
                    return bufferedReader.readLine();
                }
                return null;
            }
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        } finally {
            if (port != null && port.isOpen()) {
                port.closePort();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    public synchronized boolean saveSettings() {
        return sendCommand(CMD_EESAVE);
    }

    public synchronized boolean loadDefaults() {
        return sendCommand(CMD_DEFAULT);
    }

    public synchronized boolean loadFromEeprom() {
        return sendCommand(CMD_EELOAD);
    }

    public synchronized boolean setAutoRecoil(boolean state) {
        return sendCommand(CMD_SET_AUTO_RECOIL, state);
    }

    public boolean setInvertAxis(boolean state) {
        return sendCommand(CMD_SET_INVERT_AXIS, state);
    }

    public synchronized boolean setAxisLimits(short xMinValue, short xMaxValue, short yMinValue, short yMaxValue) {
        return sendCommand(CMD_SET_AXIS_LIMITS, xMinValue, xMaxValue, yMinValue, yMaxValue);
    }

    public synchronized boolean doRecoil(boolean state) {
        return sendCommand(CMD_RECOIL, state);
    }

    public synchronized boolean setTriggerRepeatRate(short rate) {
        return sendCommand(CMD_SET_TRIGGER_RATE, rate);
    }

    public synchronized boolean setTriggerHoldTime(short time) {
        return sendCommand(CMD_SET_TRIGGER_HOLD, time);
    }

    public synchronized boolean setPlayerNumber(short number) {
        return sendCommand(CMD_SET_PLAYER_NUMBER, number);
    }

    public synchronized boolean setUniqueId(short id) {
        return sendCommand(CMD_SET_UNIQUE_ID, id);
    }

    public boolean setRecoilStrength(int i) {
        return sendCommand(CMD_SET_RECOIL_STRENGTH, (byte) i);
    }

    private boolean sendCommand(byte command) {
        return sendCommand(command, (short) 0, (short) 0, (short) 0, (short) 0);
    }

    private boolean sendCommand(byte command, boolean state) {
        return sendCommand(command, (short) (state ? 1 : 0), (short) 0, (short) 0, (short) 0);
    }

    private boolean sendCommand(byte command, short arg1) {
        return sendCommand(command, arg1, (short) 0, (short) 0, (short) 0);
    }

    private boolean sendCommand(byte command, short arg1, short arg2, short arg3, short arg4) {
        byte[] data = new byte[10];
        data[0] = Device.CMD_VENDOR;
        data[1] = command;

        data[2] = getFirstByte(arg1);
        data[3] = getSecondByte(arg1);

        data[4] = getFirstByte(arg2);
        data[5] = getSecondByte(arg2);

        data[6] = getFirstByte(arg3);
        data[7] = getSecondByte(arg3);

        data[8] = getFirstByte(arg4);
        data[9] = getSecondByte(arg4);

        int ret = hidDevice.sendFeatureReport(data, Device.CMD_VENDOR);
        if (ret <= 0) {
            logger.severe("Device returned error on setFeatureReport:" + ret);
            return false;
        }
        return true;
    }

    public boolean resetToBootLoader() {
        if (port == null) {
            logger.warning("Com port not set, ensure no other software attached");
            return false;
        }
        if (!port.isOpen()) {
            port.openPort();
        }
        port.setBaudRate(1200);
        port.writeBytes(new byte[1], 1);
        port.flushIOBuffers();
        port.closePort();
        return true;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
    }

    public SerialPort findBootLoaderPort() {
        for (int i = 0; i < 10; i++) {
            sleep(500);
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort port : ports) {
                if (port.getVendorID() == Device.ESP32_VID && port.getProductID() == Device.ESP32_JTAG_PID) {
                    return port;
                }
            }
        }
        return null;
    }

    public HidDevice getHidDevice() {
        return hidDevice;
    }

    private byte getFirstByte(short value) {
        return (byte) (value & 0xff);
    }

    private byte getSecondByte(short value) {
        return (byte) ((value >> 8) & 0xff);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirmwareType() {
        return firmwareType;
    }

    public void setFirmwareType(String firmwareType) {
        this.firmwareType = firmwareType;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public void setPort(SerialPort port) {
        this.port = port;
    }
}
