package com.sausage.music.ui;

import com.sausage.music.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 侧边栏 - 黑白高级风导航
 */
public class SidebarPanel extends Theme.GradientPanel {
    public interface NavListener { void onNavChanged(String navKey); }

    private final List<NavListener> navListeners = new ArrayList<>();
    private final List<JButton> navButtons = new ArrayList<>();
    private final String[] navKeys = {"discover", "local", "favorite", "playlist", "mv"};
    private final String[] navLabels = {"发现音乐", "本地音乐", "我喜欢", "播放列表", "MV"};
    private final String[] navIcons = {"♪", "▤", "♥", "≡", "▶"};
    private String currentNav = "discover";

    public SidebarPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 0));

        // ---- Logo 区 ----
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 16));
        logoPanel.setOpaque(false);
        try {
            File iconFile = new File("resources/icons/sausage.png");
            if (iconFile.exists()) {
                ImageIcon raw = new ImageIcon(iconFile.getAbsolutePath());
                Image scaled = raw.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                logoPanel.add(new JLabel(new ImageIcon(scaled)));
            }
        } catch (Exception ignored) {}
        JLabel logoText = new JLabel("SAUSAGE");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoText.setForeground(Theme.TEXT_WHITE);
        logoPanel.add(logoText);
        add(logoPanel, BorderLayout.NORTH);

        // ---- 导航区 ----
        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < navKeys.length; i++) {
            JButton btn = createNavButton(navLabels[i], navIcons[i], navKeys[i]);
            navButtons.add(btn);
            navPanel.add(btn);
            navPanel.add(Box.createVerticalStrut(4));
        }
        add(navPanel, BorderLayout.CENTER);

        // ---- 底部装饰 ----
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        JLabel ver = new JLabel("v1.0 · sausage");
        ver.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        ver.setForeground(new Color(0x66, 0x66, 0x66));
        bottom.add(ver);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        add(bottom, BorderLayout.SOUTH);

        updateNavStyles();
    }

    private JButton createNavButton(String label, String icon, String key) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(10, 0));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        JLabel textLabel = new JLabel(label);
        textLabel.setFont(Theme.NAV_FONT);
        btn.add(iconLabel, BorderLayout.WEST);
        btn.add(textLabel, BorderLayout.CENTER);
        btn.putClientProperty("iconLabel", iconLabel);
        btn.putClientProperty("textLabel", textLabel);
        btn.putClientProperty("navKey", key);

        btn.addActionListener(e -> {
            currentNav = key;
            updateNavStyles();
            for (NavListener l : navListeners) l.onNavChanged(key);
        });
        return btn;
    }

    private void updateNavStyles() {
        for (JButton btn : navButtons) {
            String key = (String) btn.getClientProperty("navKey");
            JLabel iconLabel = (JLabel) btn.getClientProperty("iconLabel");
            JLabel textLabel = (JLabel) btn.getClientProperty("textLabel");
            boolean active = key.equals(currentNav);
            btn.setBackground(active ? Theme.PRIMARY : new Color(0, 0, 0, 0));
            btn.setOpaque(active);
            Color fg = active ? Theme.TEXT_WHITE : Theme.TEXT_SIDEBAR;
            iconLabel.setForeground(active ? Theme.ACCENT : fg);
            textLabel.setForeground(fg);
            textLabel.setFont(active ? Theme.BOLD_FONT : Theme.NAV_FONT);
        }
    }

    public void addNavListener(NavListener l) { navListeners.add(l); }
    public String getCurrentNav() { return currentNav; }
}