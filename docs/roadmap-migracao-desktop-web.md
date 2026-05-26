# Roadmap de Migracao Desktop -> Web (por fases)

Data: 2026-05-20  
Responsavel funcional: equipa Projeto2

## Objetivo

Migrar a experiencia da aplicacao desktop para web sem perder:

- organizacao por modulos;
- regras de permissao por perfil/cargo;
- consistencia visual e de fluxos criticos;
- validacao funcional.

## Mapa de modulos (Desktop -> Web)

- `LoginController` -> `WebLoginController`
- `DashboardController` + shell FXML -> layout web comum (cabecalho/nav + contexto)
- `GeracaoHorariosController` -> `WebHorariosController`
- `PreferenciasController`, `PedirFolgaController`, `PermutasController` -> `WebComplementaresController`
- `PainelGerentePedidosController` -> fase futura web (moderacao/aprovacoes)
- `GestaoLojaController` -> fase futura web (configuracao loja/regras)
- `RelatoriosHorasController` -> fase futura web (consultas e export)
- `PainelAuditoriaController` -> fase futura web (consulta de auditoria)
- `PerfilController` -> fase futura web (gestao de perfil)

## Fase 1 (em curso)

- [x] Base web funcional com auth e sessoes
- [x] Horarios + complementares operacionais
- [x] Correcao de erros criticos (500/overlap)
- [x] Introducao de contexto de layout por perfil (`WebLayoutService`)
- [x] Navegacao web aproximada ao shell do desktop (modulos e visibilidade por permissao)

## Fase 2

- [ ] Criar shell web comum (fragmentos Thymeleaf) para eliminar duplicacao de header/nav
- [ ] Aplicar tokens visuais desktop (espacamento, tipografia, estados de componente)
- [ ] Alinhar naming e microcopy de campos/acoes com os ecras desktop
- [ ] Integrar pagina de perfil web

## Fase 3

- [ ] Migrar Painel de Gerente (aprovacoes pendentes)
- [ ] Migrar Gestao de Loja (regras e horarios especiais)
- [ ] Migrar Relatorios de Horas (com filtros de colaborador)
- [ ] Introduzir testes de regressao web por modulo migrado

## Fase 4

- [ ] Migrar Auditoria para web
- [ ] Consolidar autorizacoes por perfil em middleware/interceptor
- [ ] Endurecer acessibilidade e responsividade
- [ ] Validacao final ponta-a-ponta + checklist de demonstracao

