param(
    [switch]$IncludeWeixinDevTools,
    [switch]$SkipMySqlConfiguration,
    [switch]$SkipMemuraiConfiguration,
    [int]$MySqlPort = 3306,
    [string]$MySqlRootPassword,
    [string]$MySqlServiceName = "MySQL80",
    [string]$CampusDatabase = "campus_run",
    [string]$CampusUser = "campus",
    [string]$CampusUserPassword = "campus123",
    [string]$MavenVersion = "3.9.12"
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

    $output = & cmd.exe /c "java -version 2>&1" | Out-String
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

    $isSatisfied = & $Satisfied
    if ($isSatisfied) {
        Write-Info "$DisplayName already looks ready."
        return
    }

    Install-WingetPackage -PackageId $PackageId -DisplayName $DisplayName
}

function Ensure-MySqlSoftware {
    $mysqlCli = Get-MySqlCliPath
    $mysqldExe = Get-MySqlServerExePath
    if ((-not [string]::IsNullOrWhiteSpace($mysqlCli)) -and (-not [string]::IsNullOrWhiteSpace($mysqldExe))) {
        Write-Info "MySQL binaries already look ready."
        return
    }

    Install-WingetPackage -PackageId "Oracle.MySQL" -DisplayName "MySQL Server"

    $mysqlCli = Get-MySqlCliPath
    $mysqldExe = Get-MySqlServerExePath
    if ([string]::IsNullOrWhiteSpace($mysqlCli) -or [string]::IsNullOrWhiteSpace($mysqldExe)) {
        throw "MySQL binaries were not found after winget install. Verify the Oracle.MySQL package on this machine."
    }
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

function Find-LatestMavenHome {
    $roots = @(
        "C:\Tools",
        "C:\Program Files\Apache",
        "C:\Program Files"
    )

    $candidates = foreach ($root in $roots) {
        if (-not (Test-Path $root)) {
            continue
        }

        Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
            ForEach-Object {
                if ($_.Name -match '^apache-maven-(\d+\.\d+\.\d+)$') {
                    [pscustomobject]@{
                        FullName = $_.FullName
                        Name = $_.Name
                        Version = [Version]$Matches[1]
                    }
                }
            }
    }

    return ($candidates | Sort-Object Version -Descending | Select-Object -First 1).FullName
}

function Ensure-MachinePathContains {
    param([string]$DirectoryPath)

    if ([string]::IsNullOrWhiteSpace($DirectoryPath) -or -not (Test-Path $DirectoryPath)) {
        return
    }

    $machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $entries = @()
    if (-not [string]::IsNullOrWhiteSpace($machinePath)) {
        $entries = $machinePath -split ';'
    }

    if (-not ($entries | Where-Object { $_ -eq $DirectoryPath })) {
        $newPath = if ([string]::IsNullOrWhiteSpace($machinePath)) {
            $DirectoryPath
        } else {
            "{0};{1}" -f $DirectoryPath, $machinePath
        }
        [Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
    }

    Refresh-EnvironmentFromRegistry
}

function Ensure-MavenHome {
    $mavenHome = Find-LatestMavenHome
    if ([string]::IsNullOrWhiteSpace($mavenHome)) {
        Write-Info "Maven was not detected. Skipping Maven environment fixup."
        return
    }

    $currentMavenHome = [Environment]::GetEnvironmentVariable("MAVEN_HOME", "Machine")
    $currentM2Home = [Environment]::GetEnvironmentVariable("M2_HOME", "Machine")
    if ($currentMavenHome -ne $mavenHome -or $currentM2Home -ne $mavenHome) {
        Write-Step "Configuring MAVEN_HOME"
        [Environment]::SetEnvironmentVariable("MAVEN_HOME", $mavenHome, "Machine")
        [Environment]::SetEnvironmentVariable("M2_HOME", $mavenHome, "Machine")
    }

    Ensure-MachinePathContains -DirectoryPath (Join-Path $mavenHome "bin")
}

function Get-MavenDownloadUrls {
    param([string]$Version)

    $archiveName = "apache-maven-$Version-bin.zip"
    return @(
        "https://mirrors.aliyun.com/apache/maven/maven-3/$Version/binaries/$archiveName",
        "https://downloads.apache.org/maven/maven-3/$Version/binaries/$archiveName",
        "https://archive.apache.org/dist/maven/maven-3/$Version/binaries/$archiveName"
    )
}

function Download-FileWithFallback {
    param(
        [string[]]$Urls,
        [string]$OutFile,
        [string]$DisplayName
    )

    foreach ($url in $Urls) {
        try {
            Write-Info "Downloading $DisplayName from $url"
            Invoke-WebRequest -Uri $url -OutFile $OutFile -UseBasicParsing
            return
        } catch {
            Write-Info "Download failed from $url"
            if (Test-Path $OutFile) {
                Remove-Item -Path $OutFile -Force -ErrorAction SilentlyContinue
            }
        }
    }

    throw "Failed to download $DisplayName from all configured URLs."
}

function Ensure-MavenInstalled {
    if (Test-CommandExists -Name "mvn") {
        Write-Info "Apache Maven already looks ready."
        return
    }

    $existingMavenHome = Find-LatestMavenHome
    if (-not [string]::IsNullOrWhiteSpace($existingMavenHome)) {
        Ensure-MavenHome
        if (Test-CommandExists -Name "mvn") {
            Write-Info "Apache Maven already looks ready."
            return
        }
    }

    $toolsDir = "C:\Tools"
    $archiveName = "apache-maven-$MavenVersion-bin.zip"
    $archivePath = Join-Path $runtimeDir $archiveName
    $installHome = Join-Path $toolsDir "apache-maven-$MavenVersion"

    if (-not (Test-Path $archivePath)) {
        Write-Step "Downloading Apache Maven $MavenVersion"
        Download-FileWithFallback -Urls (Get-MavenDownloadUrls -Version $MavenVersion) -OutFile $archivePath -DisplayName "Apache Maven $MavenVersion"
    } else {
        Write-Info "Reusing cached Maven archive at $archivePath"
    }

    if (-not (Test-Path $installHome)) {
        Write-Step "Installing Apache Maven $MavenVersion"
        New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
        Expand-Archive -Path $archivePath -DestinationPath $toolsDir -Force
    } else {
        Write-Info "Apache Maven $MavenVersion is already unpacked."
    }

    Ensure-MavenHome
    if (-not (Test-CommandExists -Name "mvn")) {
        throw "Apache Maven was unpacked, but mvn is still unavailable. Open a new PowerShell window and rerun this script."
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

    $roots = @(
        "C:\Program Files (x86)\MySQL",
        "C:\Program Files\MySQL"
    )

    foreach ($root in $roots) {
        if (-not (Test-Path $root)) {
            continue
        }

        $match = Get-ChildItem -Path $root -Filter MySQLInstallerConsole.exe -Recurse -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending |
            Select-Object -First 1

        if ($null -ne $match) {
            return $match.FullName
        }
    }

    return $null
}

function Get-MySqlExecutablePath {
    param([string]$ExecutableName)

    $roots = @(
        "C:\Program Files\MySQL",
        "C:\Program Files (x86)\MySQL"
    )
    $pattern = 'MySQL Server .*\\bin\\' + [Regex]::Escape($ExecutableName) + '$'

    $matches = foreach ($root in $roots) {
        if (-not (Test-Path $root)) {
            continue
        }

        Get-ChildItem -Path $root -Filter $ExecutableName -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match $pattern }
    }

    return ($matches | Sort-Object FullName -Descending | Select-Object -First 1).FullName
}

function Get-MySqlCliPath {
    return Get-MySqlExecutablePath -ExecutableName "mysql.exe"
}

function Get-MySqlServerExePath {
    return Get-MySqlExecutablePath -ExecutableName "mysqld.exe"
}

function Get-MySqlBaseDir {
    $mysqldExe = Get-MySqlServerExePath
    if ([string]::IsNullOrWhiteSpace($mysqldExe)) {
        return $null
    }

    return Split-Path -Parent (Split-Path -Parent $mysqldExe)
}

function Get-ManagedMySqlPaths {
    $configRoot = Join-Path $env:ProgramData ("MySQL\" + $MySqlServiceName)
    return [pscustomobject]@{
        ConfigRoot = $configRoot
        ConfigPath = Join-Path $configRoot "my.ini"
        DataDir = Join-Path $configRoot "data"
    }
}

function ConvertTo-IniPath {
    param([string]$Path)

    return $Path -replace '\\', '/'
}

function Get-MySqlService {
    $preferredNames = @($MySqlServiceName, "MySQL80", "MySQL", "mysql") | Select-Object -Unique
    foreach ($name in $preferredNames) {
        $service = Get-Service -Name $name -ErrorAction SilentlyContinue
        if ($null -ne $service) {
            return $service
        }
    }

    return Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue | Sort-Object Name | Select-Object -First 1
}

function Write-MySqlOptionFile {
    param(
        [string]$BaseDir,
        [string]$ConfigPath,
        [string]$DataDir
    )

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $ConfigPath) | Out-Null
    New-Item -ItemType Directory -Force -Path $DataDir | Out-Null

    $content = @"
[client]
port=$MySqlPort

[mysqld]
basedir=$(ConvertTo-IniPath -Path $BaseDir)
datadir=$(ConvertTo-IniPath -Path $DataDir)
port=$MySqlPort
bind-address=127.0.0.1
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
"@

    Set-Content -Path $ConfigPath -Value $content -Encoding ASCII
}

function Test-MySqlDataDirectoryInitialized {
    param([string]$DataDir)

    return (Test-Path (Join-Path $DataDir "mysql")) -and (Test-Path (Join-Path $DataDir "performance_schema"))
}

function Initialize-MySqlDataDirectory {
    param(
        [string]$MySqlServerExe,
        [string]$ConfigPath,
        [string]$DataDir
    )

    if (Test-MySqlDataDirectoryInitialized -DataDir $DataDir) {
        return
    }

    $existingEntries = @()
    if (Test-Path $DataDir) {
        $existingEntries = Get-ChildItem -Path $DataDir -Force -ErrorAction SilentlyContinue | Select-Object -First 1
    }

    if (($existingEntries.Count -gt 0) -and (-not (Test-MySqlDataDirectoryInitialized -DataDir $DataDir))) {
        throw "MySQL data directory exists but is not initialized: $DataDir. Remove it or complete setup manually before rerunning the script."
    }

    Write-Step "Initializing MySQL data directory"
    & $MySqlServerExe "--defaults-file=$ConfigPath" --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) {
        throw "mysqld failed to initialize the MySQL data directory."
    }
}

function Register-MySqlWindowsService {
    param(
        [string]$MySqlServerExe,
        [string]$ConfigPath
    )

    $service = Get-Service -Name $MySqlServiceName -ErrorAction SilentlyContinue
    if ($null -ne $service) {
        return
    }

    Write-Step "Registering $MySqlServiceName service"
    & $MySqlServerExe --install $MySqlServiceName "--defaults-file=$ConfigPath"
    if ($LASTEXITCODE -ne 0) {
        $service = Get-Service -Name $MySqlServiceName -ErrorAction SilentlyContinue
        if ($null -eq $service) {
            throw "mysqld failed to register the $MySqlServiceName Windows service."
        }
    }
}

function ConvertTo-MySqlStringLiteral {
    param([string]$Value)

    return "'" + $Value.Replace("\", "\\").Replace("'", "''") + "'"
}

function Test-MySqlRootConnection {
    param(
        [string]$Password,
        [switch]$UseBlankPassword
    )

    $mysqlCli = Get-MySqlCliPath
    if ([string]::IsNullOrWhiteSpace($mysqlCli)) {
        return $false
    }

    $command = '"' + $mysqlCli + '" --protocol=TCP -h localhost -P ' + $MySqlPort + ' -u root --connect-timeout=5'
    if ($UseBlankPassword) {
        $command += " --skip-password"
    } else {
        $command += ' "-p' + $Password + '"'
    }
    $command += ' -e "SELECT 1;" 1>nul 2>nul'

    & cmd.exe /c $command
    return $LASTEXITCODE -eq 0
}

function Ensure-MySqlRootPassword {
    $mysqlCli = Get-MySqlCliPath
    if ([string]::IsNullOrWhiteSpace($mysqlCli)) {
        throw "mysql.exe was not found after MySQL installation."
    }

    $rootPassword = Resolve-MySqlRootPassword
    if (Test-MySqlRootConnection -Password $rootPassword) {
        return $rootPassword
    }

    if (-not (Test-MySqlRootConnection -UseBlankPassword)) {
        throw "Failed to connect to MySQL as root. Verify the configured MySQL root password for this machine."
    }

    $rootPasswordLiteral = ConvertTo-MySqlStringLiteral -Value $rootPassword
    $sql = @"
ALTER USER 'root'@'localhost' IDENTIFIED BY $rootPasswordLiteral;
FLUSH PRIVILEGES;
"@

    Write-Step "Configuring MySQL root password"
    $escapedSql = $sql.Replace('"', '\"')
    $command = '"' + $mysqlCli + '" --protocol=TCP -h localhost -P ' + $MySqlPort + ' -u root --skip-password -e "' + $escapedSql + '"'
    & cmd.exe /c $command
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to set the MySQL root password after initializing the MySQL data directory."
    }

    if (-not (Test-MySqlRootConnection -Password $rootPassword)) {
        throw "Failed to verify the MySQL root password after initialization."
    }

    return $rootPassword
}

function Ensure-MySqlServer {
    $service = Get-MySqlService
    if ($null -ne $service) {
        Write-Info ($service.Name + " service already exists.")
        return
    }

    $mysqldExe = Get-MySqlServerExePath
    if ([string]::IsNullOrWhiteSpace($mysqldExe)) {
        throw "mysqld.exe was not found after MySQL installation."
    }

    $baseDir = Get-MySqlBaseDir
    if ([string]::IsNullOrWhiteSpace($baseDir)) {
        throw "Failed to determine the MySQL installation directory from mysqld.exe."
    }

    $managedPaths = Get-ManagedMySqlPaths
    Write-MySqlOptionFile -BaseDir $baseDir -ConfigPath $managedPaths.ConfigPath -DataDir $managedPaths.DataDir
    Initialize-MySqlDataDirectory -MySqlServerExe $mysqldExe -ConfigPath $managedPaths.ConfigPath -DataDir $managedPaths.DataDir
    Register-MySqlWindowsService -MySqlServerExe $mysqldExe -ConfigPath $managedPaths.ConfigPath
}

function Ensure-MySqlRunning {
    $service = Get-MySqlService
    if ($null -eq $service) {
        throw "No MySQL Windows service was found."
    }

    if ($service.Status -ne "Running") {
        Write-Step ("Starting " + $service.Name + " service")
        Start-Service -Name $service.Name
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

    $rootPassword = Ensure-MySqlRootPassword
    $campusUserLiteral = ConvertTo-MySqlStringLiteral -Value $CampusUser
    $campusUserPasswordLiteral = ConvertTo-MySqlStringLiteral -Value $CampusUserPassword
    $sql = @"
CREATE DATABASE IF NOT EXISTS $CampusDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS $campusUserLiteral@'localhost' IDENTIFIED BY $campusUserPasswordLiteral;
ALTER USER $campusUserLiteral@'localhost' IDENTIFIED BY $campusUserPasswordLiteral;
GRANT ALL PRIVILEGES ON $CampusDatabase.* TO $campusUserLiteral@'localhost';
FLUSH PRIVILEGES;
"@

    Write-Step "Initializing MySQL database and dev user"
    $escapedSql = $sql.Replace('"', '\"')
    $command = '"' + $mysqlCli + '" --protocol=TCP -h localhost -P ' + $MySqlPort + ' -u root "-p' + $rootPassword + '" -e "' + $escapedSql + '"'
    & cmd.exe /c $command
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
    Write-Info ("java: " + ((& cmd.exe /c "java -version 2>&1" | Select-Object -First 1 | Out-String).Trim()))
    Write-Info ("mvn:  " + ((& mvn -v 2>$null | Select-Object -First 2 | Out-String).Trim() -replace "`r?`n", " | "))
    Write-Info ("node: " + ((& node -v 2>$null) | Out-String).Trim())

    $mysqlService = Get-MySqlService
    if ($null -ne $mysqlService) {
        Write-Info ($mysqlService.Name + " service: installed")
    } else {
        Write-Info "MySQL service: not detected"
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
        ((Get-JavaMajorVersion) -eq 17) -or ($null -ne (Find-LatestTemurin17Home))
    }
    Ensure-JavaHome

    Ensure-MavenInstalled

    Ensure-WingetPackage -PackageId "OpenJS.NodeJS.LTS" -DisplayName "Node.js LTS" -Satisfied {
        $major = Get-NodeMajorVersion
        $null -ne $major -and $major -ge 20
    }

    Ensure-MySqlSoftware

    if (-not $SkipMemuraiConfiguration) {
        Ensure-WingetPackage -PackageId "Memurai.MemuraiDeveloper" -DisplayName "Memurai Developer" -Satisfied {
            $null -ne (Get-MemuraiExePath)
        }
    } else {
        Write-Info "Skipping Memurai installation/configuration by request."
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
