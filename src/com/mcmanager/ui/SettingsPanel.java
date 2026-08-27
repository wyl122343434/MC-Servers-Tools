package com.mcmanager.ui;

import com.mcmanager.util.ConfigStorage;
import com.mcmanager.util.ThemeManager;
import com.mcmanager.util.FontUtil;
import com.mcmanager.util.LayoutTemplate;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class SettingsPanel extends JPanel {
    private Properties settings;
    private ThemeManager tm = ThemeManager.getInstance();

    private JPanel sidebar;
    private String[] sections = {"通用设置", "Java 与启动", "外观个性化", "布局模板", "备份设置", "内网穿透", "AI 设置", "关于"};
    private String[] sectionIcons = {"⚙️", "☕", "🎨", "📐", "💾", "🌐", "🤖", "ℹ️"};
    private int currentSection = 0;
    private JPanel contentPanel;

    private JCheckBox autoStartCheck, showConsoleCheck;
    private JTextField defaultJavaField, maxMemField, minMemField, defaultServerDirField;
    private JComboBox<String> startupModeCombo, themePresetCombo, aiProviderCombo;
    private JButton bgPrimaryBtn, bgCardBtn, textPrimaryBtn, accentBtn;
    private JSlider radiusSlider;
    private JLabel radiusLabel;
    private JTextField backupDirField, frpNodeField, aiApiKeyField, aiModelField, bgImageField, curseForgeApiKeyField;
    private JSlider opacitySliderField, tabSizeField;
    private JComboBox<String> languageCombo;
    private Runnable onSettingsApplied;
    private JCheckBox autoBackupCheck;
    private JLabel statusLabel;

    public SettingsPanel() {
        setLayout(new BorderLayout());
        settings = ConfigStorage.loadSettings();
        initUI();
        loadSettings();
    }

    public void setOnSettingsApplied(Runnable callback) {
        this.onSettingsApplied = callback;
    }

    private void initUI() {
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(tm.bgSecondary());
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, tm.border()));
        sidebar.setPreferredSize(new Dimension(180, 0));

        JLabel title = new JLabel("  设置");
        title.setFont(FontUtil.getFont(Font.BOLD, 18));
        title.setForeground(tm.textPrimary());
        title.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(title);

        for (int i = 0; i < sections.length; i++) {
            final int idx = i;
            JButton btn = new JButton("  " + sectionIcons[i] + "  " + sections[i]);
            btn.setFont(FontUtil.getFont(Font.PLAIN, 14));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(true);
            btn.setForeground(tm.textPrimary());
            btn.setBackground(tm.bgSecondary());
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            btn.setMinimumSize(new Dimension(180, 44));
            btn.setPreferredSize(new Dimension(180, 44));
            btn.addActionListener(e -> selectSection(idx));
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (currentSection != idx) btn.setBackground(tm.bgCard());
                }
                public void mouseExited(MouseEvent e) {
                    if (currentSection != idx) btn.setBackground(tm.bgSecondary());
                }
            });
            sidebar.add(btn);
        }
        sidebar.add(Box.createVerticalGlue());

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(tm.bgPrimary());

        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(tm.bgSecondary());
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, tm.border()));
        statusLabel = new JLabel("  ");
        statusLabel.setForeground(tm.textSecondary());
        bottomBar.add(statusLabel, BorderLayout.WEST);
        JButton saveBtn = new JButton("保存设置");
        saveBtn.setBackground(tm.accent());
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        saveBtn.addActionListener(e -> saveSettings());
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnWrap.setBackground(tm.bgSecondary());
        btnWrap.add(saveBtn);
        bottomBar.add(btnWrap, BorderLayout.EAST);

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
        selectSection(0);
    }

    private void selectSection(int idx) {
        currentSection = idx;
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JButton) {
                c.setBackground(tm.bgSecondary());
                ((JButton) c).setForeground(tm.textPrimary());
            }
        }
        Component[] comps = sidebar.getComponents();
        if (idx + 1 < comps.length && comps[idx + 1] instanceof JButton) {
            comps[idx + 1].setBackground(tm.accent());
            ((JButton) comps[idx + 1]).setForeground(Color.WHITE);
        }
        contentPanel.removeAll();
        JScrollPane scroll = new JScrollPane(createSectionPanel(idx));
        scroll.setBorder(null);
        scroll.getViewport().setBackground(tm.bgPrimary());
        contentPanel.add(scroll, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel createSectionPanel(int idx) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(tm.bgPrimary());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel header = new JLabel(sectionIcons[idx] + "  " + sections[idx]);
        header.setFont(FontUtil.getFont(Font.BOLD, 22));
        header.setForeground(tm.textPrimary());
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(header);
        panel.add(Box.createVerticalStrut(20));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(tm.bgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(tm.border()),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        switch (idx) {
            case 0:
                addField(card, gbc, row++, "语言:", languageCombo = new JComboBox<>(new String[]{"简体中文", "繁體中文", "English", "日本語", "한국어", "Русский"}));
                JCheckBox autoStartAppCheck = new JCheckBox("开机自动启动本程序");
                autoStartAppCheck.setBackground(tm.bgCard());
                autoStartAppCheck.setForeground(tm.textPrimary());
                autoStartAppCheck.setSelected(com.mcmanager.util.AutoStart.isEnabled());
                autoStartAppCheck.addActionListener(e -> {
                    boolean ok = com.mcmanager.util.AutoStart.setEnabled(autoStartAppCheck.isSelected());
                    if (!ok) {
                        JOptionPane.showMessageDialog(this, "设置开机自启动失败，请手动设置。", "错误", JOptionPane.ERROR_MESSAGE);
                        autoStartAppCheck.setSelected(!autoStartAppCheck.isSelected());
                    }
                });
                addField(card, gbc, row++, "开机自启动:", autoStartAppCheck);
                addField(card, gbc, row++, "启动时显示控制台:", showConsoleCheck = new JCheckBox("启动程序时自动打开控制台"));
                addField(card, gbc, row++, "自动启动上次服务器:", autoStartCheck = new JCheckBox("程序启动时自动启动上次运行的服务器"));
                addField(card, gbc, row++, "默认服务器目录:", defaultServerDirField = new JTextField(30));
                break;
            case 1:
                addField(card, gbc, row++, "默认 Java 路径:", defaultJavaField = new JTextField(30));
                addField(card, gbc, row++, "启动方式:", startupModeCombo = new JComboBox<>(new String[]{"直接 java -jar 启动", "通过 run.bat / start.bat 启动"}));
                addField(card, gbc, row++, "默认最大内存 (MB):", maxMemField = new JTextField(10));
                addField(card, gbc, row++, "默认最小内存 (MB):", minMemField = new JTextField(10));
                break;
            case 2:
                addField(card, gbc, row++, "主题预设:", themePresetCombo = new JComboBox<>(new String[]{"仿 iOS 浅色", "标准浅色", "深色模式"}));
                themePresetCombo.addActionListener(e -> {
                    int sel = themePresetCombo.getSelectedIndex();
                    if (sel == 0) tm.setPreset(ThemeManager.THEME_IOS);
                    else if (sel == 1) tm.setPreset(ThemeManager.THEME_LIGHT);
                    else tm.setPreset(ThemeManager.THEME_DARK);
                    updateColorButtons();
                    JOptionPane.showMessageDialog(this, "主题已切换，重启程序后完全生效。", "主题", JOptionPane.INFORMATION_MESSAGE);
                });
                addField(card, gbc, row++, "背景颜色:", bgPrimaryBtn = createColorButton("bg.primary"));
                addField(card, gbc, row++, "卡片颜色:", bgCardBtn = createColorButton("bg.card"));
                addField(card, gbc, row++, "文字颜色:", textPrimaryBtn = createColorButton("text.primary"));
                addField(card, gbc, row++, "主题色:", accentBtn = createColorButton("accent"));
                // Background image
                JPanel bgImgPanel = new JPanel(new BorderLayout(8, 0));
                bgImgPanel.setBackground(tm.bgCard());
                JTextField bgImgField = new JTextField(tm.getBackgroundImage(), 20);
                bgImageField = bgImgField;
                bgImgField.setBackground(tm.bgPrimary());
                bgImgField.setForeground(tm.textPrimary());
                JButton browseBgBtn = new JButton("浏览...");
                browseBgBtn.addActionListener(e -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("图片文件", "jpg", "jpeg", "png", "gif", "bmp"));
                    if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        bgImgField.setText(fc.getSelectedFile().getAbsolutePath());
                    }
                });
                JButton clearBgBtn = new JButton("清除");
                clearBgBtn.addActionListener(e -> bgImgField.setText(""));
                bgImgPanel.add(bgImgField, BorderLayout.CENTER);
                JPanel bgBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
                bgBtns.setBackground(tm.bgCard());
                bgBtns.add(browseBgBtn);
                bgBtns.add(clearBgBtn);
                bgImgPanel.add(bgBtns, BorderLayout.EAST);
                addField(card, gbc, row++, "背景图片:", bgImgPanel);
                // Background opacity
                JPanel opacityPanel = new JPanel(new BorderLayout(10, 0));
                opacityPanel.setBackground(tm.bgCard());
                JSlider opacitySlider = new JSlider(0, 100, (int)(tm.getBackgroundOpacity() * 100));
                opacitySliderField = opacitySlider;
                opacitySlider.setBackground(tm.bgCard());
                JLabel opacityLabel = new JLabel((int)(tm.getBackgroundOpacity() * 100) + "%");
                opacityLabel.setForeground(tm.textPrimary());
                opacitySlider.addChangeListener(e -> opacityLabel.setText(opacitySlider.getValue() + "%"));
                opacityPanel.add(opacitySlider, BorderLayout.CENTER);
                opacityPanel.add(opacityLabel, BorderLayout.EAST);
                addField(card, gbc, row++, "背景透明度:", opacityPanel);
                // Tab font size
                JPanel tabSizePanel = new JPanel(new BorderLayout(10, 0));
                tabSizePanel.setBackground(tm.bgCard());
                JSlider tabSizeSlider = new JSlider(10, 24, tm.getTabFontSize());
                tabSizeField = tabSizeSlider;
                tabSizeSlider.setBackground(tm.bgCard());
                JLabel tabSizeLabel = new JLabel(tm.getTabFontSize() + "px");
                tabSizeLabel.setForeground(tm.textPrimary());
                tabSizeSlider.addChangeListener(e -> tabSizeLabel.setText(tabSizeSlider.getValue() + "px"));
                tabSizePanel.add(tabSizeSlider, BorderLayout.CENTER);
                tabSizePanel.add(tabSizeLabel, BorderLayout.EAST);
                addField(card, gbc, row++, "标签字体大小:", tabSizePanel);
                // Window opacity
                JPanel opacityWinPanel = new JPanel(new BorderLayout(10, 0));
                opacityWinPanel.setBackground(tm.bgCard());
                JSlider winOpacitySlider = new JSlider(30, 100, 50);
                winOpacitySlider.setBackground(tm.bgCard());
                JLabel winOpacityLabel = new JLabel("50%");
                winOpacityLabel.setForeground(tm.textPrimary());
                winOpacitySlider.addChangeListener(e -> {
                    winOpacityLabel.setText(winOpacitySlider.getValue() + "%");
                    // Apply immediately
                    Window window = SwingUtilities.getWindowAncestor(this);
                    if (window != null) {
                        try {
                            window.setOpacity(winOpacitySlider.getValue() / 100f);
                        } catch (Exception ex) {}
                    }
                });
                opacityWinPanel.add(winOpacitySlider, BorderLayout.CENTER);
                opacityWinPanel.add(winOpacityLabel, BorderLayout.EAST);
                addField(card, gbc, row++, "窗口透明度:", opacityWinPanel);
                JPanel radiusPanel = new JPanel(new BorderLayout(10, 0));
                radiusPanel.setBackground(tm.bgCard());
                radiusSlider = new JSlider(0, 30, tm.getRadius());
                radiusSlider.setBackground(tm.bgCard());
                radiusLabel = new JLabel(tm.getRadius() + "px");
                radiusLabel.setForeground(tm.textPrimary());
                radiusSlider.addChangeListener(e -> radiusLabel.setText(radiusSlider.getValue() + "px"));
                radiusPanel.add(radiusSlider, BorderLayout.CENTER);
                radiusPanel.add(radiusLabel, BorderLayout.EAST);
                addField(card, gbc, row++, "圆角大小:", radiusPanel);
                JButton resetBtn = new JButton("恢复默认主题");
                resetBtn.addActionListener(e -> {
                    tm.setPreset(ThemeManager.THEME_IOS);
                    updateColorButtons();
                    radiusSlider.setValue(tm.getRadius());
                });
                gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
                card.add(resetBtn, gbc);
                break;
            case 3:
                // Layout templates
                JPanel layoutPanel = new JPanel();
                layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));
                layoutPanel.setBackground(tm.bgCard());
                JLabel layoutHint = new JLabel("选择预设布局模板，或保存/导出当前布局");
                layoutHint.setForeground(tm.textPrimary());
                layoutHint.setAlignmentX(Component.LEFT_ALIGNMENT);
                layoutPanel.add(layoutHint);
                layoutPanel.add(Box.createVerticalStrut(10));

                DefaultComboBoxModel<LayoutTemplate> tplModel = new DefaultComboBoxModel<>();
                for (LayoutTemplate t : LayoutTemplate.getDefaultTemplates()) tplModel.addElement(t);
                for (LayoutTemplate t : LayoutTemplate.listUserTemplates()) tplModel.addElement(t);
                JComboBox<LayoutTemplate> tplCombo = new JComboBox<>(tplModel);
                tplCombo.setBackground(tm.bgPrimary());
                tplCombo.setForeground(tm.textPrimary());
                tplCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
                tplCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                layoutPanel.add(tplCombo);
                layoutPanel.add(Box.createVerticalStrut(10));

                JPanel layoutBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                layoutBtns.setBackground(tm.bgCard());
                layoutBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
                JButton applyTplBtn = new JButton("应用模板");
                applyTplBtn.setBackground(tm.accent());
                applyTplBtn.setForeground(Color.WHITE);
                applyTplBtn.addActionListener(e -> {
                    LayoutTemplate tpl = (LayoutTemplate) tplCombo.getSelectedItem();
                    if (tpl != null) {
                        JOptionPane.showMessageDialog(this, "模板 '" + tpl.getName() + "' 将在重启后生效。\n\n" +
                            "分隔条位置: " + tpl.getInt("dividerLocation", 250) + "\n" +
                            "窗口大小: " + tpl.getInt("windowWidth", 1200) + "x" + tpl.getInt("windowHeight", 800) + "\n" +
                            "显示服务器列表: " + (tpl.getBoolean("showServerList", true) ? "是" : "否"),
                            "应用模板", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
                JButton saveTplBtn = new JButton("保存当前布局");
                saveTplBtn.addActionListener(e -> {
                    String name = JOptionPane.showInputDialog(this, "输入模板名称:", "保存布局", JOptionPane.QUESTION_MESSAGE);
                    if (name != null && !name.trim().isEmpty()) {
                        LayoutTemplate tpl = new LayoutTemplate(name.trim());
                        tpl.set("dividerLocation", 250);
                        tpl.set("windowWidth", 1200);
                        tpl.set("windowHeight", 800);
                        tpl.set("showServerList", true);
                        tpl.set("consoleFontSize", 13);
                        tpl.set("tabFontSize", tm.getTabFontSize());
                        try {
                            tpl.saveToFile(LayoutTemplate.getTemplatesDir() + "/" + name.trim() + ".json");
                            tplModel.addElement(tpl);
                            JOptionPane.showMessageDialog(this, "布局模板已保存！", "成功", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                JButton exportTplBtn = new JButton("导出模板");
                exportTplBtn.addActionListener(e -> {
                    LayoutTemplate tpl = (LayoutTemplate) tplCombo.getSelectedItem();
                    if (tpl != null) {
                        JFileChooser fc = new JFileChooser();
                        fc.setSelectedFile(new File(tpl.getName() + ".json"));
                        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                            try {
                                tpl.saveToFile(fc.getSelectedFile().getAbsolutePath());
                                JOptionPane.showMessageDialog(this, "模板已导出到: " + fc.getSelectedFile().getAbsolutePath(), "导出成功", JOptionPane.INFORMATION_MESSAGE);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
                JButton importTplBtn = new JButton("导入模板");
                importTplBtn.addActionListener(e -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("布局模板 JSON", "json"));
                    if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        try {
                            LayoutTemplate tpl = LayoutTemplate.loadFromFile(fc.getSelectedFile().getAbsolutePath());
                            tplModel.addElement(tpl);
                            JOptionPane.showMessageDialog(this, "模板 '" + tpl.getName() + "' 已导入！", "导入成功", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "导入失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                layoutBtns.add(applyTplBtn);
                layoutBtns.add(saveTplBtn);
                layoutBtns.add(exportTplBtn);
                layoutBtns.add(importTplBtn);
                layoutPanel.add(layoutBtns);

                gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH;
                card.add(layoutPanel, gbc);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 1;
                break;
            case 4:
                addField(card, gbc, row++, "备份保存目录:", backupDirField = new JTextField(30));
                addField(card, gbc, row++, "自动备份:", autoBackupCheck = new JCheckBox("每天自动备份一次"));
                break;
            case 5:
                addField(card, gbc, row++, "默认节点:", frpNodeField = new JTextField(20));
                break;
            case 6:
                addField(card, gbc, row++, "AI 提供商:", aiProviderCombo = new JComboBox<>(new String[]{"内置免费规则引擎", "OpenAI", "DeepSeek", "通义千问", "豆包"}));
                addField(card, gbc, row++, "AI API Key:", aiApiKeyField = new JTextField(30));
                addField(card, gbc, row++, "AI 模型名称:", aiModelField = new JTextField(20));
                addField(card, gbc, row++, "CurseForge API Key:", curseForgeApiKeyField = new JTextField(30));
                JLabel cfHint = new JLabel("  获取地址: https://console.curseforge.com/ (免费申请)");
                cfHint.setForeground(tm.textSecondary());
                cfHint.setFont(FontUtil.getFont(Font.PLAIN, 11));
                gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
                card.add(cfHint, gbc);
                gbc.gridwidth = 1;
                break;
            case 7:
                JLabel about = new JLabel("<html><div style='font-size:13px;'>" +
                    "<b>MC-Servers-Tools</b><br><br>" +
                    "版本: 3.8<br>" +
                    "核心: Java Swing + SSH<br>" +
                    "支持: Windows / Linux / macOS / Android<br><br>" +
                    "<b>作者信息</b><br>" +
                    "作者: Dfhcg<br>" +
                    "QQ: 3565304421<br><br>" +
                    "<b>功能特性</b><br>" +
                    "• 本地 & SSH 远程服务器管理<br>" +
                    "• 服务器核心自动安装 (Vanilla/Paper/Forge/Fabric/NeoForge)<br>" +
                    "• 模组下载 (Modrinth/CurseForge/MC百科)<br>" +
                    "• 实时控制台 & RCON<br>" +
                    "• 文件管理 & 文本编辑器<br>" +
                    "• ChmlFRP 内网穿透<br>" +
                    "• 用户管理 & 一键穿透<br>" +
                    "• AI 日志分析<br>" +
                    "• 备份管理<br>" +
                    "• 自定义主题 & 背景图片<br>" +
                    "• 多语言支持<br><br>" +
                    "<b>v3.8 更新说明</b><br>" +
                    "• 程序图标（任务栏/标题栏/Alt+Tab）<br>" +
                    "• 窗口标题中英文双语显示<br>" +
                    "• ChmlFRP账号记住密码自动登录<br>" +
                    "• 服务器列表每30秒自动保存<br>" +
                    "• 项目重命名为 MC-Servers-Tools<br><br>" +
                    "© 2026 MC-Servers-Tools" +
                    "</div></html>");
                about.setForeground(tm.textPrimary());
                gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
                card.add(about, gbc);
                break;
        }
        gbc.gridx = 0; gbc.gridy = row; gbc.weighty = 1;
        card.add(Box.createVerticalGlue(), gbc);
        panel.add(card);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setForeground(tm.textPrimary());
        l.setFont(FontUtil.getFont(Font.PLAIN, 13));
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(l, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        if (field instanceof JCheckBox) {
            ((JCheckBox) field).setBackground(tm.bgCard());
            ((JCheckBox) field).setForeground(tm.textPrimary());
        } else if (field instanceof JComboBox) {
            field.setBackground(tm.bgCard());
            field.setForeground(tm.textPrimary());
        } else if (field instanceof JTextField) {
            field.setBackground(tm.bgPrimary());
            field.setForeground(tm.textPrimary());
        }
        panel.add(field, gbc);
    }

    private JButton createColorButton(String key) {
        JButton btn = new JButton("  选择颜色  ");
        btn.setBackground(tm.getColor(key));
        btn.setForeground(getContrastColor(tm.getColor(key)));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "选择颜色", tm.getColor(key));
            if (c != null) {
                tm.setColor(key, c);
                btn.setBackground(c);
                btn.setForeground(getContrastColor(c));
            }
        });
        return btn;
    }

    private Color getContrastColor(Color c) {
        double lum = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255;
        return lum > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private void updateColorButtons() {
        if (bgPrimaryBtn != null) { bgPrimaryBtn.setBackground(tm.bgPrimary()); bgPrimaryBtn.setForeground(getContrastColor(tm.bgPrimary())); }
        if (bgCardBtn != null) { bgCardBtn.setBackground(tm.bgCard()); bgCardBtn.setForeground(getContrastColor(tm.bgCard())); }
        if (textPrimaryBtn != null) { textPrimaryBtn.setBackground(tm.textPrimary()); textPrimaryBtn.setForeground(getContrastColor(tm.textPrimary())); }
        if (accentBtn != null) { accentBtn.setBackground(tm.accent()); accentBtn.setForeground(getContrastColor(tm.accent())); }
    }

    private void loadSettings() {
        if (languageCombo != null) languageCombo.setSelectedItem(settings.getProperty("language", "简体中文"));
        if (showConsoleCheck != null) showConsoleCheck.setSelected(Boolean.parseBoolean(settings.getProperty("showConsole", "true")));
        if (autoStartCheck != null) autoStartCheck.setSelected(Boolean.parseBoolean(settings.getProperty("autoStart", "false")));
        if (defaultServerDirField != null) defaultServerDirField.setText(settings.getProperty("default.serverDir", ""));
        if (defaultJavaField != null) defaultJavaField.setText(settings.getProperty("default.java", "java"));
        if (startupModeCombo != null) startupModeCombo.setSelectedIndex(Integer.parseInt(settings.getProperty("startup.mode", "0")));
        if (maxMemField != null) maxMemField.setText(settings.getProperty("default.maxMem", "4096"));
        if (minMemField != null) minMemField.setText(settings.getProperty("default.minMem", "1024"));
        if (backupDirField != null) backupDirField.setText(settings.getProperty("backup.dir", System.getProperty("user.home") + "/MCBackups"));
        if (autoBackupCheck != null) autoBackupCheck.setSelected(Boolean.parseBoolean(settings.getProperty("autoBackup", "false")));
        if (frpNodeField != null) frpNodeField.setText(settings.getProperty("frp.defaultNode", "沈阳-01"));
        if (aiProviderCombo != null) aiProviderCombo.setSelectedItem(settings.getProperty("ai.provider", "内置免费规则引擎"));
        if (aiApiKeyField != null) aiApiKeyField.setText(settings.getProperty("ai.apiKey", ""));
        if (aiModelField != null) aiModelField.setText(settings.getProperty("ai.model", "gpt-4o-mini"));
        if (curseForgeApiKeyField != null) curseForgeApiKeyField.setText(settings.getProperty("curseforge.apiKey", ""));
    }

    private void saveSettings() {
        // Confirmation dialog before applying
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定应用这些设置吗？\n\n设置将立即生效，无需重启程序。",
            "确认应用设置",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (languageCombo != null) settings.setProperty("language", (String) languageCombo.getSelectedItem());
        if (defaultServerDirField != null) settings.setProperty("default.serverDir", defaultServerDirField.getText());
        if (showConsoleCheck != null) settings.setProperty("showConsole", String.valueOf(showConsoleCheck.isSelected()));
        if (autoStartCheck != null) settings.setProperty("autoStart", String.valueOf(autoStartCheck.isSelected()));
        if (defaultJavaField != null) settings.setProperty("default.java", defaultJavaField.getText());
        if (startupModeCombo != null) settings.setProperty("startup.mode", String.valueOf(startupModeCombo.getSelectedIndex()));
        if (maxMemField != null) settings.setProperty("default.maxMem", maxMemField.getText());
        if (minMemField != null) settings.setProperty("default.minMem", minMemField.getText());
        if (backupDirField != null) settings.setProperty("backup.dir", backupDirField.getText());
        if (autoBackupCheck != null) settings.setProperty("autoBackup", String.valueOf(autoBackupCheck.isSelected()));
        if (frpNodeField != null) settings.setProperty("frp.defaultNode", frpNodeField.getText());
        if (aiProviderCombo != null) settings.setProperty("ai.provider", (String) aiProviderCombo.getSelectedItem());
        if (aiApiKeyField != null) settings.setProperty("ai.apiKey", aiApiKeyField.getText());
        if (aiModelField != null) settings.setProperty("ai.model", aiModelField.getText());
        if (curseForgeApiKeyField != null) settings.setProperty("curseforge.apiKey", curseForgeApiKeyField.getText());
        if (radiusSlider != null) tm.setRadius(radiusSlider.getValue());
        if (tabSizeField != null) tm.setTabFontSize(tabSizeField.getValue());
        if (bgImageField != null) tm.setBackgroundImage(bgImageField.getText());
        if (opacitySliderField != null) tm.setBackgroundOpacity(opacitySliderField.getValue() / 100f);
        ConfigStorage.saveSettings(settings);
        tm.save();
        statusLabel.setText("  ✓ 设置已保存并生效 - " + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()));

        // Apply immediately - trigger UI refresh
        if (onSettingsApplied != null) {
            onSettingsApplied.run();
        }

        JOptionPane.showMessageDialog(this,
            "设置已保存！\n\n" +
            "窗口将自动重启以应用所有设置。\n" +
            "✓ 主题颜色\n" +
            "✓ 字体大小\n" +
            "✓ 窗口透明度\n" +
            "✓ 背景图片\n" +
            "✓ 所有配置",
            "设置已保存",
            JOptionPane.INFORMATION_MESSAGE);
    }
}
