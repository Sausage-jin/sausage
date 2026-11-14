package com.sausage.music;

import com.sausage.music.ui.MainFrame;

import javax.swing.*;

/**
 * SAUSAGE Music - 入口
 * 黑白高级风本地音乐播放器
 */
public class SausageMusic {
    public static void main(String[] args) {
        // 使用系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}