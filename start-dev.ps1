$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Frontend = Join-Path $Root 'listifyai-frontend'
$BackendLog = Join-Path $Root 'backend-dev.log'
$FrontendLog = Join-Path $Frontend 'frontend-dev.log'

function Stop-PortProcess {
  param([int] $Port)

  $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
  $processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique

  foreach ($processId in $processIds) {
    if ($processId -and $processId -ne $PID) {
      Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
      Start-Sleep -Milliseconds 400
    }
  }
}

function Wait-HttpOk {
  param(
    [string] $Url,
    [int] $Seconds = 45
  )

  $deadline = (Get-Date).AddSeconds($Seconds)
  do {
    try {
      $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 4
      if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
        return
      }
    } catch {
      Start-Sleep -Seconds 1
    }
  } while ((Get-Date) -lt $deadline)

  throw "Timed out waiting for $Url"
}

Write-Host 'Stopping old dev servers on 8080 and 5173...'
Stop-PortProcess -Port 8080
Stop-PortProcess -Port 5173

Write-Host 'Starting Spring Boot backend on http://127.0.0.1:8080 ...'
$backendCommand = "`$env:SPRING_PROFILES_ACTIVE='dev'; .\mvnw.cmd spring-boot:run *> `"$BackendLog`""
Start-Process -FilePath 'powershell.exe' `
  -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $backendCommand) `
  -WorkingDirectory $Root `
  -WindowStyle Hidden | Out-Null

Write-Host 'Starting Vite frontend on http://127.0.0.1:5173 ...'
$frontendCommand = "npm run dev *> `"$FrontendLog`""
Start-Process -FilePath 'powershell.exe' `
  -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $frontendCommand) `
  -WorkingDirectory $Frontend `
  -WindowStyle Hidden | Out-Null

Write-Host 'Verifying backend health...'
Wait-HttpOk -Url 'http://127.0.0.1:8080/actuator/health'

Write-Host 'Verifying frontend...'
Wait-HttpOk -Url 'http://127.0.0.1:5173/'

Write-Host ''
Write-Host 'ListifyAI dev servers are running:'
Write-Host '  Frontend: http://127.0.0.1:5173'
Write-Host '  Backend:  http://127.0.0.1:8080'
Write-Host ''
Write-Host "Logs:"
Write-Host "  Backend:  $BackendLog"
Write-Host "  Frontend: $FrontendLog"
