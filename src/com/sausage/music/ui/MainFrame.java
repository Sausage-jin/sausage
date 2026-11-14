package com.sausage.music.ui;

import com.sausage.music.model.Song;
import com.sausage.music.player.MusicPlayer;
import com.sausage.music.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 主窗口 - 无边框 + 自定义标题栏
 */
public class MainFrame extends JFrame {
    private final MusicPlayer player = new MusicPlayer();
    private SidebarPanel sidebar;
    private ContentPanel content;
    private PlayerBar playerBar;
    private NowPlayingPanel nowPlaying;
    private final CardLayout bodyCards = new CardLayout();
    private JPanel bodyCardHolder;

    public MainFrame() {
        setTitle("SAUSAGE");
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true); // 无边框

        // 设置软件图标
        try {
            File iconFile = new File("resources/icons/sausage.png");
            if (iconFile.exists()) {
                ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
                setIconImage(icon.getImage());
            }
        } catch (Exception ignored) {}

        // 根面板
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_MAIN);

        // ---- 自定义标题栏 ----
        root.add(buildTitleBar(), BorderLayout.NORTH);

        // ---- 主体：侧边栏 + 内容区 ----
        JPanel body = new JPanel(new BorderLayout());
        sidebar = new SidebarPanel();
        content = new ContentPanel(player);
        body.add(sidebar, BorderLayout.WEST);
        body.add(content, BorderLayout.CENTER);

        // ---- 正在播放展开页（覆盖主体区域）----
        nowPlaying = new NowPlayingPanel(player);
        nowPlaying.setOnClose(() -> {
            bodyCards.show(bodyCardHolder, "main");
            playerBar.setVisible(true);          // 恢复播放栏
            playerBar.getParent().revalidate();
        });

        bodyCardHolder = new JPanel(bodyCards);
        bodyCardHolder.add(body, "main");
        bodyCardHolder.add(nowPlaying, "nowplaying");
        root.add(bodyCardHolder, BorderLayout.CENTER);

        // ---- 底部播放栏 ----
        playerBar = new PlayerBar(player);
        root.add(playerBar, BorderLayout.SOUTH);

        setContentPane(root);

        // 导航切换
        sidebar.addNavListener(key -> content.showNav(key));

        // 播放结束自动下一首
        player.addListener(new MusicPlayer.PlayerListener() {
            @Override public void onPlay(Song s) { content.highlightCurrent(); }
            @Override public void onPause() {}
            @Override public void onResume() {}
            @Override public void onStop() {}
            @Override public void onProgress(long c, long t) {}
            @Override public void onSongEnd() {
                // playNext() 内部统一处理单曲循环与播放队列
                SwingUtilities.invokeLater(() -> player.playNext());
            }
        });

        // ESC 退出
        root.registerKeyboardAction(e -> System.exit(0),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 空格 播放/暂停
        root.registerKeyboardAction(e -> {
            if (player.getCurrentSong() != null) player.togglePause();
            else if (!player.getPlaylist().isEmpty()) player.play(0);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
           JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 点击播放栏左侧歌曲信息 → 展开正在播放页
        playerBar.setOnExpand(() -> {
            nowPlaying.syncState();
            bodyCards.show(bodyCardHolder, "nowplaying");
            playerBar.setVisible(false);         // 展开播放页时隐藏播放栏
            playerBar.getParent().revalidate();
        });

        // 自动加载内置音乐目录
        autoLoadMusic();
    }

    /** 自定义标题栏 - 深蓝科技风 */
    private JPanel buildTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(0xF5, 0xF5, 0xF5));
        titleBar.setPreferredSize(new Dimension(0, 38));

        // 左侧 Logo（仅图片）
        JPanel logoBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        logoBox.setOpaque(false);
        logoBox.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        try {
            File iconFile = new File("resources/icons/sausage.png");
            if (iconFile.exists()) {
                ImageIcon raw = new ImageIcon(iconFile.getAbsolutePath());
                Image scaled = raw.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
                logoBox.add(new JLabel(new ImageIcon(scaled)));
            }
        } catch (Exception ignored) {}
        // 图标后的名言
        JLabel quote = new JLabel("Music is a higher revelation than all wisdom and philosophy");
        quote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        quote.setForeground(Color.BLACK);
        logoBox.add(quote);
        titleBar.add(logoBox, BorderLayout.WEST);

        // 右侧窗口按钮（图片按钮）
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        btns.setOpaque(false);
        btns.add(createImgBtn("resources/icons/minimize-512x512.png", "最小化", e -> setState(Frame.ICONIFIED)));
        btns.add(createImgBtn("resources/icons/expand-512x512.png", "全屏", e -> toggleMaximize()));
        JButton closeBtn = createImgBtn("resources/icons/delete-512x512.png", "退出", e -> System.exit(0));
        btns.add(closeBtn);
        titleBar.add(btns, BorderLayout.EAST);

        // 拖拽移动窗口
        final Point[] dragOffset = {null};
        titleBar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getY() < 38 && SwingUtilities.isLeftMouseButton(e)) {
                    dragOffset[0] = new Point(e.getXOnScreen() - getLocation().x, e.getYOnScreen() - getLocation().y);
                }
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getX() < titleBar.getWidth() - 120) toggleMaximize();
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (dragOffset[0] != null) {
                    setLocation(e.getXOnScreen() - dragOffset[0].x, e.getYOnScreen() - dragOffset[0].y);
                }
            }
        });

        return titleBar;
    }

    /** 图片窗口按钮 */
    private JButton createImgBtn(String imgPath, String tip, ActionListener action) {
        JButton btn = new JButton();
        btn.setToolTipText(tip);
        try {
            File f = new File(imgPath);
            if (f.exists()) {
                ImageIcon raw = new ImageIcon(f.getAbsolutePath());
                Image normal = raw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
                btn.setIcon(new ImageIcon(normal));
                // hover 时稍微放大变亮
                Image bright = brighten(raw.getImage(), 60);
                Image hover = bright.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                btn.setRolloverIcon(new ImageIcon(hover));
            }
        } catch (Exception ignored) {}
        btn.setPreferredSize(new Dimension(38, 30));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0x1F, 0x1F, 0x1F)); btn.setOpaque(true); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(null); btn.setOpaque(false); }
        });
        return btn;
    }

    /** 图片提亮 */
    private Image brighten(Image img, int amount) {
        int w = img.getWidth(null), h = img.getHeight(null);
        if (w <= 0 || h <= 0) return img;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = bi.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                if (a == 0) continue;
                int r = Math.min(((argb >> 16) & 0xFF) + amount, 255);
                int gg = Math.min(((argb >> 8) & 0xFF) + amount, 255);
                int b = Math.min((argb & 0xFF) + amount, 255);
                bi.setRGB(x, y, (a << 24) | (r << 16) | (gg << 8) | b);
            }
        }
        return bi;
    }

    private boolean maximized = false;
    private Rectangle prevBounds;
    private void toggleMaximize() {
        if (maximized) {
            setBounds(prevBounds);
            maximized = false;
        } else {
            prevBounds = getBounds();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            setBounds(gd.getDefaultConfiguration().getBounds());
            maximized = true;
        }
    }

    public MusicPlayer getPlayer() { return player; }

    /** 自动加载内置音乐目录 resources/music */
    private void autoLoadMusic() {
        java.util.List<Song> songs = new java.util.ArrayList<>();
        scanMusicDir(resolveAppFile("resources/music"), songs);
        if (!songs.isEmpty()) {
            player.addSongs(songs);
            content.refreshLocalList();
        }
    }

    /** 扫描目录下的音频文件 */
    private void scanMusicDir(File dir, java.util.List<Song> out) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (f.isFile() && name.matches(".*\\.(wav|mp3|ogg|flac)")) {
                out.add(new Song(f.getAbsolutePath()));
            }
        }
    }

    /**
     * 解析应用相对路径：依次尝试
     * 1) 当前工作目录  2) class/jar 所在目录  3) class/jar 所在目录的父目录
     * 确保无论从哪里启动程序都能找到 music 等资源目录。
     */
    private File resolveAppFile(String relativePath) {
        // 1. 当前工作目录
        File f = new File(relativePath);
        if (f.exists()) return f;

        // 2. 基于 class/jar 实际所在位置解析
        try {
            File codeBase = new File(
                    MainFrame.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI());
            File codeDir = codeBase.isDirectory() ? codeBase : codeBase.getParentFile();
            if (codeDir != null) {
                File f2 = new File(codeDir, relativePath);
                if (f2.exists()) return f2;
                // 开发模式下 class 可能在 out/ 或 bin/ 子目录，向上找一级
                File parent = codeDir.getParentFile();
                if (parent != null) {
                    File f3 = new File(parent, relativePath);
                    if (f3.exists()) return f3;
                }
            }
        } catch (Exception ignored) {}

        // 3. 兜底：返回原始相对路径
        return f;
    }
}