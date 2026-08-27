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
    private String[] sections = {"通用设置", "Java 与启动", "外观个性化", "布局模板", "备份设置", "内网穿透", "AI 设置", "系统工具", "关于"};
    private String[] sectionIcons = {"⚙️", "☕", "🎨", "📐", "💾", "🌐", "🤖", "🔧", "ℹ️"};
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
                // System Tools section
                JPanel toolsPanel = new JPanel();
                toolsPanel.setLayout(new BoxLayout(toolsPanel, BoxLayout.Y_AXIS));
                toolsPanel.setBackground(tm.bgCard());
                toolsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                JLabel toolsTitle = new JLabel("🔧 系统工具");
                toolsTitle.setFont(FontUtil.getFont(Font.BOLD, 18));
                toolsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                toolsPanel.add(toolsTitle);
                toolsPanel.add(Box.createVerticalStrut(15));

                // OpenSSH setup
                JPanel sshCard = new JPanel(new BorderLayout());
                sshCard.setBackground(tm.bgSecondary());
                sshCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(tm.border(), 1, true),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
                JLabel sshTitle = new JLabel("📡 Windows OpenSSH 一键开启");
                sshTitle.setFont(FontUtil.getFont(Font.BOLD, 14));
                JLabel sshDesc = new JLabel("<html>自动安装 OpenSSH 服务器、启动服务、设置开机自启、添加防火墙规则（22端口）<br>需要管理员权限，点击后会弹出 UAC 确认窗口</html>");
                sshDesc.setForeground(tm.textSecondary());
                sshDesc.setFont(FontUtil.getFont(Font.PLAIN, 12));
                JButton sshBtn = new JButton("🚀 一键开启 OpenSSH");
                sshBtn.setBackground(tm.accent());
                sshBtn.setForeground(Color.WHITE);
                sshBtn.setFocusPainted(false);
                sshBtn.setFont(FontUtil.getFont(Font.BOLD, 13));
                sshBtn.addActionListener(e -> setupOpenSSH());
                JPanel sshBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                sshBtnPanel.setBackground(tm.bgSecondary());
                sshBtnPanel.add(sshBtn);
                sshCard.add(sshTitle, BorderLayout.NORTH);
                sshCard.add(sshDesc, BorderLayout.CENTER);
                sshCard.add(sshBtnPanel, BorderLayout.SOUTH);
                sshCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                toolsPanel.add(sshCard);
                toolsPanel.add(Box.createVerticalStrut(15));

                // SSH User Management card
                JPanel userCard = new JPanel(new BorderLayout());
                userCard.setBackground(tm.bgSecondary());
                userCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(tm.border(), 1, true),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
                JLabel userTitle = new JLabel("👥 SSH 用户管理");
                userTitle.setFont(FontUtil.getFont(Font.BOLD, 14));
                JLabel userDesc = new JLabel("<html>创建/删除 SSH 用户，修改密码。创建后其他管理员可用该用户远程连接管理服务器。<br>需要管理员权限，仅支持 Windows。</html>");
                userDesc.setForeground(tm.textSecondary());
                userDesc.setFont(FontUtil.getFont(Font.PLAIN, 12));

                JPanel userBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
                userBtnPanel.setBackground(tm.bgSecondary());
                JButton createUserBtn = new JButton("➕ 创建用户");
                createUserBtn.setBackground(new Color(60, 140, 80));
                createUserBtn.setForeground(Color.WHITE);
                createUserBtn.setFocusPainted(false);
                createUserBtn.addActionListener(e -> createSSHUser());
                JButton deleteUserBtn = new JButton("🗑️ 删除用户");
                deleteUserBtn.setBackground(new Color(160, 60, 60));
                deleteUserBtn.setForeground(Color.WHITE);
                deleteUserBtn.setFocusPainted(false);
                deleteUserBtn.addActionListener(e -> deleteSSHUser());
                JButton changePassBtn = new JButton("🔑 修改密码");
                changePassBtn.setBackground(tm.accent());
                changePassBtn.setForeground(Color.WHITE);
                changePassBtn.setFocusPainted(false);
                changePassBtn.addActionListener(e -> changeSSHUserPassword());
                JButton listUserBtn = new JButton("📋 查看用户");
                listUserBtn.setBackground(new Color(80, 100, 140));
                listUserBtn.setForeground(Color.WHITE);
                listUserBtn.setFocusPainted(false);
                listUserBtn.addActionListener(e -> listSSHUsers());
                userBtnPanel.add(createUserBtn);
                userBtnPanel.add(deleteUserBtn);
                userBtnPanel.add(changePassBtn);
                userBtnPanel.add(listUserBtn);

                userCard.add(userTitle, BorderLayout.NORTH);
                userCard.add(userDesc, BorderLayout.CENTER);
                userCard.add(userBtnPanel, BorderLayout.SOUTH);
                userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                toolsPanel.add(userCard);
                toolsPanel.add(Box.createVerticalStrut(15));

                // SSH User Permission Management card
                JPanel permCard = new JPanel(new BorderLayout());
                permCard.setBackground(tm.bgSecondary());
                permCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(tm.border(), 1, true),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
                JLabel permTitle = new JLabel("🔐 SSH 用户权限与文件访问管理");
                permTitle.setFont(FontUtil.getFont(Font.BOLD, 14));
                JLabel permDesc = new JLabel("<html>管理SSH用户的目录访问权限、禁用/启用用户、设置文件读写权限。<br>需要管理员权限，仅支持 Windows。</html>");
                permDesc.setForeground(tm.textSecondary());
                permDesc.setFont(FontUtil.getFont(Font.PLAIN, 12));

                JPanel permBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
                permBtnPanel.setBackground(tm.bgSecondary());
                JButton viewPermBtn = new JButton("📋 查看用户权限");
                viewPermBtn.setBackground(new Color(80, 100, 140));
                viewPermBtn.setForeground(Color.WHITE);
                viewPermBtn.setFocusPainted(false);
                viewPermBtn.addActionListener(e -> viewUserPermissions());
                JButton disableUserBtn = new JButton("🚫 禁用用户");
                disableUserBtn.setBackground(new Color(160, 80, 60));
                disableUserBtn.setForeground(Color.WHITE);
                disableUserBtn.setFocusPainted(false);
                disableUserBtn.addActionListener(e -> disableSSHUser());
                JButton enableUserBtn = new JButton("✅ 启用用户");
                enableUserBtn.setBackground(new Color(60, 140, 80));
                enableUserBtn.setForeground(Color.WHITE);
                enableUserBtn.setFocusPainted(false);
                enableUserBtn.addActionListener(e -> enableSSHUser());
                JButton setDirPermBtn = new JButton("📁 设置目录权限");
                setDirPermBtn.setBackground(tm.accent());
                setDirPermBtn.setForeground(Color.WHITE);
                setDirPermBtn.setFocusPainted(false);
                setDirPermBtn.addActionListener(e -> setDirectoryPermissions());
                permBtnPanel.add(viewPermBtn);
                permBtnPanel.add(disableUserBtn);
                permBtnPanel.add(enableUserBtn);
                permBtnPanel.add(setDirPermBtn);

                permCard.add(permTitle, BorderLayout.NORTH);
                permCard.add(permDesc, BorderLayout.CENTER);
                permCard.add(permBtnPanel, BorderLayout.SOUTH);
                permCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                toolsPanel.add(permCard);
                toolsPanel.add(Box.createVerticalStrut(15));

                // Status info
                JLabel statusHint = new JLabel("<html><b>提示：</b>如果连接失败，请检查：<br>1. 两台电脑在同一局域网（或使用内网穿透）<br>2. 防火墙已放行 22 端口<br>3. 用户名密码正确（Windows用户名区分大小写）<br>4. SSH服务已启动（services.msc 查看 sshd）</html>");
                statusHint.setForeground(tm.textSecondary());
                statusHint.setFont(FontUtil.getFont(Font.PLAIN, 12));
                statusHint.setAlignmentX(Component.LEFT_ALIGNMENT);
                toolsPanel.add(statusHint);
                toolsPanel.add(Box.createVerticalGlue());

                JScrollPane toolsScroll = new JScrollPane(toolsPanel);
                toolsScroll.setBorder(null);
                toolsScroll.setBackground(tm.bgCard());
                panel.add(toolsScroll);
                break;
            case 8:
                JLabel about = new JLabel("<html><div style='font-size:13px;'>" +
                    "<b>MC-Servers-Tools</b><br><br>" +
                    "版本: 4.3<br>" +
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
                    "<b>v4.3 更新说明</b><br>" +
                    "• 维护版本，稳定性提升<br>" +
                    "• SSH文件浏览和用户权限管理优化<br><br>" +
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

    private void setupOpenSSH() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "即将一键开启 Windows OpenSSH 服务器，包括：\n" +
            "1. 安装 OpenSSH 服务器（如未安装）\n" +
            "2. 启动 sshd 服务\n" +
            "3. 设置开机自动启动\n" +
            "4. 添加防火墙规则（放行22端口）\n\n" +
            "需要管理员权限，点击确定后会弹出 UAC 确认窗口。\n是否继续？",
            "一键开启 OpenSSH", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        try {
            String tempDir = System.getProperty("java.io.tmpdir");

            // Generate PowerShell script with error handling
            String psScript = "# MC-Servers-Tools OpenSSH Setup Script\n" +
                "$ErrorActionPreference = 'Continue'\n" +
                "Write-Host ''\n" +
                "Write-Host '========================================' -ForegroundColor Cyan\n" +
                "Write-Host '  MC-Servers-Tools - OpenSSH 一键配置' -ForegroundColor Cyan\n" +
                "Write-Host '========================================' -ForegroundColor Cyan\n" +
                "Write-Host ''\n" +
                "try {\n" +
                "    # 1. Install OpenSSH Server\n" +
                "    Write-Host '[1/4] 检查 OpenSSH 服务器安装状态...' -ForegroundColor Yellow\n" +
                "    $sshInstalled = Get-WindowsCapability -Online | Where-Object { $_.Name -like 'OpenSSH.Server*' }\n" +
                "    if ($sshInstalled.State -ne 'Installed') {\n" +
                "        Write-Host '  正在安装 OpenSSH 服务器（可能需要几分钟）...' -ForegroundColor Gray\n" +
                "        Add-WindowsCapability -Online -Name 'OpenSSH.Server~~~~0.0.1.0' | Out-Null\n" +
                "        Write-Host '  安装完成！' -ForegroundColor Green\n" +
                "    } else {\n" +
                "        Write-Host '  OpenSSH 服务器已安装' -ForegroundColor Green\n" +
                "    }\n" +
                "    Write-Host ''\n" +
                "    # 2. Start sshd service\n" +
                "    Write-Host '[2/4] 启动 sshd 服务...' -ForegroundColor Yellow\n" +
                "    Start-Service sshd -ErrorAction Stop\n" +
                "    Write-Host '  服务已启动' -ForegroundColor Green\n" +
                "    Write-Host ''\n" +
                "    # 3. Set automatic startup\n" +
                "    Write-Host '[3/4] 设置开机自动启动...' -ForegroundColor Yellow\n" +
                "    Set-Service -Name sshd -StartupType Automatic\n" +
                "    Write-Host '  已设置为自动启动' -ForegroundColor Green\n" +
                "    Write-Host ''\n" +
                "    # 4. Add firewall rule\n" +
                "    Write-Host '[4/4] 添加防火墙规则（22端口）...' -ForegroundColor Yellow\n" +
                "    $existingRule = Get-NetFirewallRule -Name 'sshd' -ErrorAction SilentlyContinue\n" +
                "    if (-not $existingRule) {\n" +
                "        New-NetFirewallRule -Name 'sshd' -DisplayName 'OpenSSH Server (sshd)' -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22 | Out-Null\n" +
                "        Write-Host '  防火墙规则已添加' -ForegroundColor Green\n" +
                "    } else {\n" +
                "        Write-Host '  防火墙规则已存在' -ForegroundColor Green\n" +
                "    }\n" +
                "    Write-Host ''\n" +
                "    Write-Host '========================================' -ForegroundColor Green\n" +
                "    Write-Host '  配置完成！' -ForegroundColor Green\n" +
                "    Write-Host '========================================' -ForegroundColor Green\n" +
                "    Write-Host ''\n" +
                "    $serviceStatus = (Get-Service sshd).Status\n" +
                "    Write-Host \"SSH 服务状态: $serviceStatus\" -ForegroundColor Cyan\n" +
                "    Write-Host '监听端口: 22' -ForegroundColor Cyan\n" +
                "    $ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' -and $_.PrefixOrigin -eq 'Dhcp' -or $_.PrefixOrigin -eq 'Manual' } | Select-Object -First 1).IPAddress\n" +
                "    if (-not $ip) { $ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } | Select-Object -First 1).IPAddress }\n" +
                "    Write-Host \"本机IP: $ip\" -ForegroundColor Cyan\n" +
                "    Write-Host \"连接命令: ssh 用户名@$ip\" -ForegroundColor Cyan\n" +
                "    Write-Host ''\n" +
                "} catch {\n" +
                "    Write-Host ''\n" +
                "    Write-Host '========================================' -ForegroundColor Red\n" +
                "    Write-Host '  配置过程中出现错误' -ForegroundColor Red\n" +
                "    Write-Host '========================================' -ForegroundColor Red\n" +
                "    Write-Host ''\n" +
                "    Write-Host \"错误信息: $($_.Exception.Message)\" -ForegroundColor Red\n" +
                "    Write-Host ''\n" +
                "    Write-Host '可能的原因:' -ForegroundColor Yellow\n" +
                "    Write-Host '  1. 未以管理员身份运行' -ForegroundColor Gray\n" +
                "    Write-Host '  2. Windows 版本不支持 OpenSSH 服务器（家庭版）' -ForegroundColor Gray\n" +
                "    Write-Host '  3. 网络问题导致安装失败' -ForegroundColor Gray\n" +
                "    Write-Host ''\n" +
                "}\n" +
                "Write-Host '按任意键关闭窗口...' -ForegroundColor Gray\n" +
                "$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')\n";

            File psFile = new File(tempDir, "mcservers_tools_openssh_setup.ps1");
            try (FileWriter fw = new FileWriter(psFile)) {
                fw.write(psScript);
            }

            // Generate self-elevating batch file
            String batScript = "@echo off\n" +
                "chcp 65001 >nul\n" +
                "title MC-Servers-Tools - OpenSSH 一键配置\n" +
                ":: Check admin privileges\n" +
                "net session >nul 2>&1\n" +
                "if %errorLevel% neq 0 (\n" +
                "    echo 正在请求管理员权限...\n" +
                "    powershell -Command \"Start-Process cmd -ArgumentList '/c','\"%~f0\"' -Verb RunAs\"\n" +
                "    exit /b\n" +
                ")\n" +
                ":: Run PowerShell script\n" +
                "powershell -NoExit -ExecutionPolicy Bypass -File \"" + psFile.getAbsolutePath().replace("\\", "\\\\") + "\"\n";

            File batFile = new File(tempDir, "mcservers_tools_openssh_setup.bat");
            try (FileWriter fw = new FileWriter(batFile)) {
                fw.write(batScript);
            }

            // Run the batch file
            Runtime.getRuntime().exec("cmd /c \"" + batFile.getAbsolutePath() + "\"");

            JOptionPane.showMessageDialog(this,
                "已启动 OpenSSH 配置工具！\n\n" +
                "1. 会弹出 UAC 窗口，请点击\"是\"\n" +
                "2. 然后会打开 PowerShell 窗口自动配置\n" +
                "3. 配置完成后显示本机IP和连接命令\n" +
                "4. 按任意键关闭窗口\n\n" +
                "如果窗口闪退，请手动以管理员身份运行：\n" + batFile.getAbsolutePath(),
                "OpenSSH 配置已启动", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage() +
                "\n\n手动配置方法：\n1. 以管理员身份打开 PowerShell\n2. 运行: Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0\n3. 运行: Start-Service sshd\n4. 运行: Set-Service -Name sshd -StartupType Automatic\n5. 运行: New-NetFirewallRule -Name sshd -DisplayName 'OpenSSH Server' -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22",
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createSSHUser() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("用户名:"));
        panel.add(userField);
        panel.add(new JLabel("密码:"));
        panel.add(passField);
        panel.add(new JLabel("<html><font color='gray'>创建后该用户可通过 SSH 远程连接此电脑</font></html>"));
        int result = JOptionPane.showConfirmDialog(this, panel, "创建 SSH 用户", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String batContent = "@echo off\n" +
                "chcp 65001 >nul\n" +
                "title 创建 SSH 用户\n" +
                "echo 正在创建用户: " + username + "\n" +
                "net user " + username + " " + password + " /add\n" +
                "if %errorlevel% equ 0 (\n" +
                "    echo 用户创建成功！\n" +
                "    echo.\n" +
                "    echo 该用户现在可以通过 SSH 连接:\n" +
                "    echo   ssh " + username + "@IP地址\n" +
                ") else (\n" +
                "    echo 用户创建失败，可能用户名已存在\n" +
                ")\n" +
                "echo.\n" +
                "pause\n";
            File batFile = new File(tempDir, "create_ssh_user.bat");
            try (FileWriter fw = new FileWriter(batFile)) { fw.write(batContent); }
            Runtime.getRuntime().exec("cmd /c \"" + batFile.getAbsolutePath() + "\"");
            JOptionPane.showMessageDialog(this,
                "已启动创建用户脚本！\n\n" +
                "1. 弹出 UAC 窗口点\"是\"\n" +
                "2. 等待创建完成\n" +
                "3. 创建成功后显示连接命令\n\n" +
                "用户名: " + username + "\n" +
                "连接命令: ssh " + username + "@IP地址",
                "创建 SSH 用户", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSSHUser() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = JOptionPane.showInputDialog(this, "输入要删除的用户名:", "删除 SSH 用户", JOptionPane.QUESTION_MESSAGE);
        if (username == null || username.trim().isEmpty()) return;
        username = username.trim();
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要删除用户 \"" + username + "\" 吗？\n\n该用户将无法再通过 SSH 连接此电脑。",
            "确认删除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String batContent = "@echo off\n" +
                "chcp 65001 >nul\n" +
                "title 删除 SSH 用户\n" +
                "echo 正在删除用户: " + username + "\n" +
                "net user " + username + " /delete\n" +
                "if %errorlevel% equ 0 (\n" +
                "    echo 用户删除成功！\n" +
                ") else (\n" +
                "    echo 用户删除失败，可能用户不存在\n" +
                ")\n" +
                "echo.\n" +
                "pause\n";
            File batFile = new File(tempDir, "delete_ssh_user.bat");
            try (FileWriter fw = new FileWriter(batFile)) { fw.write(batContent); }
            Runtime.getRuntime().exec("cmd /c \"" + batFile.getAbsolutePath() + "\"");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeSSHUserPassword() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("用户名:"));
        panel.add(userField);
        panel.add(new JLabel("新密码:"));
        panel.add(passField);
        int result = JOptionPane.showConfirmDialog(this, panel, "修改 SSH 用户密码", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String batContent = "@echo off\n" +
                "chcp 65001 >nul\n" +
                "title 修改 SSH 用户密码\n" +
                "echo 正在修改用户: " + username + " 的密码\n" +
                "net user " + username + " " + password + "\n" +
                "if %errorlevel% equ 0 (\n" +
                "    echo 密码修改成功！\n" +
                ") else (\n" +
                "    echo 密码修改失败，可能用户不存在\n" +
                ")\n" +
                "echo.\n" +
                "pause\n";
            File batFile = new File(tempDir, "change_ssh_pass.bat");
            try (FileWriter fw = new FileWriter(batFile)) { fw.write(batContent); }
            Runtime.getRuntime().exec("cmd /c \"" + batFile.getAbsolutePath() + "\"");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listSSHUsers() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String batContent = "@echo off\n" +
                "chcp 65001 >nul\n" +
                "title SSH 用户列表\n" +
                "echo ========================================\n" +
                "echo   本机用户列表（可用于 SSH 登录）\n" +
                "echo ========================================\n" +
                "echo.\n" +
                "net user\n" +
                "echo.\n" +
                "echo ========================================\n" +
                "echo  SSH 连接命令: ssh 用户名@IP地址\n" +
                "echo ========================================\n" +
                "echo.\n" +
                "pause\n";
            File batFile = new File(tempDir, "list_ssh_users.bat");
            try (FileWriter fw = new FileWriter(batFile)) { fw.write(batContent); }
            Runtime.getRuntime().exec("cmd /c \"" + batFile.getAbsolutePath() + "\"");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewUserPermissions() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = JOptionPane.showInputDialog(this, "输入要查看权限的用户名:", "查看用户权限", JOptionPane.QUESTION_MESSAGE);
        if (username == null || username.trim().isEmpty()) return;
        username = username.trim();
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String batContent = "@echo off\n" +
                "chcp 65001 >nul\n" +
                "title 用户权限查看 - " + username + "\n" +
                "echo ========================================\n" +
                "echo   用户信息: " + username + "\n" +
                "echo ========================================\n" +
                "net user " + username + "\n" +
                "echo.\n" +
                "echo ========================================\n" +
                "echo   用户所属组\n" +
                "echo ========================================\n" +
                "net localgroup | findstr /i \"" + username + "\"\n" +
                "echo.\n" +
                "echo ========================================\n" +
                "echo   SSH 配置文件位置\n" +
                "echo ========================================\n" +
                "echo C:\\ProgramData\\ssh\\sshd_config\n" +
                "echo.\n" +
                "pause\n";
            File batFile = new File(tempDir, "view_user_perm.bat");
            try (FileWriter fw = new FileWriter(batFile)) { fw.write(batContent); }
            Runtime.getRuntime().exec("cmd /c \"" + batFile.getAbsolutePath() + "\"");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disableSSHUser() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = JOptionPane.showInputDialog(this, "输入要禁用的用户名:", "禁用SSH用户", JOptionPane.QUESTION_MESSAGE);
        if (username == null || username.trim().isEmpty()) return;
        username = username.trim();
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要禁用用户 \"" + username + "\" 吗？\n\n禁用后该用户将无法通过 SSH 登录。",
            "确认禁用", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String batContent = "@echo off\n" +
                "chcp 65001 >nul\n" +
                "title 禁用SSH用户\n" +
                "echo 正在禁用用户: " + username + "\n" +
                "net user " + username + " /active:no\n" +
                "if %errorlevel% equ 0 (\n" +
                "    echo 用户禁用成功！\n" +
                "    echo 该用户现在无法通过 SSH 登录\n" +
                ") else (\n" +
                "    echo 用户禁用失败，可能用户不存在\n" +
                ")\n" +
                "echo.\n" +
                "pause\n";
            File batFile = new File(tempDir, "disable_ssh_user.bat");
            try (FileWriter fw = new FileWriter(batFile)) { fw.write(batContent); }
            Runtime.getRuntime().exec("cmd /c \"" + batFile.getAbsolutePath() + "\"");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enableSSHUser() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = JOptionPane.showInputDialog(this, "输入要启用的用户名:", "启用SSH用户", JOptionPane.QUESTION_MESSAGE);
        if (username == null || username.trim().isEmpty()) return;
        username = username.trim();
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String batContent = "@echo off\n" +
                "chcp 65001 >nul\n" +
                "title 启用SSH用户\n" +
                "echo 正在启用用户: " + username + "\n" +
                "net user " + username + " /active:yes\n" +
                "if %errorlevel% equ 0 (\n" +
                "    echo 用户启用成功！\n" +
                "    echo 该用户现在可以通过 SSH 登录\n" +
                ") else (\n" +
                "    echo 用户启用失败，可能用户不存在\n" +
                ")\n" +
                "echo.\n" +
                "pause\n";
            File batFile = new File(tempDir, "enable_ssh_user.bat");
            try (FileWriter fw = new FileWriter(batFile)) { fw.write(batContent); }
            Runtime.getRuntime().exec("cmd /c \"" + batFile.getAbsolutePath() + "\"");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setDirectoryPermissions() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            JOptionPane.showMessageDialog(this, "此功能仅支持 Windows 系统", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTextField userField = new JTextField(15);
        JTextField dirField = new JTextField(20);
        String[] permOptions = {"只读 (R)", "读写 (RW)", "完全控制 (F)", "拒绝访问 (DENY)"};
        JComboBox<String> permCombo = new JComboBox<>(permOptions);
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("用户名:"));
        panel.add(userField);
        panel.add(new JLabel("目录路径 (如 C:\\MCServers):"));
        panel.add(dirField);
        panel.add(new JLabel("权限:"));
        panel.add(permCombo);
        panel.add(new JLabel("<html><font color='gray'>设置后该用户对指定目录的访问权限将被限制</font></html>"));
        int result = JOptionPane.showConfirmDialog(this, panel, "设置目录权限", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        String username = userField.getText().trim();
        String dirPath = dirField.getText().trim();
        String perm = (String) permCombo.getSelectedItem();
        if (username.isEmpty() || dirPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和目录路径不能为空", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String permCode = "R";
        if (perm.contains("读写")) permCode = "RW";
        else if (perm.contains("完全")) permCode = "F";
        else if (perm.contains("拒绝")) permCode = "DENY";
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String icaclsCmd;
            if ("DENY".equals(permCode)) {
                icaclsCmd = "icacls \"" + dirPath + "\" /deny " + username + ":(OI)(CI)F";
            } else {
                icaclsCmd = "icacls \"" + dirPath + "\" /grant " + username + ":(OI)(CI)" + permCode;
            }
            String batContent = "@echo off\n" +
                "chcp 65001 >nul\n" +
                "title 设置目录权限\n" +
                "echo 用户: " + username + "\n" +
                "echo 目录: " + dirPath + "\n" +
                "echo 权限: " + perm + "\n" +
                "echo.\n" +
                "echo 正在设置权限...\n" +
                icaclsCmd + "\n" +
                "if %errorlevel% equ 0 (\n" +
                "    echo.\n" +
                "    echo 权限设置成功！\n" +
                ") else (\n" +
                "    echo.\n" +
                "    echo 权限设置失败，请检查路径和用户名\n" +
                ")\n" +
                "echo.\n" +
                "pause\n";
            File batFile = new File(tempDir, "set_dir_perm.bat");
            try (FileWriter fw = new FileWriter(batFile)) { fw.write(batContent); }
            Runtime.getRuntime().exec("cmd /c \"" + batFile.getAbsolutePath() + "\"");
            JOptionPane.showMessageDialog(this,
                "已启动权限设置脚本！\n\n" +
                "用户: " + username + "\n" +
                "目录: " + dirPath + "\n" +
                "权限: " + perm + "\n\n" +
                "弹出 UAC 窗口点\"是\"，等待设置完成。",
                "设置目录权限", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
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
