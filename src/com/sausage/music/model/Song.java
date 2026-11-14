package com.sausage.music.model;

import java.io.File;
import javax.sound.sampled.*;

/**
 * 歌曲数据模型
 */
public class Song {
    private String title;
    private String artist;
    private String album;
    private String filePath;
    private long duration; // 秒

    public Song(String filePath) {
        this.filePath = filePath;
        File f = new File(filePath);
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        // 尝试解析 "艺术家 - 标题" 格式
        int sep = name.indexOf(" - ");
        if (sep > 0) {
            this.artist = name.substring(0, sep).trim();
            this.title = name.substring(sep + 3).trim();
        } else {
            this.title = name.trim();
            this.artist = "未知艺术家";
        }
        this.album = "本地音乐";
        this.duration = getDurationFromFile(f);
    }

    private long getDurationFromFile(File f) {
        // 方式1：AudioFileFormat（MP3SPI 提供 duration 属性，单位微秒；WAV 有帧长度）
        try {
            AudioFileFormat aff = AudioSystem.getAudioFileFormat(f);
            // MP3SPI 的 duration 属性（微秒）
            Object dur = aff.properties().get("duration");
            if (dur instanceof Long) {
                long seconds = ((Long) dur) / 1_000_000;
                if (seconds > 0) return seconds;
            }
            // 帧长度 / 帧率
            long frames = aff.getFrameLength();
            AudioFormat fmt = aff.getFormat();
            if (frames > 0 && fmt.getFrameRate() > 0) {
                return (long) (frames / fmt.getFrameRate());
            }
        } catch (Exception ignored) {
        }
        // 方式2：AudioInputStream 帧长度（WAV 等原生格式）
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
            AudioFormat format = ais.getFormat();
            long frames = ais.getFrameLength();
            if (frames > 0 && format.getFrameRate() > 0) {
                return (long) (frames / format.getFrameRate());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getFilePath() { return filePath; }
    public long getDuration() { return duration; }

    public static String formatTime(long seconds) {
        if (seconds <= 0) return "00:00";
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    @Override
    public String toString() {
        return title + " - " + artist;
    }
}