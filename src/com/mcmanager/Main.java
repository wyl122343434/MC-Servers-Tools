package com.mcmanager;
import com.mcmanager.ui.MainWindow;
import com.mcmanager.util.FontUtil;
import javax.swing.*;
public class Main {
    public static void main(String[] args) {
        FontUtil.init();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
