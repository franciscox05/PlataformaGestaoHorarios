@echo off
cd /d C:\LEI\2_ano\Semestre_2\P_II\Projeto2\PlataformaGestaoHorarios
call mvnw.cmd clean compile -B 2>&1 > compile_output.txt
echo Exit code: %ERRORLEVEL% >> compile_output.txt
