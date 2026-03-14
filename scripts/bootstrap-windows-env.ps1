param(
    [switch]$IncludeWeixinDevTools,
    [switch]$SkipMySqlConfiguration,
    [switch]$SkipMemuraiConfiguration,
    [int]$MySqlPort = 3306,
    [string]$MySqlRootPassword,
    [string]$CampusDatabase = "campus_run",
    [string]$CampusUser = "campus",
    [string]$CampusUserPassword = "campus123"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeDir = Join-Path $scriptDir ".runtime/windows"
$bootstrapLog = Join-Path $runtimeDir "bootstrap.log"

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
Start-Transcript -Path $bootstrapLog -Append | Out-Null

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Info {
    param([string]$Message)
    Write-Host "  $Message"
}

function Test-IsAdministrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Assert-Administrator {
    if (-not (Test-IsAdministrator)) {
        throw "Please run this script in an elevated PowerShell window (Run as Administrator)."
    }
}

function Assert-WingetAvailable {
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        throw "winget was not found. Install or update App Installer first, then rerun this script."
    }
}

function Assert-PowerShellVersion {
    if ($PSVersionTable.PSVersion.Major -lt 5) {
        throw "PowerShell 5.1 or newer is required."
    }
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
}

function Test-CommandExists {
    param([string]$Name)
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Get-JavaMajorVersion {
    if (-not (Test-CommandExists -Name "java")) {
        return $null
    }

    $output = & java -version 2>&1 | Out-String
    if ($output -match 'version "(\d+)(\.[^"]*)?"') {
        return [int]$Matches[1]
    }

    return $null
}

function Get-NodeMajorVersion {
    if (-not (Test-CommandExists -Name "node")) {
        return $null
    }

    $version = (& node -v 2>$null).Trim()
    if ($version -match '^v(\d+)') {
        return [int]$Matches[1]
    }

    return $null
}

function Get-InstalledPackageLine {
    param([string]$PackageId)

    $output = & winget list --exact --id $PackageId --accept-source-agreements 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) {
        return $null
    }

    if ($output -match [Regex]::Escape($PackageId)) {
        return $output.Trim()
    }

    return $null
}

function Install-WingetPackage {
    param(
        [string]$PackageId,
        [string]$DisplayName
    )

    Write-Step "Installing $DisplayName"
    & winget install --exact --id $PackageId --source winget --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) {
        throw "winget failed while installing $DisplayName ($PackageId)."
    }

    Refresh-EnvironmentFromRegistry
}

function Ensure-WingetPackage {
    param(
        [string]$PackageId,
        [string]$DisplayName,
        [scriptblock]$Satisfied
    )

    $satisfied = & $Satisfied
    if ($satisfied) {
        Write-Info "$DisplayName already looks ready."
        return
    }

    Install-WingetPackage -PackageId $PackageId -DisplayName $DisplayName
}

function Find-LatestTemurin17Home {
    $roots = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Adoptium",
        "C:\Program Files\Java"
    )

    foreach ($root in $roots) {
        if (-not (Test-Path $root)) {
            continue
        }

        $candidate = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^jdk-?17' } |
            Sort-Object Name -Descending |
            Select-Object -First 1

        if ($null -ne $candidate) {
            return $candidate.FullName
        }
    }

    return $null
}

function Ensure-JavaHome {
    $javaHome = Find-LatestTemurin17Home
    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        Write-Info "JAVA_HOME was not updated automatically. Skipping JAVA_HOME fixup."
        return
    }

    $currentJavaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
    if ($currentJavaHome -ne $javaHome) {
        Write-Step "Configuring JAVA_HOME"
        [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Machine")
    }

    Refresh-EnvironmentFromRegistry
    if (-not ($env:Path -split ';' | Where-Object { $_ -eq (Join-Path $javaHome "bin") })) {
        $newPath = "{0};{1}" -f (Join-Path $javaHome "bin"), [Environment]::GetEnvironmentVariable("Path", "Machine")
        [Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
        Refresh-EnvironmentFromRegistry
    }
}

function Wait-Port {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $reachable = Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -WarningAction SilentlyContinue -InformationLevel Quiet
        if ($reachable) {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function ConvertTo-PlainText {
    param([Security.SecureString]$SecureValue)

    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Resolve-MySqlRootPassword {
    if (-not [string]::IsNullOrWhiteSpace($script:MySqlRootPassword)) {
        return $script:MySqlRootPassword
    }

    $secure = Read-Host "Enter a password to assign/use for the local MySQL root account" -AsSecureString
    $script:MySqlRootPassword = ConvertTo-PlainText -SecureValue $secure
    return $script:MySqlRootPassword
}

function Get-MySqlInstallerConsolePath {
    $paths = @(
        "C:\Program Files (x86)\MySQL\MySQL Installer for Windows\MySQLInstallerConsole.exe",
        "C:\Program Files\MySQL\MySQL Installer for Windows\MySQLInstallerConsole.exe"
    )

    foreach ($path in $paths) {
        if (Test-Path $path) {
            return $path
        }
    }

    return $null
}

function Get-MySqlCliPath {
    $matches = Get-ChildItem -Path "C:\Program Files\MySQL" -Filter mysql.exe -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match 'MySQL Server .*\\bin\\mysql\.exe$' } |
        Sort-Object FullName -Descending

    return ($matches | Select-Object -First 1).FullName
}

function Ensure-MySqlServer {
    if (Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue) {
        Write-Info "MySQL80 service already exists."
        return
    }

    $installerConsole = Get-MySqlInstallerConsolePath
    if ([string]::IsNullOrWhiteSpace($installerConsole)) {
        throw "MySQL Installer Console was not found after winget install. Check Oracle.MySQL installation."
    }

    $rootPassword = Resolve-MySqlRootPassword

    Write-Step "Installing and configuring MySQL Server"
    & $installerConsole install "server:*:type=config;open_win_firewall=true;tcp_ip=true;port=$MySqlPort;root_passwd=$rootPassword" --auto-handle-prereqs --only-ga-products --silent

    if ($LASTEXITCODE -ne 0) {
        throw "MySQL Installer Console failed to configure MySQL Server. Open MySQL Installer manually and complete server setup."
    }
}

function Ensure-MySqlRunning {
    $service = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        throw "MySQL80 service was not found."
    }

    if ($service.Status -ne "Running") {
        Write-Step "Starting MySQL80 service"
        Start-Service -Name "MySQL80"
    }

    if (-not (Wait-Port -Port $MySqlPort -TimeoutSeconds 40)) {
        throw "MySQL is not reachable on port $MySqlPort after startup."
    }
}

function Initialize-ProjectDatabase {
    $mysqlCli = Get-MySqlCliPath
    if ([string]::IsNullOrWhiteSpace($mysqlCli)) {
        throw "mysql.exe was not found after MySQL installation."
    }

    $rootPassword = Resolve-MySqlRootPassword
    $sql = @"
CREATE DATABASE IF NOT EXISTS $CampusDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$CampusUser'@'localhost' IDENTIFIED BY '$CampusUserPassword';
ALTER USER '$CampusUser'@'localhost' IDENTIFIED BY '$CampusUserPassword';
GRANT ALL PRIVILEGES ON $CampusDatabase.* TO '$CampusUser'@'localhost';
FLUSH PRIVILEGES;
"@

    Write-Step "Initializing MySQL database and dev user"
    & $mysqlCli --protocol=TCP -h 127.0.0.1 -P $MySqlPort -u root "-p$rootPassword" -e $sql
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create the campus_run database or campus user. Verify the MySQL root password."
    }
}

function Get-MemuraiExePath {
    $paths = @(
        "C:\Program Files\Memurai\memurai.exe",
        "C:\Program Files\Memurai Developer\memurai.exe"
    )

    foreach ($path in $paths) {
        if (Test-Path $path) {
            return $path
        }
    }

    return $null
}

function Ensure-MemuraiService {
    $service = Get-Service -Name "Memurai" -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        $memuraiExe = Get-MemuraiExePath
        if ([string]::IsNullOrWhiteSpace($memuraiExe)) {
            throw "memurai.exe was not found after Memurai installation."
        }

        $configDir = Split-Path -Parent $memuraiExe
        $configPath = Join-Path $configDir "memurai.conf"

        if (-not (Test-Path $configPath)) {
            throw "Memurai config file was not found: $configPath"
        }

        Write-Step "Registering Memurai as a Windows service"
        & $memuraiExe --service-install $configPath
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to install the Memurai Windows service."
        }
    }

    $service = Get-Service -Name "Memurai" -ErrorAction SilentlyContinue
    if ($service.Status -ne "Running") {
        Write-Step "Starting Memurai service"
        Start-Service -Name "Memurai"
    }

    if (-not (Wait-Port -Port 6379 -TimeoutSeconds 20)) {
        throw "Memurai is not reachable on port 6379."
    }
}

function Show-ValidationSummary {
    Write-Step "Validation summary"
    Write-Info ("git:  " + ((& git --version 2>$null) | Out-String).Trim())
    Write-Info ("java: " + ((& java -version 2>&1 | Select-Object -First 1 | Out-String).Trim()))
    Write-Info ("mvn:  " + ((& mvn -v 2>$null | Select-Object -First 2 | Out-String).Trim() -replace "`r?`n", " | "))
    Write-Info ("node: " + ((& node -v 2>$null) | Out-String).Trim())

    if (Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue) {
        Write-Info "MySQL80 service: installed"
    } else {
        Write-Info "MySQL80 service: not detected"
    }

    if (Get-Service -Name "Memurai" -ErrorAction SilentlyContinue) {
        Write-Info "Memurai service: installed"
    } else {
        Write-Info "Memurai service: not detected"
    }

    if ($IncludeWeixinDevTools) {
        $weixin = Get-InstalledPackageLine -PackageId "Tencent.WeixinDevTools"
        if ($weixin) {
            Write-Info "Weixin DevTools: installed"
        } else {
            Write-Info "Weixin DevTools: not detected"
        }
    }
}

try {
    Write-Step "Checking prerequisites for automated setup"
    Assert-PowerShellVersion
    Assert-Administrator
    Assert-WingetAvailable
    Refresh-EnvironmentFromRegistry

    Write-Step "Installing developer toolchain"

    Ensure-WingetPackage -PackageId "Git.Git" -DisplayName "Git for Windows" -Satisfied {
        Test-CommandExists -Name "git"
    }

    Ensure-WingetPackage -PackageId "EclipseAdoptium.Temurin.17.JDK" -DisplayName "JDK 17" -Satisfied {
        (Get-JavaMajorVersion) -eq 17
    }
    Ensure-JavaHome

    Ensure-WingetPackage -PackageId "Apache.Maven" -DisplayName "Apache Maven" -Satisfied {
        Test-CommandExists -Name "mvn"
    }

    Ensure-WingetPackage -PackageId "OpenJS.NodeJS.LTS" -DisplayName "Node.js LTS" -Satisfied {
        $major = Get-NodeMajorVersion
        $null -ne $major -and $major -ge 20
    }

    Ensure-WingetPackage -PackageId "Oracle.MySQL" -DisplayName "MySQL Installer" -Satisfied {
        $null -ne (Get-MySqlInstallerConsolePath)
    }

    Ensure-WingetPackage -PackageId "Memurai.MemuraiDeveloper" -DisplayName "Memurai Developer" -Satisfied {
        $null -ne (Get-MemuraiExePath)
    }

    if ($IncludeWeixinDevTools) {
        Ensure-WingetPackage -PackageId "Tencent.WeixinDevTools" -DisplayName "WeChat DevTools" -Satisfied {
            $null -ne (Get-InstalledPackageLine -PackageId "Tencent.WeixinDevTools")
        }
    }

    Refresh-EnvironmentFromRegistry

    if (-not $SkipMySqlConfiguration) {
        Ensure-MySqlServer
        Ensure-MySqlRunning
        Initialize-ProjectDatabase
    } else {
        Write-Info "Skipping MySQL configuration by request."
    }

    if (-not $SkipMemuraiConfiguration) {
        Ensure-MemuraiService
    } else {
        Write-Info "Skipping Memurai configuration by request."
    }

    Show-ValidationSummary

    Write-Host ""
    Write-Host "Bootstrap finished." -ForegroundColor Green
    Write-Host "Then run:"
    Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\dev-up-windows.ps1"
    Write-Host ""
    Write-Host "Bootstrap log: $bootstrapLog"
} finally {
    Stop-Transcript | Out-Null
}
