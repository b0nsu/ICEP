@echo off
setlocal

set ROOT_DIR=%~dp0
set OUT_DIR=%ROOT_DIR%out
set CLASS_DIR=%OUT_DIR%\classes
set JAR_PATH=%OUT_DIR%\study-room-cli.jar

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
if not exist "%CLASS_DIR%" mkdir "%CLASS_DIR%"
if exist "%JAR_PATH%" del "%JAR_PATH%"

javac -encoding UTF-8 -d "%CLASS_DIR%" "%ROOT_DIR%src\*.java"
if errorlevel 1 exit /b 1

jar --create --file "%JAR_PATH%" --main-class Main -C "%CLASS_DIR%" .
if errorlevel 1 exit /b 1

echo Build success: %JAR_PATH%
