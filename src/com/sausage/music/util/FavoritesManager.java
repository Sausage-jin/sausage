package com.sausage.music.util;

import com.sausage.music.model.Song;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 我喜欢 - 收藏管理 + 文件持久化
 * 存储于 ~/.sausage/favorites.txt，每行一个歌曲文件路径
 */
public class FavoritesManager {
    private static final String DIR_NAME = ".sausage";
    private static final String FILE_NAME = "favorites.txt";

    private final File storeFile;
    private final Set<String> favoritePaths = new LinkedHashSet<>();   // 保持插入顺序
    private final List<Song> favoriteSongs = new ArrayList<>();

    public FavoritesManager() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, DIR_NAME);
        if (!dir.exists()) dir.mkdirs();
        storeFile = new File(dir, FILE_NAME);
        load();
    }

    /** 从文件加载收藏列表 */
    private void load() {
        favoritePaths.clear();
        favoriteSongs.clear();
        if (!storeFile.exists()) return;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(storeFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                File f = new File(line);
                if (!f.exists()) continue;          // 文件已被移动/删除则跳过
                if (favoritePaths.add(line)) {
                    favoriteSongs.add(new Song(line));
                }
            }
        } catch (IOException e) {
            System.err.println("加载收藏列表失败: " + e.getMessage());
        }
    }

    /** 保存收藏列表到文件 */
    private void save() {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(storeFile), StandardCharsets.UTF_8))) {
            for (String p : favoritePaths) {
                bw.write(p);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("保存收藏列表失败: " + e.getMessage());
        }
    }

    /** 判断是否已收藏 */
    public boolean isFavorite(String filePath) {
        return favoritePaths.contains(filePath);
    }

    /**
     * 切换收藏状态
     * @return 切换后是否处于收藏状态（true=已收藏）
     */
    public boolean toggleFavorite(Song song) {
        String path = song.getFilePath();
        if (favoritePaths.contains(path)) {
            favoritePaths.remove(path);
            favoriteSongs.removeIf(s -> s.getFilePath().equals(path));
            save();
            return false;
        } else {
            favoritePaths.add(path);
            favoriteSongs.add(song);
            save();
            return true;
        }
    }

    /** 移除收藏，返回是否实际移除 */
    public boolean removeFavorite(String filePath) {
        if (favoritePaths.remove(filePath)) {
            favoriteSongs.removeIf(s -> s.getFilePath().equals(filePath));
            save();
            return true;
        }
        return false;
    }

    /** 获取收藏歌曲列表（按收藏顺序） */
    public List<Song> getFavorites() {
        return new ArrayList<>(favoriteSongs);
    }

    public int getCount() {
        return favoriteSongs.size();
    }
}