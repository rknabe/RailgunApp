package com.rkade;

import java.nio.ByteBuffer;

public final class ButtonsDataReport extends DataReport {
    private final int buttonsState;

    public ButtonsDataReport(byte reportType, ByteBuffer buffer) {
        super(reportType);
        buttonsState = buffer.getInt();
    }

    public int getButtonsState() {
        return buttonsState;
    }
}
