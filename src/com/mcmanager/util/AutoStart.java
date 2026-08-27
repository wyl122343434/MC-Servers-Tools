package com.mcmanager.util;

import java.io.*;
import java.nio.file.*;

public class AutoStart {
    private static final String APP_NAME = "MCServerManager";

    public static boolean isEnabled() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                return checkWindows();
            } else if (os.contains("mac")) {
                return checkMac();
            } else {
                return checkLinux();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean setEnabled(boolean enable) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                return enable ? enableWindows() : disableWindows();
            } else if (os.contains("mac")) {
                return enable ? enableMac() : disableMac();
            } else {
                return enable ? enableLinux() : disableLinux();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getJarPath() {
        try {
            return new File(AutoStart.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Exception e) {
            return "MCServerManager.jar";
        }
    }

    private static String getJavaPath() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator +
            (System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
    }

    // Windows: use startup folder shortcut
    private static boolean checkWindows() {
        String startup = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";
        return new File(startup + APP_NAME + ".bat").exists();
    }

    private static boolean enableWindows() throws Exception {
        String startup = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";
        new File(startup).mkdirs();
        String bat = "@echo off\r\n" +
            "cd /d \"" + new File(getJarPath()).getParent() + "\"\r\n" +
            "\"" + getJavaPath() + "\" -jar \"" + getJarPath() + "\"\r\n";
        Files.write(Paths.get(startup + APP_NAME + ".bat"), bat.getBytes("GBK"));
        return true;
    }

    private static boolean disableWindows() {
        String startup = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";
        File f = new File(startup + APP_NAME + ".bat");
        return f.delete() || !f.exists();
    }

    // Linux: .desktop file in autostart
    private static boolean checkLinux() {
        String home = System.getProperty("user.home");
        return new File(home + "/.config/autostart/" + APP_NAME + ".desktop").exists();
    }

    private static boolean enableLinux() throws Exception {
        String home = System.getProperty("user.home");
        new File(home + "/.config/autostart").mkdirs();
        String desktop = "[Desktop Entry]\n" +
            "Type=Application\n" +
            "Name=MC Server Manager\n" +
            "Exec=" + getJavaPath() + " -jar " + getJarPath() + "\n" +
            "Hidden=false\n" +
            "NoDisplay=false\n" +
            "X-GNOME-Autostart-enabled=true\n";
        Files.write(Paths.get(home + "/.config/autostart/" + APP_NAME + ".desktop"), desktop.getBytes());
        return true;
    }

    private static boolean disableLinux() {
        String home = System.getProperty("user.home");
        File f = new File(home + "/.config/autostart/" + APP_NAME + ".desktop");
        return f.delete() || !f.exists();
    }

    // macOS: LaunchAgent
    private static boolean checkMac() {
        String home = System.getProperty("user.home");
        return new File(home + "/Library/LaunchAgents/com.mcmanager.plist").exists();
    }

    private static boolean enableMac() throws Exception {
        String home = System.getProperty("user.home");
        new File(home + "/Library/LaunchAgents").mkdirs();
        String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
            "<plist version=\"1.0\">\n<dict>\n" +
            "<key>Label</key><string>com.mcmanager</string>\n" +
            "<key>ProgramArguments</key>\n<array>\n" +
            "<string>" + getJavaPath() + "</string>\n" +
            "<string>-jar</string>\n" +
            "<string>" + getJarPath() + "</string>\n" +
            "</array>\n" +
            "<key>RunAtLoad</key><true/>\n" +
            "</dict>\n</plist>\n";
        Files.write(Paths.get(home + "/Library/LaunchAgents/com.mcmanager.plist"), plist.getBytes());
        Runtime.getRuntime().exec("launchctl load " + home + "/Library/LaunchAgents/com.mcmanager.plist");
        return true;
    }

    private static boolean disableMac() throws Exception {
        String home = System.getProperty("user.home");
        File f = new File(home + "/Library/LaunchAgents/com.mcmanager.plist");
        if (f.exists()) {
            Runtime.getRuntime().exec("launchctl unload " + f.getAbsolutePath());
            f.delete();
        }
        return true;
    }
}
