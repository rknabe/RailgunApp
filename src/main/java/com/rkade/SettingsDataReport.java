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
    private final ButtonAction button6Action;
    private final ButtonAction button7Action;
    private final ButtonAction button8Action;
    private final ButtonAction button9Action;

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
  bool recoilSwitchToggle;  //is toggle or momentary switch
  int8_t gunLightType;
  int8_t btn6Action;
  int8_t btn7Action;
  int8_t btn8Action;
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
        if (buffer.hasRemaining()) {
            button6Action = ButtonAction.from(buffer.get());
            button7Action = ButtonAction.from(buffer.get());
            button8Action = ButtonAction.from(buffer.get());
            button9Action = ButtonAction.from(buffer.get());
        } else {
            button6Action = ButtonAction.ESCAPE;
            button7Action = ButtonAction.SHUTDOWN;
            button8Action = ButtonAction.RECOIL_TOGGLE;
            button9Action = ButtonAction.PAUSE;
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

    public ButtonAction getButton6Action() {
        return button6Action;
    }

    public ButtonAction getButton7Action() {
        return button7Action;
    }

    public ButtonAction getButton8Action() {
        return button8Action;
    }

    public ButtonAction getButton9Action() {
        return button9Action;
    }
}
