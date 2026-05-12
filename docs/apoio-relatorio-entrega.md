# Apoio a atualizacao do relatorio

Este documento resume o que deve ficar refletido no relatorio PDF atualizado da entrega desktop e Web.

O documento final preparado para submissao passa a estar materializado em:

- [relatorio-final-entrega.md](relatorio-final-entrega.md)
- [relatorio-final-entrega.html](relatorio-final-entrega.html)
- `Projeto2_EI_33400_33397_relatorio_atualizado.pdf`

## Objetivo da aplicacao

Disponibilizar uma plataforma de gestao de horarios por turnos numa loja Levi's, com canal desktop e Web, cobrindo autenticacao, operacao diaria, planeamento e fluxos de colaborador e gerencia.

## Tecnologias usadas

- Java
- Spring Boot
- Spring Data JPA
- PostgreSQL
- JavaFX
- Spring MVC
- Thymeleaf
- Maven

## Funcionalidades implementadas

- autenticacao e acesso ao dashboard
- visualizacao de proximos turnos
- painel de equipa de hoje
- gestao da loja e das regras operacionais
- gestao de funcionarios
- pedidos de folga com historico e aprovacao
- permutas de turnos com aprovacao
- preferencias do funcionario e aprovacao pela gerencia
- geracao de propostas mensais de horarios
- relatorios mensais de horas
- perfil do utilizador e edicao de dados pessoais
- login/logout Web
- consulta Web de planeamento mensal
- geracao Web de proposta mensal de horarios
- listagem Web de alternativas por periodo

## Aspetos de demonstracao

- existem contas demo para perfis de gestao e colaborador
- existe um script de base de dados para repor o ambiente de apresentacao
- existe um roteiro de demonstracao para orientar a ordem de exibicao dos modulos

## Aspetos tecnicos a referir no relatorio

- arquitetura por camadas com persistencia, BLL, controllers e views JavaFX
- integracao entre Spring Boot e JavaFX
- uso de JPA para mapear entidades e repositórios
- separacao entre fluxos de colaborador e fluxos de gestao
- configuracao de dados demo para suporte a testes e apresentacao

## Capitulo recomendado no PDF atualizado

Vale a pena acrescentar ou rever um capitulo com:

1. funcionalidades da aplicacao desktop e Web
2. tecnologias usadas
3. estrutura da base de dados e principais entidades
4. principais fluxos de utilizacao
5. evidencias da demonstracao
6. trabalho futuro
