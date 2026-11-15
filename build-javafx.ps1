$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$fxJars = Get-ChildItem (Join-Path $root 'lib\javafx\*-win.jar') | ForEach-Object FullName
$classpath = ((Join-Path $root 'out-fx') + ';' + (($fxJars) -join ';') + ';' + (Join-Path $root 'lib\postgresql-42.7.12.jar'))
New-Item -ItemType Directory -Force (Join-Path $root 'out-fx') | Out-Null
javac -encoding UTF-8 -cp $classpath -d (Join-Path $root 'out-fx') (Get-ChildItem (Join-Path $root 'src') -Filter '*.java').FullName (Get-ChildItem (Join-Path $root 'src\crsm') -Filter '*.java').FullName (Get-ChildItem (Join-Path $root 'src\crsm\fx') -Filter '*.java').FullName
if ($LASTEXITCODE -ne 0) { throw "JavaFX compilation failed (exit $LASTEXITCODE)." }
New-Item -ItemType Directory -Force (Join-Path $root 'out-fx\crsm\fx') | Out-Null
Copy-Item -Force (Join-Path $root 'src\crsm\fx\*.fxml'),(Join-Path $root 'src\crsm\fx\*.css') (Join-Path $root 'out-fx\crsm\fx')
New-Item -ItemType Directory -Force (Join-Path $root 'out-fx\crsm\assets') | Out-Null
Copy-Item -Force (Join-Path $root 'src\crsm\assets\repair-workbench.jpg') (Join-Path $root 'out-fx\crsm\assets')
Write-Host 'JavaFX build complete.'
