@echo off
chcp 65001 >nul 2>&1
cd /d %~dp0lib\javafx
set BASE=https://maven.aliyun.com/repository/public/org/openjfx
set V=21.0.4
for %%M in (base graphics controls media swing) do (
  echo Downloading javafx-%%M-%V%-win.jar ...
  curl -sL -o javafx-%%M-%V%-win.jar %BASE%/javafx-%%M/%V%/javafx-%%M-%V%-win.jar
)
echo.
echo All downloads complete.
dir *.jar