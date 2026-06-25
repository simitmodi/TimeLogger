$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$outDir = Join-Path $root "out\classes"
$distDir = Join-Path $root "dist"

if (Test-Path $outDir) {
    Remove-Item -Recurse -Force $outDir
}
if (-not (Test-Path $distDir)) {
    New-Item -ItemType Directory -Path $distDir | Out-Null
}

New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$javaFiles = Get-ChildItem -Path (Join-Path $root "src\main\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }

if (-not $javaFiles -or $javaFiles.Count -eq 0) {
    throw "No Java source files found in src/main/java"
}

$javacPath = (Get-Command javac -ErrorAction Stop).Source
$javacDir = Split-Path -Parent $javacPath
$jarPathExe = Join-Path $javacDir "jar.exe"
if (-not (Test-Path $jarPathExe) -and $env:JAVA_HOME) {
    $jarPathExe = Join-Path $env:JAVA_HOME "bin\jar.exe"
}
$hasJarExe = Test-Path $jarPathExe

javac -d $outDir $javaFiles
if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed"
}

# Copy resource assets (non-java files) to class files directory
Get-ChildItem -Path (Join-Path $root "src\main\java") -Recurse -File | Where-Object { $_.Extension -ne ".java" } | ForEach-Object {
    $srcPrefix = Join-Path $root "src\main\java"
    $relative = $_.FullName.Substring($srcPrefix.Length + 1)
    $dest = Join-Path $outDir $relative
    $destParent = Split-Path -Parent $dest
    if (-not (Test-Path $destParent)) {
        New-Item -ItemType Directory -Path $destParent -Force | Out-Null
    }
    Copy-Item -Path $_.FullName -Destination $dest -Force
}

$jarPath = Join-Path $distDir "time-logger.jar"
if (Test-Path $jarPath) {
    try {
        Remove-Item -Force $jarPath
    } catch {
        Write-Host "JAR is locked. Terminating running TimeLogger instances..."
        Stop-Process -Name java, javaw -Force -ErrorAction SilentlyContinue
        Start-Sleep -Milliseconds 500
        Remove-Item -Force $jarPath
    }
}

if ($hasJarExe) {
    & $jarPathExe --create --file $jarPath --main-class com.timelogger.Main -C $outDir .
    if ($LASTEXITCODE -ne 0) {
        throw "JAR packaging failed"
    }
} else {
    $stagingDir = Join-Path $root "out\jar-staging"
    if (Test-Path $stagingDir) {
        Remove-Item -Recurse -Force $stagingDir
    }

    Copy-Item -Path $outDir -Destination $stagingDir -Recurse
    $manifestDir = Join-Path $stagingDir "META-INF"
    New-Item -ItemType Directory -Path $manifestDir -Force | Out-Null

    $manifestContent = "Manifest-Version: 1.0`r`nMain-Class: com.timelogger.Main`r`n`r`n"
    [System.IO.File]::WriteAllText((Join-Path $manifestDir "MANIFEST.MF"), $manifestContent, [System.Text.Encoding]::ASCII)

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    $zip = [System.IO.Compression.ZipFile]::Open($jarPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        Get-ChildItem -Path $stagingDir -Recurse -File | ForEach-Object {
            $relative = $_.FullName.Substring($stagingDir.Length + 1).Replace('\', '/')
            $entry = $zip.CreateEntry($relative, [System.IO.Compression.CompressionLevel]::Optimal)
            $entryStream = $entry.Open()
            try {
                $fileStream = [System.IO.File]::OpenRead($_.FullName)
                try {
                    $fileStream.CopyTo($entryStream)
                } finally {
                    $fileStream.Dispose()
                }
            } finally {
                $entryStream.Dispose()
            }
        }
    } finally {
        $zip.Dispose()
    }

    Remove-Item -Recurse -Force $stagingDir
}

Write-Host "Build successful: $jarPath"
