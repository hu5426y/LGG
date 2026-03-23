param(
    [int]$MySqlPort = 3306,
    [string]$Database = "campus_run",
    [string]$AppUser = "campus",
    [string]$AppPassword = "campus123",
    [string]$RootPassword
)

$ErrorActionPreference = "Stop"

function ConvertTo-PlainText {
    param([Security.SecureString]$SecureString)

    if ($null -eq $SecureString) {
        return ""
    }

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function ConvertTo-MySqlStringLiteral {
    param([string]$Value)

    if ($null -eq $Value) {
        return "NULL"
    }

    return "'" + $Value.Replace("\", "\\").Replace("'", "''") + "'"
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

    throw "mysql.exe was not found. Install MySQL Server first or add it to PATH."
}

function New-MySqlDefaultsFile {
    param([string]$Password)

    if ([string]::IsNullOrEmpty($Password)) {
        return $null
    }

    $defaultsFile = [System.IO.Path]::GetTempFileName()
    Set-Content -Path $defaultsFile -Encoding ASCII -Value @(
        "[client]",
        "password=$Password"
    )
    return $defaultsFile
}

function ConvertTo-WindowsCommandLineArgument {
    param([string]$Value)

    if ($null -eq $Value) {
        return '""'
    }

    if ($Value -notmatch '[\s"]') {
        return $Value
    }

    $escaped = $Value -replace '(\\*)"', '$1$1\"'
    $escaped = $escaped -replace '(\\+)$', '$1$1'
    return '"' + $escaped + '"'
}

function Invoke-MySqlSql {
    param(
        [string]$MySqlCli,
        [string]$Username,
        [string]$Password,
        [string]$Sql
    )

    $defaultsFile = New-MySqlDefaultsFile -Password $Password
    $normalizedSql = (($Sql -split "`r?`n") | ForEach-Object { $_.Trim() } | Where-Object { $_ }) -join " "
    $stdoutFile = [System.IO.Path]::GetTempFileName()
    $stderrFile = [System.IO.Path]::GetTempFileName()

    try {
        $arguments = @()
        if (-not [string]::IsNullOrWhiteSpace($defaultsFile)) {
            $arguments += "--defaults-extra-file=$defaultsFile"
        }
        $arguments += @(
            "--protocol=TCP",
            "-h", "localhost",
            "-P", $MySqlPort.ToString(),
            "-u", $Username,
            "--execute=$normalizedSql"
        )
        $argumentLine = ($arguments | ForEach-Object { ConvertTo-WindowsCommandLineArgument -Value $_ }) -join " "

        $process = Start-Process -FilePath $MySqlCli `
            -ArgumentList $argumentLine `
            -NoNewWindow `
            -Wait `
            -PassThru `
            -RedirectStandardOutput $stdoutFile `
            -RedirectStandardError $stderrFile

        if ($process.ExitCode -ne 0) {
            $stderr = ""
            $stdout = ""
            if (Test-Path $stderrFile) {
                $stderr = (Get-Content -Path $stderrFile | Out-String).Trim()
            }
            if (Test-Path $stdoutFile) {
                $stdout = (Get-Content -Path $stdoutFile | Out-String).Trim()
            }

            $details = @($stderr, $stdout) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

            if ($details.Count -eq 0) {
                throw "mysql.exe returned exit code $($process.ExitCode)."
            }

            throw "mysql.exe returned exit code $($process.ExitCode): $($details -join ' | ')"
        }

        if (Test-Path $stdoutFile) {
            $stdout = Get-Content -Path $stdoutFile -Raw
            if (-not [string]::IsNullOrWhiteSpace($stdout)) {
                Write-Output $stdout.Trim()
            }
        }
    } finally {
        if (-not [string]::IsNullOrWhiteSpace($defaultsFile)) {
            Remove-Item -Force $defaultsFile -ErrorAction SilentlyContinue
        }
        Remove-Item -Force $stdoutFile, $stderrFile -ErrorAction SilentlyContinue
    }
}

if ([string]::IsNullOrWhiteSpace($RootPassword)) {
    $securePassword = Read-Host "Enter MySQL root password" -AsSecureString
    $RootPassword = ConvertTo-PlainText -SecureString $securePassword
}

if ($Database -notmatch '^[A-Za-z0-9_]+$') {
    throw "Database name '$Database' contains unsupported characters. Use only letters, numbers, and underscores."
}

$mysqlCli = Get-MySqlCliPath
$appUserLiteral = ConvertTo-MySqlStringLiteral -Value $AppUser
$appPasswordLiteral = ConvertTo-MySqlStringLiteral -Value $AppPassword
$sql = @"
CREATE DATABASE IF NOT EXISTS $Database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS $appUserLiteral@'localhost' IDENTIFIED BY $appPasswordLiteral;
CREATE USER IF NOT EXISTS $appUserLiteral@'127.0.0.1' IDENTIFIED BY $appPasswordLiteral;
ALTER USER $appUserLiteral@'localhost' IDENTIFIED BY $appPasswordLiteral;
ALTER USER $appUserLiteral@'127.0.0.1' IDENTIFIED BY $appPasswordLiteral;
GRANT ALL PRIVILEGES ON $Database.* TO $appUserLiteral@'localhost';
GRANT ALL PRIVILEGES ON $Database.* TO $appUserLiteral@'127.0.0.1';
FLUSH PRIVILEGES;
"@

Write-Host ""
Write-Host "==> Initializing MySQL database and dev user" -ForegroundColor Cyan
Invoke-MySqlSql -MySqlCli $mysqlCli -Username "root" -Password $RootPassword -Sql $sql

Write-Host ""
Write-Host "==> Verifying campus login" -ForegroundColor Cyan
Invoke-MySqlSql -MySqlCli $mysqlCli -Username $AppUser -Password $AppPassword -Sql "SHOW DATABASES LIKE '$Database';"

Write-Host ""
Write-Host "MySQL dev database is ready." -ForegroundColor Green
Write-Host "Database: $Database"
Write-Host "User:     $AppUser"
Write-Host "Next:     cd .\\backend ; mvn spring-boot:run"
