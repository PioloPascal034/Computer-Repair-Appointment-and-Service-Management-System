param(
    [ValidateSet('inspect','verify','apply')][string]$Action='inspect',
    [string[]]$SqlFiles=@()
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $root
try {
    $classpath=(Join-Path $root 'out-fx')+';'+(Join-Path $root 'lib/postgresql-42.7.12.jar')
    $maintenanceSources=@(
        (Join-Path $root 'src/crsm/AppConfig.java'),
        (Join-Path $root 'src/crsm/DatabaseConfig.java'),
        (Join-Path $root 'src/crsm/JsonCodec.java'),
        (Join-Path $root 'src/crsm/SupabaseClient.java'),
        (Join-Path $root 'tools/crsm/DatabaseMaintenance.java')
    )
    javac -encoding UTF-8 -cp (Join-Path $root 'lib/postgresql-42.7.12.jar') -d (Join-Path $root 'out-fx') $maintenanceSources
    if ($LASTEXITCODE -ne 0) { throw 'Maintenance compilation failed. Build the app first.' }
    if ($Action -eq 'apply' -and $SqlFiles.Count -eq 0) { throw 'Supply the explicit project SQL files to apply.' }
    java -cp $classpath crsm.DatabaseMaintenance $Action @SqlFiles
    if ($LASTEXITCODE -ne 0) { throw 'Database maintenance failed; inspect the reported SQLSTATE.' }
} finally { Pop-Location }
