package com.sausage.music.tools;

import javax.sound.sampled.*;
import java.io.*;

/**
 * 测试音频生成器 - 生成简单的旋律 WAV 文件
 */
public class ToneGenerator {

    static final int SAMPLE_RATE = 44100;
    static final int BITS = 16;
    static final int CHANNELS = 1;

    // 音符频率表
    static final double C4=261.63, D4=293.66, E4=329.63, F4=349.23,
                        G4=392.00, A4=440.00, B4=493.88,
                        C5=523.25, D5=587.33, E5=659.25, F5=698.46,
                        G5=783.99, A5=880.00,
                        REST=0;

    public static void main(String[] args) throws Exception {
        File dir = new File("resources/music");
        if (!dir.exists()) dir.mkdirs();

        // 1. 小星星
        double[] starNotes = {C4,C4,G4,G4,A4,A4,G4,REST,F4,F4,E4,E4,D4,D4,C4,
                              G4,G4,F4,F4,E4,E4,D4,REST,G4,G4,F4,F4,E4,E4,D4};
        double[] starDur   = {0.5,0.5,0.5,0.5,0.5,0.5,1.0,0.2,0.5,0.5,0.5,0.5,0.5,0.5,1.0,
                              0.5,0.5,0.5,0.5,0.5,0.5,1.0,0.2,0.5,0.5,0.5,0.5,0.5,0.5,1.0};
        generateMelody(starNotes, starDur, "resources/music/小星星 - 测试旋律.wav", "小星星");

        // 2. 欢乐颂
        double[] odeNotes = {E4,E4,F4,G4,G4,F4,E4,D4,C4,C4,D4,E4,E4,D4,D4,
                             E4,E4,F4,G4,G4,F4,E4,D4,C4,C4,D4,E4,D4,C4,C4};
        double[] odeDur   = {0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.6,0.2,1.0,
                             0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.6,0.2,1.0};
        generateMelody(odeNotes, odeDur, "resources/music/欢乐颂 - 测试旋律.wav", "欢乐颂");

        // 3. 电子节拍
        double[] beatNotes = {C5,REST,E5,REST,G5,REST,E5,REST,C5,REST,D5,REST,F5,REST,D5,REST,
                              C5,REST,E5,REST,G5,REST,A5,REST,G5,REST,E5,REST,C5,REST,REST};
        double[] beatDur   = {0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,
                              0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,
                              0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,
                              0.25,0.25,0.25,0.25,0.5,0.5};
        generateMelody(beatNotes, beatDur, "resources/music/电子节拍 - 测试旋律.wav", "电子节拍");

        // 4. 蓝色梦幻
        double[] dreamNotes = {A4,REST,C5,REST,E5,REST,A5,REST,G5,REST,E5,REST,C5,REST,A4,REST,
                               G4,REST,B4,REST,D5,REST,G5,REST,F5,REST,D5,REST,B4,REST,G4};
        double[] dreamDur   = {0.6,0.15,0.6,0.15,0.6,0.15,0.9,0.3,
                               0.6,0.15,0.6,0.15,0.6,0.15,0.9,0.3,
                               0.6,0.15,0.6,0.15,0.6,0.15,0.9,0.3,
                               0.6,0.15,0.6,0.15,0.6,0.15,0.9};
        generateMelody(dreamNotes, dreamDur, "resources/music/蓝色梦幻 - 测试旋律.wav", "蓝色梦幻");

        // 5. 上升音阶
        double[] scaleNotes = {C4,D4,E4,F4,G4,A4,B4,C5,D5,E5,F5,G5,A5,
                               A5,G5,F5,E5,D5,C5,B4,A4,G4,F4,E4,D4,C4};
        double[] scaleDur = new double[scaleNotes.length];
        for (int i = 0; i < scaleDur.length; i++) scaleDur[i] = 0.3;
        generateMelody(scaleNotes, scaleDur, "resources/music/上升音阶 - 测试旋律.wav", "上升音阶");

        System.out.println("已生成 5 首测试旋律到 resources/music/ 目录");
    }

    /** 生成旋律 WAV */
    static void generateMelody(double[] notes, double[] durations, String filename, String name) throws Exception {
        // 计算总采样数
        long totalSamples = 0;
        for (double d : durations) totalSamples += (long)(d * SAMPLE_RATE);
        // 加 0.5 秒尾部静音
        totalSamples += SAMPLE_RATE / 2;

        byte[] buffer = new byte[(int)(totalSamples * 2)]; // 16bit mono

        int pos = 0;
        int count = Math.min(notes.length, durations.length);
        for (int i = 0; i < count; i++) {
            double freq = notes[i];
            double dur = durations[i];
            int samples = (int)(dur * SAMPLE_RATE);
            for (int j = 0; j < samples && pos < buffer.length - 1; j++) {
                double t = (double) j / SAMPLE_RATE;
                double val;
                if (freq == REST) {
                    val = 0;
                } else {
                    // 主音 + 泛音，模拟乐器音色
                    double env = envelope(j, samples);
                    val = (Math.sin(2 * Math.PI * freq * t) * 0.5
                         + Math.sin(2 * Math.PI * freq * 2 * t) * 0.2
                         + Math.sin(2 * Math.PI * freq * 3 * t) * 0.1) * env;
                }
                // 16bit 小端
                short s = (short)(val * Short.MAX_VALUE * 0.7);
                buffer[pos++] = (byte)(s & 0xFF);
                buffer[pos++] = (byte)((s >> 8) & 0xFF);
            }
        }

        writeWav(filename, buffer);
        System.out.println("生成: " + filename);
    }

    /** 简单包络：快攻击 + 指数衰减 */
    static double envelope(int j, int total) {
        double p = (double) j / total;
        double attack = Math.min(1.0, p / 0.05);      // 前5%快速升起
        double decay = Math.exp(-p * 2.5);              // 指数衰减
        return attack * decay;
    }

    /** 写 WAV 文件 */
    static void writeWav(String filename, byte[] pcm) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS, CHANNELS, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(pcm);
        AudioInputStream ais = new AudioInputStream(bais, format, pcm.length / 2);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
        ais.close();
    }
}