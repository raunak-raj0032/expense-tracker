# launch.ps1 — Build debug APK, start emulator, install & launch app

$ErrorActionPreference = "Stop"

$sdkRoot   = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
             elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
             elseif (Test-Path "C:\android-sdk") { "C:\android-sdk" }
             else { "$env:LOCALAPPDATA\Android\Sdk" }
$emulator  = "$sdkRoot\emulator\emulator.exe"
$adb       = "$sdkRoot\platform-tools\adb.exe"
$projectDir= $PSScriptRoot
$package   = "com.expensetracker.app"

# ── 0. Sanity-check SDK layout ───────────────────────────────────────────────
if (-not (Test-Path $emulator)) {
    Write-Error "Emulator not found at $emulator. Install it via: `"$sdkRoot\cmdline-tools\latest\bin\sdkmanager.bat`" `"emulator`" `"platform-tools`""
    exit 1
}
if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb. Install platform-tools via sdkmanager."
    exit 1
}

# ── 1. Pick first available AVD ──────────────────────────────────────────────
Write-Host "`n[1/4] Looking for AVDs..." -ForegroundColor Cyan
$avdList = @(& $emulator -list-avds 2>&1 | Where-Object { $_ -match '\S' -and $_ -notmatch '^INFO' })
if ($avdList.Count -eq 0) {
    Write-Error "No AVDs found. Create one with: `"$sdkRoot\cmdline-tools\latest\bin\avdmanager.bat`" create avd -n pixel -k `"system-images;android-34;google_apis;x86_64`" -d pixel"
    exit 1
}
$avd = "$($avdList[0])".Trim()
Write-Host "      Using AVD: $avd" -ForegroundColor Green

# ── 2. Start emulator if not already running ─────────────────────────────────
Write-Host "`n[2/4] Checking emulator..." -ForegroundColor Cyan
$running = & $adb devices | Select-String "emulator"
if (-not $running) {
    Write-Host "      Starting emulator (this may take ~30 seconds)..." -ForegroundColor Yellow
    Start-Process $emulator -ArgumentList "-avd `"$avd`"" -WindowStyle Hidden
    # Wait until adb sees the device as online
    $timeout = 120
    $elapsed = 0
    do {
        Start-Sleep -Seconds 3
        $elapsed += 3
        $online = & $adb devices | Select-String "emulator.*device$"
    } while (-not $online -and $elapsed -lt $timeout)
    if (-not $online) { Write-Error "Emulator did not come online within $timeout seconds."; exit 1 }
    # Wait for boot to complete
    Write-Host "      Waiting for boot to complete..." -ForegroundColor Yellow
    & $adb wait-for-device shell "while [[ -z \`$(getprop sys.boot_completed) ]]; do sleep 1; done"
    Write-Host "      Emulator ready." -ForegroundColor Green
} else {
    Write-Host "      Emulator already running." -ForegroundColor Green
}

# ── 3. Build debug APK ───────────────────────────────────────────────────────
Write-Host "`n[3/4] Building debug APK..." -ForegroundColor Cyan
Set-Location $projectDir
& ".\gradlew.bat" assembleDebug
if ($LASTEXITCODE -ne 0) { Write-Error "Gradle build failed."; exit 1 }
Write-Host "      Build successful." -ForegroundColor Green

# ── 4. Install & launch ──────────────────────────────────────────────────────
Write-Host "`n[4/4] Installing & launching app..." -ForegroundColor Cyan
$apk = Get-ChildItem -Path "$projectDir\app\build\outputs\apk\debug" -Filter "*.apk" | Select-Object -First 1
& $adb install -r $apk.FullName
& $adb shell monkey -p $package -c android.intent.category.LAUNCHER 1

Write-Host "`nDone! App should be open on the emulator." -ForegroundColor Green
