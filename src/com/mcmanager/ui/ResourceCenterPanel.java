package com.mcmanager.ui;

import com.mcmanager.util.ThemeManager;
import com.mcmanager.util.FontUtil;
import javax.swing.*;
import java.awt.*;

public class ResourceCenterPanel extends JPanel {
    public ResourceCenterPanel() {
        ThemeManager tm = ThemeManager.getInstance();
        setLayout(new BorderLayout());
        setBackground(tm.bgPrimary());

        JLabel title = new JLabel("📦 资源中心 - 服务器核心安装");
        title.setFont(FontUtil.getFont(Font.BOLD, 20));
        title.setForeground(tm.textPrimary());
        title.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setBackground(tm.bgCard());
        center.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(tm.border()),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel hint = new JLabel("<html><div style='font-size:13px;'>" +
            "支持的服务器核心类型：<br><br>" +
            "• Vanilla（原版）<br>" +
            "• Paper（高性能插件服）<br>" +
            "• Forge（模组服）<br>" +
            "• Fabric（轻量模组服）<br>" +
            "• NeoForge（新版 Forge）<br><br>" +
            "请选择核心类型和游戏版本后点击下载安装。<br>" +
            "安装完成后会自动生成 run.bat 启动脚本。" +
            "</div></html>");
        hint.setForeground(tm.textPrimary());
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(hint);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBackground(tm.bgCard());
        form.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        form.add(new JLabel("核心类型:"));
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Vanilla", "Paper", "Forge", "Fabric", "NeoForge"});
        form.add(typeCombo);
        form.add(new JLabel("游戏版本:"));
        JTextField versionField = new JTextField("1.20.1");
        form.add(versionField);
        form.add(new JLabel("安装目录:"));
        JTextField dirField = new JTextField(System.getProperty("user.home") + "/minecraft-server");
        form.add(dirField);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(form);

        JButton downloadBtn = new JButton("下载并安装");
        downloadBtn.setBackground(tm.accent());
        downloadBtn.setForeground(Color.WHITE);
        downloadBtn.setFocusPainted(false);
        downloadBtn.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        downloadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        downloadBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "下载功能开发中，请手动下载服务器核心。", "提示", JOptionPane.INFORMATION_MESSAGE));
        center.add(Box.createVerticalStrut(20));
        center.add(downloadBtn);

        add(center, BorderLayout.CENTER);
    }
}
