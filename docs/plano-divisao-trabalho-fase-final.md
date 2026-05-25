# Plano de Divisao de Trabalho (Fase Final)

Este plano serve para fechar o projeto com previsibilidade, sem dois colegas a alterarem o mesmo modulo ao mesmo tempo.

## Estado Atual

- Web com layout principal alinhado ao estilo desktop (login, topbar, sidebar, cards e tabelas).
- Modulos web ativos: painel, horarios, complementares, relatorios, gestao da loja, perfil.
- Funcionalidade web adicional implementada: exportacao CSV de relatorios.

## Divisao Recomendada

### Pessoa A (tu): Frontend Web + UX

- Ajustes finos de layout responsivo em `src/main/resources/static/web/css/app.css`.
- Consistencia visual entre todos os templates em `src/main/resources/templates/web/*`.
- Revisao de textos, labels, mensagens e hierarquia visual das paginas.
- Validacao manual cross-browser (Chrome e Edge) nos fluxos principais.

### Pessoa B (colega): Backend Web + Regras de Negocio

- Endpoints e controladores web em `src/main/java/com/example/projeto2/WEB/*`.
- Reforco de validacoes de dominio (datas, estados, autorizacoes por cargo).
- Revisao de consultas e desempenho em BLL/Repositories para modulos web.
- Cobertura de testes de integracao e regressao.

### Trabalho Partilhado (curto e com pairing)

- Definir Definition of Done por modulo (fluxo feliz + casos de erro).
- Smoke test final: login, horarios, complementares, relatorios, gestao loja, perfil.
- Preparacao da demo final e checklist de submissao.

## Sequencia de Entrega (Sprint Final)

1. Congelar layout base e sidebars (evitar regressao visual).
2. Fechar bugs funcionais por severidade (bloqueador > medio > baixo).
3. Congelar schema e scripts SQL de demo.
4. Passar bateria de testes + smoke manual final.
5. Publicar `main` estabilizada e tag de entrega.

## Regras Operacionais no Git

- Branches curtas por tarefa.
- Pull frequente de `main` antes de comecar.
- Um PR por tema (nao misturar UI com alteracoes de dados).
- Nao fechar uma tarefa sem evidencias (screenshot ou teste).
