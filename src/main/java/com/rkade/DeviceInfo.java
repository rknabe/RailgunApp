package com.rkade;

import org.hid4java.HidDevice;

import java.util.Objects;

public final class DeviceInfo {
    private final String path;

    public DeviceInfo(HidDevice hidDevice) {
        this.path = hidDevice.getPath().trim().toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DeviceInfo that = (DeviceInfo) o;
        return Objects.equals(path, that.path);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }
}
