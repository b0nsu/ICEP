@echo off
setlocal

set ROOT_DIR=%~dp0
set JAR_PATH=%ROOT_DIR%out\study-room-cli.jar

call "%ROOT_DIR%gradlew.bat" clean jar
if errorlevel 1 exit /b %errorlevel%

echo Build success: %JAR_PATH%
