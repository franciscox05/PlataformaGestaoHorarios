# Execucao do ZIP da entrega desktop

Este ficheiro explica como correr a aplicacao empacotada sem depender do IntelliJ.

## O que sai no ZIP

O artefacto final fica em `target/PlataformaGestaoHorarios-desktop.zip` e inclui:

- `app/Projeto2.jar` com a aplicacao desktop empacotada
- `config/application.properties` com a configuracao da base de dados
- `scripts/iniciar-aplicacao.bat`
- `scripts/iniciar-aplicacao.ps1`
- `sql/*.sql` com os scripts de apoio
- `docs/` com a documentacao de demonstracao e validacao

## Pre-requisitos

1. Java 25 instalado e acessivel por `JAVA_HOME` ou `PATH`.
2. PostgreSQL local com a base `gestaohorarios`.
3. Estrutura base da base de dados ja existente.

## Preparar a base de dados

Para uma demonstracao estavel, usar o script:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -d gestaohorarios -f .\sql\demo-entrega.sql
```

O script de demo:

- atualiza a estrutura necessaria das features recentes
- limpa os dados funcionais
- recarrega lojas, cargos, regras, utilizadores, horarios, folgas, preferencias e permutas

## Arrancar a aplicacao

Depois de extrair o ZIP:

### Windows PowerShell

```powershell
.\scripts\iniciar-aplicacao.ps1
```

### Windows CMD

```bat
scripts\iniciar-aplicacao.bat
```

## Ajustar configuracao

A ligacao a base de dados fica em:

`config/application.properties`

Se precisares de adaptar utilizador, password ou porta do PostgreSQL, altera esse ficheiro antes de arrancar.

## Contas de demonstracao

Password das contas demo:

```text
123456
```

Perfis recomendados:

- `francisco@levis.com`
- `francisco.gomes@levis.com`
- `henrique.siano@levis.com`
