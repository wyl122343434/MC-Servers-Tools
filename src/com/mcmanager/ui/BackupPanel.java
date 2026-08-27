package com.mcmanager.ui;

import com.mcmanager.util.ThemeManager;
import com.mcmanager.util.FontUtil;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class BackupPanel extends JPanel {
    private DefaultTableModel tableModel;

    public BackupPanel() {
        ThemeManager tm = ThemeManager.getInstance();
        setLayout(new BorderLayout());
        setBackground(tm.bgPrimary());

        JLabel title = new JLabel("💾 备份管理");
        title.setFont(FontUtil.getFont(Font.BOLD, 20));
        title.setForeground(tm.textPrimary());
        title.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        add(title, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(tm.bgPrimary());
        JButton createBtn = new JButton("创建备份");
        createBtn.setBackground(tm.accent());
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "请选择服务器后创建备份。", "提示", JOptionPane.INFORMATION_MESSAGE));
        JButton restoreBtn = new JButton("恢复备份");
        JButton deleteBtn = new JButton("删除备份");
        toolbar.add(createBtn);
        toolbar.add(restoreBtn);
        toolbar.add(deleteBtn);
        add(toolbar, BorderLayout.NORTH);

        String[] cols = {"备份名称", "大小", "创建时间", "服务器"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setBackground(tm.bgCard());
        table.setForeground(tm.textPrimary());
        table.setSelectionBackground(tm.accent());
        table.setSelectionForeground(Color.WHITE);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
}
