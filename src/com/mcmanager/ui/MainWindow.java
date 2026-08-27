package com.mcmanager.ui;

import com.mcmanager.core.*;
import com.mcmanager.ssh.SSHClient;
import com.mcmanager.util.ConfigStorage;
import com.mcmanager.util.ThemeManager;
import com.mcmanager.util.FontUtil;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class MainWindow extends JFrame {
    private List<ServerConfig> servers = new ArrayList<>();
    private Map<String, MCServerProcess> processes = new HashMap<>();
    private Map<String, SSHClient> sshClients = new HashMap<>();
    private ServerConfig currentServer;

    private DefaultListModel<ServerConfig> serverListModel;
    private JList<ServerConfig> serverList;
    private JTabbedPane tabbedPane;
    private JTextArea consoleArea;
    private JTextField commandField;
    private JTree fileTree;
    private DefaultTableModel fileTableModel;
    private JTable fileTable;
    private JLabel statusLabel;
    private String currentDir = "";

    // RCON
    private com.mcmanager.core.RconClient rcon;
    private ModManagerPanel modManagerPanel;
    private PlayerManagerPanel playerManagerPanel;
    private JTextField rconHostField;
    private JTextField rconPortField;
    private JPasswordField rconPassField;
    private JButton rconConnectBtn;
    private JLabel rconStatusLabel;

    public MainWindow() {
        super("MC 服务器管理工具 - Java 版");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Default 50% window opacity
        try { setOpacity(0.5f); } catch (Exception e) {}

        servers = ConfigStorage.loadServers();
        initUI();
        updateServerList();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                saveAll();
                for (MCServerProcess p : processes.values()) p.kill();
                for (SSHClient c : sshClients.values()) c.disconnect();
            }
        });
    }

    private void initUI() {
        ThemeManager tm = ThemeManager.getInstance();
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(250);

        // Left panel: server list (with collapse support)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(tm.bgSecondary());

        // Header with collapse button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(tm.bgSecondary());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, tm.border()));
        JLabel headerLabel = new JLabel("  📋 服务器列表");
        headerLabel.setFont(FontUtil.getFont(Font.BOLD, 14));
        headerLabel.setForeground(tm.textPrimary());
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        JButton collapseBtn = new JButton("◀");
        collapseBtn.setFont(FontUtil.getFont(Font.PLAIN, 10));
        collapseBtn.setFocusPainted(false);
        collapseBtn.setBorderPainted(false);
        collapseBtn.setContentAreaFilled(false);
        collapseBtn.setForeground(tm.textSecondary());
        collapseBtn.setToolTipText("折叠/展开服务器列表");
        headerPanel.add(collapseBtn, BorderLayout.EAST);
        leftPanel.add(headerPanel, BorderLayout.NORTH);

        serverListModel = new DefaultListModel<>();
        serverList = new JList<>(serverListModel);
        serverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverList.setBackground(tm.bgSecondary());
        serverList.setForeground(tm.textPrimary());
        serverList.setSelectionBackground(tm.accent());
        serverList.setSelectionForeground(Color.WHITE);
        serverList.setBorder(null);
        serverList.setFixedCellHeight(56);
        // Custom renderer showing icon + name + version + core
        serverList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                ServerConfig server = (ServerConfig) value;
                JPanel panel = new JPanel(new BorderLayout(8, 0));
                panel.setBackground(isSelected ? tm.accent() : tm.bgSecondary());
                panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

                // Icon
                JLabel iconLabel = new JLabel();
                iconLabel.setPreferredSize(new Dimension(40, 40));
                iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
                if (server.getIconPath() != null && new File(server.getIconPath()).exists()) {
                    try {
                        ImageIcon icon = new ImageIcon(server.getIconPath());
                        Image img = icon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                        iconLabel.setIcon(new ImageIcon(img));
                    } catch (Exception e) {
                        iconLabel.setText("📦");
                        iconLabel.setFont(FontUtil.getFont(Font.PLAIN, 20));
                    }
                } else {
                    iconLabel.setText("📦");
                    iconLabel.setFont(FontUtil.getFont(Font.PLAIN, 20));
                }
                iconLabel.setForeground(isSelected ? Color.WHITE : tm.textPrimary());
                panel.add(iconLabel, BorderLayout.WEST);

                // Text info
                JPanel textPanel = new JPanel();
                textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
                textPanel.setBackground(isSelected ? tm.accent() : tm.bgSecondary());
                textPanel.setOpaque(false);

                JLabel nameLabel = new JLabel(server.getName());
                nameLabel.setFont(FontUtil.getFont(Font.BOLD, 13));
                nameLabel.setForeground(isSelected ? Color.WHITE : tm.textPrimary());
                nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                String version = server.getMcVersion() != null ? server.getMcVersion() : "?";
                String core = server.getCoreType() != null ? server.getCoreType() : "?";
                JLabel infoLabel = new JLabel(version + " | " + core);
                infoLabel.setFont(FontUtil.getFont(Font.PLAIN, 10));
                infoLabel.setForeground(isSelected ? new Color(220, 220, 220) : tm.textSecondary());
                infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                textPanel.add(nameLabel);
                textPanel.add(Box.createVerticalStrut(2));
                textPanel.add(infoLabel);
                panel.add(textPanel, BorderLayout.CENTER);

                return panel;
            }
        });
        serverList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ServerConfig selected = serverList.getSelectedValue();
                if (selected != null) selectServer(selected);
            }
        });
        JScrollPane serverScroll = new JScrollPane(serverList);
        serverScroll.setBorder(null);
        leftPanel.add(serverScroll, BorderLayout.CENTER);

        // Collapse logic
        final int[] expandedWidth = {250};
        final boolean[] collapsed = {false};
        collapseBtn.addActionListener(e -> {
            collapsed[0] = !collapsed[0];
            if (collapsed[0]) {
                expandedWidth[0] = mainSplit.getDividerLocation();
                mainSplit.setDividerLocation(60);
                collapseBtn.setText("▶");
                serverList.setFixedCellHeight(50);
            } else {
                mainSplit.setDividerLocation(expandedWidth[0]);
                collapseBtn.setText("◀");
                serverList.setFixedCellHeight(56);
            }
        });

        JPanel serverBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        serverBtnPanel.setBackground(tm.bgSecondary());
        JButton addBtn = new JButton("添加");
        addBtn.addActionListener(e -> addServer());
        JButton editBtn = new JButton("编辑");
        editBtn.addActionListener(e -> editServer());
        JButton delBtn = new JButton("删除");
        delBtn.addActionListener(e -> deleteServer());
        JButton refreshBtn = new JButton("刷新");
        refreshBtn.addActionListener(e -> updateServerList());
        serverBtnPanel.add(addBtn);
        serverBtnPanel.add(editBtn);
        serverBtnPanel.add(delBtn);
        serverBtnPanel.add(refreshBtn);
        leftPanel.add(serverBtnPanel, BorderLayout.SOUTH);
        mainSplit.setLeftComponent(leftPanel);

        // Right panel: tabbed pane
        UIManager.put("TabbedPane.background", tm.bgSecondary());
        UIManager.put("TabbedPane.foreground", tm.textPrimary());
        UIManager.put("TabbedPane.selected", tm.bgCard());
        UIManager.put("TabbedPane.selectHighlight", tm.accent());
        UIManager.put("TabbedPane.contentAreaColor", tm.bgPrimary());
        UIManager.put("TabbedPane.darkShadow", tm.border());
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FontUtil.getFont(Font.PLAIN, 13));
        tabbedPane.setBackground(tm.bgSecondary());
        tabbedPane.setForeground(tm.textPrimary());

        tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        JPanel tutorialPanel = createTutorialPanel();
        modManagerPanel = new ModManagerPanel();
        playerManagerPanel = new PlayerManagerPanel();
        playerManagerPanel.setCommandSender(cmd -> {
            if (rcon != null && rcon.isConnected()) {
                try {
                    String resp = rcon.sendCommand(cmd);
                    consoleArea.append("[玩家管理] " + cmd + "\n" + resp + "\n");
                } catch (Exception e) {
                    consoleArea.append("[玩家管理错误] " + e.getMessage() + "\n");
                }
            } else if (currentServer != null) {
                MCServerProcess proc = processes.get(currentServer.getId());
                if (proc != null && proc.isRunning()) {
                    try {
                        proc.sendCommand(cmd);
                        consoleArea.append("[玩家管理] " + cmd + "\n");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "命令发送失败: " + e.getMessage());
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "服务器未运行，请先启动服务器或连接 RCON");
                }
            }
        });
        String[] tabTitles = {"主页", "🖥️ 控制台", "📁 文件管理", "📝 服务器设置", "📦 资源中心", "🔍 模组下载", "📦 模组管理", "👥 玩家管理", "💾 备份管理", "🤖 AI日志分析", "🌐 内网穿透", "🔗 远程连接", "👥 用户管理", "⚙️ 系统设置"};
        tabbedPane.addTab(tabTitles[0], tutorialPanel);
        tabbedPane.addTab(tabTitles[1], createConsolePanel());
        tabbedPane.addTab(tabTitles[2], createFilePanel());
        tabbedPane.addTab(tabTitles[3], createServerSettingsPanel());
        tabbedPane.addTab(tabTitles[4], new ResourceCenterPanel());
        tabbedPane.addTab(tabTitles[5], new ModDownloadPanel());
        tabbedPane.addTab(tabTitles[6], modManagerPanel);
        tabbedPane.addTab(tabTitles[7], playerManagerPanel);
        tabbedPane.addTab(tabTitles[8], new BackupPanel());
        tabbedPane.addTab(tabTitles[9], new AILogAnalysisPanel());
        tabbedPane.addTab(tabTitles[10], new ChmlFrpPanel());
        tabbedPane.addTab(tabTitles[11], new RemoteConnectPanel());
        tabbedPane.addTab(tabTitles[12], new UserManagerPanel());
        SettingsPanel settingsPanel = new SettingsPanel();
        settingsPanel.setOnSettingsApplied(() -> {
            // Apply settings immediately by recreating the window (100% reliable)
            SwingUtilities.invokeLater(() -> {
                Rectangle bounds = getBounds();
                int selectedIndex = serverList.getSelectedIndex();
                dispose();
                MainWindow newWindow = new MainWindow();
                newWindow.setBounds(bounds);
                if (selectedIndex >= 0 && selectedIndex < newWindow.serverListModel.size()) {
                    newWindow.serverList.setSelectedIndex(selectedIndex);
                }
                newWindow.setVisible(true);
            });
        });
        tabbedPane.addTab(tabTitles[13], settingsPanel);

        // Set custom tab components for highlight effect
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setTabComponentAt(i, createTabLabel(tabTitles[i], i == 0));
        }
        tabbedPane.addChangeListener(e -> updateTabStyles());

        // Make tabbedPane transparent so background image shows through
        tabbedPane.setOpaque(false);
        try {
            tabbedPane.setUI(new javax.swing.plaf.metal.MetalTabbedPaneUI() {
                @Override
                protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                    // Don't paint opaque content border
                }
            });
        } catch (Exception e) {}

        // Wrap in background image panel
        JPanel bgPanel = createBackgroundPanel();
        bgPanel.setLayout(new BorderLayout());
        bgPanel.add(tabbedPane, BorderLayout.CENTER);
        mainSplit.setRightComponent(bgPanel);

        add(mainSplit, BorderLayout.CENTER);

        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        statusLabel.setForeground(tm.textSecondary());
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createConsolePanel() {
        ThemeManager tm = ThemeManager.getInstance();
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JToolBar consoleToolbar = new JToolBar();
        consoleToolbar.setFloatable(false);
        JButton startBtn = new JButton("启动");
        startBtn.addActionListener(e -> startServer());
        JButton stopBtn = new JButton("停止");
        stopBtn.addActionListener(e -> stopServer());
        JButton restartBtn = new JButton("🔄 重启");
        restartBtn.setBackground(new Color(200, 140, 50));
        restartBtn.setForeground(Color.WHITE);
        restartBtn.addActionListener(e -> restartServer());
        JButton clearBtn = new JButton("清屏");
        clearBtn.addActionListener(e -> consoleArea.setText(""));
        consoleToolbar.add(startBtn);
        consoleToolbar.add(stopBtn);
        consoleToolbar.add(restartBtn);
        consoleToolbar.addSeparator();
        consoleToolbar.add(clearBtn);
        topPanel.add(consoleToolbar);

        // RCON bar
        JPanel rconBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        rconBar.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(tm.border()), "RCON 远程控制台"));
        rconBar.setBackground(tm.bgCard());
        rconBar.add(new JLabel("主机:"));
        rconHostField = new JTextField("127.0.0.1", 10);
        rconBar.add(rconHostField);
        rconBar.add(new JLabel("端口:"));
        rconPortField = new JTextField("25575", 5);
        rconBar.add(rconPortField);
        rconBar.add(new JLabel("密码:"));
        rconPassField = new JPasswordField(10);
        rconBar.add(rconPassField);
        rconConnectBtn = new JButton("连接 RCON");
        rconConnectBtn.setBackground(tm.success());
        rconConnectBtn.setForeground(Color.WHITE);
        rconConnectBtn.setFocusPainted(false);
        rconConnectBtn.addActionListener(e -> toggleRcon());
        rconBar.add(rconConnectBtn);
        rconStatusLabel = new JLabel("未连接");
        rconStatusLabel.setForeground(tm.textSecondary());
        rconBar.add(rconStatusLabel);
        topPanel.add(rconBar);

        panel.add(topPanel, BorderLayout.NORTH);

        consoleArea = new JTextArea();
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        consoleArea.setEditable(false);
        consoleArea.setBackground(new Color(30, 30, 30));
        consoleArea.setForeground(new Color(200, 200, 200));
        consoleArea.setCaretColor(Color.WHITE);
        panel.add(new JScrollPane(consoleArea), BorderLayout.CENTER);

        JPanel cmdPanel = new JPanel(new BorderLayout());
        cmdPanel.setBorder(BorderFactory.createTitledBorder("命令输入 (RCON已连接时通过RCON发送)"));
        commandField = new JTextField();
        commandField.addActionListener(e -> sendCommand());
        JButton sendBtn = new JButton("发送");
        sendBtn.addActionListener(e -> sendCommand());
        cmdPanel.add(commandField, BorderLayout.CENTER);
        cmdPanel.add(sendBtn, BorderLayout.EAST);
        panel.add(cmdPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createFilePanel() {
        ThemeManager tm = ThemeManager.getInstance();
        JPanel panel = new JPanel(new BorderLayout());
        JSplitPane fileSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        fileSplit.setDividerLocation(250);

        fileTree = new JTree(new DefaultMutableTreeNode("加载中..."));
        fileTree.setBackground(tm.bgSecondary());
        fileTree.setForeground(tm.textPrimary());
        fileSplit.setLeftComponent(new JScrollPane(fileTree));

        String[] cols = {"名称", "类型", "大小", "修改时间"};
        fileTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        fileTable = new JTable(fileTableModel);
        fileTable.setBackground(tm.bgCard());
        fileTable.setForeground(tm.textPrimary());
        fileTable.setSelectionBackground(tm.accent());
        fileTable.setSelectionForeground(Color.WHITE);
        fileTable.setRowHeight(28);
        fileTable.getColumnModel().getColumn(1).setMaxWidth(80);
        fileTable.getColumnModel().getColumn(2).setMaxWidth(100);
        // Custom renderer for name column with icons
        fileTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String name = value != null ? value.toString() : "";
                String icon = getFileIcon(name);
                setText(icon + "  " + name);
                setFont(FontUtil.getFont(Font.PLAIN, 13));
                return c;
            }
        });
        fileSplit.setRightComponent(new JScrollPane(fileTable));

        JPanel fileOps = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileOps.setBackground(tm.bgPrimary());
        JButton uploadBtn = new JButton("上传");
        JButton downloadBtn = new JButton("下载");
        JButton deleteFileBtn = new JButton("删除");
        JButton refreshFileBtn = new JButton("刷新");
        refreshFileBtn.addActionListener(e -> refreshFileList());
        fileOps.add(uploadBtn);
        fileOps.add(downloadBtn);
        fileOps.add(deleteFileBtn);
        fileOps.add(refreshFileBtn);

        panel.add(fileSplit, BorderLayout.CENTER);
        panel.add(fileOps, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createServerSettingsPanel() {
        ThemeManager tm = ThemeManager.getInstance();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(tm.bgPrimary());
        JLabel label = new JLabel("  选择服务器后在此编辑配置（核心类型、版本、内存、Java路径等）");
        label.setForeground(tm.textSecondary());
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTutorialPanel() {
        ThemeManager tm = ThemeManager.getInstance();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(tm.bgPrimary());

        JLabel title = new JLabel("📖 MC 服务器管理工具 - 使用教程");
        title.setFont(FontUtil.getFont(Font.BOLD, 24));
        title.setForeground(tm.textPrimary());
        title.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        panel.add(title, BorderLayout.NORTH);

        JTextPane tutorial = new JTextPane();
        tutorial.setContentType("text/html");
        tutorial.setEditable(false);
        tutorial.setBackground(tm.bgCard());
        tutorial.setForeground(tm.textPrimary());
        tutorial.setFont(FontUtil.getFont(Font.PLAIN, 14));
        String html = "<html><body style='font-family: sans-serif; font-size: 14px; padding: 20px; color: " +
            String.format("#%06x", tm.textPrimary().getRGB() & 0xFFFFFF) + ";'>" +
            "<h2 style='color: " + String.format("#%06x", tm.accent().getRGB() & 0xFFFFFF) + ";'>快速开始</h2>" +
            "<ol>" +
            "<li><b>添加服务器</b>：点击左侧「添加」按钮，填写服务器名称、目录、Java路径等信息</li>" +
            "<li><b>安装核心</b>：进入「资源中心」，选择核心类型（Vanilla/Paper/Forge/Fabric/NeoForge）和版本下载安装</li>" +
            "<li><b>启动服务器</b>：在「控制台」页面点击「启动」按钮，查看实时日志</li>" +
            "<li><b>连接游戏</b>：使用服务器IP和端口（默认25565）连接游戏</li>" +
            "</ol>" +
            "<h2 style='color: " + String.format("#%06x", tm.accent().getRGB() & 0xFFFFFF) + ";'>功能说明</h2>" +
            "<ul>" +
            "<li><b>🖥️ 控制台</b>：查看服务器实时日志，输入命令，支持RCON远程控制台</li>" +
            "<li><b>📁 文件管理</b>：浏览、上传、下载、删除服务器文件</li>" +
            "<li><b>📝 服务器设置</b>：编辑server.properties等配置文件</li>" +
            "<li><b>📦 资源中心</b>：下载安装服务器核心（Vanilla/Paper/Forge/Fabric/NeoForge）</li>" +
            "<li><b>🔍 模组下载</b>：从Modrinth搜索下载模组和插件</li>" +
            "<li><b>💾 备份管理</b>：创建和恢复服务器备份</li>" +
            "<li><b>🤖 AI日志分析</b>：自动分析服务器日志，诊断问题</li>" +
            "<li><b>🌐 内网穿透</b>：使用ChmlFRP将本地服务器映射到公网</li>" +
            "<li><b>🔗 远程连接</b>：通过SSH连接远程服务器管理</li>" +
            "<li><b>👥 用户管理</b>：管理服务器用户，一键穿透分享</li>" +
            "<li><b>⚙️ 系统设置</b>：Java路径、内存、主题、背景图片、语言等设置</li>" +
            "</ul>" +
            "<h2 style='color: " + String.format("#%06x", tm.accent().getRGB() & 0xFFFFFF) + ";'>常见问题</h2>" +
            "<ul>" +
            "<li><b>启动失败？</b>：检查Java路径是否正确，服务器目录是否有核心jar文件或run.bat</li>" +
            "<li><b>无法连接？</b>：检查服务器是否启动，端口是否开放，防火墙是否放行</li>" +
            "<li><b>RCON连不上？</b>：确认server.properties中enable-rcon=true，rcon.password和rcon.port配置正确</li>" +
            "<li><b>字体显示方框？</b>：系统缺少中文字体，程序会自动检测可用字体，安卓请安装Termux + openjdk-17</li>" +
            "</ul>" +
            "<h2 style='color: " + String.format("#%06x", tm.accent().getRGB() & 0xFFFFFF) + ";'>作者信息</h2>" +
            "<p>作者：<b>Dfhcg</b> &nbsp;|&nbsp; QQ：<b>3565304421</b></p>" +
            "</body></html>";
        tutorial.setText(html);
        JScrollPane scroll = new JScrollPane(tutorial);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(tm.bgCard());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createTabLabel(String text, boolean selected) {
        ThemeManager tm = ThemeManager.getInstance();
        int tabSize = tm.getTabFontSize();
        JLabel label = new JLabel(text);
        label.setFont(FontUtil.getFont(selected ? Font.BOLD : Font.PLAIN, tabSize));
        label.setForeground(selected ? Color.WHITE : tm.textPrimary());
        label.setOpaque(true);
        label.setBackground(selected ? tm.accent() : tm.bgSecondary());
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, selected ? 3 : 0, 0, selected ? tm.accent() : tm.border()),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        return label;
    }

    private void updateTabStyles() {
        ThemeManager tm = ThemeManager.getInstance();
        int tabSize = tm.getTabFontSize();
        int selected = tabbedPane.getSelectedIndex();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getTabComponentAt(i);
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                boolean isSel = (i == selected);
                label.setFont(FontUtil.getFont(isSel ? Font.BOLD : Font.PLAIN, tabSize));
                label.setForeground(isSel ? Color.WHITE : tm.textPrimary());
                label.setBackground(isSel ? tm.accent() : tm.bgSecondary());
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, isSel ? 3 : 0, 0, isSel ? tm.accent() : tm.border()),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }
        }
    }

    private JPanel createBackgroundPanel() {
        ThemeManager tm = ThemeManager.getInstance();
        JPanel panel = new JPanel() {
            private Image bgImage = null;
            {
                String imgPath = tm.getBackgroundImage();
                if (imgPath != null && !imgPath.isEmpty() && new File(imgPath).exists()) {
                    try {
                        bgImage = javax.imageio.ImageIO.read(new File(imgPath));
                    } catch (Exception e) { bgImage = null; }
                }
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, tm.getBackgroundOpacity()));
                    g2d.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                    g2d.dispose();
                }
            }
        };
        panel.setBackground(tm.bgPrimary());
        return panel;
    }

    private void startServer() {
        if (currentServer == null) {
            JOptionPane.showMessageDialog(this, "请先选择服务器");
            return;
        }
        try {
            MCServerProcess process = new MCServerProcess(currentServer);
            process.setLogListener(line -> SwingUtilities.invokeLater(() -> {
                consoleArea.append(line + "\n");
                consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
            }));
            process.start();
            processes.put(currentServer.getId(), process);
            statusLabel.setText("服务器已启动: " + currentServer.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "启动失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (currentServer == null) return;
        MCServerProcess process = processes.get(currentServer.getId());
        if (process != null && process.isRunning()) {
            process.stop();
            statusLabel.setText("服务器已停止: " + currentServer.getName());
        }
    }

    private void restartServer() {
        if (currentServer == null) {
            JOptionPane.showMessageDialog(this, "请先选择服务器");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "确定重启服务器 " + currentServer.getName() + " ?\n\n服务器将先停止，3秒后自动重新启动。",
            "确认重启", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        statusLabel.setText("正在重启服务器...");
        consoleArea.append("[系统] 正在重启服务器...\n");

        // Stop first
        MCServerProcess process = processes.get(currentServer.getId());
        if (process != null && process.isRunning()) {
            process.stop();
        }

        // Wait then start
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                SwingUtilities.invokeLater(() -> {
                    try {
                        MCServerProcess newProcess = new MCServerProcess(currentServer);
                        newProcess.setLogListener(line -> SwingUtilities.invokeLater(() -> {
                            consoleArea.append(line + "\n");
                            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                        }));
                        newProcess.start();
                        processes.put(currentServer.getId(), newProcess);
                        statusLabel.setText("服务器已重启: " + currentServer.getName());
                        consoleArea.append("[系统] 服务器重启完成\n");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "重启失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void sendCommand() {
        String cmd = commandField.getText().trim();
        if (cmd.isEmpty()) return;
        if (rcon != null && rcon.isConnected()) {
            try {
                String resp = rcon.sendCommand(cmd);
                consoleArea.append("[RCON] " + cmd + "\n" + resp + "\n");
            } catch (Exception e) {
                consoleArea.append("[RCON错误] " + e.getMessage() + "\n");
            }
            commandField.setText("");
            return;
        }
        if (currentServer == null) {
            JOptionPane.showMessageDialog(this, "请先选择服务器，或连接 RCON");
            return;
        }
        MCServerProcess process = processes.get(currentServer.getId());
        if (process != null && process.isRunning()) {
            try {
                process.sendCommand(cmd);
                consoleArea.append("[命令] " + cmd + "\n");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "发送失败: " + e.getMessage());
            }
        } else if ("ssh".equals(currentServer.getType())) {
            try {
                SSHClient ssh = getSSHClient();
                String output = ssh.execCommand("cd " + currentServer.getServerDir() + " && " + cmd);
                consoleArea.append("[SSH命令] " + cmd + "\n" + output + "\n");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "SSH命令失败: " + e.getMessage());
            }
        }
        commandField.setText("");
    }

    private void toggleRcon() {
        ThemeManager tm = ThemeManager.getInstance();
        if (rcon != null && rcon.isConnected()) {
            rcon.disconnect();
            rcon = null;
            rconConnectBtn.setText("连接 RCON");
            rconConnectBtn.setBackground(tm.success());
            rconStatusLabel.setText("未连接");
            rconStatusLabel.setForeground(tm.textSecondary());
            consoleArea.append("[RCON] 已断开连接\n");
            return;
        }
        String host = rconHostField.getText().trim();
        int port;
        try { port = Integer.parseInt(rconPortField.getText().trim()); }
        catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "端口必须是数字"); return; }
        String pass = new String(rconPassField.getPassword());
        if (pass.isEmpty()) { JOptionPane.showMessageDialog(this, "请输入 RCON 密码"); return; }
        try {
            rconStatusLabel.setText("连接中...");
            rconStatusLabel.setForeground(new Color(200, 200, 100));
            rcon = new com.mcmanager.core.RconClient(host, port, pass);
            rcon.connect();
            rconConnectBtn.setText("断开 RCON");
            rconConnectBtn.setBackground(new Color(160, 60, 60));
            rconStatusLabel.setText("已连接 " + host + ":" + port);
            rconStatusLabel.setForeground(new Color(100, 220, 100));
            consoleArea.append("[RCON] 已连接到 " + host + ":" + port + "\n");
            try {
                String resp = rcon.sendCommand("list");
                consoleArea.append("[RCON] list -> " + resp + "\n");
            } catch (Exception e) {
                consoleArea.append("[RCON] 测试命令失败: " + e.getMessage() + "\n");
            }
        } catch (Exception e) {
            rconStatusLabel.setText("连接失败");
            rconStatusLabel.setForeground(new Color(255, 100, 100));
            JOptionPane.showMessageDialog(this, "RCON 连接失败: " + e.getMessage() + "\n\n请确认：\n1. 服务器已启动\n2. server.properties 中 enable-rcon=true\n3. rcon.port 和 rcon.password 配置正确", "RCON 错误", JOptionPane.ERROR_MESSAGE);
            rcon = null;
        }
    }

    private void selectServer(ServerConfig server) {
        currentServer = server;
        statusLabel.setText("已选择: " + server.getName() + " (" + server.getType() + ")");
        currentDir = server.getServerDir();
        refreshFileList();
        if (modManagerPanel != null) {
            modManagerPanel.setModsDir(server.getServerDir() + File.separator + "mods");
        }
        if (playerManagerPanel != null) {
            playerManagerPanel.setServer(server);
        }
        MCServerProcess process = processes.get(server.getId());
        if (process != null) {
            consoleArea.setText("");
            for (String line : process.getLogs()) consoleArea.append(line + "\n");
        }
    }

    private void refreshFileList() {
        if (currentDir == null || currentDir.isEmpty()) return;
        File dir = new File(currentDir);
        if (!dir.exists() || !dir.isDirectory()) return;
        fileTableModel.setRowCount(0);
        File[] files = dir.listFiles();
        if (files != null) {
            // Sort: directories first, then files
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            for (File f : files) {
                fileTableModel.addRow(new Object[]{
                    f.getName(),
                    f.isDirectory() ? "文件夹" : getFileType(f.getName()),
                    f.isDirectory() ? "<DIR>" : formatFileSize(f.length()),
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(f.lastModified()))
                });
            }
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(currentDir);
        buildTree(root, dir);
        fileTree.setModel(new DefaultTreeModel(root));
    }

    private String getFileIcon(String fileName) {
        String lower = fileName.toLowerCase();
        if (new File(currentDir, fileName).isDirectory()) return "📁";
        if (lower.endsWith(".jar")) return "📦";
        if (lower.endsWith(".properties") || lower.endsWith(".yml") || lower.endsWith(".yaml") ||
            lower.endsWith(".json") || lower.endsWith(".cfg") || lower.endsWith(".conf") ||
            lower.endsWith(".ini") || lower.endsWith(".toml")) return "⚙️";
        if (lower.endsWith(".txt") || lower.endsWith(".log")) return "📄";
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp")) return "🖼️";
        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z") ||
            lower.endsWith(".tar") || lower.endsWith(".gz") || lower.endsWith(".bz2")) return "🗜️";
        if (lower.endsWith(".bat") || lower.endsWith(".sh") || lower.endsWith(".cmd") ||
            lower.endsWith(".ps1") || lower.endsWith(".exe")) return "▶️";
        if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".xml")) return "🌐";
        if (lower.endsWith(".class") || lower.endsWith(".java")) return "☕";
        return "📄";
    }

    private String getFileType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jar")) return "模组/JAR";
        if (lower.endsWith(".properties")) return "配置";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "YAML配置";
        if (lower.endsWith(".json")) return "JSON配置";
        if (lower.endsWith(".cfg") || lower.endsWith(".conf") || lower.endsWith(".ini")) return "配置文件";
        if (lower.endsWith(".toml")) return "TOML配置";
        if (lower.endsWith(".txt")) return "文本";
        if (lower.endsWith(".log")) return "日志";
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "图片";
        if (lower.endsWith(".gif") || lower.endsWith(".bmp")) return "图片";
        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z")) return "压缩包";
        if (lower.endsWith(".tar") || lower.endsWith(".gz")) return "压缩包";
        if (lower.endsWith(".bat") || lower.endsWith(".cmd")) return "批处理";
        if (lower.endsWith(".sh")) return "Shell脚本";
        if (lower.endsWith(".exe")) return "可执行文件";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "网页";
        if (lower.endsWith(".xml")) return "XML";
        if (lower.endsWith(".class") || lower.endsWith(".java")) return "Java";
        // Get extension
        int dot = lower.lastIndexOf('.');
        if (dot > 0 && dot < lower.length() - 1) {
            return lower.substring(dot + 1).toUpperCase() + " 文件";
        }
        return "文件";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void buildTree(DefaultMutableTreeNode node, File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(f.getName());
                node.add(child);
                if (node.getLevel() < 2) buildTree(child, f);
            }
        }
    }

    private void addServer() {
        ServerConfig config = new ServerConfig();
        config.setName("新服务器");
        config.setServerDir(System.getProperty("user.home") + "/minecraft-server");
        servers.add(config);
        saveAll();
        updateServerList();
    }

    private void editServer() {
        if (currentServer == null) {
            JOptionPane.showMessageDialog(this, "请先选择服务器");
            return;
        }
        ThemeManager tm = ThemeManager.getInstance();
        JTextField nameField = new JTextField(currentServer.getName());
        JTextField iconField = new JTextField(currentServer.getIconPath() != null ? currentServer.getIconPath() : "");
        JButton browseIconBtn = new JButton("浏览...");
        browseIconBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("图片文件", "png", "jpg", "jpeg", "gif", "bmp"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                iconField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.add(iconField, BorderLayout.CENTER);
        iconPanel.add(browseIconBtn, BorderLayout.EAST);
        JTextField dirField = new JTextField(currentServer.getServerDir());
        JButton searchDirBtn = new JButton("🔍 自动搜索");
        searchDirBtn.addActionListener(e -> {
            String found = showServerDirSearchDialog();
            if (found != null) dirField.setText(found);
        });
        JPanel dirPanel = new JPanel(new BorderLayout());
        dirPanel.add(dirField, BorderLayout.CENTER);
        dirPanel.add(searchDirBtn, BorderLayout.EAST);
        JTextField javaField = new JTextField(currentServer.getJavaPath());
        JTextField jarField = new JTextField(currentServer.getJarFile() != null ? currentServer.getJarFile() : "");
        JTextField maxMemField = new JTextField(String.valueOf(currentServer.getMaxMemory()));
        JTextField minMemField = new JTextField(String.valueOf(currentServer.getMinMemory()));

        // Core type and version (displayed in server list)
        JComboBox<String> coreTypeCombo = new JComboBox<>(new String[]{"Vanilla", "Paper", "Forge", "Fabric", "NeoForge", "Spigot", "Bukkit", "Purpur", "其他"});
        if (currentServer.getCoreType() != null) {
            coreTypeCombo.setSelectedItem(currentServer.getCoreType());
        }
        JTextField mcVersionField = new JTextField(currentServer.getMcVersion() != null ? currentServer.getMcVersion() : "1.20.1", 10);

        // SSH fields
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"本地服务器", "SSH 远程服务器"});
        typeCombo.setSelectedIndex("ssh".equals(currentServer.getType()) ? 1 : 0);
        JTextField hostField = new JTextField(currentServer.getHost() != null ? currentServer.getHost() : "");
        JTextField portField = new JTextField(String.valueOf(currentServer.getPort() > 0 ? currentServer.getPort() : 22));
        JTextField userField = new JTextField(currentServer.getUsername() != null ? currentServer.getUsername() : "");
        JPasswordField passField = new JPasswordField(currentServer.getPassword() != null ? currentServer.getPassword() : "");
        JTextField keyField = new JTextField(currentServer.getKeyFile() != null ? currentServer.getKeyFile() : "");
        JButton browseKeyBtn = new JButton("浏览...");
        browseKeyBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("选择 SSH 私钥文件");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                keyField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        JPanel sshPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        sshPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(tm.border()), "SSH 连接设置"));
        sshPanel.add(new JLabel("主机/IP:")); sshPanel.add(hostField);
        sshPanel.add(new JLabel("端口:")); sshPanel.add(portField);
        sshPanel.add(new JLabel("用户名:")); sshPanel.add(userField);
        sshPanel.add(new JLabel("密码:")); sshPanel.add(passField);
        sshPanel.add(new JLabel("私钥文件(可选):"));
        JPanel keyPanel = new JPanel(new BorderLayout());
        keyPanel.add(keyField, BorderLayout.CENTER);
        keyPanel.add(browseKeyBtn, BorderLayout.EAST);
        sshPanel.add(keyPanel);

        // Enable/disable SSH fields based on type
        typeCombo.addActionListener(e -> {
            boolean isSsh = typeCombo.getSelectedIndex() == 1;
            for (Component c : sshPanel.getComponents()) c.setEnabled(isSsh);
            keyPanel.setEnabled(isSsh);
            for (Component c : keyPanel.getComponents()) c.setEnabled(isSsh);
            // Disable local directory search for SSH servers
            searchDirBtn.setEnabled(!isSsh);
            searchDirBtn.setToolTipText(isSsh ? "SSH服务器请手动输入远程路径" : "自动搜索本地服务器目录");
            if (isSsh && dirField.getText().isEmpty()) {
                dirField.setText("/home/" + (userField.getText().isEmpty() ? "user" : userField.getText()) + "/minecraft-server");
            }
        });
        // Initial state
        boolean isSshInit = typeCombo.getSelectedIndex() == 1;
        for (Component c : sshPanel.getComponents()) c.setEnabled(isSshInit);
        keyPanel.setEnabled(isSshInit);
        for (Component c : keyPanel.getComponents()) c.setEnabled(isSshInit);
        searchDirBtn.setEnabled(!isSshInit);
        searchDirBtn.setToolTipText(isSshInit ? "SSH服务器请手动输入远程路径" : "自动搜索本地服务器目录");

        JPanel basicPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        basicPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(tm.border()), "基本设置"));
        basicPanel.add(new JLabel("服务器类型:")); basicPanel.add(typeCombo);
        basicPanel.add(new JLabel("名称:")); basicPanel.add(nameField);
        basicPanel.add(new JLabel("服务器图标:")); basicPanel.add(iconPanel);
        basicPanel.add(new JLabel("核心类型:")); basicPanel.add(coreTypeCombo);
        basicPanel.add(new JLabel("游戏版本:")); basicPanel.add(mcVersionField);
        JLabel dirLabel = new JLabel("服务器目录:");
        dirLabel.setToolTipText("本地服务器填本地路径，SSH服务器填远程路径");
        basicPanel.add(dirLabel); basicPanel.add(dirPanel);
        basicPanel.add(new JLabel("Java路径:")); basicPanel.add(javaField);
        basicPanel.add(new JLabel("核心Jar(留空自动搜索):")); basicPanel.add(jarField);
        basicPanel.add(new JLabel("最大内存(MB):")); basicPanel.add(maxMemField);
        basicPanel.add(new JLabel("最小内存(MB):")); basicPanel.add(minMemField);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(basicPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(sshPanel);

        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setPreferredSize(new Dimension(500, 520));
        scroll.setBorder(null);

        int result = JOptionPane.showConfirmDialog(this, scroll, "编辑服务器", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            currentServer.setName(nameField.getText());
            currentServer.setIconPath(iconField.getText().isEmpty() ? null : iconField.getText());
            currentServer.setCoreType((String) coreTypeCombo.getSelectedItem());
            currentServer.setMcVersion(mcVersionField.getText().trim());
            currentServer.setServerDir(dirField.getText());
            currentServer.setJavaPath(javaField.getText());
            currentServer.setJarFile(jarField.getText().isEmpty() ? null : jarField.getText());
            try {
                currentServer.setMaxMemory(Integer.parseInt(maxMemField.getText()));
                currentServer.setMinMemory(Integer.parseInt(minMemField.getText()));
            } catch (NumberFormatException e) {}
            // SSH settings
            boolean isSsh = typeCombo.getSelectedIndex() == 1;
            currentServer.setType(isSsh ? "ssh" : "local");
            if (isSsh) {
                currentServer.setHost(hostField.getText());
                try { currentServer.setPort(Integer.parseInt(portField.getText())); } catch (NumberFormatException e) { currentServer.setPort(22); }
                currentServer.setUsername(userField.getText());
                currentServer.setPassword(new String(passField.getPassword()));
                currentServer.setKeyFile(keyField.getText().isEmpty() ? null : keyField.getText());
            }
            saveAll();
            updateServerList();
            JOptionPane.showMessageDialog(this, "服务器已保存！\n\n类型: " + (isSsh ? "SSH 远程" : "本地") +
                (isSsh ? "\n主机: " + hostField.getText() + ":" + portField.getText() + "\n用户: " + userField.getText() : ""),
                "保存成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String showServerDirSearchDialog() {
        ThemeManager tm = ThemeManager.getInstance();
        JDialog dialog = new JDialog(this, "自动搜索服务器目录", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Top: search options
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        topPanel.setBackground(tm.bgPrimary());

        JLabel hint = new JLabel("选择搜索范围，程序会自动查找包含 server.properties / server.jar / run.bat 的目录");
        hint.setForeground(tm.textSecondary());
        topPanel.add(hint, BorderLayout.NORTH);

        JPanel searchRow = new JPanel(new BorderLayout(5, 0));
        searchRow.setBackground(tm.bgPrimary());
        JTextField customDirField = new JTextField(System.getProperty("user.home"));
        customDirField.setBackground(tm.bgCard());
        customDirField.setForeground(tm.textPrimary());
        JButton browseBtn = new JButton("浏览...");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                customDirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        searchRow.add(customDirField, BorderLayout.CENTER);
        searchRow.add(browseBtn, BorderLayout.EAST);
        topPanel.add(searchRow, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        btnRow.setBackground(tm.bgPrimary());
        JButton scanHomeBtn = new JButton("扫描用户目录");
        JButton scanCustomBtn = new JButton("扫描指定目录");
        JButton scanCommonBtn = new JButton("常见位置快速扫描");
        btnRow.add(scanHomeBtn);
        btnRow.add(scanCustomBtn);
        btnRow.add(scanCommonBtn);
        topPanel.add(btnRow, BorderLayout.SOUTH);
        dialog.add(topPanel, BorderLayout.NORTH);

        // Center: results list
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> resultList = new JList<>(listModel);
        resultList.setBackground(tm.bgCard());
        resultList.setForeground(tm.textPrimary());
        resultList.setSelectionBackground(tm.accent());
        resultList.setSelectionForeground(Color.WHITE);
        JScrollPane listScroll = new JScrollPane(resultList);
        listScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(tm.border()), "搜索结果"));
        dialog.add(listScroll, BorderLayout.CENTER);

        // Bottom: status + buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(tm.bgSecondary());
        JLabel statusLabel = new JLabel("  就绪");
        statusLabel.setForeground(tm.textSecondary());
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel okCancelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        okCancelPanel.setBackground(tm.bgSecondary());
        final String[] selected = {null};
        JButton okBtn = new JButton("选择");
        okBtn.setBackground(tm.accent());
        okBtn.setForeground(Color.WHITE);
        okBtn.addActionListener(e -> {
            selected[0] = resultList.getSelectedValue();
            if (selected[0] != null) dialog.dispose();
            else JOptionPane.showMessageDialog(dialog, "请先选择一个服务器目录");
        });
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dialog.dispose());
        okCancelPanel.add(okBtn);
        okCancelPanel.add(cancelBtn);
        bottomPanel.add(okCancelPanel, BorderLayout.EAST);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        // Search logic
        Runnable searchAction = (Runnable & java.io.Serializable) () -> {};
        java.util.function.Consumer<String> doSearch = (String startDir) -> {
            listModel.clear();
            statusLabel.setText("  正在搜索: " + startDir + " ...");
            new Thread(() -> {
                java.util.List<String> found = new java.util.ArrayList<>();
                searchServerDirs(new File(startDir), found, 0);
                SwingUtilities.invokeLater(() -> {
                    for (String s : found) listModel.addElement(s);
                    statusLabel.setText("  找到 " + found.size() + " 个服务器目录");
                });
            }).start();
        };

        scanHomeBtn.addActionListener(e -> doSearch.accept(System.getProperty("user.home")));
        scanCustomBtn.addActionListener(e -> doSearch.accept(customDirField.getText()));
        scanCommonBtn.addActionListener(e -> {
            listModel.clear();
            statusLabel.setText("  正在扫描常见位置...");
            new Thread(() -> {
                java.util.List<String> found = new java.util.ArrayList<>();
                String home = System.getProperty("user.home");
                String[] commonDirs = {
                    home + "/.minecraft",
                    home + "/minecraft",
                    home + "/servers",
                    home + "/Desktop",
                    home + "/Documents",
                    home + "/Downloads",
                    "C:/Users/Public/Documents",
                    "/opt/minecraft",
                    "/srv/minecraft",
                    "/var/minecraft"
                };
                for (String d : commonDirs) {
                    File f = new File(d);
                    if (f.exists() && f.isDirectory()) {
                        if (isServerDir(f)) found.add(f.getAbsolutePath());
                        searchServerDirs(f, found, 0);
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    for (String s : found) listModel.addElement(s);
                    statusLabel.setText("  找到 " + found.size() + " 个服务器目录");
                });
            }).start();
        });

        dialog.setVisible(true);
        return selected[0];
    }

    private boolean isServerDir(File dir) {
        if (!dir.isDirectory()) return false;
        String[] markers = {"server.properties", "server.jar", "run.bat", "start.bat", "run.sh", "start.sh"};
        for (String m : markers) {
            if (new File(dir, m).exists()) return true;
        }
        // Also check for mods folder + world folder
        if (new File(dir, "mods").isDirectory() && new File(dir, "world").isDirectory()) return true;
        return false;
    }

    private void searchServerDirs(File dir, java.util.List<String> results, int depth) {
        if (depth > 4 || !dir.isDirectory()) return;
        // Skip hidden and system dirs
        if (dir.getName().startsWith(".") && depth > 0) return;
        try {
            if (isServerDir(dir)) {
                results.add(dir.getAbsolutePath());
            }
            File[] children = dir.listFiles(File::isDirectory);
            if (children != null) {
                for (File child : children) {
                    // Skip large/system directories
                    String name = child.getName().toLowerCase();
                    if (name.equals("windows") || name.equals("program files") || name.equals("program files (x86)")
                        || name.equals("appdata") || name.equals("library") || name.equals("system32")
                        || name.equals("proc") || name.equals("sys") || name.equals("dev")) continue;
                    searchServerDirs(child, results, depth + 1);
                }
            }
        } catch (Exception e) {
            // Permission denied, skip
        }
    }

    private void deleteServer() {
        if (currentServer == null) return;
        if (JOptionPane.showConfirmDialog(this, "确定删除服务器 " + currentServer.getName() + " ?", "确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            servers.remove(currentServer);
            currentServer = null;
            saveAll();
            updateServerList();
        }
    }

    private void updateServerList() {
        serverListModel.clear();
        for (ServerConfig s : servers) serverListModel.addElement(s);
    }

    private SSHClient getSSHClient() throws Exception {
        if (currentServer == null) throw new Exception("未选择服务器");
        SSHClient ssh = sshClients.get(currentServer.getId());
        if (ssh == null || !ssh.isConnected()) {
            ssh = new SSHClient(currentServer.getHost(), currentServer.getPort(),
                currentServer.getUsername(), currentServer.getPassword(), currentServer.getKeyFile());
            ssh.connect();
            sshClients.put(currentServer.getId(), ssh);
        }
        return ssh;
    }

    private void saveAll() {
        ConfigStorage.saveServers(servers);
    }
}
