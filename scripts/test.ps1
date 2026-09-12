$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot\..
& .\mvnw.cmd clean test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
