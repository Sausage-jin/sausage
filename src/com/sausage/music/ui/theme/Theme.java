package com.sausage.music.ui.theme;

import javax.swing.*;
import java.awt.*;

/**
 * SAUSAGE - 黑白高级风主题
 */
public class Theme {
    // 主色调 - 高级白
    public static final Color PRIMARY      = new Color(0xE5, 0xE5, 0xE5);
    public static final Color PRIMARY_DARK = new Color(0xFF, 0xFF, 0xFF);
    public static final Color PRIMARY_LIGHT= new Color(0xC2, 0xC2, 0xC2);
    public static final Color ACCENT       = new Color(0x17, 0x17, 0x17);

    // 侧边栏 - 纯白渐变
    public static final Color SIDEBAR_DARK = new Color(0xF5, 0xF5, 0xF5);
    public static final Color SIDEBAR_MID  = new Color(0xEB, 0xEB, 0xEB);
    public static final Color SIDEBAR_LIGHT= new Color(0xE1, 0xE1, 0xE1);

    // 内容区 - 纯黑
    public static final Color BG_MAIN      = new Color(0x08, 0x08, 0x08);
    public static final Color BG_CARD      = Color.BLACK;
    public static final Color BG_HOVER     = new Color(0x0F, 0x0F, 0x0F);
    public static final Color BG_SELECTED  = new Color(0x1B, 0x1B, 0x1B);

    // 文字
    public static final Color TEXT_DARK    = new Color(0xE5, 0xE5, 0xE5);
    public static final Color TEXT_MID     = new Color(0xAA, 0xAA, 0xAA);
    public static final Color TEXT_LIGHT   = new Color(0x66, 0x66, 0x66);
    public static final Color TEXT_WHITE   = new Color(0x05, 0x05, 0x05);
    public static final Color TEXT_SIDEBAR = new Color(0x73, 0x73, 0x73);

    // 边框与阴影
    public static final Color BORDER       = new Color(0x1F, 0x1F, 0x1F);
    public static final Color SHADOW       = new Color(0xF5, 0xF5, 0xF5, 40);

    // 播放栏
    public static final Color PLAYER_BG    = Color.BLACK;

    // 字体
    public static Font TITLE_FONT   = new Font("Microsoft YaHei UI", Font.BOLD, 20);
    public static Font LOGO_FONT    = new Font("Segoe UI", Font.BOLD, 18);
    public static Font NAV_FONT     = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
    public static Font SONG_FONT    = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
    public static Font SONG_SMALL   = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
    public static Font TIME_FONT    = new Font("Consolas", Font.PLAIN, 12);
    public static Font BOLD_FONT    = new Font("Microsoft YaHei UI", Font.BOLD, 14);

    /** 圆角面板 */
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private Color bg = BG_CARD;
        public RoundedPanel(int radius) { super(); this.radius = radius; setOpaque(false); }
        public RoundedPanel(int radius, Color bg) { super(); this.radius = radius; this.bg = bg; setOpaque(false); }
        public void setBg(Color c) { this.bg = c; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 圆角按钮 */
    public static class RoundedButton extends JButton {
        private final int radius;
        private Color normalBg, hoverBg, pressBg;
        private Color normalFg;
        public RoundedButton(String text, int radius, Color bg, Color fg) {
            super(text);
            this.radius = radius; this.normalBg = bg; this.hoverBg = brighter(bg);
            this.pressBg = darker(bg); this.normalFg = fg;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setForeground(fg); setFont(NAV_FONT);
        }
        private Color brighter(Color c) { return new Color(Math.min(c.getRed()+25,255), Math.min(c.getGreen()+25,255), Math.min(c.getBlue()+25,255)); }
        private Color darker(Color c) { return new Color(Math.max(c.getRed()-25,0), Math.max(c.getGreen()-25,0), Math.max(c.getBlue()-25,0)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel m = getModel();
            g2.setColor(m.isPressed() ? pressBg : (m.isRollover() ? hoverBg : normalBg));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 侧边栏渐变面板 - 纯白渐变 */
    public static class GradientPanel extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, SIDEBAR_DARK, 0, getHeight(), SIDEBAR_LIGHT);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}