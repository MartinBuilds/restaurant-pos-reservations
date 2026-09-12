$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot\..

if (Test-Path .\smoke-env.ps1) {
  . .\smoke-env.ps1
}
elseif (Test-Path .\smoke-env.example.ps1) {
  Write-Host "smoke-env.ps1 missing — loading smoke-env.example.ps1"
  . .\smoke-env.example.ps1
}
else {
  throw "Missing smoke-env.ps1 (copy from smoke-env.example.ps1 first)."
}

Write-Host "Starting Spring Boot (normal profile)..."
& .\mvnw.cmd spring-boot:run
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
