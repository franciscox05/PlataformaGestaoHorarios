@echo off
set JAVA_HOME=C:\Users\franc\.jdks\openjdk-25
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d C:\LEI\2_ano\Semestre_2\P_II\Projeto2\PlataformaGestaoHorarios
.tools\maven\apache-maven-3.9.12\bin\mvn.cmd clean compile -B
echo EXITCODE=%ERRORLEVEL%
