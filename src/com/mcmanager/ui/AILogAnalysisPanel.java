package com.mcmanager.ui;

import com.mcmanager.util.ThemeManager;
import com.mcmanager.util.FontUtil;
import javax.swing.*;
import java.awt.*;

public class AILogAnalysisPanel extends JPanel {
    public AILogAnalysisPanel() {
        ThemeManager tm = ThemeManager.getInstance();
        setLayout(new BorderLayout());
        setBackground(tm.bgPrimary());

        JLabel title = new JLabel("🤖 AI 日志分析");
        title.setFont(FontUtil.getFont(Font.BOLD, 20));
        title.setForeground(tm.textPrimary());
        title.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(tm.bgCard());
        center.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(tm.border()),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));

        JTextArea logArea = new JTextArea();
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setBorder(BorderFactory.createTitledBorder("服务器日志（粘贴或自动获取）"));
        center.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(tm.bgCard());
        JButton analyzeBtn = new JButton("🔍 AI 分析日志");
        analyzeBtn.setBackground(tm.accent());
        analyzeBtn.setForeground(Color.WHITE);
        analyzeBtn.setFocusPainted(false);
        analyzeBtn.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        analyzeBtn.addActionListener(e -> {
            String log = logArea.getText();
            if (log.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先粘贴服务器日志。", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String analysis = analyzeLog(log);
            JOptionPane.showMessageDialog(this, analysis, "AI 分析结果", JOptionPane.INFORMATION_MESSAGE);
        });
        bottom.add(analyzeBtn, BorderLayout.EAST);
        center.add(bottom, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
    }

    private String analyzeLog(String log) {
        StringBuilder result = new StringBuilder("📊 日志分析结果\n\n");
        if (log.contains("Exception") || log.contains("Error") || log.contains("ERROR")) {
            result.append("⚠️ 检测到错误/异常\n");
            if (log.contains("UnsupportedClassVersionError")) {
                result.append("  → Java版本不匹配，请升级Java版本\n");
            }
            if (log.contains("ClassNotFoundException")) {
                result.append("  → 缺少类文件，可能是模组缺失或版本不兼容\n");
            }
            if (log.contains("OutOfMemoryError")) {
                result.append("  → 内存不足，请增加服务器内存分配\n");
            }
        } else {
            result.append("✅ 未检测到明显错误\n");
        }
        if (log.contains("Done") && log.contains("s")) {
            result.append("✅ 服务器启动成功\n");
        }
        result.append("\n💡 建议：检查服务器配置和模组兼容性。");
        return result.toString();
    }
}
