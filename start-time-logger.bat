@echo off
setlocal

cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-jar.ps1"
if errorlevel 1 (
  echo Build failed. Press any key to close.
  pause >nul
  exit /b 1
)

if not exist "%~dp0dist\time-logger.jar" (
  echo JAR not found after build. Press any key to close.
  pause >nul
  exit /b 1
)

where javaw >nul 2>nul
if %errorlevel%==0 (
  start "Time Logger" javaw -jar "%~dp0dist\time-logger.jar"
  exit /b 0
)

where java >nul 2>nul
if %errorlevel%==0 (
  start "Time Logger" java -jar "%~dp0dist\time-logger.jar"
  exit /b 0
)

echo Java was not found in PATH. Install Java and try again.
pause >nul
exit /b 1
