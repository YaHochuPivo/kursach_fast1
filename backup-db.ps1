Param(
  [string]$Host = $env:DB_HOST, 
  [string]$Port = $env:DB_PORT, 
  [string]$Name = $env:DB_NAME, 
  [string]$User = $env:DB_USERNAME, 
  [string]$Password = $env:DB_PASSWORD,
  [string]$OutDir = "./backups"
)

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir | Out-Null }

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$dumpFile = Join-Path $OutDir ("${Name}_$timestamp.dump")

if (-not $Host) { $Host = "localhost" }
if (-not $Port) { $Port = "5432" }
if (-not $Name) { throw "DB_NAME is required (or pass -Name)" }
if (-not $User) { throw "DB_USERNAME is required (or pass -User)" }
if (-not $Password) { throw "DB_PASSWORD is required (or pass -Password)" }

$env:PGPASSWORD = $Password

Write-Host "Backing up $Name to $dumpFile ..."
pg_dump --host=$Host --port=$Port --username=$User --format=custom --file=$dumpFile $Name

if ($LASTEXITCODE -ne 0) { throw "pg_dump failed with code $LASTEXITCODE" }

Write-Host "Backup completed: $dumpFile"
