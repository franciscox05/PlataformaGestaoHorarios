@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "APP_ROOT=%SCRIPT_DIR%.."
set "JAR_PATH=%APP_ROOT%\app\Projeto2.jar"
set "CONFIG_DIR=%APP_ROOT%\config"
set "JAVA_CMD=java"

if not exist "%JAR_PATH%" (
    echo Nao encontrei o JAR da aplicacao em "%JAR_PATH%".
    exit /b 1
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

set "CONFIG_URI=%CONFIG_DIR:\=/%"
"%JAVA_CMD%" "-Dspring.config.additional-location=file:/%CONFIG_URI%/" -jar "%JAR_PATH%"
exit /b %ERRORLEVEL%
