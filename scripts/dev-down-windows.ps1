$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeDir = Join-Path $scriptDir ".runtime/windows"
$stateFile = Join-Path $runtimeDir "state.json"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

if (-not (Test-Path $stateFile)) {
    Write-Host "No Windows dev runtime state found. Nothing to stop."
    exit 0
}

$state = Get-Content -Raw -Path $stateFile | ConvertFrom-Json

Write-Step "Stopping backend and admin-web"
foreach ($procInfo in @($state.processes)) {
    $proc = Get-Process -Id $procInfo.pid -ErrorAction SilentlyContinue
    if ($null -eq $proc) {
        Write-Host "$($procInfo.name) already stopped."
        continue
    }

    Write-Host "Stopping $($procInfo.name) (PID=$($procInfo.pid))"
    & taskkill /PID $procInfo.pid /T /F | Out-Null
}

Write-Step "Stopping services started by dev-up-windows.ps1"
foreach ($serviceInfo in @($state.services)) {
    if (-not $serviceInfo.startedByScript) {
        continue
    }

    $service = Get-Service -Name $serviceInfo.name -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        continue
    }

    if ($service.Status -eq "Running") {
        Write-Host "Stopping service $($serviceInfo.name)"
        Stop-Service -Name $serviceInfo.name
    }
}

Remove-Item -Force $stateFile

Write-Host ""
Write-Host "Windows local dev stopped." -ForegroundColor Green
