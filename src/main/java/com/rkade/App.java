package com.rkade;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class App {

    public static void main(String[] args) {
        System.setProperty("fazecast.jSerialComm.appid", "com.rkade.RKadeGunConfig");

        try {
            InputStream is = App.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration(is);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Logger logger = Logger.getLogger(App.class.getName());

        SwingUtilities.invokeLater(() -> {
            try {
                //com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMonokaiProIJTheme.setup();
                //com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme.setup();
                com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme.setup();
                JFrame frame = new JFrame("RKADE Gun Config");
                MainForm mainForm = new MainForm();
                frame.setContentPane(mainForm.getRootComponent());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                Image icon = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("crosshair.png"));
                frame.setIconImage(icon);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                mainForm.setDeviceManager(new DeviceManager(mainForm));
            } catch (Exception ex) {
                logger.warning(ex.getMessage());
            }
        });
    }
}
