package com.sausage.music.player;

import com.sausage.music.model.Song;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 音乐播放器核心 - 基于 Java Sound API + MP3SPI
 */
public class MusicPlayer {
    public enum PlayMode { SEQUENCE, REPEAT_ONE, SHUFFLE }

    private final List<Song> playlist = new ArrayList<>();
    private final List<Song> playQueue = new ArrayList<>();   // 播放队列（下一首优先）
    private int currentIndex = -1;
    private Song currentSong = null;      // 当前播放歌曲（可能来自列表或队列）
    private boolean fromQueue = false;    // 当前歌曲是否来自播放队列
    private PlayMode playMode = PlayMode.SEQUENCE;

    private SourceDataLine line;
    private AudioInputStream audioStream;
    private Thread playThread;
    private volatile boolean paused = false;
    private volatile boolean stopped = false;
    private volatile boolean playing = false;
    private float volume = 0.8f;

    private long totalBytes = 0;
    private long bytesRead = 0;
    private int bytesPerSecond = 0;

    public interface PlayerListener {
        void onPlay(Song song);
        void onPause();
        void onResume();
        void onStop();
        void onProgress(long currentSec, long totalSec);
        void onSongEnd();
    }
    private final List<PlayerListener> listeners = new ArrayList<>();
    public void addListener(PlayerListener l) { listeners.add(l); }
    private void firePlay(Song s) { for (PlayerListener l : listeners) l.onPlay(s); }
    private void firePause() { for (PlayerListener l : listeners) l.onPause(); }
    private void fireResume() { for (PlayerListener l : listeners) l.onResume(); }
    private void fireStop() { for (PlayerListener l : listeners) l.onStop(); }
    private void fireProgress(long c, long t) { for (PlayerListener l : listeners) l.onProgress(c, t); }
    private void fireSongEnd() { for (PlayerListener l : listeners) l.onSongEnd(); }

    // ---------- 播放队列监听 ----------
    public interface QueueListener { void onQueueChanged(); }
    private final List<QueueListener> queueListeners = new ArrayList<>();
    public void addQueueListener(QueueListener l) { queueListeners.add(l); }
    private void fireQueueChanged() { for (QueueListener l : queueListeners) l.onQueueChanged(); }

    // ---------- 播放列表管理 ----------
    public List<Song> getPlaylist() { return playlist; }
    public int getCurrentIndex() { return currentIndex; }
    public Song getCurrentSong() { return currentSong; }
    public boolean isFromQueue() { return fromQueue; }
    public PlayMode getPlayMode() { return playMode; }
    public void setPlayMode(PlayMode m) { playMode = m; }
    public boolean isPlaying() { return playing && !paused; }
    public boolean isPaused() { return paused; }

    public void addSong(Song s) { playlist.add(s); }
    public void addSongs(List<Song> songs) { playlist.addAll(songs); }
    public void removeSong(int index) {
        if (index < 0 || index >= playlist.size()) return;
        boolean wasCurrent = (index == currentIndex);
        playlist.remove(index);
        if (wasCurrent) { stop(); }
        else if (index < currentIndex) currentIndex--;
    }
    public void clearPlaylist() { stop(); playlist.clear(); }

    // ---------- 播放队列 ----------
    public List<Song> getQueue() { return playQueue; }
    public void addToQueue(Song s) { playQueue.add(s); fireQueueChanged(); }
    /** 插入到队首（下一首立即播放） */
    public void addToQueueNext(Song s) { playQueue.add(0, s); fireQueueChanged(); }
    public void removeFromQueue(int index) {
        if (index < 0 || index >= playQueue.size()) return;
        playQueue.remove(index);
        fireQueueChanged();
    }
    public void clearQueue() { playQueue.clear(); fireQueueChanged(); }

    // ---------- 播放控制 ----------
    public void play(int index) {
        if (index < 0 || index >= playlist.size()) return;
        stopInternal();
        currentIndex = index;
        fromQueue = false;
        Song song = playlist.get(index);
        currentSong = song;
        startPlayback(song);
        firePlay(song);
    }

    /** 直接播放一首歌曲（来自收藏/队列等，不在 playlist 索引内） */
    public void playDirect(Song song) {
        if (song == null) return;
        stopInternal();
        currentIndex = -1;
        fromQueue = true;
        currentSong = song;
        startPlayback(song);
        firePlay(song);
    }

    public void playNext() {
        // 单曲循环：重播当前歌曲
        if (playMode == PlayMode.REPEAT_ONE && currentSong != null) {
            if (fromQueue) playDirect(currentSong);
            else play(currentIndex);
            return;
        }
        // 播放队列非空：优先消费队列
        if (!playQueue.isEmpty()) {
            Song next = playQueue.remove(0);
            fireQueueChanged();
            playDirect(next);
            return;
        }
        if (playlist.isEmpty()) return;
        int next;
        switch (playMode) {
            case SHUFFLE:
                if (playlist.size() == 1) next = 0;
                else { do { next = (int)(Math.random() * playlist.size()); } while (next == currentIndex); }
                break;
            default: next = (currentIndex + 1) % playlist.size();
        }
        play(next);
    }

    public void playPrev() {
        if (playlist.isEmpty() && currentSong == null) return;
        if (playlist.isEmpty()) { playDirect(currentSong); return; }
        int prev = (currentIndex - 1 + playlist.size()) % playlist.size();
        play(prev);
    }

    public void pause() {
        if (playing && !paused) { paused = true; firePause(); }
    }

    public void resume() {
        if (playing && paused) { paused = false; fireResume(); }
    }

    public void togglePause() { if (paused) resume(); else pause(); }

    public void stop() { stopInternal(); currentIndex = -1; currentSong = null; fromQueue = false; fireStop(); }

    private void stopInternal() {
        stopped = true;
        paused = false;
        playing = false;
        closeLine(); // 先关闭音频行，解除 line.write() 阻塞
        if (playThread != null) {
            playThread.interrupt(); // 解除 Thread.sleep() 阻塞（暂停状态）
            try {
                playThread.join(2000); // 等待旧线程彻底退出，防止其关闭新音频行
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            playThread = null;
        }
    }

    private void closeLine() {
        if (line != null) { try { line.stop(); line.close(); } catch (Exception e) {} line = null; }
        if (audioStream != null) { try { audioStream.close(); } catch (Exception e) {} audioStream = null; }
    }

    // ---------- 进度 ----------
    public long getCurrentPosition() {
        if (bytesPerSecond <= 0) return 0;
        return bytesRead / bytesPerSecond;
    }

    public void seekTo(long seconds) {
        Song song = getCurrentSong();
        if (song == null || totalBytes <= 0) return;
        long targetSec = Math.max(0, Math.min(seconds, song.getDuration()));
        long targetBytes = targetSec * bytesPerSecond;
        boolean wasPaused = paused;
        stopInternal();
        startPlaybackAt(song, targetBytes);
        if (wasPaused) { paused = true; }
        else { firePlay(song); }
    }

    // ---------- 音量 ----------
    public float getVolume() { return volume; }
    public void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        applyVolume();
    }
    private void applyVolume() {
        if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl vc = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float min = vc.getMinimum();
            float max = vc.getMaximum();
            float gain = volume == 0 ? min : min + (max - min) * volume;
            vc.setValue(gain);
        }
    }

    // ---------- 内部播放 ----------
    private void startPlayback(Song song) { startPlaybackAt(song, 0); }

    private void startPlaybackAt(Song song, long skipBytes) {
        try {
            File file = new File(song.getFilePath());
            AudioInputStream rawStream = AudioSystem.getAudioInputStream(file);
            AudioFormat baseFormat = rawStream.getFormat();

            // 转换为 PCM
            AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false
            );
            AudioInputStream decoded = AudioSystem.getAudioInputStream(decodedFormat, rawStream);

            // 计算总字节数
            long frameLength = rawStream.getFrameLength();
            if (frameLength != AudioSystem.NOT_SPECIFIED) {
                totalBytes = frameLength * decodedFormat.getFrameSize();
            } else {
                totalBytes = file.length();
            }
            bytesPerSecond = (int)(decodedFormat.getFrameRate() * decodedFormat.getFrameSize());

            // skip
            if (skipBytes > 0) {
                long skipped = 0;
                byte[] tmp = new byte[8192];
                while (skipped < skipBytes) {
                    int n = decoded.read(tmp, 0, (int) Math.min(tmp.length, skipBytes - skipped));
                    if (n < 0) break;
                    skipped += n;
                }
                bytesRead = skipped;
            } else {
                bytesRead = 0;
            }

            audioStream = decoded;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(decodedFormat);
            line.start();
            applyVolume();

            stopped = false;
            paused = false;
            playing = true;

            playThread = new Thread(() -> playLoop(song), "MusicPlayThread");
            playThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            playing = false;
        }
    }

    private void playLoop(Song song) {
        byte[] buffer = new byte[4096];
        try {
            while (!stopped && line != null) {
                if (paused) { Thread.sleep(50); continue; }
                int n = audioStream.read(buffer, 0, buffer.length);
                if (n < 0) break;
                line.write(buffer, 0, n);
                bytesRead += n;
                long curSec = bytesPerSecond > 0 ? bytesRead / bytesPerSecond : 0;
                fireProgress(curSec, song.getDuration());
            }
        } catch (InterruptedException e) {
            // 正常停止
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!stopped) {
            // 自然播放结束
            playing = false;
            paused = false;
            closeLine();
            fireSongEnd();
        }
    }
}