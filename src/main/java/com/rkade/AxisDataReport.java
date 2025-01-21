package com.rkade;

import java.nio.ByteBuffer;

public final class AxisDataReport extends DataReport {
    private final byte x;
    private final byte y;

    public AxisDataReport(byte reportType, ByteBuffer buffer) {
        super(reportType);
        x = buffer.get();
        y = buffer.get();
        buffer.get();
        buffer.get();
        buffer.get();
        buffer.get();
        buffer.get();
    }

    public short getX() {
        return x;
    }

    public short getY() {
        return y;
    }
}
