$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# Bring Ollama up so the Android app can reach it from anywhere as long as
# this PC + Ollama are running:
#   - Binds Ollama to 0.0.0.0:11434 so any interface (LAN, tunnel) can reach it
#   - Ensures a Windows Firewall inbound rule exists for port 11434 (Private)
#   - If `cloudflared` is on PATH, starts a free quick-tunnel and prints the
#     https://*.trycloudflare.com URL - paste that into the app to reach the
#     PC from mobile data or any other Wi-Fi.
# ---------------------------------------------------------------------------

$ollama = "C:\Users\HP\AppData\Local\Programs\Ollama\ollama.exe"
if (-not (Test-Path -LiteralPath $ollama)) {
    throw "Ollama executable not found at $ollama"
}

# --- Wi-Fi IP for LAN access ------------------------------------------------
$wifiIp = $null
try {
    $wifiIp = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction Stop |
        Where-Object { $_.InterfaceAlias -eq "Wi-Fi" -and $_.IPAddress -notlike "169.254.*" } |
        Select-Object -First 1 -ExpandProperty IPAddress
} catch {
    Write-Warning "Could not enumerate IPv4 addresses: $($_.Exception.Message)"
}
if (-not $wifiIp) {
    Write-Warning "No Wi-Fi IPv4 address found. LAN URL will be unavailable; tunnel still works if cloudflared is installed."
}

# --- Stop existing Ollama processes ----------------------------------------
Write-Host "Stopping any existing Ollama processes..."
Get-Process -Name "ollama","ollama app" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# --- Firewall rule (best-effort; needs admin) ------------------------------
$ruleName = "Ollama 11434 (LAN)"
$rule = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if (-not $rule) {
    try {
        New-NetFirewallRule -DisplayName $ruleName `
            -Direction Inbound -Action Allow `
            -Protocol TCP -LocalPort 11434 `
            -Profile Private -Enabled True `
            -ErrorAction Stop | Out-Null
        Write-Host "Firewall rule '$ruleName' created (Private profile, TCP 11434)."
    } catch {
        Write-Warning "Could not create firewall rule (run PowerShell as Administrator to fix). LAN access may be blocked. Detail: $($_.Exception.Message)"
    }
} else {
    Write-Host "Firewall rule '$ruleName' already exists."
}

# --- Start Ollama on all interfaces ----------------------------------------
$env:OLLAMA_HOST = "0.0.0.0:11434"
Write-Host "Starting Ollama on $($env:OLLAMA_HOST)..."
Start-Process -FilePath $ollama -ArgumentList "serve" -WindowStyle Hidden

# Give it a moment to bind the socket before tunneling.
Start-Sleep -Seconds 2

# --- Optional Cloudflare quick tunnel --------------------------------------
$tunnelUrl = $null
$cloudflared = (Get-Command cloudflared -ErrorAction SilentlyContinue)
if ($cloudflared) {
    try {
        Get-CimInstance Win32_Process -Filter "name = 'cloudflared.exe'" -ErrorAction Stop |
            Where-Object { $_.CommandLine -like "*tunnel*--url*http://localhost:11434*" } |
            ForEach-Object {
                Write-Host "Stopping existing Ollama cloudflared tunnel process $($_.ProcessId)..."
                Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
            }
    } catch {
        Write-Warning "Could not check for existing cloudflared tunnel processes: $($_.Exception.Message)"
    }

    $runId = "{0}-{1}" -f $PID, (Get-Date -Format "yyyyMMddHHmmss")
    $logFile = Join-Path $env:TEMP "cloudflared-ollama-$runId.log"

    Write-Host "cloudflared detected; starting quick tunnel..."
    Start-Process -FilePath $cloudflared.Source `
        -ArgumentList "tunnel","--url","http://localhost:11434" `
        -WindowStyle Hidden `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError "$logFile.err"

    # Cloudflared logs the public URL within ~5s. Poll for up to 25s.
    for ($i = 0; $i -lt 25; $i++) {
        Start-Sleep -Seconds 1
        foreach ($f in @($logFile, "$logFile.err")) {
            if (Test-Path -LiteralPath $f) {
                $match = Select-String -Path $f -Pattern "https://[a-zA-Z0-9-]+\.trycloudflare\.com" -ErrorAction SilentlyContinue |
                    Select-Object -First 1
                if ($match) {
                    $tunnelUrl = ($match.Matches | Select-Object -First 1).Value
                    break
                }
            }
        }
        if ($tunnelUrl) { break }
    }
    if (-not $tunnelUrl) {
        Write-Warning "cloudflared started but no tunnel URL was reported in 25s. Check $logFile / $logFile.err for details."
    }
} else {
    Write-Host "cloudflared not on PATH - skipping public tunnel. Install with 'winget install --id Cloudflare.cloudflared' to enable off-LAN access."
}

# --- Print URLs to paste into the app ---------------------------------------
Write-Host ""
Write-Host "================== Paste one of these into Settings > Llama AI Insights =================="
if ($wifiIp) {
    Write-Host ("  LAN (same Wi-Fi): http://{0}:11434" -f $wifiIp)
}
if ($tunnelUrl) {
    Write-Host ("  Anywhere (mobile data / other Wi-Fi): {0}" -f $tunnelUrl)
}
if (-not $wifiIp -and -not $tunnelUrl) {
    Write-Warning "No usable URL produced. Check Wi-Fi connectivity and cloudflared installation."
}
Write-Host "==========================================================================================="
Write-Host ""
Write-Host "Tip: keep this PowerShell window open. Closing it stops Ollama and the tunnel."
