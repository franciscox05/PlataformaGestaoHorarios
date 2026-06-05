# Plataforma de Gestao de Horarios

## Relatorio atualizado da entrega desktop e web

### 1. Enquadramento

O projeto **Plataforma de Gestao de Horarios** foi desenvolvido para apoiar a organizacao do trabalho por turnos numa loja Levi's, com foco na realidade operacional da equipa, na comunicacao entre colaboradores e gerencia e na reducao do trabalho manual associado ao planeamento mensal.

Nesta fase final, a plataforma evoluiu para dois canais de utilizacao:

- **desktop** (JavaFX), para fluxo operacional ja consolidado;
- **web** (Spring MVC), para acesso por browser aos fluxos essenciais de autenticacao, horarios e complementares.

O objetivo manteve-se: disponibilizar uma solucao funcional e demonstravel, cobrindo autenticacao, gestao operacional, regras da loja, preferencias, aprovacoes, geracao de horarios e relatorios.

### 2. Objetivos da solucao

Os objetivos principais da solucao foram:

- autenticar diferentes perfis de utilizador com controlo de sessao
- disponibilizar um dashboard com informacao imediata sobre a atividade do colaborador
- permitir a gestao de folgas, permutas e preferencias
- suportar fluxos de aprovacao por parte da gerencia
- permitir a configuracao da loja e das regras operacionais
- apoiar a geracao de propostas mensais de horario
- disponibilizar relatorios mensais de horas
- garantir uma demonstracao estavel com dados de exemplo e empacotamento de entrega
- disponibilizar interface Web para os fluxos nucleares de horarios e complementares

### 3. Tecnologias utilizadas

- **Java 25**
- **Spring Boot 4.0.3**
- **Spring Data JPA**
- **PostgreSQL**
- **JavaFX 21.0.2**
- **Maven**
- **Spring Security Crypto** para hashing de passwords com BCrypt
- **Spring MVC**
- **Thymeleaf**

### 4. Arquitetura da aplicacao

A aplicacao segue uma organizacao por camadas:

- **Views JavaFX / FXML**
  - responsaveis pela interface grafica e interacao com o utilizador
- **Controllers**
  - recebem eventos da interface e coordenam os fluxos de cada ecra
- **BLL**
  - concentram as regras de negocio e validacoes
- **Repositories**
  - suportam o acesso a dados com Spring Data JPA
- **Base de dados PostgreSQL**
  - persiste utilizadores, lojas, horarios, folgas, permutas, preferencias, propostas e auditoria

A integracao entre JavaFX e Spring Boot permite criar controllers como beans do Spring, o que simplifica a injecao de dependencias e a separacao entre interface e logica de negocio.

Na vertente Web, a mesma camada de negocio (BLL) e repositorios e reutilizada pelos controllers Spring MVC, reduzindo duplicacao de regras entre canais.

### 5. Modelo de dados principal

As principais entidades persistidas sao:

- `Utilizador`
- `Cargo`
- `Loja`
- `Lojautilizador`
- `Turno`
- `Horario`
- `HistoricoHorarioEstado`
- `DayOff`
- `Permuta`
- `Preferencia`
- `Regra`
- `RegrasLoja`
- `PropostaHorarioMensal`
- `EventoAuditoria`

Estas entidades suportam:

- associacao entre colaboradores, lojas e cargos
- configuracao de regras por loja
- atribuicao de turnos e historico de estados
- pedidos de folga, permutas e preferencias
- aprovacao por parte da gerencia
- auditoria de eventos relevantes de autenticacao e sessao

### 6. Funcionalidades implementadas

#### 6.1 Autenticacao e sessao

- login com email e password
- validacao de utilizadores ativos
- hashing de passwords com BCrypt
- migracao transparente de passwords antigas em texto simples
- controlo de sessao e expiracao por inatividade
- registo de eventos de auditoria de login, logout e expiracao de sessao

#### 6.2 Dashboard e experiencia do colaborador

- dashboard principal com navegacao lateral
- visualizacao dos proximos turnos
- painel de equipa do dia
- acesso rapido aos modulos relevantes consoante o perfil autenticado

#### 6.3 Gestao da loja e regras

- definicao de hora de abertura e fecho
- parametrizacao de regras operacionais por loja
- configuracao de valores especificos e observacoes

#### 6.4 Gestao de funcionarios

- listagem da equipa
- criacao de colaboradores
- edicao de dados principais
- alteracao de estado
- associacao a loja e cargo

#### 6.5 Perfil do utilizador

- consulta de dados pessoais
- edicao de nome, email e telemovel
- alteracao de password
- consulta de indicadores do proprio perfil

#### 6.6 Folgas

- submissao de pedidos de folga
- historico de pedidos
- validacao de datas
- aprovacao e rejeicao por parte de perfis de gestao

#### 6.7 Permutas

- pedido de permuta entre turnos elegiveis
- validacao por loja e contexto
- historico de pedidos
- aprovacao e rejeicao por parte da gerencia

#### 6.8 Preferencias

- submissao de preferencias de `folgas`, `ferias`, `colegas` e `turnos`
- definicao de prioridade
- historico e estado de cada preferencia
- aprovacao e rejeicao por parte da gerencia

#### 6.9 Painel unificado da gerencia

- visualizacao centralizada de pedidos pendentes
- consulta de folgas, permutas e preferencias no mesmo ecra
- decisao rapida com atualizacao de estados

#### 6.10 Geracao de horarios

- geracao de proposta mensal de horario
- suporte a regras da loja e restricoes operacionais
- integracao com preferencias aprovadas
- validacao por supervisor

#### 6.11 Relatorios mensais

- consulta mensal de horas por colaborador
- filtros por mes, ano e utilizador
- exportacao para CSV

#### 6.12 Entrega desktop

- script demo para repor um ambiente de apresentacao estavel
- documentacao de demonstracao
- empacotamento ZIP com JAR executavel, configuracao, SQL e scripts de arranque

#### 6.13 Aplicacao Web

- login/logout Web com sessao HTTP
- pagina de consulta de planeamento mensal
- geracao de proposta mensal de horarios via browser
- listagem de alternativas geradas para o periodo
- fluxos complementares completos via Web:
  - submissao e historico de folgas
  - submissao e historico de preferencias
  - submissao dinamica de permutas via API REST com selects de dois passos
  - aprovacao e rejeicao de folgas, preferencias e permutas pelo gerente
- API REST de suporte (`/api/permutas/*`) com tres endpoints:
  - `GET /api/permutas/meus-turnos` — lista turnos aprovados do utilizador logado
  - `GET /api/permutas/turnos-elegiveis` — lista turnos de colegas elegiveis para troca
  - `POST /api/permutas/submeter` — submete pedido validando regras e `EstadoPermuta`
- formulario de permutas com UX dinamica:
  - selects carregados por fetch sem recarregar pagina
  - previews do turno de origem e destino antes de submeter
  - feedback inline de sucesso e erro
  - submissao assincrona com reload automatico apos confirmacao
- reutilizacao da BLL existente para garantir consistencia de regras entre desktop e Web
- testes automatizados E2E: 15 testes a passar (5 E2E + 10 de integracao)

### 7. Mapeamento resumido de requisitos

| Requisito | Estado |
| --- | --- |
| RFU01 - Definir horario da loja | Implementado |
| RFU02 - Horarios especiais por epoca/data | Trabalho futuro planeado |
| RFU03 - Adicionar funcionarios | Implementado |
| RFU04 - Alterar dados de funcionarios | Implementado |
| RFU05 - Desativar funcionarios | Implementado |
| RFU06 - Aprovar permutas | Implementado |
| RFU07 - Aprovar preferencias | Implementado |
| RFU08 - Definir minimo de funcionarios | Implementado |
| RFU09 - Definir dia de lancamento do horario | Implementado |
| RFU10 - Preferencias de colegas | Implementado |
| RFU12 - Preferencias de folgas | Implementado |
| RFU13 - Preferencias de ferias | Implementado |
| RFU14 - Pedir troca de turno | Implementado |
| RFU15 - Validar proposta de horario | Implementado |

### 8. Dados de demonstracao

O projeto inclui o script `sql/demo-entrega.sql`, pensado para preparar um ambiente de demonstracao consistente.

Esse script:

- atualiza a estrutura necessaria das features mais recentes
- limpa os dados funcionais
- recarrega lojas, cargos, regras e turnos
- prepara utilizadores de varios perfis
- cria horarios, folgas, preferencias e permutas de exemplo
- deixa a estrutura pronta para geracao de propostas mensais

Contas recomendadas para demonstracao:

- `francisco@levis.com`
- `francisco.gomes@levis.com`
- `henrique.siano@levis.com`

### 9. Validacao realizada

Para esta entrega foram validados:

- arranque da aplicacao desktop
- autenticacao com diferentes perfis
- navegacao entre os ecras principais
- fluxos de colaborador e fluxos de gestao
- geracao de horarios
- relatorios mensais
- seguranca basica, timeout de sessao e auditoria
- geracao do ZIP de entrega desktop
- arranque da aplicacao a partir do ZIP extraido, fora do IntelliJ
- arranque da aplicacao Web e fluxos complementares por browser

### 9.1 Evidencias visuais

Foram atualizadas evidencias visuais de apoio ao documento, incluindo:

- captura do ecra de autenticacao da aplicacao desktop
- captura da estrutura do pacote ZIP extraido para entrega

### 10. Estrutura da entrega

A entrega final passa a incluir:

- codigo-fonte do projeto
- script SQL de demonstracao
- documentacao de apoio
- ZIP de entrega desktop gerado por Maven
- ZIP de entrega Web gerado por Maven

O ZIP final integra:

- `Projeto2.jar`
- `application.properties`
- scripts de arranque para Windows
- ficheiros SQL de apoio
- documentacao de execucao

Para a vertente Web, o ZIP integra adicionalmente:

- `Projeto2-web.jar`
- scripts de arranque Web
- documentacao especifica da execucao Web

### 11. Trabalho futuro

Mesmo com a aplicacao desktop e Web funcionais para entrega, continuam identificadas algumas frentes de evolucao:

- gestao de horarios especiais e excecoes por data
- painel de auditoria com visualizacao historica
- extensao da interface Web ao modulo de gestao de funcionarios

### 12. Conclusao

A entrega final da **Plataforma de Gestao de Horarios** apresenta um conjunto funcional coerente com o objetivo do projeto, cobrindo autenticacao, operacao de colaborador, fluxos de gerencia, geracao de horarios, relatorios, complementares (folgas, preferencias e permutas) e empacotamento de entrega em desktop e Web.

O resultado final demonstra uma aplicacao organizada por camadas, integrada com Spring Boot, JavaFX, Spring MVC e PostgreSQL, pronta para demonstracao e preparada para evolucao futura.
