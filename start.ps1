# Kick AutoUploader - Spring Boot dev server
# Usage: .\start.ps1

# Load variables from .env
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.+)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
        Write-Host "  Loaded: $($matches[1].Trim())" -ForegroundColor DarkGray
    }
}

Write-Host "Starting Spring Boot on http://localhost:8080 ..." -ForegroundColor Green
.\mvnw spring-boot:run
