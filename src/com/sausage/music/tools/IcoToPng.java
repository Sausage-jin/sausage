package com.sausage.music.tools;

import java.awt.image.BufferedImage;
import java.io.*;

/** 临时工具：将 sausage.ico 转为 sausage.png */
public class IcoToPng {
    public static void main(String[] args) throws Exception {
        File ico = new File("resources/icons/sausage.ico");
        byte[] data = new byte[(int) ico.length()];
        try (FileInputStream fis = new FileInputStream(ico)) {
            fis.read(data);
        }
        int count = readU16(data, 4);
        System.out.println("ICO contains " + count + " image(s)");
        int bestOffset = 0, bestSize = 0, bestDim = 0;
        for (int i = 0; i < count; i++) {
            int base = 6 + i * 16;
            int w = data[base] == 0 ? 256 : (data[base] & 0xFF);
            int h = data[base + 1] == 0 ? 256 : (data[base + 1] & 0xFF);
            int size = readU32(data, base + 8);
            int offset = readU32(data, base + 12);
            System.out.println("  image " + i + ": " + w + "x" + h + " size=" + size);
            if (w > bestDim) { bestDim = w; bestOffset = offset; bestSize = size; }
        }
        // 检查是否为内嵌 PNG
        if (data[bestOffset] == (byte) 0x89 && data[bestOffset + 1] == 0x50) {
            System.out.println("Embedded PNG, extracting...");
            try (FileOutputStream fos = new FileOutputStream("resources/icons/sausage.png")) {
                fos.write(data, bestOffset, bestSize);
            }
        } else {
            System.out.println("BMP format, converting with AND mask...");
            BufferedImage img = readIcoBmp(data, bestOffset);
            // 用 PNG 格式写出（手动构建太复杂，改用简单PPM再转？不，直接写PNG）
            writePng(img, new File("resources/icons/sausage.png"));
        }
        System.out.println("Done: resources/icons/sausage.png");
    }

    static int readU16(byte[] d, int o) { return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8); }
    static int readU32(byte[] d, int o) {
        return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8) | ((d[o + 2] & 0xFF) << 16) | ((d[o + 3] & 0xFF) << 24);
    }

    static BufferedImage readIcoBmp(byte[] data, int offset) {
        int w = readU32(data, offset + 4);
        int h2 = readU32(data, offset + 8);
        int h = h2 / 2;
        int bitCount = readU16(data, offset + 14);
        System.out.println("BMP: " + w + "x" + h + " bitCount=" + bitCount);
        int paletteSize = (bitCount <= 8) ? (1 << bitCount) * 4 : 0;
        int paletteStart = offset + 40;
        int pixelStart = paletteStart + paletteSize;
        // AND 掩码在颜色数据之后，1bpp，每行4字节对齐
        int rowBytes = ((w * bitCount + 31) / 32) * 4;
        int andRowBytes = ((w + 31) / 32) * 4;
        int andStart = pixelStart + rowBytes * h;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        boolean hasAlpha = false;
        for (int y = 0; y < h; y++) {
            int srcY = h - 1 - y; // BMP 自底向上
            for (int x = 0; x < w; x++) {
                int r, g, b, a = 255;
                if (bitCount == 32) {
                    int p = pixelStart + (srcY * w + x) * 4;
                    b = data[p] & 0xFF; g = data[p + 1] & 0xFF; r = data[p + 2] & 0xFF;
                    a = data[p + 3] & 0xFF;
                } else if (bitCount == 24) {
                    int p = pixelStart + (srcY * w + x) * 3;
                    b = data[p] & 0xFF; g = data[p + 1] & 0xFF; r = data[p + 2] & 0xFF;
                } else if (bitCount == 8) {
                    int idx = data[pixelStart + srcY * rowBytes + x] & 0xFF;
                    int pp = paletteStart + idx * 4;
                    b = data[pp] & 0xFF; g = data[pp + 1] & 0xFF; r = data[pp + 2] & 0xFF;
                } else if (bitCount == 4) {
                    int byteVal = data[pixelStart + srcY * rowBytes + (x / 2)] & 0xFF;
                    int idx = (x % 2 == 0) ? (byteVal >> 4) : (byteVal & 0x0F);
                    int pp = paletteStart + idx * 4;
                    b = data[pp] & 0xFF; g = data[pp + 1] & 0xFF; r = data[pp + 2] & 0xFF;
                } else if (bitCount == 1) {
                    int byteVal = data[pixelStart + srcY * rowBytes + (x / 8)] & 0xFF;
                    int idx = (byteVal >> (7 - (x % 8))) & 1;
                    int pp = paletteStart + idx * 4;
                    b = data[pp] & 0xFF; g = data[pp + 1] & 0xFF; r = data[pp + 2] & 0xFF;
                } else {
                    r = g = b = 0;
                }
                // AND 掩码：1 = 透明，0 = 不透明
                int andByte = data[andStart + srcY * andRowBytes + (x / 8)] & 0xFF;
                int bit = (andByte >> (7 - (x % 8))) & 1;
                if (bit == 1) a = 0;
                else if (a == 0) a = 255; // alpha 全0时用掩码
                if (a > 0) hasAlpha = true;
                img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        System.out.println("hasAlpha=" + hasAlpha);
        return img;
    }

    static void writePng(BufferedImage img, File out) throws Exception {
        // 用 javax.imageio 写 PNG
        javax.imageio.ImageIO.write(img, "png", out);
    }
}