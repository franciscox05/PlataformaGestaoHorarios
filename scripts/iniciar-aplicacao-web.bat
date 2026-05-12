@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
set "JAR_PATH=%ROOT_DIR%\app\Projeto2-web.jar"
set "CONFIG_PATH=%ROOT_DIR%\config\application.properties"

if not exist "%JAR_PATH%" (
  echo Nao encontrei o JAR Web em "%JAR_PATH%".
  exit /b 1
)

if exist "%CONFIG_PATH%" (
  set "SPRING_CONFIG_LOCATION=%CONFIG_PATH%"
)

echo A arrancar aplicacao Web...
echo URL esperada: http://localhost:8080/web/login
java -jar "%JAR_PATH%"
