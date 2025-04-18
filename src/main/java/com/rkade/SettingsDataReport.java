package com.rkade;

import java.nio.ByteBuffer;

public final class SettingsDataReport extends DataReport {
    private final String deviceType;
    private final String deviceVersion;
    private final short xAxisMinimum;
    private final short xAxisMaximum;
    private final short yAxisMinimum;
    private final short yAxisMaximum;
    private final boolean autoRecoil;
    private final short triggerRepeatDelay;
    private final short triggerHoldTime;
    private final short playerNumber;
    private final int recoilStrength;
    private final boolean useDisplay;
    private final boolean invertAxis;
    private final int gunLightType;
    private final boolean recoilSwitchIsToggle;

    public SettingsDataReport(byte reportType, ByteBuffer buffer) {
        super(reportType);
        /*
  char id[10];
  char ver[6];
  int8_t xAxisMinimum;
  int8_t xAxisMaximum;
  int8_t yAxisMinimum;
  int8_t yAxisMaximum;
  int16_t triggerRepeatRate;
  int16_t triggerHoldTime;
  int8_t playerNumber;
  int8_t recoilStrength;
  bool autoRecoil;
  bool useDisplay;
  bool invertAxis;
  int8_t gunLightType;
        */
        deviceType = getString(buffer, 10).trim();
        deviceVersion = getString(buffer, 6).trim();
        xAxisMinimum = buffer.get();
        xAxisMaximum = buffer.get();
        yAxisMinimum = buffer.get();
        yAxisMaximum = buffer.get();
        triggerRepeatDelay = buffer.getShort();
        triggerHoldTime = buffer.getShort();
        playerNumber = buffer.get();
        recoilStrength = (buffer.get() & 0xFF);
        autoRecoil = buffer.get() > 0;
        useDisplay = buffer.get() > 0;
        if (buffer.hasRemaining()) {
            invertAxis = buffer.get() > 0;
        } else {
            invertAxis = false;
        }
        if (buffer.hasRemaining()) {
            recoilSwitchIsToggle = buffer.get() > 0;
        } else {
            recoilSwitchIsToggle = false;
        }
        if (buffer.hasRemaining()) {
            gunLightType = buffer.get();
        } else {
            gunLightType = Device.GUN_LIGHT_TYPE_SINGLE;
        }
    }

    public short getTriggerHoldTime() {
        return triggerHoldTime;
    }

    public short getTriggerRepeatDelay() {
        return triggerRepeatDelay;
    }

    public boolean isAutoRecoil() {
        return autoRecoil;
    }

    public short getXAxisMinimum() {
        return xAxisMinimum;
    }

    public short getXAxisMaximum() {
        return xAxisMaximum;
    }

    public short getYAxisMinimum() {
        return yAxisMinimum;
    }

    public short getYAxisMaximum() {
        return yAxisMaximum;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getDeviceVersion() {
        return deviceVersion;
    }

    public short getPlayerNumber() {
        return playerNumber;
    }

    public boolean isUseDisplay() {
        return useDisplay;
    }

    public int getRecoilStrength() {
        return recoilStrength;
    }

    public boolean isInvertAxis() {
        return invertAxis;
    }

    public int getGunLightType() {
        return gunLightType;
    }

    public boolean isRecoilSwitchIsToggle() {
        return recoilSwitchIsToggle;
    }
}
