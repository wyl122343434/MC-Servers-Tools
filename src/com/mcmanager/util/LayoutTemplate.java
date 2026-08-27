package com.mcmanager.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class LayoutTemplate {
    private String name;
    private Map<String, Object> config = new LinkedHashMap<>();

    public LayoutTemplate(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void set(String key, Object value) { config.put(key, value); }
    public Object get(String key) { return config.get(key); }
    public String getString(String key, String def) {
        Object v = config.get(key);
        return v != null ? v.toString() : def;
    }
    public int getInt(String key, int def) {
        Object v = config.get(key);
        try { return v != null ? Integer.parseInt(v.toString()) : def; }
        catch (Exception e) { return def; }
    }
    public boolean getBoolean(String key, boolean def) {
        Object v = config.get(key);
        try { return v != null ? Boolean.parseBoolean(v.toString()) : def; }
        catch (Exception e) { return def; }
    }

    // Default templates
    public static List<LayoutTemplate> getDefaultTemplates() {
        List<LayoutTemplate> list = new ArrayList<>();

        LayoutTemplate def = new LayoutTemplate("默认布局");
        def.set("dividerLocation", 250);
        def.set("windowWidth", 1200);
        def.set("windowHeight", 800);
        def.set("showServerList", true);
        def.set("consoleFontSize", 13);
        def.set("tabFontSize", 13);
        list.add(def);

        LayoutTemplate compact = new LayoutTemplate("紧凑布局");
        compact.set("dividerLocation", 180);
        compact.set("windowWidth", 900);
        compact.set("windowHeight", 650);
        compact.set("showServerList", true);
        compact.set("consoleFontSize", 11);
        compact.set("tabFontSize", 11);
        list.add(compact);

        LayoutTemplate console = new LayoutTemplate("控制台优先");
        console.set("dividerLocation", 200);
        console.set("windowWidth", 1400);
        console.set("windowHeight", 900);
        console.set("showServerList", true);
        console.set("consoleFontSize", 14);
        console.set("tabFontSize", 13);
        list.add(console);

        LayoutTemplate minimal = new LayoutTemplate("极简布局");
        minimal.set("dividerLocation", 0);
        minimal.set("windowWidth", 1000);
        minimal.set("windowHeight", 700);
        minimal.set("showServerList", false);
        minimal.set("consoleFontSize", 13);
        minimal.set("tabFontSize", 12);
        list.add(minimal);

        LayoutTemplate wide = new LayoutTemplate("宽屏布局");
        wide.set("dividerLocation", 300);
        wide.set("windowWidth", 1600);
        wide.set("windowHeight", 950);
        wide.set("showServerList", true);
        wide.set("consoleFontSize", 14);
        wide.set("tabFontSize", 14);
        list.add(wide);

        return list;
    }

    // Save to file
    public void saveToFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(escape(name)).append("\",\n");
        sb.append("  \"config\": {\n");
        int i = 0;
        for (Map.Entry<String, Object> e : config.entrySet()) {
            sb.append("    \"").append(escape(e.getKey())).append("\": ");
            if (e.getValue() instanceof Number) sb.append(e.getValue());
            else if (e.getValue() instanceof Boolean) sb.append(e.getValue());
            else sb.append("\"").append(escape(e.getValue().toString())).append("\"");
            if (++i < config.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("  }\n");
        sb.append("}\n");
        Files.write(Paths.get(path), sb.toString().getBytes("UTF-8"));
    }

    // Load from file
    public static LayoutTemplate loadFromFile(String path) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        return parseJson(content);
    }

    // Parse simple JSON
    private static LayoutTemplate parseJson(String json) {
        LayoutTemplate tpl = new LayoutTemplate("未命名");
        try {
            // Extract name
            int nameIdx = json.indexOf("\"name\"");
            if (nameIdx >= 0) {
                int colon = json.indexOf(":", nameIdx);
                int q1 = json.indexOf("\"", colon);
                int q2 = json.indexOf("\"", q1 + 1);
                if (q1 >= 0 && q2 >= 0) tpl.setName(json.substring(q1 + 1, q2));
            }
            // Extract config key-value pairs
            int configIdx = json.indexOf("\"config\"");
            if (configIdx >= 0) {
                int braceStart = json.indexOf("{", configIdx);
                int braceEnd = json.lastIndexOf("}");
                if (braceStart >= 0 && braceEnd > braceStart) {
                    String configStr = json.substring(braceStart + 1, braceEnd);
                    String[] pairs = configStr.split(",");
                    for (String pair : pairs) {
                        pair = pair.trim();
                        if (pair.isEmpty()) continue;
                        int colon = pair.indexOf(":");
                        if (colon < 0) continue;
                        String key = pair.substring(0, colon).trim().replace("\"", "");
                        String value = pair.substring(colon + 1).trim().replace("\"", "");
                        if (!key.isEmpty()) tpl.set(key, value);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tpl;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // Get user templates directory
    public static String getTemplatesDir() {
        String dir = System.getProperty("user.home") + File.separator + ".mcmanager" + File.separator + "templates";
        new File(dir).mkdirs();
        return dir;
    }

    // List user templates
    public static List<LayoutTemplate> listUserTemplates() {
        List<LayoutTemplate> list = new ArrayList<>();
        File dir = new File(getTemplatesDir());
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try {
                    list.add(loadFromFile(f.getAbsolutePath()));
                } catch (Exception e) {}
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return name;
    }
}
