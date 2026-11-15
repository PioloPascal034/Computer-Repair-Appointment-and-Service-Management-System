$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$oldUrl = $env:SUPABASE_URL
$oldKey = $env:SUPABASE_ANON_KEY
try {
    javac -encoding UTF-8 -cp (Join-Path $root 'out-fx') -d (Join-Path $root 'out-fx') (Join-Path $root 'tests\WorkflowChecks.java') (Join-Path $root 'tests\AccountPhotoChecks.java') (Join-Path $root 'tests\AuthEmailGuardChecks.java')
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    java -cp (Join-Path $root 'out-fx') crsm.AuthEmailGuardChecks
    if ($LASTEXITCODE -ne 0) { throw 'Email guard checks failed.' }
    $env:SUPABASE_ANON_KEY = 'local-contract-test'
    $env:SUPABASE_URL = 'http://127.0.0.1:18765'
    java -cp (Join-Path $root 'out-fx') crsm.WorkflowChecks
    if ($LASTEXITCODE -ne 0) { throw 'Workflow checks failed.' }
    $env:SUPABASE_URL = 'http://127.0.0.1:18764'
    java -cp (Join-Path $root 'out-fx') crsm.AccountPhotoChecks
    if ($LASTEXITCODE -ne 0) { throw 'Account/photo checks failed.' }
} finally {
    $env:SUPABASE_URL = $oldUrl
    $env:SUPABASE_ANON_KEY = $oldKey
}
