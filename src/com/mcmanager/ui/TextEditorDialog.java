package com.mcmanager.ui;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.*;
import java.util.regex.*;

public class TextEditorDialog extends JDialog {
    private JTextPane textPane;
    private String filePath;
    private String content;
    private boolean modified = false;
    private JLabel statusLabel;
    private boolean isRemote = false;
    private RemoteFileHandler remoteHandler;
    
    public interface RemoteFileHandler {
        String readFile(String path) throws Exception;
        void writeFile(String path, String content) throws Exception;
    }
    
    public TextEditorDialog(Frame parent, String filePath, boolean isRemote, RemoteFileHandler remoteHandler) {
        super(parent, "编辑器 - " + filePath, true);
        this.filePath = filePath;
        this.isRemote = isRemote;
        this.remoteHandler = remoteHandler;
        initUI();
        loadFile();
    }
    
    private void initUI() {
        setSize(800, 600);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        // Toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        
        JButton saveBtn = new JButton("保存 (Ctrl+S)");
        saveBtn.addActionListener(e -> saveFile());
        toolbar.add(saveBtn);
        
        toolbar.addSeparator();
        
        JButton reloadBtn = new JButton("重新加载");
        reloadBtn.addActionListener(e -> {
            if (modified && JOptionPane.showConfirmDialog(this, "文件已修改，重新加载会丢失更改，继续？") != JOptionPane.YES_OPTION) return;
            loadFile();
        });
        toolbar.add(reloadBtn);
        
        toolbar.addSeparator();
        
        JLabel fontLabel = new JLabel("字体:");
        toolbar.add(fontLabel);
        String[] fonts = {"12", "14", "16", "18", "20", "24"};
        JComboBox<String> fontCombo = new JComboBox<>(fonts);
        fontCombo.setSelectedItem("14");
        fontCombo.addActionListener(e -> {
            int size = Integer.parseInt((String) fontCombo.getSelectedItem());
            textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, size));
        });
        toolbar.add(fontCombo);
        
        toolbar.add(Box.createHorizontalGlue());
        
        JButton closeBtn = new JButton("关闭");
        closeBtn.addActionListener(e -> closeEditor());
        toolbar.add(closeBtn);
        
        add(toolbar, BorderLayout.NORTH);
        
        // Text area with line numbers
        textPane = new JTextPane();
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setRowHeaderView(new LineNumberView(textPane));
        add(scrollPane, BorderLayout.CENTER);
        
        // Status bar
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        add(statusLabel, BorderLayout.SOUTH);
        
        // Keyboard shortcut
        textPane.getInputMap().put(KeyStroke.getKeyStroke("ctrl S"), "save");
        textPane.getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveFile();
            }
        });
        
        textPane.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { modified = true; updateTitle(); }
            @Override public void removeUpdate(DocumentEvent e) { modified = true; updateTitle(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeEditor();
            }
        });
    }
    
    private void updateTitle() {
        setTitle("编辑器 - " + filePath + (modified ? " *" : ""));
    }
    
    private void loadFile() {
        try {
            if (isRemote && remoteHandler != null) {
                content = remoteHandler.readFile(filePath);
            } else {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                content = sb.toString();
            }
            textPane.setText(content);
            modified = false;
            updateTitle();
            applySyntaxHighlighting();
            statusLabel.setText("已加载 | " + content.length() + " 字符 | " + (isRemote ? "远程文件" : "本地文件"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载文件失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("加载失败: " + e.getMessage());
        }
    }
    
    private void saveFile() {
        try {
            String newContent = textPane.getText();
            if (isRemote && remoteHandler != null) {
                remoteHandler.writeFile(filePath, newContent);
            } else {
                // Backup
                File src = new File(filePath);
                File backup = new File(filePath + ".bak");
                if (src.exists()) {
                    java.nio.file.Files.copy(src.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")) {
                    writer.write(newContent);
                }
            }
            content = newContent;
            modified = false;
            updateTitle();
            statusLabel.setText("已保存 | " + new java.util.Date());
            JOptionPane.showMessageDialog(this, "保存成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void closeEditor() {
        if (modified) {
            int result = JOptionPane.showConfirmDialog(this, "文件已修改，是否保存？", "提示", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                saveFile();
                dispose();
            } else if (result == JOptionPane.NO_OPTION) {
                dispose();
            }
        } else {
            dispose();
        }
    }
    
    private void applySyntaxHighlighting() {
        // Simple syntax highlighting for .properties and .json files
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".properties")) {
            highlightProperties();
        } else if (lowerPath.endsWith(".json")) {
            highlightJson();
        }
    }
    
    private void highlightProperties() {
        StyledDocument doc = textPane.getStyledDocument();
        String text = textPane.getText();
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style keyStyle = sc.addStyle("key", null);
        StyleConstants.setForeground(keyStyle, new Color(0, 0, 180));
        StyleConstants.setBold(keyStyle, true);
        Style commentStyle = sc.addStyle("comment", null);
        StyleConstants.setForeground(commentStyle, new Color(0, 128, 0));
        StyleConstants.setItalic(commentStyle, true);
        
        String[] lines = text.split("\n");
        int pos = 0;
        for (String line : lines) {
            if (line.trim().startsWith("#") || line.trim().startsWith("!")) {
                doc.setCharacterAttributes(pos, line.length(), commentStyle, false);
            } else {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    doc.setCharacterAttributes(pos, eq, keyStyle, false);
                }
            }
            pos += line.length() + 1;
        }
    }
    
    private void highlightJson() {
        StyledDocument doc = textPane.getStyledDocument();
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style keyStyle = sc.addStyle("jsonkey", null);
        StyleConstants.setForeground(keyStyle, new Color(0, 0, 180));
        Style stringStyle = sc.addStyle("jsonstring", null);
        StyleConstants.setForeground(stringStyle, new Color(0, 128, 0));
        Style numberStyle = sc.addStyle("jsonnum", null);
        StyleConstants.setForeground(numberStyle, new Color(180, 0, 0));
        
        String text = textPane.getText();
        Pattern pattern = Pattern.compile("\"([^\"]*)\"\\s*:|\"([^\"]*)\"|\\b\\d+\\.?\\d*\\b|true|false|null");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), keyStyle, false);
            } else if (matcher.group(2) != null) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), stringStyle, false);
            } else {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), numberStyle, false);
            }
        }
    }
    
    // Line number component
    class LineNumberView extends JComponent {
        private JTextPane textPane;
        private static final int MARGIN = 5;
        
        public LineNumberView(JTextPane textPane) {
            this.textPane = textPane;
            setPreferredSize(new Dimension(40, 0));
            setBackground(new Color(240, 240, 240));
            textPane.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { repaint(); }
                @Override public void removeUpdate(DocumentEvent e) { repaint(); }
                @Override public void changedUpdate(DocumentEvent e) {}
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(220, 220, 220));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(120, 120, 120));
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            
            FontMetrics fm = g.getFontMetrics();
            int lineHeight = fm.getHeight();
            int viewWidth = textPane.getVisibleRect().width;
            
            Rectangle clip = g.getClipBounds();
            int startY = clip.y;
            int endY = clip.y + clip.height;
            
            int lineCount = textPane.getText().split("\n").length;
            for (int i = 0; i < lineCount; i++) {
                int y = i * lineHeight + fm.getAscent();
                if (y >= startY - lineHeight && y <= endY + lineHeight) {
                    String num = String.valueOf(i + 1);
                    int x = getWidth() - fm.stringWidth(num) - MARGIN;
                    g.drawString(num, x, y);
                }
            }
        }
    }
}
