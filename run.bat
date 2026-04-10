@echo off
setlocal

set ROOT_DIR=%~dp0
set JAR_PATH=%ROOT_DIR%out\study-room-cli.jar

if not exist "%JAR_PATH%" (
  call "%ROOT_DIR%build.bat"
  if errorlevel 1 exit /b %errorlevel%
)

java -jar "%JAR_PATH%"
