package com.sausage.music.ui;

import com.sausage.music.model.MV;
import com.sausage.music.ui.theme.Theme;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * MV 内嵌播放面板：基于 JavaFX MediaPlayer（Swing JFXPanel 内嵌）。
 * 支持本地视频文件和网络 URL，带播放/暂停、进度条、时长显示、返回按钮。
 */
public class MVPlaybackPanel extends JPanel {

    private final JFXPanel fxPanel;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Button playPauseBtn;
    private Slider progressSlider;
    private Label timeLabel;
    private JLabel statusLabel; // Swing 侧错误/状态提示

    private boolean sliderDragging = false;
    private MV currentMV;
    private Runnable onBack; // 返回列表回调

    public MVPlaybackPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_CARD);

        // 顶部标题栏
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.BG_CARD);
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton backBtn = new JButton("← 返回列表");
        backBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        backBtn.setFocusPainted(false);
        backBtn.setBackground(Theme.BG_SELECTED);
        backBtn.setForeground(Theme.TEXT_DARK);
        backBtn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        backBtn.addActionListener(e -> {
            if (onBack != null) onBack.run();
        });
        topBar.add(backBtn, BorderLayout.WEST);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusLabel.setForeground(Theme.TEXT_MID);
        topBar.add(statusLabel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // JavaFX 嵌入面板
        fxPanel = new JFXPanel();
        fxPanel.setBackground(Theme.BG_CARD);
        add(fxPanel, BorderLayout.CENTER);

        // 初始化 JavaFX 场景
        Platform.runLater(this::initFXScene);
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    /** 初始化 JavaFX 场景（必须在 FX 线程） */
    private void initFXScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #000000;");

        // ---- 视频区域：StackPane 让 MediaView 居中并自适应尺寸 ----
        StackPane videoStack = new StackPane();
        videoStack.setStyle("-fx-background-color: #000000;");
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true); // 保持视频宽高比
        mediaView.fitWidthProperty().bind(videoStack.widthProperty());
        mediaView.fitHeightProperty().bind(videoStack.heightProperty());
        videoStack.getChildren().add(mediaView);
        root.setCenter(videoStack);

        // ---- 底部控制区：进度条 + 按钮行（独立区域，不覆盖视频） ----
        VBox bottomBox = new VBox();
        bottomBox.setStyle("-fx-background-color: #141414;");

        // 进度条行
        progressSlider = new Slider(0, 100, 0);
        progressSlider.setMaxWidth(Double.MAX_VALUE);
        progressSlider.setMinHeight(18);
        progressSlider.setPrefHeight(18);
        progressSlider.setStyle(
            "-fx-accent: #ffffff;" +
            "-fx-control-inner-background: #444444;");
        progressSlider.setDisable(true);
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (sliderDragging && mediaPlayer != null) {
                double pct = newVal.doubleValue() / 100.0;
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && total.toMillis() > 0) {
                    mediaPlayer.seek(Duration.millis(pct * total.toMillis()));
                }
            }
        });
        progressSlider.setOnMousePressed(e -> sliderDragging = true);
        progressSlider.setOnMouseReleased(e -> {
            sliderDragging = false;
            if (mediaPlayer != null) {
                double pct = progressSlider.getValue() / 100.0;
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && total.toMillis() > 0) {
                    mediaPlayer.seek(Duration.millis(pct * total.toMillis()));
                }
            }
        });
        bottomBox.getChildren().add(progressSlider);

        // 按钮行
        HBox controls = new HBox(10);
        controls.setStyle("-fx-padding: 6 14 8 14;");
        controls.setAlignment(Pos.CENTER_LEFT);

        playPauseBtn = new Button("▶");
        playPauseBtn.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #e5e5e5; -fx-font-size: 13px; -fx-background-radius: 4; -fx-min-width: 36; -fx-min-height: 26;");
        playPauseBtn.setDisable(true);
        playPauseBtn.setOnAction(e -> togglePlay());

        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        controls.getChildren().addAll(playPauseBtn, timeLabel);
        bottomBox.getChildren().add(controls);

        root.setBottom(bottomBox);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.BLACK);
        fxPanel.setScene(scene);
    }

    /** 加载并播放 MV（本地文件或网络 URL） */
    public void loadMV(MV mv) {
        if (mv == null) return;
        currentMV = mv;
        setStatus("加载中: " + mv.getTitle());

        Platform.runLater(() -> {
            stopPlayer();

            try {
                Media media;
                if (mv.isUrl()) {
                    media = new Media(mv.getSource());
                } else {
                    File file = new File(mv.getSource());
                    if (!file.exists()) {
                        setStatus("错误: 文件不存在 - " + file.getAbsolutePath());
                        return;
                    }
                    media = new Media(file.toURI().toString());
                }

                mediaPlayer = new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);

                mediaPlayer.setOnReady(() -> {
                    Duration total = media.getDuration();
                    playPauseBtn.setDisable(false);
                    progressSlider.setDisable(false);
                    playPauseBtn.setText("⏸");
                    setStatus("正在播放: " + mv.getTitle());
                    updateSlider(total, Duration.ZERO);
                });

                mediaPlayer.setOnPlaying(() -> playPauseBtn.setText("⏸"));
                mediaPlayer.setOnPaused(() -> playPauseBtn.setText("▶"));
                mediaPlayer.setOnEndOfMedia(() -> {
                    playPauseBtn.setText("▶");
                    progressSlider.setValue(0);
                    if (mediaPlayer != null) mediaPlayer.seek(Duration.ZERO);
                });

                mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    if (!sliderDragging && mediaPlayer != null) {
                        Duration total = mediaPlayer.getTotalDuration();
                        updateSlider(total, newTime);
                    }
                });

                mediaPlayer.setOnError(() -> {
                    String err = mediaPlayer.getError() != null
                            ? mediaPlayer.getError().getMessage()
                            : "未知错误";
                    setStatus("播放错误: " + err);
                    playPauseBtn.setDisable(true);
                    progressSlider.setDisable(true);
                });

                mediaPlayer.play();

            } catch (Exception e) {
                setStatus("加载失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        });
    }

    /** 更新进度条和时长显示 */
    private void updateSlider(Duration total, Duration current) {
        if (total == null || current == null) return;
        double totalSec = total.toSeconds();
        double curSec = current.toSeconds();
        if (totalSec > 0) {
            progressSlider.setValue(Math.min(100, (curSec / totalSec) * 100));
        }
        timeLabel.setText(formatTime(curSec) + " / " + formatTime(totalSec));
    }

    private String formatTime(double seconds) {
        if (seconds < 0 || Double.isNaN(seconds) || Double.isInfinite(seconds)) seconds = 0;
        int total = (int) seconds;
        int m = total / 60;
        int s = total % 60;
        if (m >= 60) {
            int h = m / 60;
            m = m % 60;
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%02d:%02d", m, s);
    }

    /** 播放/暂停切换 */
    private void togglePlay() {
        if (mediaPlayer == null) return;
        if (MediaPlayer.Status.PLAYING.equals(mediaPlayer.getStatus())) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.play();
        }
    }

    /** 停止播放并释放资源 */
    private void stopPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
        if (playPauseBtn != null) {
            playPauseBtn.setText("▶");
            playPauseBtn.setDisable(true);
        }
        if (progressSlider != null) {
            progressSlider.setValue(0);
            progressSlider.setDisable(true);
        }
        if (timeLabel != null) {
            timeLabel.setText("00:00 / 00:00");
        }
    }

    /** 面板被切换走时调用，停止播放释放资源 */
    public void stopAndRelease() {
        Platform.runLater(this::stopPlayer);
        setStatus(" ");
    }

    /** 在 Swing 侧显示状态文本 */
    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    public MV getCurrentMV() {
        return currentMV;
    }
}