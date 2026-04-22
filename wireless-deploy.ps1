# wireless-deploy.ps1 - Connect to phone over Wi-Fi (reusing last pairing if possible) and install debug APK

param(
    [switch]$NoBuild,
    [switch]$NoLaunch,
    [switch]$ForcePair
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

$adb = "$sdkRoot\platform-tools\adb.exe"
$projectDir = $PSScriptRoot
$package = "com.expensetracker.app"
$activity = "com.expensetracker.app.ui.MainActivity"
$stateFile = Join-Path $projectDir ".wireless-device.json"

if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb. Install platform-tools with sdkmanager."
    exit 1
}

function Invoke-Adb {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $adb @Arguments 2>&1
        return @($output | ForEach-Object {
            if ($_ -is [System.Management.Automation.ErrorRecord]) {
                $_.ToString()
            } else {
                "$_"
            }
        })
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Ensure-AdbServer {
    $null = Invoke-Adb start-server
}

function Test-WirelessDevice {
    param([string]$Endpoint)
    $devices = Invoke-Adb devices
    return ($devices | Select-String -Pattern "^$([regex]::Escape($Endpoint))\s+device$" -Quiet)
}

function Try-Connect {
    param([string]$Endpoint)
    Write-Host "      adb connect $Endpoint" -ForegroundColor DarkGray
    $out = Invoke-Adb connect $Endpoint
    Write-Host "      $out" -ForegroundColor DarkGray
    Start-Sleep -Milliseconds 800
    return (Test-WirelessDevice -Endpoint $Endpoint)
}

function Resolve-DebugApk {
    param([string]$ProjectDir)

    # AGP 8.x may write the APK to either of these locations depending on
    # version and cached intermediates. Check both and pick the newest.
    $candidates = @(
        @(
            (Join-Path $ProjectDir "app\build\outputs\apk\debug\app-debug.apk"),
            (Join-Path $ProjectDir "app\build\intermediates\apk\debug\app-debug.apk")
        ) |
            Where-Object { Test-Path $_ -PathType Leaf } |
            Sort-Object { (Get-Item $_).LastWriteTimeUtc } -Descending
    )

    if ($candidates.Count -eq 0) {
        return $null
    }
    return $candidates[0]
}

function Install-DebugApk {
    param(
        [string]$Endpoint,
        [string]$ProjectDir
    )

    $apkPath = Resolve-DebugApk -ProjectDir $ProjectDir
    if (-not $apkPath) {
        Write-Error "APK not found under app\build\outputs\apk\debug or app\build\intermediates\apk\debug after build."
        exit 1
    }
    if (-not (Test-Path $apkPath -PathType Leaf)) {
        Write-Error "Resolved APK path is not a file: $apkPath"
        exit 1
    }

    Write-Host "      Installing APK ($apkPath) over adb to $Endpoint ..." -ForegroundColor Yellow
    Invoke-Adb -s $Endpoint install --no-streaming -r -t $apkPath | Out-Host
    if ($LASTEXITCODE -ne 0) {
        Write-Error "adb install failed for $Endpoint."
        exit 1
    }
}

function Pair-NewDevice {
    Write-Host "`nPairing a new device." -ForegroundColor Cyan
    Write-Host "  On your phone: Settings -> Developer options -> Wireless debugging -> 'Pair device with pairing code'" -ForegroundColor Yellow
    Write-Host "  Leave that screen open while you enter the values below.`n" -ForegroundColor Yellow

    $pairHost = Read-Host "  Pair IP address (from phone)"
    $pairPort = Read-Host "  Pair port (6-digit, from phone)"
    $pairCode = Read-Host "  Pairing code (6-digit)"

    $pairEndpoint = "${pairHost}:${pairPort}"
    Write-Host "`n  Pairing with $pairEndpoint ..." -ForegroundColor Cyan
    $pairOut = Invoke-Adb pair $pairEndpoint $pairCode
    Write-Host "  $pairOut" -ForegroundColor DarkGray

    if ($pairOut -notmatch "Successfully paired") {
        Write-Error "Pairing failed. Double-check the code and port (pair port differs from the connect port)."
        exit 1
    }

    Write-Host "`n  On the same Wireless debugging screen, note the 'IP address & Port' (top section)." -ForegroundColor Yellow
    $connectHost = Read-Host "  Connect IP address"
    $connectPort = Read-Host "  Connect port (5-digit)"
    $connectEndpoint = "${connectHost}:${connectPort}"

    if (-not (Try-Connect -Endpoint $connectEndpoint)) {
        Write-Error "Paired but could not connect to $connectEndpoint."
        exit 1
    }

    @{ endpoint = $connectEndpoint } | ConvertTo-Json | Set-Content -Path $stateFile -Encoding UTF8
    Write-Host "      Saved device to $stateFile" -ForegroundColor Green
    return $connectEndpoint
}

Ensure-AdbServer

Write-Host "`n[1/3] Checking wireless connection..." -ForegroundColor Cyan

$endpoint = $null
$connected = $false

if (-not $ForcePair -and (Test-Path $stateFile)) {
    try {
        $saved = Get-Content $stateFile -Raw | ConvertFrom-Json
        $endpoint = $saved.endpoint
    } catch {
        $endpoint = $null
    }
}

if ($endpoint) {
    Write-Host "      Found saved device: $endpoint" -ForegroundColor Green
    if (Test-WirelessDevice -Endpoint $endpoint) {
        Write-Host "      Already connected." -ForegroundColor Green
        $connected = $true
    } elseif (Try-Connect -Endpoint $endpoint) {
        Write-Host "      Reconnected." -ForegroundColor Green
        $connected = $true
    } else {
        Write-Host "      Saved device unreachable. Falling back to pairing." -ForegroundColor Yellow
    }
}

if (-not $connected) {
    $endpoint = Pair-NewDevice
    $connected = $true
}

if (-not $NoBuild) {
    Write-Host "`n[2/3] Building and installing debug APK..." -ForegroundColor Cyan
    Set-Location $projectDir
    & ".\gradlew.bat" `
        "-Pandroid.injected.build.abi=arm64-v8a" `
        assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Gradle assembleDebug failed."
        exit 1
    }
    if (-not (Test-WirelessDevice -Endpoint $endpoint) -and -not (Try-Connect -Endpoint $endpoint)) {
        Write-Error "Wireless device disconnected before install."
        exit 1
    }
    Install-DebugApk -Endpoint $endpoint -ProjectDir $projectDir
    Write-Host "      Debug app installed on $endpoint." -ForegroundColor Green
} else {
    Write-Host "`n[2/3] Skipping build." -ForegroundColor Yellow
}

Write-Host "`n[3/3] Launching app..." -ForegroundColor Cyan
if ($NoLaunch) {
    Write-Host "      Skipped (-NoLaunch)." -ForegroundColor Yellow
} else {
    Invoke-Adb -s $endpoint shell am force-stop $package | Out-Null
    Invoke-Adb -s $endpoint shell am start -n "${package}/${activity}" | Out-Null
    Write-Host "      Launched on $endpoint." -ForegroundColor Green
}

Write-Host "`nDone." -ForegroundColor Green
Write-Host "Examples:" -ForegroundColor DarkGray
Write-Host "  .\wireless-deploy.ps1              # connect (or pair), build, install, launch" -ForegroundColor DarkGray
Write-Host "  .\wireless-deploy.ps1 -NoBuild     # reconnect and relaunch only" -ForegroundColor DarkGray
Write-Host "  .\wireless-deploy.ps1 -ForcePair   # ignore cached device and pair fresh" -ForegroundColor DarkGray
