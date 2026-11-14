package com.sausage.music.ui;

import com.sausage.music.model.Song;
import com.sausage.music.player.MusicPlayer;
import com.sausage.music.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.File;

/**
 * 正在播放展开页 - 沉浸式播放界面
 * 左侧黑胶唱片旋转动画 + 右侧歌曲信息与歌词 + 底部进度与控制
 */
public class NowPlayingPanel extends JPanel {
    private final MusicPlayer player;
    private Runnable onClose;

    private DiscPanel discPanel;
    private Timer rotateTimer;
    private float discAngle = 0f;

    private JLabel titleLabel;
    private JLabel artistLabel;
    private JLabel albumLabel;
    private JLabel lyricLabel;
    private JProgressBar progressBar;
    private JLabel timeCurrent;
    private JLabel timeTotal;
    private boolean seeking = false;
    private JButton playPauseBtn;
    private JButton modeBtn;
    private ImageIcon playIcon;
    private ImageIcon pauseIcon;
    private final String[] modeIcons = {"➡", "🔁", "🔀"};
    private final String[] modeTips = {"顺序播放", "单曲循环", "随机播放"};

    public NowPlayingPanel(MusicPlayer player) {
        this.player = player;
        playIcon = loadIcon("resources/icons/bofang.png", 30);
        pauseIcon = loadIcon("resources/icons/zanting.png", 30);
        setLayout(new BorderLayout());
        setBackground(Theme.BG_MAIN);
        buildContent();

        // 唱片旋转动画
        rotateTimer = new Timer(50, e -> {
            boolean spinning = player.isPlaying();
            if (spinning) {
                discAngle = (discAngle + 1.2f) % 360;
                discPanel.setAngle(discAngle);
            }
            discPanel.tickArm();  // 唱臂抬起/落下平滑过渡
            if (!spinning && discPanel.isArmAnimating()) discPanel.repaint();
        });
        rotateTimer.start();

        // 监听播放器事件
        player.addListener(new MusicPlayer.PlayerListener() {
            @Override public void onPlay(Song s) { SwingUtilities.invokeLater(() -> { updateSongInfo(s); discPanel.setArmOnDisc(true); }); }
            @Override public void onPause() { SwingUtilities.invokeLater(() -> { setPlayIcon(false); discPanel.setArmOnDisc(false); }); }
            @Override public void onResume() { SwingUtilities.invokeLater(() -> { setPlayIcon(true); discPanel.setArmOnDisc(true); }); }
            @Override public void onStop() { SwingUtilities.invokeLater(() -> { setPlayIcon(false); resetProgress(); discPanel.setArmOnDisc(false); }); }
            @Override public void onProgress(long cur, long total) {
                SwingUtilities.invokeLater(() -> {
                    if (!seeking) {
                        progressBar.setValue(total > 0 ? (int)(cur * 100 / total) : 0);
                        timeCurrent.setText(Song.formatTime(cur));
                        timeTotal.setText(Song.formatTime(total));
                    }
                });
            }
            @Override public void onSongEnd() { SwingUtilities.invokeLater(() -> { setPlayIcon(false); discPanel.setArmOnDisc(false); }); }
        });
    }

    public void setOnClose(Runnable r) { this.onClose = r; }

    /** 展开时同步当前播放状态 */
    public void syncState() {
        Song song = player.getCurrentSong();
        if (song != null) updateSongInfo(song);
        setPlayIcon(player.isPlaying());
        discPanel.setArmOnDisc(player.isPlaying());
    }

    private void buildContent() {
        // ---- 顶部：收起按钮 ----
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 20));
        JButton downBtn = new JButton("↓");
        downBtn.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 24));
        downBtn.setForeground(Theme.TEXT_MID);
        downBtn.setBorderPainted(false);
        downBtn.setContentAreaFilled(false);
        downBtn.setFocusPainted(false);
        downBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        downBtn.setToolTipText("收起播放页");
        downBtn.addActionListener(e -> { if (onClose != null) onClose.run(); });
        topBar.add(downBtn, BorderLayout.WEST);
        JLabel topTitle = new JLabel("正在播放");
        topTitle.setFont(Theme.NAV_FONT);
        topTitle.setForeground(Theme.TEXT_LIGHT);
        topTitle.setBorder(BorderFactory.createEmptyBorder(4, 12, 0, 0));
        topBar.add(topTitle, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        // ---- 中间：左唱片 + 右信息歌词 ----
        JPanel center = new JPanel(new GridLayout(1, 2, 40, 0));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));
        discPanel = new DiscPanel();
        center.add(discPanel);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        titleLabel = new JLabel("未播放");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 28));
        titleLabel.setForeground(Theme.TEXT_DARK);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        artistLabel = new JLabel("SAUSAGE");
        artistLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 16));
        artistLabel.setForeground(Theme.TEXT_MID);
        artistLabel.setAlignmentX(LEFT_ALIGNMENT);
        artistLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        albumLabel = new JLabel("专辑：本地音乐");
        albumLabel.setFont(Theme.SONG_SMALL);
        albumLabel.setForeground(Theme.TEXT_LIGHT);
        albumLabel.setAlignmentX(LEFT_ALIGNMENT);
        albumLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        info.add(titleLabel);
        info.add(artistLabel);
        info.add(albumLabel);
        right.add(info, BorderLayout.NORTH);
        lyricLabel = new JLabel("暂无歌词，享受纯音乐的美好时光 ♫", SwingConstants.CENTER);
        lyricLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
        lyricLabel.setForeground(Theme.TEXT_MID);
        JPanel lyricWrap = new JPanel(new GridBagLayout());
        lyricWrap.setOpaque(false);
        lyricWrap.add(lyricLabel);
        right.add(lyricWrap, BorderLayout.CENTER);
        center.add(right);
        add(center, BorderLayout.CENTER);

        // ---- 底部：进度条 + 控制按钮 ----
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 80, 28, 80));
        JPanel progressRow = new JPanel(new BorderLayout(12, 0));
        progressRow.setOpaque(false);
        timeCurrent = new JLabel("00:00");
        timeCurrent.setFont(Theme.TIME_FONT);
        timeCurrent.setForeground(Theme.TEXT_LIGHT);
        timeTotal = new JLabel("00:00");
        timeTotal.setFont(Theme.TIME_FONT);
        timeTotal.setForeground(Theme.TEXT_LIGHT);
        progressBar = createProgressBar();
        progressRow.add(timeCurrent, BorderLayout.WEST);
        progressRow.add(progressBar, BorderLayout.CENTER);
        progressRow.add(timeTotal, BorderLayout.EAST);
        bottom.add(progressRow, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 16));
        btnRow.setOpaque(false);
        modeBtn = createCtrlBtn(modeIcons[0], modeTips[0], 20);
        modeBtn.setForeground(Theme.TEXT_LIGHT);
        modeBtn.addActionListener(e -> {
            int next = (player.getPlayMode().ordinal() + 1) % 3;
            player.setPlayMode(MusicPlayer.PlayMode.values()[next]);
            modeBtn.setText(modeIcons[next]);
            modeBtn.setToolTipText(modeTips[next]);
        });
        btnRow.add(modeBtn);
        JButton prevBtn = createCtrlBtn("⏮", "上一首", 22);
        prevBtn.setForeground(Theme.TEXT_DARK);
        prevBtn.addActionListener(e -> player.playPrev());
        btnRow.add(prevBtn);
        playPauseBtn = createPlayBtn();
        setPlayIcon(false);
        btnRow.add(playPauseBtn);
        JButton nextBtn = createCtrlBtn("⏭", "下一首", 22);
        nextBtn.setForeground(Theme.TEXT_DARK);
        nextBtn.addActionListener(e -> player.playNext());
        btnRow.add(nextBtn);
        bottom.add(btnRow, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private JProgressBar createProgressBar() {
        JProgressBar bar = new JProgressBar(0, 100) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = 4;
                int y = (getHeight() - h) / 2;
                g2.setColor(new Color(0x2A, 0x2A, 0x2A));
                g2.fillRoundRect(0, y, getWidth(), h, h, h);
                int w = (int)(getWidth() * getPercentComplete());
                if (w > 0) {
                    g2.setColor(Theme.PRIMARY);
                    g2.fillRoundRect(0, y, w, h, h, h);
                    g2.fillOval(w - 5, y - 3, 10, 10);
                }
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 14));
        bar.setStringPainted(false);
        bar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { seeking = true; seekFromMouse(e); }
            @Override public void mouseReleased(MouseEvent e) { seeking = false; seekFromMouse(e); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) { if (seeking) seekFromMouse(e); }
        });
        return bar;
    }

    private void seekFromMouse(MouseEvent e) {
        Song song = player.getCurrentSong();
        if (song == null || song.getDuration() <= 0) return;
        double ratio = Math.max(0, Math.min(1.0, (double) e.getX() / progressBar.getWidth()));
        long target = (long)(song.getDuration() * ratio);
        progressBar.setValue((int)(ratio * 100));
        timeCurrent.setText(Song.formatTime(target));
        if (e.getID() == MouseEvent.MOUSE_RELEASED) player.seekTo(target);
    }

    private JButton createCtrlBtn(String text, String tip, int size) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Symbol", Font.PLAIN, size));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tip);
        return btn;
    }

    private JButton createPlayBtn() {
        JButton btn = new JButton();
        btn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 22));
        btn.setForeground(Theme.TEXT_WHITE);
        btn.setPreferredSize(new Dimension(56, 56));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (player.getCurrentSong() == null) {
                if (!player.getPlaylist().isEmpty()) player.play(0);
                return;
            }
            player.togglePause();
        });
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel m = ((AbstractButton) c).getModel();
                if (m.isPressed()) g2.setColor(Theme.PRIMARY_DARK);
                else if (m.isRollover()) g2.setColor(Theme.PRIMARY_LIGHT);
                else g2.setColor(Theme.PRIMARY);
                g2.fillOval(1, 1, c.getWidth() - 2, c.getHeight() - 2);
                g2.dispose();
                super.paint(g, c);
            }
        });
        return btn;
    }

    private void updateSongInfo(Song song) {
        titleLabel.setText(song.getTitle());
        artistLabel.setText(song.getArtist());
        albumLabel.setText("专辑：" + song.getAlbum());
        timeTotal.setText(Song.formatTime(song.getDuration()));
        setPlayIcon(true);
    }

    private void resetProgress() {
        progressBar.setValue(0);
        timeCurrent.setText("00:00");
        timeTotal.setText("00:00");
        titleLabel.setText("未播放");
        artistLabel.setText("SAUSAGE");
    }

    private void setPlayIcon(boolean playing) {
        if (playing && pauseIcon != null) {
            playPauseBtn.setIcon(pauseIcon);
            playPauseBtn.setText("");
        } else if (!playing && playIcon != null) {
            playPauseBtn.setIcon(playIcon);
            playPauseBtn.setText("");
        } else {
            playPauseBtn.setIcon(null);
            playPauseBtn.setText(playing ? "⏸" : "▶");
        }
    }

    private ImageIcon loadIcon(String path, int size) {
        try {
            File f = new File(path);
            if (f.exists()) {
                ImageIcon raw = new ImageIcon(f.getAbsolutePath());
                Image img = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** 黑胶唱片面板 - 播放时旋转 */
    private class DiscPanel extends JPanel {
        private float angle = 0;
        private float armLift = 1f;       // 唱臂抬起程度：0=落针在唱片上 1=完全抬起移出唱片
        private float targetLift = 1f;
        void setAngle(float a) { this.angle = a; repaint(); }
        /** 播放时唱臂落针，暂停/停止时抬起 */
        void setArmOnDisc(boolean on) { this.targetLift = on ? 0f : 1f; }
        boolean isArmAnimating() { return Math.abs(armLift - targetLift) > 0.005f; }
        /** 唱臂抬起/落下平滑过渡 */
        void tickArm() {
            if (isArmAnimating()) {
                armLift += (targetLift - armLift) * 0.18f;
                if (!isArmAnimating()) armLift = targetLift;
            }
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            int r = Math.min(getWidth(), getHeight()) / 2 - 16;
            if (r < 20) { g2.dispose(); return; }
            // 唱片外圈
            g2.setColor(new Color(0x11, 0x11, 0x11));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            // 纹理圈
            g2.setColor(new Color(0x1D, 0x1D, 0x1D));
            for (int i = 1; i <= 6; i++) {
                int rr = (int)(r * (0.5 + i * 0.07));
                if (rr < r) g2.drawOval(cx - rr, cy - rr, rr * 2, rr * 2);
            }
            // 旋转的中心封面
            AffineTransform old = g2.getTransform();
            g2.rotate(Math.toRadians(angle), cx, cy);
            int coverR = (int)(r * 0.5);
            GradientPaint gp = new GradientPaint(cx - coverR, cy - coverR, Theme.PRIMARY,
                    cx + coverR, cy + coverR, Theme.ACCENT);
            g2.setPaint(gp);
            g2.fillOval(cx - coverR, cy - coverR, coverR * 2, coverR * 2);
            g2.setColor(new Color(0, 0, 0, 90));
            g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 36));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("♫", cx - fm.stringWidth("♫") / 2, cy + fm.getAscent() / 2 - 2);
            g2.setTransform(old);
            // 中心孔
            g2.setColor(new Color(0x08, 0x08, 0x08));
            g2.fillOval(cx - 5, cy - 5, 10, 10);

            // ---- 唱臂（拨片）----
            // 基座位于唱片右上方外侧
            double bx = cx + r * 0.92, by = cy - r * 0.92;
            double dx = bx - cx, dy = by - cy;
            double d = Math.hypot(dx, dy);
            double ux = dx / d, uy = dy / d;
            // 落针点：唱片纹理区（距中心 0.58r）
            double px = cx + ux * r * 0.58, py = cy + uy * r * 0.58;
            double avx = px - bx, avy = py - by;
            // 抬臂：绕基座旋转，针头向远离唱片方向摆出（55° 时完全移出唱片边缘）
            double rot = Math.toRadians(armLift * 55);
            double cosR = Math.cos(rot), sinR = Math.sin(rot);
            double rax = avx * cosR - avy * sinR;
            double ray = avx * sinR + avy * cosR;
            double headX = bx + rax, headY = by + ray;
            double armAng = Math.atan2(ray, rax);

            // 配重臂（基座远离唱片一侧）
            double cwx = bx - ux * r * 0.22, cwy = by - uy * r * 0.22;
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0x3A, 0x3A, 0x3E));
            g2.drawLine((int) bx, (int) by, (int) cwx, (int) cwy);
            g2.setColor(new Color(0x55, 0x55, 0x5C));
            g2.fillOval((int) cwx - 7, (int) cwy - 7, 14, 14);

            // 基座（金属转轴）
            g2.setColor(new Color(0x2A, 0x2A, 0x2E));
            g2.fillOval((int) bx - 14, (int) by - 14, 28, 28);
            g2.setColor(new Color(0x9A, 0x9A, 0xA4));
            g2.fillOval((int) bx - 9, (int) by - 9, 18, 18);
            g2.setColor(new Color(0x6A, 0x6A, 0x72));
            g2.fillOval((int) bx - 4, (int) by - 4, 8, 8);

            // 主臂（金属渐变高光，沿臂法线方向）
            AffineTransform armOld = g2.getTransform();
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float nx = (float) (-Math.sin(armAng) * 3), ny = (float) (Math.cos(armAng) * 3);
            GradientPaint armGp = new GradientPaint(
                    (float) bx + nx, (float) by + ny, new Color(0xE6, 0xE6, 0xEC),
                    (float) bx - nx, (float) by - ny, new Color(0x80, 0x80, 0x8A));
            g2.setPaint(armGp);
            g2.drawLine((int) bx, (int) by, (int) headX, (int) headY);

            // 唱头 + 唱针（旋转对齐臂方向，局部 x+ 指向唱片中心）
            g2.translate(headX, headY);
            g2.rotate(armAng);
            g2.setColor(new Color(0x1E, 0x1E, 0x22));
            g2.fillRoundRect(-8, -7, 16, 14, 3, 3);
            g2.setColor(new Color(0x4A, 0x4A, 0x52));
            g2.fillRoundRect(-8, -7, 16, 4, 3, 3);
            g2.setColor(new Color(0xC8, 0xA8, 0x4E));  // 金色唱针
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(8, 2, 13, 2);
            g2.setTransform(armOld);

            g2.dispose();
        }
    }
}