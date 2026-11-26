<#
.SYNOPSIS
    Inicia la aplicación web SAMP cargando automáticamente JDK 21 y el archivo .env.
.EXAMPLE
    .\iniciar-web.ps1 -SinBD
    Inicia la web al instante en modo local sin base de datos (http://localhost:8080).
.EXAMPLE
    .\iniciar-web.ps1
    Inicia la web conectada a la base de datos configurada en .env (Supabase o Docker Postgres).
#>
param(
    [switch]$SinBD
)

$ErrorActionPreference = "Stop"

# 1. Configurar JDK 21 automáticamente
$jdk21 = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk21) {
    $env:JAVA_HOME = $jdk21.FullName
    $env:PATH = "$($env:JAVA_HOME)\bin;$env:PATH"
}

# 2. Cargar variables desde .env si existe
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $idx = $line.IndexOf("=")
            $key = $line.Substring(0, $idx).Trim()
            $val = $line.Substring($idx + 1).Trim()
            if ($key) {
                [Environment]::SetEnvironmentVariable($key, $val, "Process")
            }
        }
    }
}

if ($SinBD) {
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "  SAMP Web — Modo Local Rápido (sin base de datos)" -ForegroundColor Cyan
    Write-Host "  Abre en tu navegador: http://localhost:8080" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Cyan
    $env:SPRING_AUTOCONFIGURE_EXCLUDE = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
} else {
    $env:SPRING_AUTOCONFIGURE_EXCLUDE = $null
    if ($env:SAMP_DB_PASSWORD -eq "<tu-password-de-supabase>") {
        Write-Host "[AVISO] Reemplaza <tu-password-de-supabase> en tu archivo .env con la contraseña real de Supabase." -ForegroundColor Yellow
        Write-Host "        O ejecuta .\iniciar-web.ps1 -SinBD para probar la interfaz web sin base de datos." -ForegroundColor Yellow
        exit 1
    }
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "  SAMP Web — Conectado a PostgreSQL / Supabase" -ForegroundColor Cyan
    Write-Host "  URL BD: $($env:SAMP_DB_URL)" -ForegroundColor Gray
    Write-Host "  Abre en tu navegador: http://localhost:8080" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Cyan
}

& "$PSScriptRoot\mvnw.cmd" spring-boot:run
