# Fecho de Issues Web (taigueis)

Data: 2026-05-19  
Responsavel: `taigueis`

## Issue #72 - Estrutura base do modulo WEB e arranque da aplicacao

Link: https://github.com/franciscox05/PlataformaGestaoHorarios/issues/72

### Estado
- Concluida.

### Entrega realizada
- Estrutura base WEB operacional com controladores e templates dedicados.
- Arranque local WEB estabilizado com `Projeto2WebApplication`.
- Separacao de perfis de execucao desktop/web para evitar arranque simultaneo indevido.
- Rota inicial funcional: `/web/login`.
- Build/teste sem regressao critica no fluxo validado.

### Evidencias
- `WEB/README.md` com instrucoes de execucao.
- `src/main/java/com/example/projeto2/WEB/*`
- `src/main/resources/templates/web/*`

---

## Issue #73 - Autenticacao e contexto de utilizador na interface Web

Link: https://github.com/franciscox05/PlataformaGestaoHorarios/issues/73

### Estado
- Concluida.

### Entrega realizada
- Login/logout implementados.
- Sessao HTTP com contexto de utilizador (id/nome) e protecao de paginas.
- Redirecionamento de nao autenticado para `/web/login`.
- Feedback de credenciais invalidas no login.
- Validacao automatizada de sessao e acesso.

### Evidencias
- `src/main/java/com/example/projeto2/WEB/WebLoginController.java`
- `src/main/java/com/example/projeto2/WEB/WebSession.java`
- `src/test/java/com/example/projeto2/WebAccessIntegrationTest.java`

---

## Issue #75 - Funcionalidades complementares Web (preferencias, folgas, permutas)

Link: https://github.com/franciscox05/PlataformaGestaoHorarios/issues/75

### Estado
- Concluida.

### Entrega realizada
- Fluxos MVP implementados:
  - registo de preferencias,
  - registo de folgas,
  - submissao de permutas.
- Historicos de preferencias/folgas/permutas com listagem.
- Validacoes de negocio com mensagens claras ao utilizador.
- Correcao de bug 500 em complementares (formato de `dataPedido`).
- Refinamento visual e organizacional da pagina.

### Evidencias
- `src/main/java/com/example/projeto2/WEB/WebComplementaresController.java`
- `src/main/resources/templates/web/complementares.html`
- `src/main/resources/static/web/css/app.css`

---

## Issue #76 - Testes de regressao desktop + validacao funcional Web

Link: https://github.com/franciscox05/PlataformaGestaoHorarios/issues/76

### Estado
- Concluida (com evidencias documentadas no repositorio).

### Entrega realizada
- Teste automatizado executado e verde:
  - `mvnw.cmd -Dtest=WebAccessIntegrationTest test`
  - Resultado: `BUILD SUCCESS`, `Tests run: 9, Failures: 0, Errors: 0`
- Checklist manual desktop e web registada.
- Bugs criticos encontrados corrigidos:
  - 500 em `/web/complementares`;
  - sobreposicao visual em `/web/horarios`.
- Registo explicito de apto para submissao.

### Evidencias
- `docs/validacao-entrega-web-desktop.md`

---

## Comentario curto sugerido para publicar em cada issue

```text
Implementacao concluida e validada. Evidencias e ficheiros alterados documentados em `docs/fecho-issues-web.md` e `docs/validacao-entrega-web-desktop.md`.
Estado: apto para submissao.
```
