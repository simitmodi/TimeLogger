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

if exist "%~dp0dist\TimeLogger\TimeLogger.exe" (
  cd /d "%~dp0dist\TimeLogger"
  start "" "TimeLogger.exe"
  exit /b 0
)

where javaw >nul 2>nul
if %errorlevel%==0 (
  start "Time Logger" javaw -Xms4m -Xmx24m -XX:+UseSerialGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:CICompilerCount=1 -XX:TieredStopAtLevel=1 -Xss256k -XX:ReservedCodeCacheSize=12m -XX:MaxMetaspaceSize=20m "-Dsun.java2d.d3d=false" "-Dsun.java2d.noddraw=true" "-Dsun.java2d.opengl=false" "-Dsun.zip.disableMemoryMapping=true" -jar "%~dp0dist\time-logger.jar"
  exit /b 0
)

where java >nul 2>nul
if %errorlevel%==0 (
  start "Time Logger" java -Xms4m -Xmx24m -XX:+UseSerialGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:CICompilerCount=1 -XX:TieredStopAtLevel=1 -Xss256k -XX:ReservedCodeCacheSize=12m -XX:MaxMetaspaceSize=20m "-Dsun.java2d.d3d=false" "-Dsun.java2d.noddraw=true" "-Dsun.java2d.opengl=false" "-Dsun.zip.disableMemoryMapping=true" -jar "%~dp0dist\time-logger.jar"
  exit /b 0
)

echo Java was not found in PATH. Install Java and try again.
pause >nul
exit /b 1
