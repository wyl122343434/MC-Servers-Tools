package com.mcmanager.ui;

import com.mcmanager.util.ThemeManager;
import com.mcmanager.util.FontUtil;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

public class ModManagerPanel extends JPanel {
    private ThemeManager tm = ThemeManager.getInstance();
    private JTextField searchField;
    private DefaultTableModel modTableModel;
    private JTable modTable;
    private JLabel statusLabel;
    private String modsDir = "";
    private List<ModInfo> allMods = new ArrayList<>();

    public ModManagerPanel() {
        setLayout(new BorderLayout());
        setBackground(tm.bgPrimary());
        initUI();
    }

    public void setModsDir(String dir) {
        this.modsDir = dir;
        refreshMods();
    }

    private void initUI() {
        // Top toolbar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(tm.bgPrimary());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("📦 模组管理");
        title.setFont(FontUtil.getFont(Font.BOLD, 20));
        title.setForeground(tm.textPrimary());
        topPanel.add(title, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.setBackground(tm.bgPrimary());
        searchField = new JTextField(20);
        searchField.setBackground(tm.bgCard());
        searchField.setForeground(tm.textPrimary());
        searchField.putClientProperty("JTextField.placeholderText", "搜索模组...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterMods(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterMods(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterMods(); }
        });
        searchPanel.add(new JLabel("🔍"));
        searchPanel.add(searchField);
        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Mod table
        String[] cols = {"状态", "模组名称", "版本", "文件名", "大小"};
        modTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 0) return Boolean.class;
                return String.class;
            }
        };
        modTable = new JTable(modTableModel);
        modTable.setBackground(tm.bgCard());
        modTable.setForeground(tm.textPrimary());
        modTable.setSelectionBackground(tm.accent());
        modTable.setSelectionForeground(Color.WHITE);
        modTable.setRowHeight(28);
        modTable.getColumnModel().getColumn(0).setMaxWidth(60);
        modTable.getColumnModel().getColumn(4).setMaxWidth(100);

        JScrollPane scroll = new JScrollPane(modTable);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        // Bottom toolbar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(tm.bgSecondary());
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, tm.border()));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        btnPanel.setBackground(tm.bgSecondary());
        JButton refreshBtn = new JButton("🔄 刷新");
        refreshBtn.addActionListener(e -> refreshMods());
        JButton enableBtn = new JButton("✅ 启用");
        enableBtn.addActionListener(e -> toggleSelectedMod(true));
        JButton disableBtn = new JButton("🚫 禁用");
        disableBtn.addActionListener(e -> toggleSelectedMod(false));
        JButton deleteBtn = new JButton("🗑️ 删除");
        deleteBtn.setBackground(new Color(200, 60, 60));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.addActionListener(e -> deleteSelectedMod());
        JButton openFolderBtn = new JButton("📂 打开文件夹");
        openFolderBtn.addActionListener(e -> openModsFolder());
        JButton checkUpdateBtn = new JButton("⬆️ 检查更新");
        checkUpdateBtn.addActionListener(e -> checkUpdates());

        btnPanel.add(refreshBtn);
        btnPanel.add(enableBtn);
        btnPanel.add(disableBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(new JSeparator(SwingConstants.VERTICAL));
        btnPanel.add(openFolderBtn);
        btnPanel.add(checkUpdateBtn);
        bottomPanel.add(btnPanel, BorderLayout.WEST);

        statusLabel = new JLabel("  请选择服务器后查看模组");
        statusLabel.setForeground(tm.textSecondary());
        bottomPanel.add(statusLabel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void refreshMods() {
        allMods.clear();
        modTableModel.setRowCount(0);
        if (modsDir == null || modsDir.isEmpty()) {
            statusLabel.setText("  未设置模组目录");
            return;
        }
        File dir = new File(modsDir);
        if (!dir.exists() || !dir.isDirectory()) {
            statusLabel.setText("  模组目录不存在: " + modsDir);
            return;
        }
        File[] files = dir.listFiles((d, name) ->
            name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".jar.disabled"));
        if (files != null) {
            for (File f : files) {
                ModInfo mod = parseModInfo(f);
                allMods.add(mod);
            }
        }
        filterMods();
        statusLabel.setText("  共 " + allMods.size() + " 个模组");
    }

    private ModInfo parseModInfo(File file) {
        ModInfo mod = new ModInfo();
        mod.file = file;
        mod.fileName = file.getName();
        mod.size = file.length();
        mod.enabled = !file.getName().toLowerCase().endsWith(".disabled");
        mod.name = file.getName().replace(".jar.disabled", "").replace(".jar", "");
        mod.version = "未知";
        // Try to read mod info from JAR
        try (ZipFile zip = new ZipFile(file)) {
            // Check for fabric.mod.json
            ZipEntry fabricEntry = zip.getEntry("fabric.mod.json");
            if (fabricEntry != null) {
                String json = readEntry(zip, fabricEntry);
                mod.name = extractJsonValue(json, "name", mod.name);
                mod.version = extractJsonValue(json, "version", mod.version);
                mod.type = "Fabric";
            }
            // Check for mods.toml (Forge/NeoForge)
            ZipEntry tomlEntry = zip.getEntry("META-INF/mods.toml");
            if (tomlEntry != null) {
                String toml = readEntry(zip, tomlEntry);
                mod.name = extractTomlValue(toml, "displayName", mod.name);
                mod.version = extractTomlValue(toml, "version", mod.version);
                mod.type = "Forge/NeoForge";
            }
            // Check for quilt.mod.json
            ZipEntry quiltEntry = zip.getEntry("quilt.mod.json");
            if (quiltEntry != null) {
                String json = readEntry(zip, quiltEntry);
                mod.name = extractJsonValue(json, "name", mod.name);
                mod.version = extractJsonValue(json, "version", mod.version);
                mod.type = "Quilt";
            }
        } catch (Exception e) {
            // Can't read mod info, use file name
        }
        return mod;
    }

    private String readEntry(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream is = zip.getInputStream(entry)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
            return baos.toString("UTF-8");
        }
    }

    private String extractJsonValue(String json, String key, String def) {
        try {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx < 0) return def;
            int colon = json.indexOf(":", idx);
            if (colon < 0) return def;
            int start = json.indexOf("\"", colon + 1);
            if (start < 0) return def;
            int end = json.indexOf("\"", start + 1);
            if (end < 0) return def;
            return json.substring(start + 1, end);
        } catch (Exception e) {
            return def;
        }
    }

    private String extractTomlValue(String toml, String key, String def) {
        try {
            int idx = toml.indexOf(key);
            if (idx < 0) return def;
            int eq = toml.indexOf("=", idx);
            if (eq < 0) return def;
            int start = toml.indexOf("\"", eq + 1);
            if (start < 0) return def;
            int end = toml.indexOf("\"", start + 1);
            if (end < 0) return def;
            return toml.substring(start + 1, end);
        } catch (Exception e) {
            return def;
        }
    }

    private void filterMods() {
        String query = searchField.getText().toLowerCase().trim();
        modTableModel.setRowCount(0);
        for (ModInfo mod : allMods) {
            if (query.isEmpty() || mod.name.toLowerCase().contains(query) ||
                mod.fileName.toLowerCase().contains(query)) {
                modTableModel.addRow(new Object[]{
                    mod.enabled,
                    mod.name + (mod.type != null ? " [" + mod.type + "]" : ""),
                    mod.version,
                    mod.fileName,
                    formatSize(mod.size)
                });
            }
        }
    }

    private void toggleSelectedMod(boolean enable) {
        int row = modTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个模组");
            return;
        }
        // Find the mod in allMods by matching file name
        String fileName = (String) modTableModel.getValueAt(row, 3);
        for (ModInfo mod : allMods) {
            if (mod.fileName.equals(fileName)) {
                if (enable && !mod.enabled) {
                    File newFile = new File(mod.file.getParent(), mod.fileName.replace(".jar.disabled", ".jar"));
                    if (mod.file.renameTo(newFile)) {
                        mod.file = newFile;
                        mod.fileName = newFile.getName();
                        mod.enabled = true;
                    }
                } else if (!enable && mod.enabled) {
                    File newFile = new File(mod.file.getParent(), mod.fileName + ".disabled");
                    if (mod.file.renameTo(newFile)) {
                        mod.file = newFile;
                        mod.fileName = newFile.getName();
                        mod.enabled = false;
                    }
                }
                break;
            }
        }
        filterMods();
        statusLabel.setText("  已更新模组状态");
    }

    private void deleteSelectedMod() {
        int row = modTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个模组");
            return;
        }
        String fileName = (String) modTableModel.getValueAt(row, 3);
        if (JOptionPane.showConfirmDialog(this, "确定删除模组 " + fileName + " ?", "确认删除",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (Iterator<ModInfo> it = allMods.iterator(); it.hasNext();) {
                ModInfo mod = it.next();
                if (mod.fileName.equals(fileName)) {
                    if (mod.file.delete()) {
                        it.remove();
                    } else {
                        JOptionPane.showMessageDialog(this, "删除失败");
                    }
                    break;
                }
            }
            filterMods();
        }
    }

    private void openModsFolder() {
        if (modsDir == null || modsDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "未设置模组目录");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(modsDir));
            } else {
                JOptionPane.showMessageDialog(this, "模组目录: " + modsDir);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "无法打开文件夹: " + e.getMessage());
        }
    }

    private void checkUpdates() {
        JOptionPane.showMessageDialog(this,
            "更新检查功能开发中。\n\n当前模组数量: " + allMods.size() + "\n" +
            "建议手动到 Modrinth 或 CurseForge 检查最新版本。",
            "检查更新", JOptionPane.INFORMATION_MESSAGE);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private static class ModInfo {
        File file;
        String fileName;
        String name;
        String version;
        String type;
        long size;
        boolean enabled;
    }
}
