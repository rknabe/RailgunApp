package com.rkade;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public abstract class DataReportFactory {

    public static List<DataReport> create(byte reportType, byte[] data) {
        List<DataReport> reports = new ArrayList<>();
        if (reportType == Device.CMD_GET_STATE || reportType == Device.CMD_GET_FEATURE) {
            ByteBuffer buffer = ByteBuffer.allocate(data.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(data);
            buffer.rewind();
            byte dataType = buffer.get(); //read past the data type first byte
            byte type = buffer.get(); //read past the data type first byte

            if (reportType == Device.CMD_GET_FEATURE) {
                reports.add(new SettingsDataReport(reportType, buffer));
            } else {
                //these have to go in the correct order because they read sequentially from same data structure
                reports.add(new AxisDataReport(reportType, buffer));
                reports.add(new ButtonsDataReport(reportType, buffer));
            }
        }
        return reports;
    }
}
