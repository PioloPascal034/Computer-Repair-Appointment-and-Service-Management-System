$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
& (Join-Path $root 'build-javafx.ps1')
$fxJars = Get-ChildItem (Join-Path $root 'lib\javafx\*-win.jar') | ForEach-Object FullName
$classpath = ((Join-Path $root 'out-fx') + ';' + (($fxJars) -join ';') + ';' + (Join-Path $root 'lib\postgresql-42.7.12.jar'))
java -cp $classpath Main
