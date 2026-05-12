# Execucao do ZIP da entrega Web

Este ficheiro explica como correr a aplicacao Web empacotada sem depender do IntelliJ.

## O que sai no ZIP

O artefacto final fica em `target/PlataformaGestaoHorarios-web.zip` e inclui:

- `app/Projeto2-web.jar` com a aplicacao Web empacotada;
- `config/application.properties` com configuracao da base de dados;
- `scripts/iniciar-aplicacao-web.bat`;
- `scripts/iniciar-aplicacao-web.ps1`;
- `sql/*.sql` com scripts de apoio.

## Pre-requisitos

1. Java 25 instalado e acessivel por `JAVA_HOME` ou `PATH`.
2. PostgreSQL local com a base `gestaohorarios`.
3. Estrutura base da base de dados ja existente.

## Preparar a base de dados

Para uma demonstracao estavel, usar:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -d gestaohorarios -f .\sql\demo-entrega.sql
```

## Arrancar a aplicacao

Depois de extrair o ZIP:

### Windows PowerShell

```powershell
.\scripts\iniciar-aplicacao-web.ps1
```

### Windows CMD

```bat
scripts\iniciar-aplicacao-web.bat
```

URL esperada:

```text
http://localhost:8080/web/login
```

## Ajustar configuracao

A ligacao a base de dados fica em:

`config/application.properties`

Se precisares de adaptar utilizador, password ou porta do PostgreSQL, altera esse ficheiro antes de arrancar.
