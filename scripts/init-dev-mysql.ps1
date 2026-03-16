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
        return [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
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

function Invoke-MySqlSql {
    param(
        [string]$MySqlCli,
        [string]$Username,
        [string]$Password,
        [string]$Sql
    )

    $arguments = @(
        "--protocol=TCP",
        "-h", "localhost",
        "-P", $MySqlPort.ToString(),
        "-u", $Username,
        "--execute=$Sql"
    )

    if (-not [string]::IsNullOrEmpty($Password)) {
        $arguments += "-p$Password"
    }

    & $MySqlCli @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "mysql.exe returned exit code $LASTEXITCODE."
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
ALTER USER $appUserLiteral@'localhost' IDENTIFIED BY $appPasswordLiteral;
GRANT ALL PRIVILEGES ON $Database.* TO $appUserLiteral@'localhost';
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
