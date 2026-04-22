# Plataforma de Gestao de Horarios

Aplicacao desktop desenvolvida em Java, Spring Boot, Spring Data JPA, PostgreSQL e JavaFX para apoiar a gestao de horarios por turnos numa loja Levi's.

## Estado atual

O projeto ja inclui os principais modulos da entrega desktop:

- autenticacao e dashboard
- gestao da loja e regras
- gestao de funcionarios
- geracao de propostas mensais de horarios
- pedidos e historico de folgas
- permutas de turnos
- preferencias do funcionario
- aprovacao de preferencias por gerencia
- perfil do utilizador
- relatorios mensais de horas

## Tecnologias

- Java 25
- Spring Boot
- Spring Data JPA
- PostgreSQL
- JavaFX 21
- Maven

## Como executar

1. Garantir que tens PostgreSQL a correr localmente.
2. Confirmar as credenciais em [application.properties](src/main/resources/application.properties).
3. Preparar a base de dados com o script de demonstracao:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -d gestaohorarios -f .\sql\demo-entrega.sql
```

4. Arrancar a aplicacao no IntelliJ ou com Maven.

## Gerar o ZIP da entrega

Para gerar o ZIP final da aplicacao desktop sem depender do IntelliJ:

```powershell
.\scripts\gerar-zip-entrega.ps1
```

Se o Java nao estiver configurado no teu `JAVA_HOME`, podes indicar o caminho manualmente:

```powershell
.\scripts\gerar-zip-entrega.ps1 -JavaHome 'C:\Users\franc\.jdks\openjdk-25'
```

O artefacto final fica em:

```text
target\PlataformaGestaoHorarios-desktop.zip
```

Depois de extrair o ZIP, a app pode ser arrancada com:

```powershell
.\scripts\iniciar-aplicacao.ps1
```

Documentacao especifica do pacote entregue:

- [docs/execucao-zip-entrega.md](docs/execucao-zip-entrega.md)

## Dados de demonstracao

O script [demo-entrega.sql](sql/demo-entrega.sql):

- prepara a estrutura adicional das features mais recentes, como aprovacao de preferencias e propostas mensais
- limpa os dados funcionais da aplicacao
- volta a criar cargos, lojas, regras e turnos
- prepara utilizadores e ligacoes a lojas
- carrega horarios, folgas, preferencias e permutas de exemplo
- deixa a aplicacao pronta para demonstracao

## Dados operacionais complementares

Para enriquecer testes manuais sem destruir a demo principal, existe também o script:

- [sql/issue-56-dados-teste-operacionais.sql](sql/issue-56-dados-teste-operacionais.sql)

Este script:

- adiciona uma equipa operacional complementar na loja Braga Parque
- cria cenarios com full-time, part-time, reforco de fim de semana e colaborador inativo
- acrescenta folgas, preferencias, permutas, horarios especiais e auditoria
- foi pensado para apoiar o dashboard de gestão, o painel do gerente e os testes do motor de geração

Helper opcional para carregar estes dados:

```powershell
.\scripts\carregar-dados-teste-operacionais.ps1 -Password 'projeto2026'
```

Documentacao especifica destes cenarios:

- [docs/cenarios-teste-operacionais.md](docs/cenarios-teste-operacionais.md)

## Contas de demonstracao

Todas as contas abaixo usam a password:

```text
123456
```

Contas recomendadas:

- Gerencia com acesso completo: `francisco@levis.com`
- Gerente da loja: `francisco.gomes@levis.com`
- Colaborador para fluxos do funcionario: `henrique.siano@levis.com`

Com estas contas consegues demonstrar:

- gestao de colaboradores
- aprovacao de preferencias
- geracao de horarios
- relatorios mensais
- fluxos do colaborador

## Apoio a apresentacao

O ficheiro [docs/roteiro-demo.md](docs/roteiro-demo.md) resume:

- contas para login
- ordem sugerida para a demonstracao
- modulos que vale a pena mostrar em cada perfil

Ficheiros adicionais de apoio:

- [docs/validacao-entrega.md](docs/validacao-entrega.md) com a checklist de validacao final
- [docs/apoio-relatorio-entrega.md](docs/apoio-relatorio-entrega.md) com o resumo para atualizar o PDF do relatorio
- [docs/relatorio-final-entrega.md](docs/relatorio-final-entrega.md) com o conteudo atualizado do relatorio
- `Projeto2_EI_33400_33397_relatorio_atualizado.pdf` como artefacto final pronto a submeter

## Nota

O script de demonstracao foi pensado para ambiente local de desenvolvimento e apresentacao. Nao deve ser usado em producao porque substitui os dados funcionais atuais.
