param(
    [string]$JavaHome
)

$scriptDir = Split-Path -Parent $PSCommandPath
$rootDir = Split-Path -Parent $scriptDir
$jarPath = Join-Path $rootDir "app\Projeto2-web.jar"
$configPath = Join-Path $rootDir "config\application.properties"

if (-not (Test-Path $jarPath)) {
    throw "Nao encontrei o JAR Web em $jarPath. Este script e para o ZIP de entrega (pasta com app\ e config\). No repositorio, usa .\scripts\iniciar-dev.ps1 -Modo web."
}

if (-not $JavaHome -and $env:JAVA_HOME) {
    $JavaHome = $env:JAVA_HOME
}

if ($JavaHome) {
    if (-not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
        throw "JAVA_HOME invalido: $JavaHome"
    }
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$env:Path"
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Nao foi encontrado o comando java. Define JAVA_HOME ou instala Java 25."
}

if (Test-Path $configPath) {
    $env:SPRING_CONFIG_LOCATION = $configPath
}

Write-Host "A arrancar aplicacao Web..." -ForegroundColor Cyan
Write-Host "URL esperada: http://localhost:8080/web/login" -ForegroundColor Cyan

java -jar $jarPath
