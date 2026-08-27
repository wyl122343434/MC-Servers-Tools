package com.mcmanager.ui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class UserManagerPanel extends JPanel {
    private List<Map<String, String>> users = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTable userTable;
    private static final String USERS_FILE = System.getProperty("user.home") + File.separator + ".mcmanager" + File.separator + "users.properties";

    public UserManagerPanel() {
        setLayout(new BorderLayout());
        loadUsers();
        initUI();
    }

    private void initUI() {
        // Toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        JButton addBtn = new JButton("添加用户");
        addBtn.addActionListener(e -> addUser());
        JButton editBtn = new JButton("编辑");
        editBtn.addActionListener(e -> editUser());
        JButton delBtn = new JButton("删除");
        delBtn.addActionListener(e -> deleteUser());
        JButton penetrateBtn = new JButton("🚀 一键穿透");
        penetrateBtn.setFont(penetrateBtn.getFont().deriveFont(Font.BOLD, 12f));
        penetrateBtn.setBackground(new Color(60, 120, 200));
        penetrateBtn.setForeground(Color.WHITE);
        penetrateBtn.addActionListener(e -> oneClickPenetrate());
        JButton refreshBtn = new JButton("刷新");
        refreshBtn.addActionListener(e -> { loadUsers(); refreshTable(); });
        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(delBtn);
        toolbar.addSeparator();
        toolbar.add(penetrateBtn);
        toolbar.addSeparator();
        toolbar.add(refreshBtn);
        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"用户名", "角色", "邮箱", "创建时间", "状态"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editUser();
            }
        });
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Status
        JLabel status = new JLabel("  共 " + users.size() + " 个用户");
        status.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        add(status, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Map<String, String> u : users) {
            tableModel.addRow(new Object[]{
                u.get("username"),
                u.get("role"),
                u.get("email"),
                u.get("created"),
                u.get("status")
            });
        }
    }

    private void oneClickPenetrate() {
        int row = userTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择一个用户"); return; }
        String username = users.get(row).get("username");

        JTextField localPortField = new JTextField("25565");
        JTextField remotePortField = new JTextField(String.valueOf(20000 + new Random().nextInt(10000)));
        String[] nodes = {"沈阳-01","北京-01","上海-01","广州-01","深圳-01","成都-01","杭州-01","武汉-01","西安-01","重庆-01","南京-01","天津-01","香港-01","台北-01","东京-01","新加坡-01","洛杉矶-01"};
        JComboBox<String> nodeCombo = new JComboBox<>(nodes);
        JComboBox<String> protoCombo = new JComboBox<>(new String[]{"TCP", "UDP"});
        JTextField descField = new JTextField("用户 " + username + " 的远程访问隧道");

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int r = 0;
        addField(panel, gbc, r++, "隧道名称:", descField);
        addField(panel, gbc, r++, "本地端口:", localPortField);
        addField(panel, gbc, r++, "远程端口:", remotePortField);
        addField(panel, gbc, r++, "节点:", nodeCombo);
        addField(panel, gbc, r++, "协议:", protoCombo);

        JLabel hint = new JLabel("<html><div style='color:gray;font-size:11px;'>本地端口=要暴露的服务端口（MC默认25565）<br>创建后用户可通过 节点域名:远程端口 从外网访问</div></html>");
        gbc.gridx = 0; gbc.gridy = r++; gbc.gridwidth = 2;
        panel.add(hint, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "一键穿透 - 为用户 " + username + " 创建隧道", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String node = (String) nodeCombo.getSelectedItem();
            String localPort = localPortField.getText().trim();
            String remotePort = remotePortField.getText().trim();
            String proto = (String) protoCombo.getSelectedItem();
            String name = descField.getText().trim();

            // Save to ChmlFRP tunnels file
            String tunnelsFile = System.getProperty("user.home") + File.separator + ".mcmanager" + File.separator + "chmlfrp_tunnels.properties";
            Properties props = new Properties();
            File f = new File(tunnelsFile);
            if (f.exists()) props.load(new FileInputStream(f));
            int count = Integer.parseInt(props.getProperty("tunnel.count", "0"));
            String p = "tunnel." + count + ".";
            props.setProperty(p + "name", name);
            props.setProperty(p + "node", node);
            props.setProperty(p + "localPort", localPort);
            props.setProperty(p + "remotePort", remotePort);
            props.setProperty(p + "protocol", proto);
            props.setProperty(p + "status", "未连接");
            props.setProperty(p + "publicAddr", "-");
            props.setProperty(p + "owner", username);
            props.setProperty("tunnel.count", String.valueOf(count + 1));
            f.getParentFile().mkdirs();
            props.store(new FileOutputStream(f), "MC-Servers-Tools - ChmlFRP Tunnels");

            String publicAddr = node.replace("-01", "").toLowerCase() + ".chmlfrp.cn:" + remotePort;
            JOptionPane.showMessageDialog(this,
                "隧道创建成功！\n\n" +
                "用户: " + username + "\n" +
                "隧道: " + name + "\n" +
                "节点: " + node + "\n" +
                "本地端口: " + localPort + "\n" +
                "公网地址: " + publicAddr + "\n\n" +
                "请切换到「🌐 内网穿透」标签页，选中该隧道点击「连接」即可启用。\n" +
                "用户从外网通过 " + publicAddr + " 即可访问。",
                "一键穿透成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "创建失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addUser() {
        Map<String, String> user = new HashMap<>();
        user.put("username", "");
        user.put("password", "");
        user.put("role", "user");
        user.put("email", "");
        user.put("status", "active");
        user.put("created", new Date().toString());
        if (showUserDialog(user, false)) {
            users.add(user);
            saveUsers();
            refreshTable();
        }
    }

    private void editUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选择用户"); return; }
        Map<String, String> user = users.get(row);
        if (showUserDialog(user, true)) {
            saveUsers();
            refreshTable();
        }
    }

    private void deleteUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选择用户"); return; }
        String username = users.get(row).get("username");
        if (JOptionPane.showConfirmDialog(this, "确定删除用户 " + username + " ?", "确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            users.remove(row);
            saveUsers();
            refreshTable();
        }
    }

    private boolean showUserDialog(Map<String, String> user, boolean isEdit) {
        JTextField nameField = new JTextField(user.get("username"));
        JPasswordField passField = new JPasswordField(user.get("password"));
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"admin", "user", "viewer"});
        roleCombo.setSelectedItem(user.get("role"));
        JTextField emailField = new JTextField(user.get("email"));
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"active", "disabled"});
        statusCombo.setSelectedItem(user.get("status"));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        addField(panel, gbc, row++, "用户名:", nameField);
        addField(panel, gbc, row++, "密码:", passField);
        addField(panel, gbc, row++, "角色:", roleCombo);
        addField(panel, gbc, row++, "邮箱:", emailField);
        addField(panel, gbc, row++, "状态:", statusCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, isEdit ? "编辑用户" : "添加用户", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            user.put("username", nameField.getText());
            user.put("password", new String(passField.getPassword()));
            user.put("role", (String) roleCombo.getSelectedItem());
            user.put("email", emailField.getText());
            user.put("status", (String) statusCombo.getSelectedItem());
            return true;
        }
        return false;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        if (field instanceof JTextField) ((JTextField) field).setColumns(20);
        panel.add(field, gbc);
        gbc.fill = GridBagConstraints.NONE;
    }

    private void loadUsers() {
        users.clear();
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            // Add default admin
            Map<String, String> admin = new HashMap<>();
            admin.put("username", "admin");
            admin.put("password", "admin123");
            admin.put("role", "admin");
            admin.put("email", "admin@localhost");
            admin.put("status", "active");
            admin.put("created", new Date().toString());
            users.add(admin);
            saveUsers();
            return;
        }
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(file));
            int count = Integer.parseInt(props.getProperty("user.count", "0"));
            for (int i = 0; i < count; i++) {
                String p = "user." + i + ".";
                Map<String, String> u = new HashMap<>();
                u.put("username", props.getProperty(p + "username", ""));
                u.put("password", props.getProperty(p + "password", ""));
                u.put("role", props.getProperty(p + "role", "user"));
                u.put("email", props.getProperty(p + "email", ""));
                u.put("status", props.getProperty(p + "status", "active"));
                u.put("created", props.getProperty(p + "created", ""));
                users.add(u);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveUsers() {
        try {
            new File(USERS_FILE).getParentFile().mkdirs();
            Properties props = new Properties();
            props.setProperty("user.count", String.valueOf(users.size()));
            for (int i = 0; i < users.size(); i++) {
                String p = "user." + i + ".";
                Map<String, String> u = users.get(i);
                props.setProperty(p + "username", u.get("username"));
                props.setProperty(p + "password", u.get("password"));
                props.setProperty(p + "role", u.get("role"));
                props.setProperty(p + "email", u.get("email"));
                props.setProperty(p + "status", u.get("status"));
                props.setProperty(p + "created", u.get("created"));
            }
            props.store(new FileOutputStream(USERS_FILE), "MC-Servers-Tools - Users");
        } catch (Exception e) { e.printStackTrace(); }
    }
}
