package com.sausage.music.tools;

import javax.swing.*;
import java.io.File;

public class TestImg {
    public static void main(String[] args) {
        System.out.println("user.dir = " + System.getProperty("user.dir"));
        for (int i = 1; i <= 4; i++) {
            File f = new File("resources/images/" + i + ".jpg");
            System.out.println("resources/images/" + i + ".jpg exists=" + f.exists() + " abs=" + f.getAbsolutePath());
            if (f.exists()) {
                ImageIcon img = new ImageIcon(f.getAbsolutePath());
                System.out.println("  iconWidth=" + img.getIconWidth() + " iconHeight=" + img.getIconHeight());
                System.out.println("  imageWidth=" + img.getImage().getWidth(null) + " imageHeight=" + img.getImage().getHeight(null));
            }
        }
    }
}