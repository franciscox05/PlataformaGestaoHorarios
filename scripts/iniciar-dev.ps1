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

    function Test-JavaHome {
        param([string]$CandidateJavaHome)
        if (-not $CandidateJavaHome) {
            return $false
        }
        $javaExePath = Join-Path $CandidateJavaHome "bin\java.exe"
        return (Test-Path $javaExePath)
    }

    function Add-JavaToPath {
        param([string]$ResolvedJavaHome)
        $env:JAVA_HOME = $ResolvedJavaHome
        if ($env:Path -notlike "$ResolvedJavaHome\bin*") {
            $env:Path = "$ResolvedJavaHome\bin;$env:Path"
        }
    }

    if ($JavaHomePath) {
        if (-not (Test-JavaHome -CandidateJavaHome $JavaHomePath)) {
            throw "JAVA_HOME invalido: $JavaHomePath"
        }
        Add-JavaToPath -ResolvedJavaHome $JavaHomePath
    }
    elseif ($env:JAVA_HOME -and (Test-JavaHome -CandidateJavaHome $env:JAVA_HOME)) {
        Add-JavaToPath -ResolvedJavaHome $env:JAVA_HOME
    }
    elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        $searchRoots = @(
            (Join-Path $env:USERPROFILE ".jdks"),
            "C:\Program Files\Eclipse Adoptium",
            "C:\Program Files\Java",
            "C:\Program Files\Microsoft"
        ) | Where-Object { $_ -and (Test-Path $_) }

        $discoveredJdks = @()
        foreach ($root in $searchRoots) {
            $discoveredJdks += Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
                Where-Object { Test-JavaHome -CandidateJavaHome $_.FullName }
        }

        if ($discoveredJdks.Count -gt 0) {
            $selectedJdk = $discoveredJdks |
                Sort-Object Name -Descending |
                Select-Object -First 1
            Add-JavaToPath -ResolvedJavaHome $selectedJdk.FullName
            Write-Host "JAVA_HOME detetado automaticamente: $($selectedJdk.FullName)" -ForegroundColor DarkCyan
        }
    }

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw "Nao foi encontrado Java. Define JAVA_HOME ou instala Java 21+."
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
    & $mavenExecutable @commonArgs "-Dmaven.test.skip=true" spring-boot:run "-Dspring-boot.run.mainClass=com.example.projeto2.AppLauncher" "-Dspring-boot.run.arguments=--server.port=$DesktopPort"
}

function Start-Web {
    $mavenExecutable = Get-MavenExecutable
    $commonArgs = Get-MavenCommonArgs
    $webJarPath = Join-Path $rootDir "target\Projeto2-0.0.1-SNAPSHOT-web.jar"

    Write-Host "A arrancar Web (porta $WebPort)..." -ForegroundColor Cyan
    Write-Host "A gerar artefacto web atualizado..." -ForegroundColor Yellow
    & $mavenExecutable @commonArgs "-DskipTests" package

    if (-not (Test-Path $webJarPath)) {
        throw "Nao foi possivel gerar o JAR web em $webJarPath."
    }

    java -jar $webJarPath "--server.port=$WebPort"
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
