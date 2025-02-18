package com.rkade;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class FirmwareDialog extends JDialog {
    private final static Logger logger = Logger.getLogger(FirmwareDialog.class.getName());
    private final static String FILE_BOOT_BIN = "boot_app0.bin";
    private final static String FILE_VERSION = "version.txt";
    private final static String FILE_FIRMWARE_BIN = "RailgunFirmware.ino.bin";
    private final static String FILE_FIRMWARE_BOOT = "RailgunFirmware.ino.bootloader.bin";
    private final static String FILE_FIRMWARE_PARTITION = "RailgunFirmware.ino.partitions.bin";
    private final static String FILE_FIRMWARE_URL = "https://github.com/rknabe/RailgunApp/releases/download/Firmware/";
    private final static String DIR_TEMP = System.getProperty("java.io.tmpdir");
    private final static String EXE_ESP_TOOL = "esptool.exe";
    private final static String updateCmd = "\"%s\" --chip esp32s3 --port \"%s\" --baud 921600 --before default_reset --after hard_reset write_flash -z --flash_mode keep --flash_freq keep --flash_size keep 0x0 \"%s\" 0x8000 \"%s\" 0xe000 \"%s\" 0x10000 \"%s\"";
    private final static String bootBinFile = DIR_TEMP + FILE_BOOT_BIN;
    private final static String firmwareBinFile = DIR_TEMP + FILE_FIRMWARE_BIN;
    private final static String firmwareBootFile = DIR_TEMP + FILE_FIRMWARE_BOOT;
    private final static String firmwarePartFile = DIR_TEMP + FILE_FIRMWARE_PARTITION;
    private final Device device;
    private JPanel contentPane;
    private JButton btnCheck;
    private JButton btnClose;
    private JTextArea txtOutput;
    private JButton btnUpdate;
    private JScrollPane scrollPane;

    public FirmwareDialog(Device device) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(btnCheck);
        btnUpdate.setEnabled(false);

        DefaultCaret caret = (DefaultCaret) txtOutput.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        this.device = device;

        btnCheck.addActionListener(_ -> onCheck());

        btnClose.addActionListener(_ -> onClose());

        btnUpdate.addActionListener(_ -> onUpdate());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(_ -> onClose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private Path getExePathFromCurrentDir() {
        Path curDir = FileSystems.getDefault().getPath("").toAbsolutePath();
        return curDir.resolve(EXE_ESP_TOOL);
    }

    private CompletableFuture<Long> downloadToFile(String fileURL, String remoteFilename, String localFilename) throws Exception {
        URL url = new URI(fileURL + remoteFilename).toURL();
        CompletableFuture<Long> completableFuture = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> {
            try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(localFilename);
                 FileChannel fileChannel = fileOutputStream.getChannel()) {

                long size = fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                if (size < 1) {
                    throw new IOException("Download Error");
                }
                completableFuture.complete(size);
            } catch (Exception ex) {
                completableFuture.complete(0L);
            }
            return null;
        });
        return completableFuture;
    }

    private void onCheck() {
        txtOutput.setText("Checking..." + System.lineSeparator());
        try {
            String versionFileName = DIR_TEMP + FILE_VERSION;
            downloadToFile(FILE_FIRMWARE_URL, FILE_VERSION, versionFileName);
            String version = Files.readString(Path.of(versionFileName)).trim();
            txtOutput.append("Remote version:" + version + System.lineSeparator());
            txtOutput.append("Device version:" + device.getFirmwareVersion() + System.lineSeparator());
            if (device.getFirmwareVersion().equals(version)) {
                txtOutput.append("Device is up-to-date" + System.lineSeparator());
            }
            btnUpdate.setEnabled(true);
        } catch (Exception ex) {
            txtOutput.append(ex.toString());
            logger.warning(ex.toString());
        }
    }

    private void onClose() {
        dispose();
    }

    private void onUpdate() {
        try {
            txtOutput.append("Downloading Firmware Bin..." + System.lineSeparator());
            CompletableFuture<Long> download2 = downloadToFile(FILE_FIRMWARE_URL, FILE_FIRMWARE_BIN, firmwareBinFile);
            txtOutput.append("Downloading Boot Bin..." + System.lineSeparator());
            CompletableFuture<Long> download1 = downloadToFile(FILE_FIRMWARE_URL, FILE_BOOT_BIN, bootBinFile);
            txtOutput.append("Downloading Firmware Boot..." + System.lineSeparator());
            CompletableFuture<Long> download3 = downloadToFile(FILE_FIRMWARE_URL, FILE_FIRMWARE_BOOT, firmwareBootFile);
            txtOutput.append("Downloading Firmware Partitions..." + System.lineSeparator());
            CompletableFuture<Long> download4 = downloadToFile(FILE_FIRMWARE_URL, FILE_FIRMWARE_PARTITION, firmwarePartFile);

            download1.thenApply(this::bootBinComplete);
            download2.thenApply(this::firmwareBinComplete);
            download3.thenApply(this::firmwareBootComplete);
            download4.thenApply(this::firmwarePartitionComplete);

            CompletableFuture.allOf(download1, download2, download3, download4).thenApplyAsync(this::applyFirmware);

        } catch (Exception ex) {
            txtOutput.append(ex.toString());
            logger.warning(ex.toString());
        }
    }

    private boolean applyFirmware(Void unused) {
        try {
            boolean reset = device.resetToBootLoader();
            if (reset) {
                txtOutput.append("Waiting for Upload port" + System.lineSeparator());
                Path exePath = getExePathFromCurrentDir();
                SerialPort port = device.findBootLoaderPort();
                txtOutput.append("Uploading firmware..." + System.lineSeparator());
                if (port != null) {
                    String portName = port.getSystemPortName();
                    String cmd = String.format(updateCmd, exePath, portName, firmwareBootFile, firmwarePartFile, bootBinFile, firmwareBinFile);
                    Runtime rt = Runtime.getRuntime();
                    String[] commands = {cmd};
                    Process proc = rt.exec(commands);
                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                    String s;
                    while ((s = stdInput.readLine()) != null) {
                        txtOutput.append(s + System.lineSeparator());
                    }
                    while ((s = stdError.readLine()) != null) {
                        txtOutput.append(s + System.lineSeparator());
                    }
                    return true;
                } else {
                    txtOutput.append("Bootloader port not found");
                    return false;
                }
            } else {
                txtOutput.append("Unable to reset to Bootloader mode, other software attached?");
                return false;
            }
        } catch (Exception ex) {
            logger.warning(ex.toString());
            return false;
        }
    }

    private boolean bootBinComplete(Long size) {
        txtOutput.append("Boot Bin downloaded:" + size + " bytes" + System.lineSeparator());
        return true;
    }

    private boolean firmwareBinComplete(Long size) {
        txtOutput.append("Firmware Bin downloaded:" + size + " bytes" + System.lineSeparator());
        return true;
    }

    private boolean firmwareBootComplete(Long size) {
        txtOutput.append("Firmware Boot downloaded:" + size + " bytes" + System.lineSeparator());
        return true;
    }

    private boolean firmwarePartitionComplete(Long size) {
        txtOutput.append("Firmware Partition downloaded:" + size + " bytes" + System.lineSeparator());
        return true;
    }
}
