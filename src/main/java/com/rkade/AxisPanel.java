package com.rkade;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AxisPanel extends JPanel implements DeviceListener, ActionListener, ChangeListener {
    private final Image target;
    private short xAxisMinimum = Device.AXIS_MIN;
    private short xAxisMaximum = Device.AXIS_MAX;
    private short yAxisMinimum = Device.AXIS_MIN;
    private short yAxisMaximum = Device.AXIS_MAX;
    private short x = 0, y = 0;
    private boolean isCalibrating = false;
    private short xMin = Device.AXIS_MIN;
    private short xMax = Device.AXIS_MAX;
    private short yMin = Device.AXIS_MIN;
    private short yMax = Device.AXIS_MAX;
    private int targetWidth = 0;
    private int targetHeight = 0;

    public AxisPanel() {
        target = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("crosshair.png"));
    }

    private void setAxisValues(short x, short y) {
        this.x = x;
        this.y = y;
        if (isCalibrating) {
            xMax = (short) Math.max(x, xMax);
            xMin = (short) Math.min(x, xMin);
            yMax = (short) Math.max(y, yMax);
            yMin = (short) Math.min(y, yMin);
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    private void updateControls(AxisDataReport axisDataReport) {
        //System.out.println("x:" + axisDataReport.getX() + " y:" + axisDataReport.getY());
        setAxisValues(axisDataReport.getX(), axisDataReport.getY());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int h = getHeight();
        int w = getWidth();
        if (targetWidth == 0 || targetHeight == 0) {
            targetWidth = target.getWidth(null);
            targetHeight = target.getHeight(null);
        }
        int nx = normalize(x, Device.AXIS_MIN, Device.AXIS_MAX, -targetWidth / 2, w - targetWidth / 2);
        int ny = normalize(y, Device.AXIS_MIN, Device.AXIS_MAX, -targetWidth / 2, h - targetHeight / 2);
        g.drawImage(target, nx, ny, null);
    }

    private int map(int value, int in_min, int in_max, int out_min, int out_max) {
        return (int) Math.round(((double) value - in_min) * (double) (out_max - out_min) / (double) (in_max - in_min) + out_min);
    }

    public int normalize(int value, int physicalMinimum, int physicalMaximum, int logicalMinimum, int logicalMaximum) {
        int realMinimum = Math.min(physicalMinimum, physicalMaximum);
        int realMaximum = Math.max(physicalMinimum, physicalMaximum);

        if (value < realMinimum) {
            value = realMinimum;
        }
        if (value > realMaximum) {
            value = realMaximum;
        }

        if (physicalMinimum > physicalMaximum) {
            // Values go from a larger number to a smaller number (e.g. 1024 to 0)
            value = realMaximum - value + realMinimum;
        }
        return map(value, realMinimum, realMaximum, logicalMinimum, logicalMaximum);
    }

    @Override
    public void deviceAttached(Device device) {
    }

    @Override
    public void deviceDetached(Device device) {
    }

    @Override
    public void deviceConnected(Device device) {
    }

    @Override
    public void deviceDisconnected(Device device) {
    }

    @Override
    public void deviceUpdated(Device device, String status, DataReport report) {
        if (report instanceof AxisDataReport axisDataReport) {
            updateControls(axisDataReport);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public void stateChanged(ChangeEvent e) {
    }

    public void setCalibrating(boolean calibrating) {
        isCalibrating = calibrating;
        if (isCalibrating) {
            xMin = Device.AXIS_MAX;
            xMax = Device.AXIS_MIN;
            yMin = Device.AXIS_MAX;
            yMax = Device.AXIS_MIN;
        } else {
            xAxisMinimum = xMin;
            xAxisMaximum = xMax;
            yAxisMinimum = yMin;
            yAxisMaximum = yMax;
        }
    }

    public short getXAxisMinimum() {
        return xAxisMinimum;
    }

    public void setXAxisMinimum(short xAxisMinimum) {
        this.xAxisMinimum = xAxisMinimum;
    }

    public short getXAxisMaximum() {
        return xAxisMaximum;
    }

    public void setXAxisMaximum(short xAxisMaximum) {
        this.xAxisMaximum = xAxisMaximum;
    }

    public short getYAxisMinimum() {
        return yAxisMinimum;
    }

    public void setYAxisMinimum(short yAxisMinimum) {
        this.yAxisMinimum = yAxisMinimum;
    }

    public short getYAxisMaximum() {
        return yAxisMaximum;
    }

    public void setYAxisMaximum(short yAxisMaximum) {
        this.yAxisMaximum = yAxisMaximum;
    }
}
