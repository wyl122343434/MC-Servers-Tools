package com.mcmanager.ui;

import com.mcmanager.core.ServerConfig;
import com.mcmanager.util.ThemeManager;
import com.mcmanager.util.FontUtil;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class PlayerManagerPanel extends JPanel {
    private ThemeManager tm = ThemeManager.getInstance();
    private ServerConfig currentServer;
    private JTextField playerNameField;
    private java.util.function.Consumer<String> commandSender;

    // Online players
    private DefaultTableModel onlineTableModel;
    private JTable onlineTable;
    private JLabel onlineCountLabel;

    // Banned players
    private DefaultListModel<String> bannedListModel;
    private JList<String> bannedList;

    // Config editor
    private JTextArea configTextArea;
    private JLabel configStatusLabel;

    public PlayerManagerPanel() {
        setLayout(new BorderLayout());
        setBackground(tm.bgPrimary());
        initUI();
    }

    public void setServer(ServerConfig server) {
        this.currentServer = server;
        refreshOnlinePlayers();
        refreshBannedList();
        loadServerConfig();
    }

    public void setCommandSender(java.util.function.Consumer<String> sender) {
        this.commandSender = sender;
    }

    private void initUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(FontUtil.getFont(Font.PLAIN, 13));
        tabs.setBackground(tm.bgSecondary());
        tabs.setForeground(tm.textPrimary());

        tabs.addTab("👥 在线玩家", createOnlinePlayersPanel());
        tabs.addTab("🚫 拉黑列表", createBannedPanel());
        tabs.addTab("⚙️ 服务器配置", createConfigPanel());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createOnlinePlayersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(tm.bgPrimary());

        // Top: quick actions
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(tm.bgPrimary());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        actionPanel.setBackground(tm.bgPrimary());
        playerNameField = new JTextField(15);
        playerNameField.setBackground(tm.bgCard());
        playerNameField.setForeground(tm.textPrimary());
        playerNameField.putClientProperty("JTextField.placeholderText", "输入玩家名...");

        JButton opBtn = new JButton("✅ 给予OP");
        opBtn.setBackground(tm.success());
        opBtn.setForeground(Color.WHITE);
        opBtn.addActionListener(e -> runPlayerCommand("op", "给予OP"));

        JButton deopBtn = new JButton("❌ 移除OP");
        deopBtn.setBackground(new Color(180, 100, 50));
        deopBtn.setForeground(Color.WHITE);
        deopBtn.addActionListener(e -> runPlayerCommand("deop", "移除OP"));

        JButton kickBtn = new JButton("👢 踢出");
        kickBtn.setBackground(new Color(200, 60, 60));
        kickBtn.setForeground(Color.WHITE);
        kickBtn.addActionListener(e -> runPlayerCommand("kick", "踢出"));

        JButton whitelistBtn = new JButton("📝 添加白名单");
        whitelistBtn.setBackground(tm.accent());
        whitelistBtn.setForeground(Color.WHITE);
        whitelistBtn.addActionListener(e -> runPlayerCommand("whitelist add", "添加白名单"));

        JButton banBtn = new JButton("🚫 拉黑");
        banBtn.setBackground(new Color(120, 40, 40));
        banBtn.setForeground(Color.WHITE);
        banBtn.addActionListener(e -> runPlayerCommand("ban", "拉黑"));

        JButton refreshBtn = new JButton("🔄 刷新");
        refreshBtn.addActionListener(e -> refreshOnlinePlayers());

        actionPanel.add(new JLabel("玩家名:"));
        actionPanel.add(playerNameField);
        actionPanel.add(opBtn);
        actionPanel.add(deopBtn);
        actionPanel.add(kickBtn);
        actionPanel.add(whitelistBtn);
        actionPanel.add(banBtn);
        actionPanel.add(refreshBtn);
        topPanel.add(actionPanel, BorderLayout.NORTH);

        onlineCountLabel = new JLabel("  在线玩家: 0");
        onlineCountLabel.setForeground(tm.textSecondary());
        topPanel.add(onlineCountLabel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        // Online players table
        String[] cols = {"玩家名", "是否OP", "操作"};
        onlineTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        onlineTable = new JTable(onlineTableModel);
        onlineTable.setBackground(tm.bgCard());
        onlineTable.setForeground(tm.textPrimary());
        onlineTable.setSelectionBackground(tm.accent());
        onlineTable.setSelectionForeground(Color.WHITE);
        onlineTable.setRowHeight(28);
        onlineTable.getColumnModel().getColumn(1).setMaxWidth(100);
        onlineTable.getColumnModel().getColumn(2).setMaxWidth(200);
        onlineTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = onlineTable.getSelectedRow();
                if (row >= 0) {
                    playerNameField.setText(onlineTableModel.getValueAt(row, 0).toString());
                }
            }
        });
        panel.add(new JScrollPane(onlineTable), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBannedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(tm.bgPrimary());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(tm.bgPrimary());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        btnPanel.setBackground(tm.bgPrimary());
        JButton refreshBannedBtn = new JButton("🔄 刷新拉黑列表");
        refreshBannedBtn.addActionListener(e -> refreshBannedList());
        JButton pardonBtn = new JButton("✅ 解除拉黑");
        pardonBtn.setBackground(tm.success());
        pardonBtn.setForeground(Color.WHITE);
        pardonBtn.addActionListener(e -> {
            String selected = bannedList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "请先选择要解除拉黑的玩家");
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "确定解除拉黑玩家 " + selected + " ?",
                "确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                sendServerCommand("pardon " + selected);
                JOptionPane.showMessageDialog(this, "已解除拉黑: " + selected);
                refreshBannedList();
            }
        });
        btnPanel.add(refreshBannedBtn);
        btnPanel.add(pardonBtn);
        topPanel.add(btnPanel, BorderLayout.NORTH);

        JLabel hint = new JLabel("  提示: 拉黑列表从 banned-players.json 读取");
        hint.setForeground(tm.textSecondary());
        topPanel.add(hint, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        bannedListModel = new DefaultListModel<>();
        bannedList = new JList<>(bannedListModel);
        bannedList.setBackground(tm.bgCard());
        bannedList.setForeground(tm.textPrimary());
        bannedList.setSelectionBackground(tm.accent());
        bannedList.setSelectionForeground(Color.WHITE);
        bannedList.setFont(FontUtil.getFont(Font.PLAIN, 14));
        panel.add(new JScrollPane(bannedList), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(tm.bgPrimary());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(tm.bgPrimary());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        btnPanel.setBackground(tm.bgPrimary());
        JButton reloadBtn = new JButton("🔄 重新加载");
        reloadBtn.addActionListener(e -> loadServerConfig());
        JButton saveBtn = new JButton("💾 保存配置");
        saveBtn.setBackground(tm.success());
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> saveServerConfig());
        JButton applyBtn = new JButton("✅ 保存并应用(需重启)");
        applyBtn.setBackground(tm.accent());
        applyBtn.setForeground(Color.WHITE);
        applyBtn.addActionListener(e -> {
            saveServerConfig();
            JOptionPane.showMessageDialog(this, "配置已保存！\n\n修改 server.properties 需要重启服务器才能生效。\n请在控制台点击「重启」按钮。", "保存成功", JOptionPane.INFORMATION_MESSAGE);
        });
        btnPanel.add(reloadBtn);
        btnPanel.add(saveBtn);
        btnPanel.add(applyBtn);
        topPanel.add(btnPanel, BorderLayout.NORTH);

        configStatusLabel = new JLabel("  选择服务器后加载 server.properties");
        configStatusLabel.setForeground(tm.textSecondary());
        topPanel.add(configStatusLabel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        configTextArea = new JTextArea();
        configTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        configTextArea.setBackground(new Color(30, 30, 30));
        configTextArea.setForeground(new Color(200, 200, 200));
        configTextArea.setCaretColor(Color.WHITE);
        configTextArea.setTabSize(2);
        panel.add(new JScrollPane(configTextArea), BorderLayout.CENTER);

        return panel;
    }

    private void runPlayerCommand(String command, String actionName) {
        String player = playerNameField.getText().trim();
        if (player.isEmpty()) {
            int row = onlineTable.getSelectedRow();
            if (row >= 0) player = onlineTableModel.getValueAt(row, 0).toString();
        }
        if (player.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入玩家名或从列表中选择玩家");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "确定对玩家 " + player + " 执行: " + actionName + " ?",
            "确认操作", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        sendServerCommand(command + " " + player);
        JOptionPane.showMessageDialog(this, "已执行: " + command + " " + player);

        if ("ban".equals(command)) refreshBannedList();
        refreshOnlinePlayers();
    }

    private void sendServerCommand(String command) {
        if (commandSender != null) {
            commandSender.accept(command);
        } else {
            System.out.println("[Server Command] " + command);
        }
    }

    public void refreshOnlinePlayers() {
        onlineTableModel.setRowCount(0);
        if (currentServer == null) {
            onlineCountLabel.setText("  在线玩家: 0 (未选择服务器)");
            return;
        }
        // Try to read from server process or use simulated data
        // In real implementation, this would parse the "list" command output
        onlineCountLabel.setText("  在线玩家: 0 (使用控制台输入 'list' 查看)");

        // Try to read ops.json to show OP status
        File opsFile = new File(currentServer.getServerDir(), "ops.json");
        java.util.List<String> ops = new java.util.ArrayList<>();
        if (opsFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(opsFile.toPath()), "UTF-8");
                // Simple parsing for "name":"xxx"
                int idx = 0;
                while ((idx = content.indexOf("\"name\"", idx)) >= 0) {
                    int colon = content.indexOf(":", idx);
                    int q1 = content.indexOf("\"", colon);
                    int q2 = content.indexOf("\"", q1 + 1);
                    if (q1 >= 0 && q2 >= 0) {
                        ops.add(content.substring(q1 + 1, q2));
                    }
                    idx = q2 + 1;
                }
            } catch (Exception e) {}
        }
    }

    public void refreshBannedList() {
        bannedListModel.clear();
        if (currentServer == null) return;
        File bannedFile = new File(currentServer.getServerDir(), "banned-players.json");
        if (bannedFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(bannedFile.toPath()), "UTF-8");
                int idx = 0;
                while ((idx = content.indexOf("\"name\"", idx)) >= 0) {
                    int colon = content.indexOf(":", idx);
                    int q1 = content.indexOf("\"", colon);
                    int q2 = content.indexOf("\"", q1 + 1);
                    if (q1 >= 0 && q2 >= 0) {
                        String name = content.substring(q1 + 1, q2);
                        // Get reason
                        String reason = "";
                        int reasonIdx = content.indexOf("\"reason\"", q2);
                        if (reasonIdx >= 0 && reasonIdx < q2 + 200) {
                            int rColon = content.indexOf(":", reasonIdx);
                            int rq1 = content.indexOf("\"", rColon);
                            int rq2 = content.indexOf("\"", rq1 + 1);
                            if (rq1 >= 0 && rq2 >= 0) reason = content.substring(rq1 + 1, rq2);
                        }
                        bannedListModel.addElement(name + (reason.isEmpty() ? "" : "  -  " + reason));
                    }
                    idx = q2 + 1;
                }
            } catch (Exception e) {}
        }
        if (bannedListModel.isEmpty()) {
            bannedListModel.addElement("(暂无拉黑玩家)");
        }
    }

    public void loadServerConfig() {
        configTextArea.setText("");
        if (currentServer == null) {
            configStatusLabel.setText("  未选择服务器");
            return;
        }
        File configFile = new File(currentServer.getServerDir(), "server.properties");
        if (configFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(configFile.toPath()), "UTF-8");
                configTextArea.setText(content);
                configStatusLabel.setText("  已加载: " + configFile.getAbsolutePath() + " (" + content.split("\n").length + " 行)");
            } catch (Exception e) {
                configStatusLabel.setText("  加载失败: " + e.getMessage());
            }
        } else {
            configTextArea.setText("#Minecraft server properties\n" +
                "#Sat Aug 27 12:00:00 CST 2026\n" +
                "enable-jmx-monitoring=false\n" +
                "rcon.port=25575\n" +
                "level-seed=\n" +
                "gamemode=survival\n" +
                "enable-command-block=false\n" +
                "enable-query=false\n" +
                "generator-settings={}\n" +
                "enforce-secure-profile=true\n" +
                "level-name=world\n" +
                "motd=A Minecraft Server\n" +
                "query.port=25565\n" +
                "pvp=true\n" +
                "generate-structures=true\n" +
                "max-chained-neighbor-updates=1000000\n" +
                "difficulty=easy\n" +
                "network-compression-threshold=256\n" +
                "max-tick-time=60000\n" +
                "require-resource-pack=false\n" +
                "use-native-transport=true\n" +
                "max-players=20\n" +
                "online-mode=true\n" +
                "enable-status=true\n" +
                "allow-flight=false\n" +
                "initial-disabled-packs=\n" +
                "broadcast-rcon-to-ops=true\n" +
                "view-distance=10\n" +
                "server-ip=\n" +
                "resource-pack-prompt=\n" +
                "allow-nether=true\n" +
                "server-port=25565\n" +
                "enable-rcon=false\n" +
                "sync-chunk-writes=true\n" +
                "op-permission-level=4\n" +
                "prevent-proxy-connections=false\n" +
                "hide-online-players=false\n" +
                "resource-pack=\n" +
                "entity-broadcast-range-percentage=100\n" +
                "simulation-distance=10\n" +
                "rcon.password=\n" +
                "player-idle-timeout=0\n" +
                "force-gamemode=false\n" +
                "rate-limit=0\n" +
                "hardcore=false\n" +
                "white-list=false\n" +
                "broadcast-console-to-ops=true\n" +
                "spawn-npcs=true\n" +
                "spawn-animals=true\n" +
                "log-ips=true\n" +
                "function-permission-level=2\n" +
                "level-type=minecraft\\:normal\n" +
                "text-filtering-config=\n" +
                "spawn-monsters=true\n" +
                "enforce-whitelist=false\n" +
                "spawn-protection=16\n" +
                "resource-pack-sha1=\n" +
                "max-world-size=29999984\n");
            configStatusLabel.setText("  server.properties 不存在，已生成默认模板");
        }
    }

    public void saveServerConfig() {
        if (currentServer == null) {
            JOptionPane.showMessageDialog(this, "请先选择服务器");
            return;
        }
        File configFile = new File(currentServer.getServerDir(), "server.properties");
        try {
            java.nio.file.Files.write(configFile.toPath(), configTextArea.getText().getBytes("UTF-8"));
            configStatusLabel.setText("  ✓ 已保存: " + configFile.getAbsolutePath() + " - " +
                new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()));
            JOptionPane.showMessageDialog(this, "配置已保存到:\n" + configFile.getAbsolutePath() +
                "\n\n修改需要重启服务器才能生效。", "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
