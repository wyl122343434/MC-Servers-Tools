package com.mcmanager.util;

import java.awt.*;
import java.io.*;
import java.util.Properties;

public class ThemeManager {
    private static ThemeManager instance;
    private Properties themeProps;
    private static final String THEME_FILE = System.getProperty("user.home") + File.separator + ".mcmanager" + File.separator + "theme.properties";

    // Preset themes
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_IOS = "ios";

    private ThemeManager() {
        themeProps = new Properties();
        load();
    }

    public static ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    public void load() {
        try {
            File f = new File(THEME_FILE);
            if (f.exists()) {
                themeProps.load(new FileInputStream(f));
            } else {
                setPreset(THEME_IOS);
            }
        } catch (Exception e) {
            setPreset(THEME_IOS);
        }
    }

    public void save() {
        try {
            new File(THEME_FILE).getParentFile().mkdirs();
            themeProps.store(new FileOutputStream(THEME_FILE), "MC-Servers-Tools Theme");
        } catch (Exception e) {}
    }

    public void setPreset(String preset) {
        themeProps.setProperty("theme.preset", preset);
        switch (preset) {
            case THEME_DARK:
                themeProps.setProperty("bg.primary", "#1E1E2E");
                themeProps.setProperty("bg.secondary", "#2A2A3C");
                themeProps.setProperty("bg.card", "#33334D");
                themeProps.setProperty("text.primary", "#E2E2F0");
                themeProps.setProperty("text.secondary", "#A0A0B4");
                themeProps.setProperty("accent", "#6366F1");
                themeProps.setProperty("border", "#444466");
                themeProps.setProperty("success", "#4ADE80");
                themeProps.setProperty("danger", "#F87171");
                themeProps.setProperty("warning", "#FBBF24");
                themeProps.setProperty("radius", "12");
                break;
            case THEME_LIGHT:
                themeProps.setProperty("bg.primary", "#F5F5F7");
                themeProps.setProperty("bg.secondary", "#FFFFFF");
                themeProps.setProperty("bg.card", "#FFFFFF");
                themeProps.setProperty("text.primary", "#1D1D1F");
                themeProps.setProperty("text.secondary", "#6E6E73");
                themeProps.setProperty("accent", "#007AFF");
                themeProps.setProperty("border", "#D2D2D7");
                themeProps.setProperty("success", "#34C759");
                themeProps.setProperty("danger", "#FF3B30");
                themeProps.setProperty("warning", "#FF9500");
                themeProps.setProperty("radius", "12");
                break;
            case THEME_IOS:
            default:
                themeProps.setProperty("bg.primary", "#F2F2F7");
                themeProps.setProperty("bg.secondary", "#FFFFFF");
                themeProps.setProperty("bg.card", "#FFFFFF");
                themeProps.setProperty("text.primary", "#000000");
                themeProps.setProperty("text.secondary", "#8E8E93");
                themeProps.setProperty("accent", "#007AFF");
                themeProps.setProperty("border", "#C6C6C8");
                themeProps.setProperty("success", "#34C759");
                themeProps.setProperty("danger", "#FF3B30");
                themeProps.setProperty("warning", "#FF9500");
                themeProps.setProperty("radius", "14");
                break;
        }
        save();
    }

    public String getPreset() {
        return themeProps.getProperty("theme.preset", THEME_IOS);
    }

    public Color getColor(String key) {
        String hex = themeProps.getProperty(key, "#000000");
        return Color.decode(hex);
    }

    public String getHex(String key) {
        return themeProps.getProperty(key, "#000000");
    }

    public void setColor(String key, Color color) {
        themeProps.setProperty(key, String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()));
    }

    public void setColor(String key, String hex) {
        themeProps.setProperty(key, hex);
    }

    public int getRadius() {
        return Integer.parseInt(themeProps.getProperty("radius", "12"));
    }

    public void setRadius(int radius) {
        themeProps.setProperty("radius", String.valueOf(radius));
    }

    // Convenience getters
    public Color bgPrimary() { return getColor("bg.primary"); }
    public Color bgSecondary() { return getColor("bg.secondary"); }
    public Color bgCard() { return getColor("bg.card"); }
    public Color textPrimary() { return getColor("text.primary"); }
    public Color textSecondary() { return getColor("text.secondary"); }
    public Color accent() { return getColor("accent"); }
    public Color border() { return getColor("border"); }
    public Color success() { return getColor("success"); }
    public Color danger() { return getColor("danger"); }
    public Color warning() { return getColor("warning"); }

    public Properties getProperties() { return themeProps; }

    // Background image support
    public String getBackgroundImage() {
        return themeProps.getProperty("bg.image", "");
    }

    public void setBackgroundImage(String path) {
        themeProps.setProperty("bg.image", path != null ? path : "");
        save();
    }

    public float getBackgroundOpacity() {
        return Float.parseFloat(themeProps.getProperty("bg.opacity", "0.3"));
    }

    public void setBackgroundOpacity(float opacity) {
        themeProps.setProperty("bg.opacity", String.valueOf(opacity));
        save();
    }

    // Content panel opacity (for seeing background image through panels)
    public float getContentOpacity() {
        return Float.parseFloat(themeProps.getProperty("content.opacity", "0.85"));
    }

    public void setContentOpacity(float opacity) {
        themeProps.setProperty("content.opacity", String.valueOf(opacity));
        save();
    }

    // Tab font size
    public int getTabFontSize() {
        return Integer.parseInt(themeProps.getProperty("tab.fontSize", "13"));
    }

    public void setTabFontSize(int size) {
        themeProps.setProperty("tab.fontSize", String.valueOf(size));
        save();
    }
}
