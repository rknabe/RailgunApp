package com.rkade;

public interface DeviceListener {
    void deviceAttached(Device device);

    void deviceDetached(Device device);

    void deviceConnected(Device device);

    void deviceDisconnected(Device device);

    void deviceUpdated(Device device, String status, DataReport report);
}
