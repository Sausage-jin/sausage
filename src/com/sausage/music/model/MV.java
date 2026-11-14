package com.sausage.music.model;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * MV 数据模型 - 支持本地视频文件和网络URL
 */
public class MV {
    private String title;
    private String source;      // 文件路径 或 URL
    private boolean isUrl;
    private String format;      // mp4 / avi / mkv ...
    private long fileSize;      // 字节（URL 为 -1）

    public MV(String title, String source, boolean isUrl, String format, long fileSize) {
        this.title = title;
        this.source = source;
        this.isUrl = isUrl;
        this.format = format;
        this.fileSize = fileSize;
    }

    /** 从本地视频文件构建 */
    public static MV fromFile(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String fmt = dot > 0 ? name.substring(dot + 1).toLowerCase() : "video";
        String baseName = dot > 0 ? name.substring(0, dot) : name;
        // 尝试解析 "艺术家 - 标题" 格式
        String title;
        int sep = baseName.indexOf(" - ");
        if (sep > 0) {
            title = baseName.substring(sep + 3).trim();
        } else {
            title = baseName.trim();
        }
        return new MV(title, file.getAbsolutePath(), false, fmt, file.length());
    }

    /** 解析网络 MV URL，提取标题和格式 */
    public static MV fromUrl(String url) {
        String cleaned = url.trim();
        // 去掉查询参数
        String pathPart = cleaned;
        int q = pathPart.indexOf('?');
        if (q > 0) pathPart = pathPart.substring(0, q);
        // 提取文件名
        String fileName = pathPart;
        int slash = Math.max(pathPart.lastIndexOf('/'), pathPart.lastIndexOf('\\'));
        if (slash >= 0 && slash < pathPart.length() - 1) {
            fileName = pathPart.substring(slash + 1);
        }
        try {
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        int dot = fileName.lastIndexOf('.');
        String fmt = dot > 0 ? fileName.substring(dot + 1).toLowerCase() : "stream";
        String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
        if (baseName.isEmpty()) baseName = "网络MV";
        return new MV(baseName, cleaned, true, fmt, -1);
    }

    /** 判断是否为支持的视频格式 */
    public static boolean isVideoFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.matches(".*\\.(mp4|avi|mkv|mov|flv|wmv|webm|m4v|ts|mpg|mpeg|3gp)");
    }

    /** 判断URL是否看起来像视频链接 */
    public static boolean looksLikeVideoUrl(String url) {
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://")
            || lower.startsWith("rtmp://") || lower.startsWith("rtsp://")
            || lower.startsWith("mms://");
    }

    public String getTitle() { return title; }
    public String getSource() { return source; }
    public boolean isUrl() { return isUrl; }
    public String getFormat() { return format; }
    public long getFileSize() { return fileSize; }

    /** 友好的大小显示 */
    public String getSizeText() {
        if (fileSize < 0) return "网络流";
        if (fileSize < 1024) return fileSize + " B";
        double kb = fileSize / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.2f GB", mb / 1024.0);
    }

    /** 来源简述 */
    public String getSourceText() {
        return isUrl ? "网络 · " + format.toUpperCase() : "本地 · " + format.toUpperCase();
    }

    @Override
    public String toString() {
        return title + " [" + format + "]";
    }
}