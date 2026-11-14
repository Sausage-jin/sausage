package com.sausage.music.ui;

import com.sausage.music.model.MV;
import com.sausage.music.model.Song;
import com.sausage.music.player.MusicPlayer;
import com.sausage.music.player.MVPlayer;
import com.sausage.music.ui.theme.Theme;
import com.sausage.music.util.FavoritesManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 主内容区 - 发现/本地/喜欢/播放列表
 */
public class ContentPanel extends JPanel {
    private final MusicPlayer player;
    private final FavoritesManager favorites = new FavoritesManager();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHolder = new JPanel(cards);

    // 发现页
    private JPanel discoverPanel;
    // 本地音乐页
    private JPanel localPanel;
    private JPanel localSongList;
    private JLabel localEmpty;
    // 喜欢页
    private JPanel favoritePanel;
    private JPanel favoriteSongList;
    private JLabel favoriteEmpty;
    // 播放列表页
    private JPanel playlistPanel;
    private JPanel playlistSongList;
    private JLabel playlistEmpty;

    // MV页
    private final MVPlayer mvPlayer = new MVPlayer();
    private JPanel mvPanel;
    private final CardLayout mvCards = new CardLayout();
    private JPanel mvCardHolder;           // 列表↔播放 卡片容器
    private JPanel mvListPanel;            // 列表视图
    private MVPlaybackPanel mvPlaybackPanel; // 内嵌播放视图
    private JPanel mvGrid;
    private JScrollPane mvScroll;
    private JLabel mvEmpty;

    private int playingIndex = -1;

    public ContentPanel(MusicPlayer player) {
        this.player = player;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_MAIN);
        buildDiscoverPanel();
        buildLocalPanel();
        buildFavoritePanel();
        buildPlaylistPanel();
        buildMVPanel();
        cardHolder.add(discoverPanel, "discover");
        cardHolder.add(localPanel, "local");
        cardHolder.add(favoritePanel, "favorite");
        cardHolder.add(playlistPanel, "playlist");
        cardHolder.add(mvPanel, "mv");
        add(cardHolder, BorderLayout.CENTER);

        // MV 列表变化监听
        mvPlayer.addListener(() -> SwingUtilities.invokeLater(() -> refreshMVGrid()));
        // 播放队列变化监听
        player.addQueueListener(() -> SwingUtilities.invokeLater(() -> refreshQueueList()));
    }

    public void showNav(String key) {
        // 离开 MV 页时停止内嵌播放并返回列表视图
        if (!"mv".equals(key) && mvPlaybackPanel != null) {
            mvPlaybackPanel.stopAndRelease();
            mvCards.show(mvCardHolder, "list");
        }
        // 切换到喜欢/队列页时刷新列表
        if ("favorite".equals(key)) refreshFavoriteList();
        if ("playlist".equals(key)) refreshQueueList();
        cards.show(cardHolder, key);
    }

    // ==================== 发现页 ====================
    private void buildDiscoverPanel() {
        discoverPanel = new JPanel(new BorderLayout());
        discoverPanel.setBackground(Theme.BG_MAIN);
        discoverPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 20, 30));

        // 顶部横幅
        Theme.RoundedPanel banner = new Theme.RoundedPanel(16, new Color(0xF0, 0xF0, 0xF0));
        banner.setPreferredSize(new Dimension(0, 160));
        banner.setLayout(new BorderLayout());
        banner.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        JLabel bannerTitle = new JLabel("欢迎使用 SAUSAGE");
        bannerTitle.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 26));
        bannerTitle.setForeground(Color.BLACK);
        JPanel bannerText = new JPanel();
        bannerText.setOpaque(false);
        bannerText.setLayout(new BoxLayout(bannerText, BoxLayout.Y_AXIS));
        bannerText.add(bannerTitle);
        banner.add(bannerText, BorderLayout.CENTER);
        JLabel bannerIcon = new JLabel();
        try {
            File iconFile = new File("resources/icons/sausage.png");
            if (iconFile.exists()) {
                ImageIcon raw = new ImageIcon(iconFile.getAbsolutePath());
                Image scaled = raw.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                bannerIcon.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}
        banner.add(bannerIcon, BorderLayout.EAST);
        discoverPanel.add(banner, BorderLayout.NORTH);

        // 推荐歌单卡片
        JPanel cardsArea = new JPanel();
        cardsArea.setOpaque(false);
        cardsArea.setLayout(new BoxLayout(cardsArea, BoxLayout.Y_AXIS));
        JLabel secTitle = new JLabel("人气歌王");
        secTitle.setFont(Theme.TITLE_FONT);
        secTitle.setForeground(Theme.TEXT_DARK);
        secTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 12, 0));
        secTitle.setAlignmentX(LEFT_ALIGNMENT);
        cardsArea.add(secTitle);

        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        String[][] singers = {
            {"霜野雫", "resources/images/1.jpg"},
            {"月见凛", "resources/images/2.jpg"},
            {"黑川澪", "resources/images/3.jpg"},
            {"白夜萤", "resources/images/4.jpg"}
        };
        for (String[] p : singers) {
            Theme.RoundedPanel card = new Theme.RoundedPanel(12, new Color(0xE5, 0xE5, 0xE5));
            card.setPreferredSize(new Dimension(160, 210));
            card.setLayout(new BorderLayout());
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, 0, 10, 0)));
            // 预加载封面图片（ImageIO 同步读取，确保立即可用）
            Image tmpImg = null;
            try {
                tmpImg = javax.imageio.ImageIO.read(new File(p[1]));
            } catch (Exception ignored) {}
            final Image coverImg = tmpImg;
            // 封面 - 使用 images 图片，保持宽高比居中裁剪填充
            JPanel cover = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // 先画黑色底
                    g2.setColor(new Color(0x0F, 0x0F, 0x0F));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // 画图片：保持宽高比，居中裁剪填充（用裁剪式 drawImage 避免异步缩放问题）
                    if (coverImg != null && coverImg.getWidth(null) > 0) {
                        int cw = getWidth(), ch = getHeight();
                        int iw = coverImg.getWidth(null), ih = coverImg.getHeight(null);
                        // 计算源裁剪区域：按目标宽高比从原图居中裁剪
                        double targetRatio = (double) cw / ch;
                        double srcRatio = (double) iw / ih;
                        int sx, sy, sw, sh;
                        if (srcRatio > targetRatio) {
                            // 原图更宽，裁左右
                            sh = ih;
                            sw = (int) (ih * targetRatio);
                            sx = (iw - sw) / 2;
                            sy = 0;
                        } else {
                            // 原图更高，裁上下
                            sw = iw;
                            sh = (int) (iw / targetRatio);
                            sx = 0;
                            sy = (ih - sh) / 2;
                        }
                        g2.drawImage(coverImg, 0, 0, cw, ch, sx, sy, sx + sw, sy + sh, this);
                    }
                    g2.dispose();
                }
            };
            cover.setOpaque(true);
            cover.setPreferredSize(new Dimension(160, 150));
            card.add(cover, BorderLayout.CENTER);
            JLabel name = new JLabel(p[0]);
            name.setFont(Theme.SONG_SMALL);
            name.setForeground(Theme.TEXT_MID);
            name.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
            card.add(name, BorderLayout.SOUTH);
            grid.add(card);
        }
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        cardsArea.add(grid);

        // ---- 排行榜 ----
        cardsArea.add(createSectionTitle("排行榜"));
        cardsArea.add(buildRankingPanel());

        // ---- 大家都在听 ----
        cardsArea.add(createSectionTitle("大家都在听"));
        cardsArea.add(buildTrendingPanel());
        cardsArea.add(Box.createVerticalStrut(24));

        // 滚动容器 - 支持滚轮滑动
        JScrollPane discoverScroll = new JScrollPane(cardsArea);
        discoverScroll.setBorder(null);
        discoverScroll.setOpaque(false);
        discoverScroll.getViewport().setOpaque(false);
        discoverScroll.getVerticalScrollBar().setUnitIncrement(16);
        discoverScroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        discoverScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        discoverPanel.add(discoverScroll, BorderLayout.CENTER);
    }

    /** 发现页区域标题 */
    private JLabel createSectionTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(24, 0, 12, 0));
        title.setAlignmentX(LEFT_ALIGNMENT);
        return title;
    }

    /** 排行榜 - 热门歌曲排名列表 */
    private JPanel buildRankingPanel() {
        String[][] rankData = {
            {"心墙", "队长", "982.3万"},
            {"蓝色梦幻", "星野源", "756.1万"},
            {"欢乐颂", "贝多芬", "623.4万"},
            {"电子节拍", "Daft Punk", "412.8万"},
            {"小星星", "莫扎特", "356.2万"},
            {"上升音阶", "练习曲集", "198.5万"},
        };
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(0xE5, 0xE5, 0xE5));
        panel.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, rankData.length * 46));

        for (int i = 0; i < rankData.length; i++) {
            final String songTitle = rankData[i][0];
            JPanel row = new JPanel(new GridBagLayout());
            row.setBackground(new Color(0xE5, 0xE5, 0xE5));
            if (i < rankData.length - 1)
                row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xD0, 0xD0, 0xD0)));
            row.setCursor(new Cursor(Cursor.HAND_CURSOR));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            row.setPreferredSize(new Dimension(0, 46));
            row.setAlignmentX(LEFT_ALIGNMENT);

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(0, 16, 0, 16);

            boolean top3 = i < 3;
            JLabel rankLabel = new JLabel(String.valueOf(i + 1));
            rankLabel.setFont(top3 ? new Font("Consolas", Font.BOLD, 18) : Theme.TIME_FONT);
            rankLabel.setForeground(top3 ? Theme.ACCENT : Theme.TEXT_LIGHT);
            rankLabel.setPreferredSize(new Dimension(36, 22));
            gc.weightx = 0; gc.gridx = 0; row.add(rankLabel, gc);

            JLabel titleLabel = new JLabel(rankData[i][0]);
            titleLabel.setFont(top3 ? Theme.BOLD_FONT : Theme.SONG_FONT);
            titleLabel.setForeground(Theme.ACCENT);
            gc.weightx = 2.0; gc.gridx = 1; row.add(titleLabel, gc);

            JLabel artistLabel = new JLabel(rankData[i][1]);
            artistLabel.setFont(Theme.SONG_SMALL);
            artistLabel.setForeground(Theme.TEXT_MID);
            gc.weightx = 1.5; gc.gridx = 2; row.add(artistLabel, gc);

            JLabel hotLabel = new JLabel("▶ " + rankData[i][2], SwingConstants.RIGHT);
            hotLabel.setFont(Theme.TIME_FONT);
            hotLabel.setForeground(Theme.TEXT_LIGHT);
            gc.weightx = 0.8; gc.gridx = 3; row.add(hotLabel, gc);

            row.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { playSongByTitle(songTitle); }
                @Override public void mouseEntered(MouseEvent e) { row.setBackground(new Color(0xD8, 0xD8, 0xD8)); }
                @Override public void mouseExited(MouseEvent e) { row.setBackground(new Color(0xE5, 0xE5, 0xE5)); }
            });
            panel.add(row);
        }
        return panel;
    }

    /** 大家都在听 - 歌曲卡片网格 */
    private JPanel buildTrendingPanel() {
        String[][] trendData = {
            {"心墙", "队长", "resources/images/1.jpg"},
            {"蓝色梦幻", "星野源", "resources/images/2.jpg"},
            {"欢乐颂", "贝多芬", "resources/images/3.jpg"},
            {"电子节拍", "Daft Punk", "resources/images/4.jpg"},
        };
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));

        for (String[] p : trendData) {
            final String songTitle = p[0];
            Theme.RoundedPanel card = new Theme.RoundedPanel(12, new Color(0xE5, 0xE5, 0xE5));
            card.setPreferredSize(new Dimension(160, 210));
            card.setLayout(new BorderLayout());
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, 0, 10, 0)));
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));

            Image tmpImg = null;
            try { tmpImg = javax.imageio.ImageIO.read(new File(p[2])); } catch (Exception ignored) {}
            final Image coverImg = tmpImg;

            JPanel cover = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0x0F, 0x0F, 0x0F));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    if (coverImg != null && coverImg.getWidth(null) > 0) {
                        int cw = getWidth(), ch = getHeight();
                        int iw = coverImg.getWidth(null), ih = coverImg.getHeight(null);
                        double targetRatio = (double) cw / ch;
                        double srcRatio = (double) iw / ih;
                        int sx, sy, sw, sh;
                        if (srcRatio > targetRatio) { sh = ih; sw = (int)(ih * targetRatio); sx = (iw - sw)/2; sy = 0; }
                        else { sw = iw; sh = (int)(iw / targetRatio); sx = 0; sy = (ih - sh)/2; }
                        g2.drawImage(coverImg, 0, 0, cw, ch, sx, sy, sx+sw, sy+sh, this);
                    }
                    // 悬浮播放按钮
                    int cx = getWidth()/2, cy = getHeight()/2;
                    int r = Math.min(getWidth(), getHeight()) / 6;
                    g2.setColor(new Color(0x08, 0x08, 0x08, 170));
                    g2.fillOval(cx-r, cy-r, r*2, r*2);
                    g2.setColor(new Color(0xE5, 0xE5, 0xE5, 230));
                    int tx = cx - r/4;
                    int[] xs = {tx, tx+r, tx};
                    int[] ys = {cy-r/2, cy, cy+r/2};
                    g2.fillPolygon(xs, ys, 3);
                    g2.dispose();
                }
            };
            cover.setOpaque(true);
            cover.setPreferredSize(new Dimension(160, 150));
            card.add(cover, BorderLayout.CENTER);

            JPanel info = new JPanel();
            info.setOpaque(false);
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
            JLabel nameLabel = new JLabel(p[0]);
            nameLabel.setFont(Theme.SONG_SMALL);
            nameLabel.setForeground(Theme.TEXT_MID);
            nameLabel.setAlignmentX(LEFT_ALIGNMENT);
            info.add(nameLabel);
            JLabel artistLabel = new JLabel(p[1]);
            artistLabel.setFont(Theme.SONG_SMALL);
            artistLabel.setForeground(Theme.TEXT_LIGHT);
            artistLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            artistLabel.setAlignmentX(LEFT_ALIGNMENT);
            info.add(artistLabel);
            card.add(info, BorderLayout.SOUTH);

            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { playSongByTitle(songTitle); }
                @Override public void mouseEntered(MouseEvent e) { card.setBg(new Color(0xDC, 0xDC, 0xDC)); }
                @Override public void mouseExited(MouseEvent e) { card.setBg(new Color(0xE5, 0xE5, 0xE5)); }
            });
            grid.add(card);
        }
        return grid;
    }

    /** 按标题或艺术家匹配播放列表中的歌曲 */
    private void playSongByTitle(String name) {
        List<Song> songs = player.getPlaylist();
        for (int i = 0; i < songs.size(); i++) {
            Song s = songs.get(i);
            if (s.getTitle().equals(name) || s.getArtist().equals(name)) {
                playingIndex = i;
                player.play(i);
                refreshLocalList();
                return;
            }
        }
    }

    // ==================== 本地音乐页 ====================
    private void buildLocalPanel() {
        localPanel = new JPanel(new BorderLayout());
        localPanel.setBackground(Theme.BG_MAIN);
        localPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 顶部工具栏
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JLabel title = new JLabel("本地音乐");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT_DARK);
        toolbar.add(title, BorderLayout.WEST);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);
        Theme.RoundedButton importBtn = new Theme.RoundedButton("＋ 导入歌曲", 20, Theme.PRIMARY, Theme.TEXT_WHITE);
        importBtn.setPreferredSize(new Dimension(120, 34));
        importBtn.addActionListener(e -> importSongs());
        Theme.RoundedButton importDirBtn = new Theme.RoundedButton("📂 导入文件夹", 20, new Color(0xD5, 0xD5, 0xD5), Theme.PRIMARY_LIGHT);
        importDirBtn.setPreferredSize(new Dimension(130, 34));
        importDirBtn.addActionListener(e -> importFolder());
        rightTools.add(importDirBtn);
        rightTools.add(importBtn);
        toolbar.add(rightTools, BorderLayout.EAST);
        localPanel.add(toolbar, BorderLayout.NORTH);

        // 列表头
        JPanel header = createListHeader();
        // 空提示
        localEmpty = new JLabel("还没有歌曲，点击右上角「导入歌曲」添加本地 MP3/WAV 文件", SwingConstants.CENTER);
        localEmpty.setFont(Theme.SONG_FONT);
        localEmpty.setForeground(Theme.TEXT_LIGHT);
        // 歌曲列表（滚动）
        localSongList = new JPanel();
        localSongList.setLayout(new BoxLayout(localSongList, BoxLayout.Y_AXIS));
        localSongList.setBackground(new Color(0xE5, 0xE5, 0xE5));
        JScrollPane scroll = new JScrollPane(localSongList);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        center.add(header, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        localPanel.add(center, BorderLayout.CENTER);
    }

    private JPanel createListHeader() {
        JPanel header = new JPanel(new GridLayout(1, 1));
        header.setBackground(new Color(0xDD, 0xDD, 0xDD));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(new Color(0xDD, 0xDD, 0xDD));
        row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0; gc.gridx = 0; row.add(boldLabel("#"), gc);
        gc.weightx = 2.2; gc.gridx = 1; row.add(boldLabel("歌曲标题"), gc);
        gc.weightx = 1.5; gc.gridx = 2; row.add(boldLabel("艺术家"), gc);
        gc.weightx = 1.2; gc.gridx = 3; row.add(boldLabel("专辑"), gc);
        gc.weightx = 0.5; gc.gridx = 4; row.add(boldLabel("时长"), gc);
        header.add(row);
        header.setPreferredSize(new Dimension(0, 36));
        return header;
    }

    private JLabel boldLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Theme.SONG_SMALL);
        l.setForeground(Theme.TEXT_LIGHT);
        return l;
    }

    // ==================== 喜欢页 ====================
    private void buildFavoritePanel() {
        favoritePanel = new JPanel(new BorderLayout());
        favoritePanel.setBackground(Theme.BG_MAIN);
        favoritePanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 顶部工具栏
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JLabel title = new JLabel("我喜欢的音乐");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT_DARK);
        toolbar.add(title, BorderLayout.WEST);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);
        Theme.RoundedButton playAllBtn = new Theme.RoundedButton("▶ 播放全部", 20, Theme.PRIMARY, Theme.TEXT_WHITE);
        playAllBtn.setPreferredSize(new Dimension(120, 34));
        playAllBtn.addActionListener(e -> playAllFavorites());
        rightTools.add(playAllBtn);
        toolbar.add(rightTools, BorderLayout.EAST);
        favoritePanel.add(toolbar, BorderLayout.NORTH);

        // 列表头 + 滚动列表
        JPanel header = createListHeader();
        favoriteEmpty = new JLabel("还没有喜欢的歌曲，在歌曲列表点击 ♥ 收藏", SwingConstants.CENTER);
        favoriteEmpty.setFont(Theme.SONG_FONT);
        favoriteEmpty.setForeground(Theme.TEXT_LIGHT);
        favoriteSongList = new JPanel();
        favoriteSongList.setLayout(new BoxLayout(favoriteSongList, BoxLayout.Y_AXIS));
        favoriteSongList.setBackground(new Color(0xE5, 0xE5, 0xE5));
        JScrollPane scroll = new JScrollPane(favoriteSongList);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        center.add(header, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        favoritePanel.add(center, BorderLayout.CENTER);
    }

    // ==================== 播放列表页 ====================
    private void buildPlaylistPanel() {
        playlistPanel = new JPanel(new BorderLayout());
        playlistPanel.setBackground(Theme.BG_MAIN);
        playlistPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 顶部工具栏
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JLabel title = new JLabel("当前播放列表");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT_DARK);
        toolbar.add(title, BorderLayout.WEST);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);
        Theme.RoundedButton clearBtn = new Theme.RoundedButton("✕ 清空队列", 20, new Color(0xD5, 0xD5, 0xD5), Theme.PRIMARY_LIGHT);
        clearBtn.setPreferredSize(new Dimension(120, 34));
        clearBtn.addActionListener(e -> player.clearQueue());
        rightTools.add(clearBtn);
        toolbar.add(rightTools, BorderLayout.EAST);
        playlistPanel.add(toolbar, BorderLayout.NORTH);

        // 列表头 + 滚动列表
        JPanel header = createListHeader();
        playlistEmpty = new JLabel("播放队列为空，在歌曲列表点击 ＋ 添加到队列", SwingConstants.CENTER);
        playlistEmpty.setFont(Theme.SONG_FONT);
        playlistEmpty.setForeground(Theme.TEXT_LIGHT);
        playlistSongList = new JPanel();
        playlistSongList.setLayout(new BoxLayout(playlistSongList, BoxLayout.Y_AXIS));
        playlistSongList.setBackground(new Color(0xE5, 0xE5, 0xE5));
        JScrollPane scroll = new JScrollPane(playlistSongList);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        center.add(header, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        playlistPanel.add(center, BorderLayout.CENTER);
    }

    // ==================== 导入歌曲 ====================
    private void importSongs() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().matches(".*\\.(mp3|wav|ogg|flac)"); }
            public String getDescription() { return "音频文件 (MP3/WAV/OGG/FLAC)"; }
        });
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            List<Song> songs = new ArrayList<>();
            for (File f : fc.getSelectedFiles()) songs.add(new Song(f.getAbsolutePath()));
            player.addSongs(songs);
            refreshLocalList();
        }
    }

    private void importFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile();
            List<Song> songs = new ArrayList<>();
            scanFolder(dir, songs);
            player.addSongs(songs);
            refreshLocalList();
        }
    }

    private void scanFolder(File dir, List<Song> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanFolder(f, out);
            else if (f.getName().toLowerCase().matches(".*\\.(mp3|wav|ogg|flac)")) out.add(new Song(f.getAbsolutePath()));
        }
    }

    // ==================== 刷新列表 ====================
    public void refreshLocalList() {
        localSongList.removeAll();
        List<Song> songs = player.getPlaylist();
        if (songs.isEmpty()) {
            localSongList.add(localEmpty);
        } else {
            for (int i = 0; i < songs.size(); i++) {
                localSongList.add(createSongRow(songs.get(i), i));
            }
        }
        localSongList.revalidate();
        localSongList.repaint();
    }

    private JPanel createSongRow(Song song, int index) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(new Color(0xE5, 0xE5, 0xE5));
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setPreferredSize(new Dimension(0, 48));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(7, 12, 7, 12);

        boolean isPlaying = (index == player.getCurrentIndex() && player.isPlaying() || index == playingIndex);
        Color fg = isPlaying ? Theme.PRIMARY : Theme.TEXT_DARK;

        JLabel numLabel = new JLabel(String.valueOf(index + 1));
        numLabel.setFont(Theme.TIME_FONT);
        numLabel.setForeground(isPlaying ? Theme.PRIMARY : Theme.TEXT_LIGHT);
        numLabel.setPreferredSize(new Dimension(30, 20));
        gc.weightx = 0; gc.gridx = 0; row.add(numLabel, gc);

        JLabel titleLabel = new JLabel(song.getTitle());
        titleLabel.setFont(Theme.SONG_FONT);
        titleLabel.setForeground(fg);
        gc.weightx = 2.2; gc.gridx = 1; row.add(titleLabel, gc);

        JLabel artistLabel = new JLabel(song.getArtist());
        artistLabel.setFont(Theme.SONG_SMALL);
        artistLabel.setForeground(Theme.TEXT_MID);
        gc.weightx = 1.5; gc.gridx = 2; row.add(artistLabel, gc);

        JLabel albumLabel = new JLabel(song.getAlbum());
        albumLabel.setFont(Theme.SONG_SMALL);
        albumLabel.setForeground(Theme.TEXT_LIGHT);
        gc.weightx = 1.2; gc.gridx = 3; row.add(albumLabel, gc);

        JLabel durLabel = new JLabel(Song.formatTime(song.getDuration()));
        durLabel.setFont(Theme.TIME_FONT);
        durLabel.setForeground(Theme.TEXT_LIGHT);
        gc.weightx = 0.5; gc.gridx = 4; row.add(durLabel, gc);

        // 操作按钮区：♥ 喜欢 / ＋ 加入队列
        JPanel ops = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        ops.setOpaque(false);
        ops.setPreferredSize(new Dimension(96, 24));
        ops.add(createFavoriteButton(song));
        ops.add(createQueueButton(song));
        gc.weightx = 0; gc.gridx = 5; row.add(ops, gc);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    playingIndex = index;
                    player.play(index);
                    refreshLocalList();
                }
            }
            @Override public void mouseEntered(MouseEvent e) { if (!isPlaying) row.setBackground(Theme.BG_HOVER); }
            @Override public void mouseExited(MouseEvent e) {
                if (!isPlaying && !row.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), row)))
                    row.setBackground(new Color(0xE5, 0xE5, 0xE5));
            }
        });
        return row;
    }

    /** ♥ 喜欢/取消喜欢 按钮 */
    private JButton createFavoriteButton(Song song) {
        JButton btn = new JButton("♥");
        btn.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 15));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("我喜欢");
        btn.setForeground(favorites.isFavorite(song.getFilePath()) ? Theme.TEXT_DARK : Theme.TEXT_LIGHT);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setForeground(Theme.TEXT_DARK); }
            @Override public void mouseExited(MouseEvent e) {
                btn.setForeground(favorites.isFavorite(song.getFilePath()) ? Theme.TEXT_DARK : Theme.TEXT_LIGHT);
            }
        });
        btn.addActionListener(e -> {
            favorites.toggleFavorite(song);
            btn.setForeground(favorites.isFavorite(song.getFilePath()) ? Theme.TEXT_DARK : Theme.TEXT_LIGHT);
            refreshFavoriteList();
        });
        return btn;
    }

    /** ＋ 添加到队列 按钮 */
    private JButton createQueueButton(Song song) {
        JButton btn = new JButton("＋");
        btn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("添加到队列");
        btn.setForeground(Theme.TEXT_LIGHT);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setForeground(Theme.TEXT_DARK); }
            @Override public void mouseExited(MouseEvent e) { btn.setForeground(Theme.TEXT_LIGHT); }
        });
        btn.addActionListener(e -> player.addToQueue(song));
        return btn;
    }

    public void highlightCurrent() {
        playingIndex = player.getCurrentIndex();
        refreshLocalList();
    }

    // ==================== 我喜欢 / 播放队列 ====================
    public void refreshFavoriteList() {
        favoriteSongList.removeAll();
        List<Song> favs = favorites.getFavorites();
        if (favs.isEmpty()) {
            favoriteSongList.add(favoriteEmpty);
        } else {
            for (int i = 0; i < favs.size(); i++) {
                favoriteSongList.add(createFavoriteRow(favs.get(i), i));
            }
        }
        favoriteSongList.revalidate();
        favoriteSongList.repaint();
    }

    public void refreshQueueList() {
        playlistSongList.removeAll();
        List<Song> queue = player.getQueue();
        if (queue.isEmpty()) {
            playlistSongList.add(playlistEmpty);
        } else {
            for (int i = 0; i < queue.size(); i++) {
                playlistSongList.add(createQueueRow(queue.get(i), i));
            }
        }
        playlistSongList.revalidate();
        playlistSongList.repaint();
    }

    /** 播放一首歌：优先在本地列表中定位索引，找不到则直接播放 */
    private void playSongFromList(Song song) {
        List<Song> songs = player.getPlaylist();
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getFilePath().equals(song.getFilePath())) {
                playingIndex = i;
                player.play(i);
                refreshLocalList();
                return;
            }
        }
        player.playDirect(song);
    }

    /** 播放全部收藏：第一首立即播放，其余加入队列 */
    private void playAllFavorites() {
        List<Song> favs = favorites.getFavorites();
        if (favs.isEmpty()) return;
        playSongFromList(favs.get(0));
        for (int i = 1; i < favs.size(); i++) {
            player.addToQueue(favs.get(i));
        }
    }

    /** 收藏页歌曲行 */
    private JPanel createFavoriteRow(Song song, int index) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(new Color(0xE5, 0xE5, 0xE5));
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setPreferredSize(new Dimension(0, 48));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(7, 12, 7, 12);

        Song cur = player.getCurrentSong();
        boolean isCurrent = cur != null && song.getFilePath().equals(cur.getFilePath());
        Color fg = (isCurrent && player.isPlaying()) ? Theme.PRIMARY : Theme.TEXT_DARK;

        JLabel numLabel = new JLabel(String.valueOf(index + 1));
        numLabel.setFont(Theme.TIME_FONT);
        numLabel.setForeground(isCurrent ? Theme.PRIMARY : Theme.TEXT_LIGHT);
        numLabel.setPreferredSize(new Dimension(30, 20));
        gc.weightx = 0; gc.gridx = 0; row.add(numLabel, gc);

        JLabel titleLabel = new JLabel(song.getTitle());
        titleLabel.setFont(Theme.SONG_FONT);
        titleLabel.setForeground(fg);
        gc.weightx = 2.2; gc.gridx = 1; row.add(titleLabel, gc);

        JLabel artistLabel = new JLabel(song.getArtist());
        artistLabel.setFont(Theme.SONG_SMALL);
        artistLabel.setForeground(Theme.TEXT_MID);
        gc.weightx = 1.5; gc.gridx = 2; row.add(artistLabel, gc);

        JLabel albumLabel = new JLabel(song.getAlbum());
        albumLabel.setFont(Theme.SONG_SMALL);
        albumLabel.setForeground(Theme.TEXT_LIGHT);
        gc.weightx = 1.2; gc.gridx = 3; row.add(albumLabel, gc);

        JLabel durLabel = new JLabel(Song.formatTime(song.getDuration()));
        durLabel.setFont(Theme.TIME_FONT);
        durLabel.setForeground(Theme.TEXT_LIGHT);
        gc.weightx = 0.5; gc.gridx = 4; row.add(durLabel, gc);

        // 操作按钮区：♥ 取消喜欢 / ＋ 加入队列
        JPanel ops = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        ops.setOpaque(false);
        ops.setPreferredSize(new Dimension(96, 24));
        JButton unfavBtn = new JButton("♥");
        unfavBtn.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 15));
        unfavBtn.setBorderPainted(false);
        unfavBtn.setContentAreaFilled(false);
        unfavBtn.setFocusPainted(false);
        unfavBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        unfavBtn.setToolTipText("取消喜欢");
        unfavBtn.setForeground(Theme.TEXT_DARK);
        unfavBtn.addActionListener(e -> { favorites.removeFavorite(song.getFilePath()); refreshFavoriteList(); });
        ops.add(unfavBtn);
        ops.add(createQueueButton(song));
        gc.weightx = 0; gc.gridx = 5; row.add(ops, gc);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { playSongFromList(song); }
            @Override public void mouseEntered(MouseEvent e) { if (!isCurrent) row.setBackground(Theme.BG_HOVER); }
            @Override public void mouseExited(MouseEvent e) {
                if (!isCurrent && !row.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), row)))
                    row.setBackground(new Color(0xE5, 0xE5, 0xE5));
            }
        });

        // 右键菜单
        JPopupMenu popup = new JPopupMenu();
        JMenuItem playItem = new JMenuItem("播放");
        playItem.addActionListener(e -> playSongFromList(song));
        JMenuItem queueItem = new JMenuItem("添加到队列");
        queueItem.addActionListener(e -> player.addToQueue(song));
        JMenuItem unfavItem = new JMenuItem("取消喜欢");
        unfavItem.addActionListener(e -> { favorites.removeFavorite(song.getFilePath()); refreshFavoriteList(); });
        popup.add(playItem);
        popup.add(queueItem);
        popup.addSeparator();
        popup.add(unfavItem);
        row.setComponentPopupMenu(popup);

        return row;
    }

    /** 队列页歌曲行 */
    private JPanel createQueueRow(Song song, int index) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(new Color(0xE5, 0xE5, 0xE5));
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setPreferredSize(new Dimension(0, 48));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(7, 12, 7, 12);

        Song cur = player.getCurrentSong();
        boolean isCurrent = cur != null && song.getFilePath().equals(cur.getFilePath());
        Color fg = (isCurrent && player.isPlaying()) ? Theme.PRIMARY : Theme.TEXT_DARK;

        JLabel numLabel = new JLabel(String.valueOf(index + 1));
        numLabel.setFont(Theme.TIME_FONT);
        numLabel.setForeground(Theme.TEXT_LIGHT);
        numLabel.setPreferredSize(new Dimension(30, 20));
        gc.weightx = 0; gc.gridx = 0; row.add(numLabel, gc);

        JLabel titleLabel = new JLabel(song.getTitle());
        titleLabel.setFont(Theme.SONG_FONT);
        titleLabel.setForeground(fg);
        gc.weightx = 2.2; gc.gridx = 1; row.add(titleLabel, gc);

        JLabel artistLabel = new JLabel(song.getArtist());
        artistLabel.setFont(Theme.SONG_SMALL);
        artistLabel.setForeground(Theme.TEXT_MID);
        gc.weightx = 1.5; gc.gridx = 2; row.add(artistLabel, gc);

        JLabel albumLabel = new JLabel(song.getAlbum());
        albumLabel.setFont(Theme.SONG_SMALL);
        albumLabel.setForeground(Theme.TEXT_LIGHT);
        gc.weightx = 1.2; gc.gridx = 3; row.add(albumLabel, gc);

        JLabel durLabel = new JLabel(Song.formatTime(song.getDuration()));
        durLabel.setFont(Theme.TIME_FONT);
        durLabel.setForeground(Theme.TEXT_LIGHT);
        gc.weightx = 0.5; gc.gridx = 4; row.add(durLabel, gc);

        // 操作按钮区：✕ 从队列移除
        JPanel ops = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        ops.setOpaque(false);
        ops.setPreferredSize(new Dimension(96, 24));
        JButton delBtn = new JButton("✕");
        delBtn.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        delBtn.setBorderPainted(false);
        delBtn.setContentAreaFilled(false);
        delBtn.setFocusPainted(false);
        delBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        delBtn.setToolTipText("从队列移除");
        delBtn.setForeground(Theme.TEXT_LIGHT);
        delBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { delBtn.setForeground(Theme.TEXT_DARK); }
            @Override public void mouseExited(MouseEvent e) { delBtn.setForeground(Theme.TEXT_LIGHT); }
        });
        delBtn.addActionListener(e -> player.removeFromQueue(index));
        ops.add(delBtn);
        gc.weightx = 0; gc.gridx = 5; row.add(ops, gc);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                player.playDirect(song);
                player.removeFromQueue(index);
            }
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(Theme.BG_HOVER); }
            @Override public void mouseExited(MouseEvent e) {
                if (!row.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), row)))
                    row.setBackground(new Color(0xE5, 0xE5, 0xE5));
            }
        });

        // 右键菜单
        JPopupMenu popup = new JPopupMenu();
        JMenuItem playItem = new JMenuItem("立即播放");
        playItem.addActionListener(e -> { player.playDirect(song); player.removeFromQueue(index); });
        JMenuItem removeItem = new JMenuItem("从队列移除");
        removeItem.addActionListener(e -> player.removeFromQueue(index));
        popup.add(playItem);
        popup.addSeparator();
        popup.add(removeItem);
        row.setComponentPopupMenu(popup);

        return row;
    }

    public MVPlayer getMVPlayer() { return mvPlayer; }

    // ==================== MV页 ====================
    private void buildMVPanel() {
        mvPanel = new JPanel(new BorderLayout());
        mvPanel.setBackground(Theme.BG_MAIN);
        mvPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 顶部工具栏
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JLabel title = new JLabel("MV");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT_DARK);
        toolbar.add(title, BorderLayout.WEST);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);
        Theme.RoundedButton addUrlBtn = new Theme.RoundedButton("🔗 添加链接", 20, new Color(0x2A, 0x2A, 0x2A), Theme.TEXT_DARK);
        addUrlBtn.setPreferredSize(new Dimension(120, 34));
        addUrlBtn.addActionListener(e -> addMVFromUrl());
        Theme.RoundedButton importDirBtn = new Theme.RoundedButton("📂 导入文件夹", 20, new Color(0x2A, 0x2A, 0x2A), Theme.TEXT_DARK);
        importDirBtn.setPreferredSize(new Dimension(130, 34));
        importDirBtn.addActionListener(e -> importMVFolder());
        Theme.RoundedButton importBtn = new Theme.RoundedButton("＋ 导入MV", 20, Theme.PRIMARY, Theme.TEXT_WHITE);
        importBtn.setPreferredSize(new Dimension(120, 34));
        importBtn.addActionListener(e -> importMVFiles());
        rightTools.add(addUrlBtn);
        rightTools.add(importDirBtn);
        rightTools.add(importBtn);
        toolbar.add(rightTools, BorderLayout.EAST);
        mvPanel.add(toolbar, BorderLayout.NORTH);

        // ---- 列表视图 ----
        mvListPanel = new JPanel(new BorderLayout());
        mvListPanel.setOpaque(false);
        mvListPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // 空提示
        mvEmpty = new JLabel("还没有MV，点击右上角「导入MV」添加本地视频，或「添加链接」解析网络MV", SwingConstants.CENTER);
        mvEmpty.setFont(Theme.SONG_FONT);
        mvEmpty.setForeground(Theme.TEXT_LIGHT);
        mvEmpty.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));

        // MV 网格（3列）- 外层包 BorderLayout(NORTH) 防止视口拉伸网格导致卡片高度随数量变化
        mvGrid = new JPanel(new GridLayout(0, 3, 16, 16));
        mvGrid.setOpaque(false);
        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setOpaque(false);
        gridWrapper.add(mvGrid, BorderLayout.NORTH);
        mvScroll = new JScrollPane(gridWrapper);
        mvScroll.setBorder(null);
        mvScroll.setOpaque(false);
        mvScroll.getViewport().setOpaque(false);
        mvScroll.getVerticalScrollBar().setUnitIncrement(16);
        mvScroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        mvListPanel.add(mvEmpty, BorderLayout.NORTH);
        mvListPanel.add(mvScroll, BorderLayout.CENTER);

        // ---- 播放视图（JavaFX 内嵌） ----
        mvPlaybackPanel = new MVPlaybackPanel();
        mvPlaybackPanel.setOnBack(() -> {
            mvPlaybackPanel.stopAndRelease();
            mvCards.show(mvCardHolder, "list");
        });

        // ---- 卡片容器：列表 ↔ 播放 ----
        mvCardHolder = new JPanel(mvCards);
        mvCardHolder.setOpaque(false);
        mvCardHolder.add(mvListPanel, "list");
        mvCardHolder.add(mvPlaybackPanel, "play");
        mvPanel.add(mvCardHolder, BorderLayout.CENTER);

        refreshMVGrid();
    }

    /** 切换到内嵌播放视图并加载 MV */
    private void playMV(MV mv) {
        mvCards.show(mvCardHolder, "play");
        mvPlaybackPanel.loadMV(mv);
    }

    /** 刷新 MV 网格 */
    private void refreshMVGrid() {
        mvGrid.removeAll();
        List<MV> list = mvPlayer.getList();
        boolean empty = list.isEmpty();
        mvEmpty.setVisible(empty);
        mvScroll.setVisible(!empty);
        if (!empty) {
            for (int i = 0; i < list.size(); i++) {
                mvGrid.add(createMVCard(list.get(i), i));
            }
        }
        mvGrid.revalidate();
        mvGrid.repaint();
    }

    /** 创建 MV 卡片 */
    private JPanel createMVCard(MV mv, int index) {
        Theme.RoundedPanel card = new Theme.RoundedPanel(12, new Color(0x14, 0x14, 0x14));
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // 固定卡片高度，保证添加/删除 MV 时尺寸不变（宽度由网格列宽决定）
        card.setPreferredSize(new Dimension(0, 200));

        // 封面区 - 播放按钮占位封面
        JPanel cover = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // 深色底
                g2.setColor(new Color(0x0D, 0x0D, 0x0D));
                g2.fillRect(0, 0, w, h);
                // 装饰竖线纹理
                g2.setColor(new Color(0x16, 0x16, 0x16));
                for (int x = 10; x < w; x += 22) g2.drawLine(x, 0, x, h);
                // 中央播放圆
                int cx = w / 2, cy = h / 2;
                int r = Math.min(w, h) / 5;
                g2.setColor(new Color(0xE5, 0xE5, 0xE5, 210));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                // 播放三角
                int tx = cx - r / 4;
                int[] xs = {tx, tx + r, tx};
                int[] ys = {cy - r / 2, cy, cy + r / 2};
                g2.setColor(new Color(0x08, 0x08, 0x08));
                g2.fillPolygon(xs, ys, 3);
                g2.dispose();
            }
        };
        cover.setPreferredSize(new Dimension(0, 130));
        cover.setOpaque(false);
        card.add(cover, BorderLayout.CENTER);

        // 信息区
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        JLabel titleLabel = new JLabel(mv.getTitle());
        titleLabel.setFont(Theme.SONG_FONT);
        titleLabel.setForeground(Theme.TEXT_DARK);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        info.add(titleLabel);
        JLabel metaLabel = new JLabel(mv.getSourceText() + " · " + mv.getSizeText());
        metaLabel.setFont(Theme.SONG_SMALL);
        metaLabel.setForeground(Theme.TEXT_LIGHT);
        metaLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        metaLabel.setAlignmentX(LEFT_ALIGNMENT);
        info.add(metaLabel);
        card.add(info, BorderLayout.SOUTH);

        // 点击播放 / hover 高亮
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { playMV(mv); }
            @Override public void mouseEntered(MouseEvent e) { card.setBg(new Color(0x1B, 0x1B, 0x1B)); }
            @Override public void mouseExited(MouseEvent e) { card.setBg(new Color(0x14, 0x14, 0x14)); }
        });

        // 右键菜单
        JPopupMenu popup = new JPopupMenu();
        JMenuItem playItem = new JMenuItem("播放 MV");
        playItem.addActionListener(e -> playMV(mv));
        JMenuItem delItem = new JMenuItem("从列表删除");
        delItem.addActionListener(e -> mvPlayer.remove(index));
        popup.add(playItem);
        popup.addSeparator();
        popup.add(delItem);
        card.setComponentPopupMenu(popup);

        return card;
    }

    /** 导入本地 MV 文件 */
    private void importMVFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) { return f.isDirectory() || MV.isVideoFile(f.getName()); }
            public String getDescription() { return "视频文件 (MP4/AVI/MKV/MOV/FLV/WMV/WebM)"; }
        });
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            int added = mvPlayer.importFiles(fc.getSelectedFiles());
            if (added == 0) {
                JOptionPane.showMessageDialog(this, "未找到支持的视频文件", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /** 导入文件夹中的 MV */
    private void importMVFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            int added = mvPlayer.importFolder(fc.getSelectedFile());
            if (added == 0) {
                JOptionPane.showMessageDialog(this, "该文件夹中未找到视频文件", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /** 解析网络 MV 链接 */
    private void addMVFromUrl() {
        String url = JOptionPane.showInputDialog(this,
            "输入 MV 视频链接：\n（支持 http/https/rtmp/rtsp 直链）",
            "添加MV链接", JOptionPane.PLAIN_MESSAGE);
        if (url == null) return;
        if (!mvPlayer.addUrl(url)) {
            JOptionPane.showMessageDialog(this,
                "链接格式无效，请输入以 http:// 、https:// 、rtmp:// 、rtsp:// 开头的链接",
                "解析失败", JOptionPane.ERROR_MESSAGE);
        }
    }
}