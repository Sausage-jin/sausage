package com.sausage.music.player;

import com.sausage.music.model.MV;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * MV 解析管理器 - 管理MV列表、解析来源（播放由 MVPlaybackPanel 内嵌 JavaFX 完成）
 */
public class MVPlayer {
    public interface MVListener {
        void onMVListChanged();
    }

    private final List<MV> mvList = new ArrayList<>();
    private final List<MVListener> listeners = new ArrayList<>();

    public void addListener(MVListener l) { listeners.add(l); }
    private void fireListChanged() { for (MVListener l : listeners) l.onMVListChanged(); }

    // ---------- 列表管理 ----------
    public List<MV> getList() { return mvList; }
    public int size() { return mvList.size(); }
    public boolean isEmpty() { return mvList.isEmpty(); }

    /** 导入本地视频文件 */
    public int importFiles(File[] files) {
        int added = 0;
        for (File f : files) {
            if (f.isFile() && MV.isVideoFile(f.getName())) {
                mvList.add(MV.fromFile(f));
                added++;
            }
        }
        if (added > 0) fireListChanged();
        return added;
    }

    /** 递归扫描文件夹中的视频文件 */
    public int importFolder(File dir) {
        int added = scanFolder(dir);
        if (added > 0) fireListChanged();
        return added;
    }

    private int scanFolder(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int added = 0;
        for (File f : files) {
            if (f.isDirectory()) {
                added += scanFolder(f);
            } else if (MV.isVideoFile(f.getName())) {
                mvList.add(MV.fromFile(f));
                added++;
            }
        }
        return added;
    }

    /** 解析 URL 并添加为 MV */
    public boolean addUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String trimmed = url.trim();
        if (!MV.looksLikeVideoUrl(trimmed)) return false;
        mvList.add(MV.fromUrl(trimmed));
        fireListChanged();
        return true;
    }

    /** 删除指定索引的 MV */
    public void remove(int index) {
        if (index < 0 || index >= mvList.size()) return;
        mvList.remove(index);
        fireListChanged();
    }

    public void clear() {
        mvList.clear();
        fireListChanged();
    }
}