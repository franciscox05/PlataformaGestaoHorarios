# Plataforma de Gestao de Horarios

Aplicacao desktop desenvolvida em Java, Spring Boot, Spring Data JPA, PostgreSQL e JavaFX para apoiar a gestao de horarios por turnos numa loja Levi's.

## Estado atual

O projeto ja inclui os principais modulos da entrega desktop:

- autenticacao e dashboard
- gestao da loja e regras
- gestao de funcionarios
- pedidos e historico de folgas
- permutas de turnos
- preferencias do funcionario
- perfil do utilizador
- relatorios mensais de horas

## Tecnologias

- Java 24
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

## Dados de demonstracao

O script [demo-entrega.sql](sql/demo-entrega.sql):

- limpa os dados funcionais da aplicacao
- volta a criar cargos, lojas, regras e turnos
- prepara utilizadores e ligacoes a lojas
- carrega horarios, folgas, preferencias e permutas de exemplo
- deixa a aplicacao pronta para demonstracao

## Contas de demonstracao

Todas as contas abaixo usam a password:

```text
123456
```

Contas recomendadas:

- Gerencia com acesso completo: `francisco@levis.com`
- Gerente da loja: `francisco.gomes@levis.com`
- Colaborador para fluxos do funcionario: `henrique.siano@levis.com`

## Apoio a apresentacao

O ficheiro [docs/roteiro-demo.md](docs/roteiro-demo.md) resume:

- contas para login
- ordem sugerida para a demonstracao
- modulos que vale a pena mostrar em cada perfil

## Nota

O script de demonstracao foi pensado para ambiente local de desenvolvimento e apresentacao. Nao deve ser usado em producao porque substitui os dados funcionais atuais.
