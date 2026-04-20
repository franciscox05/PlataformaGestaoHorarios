# Validacao final da entrega desktop

Este ficheiro resume a validacao manual e tecnica feita para a entrega desktop.

## Ambiente usado

- Sistema: Windows
- Base de dados: PostgreSQL local
- Aplicacao: JavaFX + Spring Boot + Spring Data JPA
- Compilacao: Maven com Java 24

## Preparacao validada

1. Reexecucao do script [demo-entrega.sql](../sql/demo-entrega.sql).
2. Confirmacao das contas demo recomendadas no [README.md](../README.md).
3. Compilacao da aplicacao com `mvn -DskipTests compile`.

## Fluxos validados na demo

- login com perfil de gestao
- login com perfil de colaborador
- dashboard com proximos turnos e equipa de hoje
- gestao da loja e das regras
- gestao de funcionarios
- preferencias do funcionario
- aprovacao de preferencias
- geracao de proposta mensal de horarios
- relatorios mensais de horas
- pedidos de folga e historico
- permutas de turnos
- perfil do utilizador e edicao de dados

## Ajustes finais incluidos nesta issue

- melhoria dos estados vazios e das mensagens de feedback
- refinamento visual do login
- consistencia visual nos modais do perfil
- limpeza de mensagens de consola evitaveis em fluxos de interface
- documentacao de apoio para demonstracao e entrega

## Observacoes

- a password continua a seguir a logica atual do projeto; reforcos de seguranca mais profundos pertencem a uma frente propria
- este ficheiro serve como apoio rapido para verificar se a entrega desktop esta pronta para demonstracao
