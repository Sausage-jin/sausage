<div align="center">

# 🎵 SAUSAGE Music

**Black & White Premium Local Music / MV Player**

Java 21 · Swing · JavaFX 21 · Windows

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)]()
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.4-blue?style=flat-square)]()
[![Platform](https://img.shields.io/badge/Platform-Windows-success?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)]()

</div>

---

## 🖼️ Interface Preview

<table>
<img width="821" height="538" alt="屏幕截图 2026-11-14 223447" src="https://github.com/user-attachments/assets/82b7edac-b067-4c81-9be0-a4c6c629e4e6" />

</table>

---

## ✨ Features

**🎵 Music Playback**
- Multi-format decoding: WAV / MP3 / OGG / FLAC
- Play/Pause · Previous/Next · Progress seeking · Volume control
- Sequential / Repeat One / Shuffle, one-click switch
- Independent playback queue with "Play Next" insertion

**📺 MV Playback**
- Local video: MP4 / AVI / MKV / MOV / FLV / WebM
- Network streams: HTTP / HTTPS / RTMP / RTSP
- JavaFX MediaPlayer seamlessly embedded in Swing via JFXPanel
- Play/Pause · Progress seeking · Duration display · Error prompts
  <img width="824" height="535" alt="屏幕截图 2026-11-14 223553" src="https://github.com/user-attachments/assets/b3566e7c-1a8e-4a18-a3c8-64821ccce878" />


**💾 Favorites**
- One-click favorite, persisted to `~/.sausage/favorites.txt`
- Auto-restore on startup, invalid files automatically skipped

**🖥️ Interface**
- Borderless window · Custom title bar · Drag to move · Double-click to maximize
- Immersive "Now Playing" page with vinyl record rotation + tonearm animation
- Sidebar navigation: Discover / Local / Favorites / Playlists / MV
<img width="820" height="542" alt="屏幕截图 2026-11-14 223606" src="https://github.com/user-attachments/assets/780e9cb9-b594-4799-97a1-7e0c75783607" />

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------|------|------|
| Java | 21 | Language & runtime |
| Swing | JDK built-in | Main UI framework |
| JavaFX | 21.0.4 | MV video (JFXPanel embedding) |
| Java Sound API | JDK built-in | Audio decoding (SourceDataLine) |
| MP3SPI | 1.9.5 | MP3 decoding |
| JLayer | 1.0.1 | MP3 decoding base library |
| Tritonus Share | 0.3.6 | Audio service base library |
| jpackage / jlink | JDK built-in | Packaging & runtime image |
| WiX Toolset | 3.x | exe installer |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────┐
│                  MainFrame                   │
│  ┌─────────┐  ┌───────────────────────────┐  │
│  │Sidebar  │  │      ContentPanel         │  │
│  │(Nav)    │  │ Discover│Local│Fav│List│MV│  │
│  └─────────┘  └───────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │            PlayerBar (Bottom)          │  │
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │       NowPlayingPanel (Expanded)       │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
       │                      │
 MusicPlayer            MVPlayer
(Java Sound+MP3SPI)    (JavaFX Media)
       │
FavoritesManager
```

- **Observer Pattern** — PlayerListener / QueueListener / MVListener decouple core from UI
- **CardLayout Navigation** — Card switching in content area
- **Swing + JavaFX Hybrid** — JFXPanel bridge, leveraging the strengths of each
- **Thread Safety** — Independent playback thread, UI updates via SwingUtilities / Platform.runLater

---

## 📁 Project Structure

```
sausage-music/
├── src/com/sausage/music/
│   ├── SausageMusic.java       # Entry point
│   ├── model/                  # Song / MV models
│   ├── player/                 # MusicPlayer / MVPlayer
│   ├── ui/                     # Main window / Sidebar / Player bar / Theme
│   ├── util/                   # FavoritesManager
│   └── tools/                  # ToneGenerator and other utilities
├── lib/                        # JavaFX / JLayer / MP3SPI / Tritonus
├── resources/
│   ├── icons/  images/  music/ # Icons / Covers / Test melodies
├── run.bat  package.bat  SAUSAGE.vbs
└── README.md  LICENSE  version.txt
```

---

## 🚀 Quick Start

**Requirements**: JDK 21+ · Windows 10/11

```bat
git clone https://github.com/<your-name>/sausage-music.git
cd sausage-music
run.bat
```

> `run.bat` compiles and launches automatically. Requires `JAVA_HOME` to be set or `java` in your `PATH`.

`resources/music/` includes 5 royalty-free test melodies generated programmatically. You can also drop in your own music files for auto-loading.

<details>
<summary>Manual Compile & Run</summary>

```bat
javac -encoding UTF-8 ^
  --module-path lib/javafx --add-modules javafx.controls,javafx.media,javafx.swing ^
  -cp "lib/jlayer.jar;lib/mp3spi.jar;lib/tritonus-share.jar" ^
  -d out src/com/sausage/music/**/*.java

java --module-path lib/javafx --add-modules javafx.controls,javafx.media,javafx.swing ^
  -cp "out;lib/jlayer.jar;lib/mp3spi.jar;lib/tritonus-share.jar" ^
  com.sausage.music.SausageMusic
```
</details>

---

## 📦 Packaging

```bat
set JAVA_HOME=C:\path\to\jdk-21
package.bat
```

jlink generates the runtime image → jar packaging → jpackage produces both a portable version and an exe installer (requires [WiX 3.x](https://wixtoolset.org/)).

---

## ⌨️ Keyboard Shortcuts

| Key | Function |
|----|------|
| `Space` | Play / Pause |
| `ESC` | Exit |

---

## 📄 License

[MIT License](LICENSE)
<div align="center">

# 🎵 SAUSAGE Music

**黑白高级风本地音乐 / MV 播放器**

Java 21 · Swing · JavaFX 21 · Windows

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)]()
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.4-blue?style=flat-square)]()
[![Platform](https://img.shields.io/badge/Platform-Windows-success?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)]()

</div>

---

## � 界面预览

<table>
<img width="821" height="538" alt="屏幕截图 2026-11-14 223447" src="https://github.com/user-attachments/assets/e6fb3d01-3cb0-4b9c-835f-497116101684" />

</table>

---

## ✨ 功能

**� 音乐播放**
- WAV / MP3 / OGG / FLAC 多格式解码
- 播放/暂停 · 上/下一首 · 进度拖拽 · 音量调节
- 顺序 / 单曲循环 / 随机,一键切换
- 独立播放队列,支持"下一首播放"插队

**📺 MV 播放**
- 本地视频:MP4 / AVI / MKV / MOV / FLV / WebM
- 网络流:HTTP / HTTPS / RTMP / RTSP
- JavaFX MediaPlayer 经 JFXPanel 无缝嵌入 Swing
- 播放/暂停 · 进度拖拽 · 时长显示 · 错误提示
- <img width="824" height="535" alt="屏幕截图 2026-11-14 223553" src="https://github.com/user-attachments/assets/b6a39160-67a0-47c9-b16a-1f3e674f6bfe" />


**💾 收藏**
- 一键收藏,持久化至 `~/.sausage/favorites.txt`
- 启动自动恢复,失效文件自动跳过

**🖥️ 界面**
- 无边框窗口 · 自定义标题栏 · 拖拽移动 · 双击最大化
- 沉浸式"正在播放"页,黑胶唱片旋转 + 唱臂动画
- 侧边栏导航:发现 / 本地 / 喜欢 / 列表 / MV
<img width="820" height="542" alt="屏幕截图 2026-11-14 223606" src="https://github.com/user-attachments/assets/f204b1dd-9765-4f23-9500-374ec9a76cbe" />


---

## 🛠️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 语言与运行时 |
| Swing | JDK 内置 | 主界面框架 |
| JavaFX | 21.0.4 | MV 视频(JFXPanel 嵌入) |
| Java Sound API | JDK 内置 | 音频解码(SourceDataLine) |
| MP3SPI | 1.9.5 | MP3 解码 |
| JLayer | 1.0.1 | MP3 解码基础库 |
| Tritonus Share | 0.3.6 | 音频服务基础库 |
| jpackage / jlink | JDK 内置 | 打包与运行时镜像 |
| WiX Toolset | 3.x | exe 安装程序 |

---

## 🏗️ 架构

```
┌─────────────────────────────────────────────┐
│                  MainFrame                   │
│  ┌─────────┐  ┌───────────────────────────┐  │
│  │Sidebar  │  │      ContentPanel         │  │
│  │(导航)   │  │ 发现│本地│喜欢│列表│MV    │  │
│  └─────────┘  └───────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │            PlayerBar (底部)            │  │
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │       NowPlayingPanel (展开页)         │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
       │                      │
  MusicPlayer            MVPlayer
(Java Sound+MP3SPI)    (JavaFX 媒体)
       │
 FavoritesManager
```

- **观察者模式** — PlayerListener / QueueListener / MVListener 解耦核心与 UI
- **CardLayout 导航** — 内容区卡片切换
- **Swing + JavaFX 混合** — JFXPanel 桥接,各取所长
- **线程安全** — 播放线程独立,UI 更新经 SwingUtilities / Platform.runLater

---

## 📁 项目结构

```
sausage-music/
├── src/com/sausage/music/
│   ├── SausageMusic.java       # 入口
│   ├── model/                  # Song / MV 模型
│   ├── player/                 # MusicPlayer / MVPlayer
│   ├── ui/                     # 主窗口 / 侧边栏 / 播放栏 / 主题
│   ├── util/                   # FavoritesManager
│   └── tools/                  # ToneGenerator 等工具
├── lib/                        # JavaFX / JLayer / MP3SPI / Tritonus
├── resources/
│   ├── icons/  images/  music/ # 图标 / 封面 / 测试旋律
├── run.bat  package.bat  SAUSAGE.vbs
└── README.md  LICENSE  version.txt
```

---

## 🚀 快速开始

**环境**:JDK 21+ · Windows 10/11

```bat
git clone https://github.com/<your-name>/sausage-music.git
cd sausage-music
run.bat
```

> `run.bat` 自动编译并启动。需设置 `JAVA_HOME` 或将 `java` 加入 `PATH`。

`resources/music/` 内置 5 首程序生成的免版权测试旋律,也可放入自己的音乐文件自动加载。

<details>
<summary>手动编译运行</summary>

```bat
javac -encoding UTF-8 ^
  --module-path lib/javafx --add-modules javafx.controls,javafx.media,javafx.swing ^
  -cp "lib/jlayer.jar;lib/mp3spi.jar;lib/tritonus-share.jar" ^
  -d out src/com/sausage/music/**/*.java

java --module-path lib/javafx --add-modules javafx.controls,javafx.media,javafx.swing ^
  -cp "out;lib/jlayer.jar;lib/mp3spi.jar;lib/tritonus-share.jar" ^
  com.sausage.music.SausageMusic
```
</details>

---

## 📦 打包

```bat
set JAVA_HOME=C:\path\to\jdk-21
package.bat
```

jlink 生成运行时镜像 → jar 打包 → jpackage 生成免安装版与 exe 安装包(需 [WiX 3.x](https://wixtoolset.org/))。

---

## ⌨️ 快捷键

| 键 | 功能 |
|----|------|
| `Space` | 播放 / 暂停 |
| `ESC` | 退出 |

---

## 📄 协议

[MIT License](LICENSE)
