$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$appRoot = Split-Path -Parent $scriptDir
$jarPath = Join-Path $appRoot "app\Projeto2.jar"
$configDir = Join-Path $appRoot "config"
$configUri = "file:/{0}/" -f (($configDir -replace "\\", "/").TrimStart("/"))

if (-not (Test-Path $jarPath)) {
    throw "Nao encontrei o JAR da aplicacao em $jarPath."
}

$javaExecutavel = $null

if ($env:JAVA_HOME) {
    $javaFromHome = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $javaFromHome) {
        $javaExecutavel = $javaFromHome
    }
}

if (-not $javaExecutavel) {
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $javaExecutavel = $javaCommand.Source
    }
}

if (-not $javaExecutavel) {
    throw "Nao encontrei Java no PATH nem em JAVA_HOME. Instala Java 25 e volta a tentar."
}

& $javaExecutavel `
    "-Dspring.config.additional-location=$configUri" `
    "-jar" `
    $jarPath
