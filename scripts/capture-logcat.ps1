#Requires -Version 5.1
<#
.SYNOPSIS
    Захватывает adb logcat для com.example.voicemind и пишет в файл в проекте.
    Cascade/Windsurf может затем прочитать этот файл самостоятельно.

.PARAMETER DurationSec
    Сколько секунд собирать логи (default: 10). Для багов запуска ставь 15-30.

.PARAMETER ClearBuffer
    Очистить буфер logcat перед началом.

.PARAMETER Lines
    Если указан — вместо ожидания дампит последние N строк.

.EXAMPLE
    .\scripts\capture-logcat.ps1 -DurationSec 20
    .\scripts\capture-logcat.ps1 -Lines 500
#>
param(
    [int] $DurationSec = 10,
    [switch] $ClearBuffer,
    [int] $Lines = 0
)

$pkg = "com.example.voicemind"
$outDir = Join-Path (Join-Path $PSScriptRoot "..") "logs"
$outFile = Join-Path $outDir "voicemind-logcat.txt"

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# Проверяем adb
$cmd = Get-Command adb -ErrorAction SilentlyContinue
$adb = if ($cmd) { $cmd.Source } else { $null }
if (-not $adb) {
    $adb = Join-Path $env:LOCALAPPDATA "Android" "Sdk" "platform-tools" "adb.exe"
    if (-not (Test-Path $adb)) {
        Write-Error "adb не найден. Установи Android SDK Platform Tools или добавь adb в PATH."
        exit 1
    }
}

Write-Host "adb: $adb" -ForegroundColor DarkGray
Write-Host "Package: $pkg" -ForegroundColor DarkGray
Write-Host "Output : $outFile" -ForegroundColor DarkGray

if ($ClearBuffer) {
    Write-Host "Clearing logcat buffer..." -ForegroundColor Yellow
    & $adb logcat -c
}

$header = @"
=== VoiceMind Logcat ===
Timestamp: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Package  : $pkg
Device   : $(& $adb shell getprop ro.product.model 2>$null)
Android  : $(& $adb shell getprop ro.build.version.release 2>$null)
========================

"@
Set-Content -Path $outFile -Value $header -Encoding UTF8 -NoNewline

if ($Lines -gt 0) {
    Write-Host "Dumping last $Lines lines (filtered by package)..." -ForegroundColor Cyan
    & $adb logcat -d -t $Lines -v threadtime | Select-String $pkg | Add-Content -Path $outFile -Encoding UTF8
    Write-Host "Done. Saved to $outFile" -ForegroundColor Green
} else {
    Write-Host "Collecting logs for $DurationSec seconds... Press Ctrl+C to stop early." -ForegroundColor Cyan
    $proc = Start-Process -FilePath $adb -ArgumentList "logcat","-v","threadtime","*:D" -RedirectStandardOutput (Join-Path $env:TEMP "vm-logcat-pipe.txt") -WindowStyle Hidden -PassThru

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    Start-Sleep -Milliseconds 500

    while ($sw.Elapsed.TotalSeconds -lt $DurationSec -and -not $proc.HasExited) {
        Start-Sleep -Milliseconds 500
    }

    if (-not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }

    Get-Content -Path (Join-Path $env:TEMP "vm-logcat-pipe.txt") -ErrorAction SilentlyContinue |
        Select-String $pkg |
        Add-Content -Path $outFile -Encoding UTF8

    Remove-Item -Path (Join-Path $env:TEMP "vm-logcat-pipe.txt") -ErrorAction SilentlyContinue
    Write-Host "Done. Saved to $outFile ($DurationSec sec)" -ForegroundColor Green
}

Write-Host ""
Write-Host "Cascade теперь может прочитать логи из:" -ForegroundColor Green
Write-Host "  logs/voicemind-logcat.txt" -ForegroundColor White
