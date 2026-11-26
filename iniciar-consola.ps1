<#
.SYNOPSIS
    Inicia SAMP en Modo Consola (ArrayList + archivos JSON en ./data-json) sin servidor web ni BD.
.EXAMPLE
    .\iniciar-consola.ps1
#>
$ErrorActionPreference = "Stop"

# 1. Configurar JDK 21 automáticamente
$jdk21 = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk21) {
    $env:JAVA_HOME = $jdk21.FullName
    $env:PATH = "$($env:JAVA_HOME)\bin;$env:PATH"
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Iniciando SAMP en Modo Consola (ArrayList + JSON)..." -ForegroundColor Cyan
Write-Host "  Archivos de datos: .\data-json\" -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Cyan

& "$PSScriptRoot\mvnw.cmd" -q spring-boot:run "-Dspring-boot.run.profiles=consola"
