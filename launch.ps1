# launch.ps1 - Build/debug helper for emulator install and fast relaunch loops

param(
    [switch]$NoBuild,
    [switch]$NoInstall,
    [switch]$RelaunchOnly
)

$ErrorActionPreference = "Stop"

$sdkRoot = if ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} elseif (Test-Path "C:\android-sdk") {
    "C:\android-sdk"
} else {
    "$env:LOCALAPPDATA\Android\Sdk"
}

$emulator = "$sdkRoot\emulator\emulator.exe"
$adb = "$sdkRoot\platform-tools\adb.exe"
$projectDir = $PSScriptRoot
$package = "com.expensetracker.app"
$activity = "com.expensetracker.app.ui.MainActivity"

if ($RelaunchOnly) {
    $NoBuild = $true
    $NoInstall = $true
}

if (-not (Test-Path $emulator)) {
    Write-Error "Emulator not found at $emulator. Install it with sdkmanager."
    exit 1
}

if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb. Install platform-tools with sdkmanager."
    exit 1
}

Write-Host "`n[1/4] Looking for AVDs..." -ForegroundColor Cyan
$avdList = @(& $emulator -list-avds 2>&1 | Where-Object { $_ -match '\S' -and $_ -notmatch '^INFO' })
if ($avdList.Count -eq 0) {
    Write-Error "No AVDs found. Create one first with avdmanager."
    exit 1
}

$avd = "$($avdList[0])".Trim()
Write-Host "      Using AVD: $avd" -ForegroundColor Green

Write-Host "`n[2/4] Checking emulator..." -ForegroundColor Cyan
$running = & $adb devices | Select-String "emulator"
if (-not $running) {
    Write-Host "      Starting emulator..." -ForegroundColor Yellow
    Start-Process $emulator -ArgumentList "-avd `"$avd`"" -WindowStyle Hidden

    $timeout = 120
    $elapsed = 0
    do {
        Start-Sleep -Seconds 3
        $elapsed += 3
        $online = & $adb devices | Select-String "emulator.*device$"
    } while (-not $online -and $elapsed -lt $timeout)

    if (-not $online) {
        Write-Error "Emulator did not come online within $timeout seconds."
        exit 1
    }

    Write-Host "      Waiting for boot to complete..." -ForegroundColor Yellow
    & $adb wait-for-device shell "while [[ -z \`$(getprop sys.boot_completed) ]]; do sleep 1; done"
    Write-Host "      Emulator ready." -ForegroundColor Green
} else {
    Write-Host "      Emulator already running." -ForegroundColor Green
}

if (-not $NoBuild) {
    Write-Host "`n[3/4] Building and installing debug app..." -ForegroundColor Cyan
    Set-Location $projectDir
    & ".\gradlew.bat" installDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Gradle installDebug failed."
        exit 1
    }
    Write-Host "      Debug app updated on device." -ForegroundColor Green
} else {
    Write-Host "`n[3/4] Skipping build." -ForegroundColor Yellow
}

Write-Host "`n[4/4] Launching app..." -ForegroundColor Cyan
if ($NoInstall) {
    Write-Host "      Reusing existing installed app." -ForegroundColor Yellow
}

& $adb shell am force-stop $package | Out-Null
& $adb shell am start -n "${package}/${activity}" | Out-Null

Write-Host "`nDone. App should be open on the emulator." -ForegroundColor Green
Write-Host "Examples:" -ForegroundColor DarkGray
Write-Host "  .\launch.ps1               # build, install, relaunch" -ForegroundColor DarkGray
Write-Host "  .\launch.ps1 -NoBuild      # relaunch the installed app without rebuilding" -ForegroundColor DarkGray
Write-Host "  .\launch.ps1 -RelaunchOnly # alias for -NoBuild -NoInstall" -ForegroundColor DarkGray
