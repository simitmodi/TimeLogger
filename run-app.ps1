$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Join-Path $root "dist\time-logger.jar"

if (-not (Test-Path $jarPath)) {
    throw "JAR not found. Run .\\build-jar.ps1 first."
}

Set-Location $root
java -jar $jarPath
