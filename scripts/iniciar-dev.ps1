param(
    [ValidateSet("desktop", "web", "ambas")]
    [string]$Modo = "ambas",
    [string]$JavaHome,
    [int]$DesktopPort = 8080,
    [int]$WebPort = 8081
)

$ErrorActionPreference = "Stop"

$scriptPath = $PSCommandPath
$scriptDir = Split-Path -Parent $scriptPath
$rootDir = Split-Path -Parent $scriptDir

function Set-Java {
    param([string]$JavaHomePath)

    if ($JavaHomePath) {
        $javaExe = Join-Path $JavaHomePath "bin\java.exe"
        if (-not (Test-Path $javaExe)) {
            throw "JAVA_HOME invalido: $JavaHomePath"
        }
        $env:JAVA_HOME = $JavaHomePath
        $env:Path = "$JavaHomePath\bin;$env:Path"
    }

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw "Nao foi encontrado Java. Define JAVA_HOME ou instala Java 25."
    }
}

function Get-MavenExecutable {
    $embeddedMaven = Join-Path $rootDir ".tools\maven\apache-maven-3.9.12\bin\mvn.cmd"
    $wrapperMaven = Join-Path $rootDir "mvnw.cmd"

    if (Test-Path $embeddedMaven) {
        return $embeddedMaven
    }

    if (Test-Path $wrapperMaven) {
        return $wrapperMaven
    }

    $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mavenCommand) {
        return $mavenCommand.Source
    }

    throw "Nao foi encontrado Maven (.tools, mvnw.cmd ou mvn no PATH)."
}

function Get-MavenCommonArgs {
    $args = @()
    $settingsPath = Join-Path $rootDir ".m2\settings.xml"
    $localRepo = Join-Path $rootDir ".m2\repository"

    if (Test-Path $settingsPath) {
        $args += "-s"
        $args += $settingsPath
    }

    if (Test-Path $localRepo) {
        $args += "-Dmaven.repo.local=$localRepo"
    }

    return $args
}

function Start-Desktop {
    $mavenExecutable = Get-MavenExecutable
    $commonArgs = Get-MavenCommonArgs

    Write-Host "A arrancar Desktop (porta $DesktopPort)..." -ForegroundColor Cyan
    & $mavenExecutable @commonArgs spring-boot:run "-Dspring-boot.run.main-class=com.example.projeto2.AppLauncher" "-Dspring-boot.run.arguments=--server.port=$DesktopPort"
}

function Start-Web {
    $mavenExecutable = Get-MavenExecutable
    $commonArgs = Get-MavenCommonArgs

    Write-Host "A arrancar Web (porta $WebPort)..." -ForegroundColor Cyan
    & $mavenExecutable @commonArgs spring-boot:run "-Dspring-boot.run.main-class=com.example.projeto2.Projeto2WebApplication" "-Dspring-boot.run.arguments=--server.port=$WebPort"
}

Set-Java -JavaHomePath $JavaHome

switch ($Modo) {
    "desktop" {
        Start-Desktop
    }
    "web" {
        Start-Web
    }
    "ambas" {
        $psExe = (Get-Command powershell.exe).Source
        $desktopArgs = @(
            "-NoExit",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            $scriptPath,
            "-Modo",
            "desktop",
            "-DesktopPort",
            $DesktopPort
        )
        $webArgs = @(
            "-NoExit",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            $scriptPath,
            "-Modo",
            "web",
            "-WebPort",
            $WebPort
        )

        if ($JavaHome) {
            $desktopArgs += @("-JavaHome", $JavaHome)
            $webArgs += @("-JavaHome", $JavaHome)
        }

        Start-Process -FilePath $psExe -ArgumentList $desktopArgs | Out-Null
        Start-Process -FilePath $psExe -ArgumentList $webArgs | Out-Null

        Write-Host "Desktop e Web foram lancadas em janelas separadas." -ForegroundColor Green
        Write-Host "Desktop/API: http://localhost:$DesktopPort" -ForegroundColor Green
        Write-Host "Web:         http://localhost:$WebPort/web/login" -ForegroundColor Green
    }
}
