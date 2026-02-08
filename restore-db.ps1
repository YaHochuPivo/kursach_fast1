Param(
  [string]$Host = $env:DB_HOST, 
  [string]$Port = $env:DB_PORT, 
  [string]$Name = $env:DB_NAME, 
  [string]$User = $env:DB_USERNAME, 
  [string]$Password = $env:DB_PASSWORD,
  [Parameter(Mandatory=$true)][string]$DumpFile
)

if (-not (Test-Path $DumpFile)) { throw "Dump file not found: $DumpFile" }
if (-not $Host) { $Host = "localhost" }
if (-not $Port) { $Port = "5432" }
if (-not $Name) { throw "DB_NAME is required (or pass -Name)" }
if (-not $User) { throw "DB_USERNAME is required (or pass -User)" }
if (-not $Password) { throw "DB_PASSWORD is required (or pass -Password)" }

$env:PGPASSWORD = $Password

Write-Host "Restoring $Name from $DumpFile ..."
pg_restore --host=$Host --port=$Port --username=$User --dbname=$Name --clean --if-exists $DumpFile

if ($LASTEXITCODE -ne 0) { throw "pg_restore failed with code $LASTEXITCODE" }
Write-Host "Restore completed"
