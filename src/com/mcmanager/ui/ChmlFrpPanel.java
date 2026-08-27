package com.mcmanager.ui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class ChmlFrpPanel extends JPanel {
    private boolean loggedIn = false;
    private String currentUser = "";
    private List<Map<String, String>> tunnels = new ArrayList<>();
    private DefaultTableModel tunnelModel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JPanel loginPanel;
    private JPanel mainPanel;
    private JTextField userField;
    private JPasswordField passField;
    private JCheckBox rememberCheck;

    private static final String[] NODES = {
        "沈阳-01", "沈阳-02", "北京-01", "北京-02", "上海-01", "上海-02",
        "广州-01", "深圳-01", "成都-01", "杭州-01", "武汉-01", "西安-01",
        "重庆-01", "南京-01", "天津-01", "青岛-01", "郑州-01", "长沙-01",
        "香港-01", "香港-02", "台北-01", "东京-01", "新加坡-01", "洛杉矶-01"
    };

    public ChmlFrpPanel() {
        setLayout(new BorderLayout());
        initUI();
        loadTunnels();
        // Auto-login if saved account exists
        String[] account = loadAccount();
        if (account != null && account[0] != null && !account[0].isEmpty()) {
            userField.setText(account[0]);
            passField.setText(account[1]);
            rememberCheck.setSelected(true);
            doLogin();
        }
    }

    private void initUI() {
        // Login panel
        loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel title = new JLabel("ChmlFRP 内网穿透");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        loginPanel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        loginPanel.add(new JLabel("用户名:"), gbc);
        userField = new JTextField(20);
        gbc.gridx = 1;
        loginPanel.add(userField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        loginPanel.add(new JLabel("密码:"), gbc);
        passField = new JPasswordField(20);
        gbc.gridx = 1;
        loginPanel.add(passField, gbc);

        JButton loginBtn = new JButton("登录");
        loginBtn.addActionListener(e -> doLogin());
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        loginPanel.add(loginBtn, gbc);

        rememberCheck = new JCheckBox("记住密码（自动登录）");
        rememberCheck.setBackground(new Color(0,0,0,0));
        gbc.gridy = 4; gbc.gridwidth = 2;
        loginPanel.add(rememberCheck, gbc);

        JLabel hint = new JLabel("<html><center>没有账号？访问 chmlfrp.com 注册<br>支持账号密码登录和密钥文件登录</center></html>");
        hint.setForeground(Color.GRAY);
        gbc.gridy = 5;
        loginPanel.add(hint, gbc);

        // Main panel (after login)
        mainPanel = new JPanel(new BorderLayout());

        // Top: user info + status
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("未连接");
        statusLabel.setForeground(Color.ORANGE);
        userInfo.add(new JLabel("用户: "));
        JLabel userNameLabel = new JLabel();
        userNameLabel.setName("userNameLabel");
        userInfo.add(userNameLabel);
        userInfo.add(Box.createHorizontalStrut(20));
        userInfo.add(new JLabel("状态: "));
        userInfo.add(statusLabel);
        topPanel.add(userInfo, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createBtn = new JButton("创建隧道");
        createBtn.addActionListener(e -> createTunnel());
        JButton refreshBtn = new JButton("刷新");
        refreshBtn.addActionListener(e -> refreshTunnelTable());
        JButton logoutBtn = new JButton("退出登录");
        logoutBtn.addActionListener(e -> doLogout());
        btnPanel.add(createBtn);
        btnPanel.add(refreshBtn);
        btnPanel.add(logoutBtn);
        topPanel.add(btnPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center: split - tunnels + log
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setDividerLocation(300);

        // Tunnel table
        String[] cols = {"名称", "节点", "本地端口", "远程端口", "协议", "状态", "公网地址"};
        tunnelModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tunnelTable = new JTable(tunnelModel);
        tunnelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel tunnelWrapper = new JPanel(new BorderLayout());
        tunnelWrapper.setBorder(BorderFactory.createTitledBorder("隧道列表"));
        tunnelWrapper.add(new JScrollPane(tunnelTable), BorderLayout.CENTER);

        // Tunnel operations
        JPanel tunnelOps = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton connectBtn = new JButton("连接");
        connectBtn.addActionListener(e -> connectTunnel(tunnelTable.getSelectedRow()));
        JButton disconnectBtn = new JButton("断开");
        disconnectBtn.addActionListener(e -> disconnectTunnel(tunnelTable.getSelectedRow()));
        JButton deleteTunnelBtn = new JButton("删除");
        deleteTunnelBtn.addActionListener(e -> deleteTunnel(tunnelTable.getSelectedRow()));
        tunnelOps.add(connectBtn);
        tunnelOps.add(disconnectBtn);
        tunnelOps.add(deleteTunnelBtn);
        tunnelWrapper.add(tunnelOps, BorderLayout.SOUTH);
        split.setTopComponent(tunnelWrapper);

        // Log area
        logArea = new JTextArea();
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 200, 200));
        JPanel logWrapper = new JPanel(new BorderLayout());
        logWrapper.setBorder(BorderFactory.createTitledBorder("连接日志"));
        logWrapper.add(new JScrollPane(logArea), BorderLayout.CENTER);
        split.setBottomComponent(logWrapper);
        mainPanel.add(split, BorderLayout.CENTER);

        add(loginPanel, BorderLayout.CENTER);
    }

    private void doLogin() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名和密码");
            return;
        }
        // Save account if remember is checked
        if (rememberCheck.isSelected()) {
            saveAccount(user, pass);
        } else {
            clearAccount();
        }
        // Simulate login (in real implementation, call ChmlFRP API)
        loggedIn = true;
        currentUser = user;
        appendLog("[系统] 登录成功，欢迎 " + user);
        appendLog("[系统] 获取节点列表... 共 " + NODES.length + " 个节点可用");

        // Update UI
        remove(loginPanel);
        add(mainPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
        refreshTunnelTable();
    }

    private void saveAccount(String user, String pass) {
        try {
            String file = System.getProperty("user.home") + File.separator + ".mcmanager" + File.separator + "chmlfrp_account.properties";
            Properties props = new Properties();
            props.setProperty("username", user);
            props.setProperty("password", pass);
            props.setProperty("remember", "true");
            props.store(new FileOutputStream(file), "ChmlFRP Account");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] loadAccount() {
        try {
            String file = System.getProperty("user.home") + File.separator + ".mcmanager" + File.separator + "chmlfrp_account.properties";
            File f = new File(file);
            if (!f.exists()) return null;
            Properties props = new Properties();
            props.load(new FileInputStream(f));
            if ("true".equals(props.getProperty("remember", "false"))) {
                return new String[]{props.getProperty("username", ""), props.getProperty("password", "")};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void clearAccount() {
        try {
            String file = System.getProperty("user.home") + File.separator + ".mcmanager" + File.separator + "chmlfrp_account.properties";
            new File(file).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doLogout() {
        loggedIn = false;
        currentUser = "";
        remove(mainPanel);
        add(loginPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
        appendLog("[系统] 已退出登录");
    }

    private void createTunnel() {
        JTextField nameField = new JTextField("MC-服务器-" + (tunnels.size() + 1));
        JComboBox<String> nodeCombo = new JComboBox<>(NODES);
        JTextField localPortField = new JTextField("25565");
        JTextField remotePortField = new JTextField(String.valueOf(10000 + new Random().nextInt(50000)));
        JComboBox<String> protoCombo = new JComboBox<>(new String[]{"TCP", "UDP"});

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        addField(panel, gbc, row++, "隧道名称:", nameField);
        addField(panel, gbc, row++, "节点:", nodeCombo);
        addField(panel, gbc, row++, "本地端口:", localPortField);
        addField(panel, gbc, row++, "远程端口:", remotePortField);
        addField(panel, gbc, row++, "协议:", protoCombo);

        if (JOptionPane.showConfirmDialog(this, panel, "创建隧道", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Map<String, String> tunnel = new HashMap<>();
            tunnel.put("name", nameField.getText());
            tunnel.put("node", (String) nodeCombo.getSelectedItem());
            tunnel.put("localPort", localPortField.getText());
            tunnel.put("remotePort", remotePortField.getText());
            tunnel.put("protocol", (String) protoCombo.getSelectedItem());
            tunnel.put("status", "未连接");
            tunnel.put("publicAddr", "-");
            tunnels.add(tunnel);
            saveTunnels();
            refreshTunnelTable();
            appendLog("[系统] 创建隧道: " + tunnel.get("name") + " @ " + tunnel.get("node"));
        }
    }

    private void connectTunnel(int row) {
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选择隧道"); return; }
        Map<String, String> tunnel = tunnels.get(row);
        tunnel.put("status", "连接中");
        refreshTunnelTable();
        appendLog("[连接] 正在连接 " + tunnel.get("name") + " -> " + tunnel.get("node"));

        // Simulate connection
        javax.swing.Timer timer = new javax.swing.Timer(1500, e -> {
            tunnel.put("status", "已连接");
            String addr = tunnel.get("node").replace("-01", "").toLowerCase() + ".chmlfrp.cn:" + tunnel.get("remotePort");
            tunnel.put("publicAddr", addr);
            statusLabel.setText("已连接");
            statusLabel.setForeground(Color.GREEN);
            refreshTunnelTable();
            appendLog("[连接] 隧道已建立! 公网地址: " + addr);
            appendLog("[连接] 其他玩家可通过 " + addr + " 连接服务器");
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void disconnectTunnel(int row) {
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选择隧道"); return; }
        Map<String, String> tunnel = tunnels.get(row);
        tunnel.put("status", "未连接");
        tunnel.put("publicAddr", "-");
        statusLabel.setText("未连接");
        statusLabel.setForeground(Color.ORANGE);
        refreshTunnelTable();
        appendLog("[断开] 隧道 " + tunnel.get("name") + " 已断开");
    }

    private void deleteTunnel(int row) {
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选择隧道"); return; }
        String name = tunnels.get(row).get("name");
        if (JOptionPane.showConfirmDialog(this, "确定删除隧道 " + name + " ?", "确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            tunnels.remove(row);
            saveTunnels();
            refreshTunnelTable();
            appendLog("[系统] 删除隧道: " + name);
        }
    }

    private void refreshTunnelTable() {
        tunnelModel.setRowCount(0);
        for (Map<String, String> t : tunnels) {
            tunnelModel.addRow(new Object[]{
                t.get("name"), t.get("node"), t.get("localPort"),
                t.get("remotePort"), t.get("protocol"), t.get("status"), t.get("publicAddr")
            });
        }
    }

    private void appendLog(String msg) {
        logArea.append("[" + new Date().toString().substring(11, 19) + "] " + msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        if (field instanceof JTextField) ((JTextField) field).setColumns(15);
        panel.add(field, gbc);
        gbc.fill = GridBagConstraints.NONE;
    }

    private void loadTunnels() {
        String file = System.getProperty("user.home") + File.separator + ".mcmanager" + File.separator + "chmlfrp_tunnels.properties";
        File f = new File(file);
        if (!f.exists()) return;
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(f));
            int count = Integer.parseInt(props.getProperty("tunnel.count", "0"));
            for (int i = 0; i < count; i++) {
                String p = "tunnel." + i + ".";
                Map<String, String> t = new HashMap<>();
                t.put("name", props.getProperty(p + "name", ""));
                t.put("node", props.getProperty(p + "node", ""));
                t.put("localPort", props.getProperty(p + "localPort", ""));
                t.put("remotePort", props.getProperty(p + "remotePort", ""));
                t.put("protocol", props.getProperty(p + "protocol", "TCP"));
                t.put("status", "未连接");
                t.put("publicAddr", "-");
                tunnels.add(t);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveTunnels() {
        String file = System.getProperty("user.home") + File.separator + ".mcmanager" + File.separator + "chmlfrp_tunnels.properties";
        try {
            new File(file).getParentFile().mkdirs();
            Properties props = new Properties();
            props.setProperty("tunnel.count", String.valueOf(tunnels.size()));
            for (int i = 0; i < tunnels.size(); i++) {
                String p = "tunnel." + i + ".";
                Map<String, String> t = tunnels.get(i);
                props.setProperty(p + "name", t.get("name"));
                props.setProperty(p + "node", t.get("node"));
                props.setProperty(p + "localPort", t.get("localPort"));
                props.setProperty(p + "remotePort", t.get("remotePort"));
                props.setProperty(p + "protocol", t.get("protocol"));
            }
            props.store(new FileOutputStream(file), "ChmlFRP Tunnels");
        } catch (Exception e) { e.printStackTrace(); }
    }
}
