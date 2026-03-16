param(
    [int]$MySqlPort = 3306,
    [int]$RedisPort = 6379,
    [string]$ApiBaseUrl = "http://127.0.0.1:8080/api",
    [switch]$SkipNpmInstall
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$backendDir = Join-Path $rootDir "backend"
$adminDir = Join-Path $rootDir "admin-web"
$miniappDir = Join-Path $rootDir "miniapp"
$runtimeDir = Join-Path $scriptDir ".runtime/windows"
$stateFile = Join-Path $runtimeDir "state.json"
$backendLog = Join-Path $runtimeDir "backend.log"
$adminLog = Join-Path $runtimeDir "admin-web.log"

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Refresh-EnvironmentFromRegistry {
    $machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $parts = @()
    if (-not [string]::IsNullOrWhiteSpace($machinePath)) {
        $parts += $machinePath
    }
    if (-not [string]::IsNullOrWhiteSpace($userPath)) {
        $parts += $userPath
    }
    $env:Path = $parts -join ";"

    foreach ($name in @("JAVA_HOME", "MAVEN_HOME", "M2_HOME")) {
        $machineValue = [Environment]::GetEnvironmentVariable($name, "Machine")
        if (-not [string]::IsNullOrWhiteSpace($machineValue)) {
            Set-Item -Path ("Env:" + $name) -Value $machineValue
        }
    }
}

function Test-CommandExists {
    param([string]$CommandName)
    return $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Get-MySqlCliPath {
    $command = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.3\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.2\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.1\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    )

    foreach ($path in $candidates) {
        if (Test-Path $path) {
            return $path
        }
    }

    return $null
}

function Test-MySqlLogin {
    param(
        [string]$Username,
        [string]$Password
    )

    $mysqlCli = Get-MySqlCliPath
    if ([string]::IsNullOrWhiteSpace($mysqlCli)) {
        return $false
    }

    & $mysqlCli --protocol=TCP -h localhost -P $MySqlPort -u $Username "-p$Password" --execute="SELECT 1;" *> $null
    return $LASTEXITCODE -eq 0
}

function Test-PortOpen {
    param([int]$Port)

    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $async = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
        $connected = $async.AsyncWaitHandle.WaitOne(1000, $false)
        if (-not $connected) {
            $client.Close()
            return $false
        }
        $client.EndConnect($async)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

function Wait-Port {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortOpen -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Get-JavaMajorVersion {
    $javaVersionOutput = & cmd.exe /c "java -version 2>&1"
    $joined = ($javaVersionOutput | Out-String).Trim()
    if ($joined -match 'version "(\d+)(\.[^"]*)?"') {
        return [int]$Matches[1]
    }
    return $null
}

function Assert-Prerequisites {
    Write-Step "Checking required commands"
    Refresh-EnvironmentFromRegistry

    $missing = @()
    foreach ($cmd in @("java", "mvn", "node", "npm")) {
        if (-not (Test-CommandExists -CommandName $cmd)) {
            $missing += $cmd
        }
    }

    if ($missing.Count -gt 0) {
        throw "Missing required commands: $($missing -join ', '). See docs/03-Windows-本机启动说明.md for installation steps."
    }

    $javaMajor = Get-JavaMajorVersion
    if ($javaMajor -ne 17) {
        throw "Detected Java version $javaMajor. This project requires Java 17."
    }
}

function Start-ServiceIfAvailable {
    param(
        [string[]]$CandidateNames,
        [string]$DisplayLabel
    )

    foreach ($name in $CandidateNames) {
        $service = Get-Service -Name $name -ErrorAction SilentlyContinue
        if ($null -eq $service) {
            continue
        }

        if ($service.Status -ne "Running") {
            Write-Host "Starting $DisplayLabel service: $name"
            Start-Service -Name $name
            return @{
                name = $name
                startedByScript = $true
            }
        }

        Write-Host "$DisplayLabel service already running: $name"
        return @{
            name = $name
            startedByScript = $false
        }
    }

    return $null
}

function Ensure-Infrastructure {
    Write-Step "Checking MySQL and Redis"

    $startedServices = @()

    if (-not (Test-PortOpen -Port $MySqlPort)) {
        $mysqlState = Start-ServiceIfAvailable -CandidateNames @("MySQL80", "MySQL", "mysql") -DisplayLabel "MySQL"
        if ($null -ne $mysqlState) {
            $startedServices += $mysqlState
        }
    }

    if (-not (Wait-Port -Port $MySqlPort -TimeoutSeconds 20)) {
        throw "MySQL is not reachable on 127.0.0.1:$MySqlPort. Install/start MySQL first."
    }

    if (-not (Test-PortOpen -Port $RedisPort)) {
        $redisState = Start-ServiceIfAvailable -CandidateNames @("Memurai", "Redis", "RedisService") -DisplayLabel "Redis"
        if ($null -ne $redisState) {
            $startedServices += $redisState
        }
    }

    if (-not (Wait-Port -Port $RedisPort -TimeoutSeconds 20)) {
        throw "Redis is not reachable on 127.0.0.1:$RedisPort. Install/start Memurai or another Redis-compatible service first."
    }

    return $startedServices
}

function Assert-DatabaseReady {
    if (Test-MySqlLogin -Username "campus" -Password "campus123") {
        return
    }

    $initScript = Join-Path $scriptDir "init-dev-mysql.ps1"
    throw "MySQL is reachable on 127.0.0.1:$MySqlPort, but the dev account campus/campus123 cannot log in. Run powershell -ExecutionPolicy Bypass -File $initScript first."
}

function Ensure-MiniappConfig {
    Write-Step "Preparing miniapp/config.js"

    $configFile = Join-Path $miniappDir "config.js"
    $content = @"
module.exports = {
  apiBaseUrl: '$ApiBaseUrl',
  cloudEnvId: 'cloud1-8g7ph5n7f6515ead',
  cloudService: '',
  tencentMapKey: 'QN6BZ-OUWCH-5TODB-WUDD6-XQAXV-U7FKQ',
  mapSubKey: '',
  simulationEnabled: true
}
"@
    Set-Content -Path $configFile -Value $content -Encoding UTF8
    Write-Host "miniapp apiBaseUrl => $ApiBaseUrl"
}

function Ensure-AdminDependencies {
    if ($SkipNpmInstall) {
        return
    }

    if (Test-Path (Join-Path $adminDir "node_modules")) {
        return
    }

    Write-Step "Installing admin-web dependencies"
    Push-Location $adminDir
    try {
        & npm install
    } finally {
        Pop-Location
    }
}

function Read-State {
    if (-not (Test-Path $stateFile)) {
        return $null
    }

    return Get-Content -Raw -Path $stateFile | ConvertFrom-Json
}

function Assert-NotRunning {
    $state = Read-State
    if ($null -eq $state) {
        return
    }

    foreach ($procInfo in @($state.processes)) {
        $proc = Get-Process -Id $procInfo.pid -ErrorAction SilentlyContinue
        if ($null -ne $proc) {
            throw "A previous Windows dev session is still running. Execute scripts/dev-down-windows.ps1 first."
        }
    }
}

function Start-ManagedProcess {
    param(
        [string]$Name,
        [string]$CommandLine,
        [string]$LogFile
    )

    if (Test-Path $LogFile) {
        Remove-Item -Force $LogFile
    }

    $fullCommand = "$CommandLine >> `"$LogFile`" 2>&1"
    $process = Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c $fullCommand" `
        -WorkingDirectory $rootDir `
        -PassThru

    Start-Sleep -Seconds 2
    if ($process.HasExited) {
        $logExcerpt = ""
        if (Test-Path $LogFile) {
            $logExcerpt = (Get-Content -Path $LogFile -Tail 20 | Out-String).Trim()
        }
        throw "$Name failed to start. Log: $LogFile`n$logExcerpt"
    }

    Write-Host "$Name started, PID=$($process.Id), log=$LogFile"
    return @{
        name = $Name
        pid = $process.Id
        logFile = $LogFile
    }
}

Assert-Prerequisites
Assert-NotRunning
$startedServices = Ensure-Infrastructure
Assert-DatabaseReady
Ensure-MiniappConfig
Ensure-AdminDependencies

Write-Step "Starting backend and admin-web"

$backendCommand = "set `"SPRING_PROFILES_ACTIVE=dev`" && set `"SPRING_DATASOURCE_URL=jdbc:mysql://localhost:$MySqlPort/campus_run?useUnicode=true^&characterEncoding=UTF-8^&serverTimezone=Asia/Shanghai`" && set `"SPRING_DATASOURCE_USERNAME=campus`" && set `"SPRING_DATASOURCE_PASSWORD=campus123`" && set `"SPRING_DATA_REDIS_HOST=localhost`" && set `"SPRING_DATA_REDIS_PORT=$RedisPort`" && set `"CAMPUSRUN_SWAGGER_ENABLED=true`" && set `"CAMPUSRUN_RUN_ALLOW_SIMULATED_RUNS=true`" && cd /d `"$backendDir`" && mvn spring-boot:run"
$adminCommand = "cd /d `"$adminDir`" && npm run dev -- --host 127.0.0.1"

$backendProcess = Start-ManagedProcess -Name "backend" -CommandLine $backendCommand -LogFile $backendLog
$adminProcess = Start-ManagedProcess -Name "admin-web" -CommandLine $adminCommand -LogFile $adminLog

$state = @{
    createdAt = (Get-Date).ToString("s")
    services = @($startedServices)
    processes = @($backendProcess, $adminProcess)
}
$state | ConvertTo-Json -Depth 4 | Set-Content -Path $stateFile -Encoding UTF8

Write-Host ""
Write-Host "Windows local dev started." -ForegroundColor Green
Write-Host "Admin:   http://127.0.0.1:5173"
Write-Host "Swagger: http://127.0.0.1:8080/swagger-ui.html"
Write-Host "Health:  http://127.0.0.1:8080/actuator/health"
Write-Host "Miniapp config: $miniappDir\config.js"
Write-Host ""
Write-Host "Stop with: powershell -ExecutionPolicy Bypass -File scripts/dev-down-windows.ps1"
