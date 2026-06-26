$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$nativeLauncherPath = Join-Path $root "dist\TimeLogger\TimeLogger.exe"
$jarPath = Join-Path $root "dist\time-logger.jar"

if (Test-Path $nativeLauncherPath) {
    Write-Host "Launching app via standalone native launcher..."
    Set-Location (Join-Path $root "dist\TimeLogger")
    & ".\TimeLogger.exe"
} else {
    if (-not (Test-Path $jarPath)) {
        throw "JAR not found. Run .\\build-jar.ps1 first."
    }
    Set-Location $root
    java -Xms4m -Xmx24m -XX:+UseSerialGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:CICompilerCount=1 -XX:TieredStopAtLevel=1 -Xss256k -XX:ReservedCodeCacheSize=12m -XX:MaxMetaspaceSize=20m "-Dsun.java2d.d3d=false" "-Dsun.java2d.noddraw=true" "-Dsun.java2d.opengl=false" "-Dsun.zip.disableMemoryMapping=true" -jar $jarPath
}
