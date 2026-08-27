package com.mcmanager.ui;

import com.mcmanager.util.ThemeManager;
import com.mcmanager.util.FontUtil;
import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    public DashboardPanel() {
        ThemeManager tm = ThemeManager.getInstance();
        setLayout(new BorderLayout());
        setBackground(tm.bgPrimary());

        JLabel title = new JLabel("📊 仪表盘");
        title.setFont(FontUtil.getFont(Font.BOLD, 24));
        title.setForeground(tm.textPrimary());
        title.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 2, 15, 15));
        center.setBackground(tm.bgPrimary());
        center.setBorder(BorderFactory.createEmptyBorder(10, 25, 25, 25));

        center.add(createStatCard("服务器总数", "0", tm.accent()));
        center.add(createStatCard("运行中", "0", tm.success()));
        center.add(createStatCard("总内存分配", "0 MB", tm.warning()));
        center.add(createStatCard("运行时长", "0h", new Color(255, 100, 100)));

        add(center, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String label, String value, Color color) {
        ThemeManager tm = ThemeManager.getInstance();
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(tm.bgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(tm.border()),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        JLabel valLabel = new JLabel(value);
        valLabel.setFont(FontUtil.getFont(Font.BOLD, 28));
        valLabel.setForeground(color);
        card.add(valLabel, BorderLayout.CENTER);
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(FontUtil.getFont(Font.PLAIN, 13));
        lblLabel.setForeground(tm.textSecondary());
        card.add(lblLabel, BorderLayout.SOUTH);
        return card;
    }
}
