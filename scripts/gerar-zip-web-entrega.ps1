param(
    [string]$JavaHome
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$mavenCmd = Join-Path $repoRoot ".tools\maven\apache-maven-3.9.12\bin\mvn.cmd"
$settingsPath = Join-Path $repoRoot ".m2\settings.xml"
$repoLocal = Join-Path $repoRoot ".m2\repository"

if (-not (Test-Path $mavenCmd)) {
    throw "Nao encontrei o Maven local em $mavenCmd."
}

if (-not $JavaHome) {
    if ($env:JAVA_HOME) {
        $JavaHome = $env:JAVA_HOME
    } else {
        $javaFallback = Join-Path $env:USERPROFILE ".jdks\openjdk-25"
        if (Test-Path $javaFallback) {
            $JavaHome = $javaFallback
        } else {
            $jbrFallback = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\jbr"
            if (Test-Path $jbrFallback) {
                $JavaHome = $jbrFallback
            }
        }
    }
}

if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
    throw "Indica um JAVA_HOME valido ou instala um JDK 25. Exemplo: .\scripts\gerar-zip-web-entrega.ps1 -JavaHome 'C:\Users\franc\.jdks\openjdk-25'"
}

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"

Write-Host "A gerar o ZIP da entrega Web com JAVA_HOME=$JavaHome" -ForegroundColor Cyan

& $mavenCmd `
    "-s" $settingsPath `
    "-Dmaven.repo.local=$repoLocal" `
    "-DskipTests" `
    "clean" `
    "package"

if ($LASTEXITCODE -ne 0) {
    throw "A geracao do ZIP Web falhou."
}

$zipPath = Join-Path $repoRoot "target\PlataformaGestaoHorarios-web.zip"

if (-not (Test-Path $zipPath)) {
    throw "O ZIP Web final nao foi encontrado em $zipPath."
}

Write-Host ""
Write-Host "ZIP Web gerado com sucesso:" -ForegroundColor Green
Write-Host $zipPath -ForegroundColor Green
