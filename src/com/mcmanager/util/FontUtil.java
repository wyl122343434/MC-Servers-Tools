package com.mcmanager.util;

import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class FontUtil {
    private static String preferredFont = null;
    private static boolean initialized = false;
    private static Font customFont = null;
    private static final String BUILTIN_FONT_PATH = "/fonts/wqy-microhei.ttc";

    private static final List<String> PREFERRED_FONTS = Arrays.asList(
        "微软雅黑", "Microsoft YaHei", "Microsoft YaHei UI",
        "思源黑体", "Noto Sans CJK SC", "Noto Sans SC", "Noto Sans CJK",
        "文泉驿微米黑", "WenQuanYi Micro Hei",
        "文泉驿正黑", "WenQuanYi Zen Hei",
        "宋体", "SimSun", "黑体", "SimHei", "楷体", "KaiTi",
        "PingFang SC", "PingFang TC", "Hiragino Sans GB",
        "Droid Sans Fallback", "Droid Sans", "Roboto",
        "SansSerif", "Dialog"
    );

    // Common Android/Linux font file paths
    private static final String[] FONT_FILE_PATHS = {
        "/system/fonts/NotoSansCJK-Regular.ttc",
        "/system/fonts/NotoSansSC-Regular.otf",
        "/system/fonts/DroidSansFallback.ttf",
        "/system/fonts/SourceHanSansCN-Regular.otf",
        "/system/fonts/MTLmr3m.ttf",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/wqy-microhei/wqy-microhei.ttc",
        "/usr/share/fonts/wqy-zenhei/wqy-zenhei.ttc",
        "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
        "/Library/Fonts/PingFang.ttc",
        "/System/Library/Fonts/PingFang.ttc",
        "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/msyh.ttf",
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/simsun.ttc"
    };

    public static void init() {
        if (initialized) return;
        initialized = true;

        // First priority: load built-in font from JAR resource
        if (loadBuiltinFont()) {
            setUIFonts();
            return;
        }

        // Second: try to load font from file (works on Android)
        loadFontFromFile();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        if (customFont != null) {
            preferredFont = customFont.getFamily();
        } else {
            for (String preferred : PREFERRED_FONTS) {
                for (String available : availableFonts) {
                    if (available.equalsIgnoreCase(preferred) ||
                        available.toLowerCase().contains(preferred.toLowerCase())) {
                        preferredFont = available;
                        break;
                    }
                }
                if (preferredFont != null) break;
            }
        }

        if (preferredFont == null) preferredFont = Font.SANS_SERIF;
        setUIFonts();
    }

    private static boolean loadBuiltinFont() {
        try {
            InputStream is = FontUtil.class.getResourceAsStream(BUILTIN_FONT_PATH);
            if (is != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                customFont = font;
                preferredFont = font.getFamily();
                is.close();
                return true;
            }
        } catch (Exception e) {
            // Built-in font not available, fall through
        }
        return false;
    }

    private static void loadFontFromFile() {
        for (String path : FONT_FILE_PATHS) {
            File fontFile = new File(path);
            if (fontFile.exists() && fontFile.canRead()) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    ge.registerFont(font);
                    customFont = font;
                    preferredFont = font.getFamily();
                    return;
                } catch (Exception e) {
                    // Try next font file
                }
            }
        }
        // Also check user home directory for fonts
        String userHome = System.getProperty("user.home");
        String[] userFontDirs = {"/.fonts", "/.local/share/fonts", "/Library/Fonts"};
        for (String dir : userFontDirs) {
            File fontDir = new File(userHome + dir);
            if (fontDir.exists() && fontDir.isDirectory()) {
                File[] files = fontDir.listFiles((d, name) ->
                    name.toLowerCase().endsWith(".ttf") ||
                    name.toLowerCase().endsWith(".ttc") ||
                    name.toLowerCase().endsWith(".otf"));
                if (files != null) {
                    for (File f : files) {
                        try {
                            Font font = Font.createFont(Font.TRUETYPE_FONT, f);
                            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                            ge.registerFont(font);
                            // Prefer CJK fonts
                            if (font.getFamily().toLowerCase().contains("cjk") ||
                                font.getFamily().toLowerCase().contains("noto") ||
                                font.getFamily().toLowerCase().contains("wqy") ||
                                font.getFamily().toLowerCase().contains("han")) {
                                customFont = font;
                                preferredFont = font.getFamily();
                                return;
                            }
                        } catch (Exception e) {}
                    }
                }
            }
        }
    }

    private static void setUIFonts() {
        Font baseFont = getFont(Font.PLAIN, 13);
        java.util.Enumeration<Object> keys = javax.swing.UIManager.getLookAndFeelDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = javax.swing.UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                javax.swing.UIManager.put(key, new javax.swing.plaf.FontUIResource(baseFont));
            }
        }
    }

    public static Font getFont(int style, float size) {
        if (!initialized) init();
        if (customFont != null) {
            return customFont.deriveFont(style, size);
        }
        return new Font(preferredFont, style, (int) size);
    }

    public static Font getFont(String preferred, int style, float size) {
        if (!initialized) init();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();
        for (String f : available) {
            if (f.equalsIgnoreCase(preferred)) return new Font(preferred, style, (int) size);
        }
        return getFont(style, size);
    }

    public static String getPreferredFontName() {
        if (!initialized) init();
        return preferredFont;
    }

    public static boolean hasChineseFont() {
        if (!initialized) init();
        return customFont != null ||
            (!Font.SANS_SERIF.equals(preferredFont) && !"Dialog".equals(preferredFont));
    }
}
