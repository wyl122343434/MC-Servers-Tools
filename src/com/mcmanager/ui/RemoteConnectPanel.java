package com.mcmanager.ui;
import com.mcmanager.ssh.SSHClient;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class RemoteConnectPanel extends JPanel {
    private SSHClient ssh;
    private boolean connected = false;
    private String currentRemoteDir = "/";

    // Login components
    private JTextField hostField;
    private JTextField portField;
    private JTextField userField;
    private JPasswordField passField;
    private JTextField keyFileField;
    private JLabel statusLabel;

    // Console components
    private JTextArea consoleArea;
    private JTextField cmdField;

    // File browser components
    private JTable fileTable;
    private DefaultTableModel fileModel;
    private JLabel pathLabel;

    public RemoteConnectPanel() {
        setLayout(new BorderLayout());
        initLoginUI();
    }

    private void initLoginUI() {
        removeAll();
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(22, 22, 34));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("远程连接 - SSH");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(new Color(220, 220, 240));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        loginPanel.add(title, gbc);

        JLabel hint = new JLabel("<html><center>支持 Linux / macOS / Windows(需开启OpenSSH)<br>连接后可远程管理服务器、查看控制台、传输文件</center></html>");
        hint.setForeground(new Color(160, 160, 180));
        gbc.gridy = 1;
        loginPanel.add(hint, gbc);

        gbc.gridwidth = 1;
        addField(loginPanel, gbc, 2, "主机IP:", hostField = new JTextField("192.168.1.100", 20));
        addField(loginPanel, gbc, 3, "端口:", portField = new JTextField("22", 10));
        addField(loginPanel, gbc, 4, "用户名:", userField = new JTextField("root", 20));
        addField(loginPanel, gbc, 5, "密码:", passField = new JPasswordField(20));

        JLabel keyLabel = new JLabel("密钥文件(可选):");
        keyLabel.setForeground(new Color(200, 200, 220));
        gbc.gridx = 0; gbc.gridy = 6;
        loginPanel.add(keyLabel, gbc);
        keyFileField = new JTextField(20);
        gbc.gridx = 1;
        loginPanel.add(keyFileField, gbc);
        JButton browseBtn = new JButton("浏览");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                keyFileField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        gbc.gridx = 2;
        loginPanel.add(browseBtn, gbc);

        JButton connectBtn = new JButton("连接");
        connectBtn.setFont(connectBtn.getFont().deriveFont(Font.BOLD, 14f));
        connectBtn.setBackground(new Color(60, 140, 80));
        connectBtn.setForeground(Color.WHITE);
        connectBtn.setPreferredSize(new Dimension(120, 35));
        connectBtn.addActionListener(e -> doConnect());
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(connectBtn, gbc);

        JButton sshSetupBtn = new JButton("🔧 一键开启本机 OpenSSH (Windows)");
        sshSetupBtn.setFont(sshSetupBtn.getFont().deriveFont(Font.PLAIN, 12f));
        sshSetupBtn.setBackground(new Color(50, 80, 130));
        sshSetupBtn.setForeground(Color.WHITE);
        sshSetupBtn.addActionListener(e -> setupOpenSSH());
        gbc.gridy = 8;
        loginPanel.add(sshSetupBtn, gbc);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(255, 150, 150));
        gbc.gridy = 9;
        loginPanel.add(statusLabel, gbc);

        add(loginPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setForeground(new Color(200, 200, 220));
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(l, gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(field, gbc);
        gbc.gridwidth = 1;
    }

    private void setupOpenSSH() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统。\nLinux/macOS 通常已内置 SSH 服务。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "将执行以下操作（需要管理员权限）：\n" +
            "1. 安装 OpenSSH Server 可选功能\n" +
            "2. 启动 sshd 服务\n" +
            "3. 设置 sshd 开机自启动\n" +
            "4. 在防火墙放行 22 端口\n\n" +
            "完成后其他设备可通过 SSH 连接本机。\n是否继续？",
            "一键开启 OpenSSH", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        statusLabel.setText("正在配置 OpenSSH，请稍候...");
        statusLabel.setForeground(new Color(200, 200, 100));

        new Thread(() -> {
            try {
                // Write PS script to temp file
                File tmpScript = File.createTempFile("openssh_setup", ".ps1");
                String script = 
                    "Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0\n" +
                    "Start-Service sshd\n" +
                    "Set-Service -Name sshd -StartupType Automatic\n" +
                    "New-NetFirewallRule -Name sshd -DisplayName 'OpenSSH Server (sshd)' -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22 -ErrorAction SilentlyContinue\n" +
                    "Write-Host 'OpenSSH setup complete!'\n";
                java.nio.file.Files.write(tmpScript.toPath(), script.getBytes("UTF-8"));
                // Run as admin
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command",
                    "Start-Process powershell -Verb RunAs -ArgumentList '-NoProfile -ExecutionPolicy Bypass -File " + tmpScript.getAbsolutePath() + "'");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
                final File scriptRef = tmpScript;
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("OpenSSH 配置命令已发送，请在弹出的管理员窗口中确认。");
                    statusLabel.setForeground(new Color(100, 220, 100));
                    JOptionPane.showMessageDialog(this,
                        "OpenSSH 配置命令已发送！\n\n" +
                        "如果弹出 UAC 管理员确认窗口，请点击「是」。\n" +
                        "配置完成后，本机 SSH 服务将在 22 端口运行。\n\n" +
                        "查看状态：PowerShell 运行 Get-Service sshd\n" +
                        "连接方式：ssh 用户名@本机IP",
                        "OpenSSH 配置", JOptionPane.INFORMATION_MESSAGE);
                    scriptRef.deleteOnExit();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("配置失败: " + ex.getMessage());
                    statusLabel.setForeground(new Color(255, 100, 100));
                });
            }
        }).start();
    }

    private void doConnect() {
        final String host = hostField.getText().trim();
        final int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setText("端口号格式错误，请输入数字");
            statusLabel.setForeground(new Color(255, 100, 100));
            return;
        }
        final String user = userField.getText().trim();
        final String pass = new String(passField.getPassword());
        final String keyFile = keyFileField.getText().trim();

        if (host.isEmpty()) {
            statusLabel.setText("请输入主机IP地址");
            statusLabel.setForeground(new Color(255, 100, 100));
            return;
        }
        if (user.isEmpty()) {
            statusLabel.setText("请输入用户名");
            statusLabel.setForeground(new Color(255, 100, 100));
            return;
        }

        // Disable connect button and show connecting status
        for (Component c : getComponents()) {
            if (c instanceof JPanel) {
                for (Component cc : ((JPanel) c).getComponents()) {
                    if (cc instanceof JButton && "连接".equals(((JButton) cc).getText())) {
                        ((JButton) cc).setEnabled(false);
                        ((JButton) cc).setText("连接中...");
                    }
                }
            }
        }
        statusLabel.setText("正在连接 " + host + ":" + port + " ...（最多等待15秒）");
        statusLabel.setForeground(new Color(200, 200, 100));

        // Connect in background thread to avoid UI freeze
        new Thread(() -> {
            try {
                ssh = new SSHClient(host, port, user, pass, keyFile.isEmpty() ? null : keyFile);
                ssh.connect();
                connected = true;
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("✅ 已连接到 " + user + "@" + host);
                    statusLabel.setForeground(new Color(100, 220, 100));
                    initConnectedUI(host, user);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    String errorMsg = ex.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = ex.getClass().getSimpleName();
                    }
                    statusLabel.setText("❌ 连接失败: " + errorMsg);
                    statusLabel.setForeground(new Color(255, 100, 100));
                    // Re-enable connect button
                    for (Component c : getComponents()) {
                        if (c instanceof JPanel) {
                            for (Component cc : ((JPanel) c).getComponents()) {
                                if (cc instanceof JButton && "连接中...".equals(((JButton) cc).getText())) {
                                    ((JButton) cc).setEnabled(true);
                                    ((JButton) cc).setText("连接");
                                }
                            }
                        }
                    }
                    // Show detailed error dialog
                    JOptionPane.showMessageDialog(this,
                        "SSH 连接失败！\n\n" +
                        "错误信息: " + errorMsg + "\n\n" +
                        "可能的原因:\n" +
                        "1. 主机IP或端口错误\n" +
                        "2. 目标机器未开启SSH服务\n" +
                        "3. 防火墙阻止了22端口\n" +
                        "4. 用户名或密码错误\n" +
                        "5. 两台机器不在同一网络\n\n" +
                        "排查方法:\n" +
                        "- 命令行测试: ssh " + user + "@" + host + "\n" +
                        "- 测试端口: telnet " + host + " " + port + "\n" +
                        "- Windows目标机: 运行 一键开启OpenSSH.bat",
                        "连接失败", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void initConnectedUI(String host, String user) {
        removeAll();
        setLayout(new BorderLayout());

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(30, 30, 46));
        topBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JLabel infoLabel = new JLabel("已连接: " + user + "@" + host + "  |  当前目录: ");
        infoLabel.setForeground(new Color(200, 220, 200));
        topBar.add(infoLabel, BorderLayout.WEST);
        pathLabel = new JLabel("/");
        pathLabel.setForeground(new Color(180, 200, 255));
        topBar.add(pathLabel, BorderLayout.CENTER);
        JButton disconnectBtn = new JButton("断开连接");
        disconnectBtn.setBackground(new Color(160, 60, 60));
        disconnectBtn.setForeground(Color.WHITE);
        disconnectBtn.addActionListener(e -> doDisconnect());
        topBar.add(disconnectBtn, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Split: console top, files bottom
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setDividerLocation(300);
        split.setBackground(new Color(22, 22, 34));

        // Console panel
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(60, 60, 80)), "远程控制台"));
        consolePanel.setBackground(new Color(22, 22, 34));
        consoleArea = new JTextArea();
        consoleArea.setBackground(new Color(15, 15, 25));
        consoleArea.setForeground(new Color(200, 220, 200));
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        consoleArea.setEditable(false);
        consolePanel.add(new JScrollPane(consoleArea), BorderLayout.CENTER);
        JPanel cmdBar = new JPanel(new BorderLayout());
        cmdBar.setBackground(new Color(22, 22, 34));
        cmdField = new JTextField();
        cmdField.setBackground(new Color(30, 30, 46));
        cmdField.setForeground(Color.WHITE);
        cmdField.addActionListener(e -> sendRemoteCommand());
        JButton sendBtn = new JButton("执行");
        sendBtn.addActionListener(e -> sendRemoteCommand());
        cmdBar.add(new JLabel("命令: "), BorderLayout.WEST);
        cmdBar.add(cmdField, BorderLayout.CENTER);
        cmdBar.add(sendBtn, BorderLayout.EAST);
        consolePanel.add(cmdBar, BorderLayout.SOUTH);
        split.setTopComponent(consolePanel);

        // File browser panel
        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(60, 60, 80)), "远程文件管理"));
        filePanel.setBackground(new Color(22, 22, 34));
        String[] cols = {"名称", "大小", "权限", "修改时间"};
        fileModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        fileTable = new JTable(fileModel);
        fileTable.setBackground(new Color(25, 25, 38));
        fileTable.setForeground(new Color(220, 220, 240));
        fileTable.setSelectionBackground(new Color(50, 50, 80));
        fileTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = fileTable.getSelectedRow();
                    if (row >= 0) {
                        String name = (String) fileModel.getValueAt(row, 0);
                        String size = (String) fileModel.getValueAt(row, 1);
                        if ("<DIR>".equals(size) || "..".equals(name)) {
                            navigateRemoteDir(name);
                        } else {
                            editRemoteFile(name);
                        }
                    }
                }
            }
        });
        filePanel.add(new JScrollPane(fileTable), BorderLayout.CENTER);

        JPanel fileOps = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileOps.setBackground(new Color(22, 22, 34));
        JButton upBtn = new JButton("上级目录");
        upBtn.addActionListener(e -> navigateRemoteDir(".."));
        JButton uploadBtn = new JButton("上传文件");
        uploadBtn.addActionListener(e -> uploadRemoteFile());
        JButton downloadBtn = new JButton("下载文件");
        downloadBtn.addActionListener(e -> downloadRemoteFile());
        JButton deleteBtn = new JButton("删除");
        deleteBtn.addActionListener(e -> deleteRemoteFile());
        JButton refreshBtn = new JButton("刷新");
        refreshBtn.addActionListener(e -> loadRemoteFiles());
        fileOps.add(upBtn);
        fileOps.add(uploadBtn);
        fileOps.add(downloadBtn);
        fileOps.add(deleteBtn);
        fileOps.add(refreshBtn);
        filePanel.add(fileOps, BorderLayout.SOUTH);
        split.setBottomComponent(filePanel);

        add(split, BorderLayout.CENTER);
        revalidate();
        repaint();

        // Detect home dir
        try {
            String home = ssh.execCommand("echo $HOME").trim();
            if (!home.isEmpty()) {
                currentRemoteDir = home;
            }
        } catch (Exception e) {}
        pathLabel.setText(currentRemoteDir);
        loadRemoteFiles();
        appendConsole("[系统] 已连接到 " + user + "@" + host);
        appendConsole("[系统] 当前目录: " + currentRemoteDir);
    }

    private void sendRemoteCommand() {
        String cmd = cmdField.getText().trim();
        if (cmd.isEmpty()) return;
        cmdField.setText("");
        appendConsole("$ " + cmd);
        try {
            String result = ssh.execCommand("cd " + currentRemoteDir + " && " + cmd);
            appendConsole(result);
        } catch (Exception ex) {
            appendConsole("[错误] " + ex.getMessage());
        }
    }

    private void loadRemoteFiles() {
        try {
            List<String> files = ssh.listFiles(currentRemoteDir);
            fileModel.setRowCount(0);
            fileModel.addRow(new Object[]{"..", "<DIR>", "", ""});
            for (String f : files) {
                String[] parts = f.split("\\|");
                if (parts.length >= 4) {
                    fileModel.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3]});
                } else {
                    fileModel.addRow(new Object[]{f, "", "", ""});
                }
            }
            pathLabel.setText(currentRemoteDir);
        } catch (Exception ex) {
            appendConsole("[错误] 无法列出文件: " + ex.getMessage());
        }
    }

    private void navigateRemoteDir(String name) {
        if ("..".equals(name)) {
            int idx = currentRemoteDir.lastIndexOf('/');
            if (idx > 0) currentRemoteDir = currentRemoteDir.substring(0, idx);
            else currentRemoteDir = "/";
        } else {
            if (!currentRemoteDir.endsWith("/")) currentRemoteDir += "/";
            currentRemoteDir += name;
        }
        loadRemoteFiles();
    }

    private void editRemoteFile(String name) {
        try {
            String path = currentRemoteDir + "/" + name;
            TextEditorDialog.RemoteFileHandler handler = new TextEditorDialog.RemoteFileHandler() {
                public String readFile(String p) throws Exception { return ssh.readFile(p); }
                public void writeFile(String p, String content) throws Exception { ssh.writeFile(p, content); }
            };
            Window win = SwingUtilities.getWindowAncestor(this);
            Frame frame = (win instanceof Frame) ? (Frame) win : null;
            TextEditorDialog editor = new TextEditorDialog(frame, path, true, handler);
            editor.setVisible(true);
            appendConsole("[文件] 已打开编辑: " + name);
        } catch (Exception ex) {
            appendConsole("[错误] 无法编辑文件: " + ex.getMessage());
        }
    }

    private void uploadRemoteFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String local = fc.getSelectedFile().getAbsolutePath();
                String remote = currentRemoteDir + "/" + fc.getSelectedFile().getName();
                ssh.uploadFile(local, remote);
                appendConsole("[上传] " + local + " -> " + remote);
                loadRemoteFiles();
            } catch (Exception ex) {
                appendConsole("[错误] 上传失败: " + ex.getMessage());
            }
        }
    }

    private void downloadRemoteFile() {
        int row = fileTable.getSelectedRow();
        if (row < 1) return;
        String name = (String) fileModel.getValueAt(row, 0);
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(name));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String remote = currentRemoteDir + "/" + name;
                String local = fc.getSelectedFile().getAbsolutePath();
                ssh.downloadFile(remote, local);
                appendConsole("[下载] " + remote + " -> " + local);
            } catch (Exception ex) {
                appendConsole("[错误] 下载失败: " + ex.getMessage());
            }
        }
    }

    private void deleteRemoteFile() {
        int row = fileTable.getSelectedRow();
        if (row < 1) return;
        String name = (String) fileModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "确认删除 " + name + "?", "删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                ssh.deleteFile(currentRemoteDir + "/" + name);
                appendConsole("[删除] " + name);
                loadRemoteFiles();
            } catch (Exception ex) {
                appendConsole("[错误] 删除失败: " + ex.getMessage());
            }
        }
    }

    private void doDisconnect() {
        if (ssh != null) ssh.disconnect();
        connected = false;
        initLoginUI();
    }

    private void appendConsole(String text) {
        consoleArea.append(text + "\n");
        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
    }
}
