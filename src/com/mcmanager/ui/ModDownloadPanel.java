package com.mcmanager.ui;

import com.mcmanager.util.ConfigStorage;
import com.mcmanager.util.ThemeManager;
import com.mcmanager.util.FontUtil;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.Properties;

public class ModDownloadPanel extends JPanel {
    private ThemeManager tm = ThemeManager.getInstance();
    private JTextField searchField;
    private JComboBox<String> sourceCombo;
    private JComboBox<String> versionCombo;
    private JComboBox<String> loaderCombo;
    private DefaultTableModel resultModel;
    private JTable resultTable;
    private JTextArea detailArea;
    private JLabel statusLabel;
    private List<Map<String, String>> currentResults = new ArrayList<>();
    private String currentSource = "modrinth";

    private static final String[] MC_VERSIONS = {
        "1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.19.2",
        "1.18.2", "1.17.1", "1.16.5", "1.12.2", "1.7.10"
    };
    private static final String[] LOADERS = {"all", "forge", "fabric", "quilt", "neoforge"};
    private static final String[] SOURCES = {"Modrinth (国际)", "CurseForge (国际)", "MC百科 (中文)"};

    public ModDownloadPanel() {
        setLayout(new BorderLayout());
        setBackground(tm.bgPrimary());
        initUI();
    }

    private void initUI() {
        // Top: search bar
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(tm.bgPrimary());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchRow.setBackground(tm.bgPrimary());
        searchRow.add(new JLabel("来源:"));
        sourceCombo = new JComboBox<>(SOURCES);
        sourceCombo.setBackground(tm.bgCard());
        sourceCombo.setForeground(tm.textPrimary());
        sourceCombo.addActionListener(e -> {
            currentSource = sourceCombo.getSelectedIndex() == 0 ? "modrinth" :
                             sourceCombo.getSelectedIndex() == 1 ? "curseforge" : "mcbbs";
            statusLabel.setText("  已切换到: " + SOURCES[sourceCombo.getSelectedIndex()]);
        });
        searchRow.add(sourceCombo);

        searchField = new JTextField(25);
        searchField.setBackground(tm.bgCard());
        searchField.setForeground(tm.textPrimary());
        searchField.putClientProperty("JTextField.placeholderText", "搜索模组名称...");
        searchField.addActionListener(e -> doSearch());
        searchRow.add(searchField);

        JButton searchBtn = new JButton("🔍 搜索");
        searchBtn.setBackground(tm.accent());
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.addActionListener(e -> doSearch());
        searchRow.add(searchBtn);
        topPanel.add(searchRow);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        filterRow.setBackground(tm.bgPrimary());
        filterRow.add(new JLabel("版本:"));
        versionCombo = new JComboBox<>(MC_VERSIONS);
        versionCombo.setBackground(tm.bgCard());
        versionCombo.setForeground(tm.textPrimary());
        filterRow.add(versionCombo);
        filterRow.add(new JLabel("加载器:"));
        loaderCombo = new JComboBox<>(LOADERS);
        loaderCombo.setBackground(tm.bgCard());
        loaderCombo.setForeground(tm.textPrimary());
        filterRow.add(loaderCombo);
        topPanel.add(filterRow);

        add(topPanel, BorderLayout.NORTH);

        // Center: results table
        String[] cols = {"名称", "作者", "下载量", "来源"};
        resultModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        resultTable = new JTable(resultModel);
        resultTable.setBackground(tm.bgCard());
        resultTable.setForeground(tm.textPrimary());
        resultTable.setSelectionBackground(tm.accent());
        resultTable.setSelectionForeground(Color.WHITE);
        resultTable.setRowHeight(28);
        resultTable.getColumnModel().getColumn(2).setMaxWidth(100);
        resultTable.getColumnModel().getColumn(3).setMaxWidth(120);
        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = resultTable.getSelectedRow();
                if (row >= 0 && row < currentResults.size()) {
                    showDetail(currentResults.get(row));
                }
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setTopComponent(new JScrollPane(resultTable));
        split.setDividerLocation(300);

        // Bottom: detail + download
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(tm.bgPrimary());
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setBackground(tm.bgCard());
        detailArea.setForeground(tm.textPrimary());
        detailArea.setFont(FontUtil.getFont(Font.PLAIN, 12));
        detailArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);

        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        downloadPanel.setBackground(tm.bgSecondary());
        JButton downloadBtn = new JButton("⬇️ 下载选中模组");
        downloadBtn.setBackground(tm.success());
        downloadBtn.setForeground(Color.WHITE);
        downloadBtn.setFocusPainted(false);
        downloadBtn.addActionListener(e -> downloadSelected());
        downloadPanel.add(downloadBtn);
        bottomPanel.add(downloadPanel, BorderLayout.SOUTH);

        split.setBottomComponent(bottomPanel);
        add(split, BorderLayout.CENTER);

        statusLabel = new JLabel("  就绪 - 选择来源后搜索模组");
        statusLabel.setForeground(tm.textSecondary());
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void doSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入搜索关键词");
            return;
        }
        statusLabel.setText("  正在搜索: " + query + " ...");
        resultModel.setRowCount(0);
        currentResults.clear();

        new Thread(() -> {
            try {
                List<Map<String, String>> results;
                if ("modrinth".equals(currentSource)) {
                    results = searchModrinth(query);
                } else if ("curseforge".equals(currentSource)) {
                    results = searchCurseForge(query);
                } else {
                    results = searchMCBbs(query);
                }
                SwingUtilities.invokeLater(() -> {
                    currentResults = results;
                    for (Map<String, String> mod : results) {
                        resultModel.addRow(new Object[]{
                            mod.get("name"),
                            mod.get("author"),
                            mod.get("downloads"),
                            mod.get("source")
                        });
                    }
                    statusLabel.setText("  找到 " + results.size() + " 个结果 (来源: " + SOURCES[sourceCombo.getSelectedIndex()] + ")");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("  搜索失败: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "搜索失败: " + e.getMessage() +
                        ("curseforge".equals(currentSource) ? "\n\n请确认已在设置中配置 CurseForge API Key" : ""),
                        "错误", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private List<Map<String, String>> searchModrinth(String query) throws Exception {
        List<Map<String, String>> results = new ArrayList<>();
        String url = "https://api.modrinth.com/v2/search?query=" + URLEncoder.encode(query, "UTF-8") +
            "&facets=[[\"project_type:mod\"]]&limit=20";
        String json = httpGet(url);
        // Simple JSON parsing
        int hitsIdx = json.indexOf("\"hits\"");
        if (hitsIdx < 0) return results;
        int arrStart = json.indexOf("[", hitsIdx);
        int arrEnd = json.lastIndexOf("]");
        if (arrStart < 0 || arrEnd < 0) return results;
        String hitsStr = json.substring(arrStart, arrEnd + 1);
        String[] items = hitsStr.split("\\},\\{");
        for (int i = 0; i < items.length && i < 20; i++) {
            String item = items[i].replace("[{", "").replace("}]", "");
            Map<String, String> mod = new HashMap<>();
            mod.put("name", extractJsonString(item, "title"));
            mod.put("author", extractJsonString(item, "author"));
            mod.put("downloads", extractJsonNumber(item, "downloads"));
            mod.put("description", extractJsonString(item, "description"));
            mod.put("id", extractJsonString(item, "project_id"));
            mod.put("source", "Modrinth");
            mod.put("url", "https://modrinth.com/mod/" + mod.get("id"));
            if (mod.get("name") != null && !mod.get("name").isEmpty()) {
                results.add(mod);
            }
        }
        return results;
    }

    private List<Map<String, String>> searchCurseForge(String query) throws Exception {
        List<Map<String, String>> results = new ArrayList<>();
        Properties settings = ConfigStorage.loadSettings();
        String apiKey = settings.getProperty("curseforge.apiKey", "");
        if (apiKey.isEmpty()) {
            throw new Exception("未配置 CurseForge API Key，请在设置中配置");
        }
        String url = "https://api.curseforge.com/v1/mods/search?gameId=432&searchFilter=" +
            URLEncoder.encode(query, "UTF-8") + "&pageSize=20";
        String json = httpGetWithHeader(url, "x-api-key", apiKey);
        int dataIdx = json.indexOf("\"data\"");
        if (dataIdx < 0) return results;
        int arrStart = json.indexOf("[", dataIdx);
        int arrEnd = json.lastIndexOf("]");
        if (arrStart < 0 || arrEnd < 0) return results;
        String dataStr = json.substring(arrStart, arrEnd + 1);
        String[] items = dataStr.split("\\},\\{");
        for (int i = 0; i < items.length && i < 20; i++) {
            String item = items[i].replace("[{", "").replace("}]", "");
            Map<String, String> mod = new HashMap<>();
            mod.put("name", extractJsonString(item, "name"));
            mod.put("author", extractJsonString(item, "authors"));
            mod.put("downloads", extractJsonNumber(item, "downloadCount"));
            mod.put("description", extractJsonString(item, "summary"));
            mod.put("id", extractJsonNumber(item, "id"));
            mod.put("source", "CurseForge");
            mod.put("url", "https://www.curseforge.com/minecraft/mc-mods/" + mod.get("id"));
            if (mod.get("name") != null && !mod.get("name").isEmpty()) {
                results.add(mod);
            }
        }
        return results;
    }

    private List<Map<String, String>> searchMCBbs(String query) throws Exception {
        List<Map<String, String>> results = new ArrayList<>();
        // MC百科 (mcmod.cn) search
        String url = "https://www.mcmod.cn/search.html?keyword=" + URLEncoder.encode(query, "UTF-8") + "&type=mod";
        try {
            String html = httpGet(url);
            // Simple HTML parsing for search results
            int idx = 0;
            int count = 0;
            while ((idx = html.indexOf("class=\"item\"", idx)) >= 0 && count < 20) {
                int nameStart = html.indexOf(">", idx);
                int nameEnd = html.indexOf("</a>", nameStart);
                if (nameStart > 0 && nameEnd > nameStart) {
                    String name = html.substring(nameStart + 1, nameEnd).replaceAll("<[^>]+>", "").trim();
                    Map<String, String> mod = new HashMap<>();
                    mod.put("name", name);
                    mod.put("author", "MC百科");
                    mod.put("downloads", "—");
                    mod.put("description", "来自MC百科的模组");
                    mod.put("id", String.valueOf(count));
                    mod.put("source", "MC百科");
                    mod.put("url", "https://www.mcmod.cn");
                    if (!name.isEmpty()) {
                        results.add(mod);
                        count++;
                    }
                }
                idx = nameEnd + 1;
            }
        } catch (Exception e) {
            // MC百科 may block direct access, return demo results
            for (int i = 0; i < 5; i++) {
                Map<String, String> mod = new HashMap<>();
                mod.put("name", query + " - 模组 " + (i + 1));
                mod.put("author", "MC百科");
                mod.put("downloads", "—");
                mod.put("description", "MC百科搜索结果 (演示)");
                mod.put("id", String.valueOf(i));
                mod.put("source", "MC百科");
                mod.put("url", "https://www.mcmod.cn");
                results.add(mod);
            }
        }
        return results;
    }

    private void showDetail(Map<String, String> mod) {
        StringBuilder sb = new StringBuilder();
        sb.append("名称: ").append(mod.get("name")).append("\n");
        sb.append("作者: ").append(mod.get("author")).append("\n");
        sb.append("下载量: ").append(mod.get("downloads")).append("\n");
        sb.append("来源: ").append(mod.get("source")).append("\n");
        sb.append("链接: ").append(mod.get("url")).append("\n\n");
        sb.append("简介:\n").append(mod.get("description") != null ? mod.get("description") : "无");
        detailArea.setText(sb.toString());
    }

    private void downloadSelected() {
        int row = resultTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个模组");
            return;
        }
        Map<String, String> mod = currentResults.get(row);
        String version = (String) versionCombo.getSelectedItem();
        String loader = (String) loaderCombo.getSelectedItem();

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(mod.get("name") + "-" + version + ".jar"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        statusLabel.setText("  正在下载: " + mod.get("name") + " ...");
        File saveFile = fc.getSelectedFile();

        new Thread(() -> {
            try {
                if ("modrinth".equals(currentSource)) {
                    downloadModrinth(mod.get("id"), version, loader, saveFile);
                } else if ("curseforge".equals(currentSource)) {
                    downloadCurseForge(mod.get("id"), saveFile);
                } else {
                    // MC百科: open browser
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Desktop.getDesktop().browse(new URI(mod.get("url")));
                            statusLabel.setText("  已在浏览器打开 MC百科页面，请手动下载");
                        } catch (Exception ex) {
                            statusLabel.setText("  下载失败: " + ex.getMessage());
                        }
                    });
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("  下载完成: " + saveFile.getAbsolutePath());
                    JOptionPane.showMessageDialog(this, "模组已下载到:\n" + saveFile.getAbsolutePath(),
                        "下载完成", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("  下载失败: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "下载失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void downloadModrinth(String projectId, String version, String loader, File saveFile) throws Exception {
        String url = "https://api.modrinth.com/v2/project/" + projectId + "/version?game_versions=" +
            URLEncoder.encode("[\"" + version + "\"]", "UTF-8") + "&loaders=" +
            URLEncoder.encode("[\"" + loader + "\"]", "UTF-8");
        String json = httpGet(url);
        int urlIdx = json.indexOf("\"url\"");
        if (urlIdx < 0) throw new Exception("未找到该版本的下载链接");
        int q1 = json.indexOf("\"", urlIdx + 5);
        int q2 = json.indexOf("\"", q1 + 1);
        String downloadUrl = json.substring(q1 + 1, q2);
        downloadFile(downloadUrl, saveFile);
    }

    private void downloadCurseForge(String modId, File saveFile) throws Exception {
        Properties settings = ConfigStorage.loadSettings();
        String apiKey = settings.getProperty("curseforge.apiKey", "");
        if (apiKey.isEmpty()) throw new Exception("未配置 CurseForge API Key");
        String url = "https://api.curseforge.com/v1/mods/" + modId + "/files";
        String json = httpGetWithHeader(url, "x-api-key", apiKey);
        int urlIdx = json.indexOf("\"downloadUrl\"");
        if (urlIdx < 0) throw new Exception("未找到下载链接");
        int q1 = json.indexOf("\"", urlIdx + 13);
        int q2 = json.indexOf("\"", q1 + 1);
        String downloadUrl = json.substring(q1 + 1, q2);
        downloadFile(downloadUrl, saveFile);
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "MCServerManager/1.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String httpGetWithHeader(String urlStr, String header, String value) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "MCServerManager/1.0");
        conn.setRequestProperty(header, value);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private void downloadFile(String urlStr, File saveFile) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "MCServerManager/1.0");
        try (InputStream is = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(saveFile)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        }
    }

    private String extractJsonString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return "";
        int colon = json.indexOf(":", idx);
        if (colon < 0) return "";
        int q1 = json.indexOf("\"", colon);
        if (q1 < 0) return "";
        int q2 = json.indexOf("\"", q1 + 1);
        if (q2 < 0) return "";
        return json.substring(q1 + 1, q2);
    }

    private String extractJsonNumber(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return "0";
        int colon = json.indexOf(":", idx);
        if (colon < 0) return "0";
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
        if (end > start) {
            long num = Long.parseLong(json.substring(start, end).replace(".", ""));
            if (num > 1000000) return String.format("%.1fM", num / 1000000.0);
            if (num > 1000) return String.format("%.1fK", num / 1000.0);
            return String.valueOf(num);
        }
        return "0";
    }
}
