package com.sausage.music.ui;

import com.sausage.music.model.Song;
import com.sausage.music.player.MusicPlayer;
import com.sausage.music.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * 底部播放栏 - 黑白高级风控制
 */
public class PlayerBar extends JPanel {
    private final MusicPlayer player;

    private JLabel songTitleLabel;
    private JLabel songArtistLabel;
    private JPanel coverPanel;
    private JButton playPauseBtn;
    private JProgressBar progressBar;
    private JLabel timeCurrent;
    private JLabel timeTotal;
    private JButton modeBtn;
    private JSlider volumeSlider;
    private boolean seeking = false;
    private Runnable onExpand;

    // 播放/暂停图标
    private ImageIcon playIcon;   // bofang.png
    private ImageIcon pauseIcon;  // zanting.png

    private final String[] modeIcons = {"➡", "🔁", "🔀"};
    private final String[] modeTips = {"顺序播放", "单曲循环", "随机播放"};

    public PlayerBar(MusicPlayer player) {
        this.player = player;
        // 加载播放/暂停图标
        playIcon = loadIcon("resources/icons/bofang.png", 22);
        pauseIcon = loadIcon("resources/icons/zanting.png", 22);
        setLayout(new BorderLayout());
        setBackground(new Color(0x1A, 0x1A, 0x1A));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        setPreferredSize(new Dimension(0, 80));

        buildLeft();
        buildCenter();
        buildRight();

        // 监听播放器事件
        player.addListener(new MusicPlayer.PlayerListener() {
            @Override public void onPlay(Song song) { SwingUtilities.invokeLater(() -> updateSongInfo(song)); }
            @Override public void onPause() { SwingUtilities.invokeLater(() -> setPlayIcon(false)); }
            @Override public void onResume() { SwingUtilities.invokeLater(() -> setPlayIcon(true)); }
            @Override public void onStop() { SwingUtilities.invokeLater(() -> { setPlayIcon(false); resetProgress(); }); }
            @Override public void onProgress(long cur, long total) {
                SwingUtilities.invokeLater(() -> {
                    if (!seeking) {
                        int pct = total > 0 ? (int)(cur * 100 / total) : 0;
                        progressBar.setValue(pct);
                        timeCurrent.setText(Song.formatTime(cur));
                        timeTotal.setText(Song.formatTime(total));
                    }
                });
            }
            @Override public void onSongEnd() { SwingUtilities.invokeLater(() -> { setPlayIcon(false); }); }
        });
    }

    // ==================== 左侧：封面 + 歌曲信息 ====================
    private void buildLeft() {
        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        // 固定左侧宽度，防止切歌时歌名长短不同导致中间控制区左右移动
        left.setPreferredSize(new Dimension(330, 56));

        // 封面（渐变圆角方块）
        coverPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), getHeight(), Theme.ACCENT);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0, 0, 0, 90));
                g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("♫", (getWidth() - fm.stringWidth("♫")) / 2, (getHeight() + fm.getAscent()) / 2 - 2);
                g2.dispose();
            }
        };
        coverPanel.setPreferredSize(new Dimension(56, 56));
        left.add(coverPanel, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        songTitleLabel = new JLabel("未播放");
        songTitleLabel.setFont(Theme.BOLD_FONT);
        songTitleLabel.setForeground(Theme.TEXT_DARK);
        songTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        songArtistLabel = new JLabel("SAUSAGE");
        songArtistLabel.setFont(Theme.SONG_SMALL);
        songArtistLabel.setForeground(Theme.TEXT_LIGHT);
        songArtistLabel.setAlignmentX(LEFT_ALIGNMENT);
        info.add(songTitleLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(songArtistLabel);
        left.add(info, BorderLayout.CENTER);

        // 展开按钮
        ImageIcon expandIcon = loadIcon("resources/icons/expand-512x512.png", 16);
        JButton expandBtn = new JButton();
        if (expandIcon != null) expandBtn.setIcon(expandIcon);
        else { expandBtn.setText("⤢"); expandBtn.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14)); }
        expandBtn.setForeground(Theme.TEXT_LIGHT);
        expandBtn.setBorderPainted(false);
        expandBtn.setContentAreaFilled(false);
        expandBtn.setFocusPainted(false);
        expandBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        expandBtn.setToolTipText("展开播放页");
        expandBtn.addActionListener(e -> { if (onExpand != null) onExpand.run(); });
        left.add(expandBtn, BorderLayout.EAST);

        // 点击左侧歌曲信息区域 → 展开正在播放页
        left.setCursor(new Cursor(Cursor.HAND_CURSOR));
        left.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (onExpand != null) onExpand.run();
            }
        });

        add(left, BorderLayout.WEST);
    }

    // ==================== 中间：控制按钮 + 进度条 ====================
    private void buildCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        // 按钮行
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnRow.setOpaque(false);

        // 播放模式
        modeBtn = createCtrlBtn(modeIcons[0], modeTips[0], 16);
        modeBtn.setForeground(Theme.TEXT_LIGHT);
        modeBtn.addActionListener(e -> {
            int next = (player.getPlayMode().ordinal() + 1) % 3;
            player.setPlayMode(MusicPlayer.PlayMode.values()[next]);
            modeBtn.setText(modeIcons[next]);
            modeBtn.setToolTipText(modeTips[next]);
        });
        btnRow.add(modeBtn);

        // 上一首
        JButton prevBtn = createCtrlBtn("⏮", "上一首", 18);
        prevBtn.setForeground(Theme.TEXT_DARK);
        prevBtn.addActionListener(e -> player.playPrev());
        btnRow.add(prevBtn);

        // 播放/暂停（大蓝色圆钮）
        playPauseBtn = new JButton();
        playPauseBtn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 20));
        playPauseBtn.setForeground(Theme.TEXT_WHITE);
        playPauseBtn.setPreferredSize(new Dimension(44, 44));
        playPauseBtn.setFocusPainted(false);
        playPauseBtn.setBorderPainted(false);
        playPauseBtn.setContentAreaFilled(false);
        playPauseBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playPauseBtn.addActionListener(e -> {
            if (player.getCurrentSong() == null) {
                if (!player.getPlaylist().isEmpty()) player.play(0);
                return;
            }
            player.togglePause();
        });
        playPauseBtn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                AbstractButton b = (AbstractButton) c;
                ButtonModel m = b.getModel();
                if (m.isPressed()) g2.setColor(Theme.PRIMARY_DARK);
                else if (m.isRollover()) g2.setColor(Theme.PRIMARY_LIGHT);
                else g2.setColor(Theme.PRIMARY);
                g2.fillOval(1, 1, c.getWidth() - 2, c.getHeight() - 2);
                g2.dispose();
                super.paint(g, c);
            }
        });
        setPlayIcon(false); // 初始为播放图标
        btnRow.add(playPauseBtn);

        // 下一首
        JButton nextBtn = createCtrlBtn("⏭", "下一首", 18);
        nextBtn.setForeground(Theme.TEXT_DARK);
        nextBtn.addActionListener(e -> player.playNext());
        btnRow.add(nextBtn);

        center.add(btnRow, BorderLayout.CENTER);

        // 进度条行
        JPanel progressRow = new JPanel(new BorderLayout(8, 0));
        progressRow.setOpaque(false);
        timeCurrent = new JLabel("00:00");
        timeCurrent.setFont(Theme.TIME_FONT);
        timeCurrent.setForeground(Theme.TEXT_LIGHT);
        timeTotal = new JLabel("00:00");
        timeTotal.setFont(Theme.TIME_FONT);
        timeTotal.setForeground(Theme.TEXT_LIGHT);

        progressBar = new JProgressBar(0, 100) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = 4;
                int y = (getHeight() - h) / 2;
                // 底轨
                g2.setColor(new Color(0xC2, 0xC2, 0xC2));
                g2.fillRoundRect(0, y, getWidth(), h, h, h);
                // 进度
                int w = (int)(getWidth() * getPercentComplete());
                if (w > 0) {
                    GradientPaint gp = new GradientPaint(0, 0, Theme.PRIMARY_LIGHT, w, 0, Theme.PRIMARY);
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, y, w, h, h, h);
                    // 滑块圆点
                    g2.setColor(Theme.PRIMARY);
                    g2.fillOval(w - 5, y - 3, 10, 10);
                }
                g2.dispose();
            }
        };
        progressBar.setPreferredSize(new Dimension(0, 14));
        progressBar.setStringPainted(false);
        progressBar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        progressBar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { seeking = true; seekFromMouse(e); }
            @Override public void mouseReleased(MouseEvent e) { seeking = false; seekFromMouse(e); }
        });
        progressBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) { if (seeking) seekFromMouse(e); }
        });

        progressRow.add(timeCurrent, BorderLayout.WEST);
        progressRow.add(progressBar, BorderLayout.CENTER);
        progressRow.add(timeTotal, BorderLayout.EAST);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        centerWrap.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));
        centerWrap.add(progressRow, BorderLayout.SOUTH);
        centerWrap.add(center, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);
    }

    private void seekFromMouse(MouseEvent e) {
        Song song = player.getCurrentSong();
        if (song == null || song.getDuration() <= 0) return;
        double ratio = Math.max(0, Math.min(1.0, (double) e.getX() / progressBar.getWidth()));
        long target = (long)(song.getDuration() * ratio);
        progressBar.setValue((int)(ratio * 100));
        timeCurrent.setText(Song.formatTime(target));
        // 仅在鼠标释放时执行真正的 seek，拖拽中只更新进度条 UI
        if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            player.seekTo(target);
        }
    }

    // ==================== 右侧：音量 ====================
    private void buildRight() {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JLabel volIcon = new JLabel("🔊");
        volIcon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        volIcon.setForeground(Theme.TEXT_LIGHT);
        right.add(volIcon);

        volumeSlider = new JSlider(0, 100, 80) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = 4;
                int y = (getHeight() - h) / 2;
                g2.setColor(new Color(0xC2, 0xC2, 0xC2));
                g2.fillRoundRect(0, y, getWidth(), h, h, h);
                int w = (int)(getWidth() * getValue() / 100.0);
                if (w > 0) {
                    g2.setColor(Theme.PRIMARY);
                    g2.fillRoundRect(0, y, w, h, h, h);
                    g2.fillOval(w - 4, y - 2, 8, 8);
                }
                g2.dispose();
            }
        };
        volumeSlider.setPreferredSize(new Dimension(100, 16));
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.addChangeListener(e -> player.setVolume(volumeSlider.getValue() / 100f));
        right.add(volumeSlider);

        add(right, BorderLayout.EAST);
    }

    // ==================== 工具方法 ====================
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

    private void updateSongInfo(Song song) {
        songTitleLabel.setText(truncateText(song.getTitle(), songTitleLabel.getFont(), 220));
        songArtistLabel.setText(truncateText(song.getArtist(), songArtistLabel.getFont(), 220));
        setPlayIcon(true);
        timeTotal.setText(Song.formatTime(song.getDuration()));
        // 封面动画：重绘
        coverPanel.repaint();
    }

    /** 按像素宽度截断文本，超出部分以省略号结尾，保证固定宽度区域内不溢出 */
    private String truncateText(String text, Font font, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        FontMetrics fm = getFontMetrics(font);
        if (fm.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        int ellW = fm.stringWidth(ellipsis);
        int len = text.length();
        while (len > 0 && fm.stringWidth(text.substring(0, len)) + ellW > maxWidth) {
            len--;
        }
        return text.substring(0, len) + ellipsis;
    }

    private void resetProgress() {
        progressBar.setValue(0);
        timeCurrent.setText("00:00");
        timeTotal.setText("00:00");
        songTitleLabel.setText("未播放");
        songArtistLabel.setText("SAUSAGE");
    }

    public void refreshPlayState() {
        setPlayIcon(player.isPlaying());
    }

    public void setOnExpand(Runnable r) { this.onExpand = r; }

    /** 加载并缩放图标 */
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

    /** 切换播放/暂停图标（图标缺失时回退文字） */
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
}