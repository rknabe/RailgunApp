package com.rkade;

import com.fazecast.jSerialComm.SerialPort;
import org.hid4java.*;
import org.hid4java.event.HidServicesEvent;

import java.util.*;
import java.util.logging.Logger;

public final class DeviceManager implements HidServicesListener {
    private final static Logger logger = Logger.getLogger(DeviceManager.class.getName());
    private final static List<DeviceListener> deviceListeners = Collections.synchronizedList(new ArrayList<>());
    private final static Map<String, Device> deviceMap = Collections.synchronizedMap(new HashMap<>());
    private final static Map<Device, SettingsDataReport> deviceSettings = Collections.synchronizedMap(new HashMap<>());
    private final static List<HidDevice> devices = Collections.synchronizedList(new ArrayList<>());
    private final static Random random = new Random();
    private static HidDevice openedDevice = null;
    private InputReportTask inputReportTask;

    public DeviceManager(DeviceListener listener) {
        addDeviceListener(listener);
        SerialPort.autoCleanupAtShutdown();
        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
        hidServicesSpecification.setAutoStart(false);

        // Get HID services using custom specification
        HidServices hidServices = HidManager.getHidServices(hidServicesSpecification);
        hidServices.addHidServicesListener(this);

        // Manually start the services to get attachment event
        hidServices.start();
    }

    private Device getDevice(HidDevice hidDevice) {
        return deviceMap.computeIfAbsent(hidDevice.getPath(), _ -> new Device(hidDevice));
    }

    @Override
    public void hidDeviceAttached(HidServicesEvent hidServicesEvent) {
        HidDevice hidDevice = hidServicesEvent.getHidDevice();
        if (hidDevice.getVendorId() == Device.ESP32_VID && hidDevice.getProductId() == Device.ESP32_PID) {
            boolean open = hidDevice.open();
            if (open) {
                byte[] reportData = new byte[64];
                sleep(50);
                for (int i = 0; i < 250; i++) {
                    hidDevice.getFeatureReport(reportData, Device.CMD_VENDOR);
                    sleep(20);
                    byte[] data = hidDevice.readAll(6);
                    if (data.length > 0) {
                        boolean handled = handleInputReport(hidDevice, data);
                        if (handled) {
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void hidDeviceDetached(HidServicesEvent hidServicesEvent) {
        HidDevice hidDevice = hidServicesEvent.getHidDevice();
        Device device = getDevice(hidDevice);
        if (device != null) {
            notifyListenersDeviceDetached(device);
            if (hidDevice == openedDevice) {
                closeDevice(openedDevice);
                notifyListenersDeviceDisconnected(device);
            }
            deviceSettings.remove(device);
        }
        devices.remove(hidDevice);
        deviceMap.remove(hidDevice.getPath());
    }

    @Override
    public void hidFailure(HidServicesEvent hidServicesEvent) {
        logger.warning("hidFailure:" + hidServicesEvent);
    }

    @Override
    public void hidDataReceived(HidServicesEvent hidServicesEvent) {
        onInputReport(hidServicesEvent.getHidDevice(), hidServicesEvent.getDataReceived());
    }

    public void onInputReport(HidDevice hidDevice, byte[] data) {
        handleInputReport(hidDevice, data);
    }

    private boolean handleInputReport(HidDevice hidDevice, byte[] data) {
        byte id = data[0];
        if (id == Device.CMD_VENDOR) {
            byte reportType = data[1];
            List<DataReport> reports = DataReportFactory.create(reportType, data);
            Device device = getDevice(hidDevice);

            for (DataReport report : reports) {
                if (report instanceof SettingsDataReport settings) {
                    SettingsDataReport prevSettings = deviceSettings.get(device);
                    if (prevSettings != null) {//not first pass
                        notifyListenersDeviceUpdated(device, null, settings);
                        continue;
                    }
                    deviceSettings.put(device, settings);
                    if (Device.FIRMWARE_TYPE.equalsIgnoreCase(settings.getDeviceType())) {
                        device.setName(settings.getDeviceType());
                        short uniqueId = (short) random.nextInt(Short.MAX_VALUE + 1);
                        boolean ret = device.setUniqueId(uniqueId);
                        sleep(20);
                        if (ret) {
                            SerialPort port = findMatchingCommPort(uniqueId);
                            if (port != null) {
                                device.setName(settings.getDeviceType() + " (" + port.getSystemPortName() + ")");
                                device.setPort(port);
                            }
                        }
                        device.setFirmwareType(settings.getDeviceType());
                        device.setFirmwareVersion(settings.getDeviceVersion());
                        notifyListenersDeviceAttached(device);
                        devices.add(hidDevice);
                    }
                } else {
                    notifyListenersDeviceUpdated(getDevice(hidDevice), null, report);
                }
            }
            return true;
        }
        return false;
    }

    private void notifyListenersDeviceAttached(Device device) {
        for (DeviceListener deviceListener : deviceListeners) {
            deviceListener.deviceAttached(device);
        }
    }

    private void notifyListenersDeviceDetached(Device device) {
        for (DeviceListener deviceListener : deviceListeners) {
            deviceListener.deviceDetached(device);
        }
    }

    private void notifyListenersDeviceConnected(Device device) {
        for (DeviceListener deviceListener : deviceListeners) {
            deviceListener.deviceConnected(device);
        }
    }

    private void notifyListenersDeviceDisconnected(Device device) {
        for (DeviceListener deviceListener : deviceListeners) {
            deviceListener.deviceDisconnected(device);
        }
    }

    private void notifyListenersDeviceUpdated(Device device, String status, DataReport report) {
        if (report != null /*&& openedDevice != null && device.getHidDevice() == openedDevice*/) {
            for (DeviceListener deviceListener : deviceListeners) {
                deviceListener.deviceUpdated(device, status, report);
            }
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
    }

    public void addDeviceListener(DeviceListener deviceListener) {
        deviceListeners.add(deviceListener);
    }

    private void closeDevice(HidDevice hidDevice) {
        try {
            hidDevice.close();
            inputReportTask.cancel();
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
    }

    private SerialPort findMatchingCommPort(short uniqueId) {
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            if (port.getVendorID() == Device.ESP32_VID && port.getProductID() == Device.ESP32_PID) {
                String uniqueIdStr = Device.readUniqueId(port);
                if (uniqueIdStr != null) try {
                    short id = Short.parseShort(uniqueIdStr);
                    if (id == uniqueId) {
                        return port;
                    }
                } catch (Exception ex) {
                    logger.warning(ex.getMessage());
                }
            }
        }
        return null;
    }

    public Device getConnectedDevice() {
        if (openedDevice != null && !openedDevice.isClosed()) {
            return getDevice(openedDevice);
        }
        return null;
    }

    public boolean connectDevice(Device device) {
        if (device != null) {
            if (openedDevice == device.getHidDevice()) {
                //do not reopen already open
                return true;
            }
            if (openedDevice != null) {
                closeDevice(openedDevice);
            }
            openedDevice = device.getHidDevice();
            openedDevice.open();
            notifyListenersDeviceUpdated(device, "Connected", deviceSettings.get(device));
            notifyListenersDeviceConnected(device);

            Timer timer = new Timer();
            if (inputReportTask != null) {
                inputReportTask.cancel();
            }
            inputReportTask = new InputReportTask(openedDevice);
            timer.scheduleAtFixedRate(inputReportTask, 50, 5);

            return true;
        }
        return false;
    }

    private class InputReportTask extends TimerTask {
        private final HidDevice hidDevice;

        InputReportTask(HidDevice hidDevice) {
            this.hidDevice = hidDevice;
        }

        @Override
        public void run() {
            if (hidDevice.open()) {
                byte[] data = new byte[64];
                int bytes = hidDevice.read(data);
                if (bytes > 0) {
                    onInputReport(hidDevice, data);
                }
            } else {
                this.cancel();
            }
        }
    }
}