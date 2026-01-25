package com.rkade;

import java.util.Vector;

public enum ButtonAction {
    NONE(0),
    PAUSE(1),
    SHUTDOWN(2),
    ESCAPE(3),
    RECOIL_TOGGLE(4);

    private int code = 255;

    ButtonAction(int i) {
        this.code = i;
    }

    public static ButtonAction from(int code) {
        for (ButtonAction action : ButtonAction.values()) {
            if (action.getCode() == code) {
                return action;
            }
        }
        return NONE;
    }

    public int getCode() {
        return code;
    }

    public static Vector<String> getStringValues() {
        Vector<String> values = new Vector<>();
        for (ButtonAction action : ButtonAction.values()) {
            values.add(action.name());
        }
        return values;
    }
}
