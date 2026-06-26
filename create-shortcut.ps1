$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Join-Path $root "dist\time-logger.jar"
$pngPath = Join-Path $root "src\main\java\com\timelogger\icon.png"
$icoPath = Join-Path $root "icon.ico"

if (-not (Test-Path $jarPath)) {
    throw "JAR not found. Run .\\build-jar.ps1 first."
}

# 1. Convert PNG to high-res ICO file if PNG exists
if (Test-Path $pngPath) {
    Write-Host "Generating icon.ico from PNG asset..."
    $pngBytes = [System.IO.File]::ReadAllBytes($pngPath)
    $pngSize = $pngBytes.Length

    # Create 22-byte ICO header wrapping the raw PNG
    $icoHeader = New-Object byte[] 22
    $icoHeader[0] = 0; $icoHeader[1] = 0                 # Reserved (0)
    $icoHeader[2] = 1; $icoHeader[3] = 0                 # Type (1 = Icon)
    $icoHeader[4] = 1; $icoHeader[5] = 0                 # Count (1 image)
    $icoHeader[6] = 0                                    # Width (0 for 256)
    $icoHeader[7] = 0                                    # Height (0 for 256)
    $icoHeader[8] = 0                                    # Colors (0 for >256)
    $icoHeader[9] = 0                                    # Reserved (0)
    $icoHeader[10] = 1; $icoHeader[11] = 0               # Planes (1)
    $icoHeader[12] = 32; $icoHeader[13] = 0              # Bit count (32)
    # Size of raw PNG data (4 bytes)
    $icoHeader[14] = $pngSize -band 0xFF
    $icoHeader[15] = ($pngSize -shr 8) -band 0xFF
    $icoHeader[16] = ($pngSize -shr 16) -band 0xFF
    $icoHeader[17] = ($pngSize -shr 24) -band 0xFF
    # Offset to raw PNG data (22)
    $icoHeader[18] = 22; $icoHeader[19] = 0; $icoHeader[20] = 0; $icoHeader[21] = 0

    $stream = [System.IO.File]::Create($icoPath)
    try {
        $stream.Write($icoHeader, 0, $icoHeader.Length)
        $stream.Write($pngBytes, 0, $pngBytes.Length)
    } finally {
        $stream.Dispose()
    }
}

# 2. Determine target path and arguments based on standalone native launcher availability
$nativeLauncherPath = Join-Path $root "dist\TimeLogger\TimeLogger.exe"
$targetPath = "javaw.exe"
$arguments = "-Xms4m -Xmx24m -XX:+UseSerialGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:CICompilerCount=1 -XX:TieredStopAtLevel=1 -Xss256k -XX:ReservedCodeCacheSize=12m -XX:MaxMetaspaceSize=20m -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Dsun.java2d.opengl=false -Dsun.zip.disableMemoryMapping=true -jar `"$jarPath`""
$workingDirectory = $root

if (Test-Path $nativeLauncherPath) {
    Write-Host "Found standalone native launcher at dist\TimeLogger\TimeLogger.exe. Targeting native executable..."
    $targetPath = $nativeLauncherPath
    $arguments = ""
    $workingDirectory = Join-Path $root "dist\TimeLogger"
}

# 3. Create the shortcut in the project folder
$shortcutPath = Join-Path $root "Time Logger.lnk"

$WshShell = New-Object -ComObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut($shortcutPath)
$Shortcut.TargetPath = $targetPath
$Shortcut.Arguments = $arguments
$Shortcut.WorkingDirectory = $workingDirectory
$Shortcut.Description = "Time Logger Desktop Application"

# Apply the custom generated icon
if (Test-Path $icoPath) {
    $Shortcut.IconLocation = "$icoPath,0"
}

$Shortcut.Save()

Write-Host "Shortcut created successfully at: $shortcutPath"
Write-Host "You can now copy/move this shortcut to your Desktop, Start Menu, or Pin to Taskbar."
