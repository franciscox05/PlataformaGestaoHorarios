# Progresso de Refatoração — PlataformaGestaoHorarios

## Fase 1 — Centralizar permissões e utilitários de string ✅ CONCLUÍDA

**Objetivo:** Eliminar duplicação de constantes de cargo, métodos de permissão e `valorOuTraco()` espalhados por 10 serviços.

### O que foi criado

| Ficheiro | Descrição |
|---|---|
| `API/Services/LojautilizadorHelper.java` | Novo `@Component` com constantes `APROVACAO`, `GESTAO`, `VALIDACAO` e 5 métodos cobrindo todos os padrões de uso |

### Serviços migrados para `LojautilizadorHelper`

| Serviço | Constante removida | Repo removido | Métodos privados removidos |
|---|---|---|---|
| `DayOffService` | `CARGOS_COM_APROVACAO` | ✅ | `obterLigacaoAtiva`, `validarPermissaoDeAprovacao` |
| `PermutaService` | `CARGOS_COM_APROVACAO` | ✅ | `obterLigacaoAtiva`, `validarPermissaoDeAprovacao` |
| `PreferenciaService` | `CARGOS_COM_APROVACAO` | — (ainda usado) | `obterLigacaoAtiva`, `validarPermissaoAprovacao` |
| `HorarioService` | — | — | `obterLigacaoAtiva` (delegava) |
| `GestaoFuncionariosService` | `CARGOS_COM_GESTAO_FUNCIONARIOS` | — | `utilizadorPodeGerirFuncionarios`, `obterLigacaoAtivaComPermissao` |
| `GestaoLojaService` | `CARGOS_COM_GESTAO_LOJA` | ✅ | `utilizadorPodeGerirLoja`, `obterLigacaoAtivaComPermissao` |
| `PainelGerenteService` | `CARGOS_COM_PAINEL` | ✅ | `utilizadorPodeAcederPainel`, `obterLigacaoAtivaComPermissao` |
| `SnapshotOperacionalLojaService` | `CARGOS_COM_ACESSO` | — | `obterLigacaoAtivaComPermissao` |
| `RelatorioHorasService` | `CARGOS_COM_RELATORIO` | — | `obterLigacaoAtiva`, `obterLigacaoAtivaComPermissao`, `temPermissaoDeRelatorio` |
| `GeracaoHorariosService` | `CARGOS_COM_GERACAO`, `CARGOS_COM_VALIDACAO` | — | `obterLigacaoAtiva`, `obterLigacaoAtivaComPermissao`, `obterLigacaoAtivaComAcessoAoPainel`, `obterLigacaoAtivaComPermissaoDeValidacao`, `temPermissaoDeGeracao`, `temPermissaoDeValidacao` |

### `valorOuTraco()` centralizado em `HorarioFormatters`

Duplicatas removidas e substituídas por `static import` em:
- `GestaoFuncionariosService`
- `GestaoLojaService`
- `PainelGerenteService`
- `PerfilService`

`HorarioFormatters` (em `geracao/`) já tinha `valorOuTraco`, `normalizarTexto` e `limparTexto` — é agora a fonte canónica.

### Resultado dos testes

```
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0  ✅
```

---

## Fase 2 — Extrair `RegraGeracaoResolver` ✅ CONCLUÍDA

**Objetivo:** Retirar de `GeracaoHorariosService` a lógica de interpretação de regras da loja (parsing NLP de textos de regras → parâmetros de geração), sem impacto em controllers.

### O que foi criado em `geracao/`

| Ficheiro | Descrição |
|---|---|
| `geracao/RegraAplicada.java` | Record público — antes era `private record` dentro de `GeracaoHorariosService` |
| `geracao/ParametrosGeracao.java` | Record público — idem |
| `geracao/PerfilContratual.java` | Enum público com perfis contratuais e regras de compatibilidade |
| `geracao/RegraGeracaoResolver.java` | `@Component` com `obterRegrasAplicadas` + `resolverParametrosGeracao` + 8 helpers `ehRegra*` |

### O que saiu de `GeracaoHorariosService`

- Removidos `RegrasLojaRepository` e `RegraRepository` como dependências directas
- Removidos 12 métodos privados (resolverParametrosGeracao + 8 × ehRegra* + aliasesTurno + obterRegrasAplicadas + regraMenciona*)
- Removidas 3 inner types (`ParametrosGeracao`, `PerfilContratual`, `RegraAplicada`)
- Removida constante `DURACAO_MINIMA_TURNO_TEMPO_INTEIRO_MINUTOS` (movida para `PerfilContratual`)
- **2273 → 1930 linhas (−343)**

### Resultado dos testes

```
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0  ✅
```

---

## Fase 2b — Extrair `MetricasPlaneamentoCalculator` ✅ CONCLUÍDA

**Objetivo:** Retirar de `GeracaoHorariosService` toda a lógica de cálculo de métricas de qualidade de planeamento.

### O que foi criado em `geracao/`

| Ficheiro | Descrição |
|---|---|
| `geracao/MetricasPlaneamento.java` | Record público com campos de métricas — antes `public record` dentro de `GeracaoHorariosService` |
| `geracao/MetricasPlaneamentoCalculator.java` | `@Component` com 3 overloads de `calcular()` + `extrairPolitica()` + helpers privados |
| `geracao/PoliticaOtimizacao.java` | Enum público com 5 políticas e seus pesos — antes `private enum` dentro de `GeracaoHorariosService` |
| `geracao/CargaColaborador.java` | Record público — antes `private record` dentro de `GeracaoHorariosService` |
| `geracao/PlaneamentoGerado.java` | Record público — idem |
| `geracao/EstadoColaboradorResumo.java` | Classe pública — antes `private static final class` dentro de `GeracaoHorariosService` |
| `geracao/HorarioFormatters.java` | Acrescentado `calcularDuracaoEmMinutos(Turno)` estático |

### O que saiu de `GeracaoHorariosService`

- Removidos 5 métodos privados: `calcularMetricasPlaneamento` ×3 + `penalizacaoUtilizacaoContratual` + `extrairPoliticaOtimizacao`
- Removidas 5 inner types: `MetricasPlaneamento`, `PoliticaOtimizacao`, `CargaColaborador`, `PlaneamentoGerado`, `EstadoColaboradorResumo`
- **1930 → 1643 linhas (−287)**

### Resultado dos testes

```
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0  ✅
```

---

## Fase 3a — Extrair `PropostaResultadoBuilder` ✅ CONCLUÍDA

**Objetivo:** Retirar de `GeracaoHorariosService` a construção de `PropostaResultado` e tipos relacionados.

### O que foi criado em `API/Services/`

| Ficheiro | Descrição |
|---|---|
| `PropostaResultadoBuilder.java` | `@Component` com `criarResumoGeracao`, `construirResultado` (×3), `construirResumoProposta`, `rotuloCurtoProposta`, `mapearLinhaHorario`, `construirOrigemPlaneamento` + `ResumoAcumulado` privado |

### O que saiu de `GeracaoHorariosService`

- Removidos 8 métodos privados: `criarResumoGeracao`, `construirResultado` ×3, `construirResumoProposta`, `rotuloCurtoProposta`, `mapearLinhaHorario`, `construirOrigemPlaneamento`
- Removida inner record `ResumoAcumulado`
- Removidos 11 thin-delegate wrappers de formatação (substituídos por static imports de `HorarioFormatters`)
- **1643 → 1399 linhas (−244)**

### Resultado dos testes

```
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0  ✅
```

---

## Fase 3b — Extrair `PropostaPersistenciaHelper` ✅ CONCLUÍDA

**Objetivo:** Retirar de `GeracaoHorariosService` a persistência e decisão de propostas.

### O que foi criado em `API/Services/`

| Ficheiro | Descrição |
|---|---|
| `PropostaPersistenciaHelper.java` | `@Component` com `persistirProposta`, `decidirProposta`, `validarAprovacaoSemConflitos`, `rejeitarPropostasPendentesConcorrentes`, `criarObservacaoHistoricoDecisao` |

### O que saiu de `GeracaoHorariosService`

- Removidos 5 métodos privados: `persistirProposta`, `decidirProposta`, `validarAprovacaoSemConflitos`, `rejeitarPropostasPendentesConcorrentes`, `criarObservacaoHistoricoDecisao`
- **1399 → 1232 linhas (−167)**

### Resultado dos testes

```
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0  ✅
```

---

## Fase 4 — Dividir `PainelGerentePedidosController` ✅ CONCLUÍDA

**Objetivo:** Separar a lógica de folgas, permutas e preferências do controller monolítico em três *sections* Java puras (sem alterações ao FXML — Opção B do `FASE4_INSTRUCOES.md`).

### O que foi criado em `DESKTOP/support/`

| Ficheiro | Descrição |
|---|---|
| `support/PainelPedidosCoordinator.java` | Interface — bridge para o controller pai (id do utilizador, janela, callback pós-acção) |
| `support/FeedbackHelper.java` | Helper estático `mostrar`/`esconder` para labels de feedback (com auto-dismiss 5s) |
| `support/FolgasPainelSection.java` | Encapsula tabela, colunas, botões e `tratar(aprovar)` das folgas |
| `support/PermutasPainelSection.java` | Encapsula tabela, colunas, botões e `tratar(aprovar)` das permutas |
| `support/PreferenciasPainelSection.java` | Encapsula tabela, colunas, textarea, botões e `tratar(aprovar)` das preferências |

### O que saiu de `PainelGerentePedidosController`

- Removidos 3 métodos `configurarTabela*` (Folgas, Permutas, Preferencias) — agora em `Section.configurar()`
- Removidos 3 métodos `tratar*` (Folga, Permuta, Preferencia) — agora em `Section.tratar(aprovar)`
- Removidos `mostrarFeedback`/`esconderFeedback` → `FeedbackHelper`
- Removido campo `nomesFolgasPendentes` (passou a viver dentro de `FolgasPainelSection`)
- Removidos imports: `PauseTransition`, `Duration`, `Bindings`, `TableCell`, `DialogosHelper` e 11 `static` imports de formatação que só são usados pelas sections
- `onAprovarXxxClick`/`onRejeitarXxxClick` simplificados para delegar à respectiva *section*
- Selecção contextual (listeners) e contexto operacional (calendário + tabela colaboradores) permanecem no controller — coordenam as três tabelas
- **916 → 630 linhas (−286)**

### FXML

`painel-gerente-pedidos-view.fxml` **não foi alterado** — Opção B mantém todos os `@FXML` injetados no controller raiz, que apenas passa as referências aos *sections* no `initialize()`.

### Resultado dos testes

```
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0  ✅
```

> ⚠️ Verificação manual desktop (`mvnw spring-boot:run` → ecrã *Gestão de Pedidos*) recomendada antes de fechar a fase: a refatoração toca em listeners de selecção e bindings de botões, e nenhum teste cobre o controller JavaFX.

---

## Fase 5 — Consolidar helpers de UI ✅ CONCLUÍDA

**Objetivo:** Extrair os métodos de formatação privados do `PainelGerentePedidosController` para uma classe utilitária partilhável.

### O que foi criado em `DESKTOP/support/`

| Ficheiro | Descrição |
|---|---|
| `support/PedidosFormatters.java` | Classe `final` com 14 métodos estáticos + 3 constantes (`DATA_FORMATTER`, `DATA_HORA_FORMATTER`, `LOCALE_PT`) |

### O que saiu de `PainelGerentePedidosController`

- Removidos 14 métodos privados de formatação: `formatarData`, `formatarDataHora`, `formatarTipoFolga`, `formatarTipoPreferencia`, `formatarPeriodo`, `formatarVigencia`, `formatarTexto`, `obterNomePermuta`, `obterNomePreferencia`, `formatarTurno`, `formatarTurnosColaborador`, `formatarAusenciasColaborador`, `descreverPedido`, `descreverPeriodoContexto`
- Removidas 3 constantes: `DATA_FORMATTER`, `DATA_HORA_FORMATTER`, `LOCALE_PT`
- Removidos imports `java.time.ZoneId`, `java.time.format.DateTimeFormatter`, `java.util.Locale`
- Substituídos por static imports de `PedidosFormatters`
- **1064 → 913 linhas (−151)**

### Resultado dos testes

```
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0  ✅
```
