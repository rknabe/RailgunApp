package com.rkade;

import java.util.Vector;

public enum BarrelLiteColor {
    RED(0),
    GREEN(1),
    BLUE(2),
    YELLOW(3),
    PURPLE(4),
    PINK(5),
    ORANGE(6),
    BROWN(7);

    private int code = 255;

    BarrelLiteColor(int i) {
        this.code = i;
    }

    public static BarrelLiteColor from(int code) {
        for (BarrelLiteColor color : BarrelLiteColor.values()) {
            if (color.getCode() == code) {
                return color;
            }
        }
        return RED;
    }

    public int getCode() {
        return code;
    }

    public static Vector<String> getStringValues() {
        Vector<String> values = new Vector<>();
        for (BarrelLiteColor color : BarrelLiteColor.values()) {
            values.add(color.name());
        }
        return values;
    }
}
