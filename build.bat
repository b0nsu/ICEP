@echo off
setlocal

set ROOT_DIR=%~dp0
set OUT_DIR=%ROOT_DIR%out
set JAR_PATH=%OUT_DIR%\study-room-cli.jar

call "%ROOT_DIR%gradlew.bat" clean jar
if errorlevel 1 exit /b %errorlevel%

echo Build success: %JAR_PATH%
