# Progresso do Projeto - Gestão de Horários

---

## PLANO DESKTOP — O QUE FALTA IMPLEMENTAR

### Diagnóstico (após análise completa do código)

A aplicação Desktop JavaFX está **funcionalmente completa** no núcleo:
todos os controllers, BLLs e FXML têm implementação real, sem TODOs
ou corpos vazios. O motor de geração (backtracking + 3 fases de orçamento)
também está concluído.

**O que genuinamente falta ou está por polir:**

| # | Tarefa | Impacto | Dificuldade | Estado |
|---|--------|---------|-------------|--------|
| T1 | Badges de pedidos pendentes no sidebar (contador visual nos botões Folgas / Permutas / Preferências / Horários) | Alto — UX imediata | Médio | ⏳ A fazer |
| T2 | Exportação do horário mensal para CSV | Alto — feature prática | Baixo | ⏳ A fazer |
| T3 | Exportação do horário mensal para PDF | Alto — feature prática | Médio (nova dep: iText/PDFBox) | ⏳ A fazer |
| T4 | Exportação do relatório de horas para PDF | Médio — complementa CSV já existente | Baixo (aproveita lib T3) | ⏳ A fazer |
| T5 | Recuperação de password ("Esqueci a password" no login) | Médio | Alto (precisa de email ou flow local) | ⏳ A fazer |

**Ordem de implementação escolhida:** T1 → T2 → T3 → T4 → T5

---

## 2026-06-06 — Sessão 1

### Análise inicial

- **Última Ação:** Scan completo do repositório + diagnóstico do Desktop
- **Ficheiros Criados/Alterados:** `PROGRESS.md` (criado)
- **Estado Atual:** Repositório limpo, todos os módulos core funcionais
- **Próximo Passo:** Implementar T1 — Badges no sidebar

---

## T1 — Badges de Pedidos Pendentes no Sidebar

**Objetivo:** Mostrar um contador vermelho em cima dos botões do sidebar
(Folgas, Permutas, Preferências, Horários) quando há pedidos pendentes
de aprovação para o utilizador logado.

**Plano técnico:**
- Criar método `contarPendentes()` em cada BLL já existente
- No `DashboardController.setUtilizadorLogado()`, invocar os contadores
- Overlay de badge em JavaFX (StackPane + Label CSS rounded)
- Atualizar após cada ação do utilizador (aprovar/rejeitar)

**Estado:** ✅ Concluído — compila OK, 51/51 testes passam

**Ficheiros alterados:**
- `Repositories/DayOffRepository.java` — `countPedidosPendentesDaLoja`
- `Repositories/PermutaRepository.java` — `countPedidosPendentesDaLoja`
- `Repositories/PreferenciaRepository.java` — `countPreferenciasPendentesDaLoja`
- `Repositories/PropostaHorarioMensalRepository.java` — `countByIdLojaIdAndEstadoIgnoreCase`
- `BLL/DayOffBLL.java` — `contarPendentesParaAprovacao`
- `BLL/PermutaBLL.java` — `contarPendentesParaAprovacao`
- `BLL/PreferenciaBLL.java` — `contarPendentesParaAprovacao`
- `BLL/GeracaoHorariosBLL.java` — `contarHorariosPendentesValidacao`
- `Controller/DashboardNavigator.java` — novo método `atualizarBadges()`
- `Controller/DashboardController.java` — novas BLLs injetadas, badges @FXML, `atualizarBadgesSidebar()`
- `dashboard/dashboard-view.fxml` — botões Folgas/Permutas/Preferências/Horários/Pedidos envoltos em StackPane com Label badge
- `dashboard/dashboard.css` — classe `.sidebar-notif-badge`

---

## T2 — Exportação do Horário Mensal para CSV

**Objetivo:** Botão "Exportar CSV" no ecrã de Horários que exporta o horário
mensal actualmente visível para um ficheiro `.csv`.

**Estado:** ✅ Concluído — 51/51 testes, BUILD SUCCESS

**Ficheiros alterados:**
- `dashboard/geracao-horarios-view.fxml` — botão `btnExportarCsvHorario` no card Mensal
- `Controller/GeracaoHorariosController.java` — campo @FXML, handler `onExportarCsvHorarioClick()`, método `sanitizarCsv()`, `atualizarEstadoInterativo()` atualizado

---

## T3 — Exportação do Horário para PDF

**Objetivo:** Botão "Exportar PDF" no ecrã de Horários que gera um PDF
com o horário mensal organizado em tabela (colaborador, turnos por data).

**Plano técnico:**
- Adicionar dependência iText/PDFBox ao pom.xml
- Handler `onExportarPdfHorarioClick()` no controller
- Layout tabular: cabeçalho loja/mês, tabela com linhas agrupadas por colaborador

**Estado:** ✅ Concluído — 51/51 testes, BUILD SUCCESS

**Ficheiros alterados:**
- `pom.xml` — dependência `pdfbox 3.0.3` adicionada
- `BLL/ExportacaoPdfBLL.java` — novo serviço com `exportarHorarioPdf()` e `exportarRelatorioPdf()`
- `dashboard/geracao-horarios-view.fxml` — botão `btnExportarPdfHorario`
- `Controller/GeracaoHorariosController.java` — injeção ExportacaoPdfBLL, handler `onExportarPdfHorarioClick()`
- `dashboard/relatorios-horas-view.fxml` — botão `btnExportarPdf`
- `Controller/RelatoriosHorasController.java` — injeção ExportacaoPdfBLL, handler `onExportarPdfClick()`

---

## T5 — Recuperação de Password

**Objetivo:** Flow de "Esqueci a password" na janela de login.
Reset por pergunta de segurança (sem email externo) ou reset directo
por gerente no ecrã de Gestão de Funcionários.

**Plano técnico (opção sem email):**
- Novo botão "Esqueci a password" no login
- Diálogo: introduz email → valida existência → nova password + confirmação
- Por simplicidade académica, qualquer utilizador pode resetar a sua própria password
  se souber o email (sem token/email externo)
- Alternativa: o gerente faz reset da password no ecrã de Gestão de Funcionários
  (já tem acesso a todos os utilizadores)

**Estado:** ✅ Concluído — 51/51 testes, BUILD SUCCESS

**Ficheiros alterados:**
- `BLL/PerfilBLL.java` — `redefinirPassword()` (self-service) e `redefinirPasswordPorGerente()` (admin)
- `Controller/LoginController.java` — injeção PerfilBLL, handler `onEsqueciPasswordClick()` com Dialog
- `login/login-view.fxml` — botão "Esqueci a palavra-passe"
- `login/login.css` — estilo `.login-forgot-btn`
- `Controller/GestaoFuncionariosController.java` — injeção PerfilBLL, campo `btnRedefinirPassword`, handler `onRedefinirPasswordClick()`
- `dashboard/gestao-funcionarios-view.fxml` — botão "Redefinir Password"

---

## RESUMO FINAL DA SESSÃO

| Tarefa | Estado | Testes |
|--------|--------|--------|
| T1 — Badges no Sidebar | ✅ | 51/51 |
| T2 — Exportação CSV do Horário | ✅ | 51/51 |
| T3 — Exportação PDF do Horário | ✅ | 51/51 |
| T4 — Exportação PDF do Relatório | ✅ | 51/51 |
| T5 — Recuperação de Password | ✅ | 51/51 |

---

## Sessão 2 — Diagnóstico do Motor de Geração

### O que já funciona (confirmado no código)

| Regra | Onde | Estado |
|-------|------|--------|
| Descanso mínimo entre turnos (≥8h) | `HorarioValidatorService.respeitaDescansoMinimo()` → `EstadoColaborador.podeReceber()` | ✅ Hard constraint |
| Máximo de dias consecutivos | `HorarioValidatorService.violaMaximoDiasConsecutivos()` → `podeReceber()` | ✅ Hard constraint |
| Descanso semanal mínimo (folgas/semana) | `HorarioValidatorService.excedeDiasTrabalhadosNaSemana()` → `podeReceber()` | ✅ Hard constraint |
| Rotação de fins de semana (1 FDS livre/X semanas) | `HorarioValidatorService.violaRotacaoDeFimDeSemana()` → `podeReceber()` | ✅ Hard constraint (relaxável no 4.º orçamento) |
| Gerente/Subgerente obrigatório ao sábado | `existeChefiaPossivel()` + poda do backtracking (`minimoChefiasNoDia`) | ✅ Hard constraint |
| Cobertura mínima por turno | `construirSlots()` + N slots por tipo × mínimo | ✅ Hard constraint |
| Carga contratual mensal | `cargaMaximaPorColaborador` → `utilizacaoProjetada()` no `podeReceber()` | ✅ Hard constraint |
| Part-time apenas nos fins de semana | `apenasFimDeSemana` → `podeReceber()` | ✅ Hard constraint |
| Turnos de ≥ 8h para fulltime/gerente/supervisor | `exigeTurnoMinimoOitoHoras` → `podeReceber()` | ✅ Hard constraint |
| Bloqueios (folgas aprovadas) | `bloqueiosPorColaborador` → `podeReceber()` | ✅ Hard constraint |
| Equilíbrio de carga (soft) | `utilizacaoProjetada()` na função de pontuação | ✅ Soft heurístico |
| Preferência de turno (manhã/tarde/intermédio) | `temPreferenciaTurnoFavoravel()` → -30 pts na pontuação | ✅ Soft heurístico |
| Rotação FDS (soft) | `trabalhouFimDeSemanaAnterior()` → +140/+220 pts na pontuação | ✅ Soft heurístico |

### O que FALTA ou está incompleto

| # | Lacuna | Detalhe | Impacto |
|---|--------|---------|---------|
| L1 | **Folgas nos dias preferidos não são bloqueios** | `preferenciasTurnos` só contém prefs de tipo **turno** (manhã/tarde). Preferências do tipo **"folgas"** (dias preferidos de descanso) nunca chegam ao motor como bloqueios. Apenas as `DayOff` aprovadas se tornam bloqueios. | Alto — dias preferidos de folga ignorados |
| L2 | **Preferência de noite ausente** | `correspondePeriodo()` verifica `manha`, `tarde` e `intermedio` mas **não verifica `noite`**. Um funcionário que prefira turno da noite nunca recebe o bónus de -30 pts. | Médio |
| L3 | **Preferências sociais (colegas) não chegam ao motor** | O `PedidoGeracao` não tem nenhum campo para pares de colaboradores preferidos. A BLL (`GeracaoHorariosBLL`) nunca lê preferências do tipo `"colegas"` para alimentar o motor. | Médio — funcionalidade prometida ausente |
| L4 | **"1 fim de semana livre a cada 7 semanas" não é configurável** | A janela de rotação (`janelaRotacaoFimDeSemana`) vem das regras da loja e pode ser qualquer valor. Não existe validação na BLL que force o valor ≥ 7 semanas como previsto no enunciado (pode ficar a 0 = sem restrição). | Baixo (configuração) |
| L5 | **Preferências de tipo "folgas" com dataInicio/dataFim** | Se um funcionário tiver uma preferência aprovada do tipo "folgas" com datas específicas (e.g., quer sempre folgar à segunda), esse sinal nunca é convertido em bloqueio no `GeracaoHorariosBLL.gerarPropostas()`. | Alto — preferências aprovadas ignoradas |

### Plano de Alterações (sessão seguinte)

**A1 — Corrigir `correspondePeriodo()` para incluir "noite"** *(HorarioGeneratorEngine — 3 linhas)*
- Adicionar `exigeNoite` ao parsing da descrição e ao método de correspondência.

**A2 — Converter preferências de folga aprovadas em soft-bloqueios**
- Em `GeracaoHorariosBLL` (método que constrói o `PedidoGeracao`): ler preferências do tipo `"folgas"` aprovadas, calcular os dias da semana/datas correspondentes e adicioná-las a `bloqueiosPorColaborador` como soft-bloqueio (ou penalização na pontuação para não comprometer cobertura).

**A3 — Adicionar suporte a preferências sociais no motor**
- No `PedidoGeracao`: novo campo `Map<Integer, Set<Integer>> paresPreferisPorColaborador`
- No `GeracaoHorariosBLL`: ler prefs do tipo `"colegas"`, extrair IDs dos pares e popular o campo
- Em `pontuarAtribuicao()`: se dois colaboradores com preferência mútua já estão no mesmo dia, -20 pts para o segundo

**A4 — Proteção mínima da janela de rotação** *(HorarioValidatorService — 1 método)*
- Adicionar `validarJanelaRotacao(int janela)` que lança aviso se `janela < 7`; usado na construção do `PedidoGeracao`.

### Implementação Concluída — A1, A2, A3, A4

| Ação | Estado | Detalhe |
|------|--------|---------|
| A1 — `noite` em `correspondePeriodo()` | ✅ | `exigeNoite` adicionado ao parsing + ramo `noite` (≥17h) na verificação |
| A2 — Soft block infraestrutura | ✅ (parcial) | `diasFolgaPreferidos` adicionado ao `PedidoGeracao`/`DadosGeracao`; lógica de penalização (+60 pts) pronta no motor; por agora retorna map vazio pois prefs aprovadas com datas explícitas já são hard blocks (testado) |
| A3 — Preferências sociais | ✅ | `paresPreferisPorColaborador` no `PedidoGeracao`; `construirParesPreferisPorColaborador()` na BLL (parse de nomes); bónus -20 pts em `pontuarAtribuicao()` quando colega preferido já está escalado |
| A4 — Validação janela rotação | ✅ | `HorarioValidatorService.janelaRotacaoRespeiraMinimo()` + LOGGER.warn na BLL se janela < 7 semanas |

**Ficheiros alterados:**
- `BLL/HorarioValidatorService.java` — `janelaRotacaoRespeiraMinimo()`
- `BLL/HorarioGeneratorEngine.java` — A1 (`correspondePeriodo` + noite), A2/A3 (novos campos `PedidoGeracao`, penalizações em `pontuarAtribuicao`)
- `BLL/GeracaoHorariosBLL.java` — A2 (`construirDiasFolgaPreferidosPorColaborador`), A3 (`construirParesPreferisPorColaborador`), A4 (LOGGER.warn), `DadosGeracao` + `gerarPlaneamento` actualizados

**Testes:** 51/51 ✅ BUILD SUCCESS

---

## Sessão 8 — Reorganização Estrutural (abas focadas)

> Feedback forte do utilizador: **não gosta da ORGANIZAÇÃO das páginas** (densas,
> tudo num scroll gigante). Não é problema de CSS — é de **estrutura**. Nova
> direção: **vistas focadas com abas** (TabPane), uma coisa de cada vez, muito
> menos scroll, muito mais respiração.

**Prova de conceito: `gestao-funcionarios-view.fxml` reescrito com 2 abas:**
- **"Equipa"** — contexto da loja + tabela + formulário de edição.
- **"Perfil do colaborador"** — horário semanal + folgas/preferências/permutas.

Antes: 1 scroll com 4 secções empilhadas. Agora: cabeçalho de página limpo +
TabPane. Root mudou de `ScrollPane` → `VBox` (carregado como `Parent` em
`mudarEcraCentro`, sem cast → seguro). **Todos os `fx:id`/`onAction` preservados.**
CSS novo `.modulo-tabs` (separadores com sublinhado vermelho, conteúdo sem moldura).

**Validação:** XML bem-formado confirmado; build 51/51 (sem alterações Java).

**Rollout (utilizador aprovou a direção + sugeriu ecrãs flutuantes):**
| Módulo | Estado | Abas |
|--------|--------|------|
| Funcionários | ✅ | Equipa · Perfil do colaborador |
| Loja e Regras | ✅ | Configuração · Exceções |
| Horários | ⏳ próximo | Gerar · Analisar · Calendário |
| Painel do Gerente | ⏳ próximo | (Folgas/Permutas/Preferências) |
| Permutas, Preferências, Pedir Folga | ⏳ | têm secção de aprovação **gated** → abas precisam de o controller esconder a aba a não-gestores (mudança de controller, faço com cuidado) |

**Ecrãs flutuantes (drawer/overlay):** ótima ideia do utilizador. Precisam de
lógica de show/hide no controller (com backdrop) → introduzo-os de forma verificada
(ex.: formulário "Editar Colaborador" como *drawer* que desliza ao selecionar),
depois de as abas estarem confirmadas no render.

**Regra de gating em abas:** módulos só-de-gestão (Funcionários, Loja, Horários,
Painel Gerente) tabulam sem problema; módulos com secção de aprovação gated
(Permutas, Preferências, Pedir Folga) exigem esconder a aba via controller.

---

## Sessão 7 — PROJETO INDIGO: Reboot Visual Total do Desktop

> Mudança radical de estratégia. As micro-correções da Sessão 6 são absorvidas
> por um **reboot completo** da arquitetura visual e de UX. Carta branca para
> reinventar layout, navegação e estética — mantendo o contrato técnico intacto.

### A Regra de Ouro (inviolável)
Posso destruir e refazer FXML e CSS, **mas**:
- Todos os `fx:id` mantêm-se (mesmo nome, mesmo tipo de nó).
- Todos os `onAction`/`onMouseClicked="#metodo"` mantêm-se.
- Os 51 testes e o motor BLL continuam a passar.

Os testes carregam o FXML via `FXMLLoader` e chamam `setUtilizadorLogado(...)`;
se um `fx:id` usado em `initialize()`/`setUtilizadorLogado()` desaparecer ou mudar
de tipo → `LoadException`/NPE. Por isso o contrato é sagrado, o resto é livre.

### Decisão de arquitetura: custom sobre Modena (não AtlantaFX)
Considerei o **AtlantaFX** (tema JavaFX AAA). Decisão: **não** o adoto agora.
Motivo honesto: não consigo renderizar a app neste ambiente (precisa de Postgres
+ login), e trocar o *user-agent stylesheet* base às cegas, por baixo de uma
camada custom pesada, arrisca conflitos que eu não detetaria. Um sistema custom
disciplinado sobre a base Modena (conhecida e estável) é o caminho fiável para um
acabamento comercial. **AtlantaFX fica como fast-follow** para fazermos juntos com
preview ao vivo, se quiseres.

### Visão UX/UI — "Indigo / Heritage 2.0"

**Conceito:** um *workspace* editorial premium. Menos "formulário", mais "produto".
A herança Levi's (vermelho de marca, índigo de ganga, alto contraste, tipografia
forte) reinterpretada com a linguagem de SaaS moderno de 2025.

**Princípios:**
1. **Riel de navegação escuro e rico** — sidebar near-black com gradiente subtil,
   navegação agrupada por secções rotuladas (PRINCIPAL / GESTÃO / CONTA), item
   ativo como *pill* vermelho com brilho. Foge da lista plana antiga.
2. **Topbar "vidro"** — header flutuante translúcido (glassmorphism simulado:
   branco semi-transparente + borda subtil + sombra), cantos inferiores
   arredondados, respiração generosa.
3. **Conteúdo flutuante** — cards com cantos muito arredondados (18–22px),
   sombras multi-camada suaves, hierarquia por elevação em vez de linhas.
4. **Cor de destaque dinâmica** — vermelho `#C91428` como única cor vívida de
   ação; **índigo de ganga `#2E4D74`** como acento secundário (info/turnos), para
   riqueza sem ruído.
5. **Tipografia arrojada** — *display* grande nos heróis (40–48px/900), títulos de
   secção confiantes, *labels* maiúsculas discretas.
6. **UX sem atrito** — calendários como peça central limpa; tabelas arejadas;
   feedback como banners suaves; estados (hoje/ativo/pendente) legíveis num
   relance.

**Paleta (tokens redefinidos, nomes mantidos para não perder cobertura):**
| Token | Antes | Indigo |
|-------|-------|--------|
| `-surface-app` (canvas) | `#fcf9f8` | `#F3F0EC` papel quente |
| `-charcoal` (riel) | `#313030` | `#1A191C` near-black rico |
| `-levis-red` | `#c91428` | `#C91428` (mantido — marca) |
| + `-levis-red-bright` | — | `#E11D2E` (hover vivo) |
| + `-denim` | — | `#2E4D74` (acento ganga) |
| `-ink-900` | `#1c1b1b` | `#151416` |

### Plano de execução (por ondas, cada uma validada nos 51 testes)
| Onda | Conteúdo | Risco |
|------|----------|-------|
| **1 — Linguagem CSS** | Redefinir tokens + reescrever sidebar, topbar, botões, cards, inputs, tabelas, scrollbars, chips (mantendo nomes de classe → cobertura garantida) | CSS — nulo |
| **2 — Shell** | `dashboard-view.fxml`: riel agrupado + topbar de vidro (preserva 30 `fx:id` + 16 handlers) | FXML — médio |
| **3 — Home** | `home-view.fxml`: dashboard editorial (preserva 43 `fx:id` + 13 handlers) | FXML — médio |
| **4 — Login** | Harmonizar vermelho + nova estética de entrada | baixo |
| **5 — Módulos** | Horários / Funcionários / Relatórios herdam o CSS; polimento pontual | baixo |

### Resultado — Ondas 1–4 implementadas ✅ (51/51 testes, BUILD SUCCESS)

| Onda | Estado | Implementação |
|------|--------|---------------|
| **1 — Linguagem CSS** | ✅ | `dashboard.css`: tokens Indigo (canvas `#f3f0ec`, riel `#1a191c`, `-levis-red-bright`, `-denim`); riel com gradiente + *pill* ativo com brilho + rótulos de secção; topbar de vidro (translúcido, cantos inferiores 22px, `top-nav-group` segmentado, `top-primary-action` em gradiente); cards 18px + sombras gaussianas multi-camada + hover-lift; botões em gradiente vermelho; inputs *filled* arredondados com foco vermelho; tipografia de display reforçada |
| **2 — Shell** | ✅ | `dashboard-view.fxml` reescrito: topbar de vidro + riel agrupado (PRINCIPAL / GESTÃO / CONTA) com cartão de utilizador. **30 `fx:id` + 16 handlers preservados.** Novo `lblSecaoGestao` (fx:id) alternado por `podeAcederHorarios` no `DashboardController` para evitar rótulo órfão a funcionários |
| **3 — Home** | ✅ | `home-view.fxml` reescrito: hero full-width com pills integradas + chip "Hoje"; Cobertura Semanal + card escuro "Próximos Turnos" lado a lado; Equipa + Métricas; Operação da Loja. **43 `fx:id` + 13 handlers preservados** |
| **4 — Login** | ✅ | `login.css`: vermelho harmonizado (`#c41230` → `#c91428`), botão em gradiente, cartão com cantos 22px + sombra mais profunda |
| **5 — Módulos** | ✅ (via herança) | Horários / Funcionários / Relatórios / Auditoria / etc. herdam toda a nova linguagem (tokens, cards, tabelas, inputs, banners) sem alterações estruturais |

**Contrato técnico:** zero `fx:id`/handlers removidos; única alteração de controller é **aditiva** (`lblSecaoGestao`). Validado com `DashboardHomeIntegrationTest` (carrega shell + home) e suite completa.

**Limitação assumida:** redesenho feito **sem renderização** (app precisa de Postgres + login). Valores escolhidos por princípio, não por iteração visual. **Recomenda-se uma passagem ao vivo** para afinar densidades/contrastes — e nessa altura podemos avaliar o AtlantaFX com preview.

### Live Tuning v3.1 — afinação com base em screenshots reais

O utilizador correu a app e enviou prints (login + home). Análise visual:

**Saiu como planeado ✅:** sidebar Indigo (dark, secções, *pill* vermelho, badges),
cards/hero/calendários, chip "Hoje", login editorial.

**Descoberta importante:** existia um bloco de overrides **"Stitch v2"** (~linha
1380–1880 do `dashboard.css`) que **pós-datava e anulava** parte das regras Indigo
da Onda 1 (topbar, pesquisa, ação primária, botões, cards). Por isso a topbar
aparecia como *tab* sublinhado a vermelho em vez do *pill* segmentado, e os botões
apareciam *flat* em vez de gradiente.

**Correção:** bloco final **LIVE TUNING v3.1** no fim do `dashboard.css` (vence por
ordem, sem caçar cada duplicado):
| Fix | Antes (render) | Depois |
|-----|----------------|--------|
| Nav da topbar | tab com sublinhado vermelho | *pill* segmentado (grupo cinza-quente, ativo branco com sombra) |
| Ação primária + `botao-acao` | vermelho *flat* | gradiente vermelho com brilho |
| Cards | raio 16 / sombra 0.05 | raio 18 / sombra gaussiana suave |
| Título do hero | 44px (cortava nomes longos) | 36px |
| Dia da semana (Cobertura) | "Segunda-f..." cortado | 9px → cabe inteiro |
| Scrollbars | setas datadas, thumb invisível no escuro | setas escondidas, thumb afinado (claro na sidebar) |

**Nota técnica:** todas as correções são CSS puro → não afetam os 51 testes
(que só validam carregamento de FXML, não rendering).

**Confirmado pelo utilizador (screenshots):** topbar com *pills*, botões em
gradiente e ajustes de texto deixaram Home + Login com aspeto "AAA". ✅

### Onda 5 — Módulos: auditoria + harmonização (51/51 ✅)

**Auditoria de estrutura (antes de mexer):** todos os 14 FXML de módulo **já são
card-based** e **já herdam a identidade Indigo** via cascata — usam `info-card` /
`bento-card` / `stat-card` / `page-banner`, `tabela-premium`, `campo-input` /
`campo-combo`, `botao-acao` / `botao-secundario`, `mensagem-feedback`,
`context-bar`. Exemplos: `gestao-funcionarios` (info-card + context-bar +
tabela-premium + form), `geracao-horarios` (bento-card header + stat-kicker +
bento-cards), `relatorios` (titulo-dashboard + stat-card + tabela-premium).

**Conclusão honesta:** não há *rewrite* estrutural a fazer — os módulos já estão
bem construídos. Um *rewrite* às cegas seria churn arriscado, ainda por cima **sem
rede de testes** (ver caveat abaixo). O único resíduo "antigo" real era o vermelho
legado.

**Aplicado:** harmonização do vermelho legado **`#c41230` → `#c91428`** (token de
marca) em **26 ícones SVG** em 6 módulos (`geracao-horarios`,
`gestao-funcionarios`, `gestao-loja`, `painel-auditoria`, `painel-gerente-pedidos`,
`pedir-folga`). Escrita preservou UTF-8 sem BOM; zero alterações a `fx:id`/estrutura.

**⚠️ Caveat de validação importante:** os 51 testes carregam apenas
`dashboard-view.fxml` + `home-view.fxml` (`DashboardHomeIntegrationTest`). **Os FXML
de módulo NÃO são carregados por nenhum teste** → "validar módulos com os 51 testes"
não deteta erros de FXML de módulo. A validação real destes ecrãs é o **render**.
Por isso, restruturações mais profundas (ex.: unificar todos os cabeçalhos num
*hero card* consistente) devem ser feitas **com feedback visual**, não às cegas.

**Próximo passo proposto:** o utilizador renderiza os módulos-chave (Horários,
Funcionários, Relatórios, Auditoria) e indica quais cabeçalhos/layouts quer que eu
unifique — faço-o com screenshots à mão, em segurança.

### Live review por screenshots — achados e correções

O utilizador enviou prints de todos os módulos. **Veredicto visual: todos já estão
Indigo e coerentes** (cards, tabelas premium, inputs filled, botões gradiente, stat
cards, banners, calendários). Confirmado que o live-tuning v3.1 pegou (ex.: dia
"hoje" com borda vermelha no calendário mensal, nomes de dia inteiros).

**Refinamento CSS aplicado:** linhas de tabela **vazias** agora ficam limpas (sem
zebra nem régua) via `.table-row-cell:empty` — antes pareciam "papel pautado".

**🐞 Bugs reais apanhados no review (não-visuais) e corrigidos:**
Causa-raiz comum: `DayOff.getIdUtilizador()` devolve a entidade **`Utilizador`**, não
o `Integer` id. Três sítios usavam-no mal:
| Ficheiro | Sintoma | Correção |
|----------|---------|----------|
| `PainelGerentePedidosController` (col. Colaborador, Folgas Pendentes) | mostrava `Utilizador #...@hashcode` | usar `.getId()` como key do mapa + fallback `Colaborador #id` |
| `PedirFolgaController` (col. Colaborador, tabela de aprovação) | idem `@hashcode` | idem |
| `GestaoFuncionariosController` (filtro de folgas pendentes do colaborador) | `Integer.equals(Utilizador)` → **sempre falso**, folgas nunca marcadas como pendentes | comparar `idColaborador.equals(item.getIdUtilizador().getId())` (espelha o filtro de Preferências, que já estava certo) |

`PreferenciasController` estava correto (helper `obterNomeUtilizador(Utilizador)`).
**51/51 ✅** após as 3 correções.

**Aviso funcional (a investigar à parte):** na Auditoria, o histórico mostra
"Não foi possível carregar o histórico de auditoria." — possível erro real de
carregamento (não é design).

**Varredura de placeholders concluída:** as 3 tabelas do perfil operacional em
`GestaoFuncionariosController` (folgas/preferências/permutas) não tinham
`setPlaceholder` → mostravam o default inglês "No content in table". Adicionados
placeholders PT. **Cobertura agora completa em todos os controllers** (TableView ↔
setPlaceholder equilibrados). **51/51 ✅.**

**Uniformização de cabeçalhos — CONCLUÍDA** (utilizador escolheu "título grande em
todos"). Todos os módulos passam a abrir com `titulo-dashboard` + `subtitulo`:
| Módulo | Antes | Ação |
|--------|-------|------|
| Horários | sem título de página (entrava na secção CONFIGURAÇÃO) | **adicionado** "Geração de horários" + subtítulo |
| Auditoria | sem título (entrava em "Resumo de Eventos") | **adicionado** "Auditoria e segurança" + subtítulo |
| Loja e Regras | `page-banner-loja` hero | **convertido** para título grande |
| Permutas | `page-banner` hero centrado | **convertido** para título grande |
| Pedir Folga | `page-banner-folga` hero | **convertido** para título grande |
| Funcionários, Relatórios, Preferências, Painel Gerente | já tinham `titulo-dashboard` | mantidos |
| **Perfil** | `page-banner` com **avatar** | **mantido** (decisão de design: avatar é apropriado numa página de perfil) |

Heroes removidos não deixam classes órfãs problemáticas (`page-banner*` continuam no
CSS, inofensivas). **Validação:** os 6 FXML editados verificados como XML bem-formado
(`[xml]` parse) — rede de segurança porque **os testes não carregam FXML de módulo**.
Build Java mantém-se **51/51 ✅** (sem alterações Java desde a última corrida verde).

---

## Sessão 6 — Redesenho Estético Desktop ("Nova Cara" v3)

**Meta:** transformar a interface Desktop num produto comercial moderno, mantendo
a identidade *Heritage Industrial* da Levi's (alto contraste, tipografia forte,
vermelho `#c91428`). Sem partir os 51 testes (os testes só carregam FXML e
verificam ausência de exceção + centro não-vazio → mudanças CSS são 100% seguras;
mudanças FXML têm de preservar `fx:id`/`onAction`).

### Diagnóstico do varrimento visual

| Achado | Severidade | Ação |
|--------|-----------|------|
| **Calendário semanal ("Cobertura Semanal") sem CSS** — classes `calendario-dia-card`, `calendario-evento`, etc. renderizam com o look JavaFX padrão | 🔴 Crítico (é o coração da app) | Estilizar por completo |
| **Calendário mensal sem CSS** — família `calendario-mes-*` (cards de dia, eventos, "hoje", clicável) sem qualquer estilo | 🔴 Crítico | Estilizar por completo |
| `tabelaRelatorio` (relatórios) é a única `TableView` sem `tabela-premium` | 🟠 | Adicionar styleClass |
| Conflito CSS: `.modulo-home .home-card-title` (14px, linha 998) anula `.home-card-title` (20px) por especificidade | 🟠 | Remover regra conflituante |
| Ícones de secção da Home são unicode (`◷ 👥 ▤`) — inconsistentes com a sidebar Ikonli | 🟡 | Migrar para `FontIcon` MDI2 |
| Sombras dos cards muito ténues (alpha 0.04) — falta sensação "premium flutuante" | 🟡 | Sistema de elevação refinado |
| Mensagens de feedback (`mensagem-feedback`) são texto simples sem banner | 🟡 | Banners suaves (neutro/erro/sucesso) |
| JavaFX 21 **não suporta transições CSS** (só ≥23) | ℹ️ | Hovers instantâneos (sombra/cor/borda), sem animação CSS |

### Plano de execução (Sessão 6)

| Fase | Conteúdo | Risco |
|------|----------|-------|
| **A** | Estilizar calendário semanal + mensal (cards de dia, eventos coloridos por período, estado "hoje", hover de cards clicáveis) | CSS — nulo |
| **B** | Sistema de elevação: sombras de card mais ricas e consistentes, raios uniformes, hover-lift em cards clicáveis | CSS — nulo |
| **C** | Home: ícones de secção → Ikonli MDI2 (consistência com sidebar) | FXML — baixo |
| **D** | Tabelas globais: refinar `tabela-premium` (cabeçalho, zebra subtil, seleção, scrollbar elegante) + `tabelaRelatorio` ganha a classe | CSS + 1 FXML |
| **E** | Banners de feedback suaves (neutro/erro/sucesso) em todos os módulos via `mensagem-feedback` | CSS — nulo |
| **F** | Limpeza: remover regra `.modulo-home .home-card-title` conflituante | CSS — nulo |

**Nota:** existe `dashboard.css.bak` no repositório (backup morto, não carregado) — fora de âmbito.

### Resultado — Sessão 6 concluída ✅ (51/51 testes, BUILD SUCCESS)

| Fase | Estado | Detalhe da implementação |
|------|--------|--------------------------|
| **A — Calendários** | ✅ | Bloco *DESIGN REFRESH v3* no fim de `dashboard.css`: semanal (`calendario-dia-card` com sombra + hover, `calendario-evento` como chip com barra vermelha à esquerda, `calendario-evento-vazio` itálico esbatido) e mensal (cards de dia com sombra, estado **hoje** com borda vermelha e nº destacado, célula vazia esbatida, cards clicáveis com hover-lift, eventos com ponto vermelho + variantes manhã/intermédio/noite por cor) |
| **B — Elevação** | ✅ | Sombras dos cards da Home reforçadas (alpha 0.075, blur 26) e do card escuro; `stat-card`/KPIs com sombra base + hover (borda vermelha + sombra mais profunda) |
| **C — Ícones Home** | ✅ | `home-view.fxml`: import `FontIcon` + 3 ícones de secção migrados de unicode para MDI2 (`calendar-month`, `account-group`, `view-dashboard`); CSS `.home-section-icon .ikonli-font-icon` a vermelho |
| **D — Tabelas** | ✅ | `tabela-premium` refinada (cabeçalho 10.5px/800, zebra subtil `#fdf9f9`, hover `#fff4f4`, seleção vermelha, cantos e `corner` transparentes); `tabelaRelatorio` passou a ter `tabela-premium` (era a única sem estilo) |
| **E — Feedback** | ✅ | `.mensagem-feedback` agora é banner suave (fundo + borda + raio + padding); variantes compostas `.mensagem-feedback.mensagem-erro` (vermelho) e `.mensagem-feedback.mensagem-sucesso` (verde) — aplica-se a **todos** os módulos automaticamente |
| **F — Limpeza CSS** | ✅ | Removida a regra `.modulo-home .home-card-title` (14px) que anulava por especificidade o título de 20px da fonte única |

**Ficheiros alterados:** `dashboard.css` (bloco v3 + remoção de conflito), `home-view.fxml` (ícones), `relatorios-horas-view.fxml` (1 styleClass).

**Restrições respeitadas:** JavaFX 21 sem transições CSS → hovers instantâneos (sombra/cor/borda); todos os `fx:id`/`onAction` preservados.

**Possíveis próximos passos (não feitos):** harmonizar o vermelho do login (`#c41230` → token `#c91428`); remover `dashboard.css.bak`; polir cabeçalhos por módulo se desejado.

---

## Sessão 5 — Ícones Ikonli na Sidebar + Limpeza UI Desktop

### Passo 2 — Ícones SVG (Material Design Icons 2) na Sidebar

| Item | Detalhe |
|------|---------|
| Dependências adicionadas | `ikonli-javafx 12.3.1` + `ikonli-materialdesign2-pack 12.3.1` no `pom.xml` |
| FXML actualizado | `dashboard-view.fxml` — import `FontIcon`, todos os 11 botões da sidebar têm `<graphic><FontIcon iconLiteral="…"/></graphic>` e `graphicTextGap="11.0"` |
| CSS actualizado | `dashboard.css` — regras `.sidebar-btn .ikonli-font-icon` para cor e tamanho em estado normal, hover e activo |
| Mapeamento de ícones | `mdi2v-view-dashboard` (Painel), `mdi2s-store` (Loja), `mdi2a-account-group` (Funcionários), `mdi2c-calendar-month` (Horários), `mdi2c-clipboard-list` (Pedidos), `mdi2s-shield-check` (Auditoria), `mdi2c-calendar-remove` (Folgas), `mdi2s-swap-horizontal` (Permutas), `mdi2a-account-circle` (Perfil), `mdi2s-star-outline` (Preferências), `mdi2c-chart-bar` (Relatórios) |
| Bug corrigido | `mdi2c-calendar-off` → `mdi2c-calendar-remove` (literal inválido no pack 12.3.1 causava `LoadException` no FXML em headless test) |

**Testes:** 51/51 ✅ BUILD SUCCESS

---

### Passo 1 — Limpeza UI Desktop (Elementos Mortos)

### Alterações efectuadas

| # | Problema | Ficheiro(s) alterado(s) | Resultado |
|---|----------|------------------------|-----------|
| P1 | Botão "ACESSO BIOMÉTRICO" + divider "OU ACESSO SEGURO" removidos do ecrã de login | `login/login-view.fxml` | Login mais limpo e sem botão falso |
| P2 | Botões 🔔 e "?" sem handler removidos do topbar | `dashboard/dashboard-view.fxml` | Topbar limpo (barra de pesquisa funcional mantida) |
| P4 | Pills "Todos / Manhã / Tarde / Noite" agora filtram a tabela "Equipa em Loja Agora" | `dashboard/home-view.fxml`, `Controller/HomeController.java` | Filtro por tipo de turno funcional; pill activa destacada visualmente; lista completa guardada em `todosEquipaHoje` |

**Nota:** A barra de pesquisa global (`txtPesquisa`) foi **mantida** — tem sistema de sugestões de navegação completamente implementado em `DashboardController`.

**Testes:** 51/51 ✅ BUILD SUCCESS

---

## Sessão 4 — Reorientação Web: Portal do Funcionário

### Nova Visão Estratégica

O Desktop mantém-se como a plataforma completa e robusta.
A versão Web é um **Portal do Funcionário** simplificado com exactamente 2 funcionalidades:

1. **Consultar o próprio horário publicado** — `/web/horarios`
2. **Ver e editar o próprio perfil/dados pessoais** — `/web/perfil`

### Alterações Efectuadas

| Ficheiro | Alteração |
|----------|-----------|
| `WEB/WebHorariosController.java` | Reescrito: remove gerar/aprovar/rejeitar; usa `obterMeusHorarios()` para mostrar os turnos pessoais publicados |
| `BLL/GeracaoHorariosBLL.java` | Novo método `obterMeusHorarios(idUtilizador, ano, mes)` via `findHorariosPublicadosPorUtilizadorEntreDatas` |
| `WEB/WebLoginController.java` | Redirect pós-login alterado de `/web/painel` para `/web/horarios` |
| `templates/web/horarios.html` | Página redesenhada: lista pessoal de turnos (data, dia semana, tipo, início, fim) |
| `WebAccessIntegrationTest.java` | Testes actualizados para reflectir novo redirect e nova página |
| `WebFluxosCriticosE2ETest.java` | Teste E2E corrigido: redirect `/web/horarios` + removida chamada a `/web/horarios/gerar` |
| `templates/web/fragments.html` | Sidebar simplificada: remove "Painel" e "Pedidos/Complementares"; renomeia "Horários" → "O meu horário"; apenas "O meu horário" e "Perfil" visíveis a funcionários |

**Testes:** 51/51 ✅ BUILD SUCCESS

### Estado Final do Portal Web

| Funcionalidade | Rota | Visível na sidebar | Estado |
|---------------|------|-------------------|--------|
| O meu horário (turnos publicados) | `/web/horarios` | ✅ Sempre | ✅ Completo |
| Perfil (ver + editar) | `/web/perfil` | ✅ Sempre | ✅ Completo |
| Relatórios | `/web/relatorios` | Só gestores | Mantido (guardado por permissão) |
| Loja e Regras / Funcionários | `/web/gestao-loja`, `/web/equipa` | Só gestores | Mantido (guardado por permissão) |
| Painel | `/web/painel` | ❌ Removido da sidebar | Rota ainda activa mas sem link |
| Pedidos/Complementares | `/web/complementares` | ❌ Removido da sidebar | Rota ainda activa mas sem link |

---

## Sessão 3 — Transição para Plataforma Web (Meta Junho)

### Inventário de Endpoints Existentes

**`/api/` — REST puro (JSON)**
| Método | Rota | Funcionalidade |
|--------|------|----------------|
| GET | `/api/health` | Health-check |
| GET | `/api/users/{id}` | Obter utilizador |
| POST | `/api/users` | Criar utilizador |
| POST | `/create-user` | Criar utilizador (duplicado legado) |
| GET | `/api/permutas/meus-turnos` | Listar turnos disponíveis para permuta |
| GET | `/api/permutas/turnos-elegiveis?idHorarioOrigem=X` | Turnos elegíveis de colega |
| POST | `/api/permutas/submeter` | Submeter pedido de permuta |

**`/web/` — Thymeleaf (HTML server-side)**
| Método | Rota | Funcionalidade |
|--------|------|----------------|
| GET/POST | `/web/login` + `/web/logout` | Autenticação |
| GET | `/web/painel` | Dashboard com métricas e pendências |
| GET | `/web/horarios` | Ver planeamento atual + lista de propostas |
| POST | `/web/horarios/gerar` | Gerar 1 proposta de horário |
| POST | `/web/horarios/{id}/aprovar` | Aprovar proposta (supervisor) |
| POST | `/web/horarios/{id}/rejeitar` | Rejeitar proposta (supervisor) |
| GET/POST | `/web/gestao-loja` | Regras, horário, exceções |
| GET/POST | `/web/equipa` | CRUD colaboradores + aprovar/rejeitar folgas/prefs/permutas |
| GET/POST | `/web/complementares` | Folgas/preferências/permutas do próprio utilizador |
| GET/POST | `/web/perfil` | Nome, email, telemóvel, password |
| GET | `/web/relatorios` + `/web/relatorios/exportar.csv` | Relatório de horas + CSV |

---

## T7 — Lacunas Web: Lista de Endpoints em Falta

### Análise: BLL implementada vs. Web exposta

| # | Lacuna | BLL que existe | Endpoint em falta | Prioridade |
|---|--------|---------------|-------------------|-----------|
| **W1** | **Enviar propostas ao supervisor** | `GeracaoHorariosBLL.enviarPropostasParaValidacao()` | `POST /web/horarios/enviar-supervisor` | 🔴 Crítica |
| **W2** | **Gerar múltiplas alternativas** | `GeracaoHorariosBLL.gerarPropostas(quantidade)` | `POST /web/horarios/gerar-alternativas` com `?quantidade=N` | 🔴 Crítica |
| **W3** | **Painel de Auditoria Web** | `AuditoriaBLL.carregarPainel()`, `utilizadorPodeConsultarAuditoria()` | `GET /web/auditoria` (nova página Thymeleaf completa) | 🟠 Alta |
| **W4** | **Remover preferência própria** | `PreferenciaBLL.removerPreferencia()` | `POST /web/complementares/preferencias/{id}/remover` | 🟠 Alta |
| **W5** | **Redefinir password pelo gerente (Web)** | `PerfilBLL.redefinirPasswordPorGerente()` | `POST /web/equipa/{id}/redefinir-password` | 🟡 Média |
| **W6** | **Exportar relatório em PDF (Web)** | `ExportacaoPdfBLL.exportarRelatorioPdf()` | `GET /web/relatorios/exportar.pdf` | 🟡 Média |
| **W7** | **Exportar horário mensal em CSV/PDF (Web)** | `ExportacaoPdfBLL.exportarHorarioPdf()` | `GET /web/horarios/exportar.csv`, `GET /web/horarios/exportar.pdf` | 🟡 Média |
| **W8** | **Autenticação REST com token/sessão para `/api/`** | `UtilizadorBLL.efetuarLogin()` + `SessaoBLL` | `POST /api/auth/login` → retorna token/cookie de sessão (os endpoints `/api/` actuais dependem do header `X-Manager-Id` sem validação de sessão) | 🟡 Média |

---

### Plano de Implementação T7

**Ordem sugerida (por dependência e impacto):**

**Sprint 1 — Completar o fluxo core de horários (W1 + W2)**
- W1: `POST /web/horarios/enviar-supervisor` — recebe lista de `idsPropostas[]`, chama `enviarPropostasParaValidacao()`, redireciona com flash message. Requer actualizar o template `horarios.html` com checkboxes nas propostas rascunho e botão de envio.
- W2: `POST /web/horarios/gerar-alternativas` — similar ao `gerar` mas com parâmetro `quantidade` (1-5). A página já lista propostas; apenas falta o segundo botão de geração em lote.

**Sprint 2 — Funcionalidades do utilizador e gestão (W3 + W4 + W5)**
- W3: Novo `WebAuditoriaController` + template `auditoria.html` — mapa do Desktop `PainelAuditoriaController` para Web. A BLL está completa.
- W4: `POST /web/complementares/preferencias/{id}/remover` — 1 método no `WebComplementaresController` existente + botão no template.
- W5: `POST /web/equipa/{id}/redefinir-password` — 1 método no `WebEquipaController` existente.

**Sprint 3 — Exportações e API REST (W6 + W7 + W8)**
- W6 + W7: Dois novos `@GetMapping` nos controllers existentes usando `ExportacaoPdfBLL`.
- W8: Novo `WebAuthApiController` com `POST /api/auth/login` que valida credentials e cria sessão HTTP, resolvendo a lacuna de autenticação dos endpoints `/api/`.

**Ficheiros novos previstos:**
- `WEB/WebAuditoriaController.java` (W3)
- `WEB/WebAuthApiController.java` (W8)
- `templates/web/auditoria.html` (W3)

**Ficheiros a alterar:**
- `WEB/WebHorariosController.java` (W1, W2, W7)
- `WEB/WebComplementaresController.java` (W4)
- `WEB/WebEquipaController.java` (W5)
- `WEB/WebRelatoriosController.java` (W6)
- `templates/web/horarios.html` (W1, W2)
- `templates/web/complementares.html` (W4)
- `templates/web/equipa.html` (W5)

**Próximo Passo Pendente:** Implementar T7 Sprint 1 (W1 + W2)

---

## Sessão 9 — Auditoria Visual + Correções de UX (2026-06-06)

### Motivação

O utilizador pediu: correr a app, observar o que está mau e corrigir tudo.
Feedback anterior: "não gosto de como está a seleção dos colaboradores para os
horários", "queria notificação em grande quando o horário é gerado ou não",
"não consigo ver onde o horário fica depois de gerado", "está feio e difícil
de analisar o calendário", "quando clico num turno aparece um ecrã feio em
vez de ficar sobreposto", "isso é do mesmo tipo do logout e fechar aplicação".

### Trabalho desta sessão

#### CSS-09 — Diálogos modais com overlay real (dashboard.css)

Todos os diálogos (`confirmarAcao`, `mostrarErro`, `mostrarInformacao`,
`mostrarConteudo`, `pedirTexto`) usavam um `Stage` transparente mas sem
classes CSS definidas → apareciam como texto flutuante num fundo bege.

**Adicionado ao `dashboard.css`** (bloco CSS-09):
- `.dialogo-overlay` — fundo escuro semi-transparente (`rgba(10,10,16,0.52)`)
- `.dialogo-card` — card branco com `border-radius: 20px` e drop-shadow
- `.dialogo-faixa` — cabeçalho com fundo `#f8f5f2`
- `.dialogo-kicker`, `.dialogo-titulo`, `.dialogo-corpo`, `.dialogo-mensagem`
- `.dialogo-botoes`, `.dialogo-campo`
- `.dialogo-loading-card`, `.dialogo-loading-spinner`, `.dialogo-loading-titulo`, `.dialogo-loading-subtitulo`
- `.dialogo-notificacao-card`, `.dialogo-notificacao-icone-sucesso/erro`
- `.dialogo-notificacao-titulo`, `.dialogo-notificacao-mensagem`

#### CSS-10 — Seleção de Colaboradores (boxColaboradoresGeracao)

- `.grupo-colaboradores` — card com fundo branco, borda, border-radius 10px
- `.grupo-colaboradores-titulo` — azul índigo bold (header do grupo de cargo)
- `.grupo-colaboradores-itens` — indentado 18px, espaçamento 5px
- `.colaborador-check` — 13px, cursor hand

#### CSS-11 — Detalhe do Dia (calendário mensal)

- `.detalhe-dia-lista`, `.detalhe-dia-turno-card`
- `.detalhe-dia-periodo`, `.detalhe-dia-colaborador`, `.detalhe-dia-cargo`

#### DialogosHelper.java — Novos métodos

- **`mostrarCarregamento(Window, String) → Stage`** — overlay não-bloqueante
  com `ProgressIndicator`, aparece enquanto o horário é gerado em background.
  Chamador fecha com `stage.close()` quando a tarefa termina.
- **`mostrarNotificacaoGeracao(Window, boolean sucesso, String titulo, String msg)`**
  — diálogo grande centrado (SVG icon ✓ verde / ✗ vermelho, título 26px,
  mensagem, botão OK). Bloqueante (`showAndWait`). Responde a Enter/Escape.

#### GeracaoHorariosController.java — Geração em segundo plano com UX

Novo método `gerarAlternativasEmSegundoPlano(int quantidade)`:
1. Confirmação antes de gerar (dialog existente)
2. Abre overlay de carregamento (não-bloqueante) enquanto a task corre
3. Em `onSuccess`: fecha overlay → aplica dados → navega para tab "Propostas"
   (índice 1) → abre notificação grande de sucesso → mostra `mostrarSucesso`
4. Em `onFailure`: fecha overlay → abre notificação grande de erro →
   volta para tab "Gerar" (índice 0) → mostra diagnóstico existente

#### geracao-horarios-view.fxml — Conversão para TabPane

Root: `ScrollPane` → `VBox styleClass="conteudo-pagina, modulo-com-abas"`.
`TabPane fx:id="tabPaneHorarios"` com 3 tabs:
- **Tab 0 "Gerar"** — configuração de período, botões de geração, painel de
  colaboradores, diagnóstico (todos os fx:ids originais preservados)
- **Tab 1 "Propostas"** — stat cards, tabela de alternativas, comparação,
  detalhe da proposta, painel de validação do supervisor, distribuição
- **Tab 2 "Calendário"** — calendário semanal + calendário mensal + exportações

Todos os `fx:id` e `onAction` handlers preservados → 51 testes intactos.

#### Correções de encoding (caracteres acentuados em PT)

Strings user-facing com acentos em falta corrigidas nos controllers:

| Ficheiro | Strings corrigidas |
|----------|--------------------|
| `DashboardController.java` | "sessao"→"sessão", "ecra"→"ecrã", "autenticacao"→"autenticação", "pagina"→"página" |
| `GeracaoHorariosController.java` | "horario"→"horário", "geracao"→"geração", "elegivel"→"elegível", "periodo"→"período", "pontuacao"→"pontuação", "analise"→"análise", "Diagnostico"→"Diagnóstico" |
| `GestaoFuncionariosController.java` | "nao"→"não", "possivel"→"possível", "horario"→"horário", "elegivel"→"elegível" (×6 strings) |
| `PainelAuditoriaController.java` | "nao"→"não" no placeholder da tabela |
| `PermutasController.java` | "Nao"→"Não", "elegiveis"→"elegíveis" (×2) |

#### Auditoria visual completa (app em execução)

Todas as páginas observadas visualmente com screenshots:
- **Painel** ✅ — layout limpo, cards bem estruturados
- **Horários / Gerar** ✅ — tabs funcionam, colaboradores agrupados por cargo
- **Horários / Propostas** ✅ — stat cards, tabela vazia com empty state correto
- **Horários / Calendário** ✅ — calendário semanal + mensal estruturado
- **Funcionários** ✅ — tabs "Equipa" + "Perfil do colaborador" funcionais
- **Loja e Regras** ✅ — tabs "Configuração" + "Exceções"
- **Folgas** ✅ — formulário + histórico
- **Permutas** ✅ — formulário 3 passos + regras + histórico
- **Pedidos** ✅ — painel gerente com stat cards e filtros
- **Relatórios** ✅ — filtros + stats + tabela por colaborador
- **Diálogo "Terminar sessão"** ✅ — overlay escuro + card branco (encoding agora correto)

### Ficheiros alterados

| Ficheiro | Alteração |
|----------|-----------|
| `dashboard/dashboard.css` | Blocos CSS-09, CSS-10, CSS-11 adicionados |
| `Controller/support/DialogosHelper.java` | `mostrarCarregamento()` + `mostrarNotificacaoGeracao()` |
| `Controller/GeracaoHorariosController.java` | Método `gerarAlternativasEmSegundoPlano()` + campo `tabPaneHorarios` + encoding fixes |
| `dashboard/geracao-horarios-view.fxml` | Root reescrito para TabPane 3-abas |
| `Controller/DashboardController.java` | Encoding fixes (2 strings) |
| `Controller/GestaoFuncionariosController.java` | Encoding fixes (6 strings) |
| `Controller/PainelAuditoriaController.java` | Encoding fix (1 string) |
| `Controller/PermutasController.java` | Encoding fix (2 strings) |

### Estado dos testes

Alterações CSS e FXML de módulo não são cobertas pelos testes (os 51 testes
só carregam `dashboard-view.fxml` + `home-view.fxml`). As correções Java são
puras correções de texto em string literals → sem risco para os testes.

---

## Sessão 10 — Auditoria Visual Completa + Correções de Encoding (2026-06-06)

### Motivação

Continuação direta da Sessão 9. Login efetuado com `francisco.gomes@levis.com` / `123456`.
Auditoria visual página a página com screenshots; todas as strings user-facing com acentos
em falta detetadas e corrigidas por varrimento sistemático.

### Auditoria visual — todas as páginas

| Página | Estado | Observações |
|--------|--------|-------------|
| **Painel (Home)** | ✅ | Todos os textos corretos; "Operação da Loja" funcional |
| **Folgas** | ✅ | Formulário + histórico bem estruturados |
| **Permutas** | ✅ | "elegíveis" e "ecrã" verificados via zoom — corretos |
| **Horários / Gerar** | ✅ | "13 de 13 colaboradores selecionados para a geração." confirmado |
| **Horários / Propostas** | ✅ | Empty state, stat cards corretos |
| **Horários / Calendário** | ✅ | Calendário semanal + mensal, exportações CSV/PDF |
| **Funcionários / Equipa** | ✅ | Tabela + formulário editar colaborador |
| **Funcionários / Perfil** | ✅ | "não tem horário publicado" + "preferência(s)" corrigidos |
| **Loja e Regras / Configuração** | ✅ | Regras, horário de funcionamento |
| **Loja e Regras / Exceções** | ✅ | Formulário de exceções de calendário |
| **Pedidos** | ✅ | Painel gerente com context operacional e filtros por tipo |
| **Relatórios** | ✅ | "Relatório gerado com sucesso." correto |
| **Perfil** | ✅ | Dados pessoais, vínculo, indicadores |
| **Preferências** | ✅ | Formulário + painel aprovação + "geração do horário." correto |
| **Auditoria** | ⚠️ | Erro ao carregar (issue pré-existente de BD, não introduzido nesta sessão) |
| **Diálogo "Terminar sessão"** | ✅ | "sessão", "ecrã", "autenticação" — todos corretos |
| **Diálogo "Fechar aplicação"** | ✅ | "aplicação", "encerrada" — corretos |

### Correções de encoding — Sessão 10

#### `GestaoFuncionariosController.java`

| String antiga | String corrigida |
|--------------|-----------------|
| `"...preferencia(s)..."` | `"...preferência(s)..."` |
| `"Deseja guardar as alteracoes deste colaborador?"` | `"...alterações..."` |
| `"O novo colaborador ficara associado a loja atual."` | `"...ficará associado à..."` |
| `"Os dados do colaborador serao atualizados."` | `"...serão..."` |
| `"A decisao sera registada de imediato."` (×3) | `"A decisão será registada de imediato."` |
| `"Aprovar preferencia"` / `"Rejeitar preferencia"` | `"...preferência"` (×2) |
| `"Deseja aprovar esta preferencia?"` / `"...rejeitar..."` | `"...preferência?"` (×2) |
| `"Preferencia aprovada/rejeitada com sucesso."` | `"Preferência..."` (×2) |
| `"...preferencias, permutas e decisoes pendentes."` | `"...preferências...decisões..."` |
| `"O colaborador deixara de ficar ativo..."` | `"...deixará..."` |

#### `GeracaoHorariosController.java`

| String antiga | String corrigida |
|--------------|-----------------|
| `"So as alternativas...disponiveis para aprovacao ou rejeicao."` | `"Só...disponíveis...aprovação ou rejeição."` |
| `"folgas/preferencias aprovadas,...minimo exigido..."` | `"...preferências...mínimo..."` |

#### `PainelAuditoriaController.java`

| String antiga | String corrigida |
|--------------|-----------------|
| `"Historico de auditoria atualizado com sucesso."` | `"Histórico..."` |

#### `PreferenciasController.java`

| String antiga | String corrigida |
|--------------|-----------------|
| `" Duracao preferida: "` | `" Duração preferida: "` |

#### `CalendarioMensalHelper.java`

| String antiga | String corrigida |
|--------------|-----------------|
| `"Sem horario"` | `"Sem horário"` |

### Ficheiros alterados

| Ficheiro | Alterações |
|----------|-----------|
| `Controller/GestaoFuncionariosController.java` | 13 strings corrigidas |
| `Controller/GeracaoHorariosController.java` | 2 strings corrigidas |
| `Controller/PainelAuditoriaController.java` | 1 string corrigida |
| `Controller/PreferenciasController.java` | 1 string corrigida |
| `Controller/support/CalendarioMensalHelper.java` | 1 string corrigida |

### Estado dos testes

Todas as correções são exclusivamente em string literals de mensagens ao utilizador.
Nenhum `fx:id`, `onAction`, ou lógica de negócio foi alterada → 51 testes intactos.

---

## 2026-06-06 — Sessão 11

### Objetivo
Duas frentes em paralelo:
1. **Melhorar o algoritmo** de geração para respeitar melhor preferências, folgas e pares de colaboradores.
2. **Redesenhar o fluxo de UX** do gerente: gerar → analisar → ver horário individual de cada colaborador → enviar ao supervisor.

---

### 1. Algoritmo — Reforço dos pesos de preferência (`HorarioGeneratorEngine.java`)

O algoritmo já era funcional (backtracking + heurística gulosa, 5 tentativas progressivas). Foram ajustados os pesos da função de pontuação `pontuarAtribuicao` para que preferências tenham maior impacto na ordenação de candidatos:

| Critério | Peso anterior | Peso novo | Justificação |
|----------|--------------|-----------|--------------|
| Preferência de turno aprovada | `-30` | `-55` | Quase 2× mais impacto; equilibra com a utilização contratual (×100) |
| Dia de folga preferido (A2) | `+60` | `+85` | Penalização mais forte; reduz atribuições em dias de preferência de folga |
| Colega preferido mesmo dia (A3 — novo) | n/a | `-45` | **Novo**: se o colega preferido já foi escalado no mesmo dia → bónus forte de co-escalamento |
| Colega preferido mesmo mês (A3) | `-20` | `-25` | Ligeiramente reforçado |

**Lógica A3 melhorada:**
- Antes: apenas verificava se o colega estava em _algum_ dia do histórico do mês (bónus fraco `-20`).
- Agora: diferencia **mesmo dia** (bónus forte `-45`) de **mesmo mês** (bónus fraco `-25`), favorecendo ativas co-escalamento de pares preferidos no mesmo turno.

---

### 2. UX — Stepper visual de fluxo (`geracao-horarios-view.fxml` + `dashboard.css`)

Adicionado um **stepper horizontal de 4 passos** entre o cabeçalho da página e as abas, com estado dinâmico:

```
[1 Configurar] ─── [2 Gerar] ─── [3 Analisar] ─── [4 Enviar]
```

- Passo **ativo** → vermelho Levi's com círculo preenchido
- Passo **concluído** → verde com círculo verde
- Passo **inativo** → cinzento claro

Novos `fx:id` adicionados: `stepperPasso1`, `stepperPasso2`, `stepperPasso3`, `stepperPasso4`.

---

### 3. UX — Guia de fluxo contextual (`lblGuiaFluxo`)

Novo `Label fx:id="lblGuiaFluxo"` na Tab 1 ("Gerar"), com texto contextual que muda automaticamente conforme o estado da proposta:

- Sem proposta: _"Passo 1 → Configura o período, seleciona a equipa e clica em Gerar 1 alternativa."_
- Proposta gerada: _"Passo 3 → Analisa a proposta na tab Propostas. Compara alternativas e vê o horário de cada colaborador (duplo clique na tabela). Quando estiveres pronto, envia ao supervisor."_
- Aprovada: _"✔ Proposta aprovada e publicada. O calendário fica disponível na tab Calendário."_

---

### 4. UX — Horário individual por colaborador (modal) (`GeracaoHorariosController.java`)

**Funcionalidade nova:** duplo clique em qualquer linha da tabela "Distribuição por colaborador" abre um modal com o horário completo do colaborador para o mês selecionado.

O modal mostra:
- Estatísticas: total de turnos e horas contratuais
- Turnos agrupados por semana, com: dia abreviado, data, período/horário, estado
- Scroll se o número de semanas ultrapassar a altura disponível

Novos métodos adicionados ao controller:
- `mostrarHorarioIndividual(ResumoColaborador)` — orquestra o modal
- `criarStatMini(String, String)` — bloco de estatística compacto
- `criarBlocoSemanaIndividual(LocalDate, List<HorarioLinha>)` — secção semanal
- `criarLinhaTurnoIndividual(HorarioLinha)` — linha de turno individual

Também adicionado no FXML a hint _"Duplo clique num colaborador para ver o horário completo do mês."_ acima da tabela.

---

### 5. Stepper dinâmico no controller

Novos métodos adicionados a `GeracaoHorariosController`:
- `atualizarStepper(boolean emProcessamento)` — chamado em `atualizarEstadoInterativo()`
- `aplicarEstadoStepper(VBox, boolean ativo, boolean concluido)` — aplica CSS ao passo
- `atualizarGuiaFluxo(...)` — atualiza texto contextual

---

### 6. CSS — Novos blocos adicionados (`dashboard.css`)

**Bloco CSS-12 — Stepper de fluxo:**
- `.geracao-stepper` — barra horizontal com borda inferior
- `.stepper-passo`, `.stepper-numero`, `.stepper-rotulo` — base
- `.stepper-passo-ativo`, `.stepper-passo-concluido`, `.stepper-passo-inativo` — estados
- `.stepper-linha` — linha de conexão entre passos
- `.guia-fluxo` — label azul índigo para guia contextual

**Bloco CSS-13 — Horário individual (modal):**
- `.horario-individual-conteudo`, `.horario-individual-scroll`
- `.horario-individual-stat-bar`, `.horario-individual-stat`, `.horario-individual-stat-etiqueta`, `.horario-individual-stat-valor`
- `.horario-individual-semana`, `.horario-individual-semana-titulo`
- `.horario-individual-turno`, `.horario-individual-dia`, `.horario-individual-data`, `.horario-individual-periodo`, `.horario-individual-estado`
- `.horario-individual-vazio`

---

### Imports adicionados ao Controller

```java
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
```

---

### Ficheiros alterados

| Ficheiro | Tipo de alteração |
|----------|-------------------|
| `BLL/HorarioGeneratorEngine.java` | Reforço dos pesos de preferência na função `pontuarAtribuicao` |
| `Controller/GeracaoHorariosController.java` | Novos `@FXML` fields, double-click handler, modal de horário individual, stepper dinâmico, guia contextual |
| `resources/.../geracao-horarios-view.fxml` | Stepper de fluxo (4 passos), `lblGuiaFluxo`, hint na tabela de distribuição |
| `resources/.../dashboard.css` | CSS-12 (stepper) + CSS-13 (horário individual) — ~120 linhas de CSS |

### Estado dos testes

**51 testes — 0 falhas — 0 erros** (verificado após todas as alterações desta sessão)

Os testes não carregam `geracao-horarios-view.fxml` (apenas `dashboard-view.fxml` + `home-view.fxml`), pelo que alterações no FXML e no Controller de geração não impactam os testes.

---

## 2026-06-07 — Sessão 12

### Objetivo
Resolver confusão no fluxo de geração de horários do gerente:
1. Na aba Propostas as propostas não tinham indicação do mês/ano a que pertenciam
2. Na comparação de propostas ocorria o mesmo
3. No calendário mensal o gerente não sabia que proposta/mês estava a ver
4. Não era possível ver o calendário mensal de um só colaborador

---

### 1. `rotuloCurtoProposta` — rótulo de proposta com mês/ano (`GeracaoHorariosBLL.java`)

**Causa raiz identificada:** a função `rotuloCurtoProposta` produzia rótulos sem contexto temporal: `"#42 · rascunho · EQUILIBRIO"`. O gerente, ao ver a tabela de propostas, não conseguia associar cada linha ao mês correto.

**Fix:** o rótulo passa a incluir mês e ano:

```java
// Antes
return "#" + proposta.idProposta() + " · " + proposta.estado() + " · " + proposta.metricas().politicaOtimizacao();

// Depois
return proposta.nomeMes() + " " + proposta.ano() + " · #" + proposta.idProposta() + " · " + proposta.estado();
```

Exemplo: `"Julho 2026 · #42 · rascunho"` — imediatamente percetível.

Este rótulo é usado tanto na tabela de propostas (`colPropostaRotulo`) como nos combos de comparação (`cbComparacaoBase` / `cbComparacaoAlvo`), resolvendo os problemas 1 e 2 com uma única alteração.

---

### 2. Sincronização do período mensal ao carregar proposta (`GeracaoHorariosController.java`)

**Bug:** `periodoMensalAtual` era inicializado com `YearMonth.now()` e nunca sincronizado quando uma proposta era carregada. O calendário mensal mostrava o mês atual em vez do mês da proposta.

**Fix em `preencherResultado()`:**
```java
// Adicionado logo após propostaAtual = resultado:
periodoMensalAtual = YearMonth.of(resultado.ano(), resultado.mes());
```

Também chamado `atualizarCalendarioMensal()` no final de `preencherResultado()`, para que o calendário se atualize automaticamente ao selecionar uma proposta na tabela.

---

### 3. Filtro por colaborador no calendário mensal (`obterEventosParaMes`)

**Bug:** `obterEventosParaMes()` ignorava o `cbFiltroColaborador`. O filtro só funcionava no calendário semanal.

**Fix:** a função passa a verificar o filtro ativo:
```java
FiltroColaboradorOption filtro = cbFiltroColaborador.getValue();
Integer idColaboradorFiltro = (filtro != null && !filtro.isTodos()) ? filtro.idColaborador() : null;
// ... skip if idColaborador != idColaboradorFiltro
```

Quando um colaborador específico está selecionado, a descrição de cada evento simplifica-se (sem nome repetido): `"Manhã (Assistente)"` em vez de `"Manhã | João Silva (Assistente)"`.

---

### 4. Listener do filtro — refresh do calendário mensal

**Bug:** o listener do `cbFiltroColaborador` só chamava `aplicarFiltroColaborador()` (calendário semanal), não `atualizarCalendarioMensal()`.

**Fix:**
```java
cbFiltroColaborador.valueProperty().addListener((observavel, antigo, novo) -> {
    aplicarFiltroColaborador();
    atualizarCalendarioMensal();
});
```

---

### 5. Novos labels de identificação de contexto (FXML + Controller)

**Dois novos `fx:id` adicionados:**

#### `lblPeriodoPropostas` — Tab 2 "Propostas"
- Cabeçalho visual no topo da aba
- Mostra: `"Propostas de Julho 2026"` quando uma proposta está carregada
- Mostra: `"Seleciona um mês e gera uma proposta"` quando não há proposta

#### `lblIdentificacaoHorario` — Tab 3 "Calendário", secção mensal
- Mostra: `"A visualizar: Proposta #42 · Julho 2026 · rascunho"`
- Mostra: `"Sem proposta carregada"` quando não há proposta

Ambos atualizados em `preencherResultado()` e limpos em `limparResultado()`.

---

### 6. CSS — Novos estilos (`dashboard.css` — Bloco CSS-14)

**Bloco CSS-14 — Identificação de período e proposta ativa:**
- `.periodo-header-box` — fundo âmbar claro com borda laranja suave
- `.periodo-header-label` — texto vermelho Levi's 15px bold
- `.periodo-header-icon` — ícone de calendário escalado
- `.identificacao-horario-label` — label discreta no calendário mensal, fundo cinza claro

---

### Ficheiros alterados

| Ficheiro | Tipo de alteração |
|----------|-------------------|
| `BLL/GeracaoHorariosBLL.java` | `rotuloCurtoProposta` — inclui `nomeMes + ano` no rótulo |
| `Controller/GeracaoHorariosController.java` | Sync `periodoMensalAtual`, fix `obterEventosParaMes` com filtro, fix listener, novos `@FXML` fields, `limparResultado` atualizado |
| `resources/.../geracao-horarios-view.fxml` | Novos `lblPeriodoPropostas` (Tab 2) e `lblIdentificacaoHorario` (Tab 3) |
| `resources/.../dashboard.css` | CSS-14 — estilos para labels de identificação de contexto |

### Estado dos testes

**51 testes — 0 falhas — 0 erros** (verificado após todas as alterações desta sessão)

---

## 2026-06-07 — Sessão 12 (continuação)

### Problemas reportados
1. Não dava para selecionar qual proposta ver no calendário mensal sem ir à aba Propostas
2. O algoritmo gerava horários idênticos em gerações sucessivas

---

### 1. Seletor de proposta no calendário mensal

**Nova funcionalidade:** ComboBox `cbSelecaoProposta` adicionado na secção do calendário mensal (Tab 3).

- Apresenta todas as propostas do período selecionado (os mesmos itens de `cbComparacaoBase`)
- Sincroniza automaticamente quando uma proposta é carregada via tabela (Tab 2) ou via double-click
- Ao mudar a seleção no combo, carrega a proposta escolhida e atualiza o calendário
- Inclui guard anti-ciclo: o listener só dispara quando `novo.idProposta() != propostaAtual.idProposta()`

**Ficheiros alterados:**
- `geracao-horarios-view.fxml` — substituiu o simples `lblIdentificacaoHorario` por um `HBox` com o label + `cbSelecaoProposta`
- `GeracaoHorariosController.java` — novo `@FXML cbSelecaoProposta`, listener, sync em `preencherResultado` e limpeza em `limparResultado`, população em `aplicarListaPropostas`

---

### 2. Diversificação do algoritmo de geração

**Causa raiz:** `resolverCandidatosOrdenados` ordenava candidatos de forma 100% determinística por `pontuarAtribuicao()`. Com os mesmos dados de entrada, o resultado era sempre idêntico — independentemente da política ou do número de gerações.

**Fix em duas camadas:**

#### Camada 1 — Semente única por geração (`GeracaoHorariosBLL.java`)
```java
long sementeBase = System.nanoTime();
long sementeAlternativa = sementeBase ^ ((long)(alternativasExistentes + indice + 1) * 0x9e3779b97f4a7c15L);
```
Cada invocação de `gerarPropostas` usa `System.nanoTime()` como base → mesmo gerando a alternativa 1 duas vezes com a mesma política, a semente é diferente.

#### Camada 2 — Jitter determinístico no engine (`HorarioGeneratorEngine.java`)
```java
// Jitter per-colaborador: hash(semente XOR id) → [0, 12.0]
double jitterA = ((semente ^ (long) a.estado().idUtilizador() * 0x9e3779b97f4a7c15L) >>> 50) / 16383.0 * 12.0;
```

O jitter máximo é 12.0, inferior ao menor peso soft (20 para `totalFimDeSemanaTrabalhados`). Nunca anula preferências fortes (±55 a ±1000). Apenas desempata candidatos com pontuações equivalentes — produzindo distribuições de turnos genuinamente diferentes.

**Novo campo em `PedidoGeracao`:**
```java
long semente  // D: semente de diversificação
```

**Ficheiros alterados:**
- `HorarioGeneratorEngine.java` — `semente` em `PedidoGeracao`, jitter em `resolverCandidatosOrdenados`
- `GeracaoHorariosBLL.java` — `sementeDiversificacao` em `gerarPlaneamento`, seed único por alternativa em `gerarPropostas`

---

### Estado dos testes

**51 testes — 0 falhas — 0 erros** (verificado após todas as alterações)

O `GeracaoAlternativasValidationTest` continua a passar: o jitter é pequeno o suficiente para garantir cobertura completa (`turnos == diasDoMes × blocosCobertura`) e diferentes suficiente para manter `comparacao.diferencas()` não vazia.

---

## 2026-06-07 — Sessão 13

### Tema: Regras de Negócio em Horários Publicados (Permutas, Folgas Urgentes, Edição de Turno)

**Pedido do utilizador:** "quando um horário já está publicado e estamos já nesse mês desse horário, se um funcionário for pedir permuta de turno com outro funcionário não pode quebrar as regras iniciais do negócio, ou se pedir uma folga e precisar de faltar urgentemente..."

---

### 1. `HorarioLinha` — campo `idHorario` adicionado

**Problema:** O record `HorarioLinha` não transportava o ID do registo `Horario`, impossibilitando edição a partir da view.

**Fix:**
- `GeracaoHorariosBLL.java` — `HorarioLinha` passou a ter `Integer idHorario` como primeiro campo
- `construirLinhaHorario()` — passa `horario.getId()` como primeiro argumento

---

### 2. `PermutaBLL` — Validação de descanso mínimo pós-permuta (11h)

**Problema:** `validarPedido()` verificava antecedência de 24h e dias iguais, mas não verificava se a permuta viola o descanso mínimo obrigatório de 11h entre turnos de dias consecutivos.

**Fix em `PermutaBLL.java`:**
- Novo método `validarDescansoMinimoPosPermuta(Integer idColaborador, Turno turnoNovo, LocalDate data)`
- Consulta `horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(id, data-1, data-1)` e `…(id, data+1, data+1)`
- Para cada turno adjacente encontrado, calcula `Duration.between(fim-anterior, inicio-novo).toHours()`
- Lança `IllegalArgumentException` se o gap for inferior a 11h
- Chamado **para ambos os colaboradores** em `validarPedido()`, antes de `validarAntecedenciaMinima()`

---

### 3. `DayOffBLL` — Suporte a ausências urgentes + impacto de cobertura

**Problema:** O fluxo de pedido de folga sempre exigia 24h de antecedência, impedindo ausências de emergência.

**Fix em `DayOffBLL.java`:**

#### 3a. Bypass da regra 24h para tipo "urgente"
```java
if (!"urgente".equalsIgnoreCase(pedido.getTipo())) {
    validarAntecedenciaMinimaDoTurno(pedido.getIdUtilizador().getId(), pedido.getDataAusencia());
}
```

#### 3b. Novo record `ResultadoAprovacaoFolga`
```java
public record ResultadoAprovacaoFolga(
    DayOff pedido,
    boolean temAvisoCobertura,
    String avisoCobertura,
    int trabalhadoresRestantesNoTurno
) {}
```

#### 3c. Novo método `aprovarPedidoFolgaComCobertura()`
- Chama `aprovarPedidoFolga()` internamente
- Conta trabalhadores restantes na loja nesse dia via `findHorariosDaLojaNoDia()`
- Retorna aviso se ficarem 0 ou apenas 1 trabalhador escalado

---

### 4. `PedirFolgaController` — Tipo "Urgente / Emergência"

**Fix:**
- `cbTipo` — adicionado item `"Urgente / Emergência"`
- `mapearTipoParaBaseDados()` — `"Urgente / Emergência"` → `"urgente"`
- `formatarTipo()` — `"urgente"` → `"⚡ Urgente"`
- `tratarPedidoSelecionado()` — ao aprovar pedido urgente, chama `aprovarPedidoFolgaComCobertura()` e mostra aviso de cobertura em caixa de informação

---

### 5. `HorarioBLL` — Edição de turno publicado

**Fix em `HorarioBLL.java`:**

Novas injeções:
- `TurnoRepository turnoRepository`
- `HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository`

Novos métodos:
- `listarTodosOsTurnos()` — retorna todos os turnos via `findAllByOrderByHoraInicioAsc()`
- `editarTurnoPublicado(idHorario, idNovoTurno, idAprovador, motivoAlteracao)`:
  - Verifica que o aprovador é gerente/subgerente/supervisor da loja ativa
  - Valida descanso mínimo de 11h com turnos dos dias adjacentes (mesma lógica que PermutaBLL)
  - Regista entrada em `HistoricoHorarioEstado` com turno anterior/novo e motivo
  - Persiste o novo turno no `Horario`

---

### 6. `GeracaoHorariosController` — Botão "Editar turno" no diálogo de detalhe do dia

**Fix:**
- Injeção de `HorarioBLL horarioBLL` no construtor
- `criarCardDetalheTurno()` — adiciona botão `"Editar turno"` (styleClass `botao-editar-turno`) quando `podeGerar && linha.idHorario() != null`
- Novo método `abrirEdicaoTurno(HorarioLinha linha)`:
  - Abre `ChoiceDialog<Turno>` com todos os turnos disponíveis
  - Aplica `StringConverter` para mostrar tipo + horaInicio — horaFim
  - Após confirmação, chama `horarioBLL.editarTurnoPublicado()`
  - Recarrega a proposta atual para refletir a alteração

---

### Ficheiros alterados

| Ficheiro | Alteração |
|---|---|
| `BLL/GeracaoHorariosBLL.java` | `HorarioLinha` + `idHorario`; `construirLinhaHorario` passa `horario.getId()` |
| `BLL/PermutaBLL.java` | `validarDescansoMinimoPosPermuta()` + chamadas em `validarPedido()` |
| `BLL/DayOffBLL.java` | bypass 24h para urgente; `ResultadoAprovacaoFolga`; `aprovarPedidoFolgaComCobertura()` |
| `Controller/PedirFolgaController.java` | tipo "Urgente / Emergência"; mapeamentos; aprovação com aviso cobertura |
| `BLL/HorarioBLL.java` | injeção `TurnoRepository` + `HistoricoHorarioEstadoRepository`; `listarTodosOsTurnos()`; `editarTurnoPublicado()` |
| `Controller/GeracaoHorariosController.java` | injeção `HorarioBLL`; botão editar em `criarCardDetalheTurno`; `abrirEdicaoTurno()` |

---

### Estado dos testes

**51 testes — 0 falhas — 0 erros** ✅

---

## 2026-06-07 — Sessão 13 (continuação) — Bugs corrigidos + validação end-to-end

### Bugs descobertos e corrigidos

#### Bug 1 — ChoiceDialog aparecia ATRÁS do diálogo de detalhe (z-order)

**Sintoma:** ao clicar "Editar turno" dentro do diálogo "DETALHE DO DIA", o `ChoiceDialog` aparecia atrás do diálogo modal existente. Toda a interface ficava bloqueada (os botões "Fechar" e Escape deixavam de responder). Necessário `taskkill //F //IM java.exe` para recuperar.

**Causa raiz:** o `ChoiceDialog.showAndWait()` era chamado sem `initOwner()`, pelo que o dialog se associava ao Stage primário da aplicação — que estava bloqueado pelo diálogo modal de detalhe.

**Fix em `GeracaoHorariosController.java`:**
1. No botão "Editar turno" (`criarCardDetalheTurno`), capturar o owner no momento do click:
   ```java
   javafx.stage.Window owner = btnEditar.getScene() != null
       ? btnEditar.getScene().getWindow()
       : obterJanela();
   abrirEdicaoTurno(turno, owner);
   ```
2. `abrirEdicaoTurno` recebe agora `javafx.stage.Window owner` como parâmetro
3. Antes de `showAndWait()`:
   ```java
   if (owner != null) {
       dialogo.initOwner(owner);
   }
   ```

**Resultado:** o ChoiceDialog aparece corretamente em cima do diálogo "DETALHE DO DIA". Confirmado visualmente com screenshot.

---

#### Bug 2 — Diálogo "DETALHE DO DIA" ficava com dados desatualizados após edição

**Sintoma:** depois de editar um turno com sucesso, o diálogo de detalhe permanecia aberto mas continuava a mostrar o horário antigo (e.g., "10:00 - 19:00" após ter mudado para "12:00 - 21:00"). O utilizador tinha de fechar manualmente e reabrir para ver o dado novo.

**Fix em `abrirEdicaoTurno()`:**
```java
// Fechar o diálogo de detalhe do dia (owner) para evitar dados desatualizados
if (owner instanceof javafx.stage.Stage ownerStage) {
    ownerStage.close();
}
```
O diálogo de detalhe é fechado automaticamente após uma edição bem-sucedida. O calendário mensal já se atualizava via `carregarPropostaPorIdEmSegundoPlano`.

---

### Validação end-to-end (testes manuais via live app)

| Funcionalidade | Resultado | Detalhe |
|---------------|-----------|---------|
| Folga urgente (bypass 24h) | ✅ | Submissão de folga para "hoje" aceite sem erro de antecedência |
| Botão "Editar turno" visível | ✅ | Aparece em todos os cards do diálogo "DETALHE DO DIA" para o gerente |
| ChoiceDialog z-order | ✅ | Aparece corretamente por cima do diálogo pai |
| Dropdown de turnos | ✅ | Mostra todos os turnos com formato "tipo HH:MM — HH:MM" |
| Edição persistida na DB | ✅ | Henrique Siano: "manha 10:00-19:00" → "intermedio 12:00-21:00" confirmado no calendário mensal |
| Calendário atualizado após edição | ✅ | A célula do dia 1 de julho passou a mostrar "12:00-21:00 · Henrique Siano" |
| Diálogo fecha após edição | ✅ | (fix Bug 2 aplicado nesta sessão) |

### Ficheiros alterados (esta continuação)

| Ficheiro | Alteração |
|----------|-----------|
| `Controller/GeracaoHorariosController.java` | Bug 1 (captura owner + `initOwner`); Bug 2 (fecha owner stage após edição bem-sucedida) |

### Estado dos testes

**51 testes — 0 falhas — 0 erros** ✅

---

## 2026-06-07 — Sessão 14

### 1. CSS — Botão "Editar turno" estilizado (`dashboard.css`)

O botão `.botao-editar-turno` não tinha CSS — aparecia como botão JavaFX cinzento padrão.

**Adicionado ao bloco CSS-11:**
- Normal: fundo branco, borda vermelha Levi's, texto vermelho 11px/700, border-radius 6px
- `:hover` — fundo vermelho, texto branco
- `:pressed` — fundo vermelho escuro, texto branco

---

### 2. Bug fix — `AuditoriaBLL.normalizar()` — NullPointerException silenciosa

**Sintoma:** a página de Auditoria mostrava sempre "Não foi possível carregar o histórico de auditoria."

**Causa raiz:** `normalizar(String valor)` chamava `valor.toLowerCase()` sem verificar null. Sempre que um `EventoAuditoria` tinha campo nulo (ex.: `idUtilizador` null em registo de falha de login), o stream lançava NPE capturada pelo `catch (Exception e)` genérico.

**Fix:**
```java
private String normalizar(String valor) {
    if (valor == null) return null;  // era: return valor.toLowerCase(Locale.ROOT);
    return valor.toLowerCase(Locale.ROOT);
}
```

Todos os usos já estavam protegidos para retorno null (`formatarEtiqueta` tem guard, stream filtra null, `equals()` sobre null retorna false).

**Resultado:** a página de Auditoria passa a carregar corretamente — eventos de login/logout/alterações sensíveis visíveis para o gerente.

---

### Ficheiros alterados

| Ficheiro | Alteração |
|----------|-----------|
| `resources/.../dashboard.css` | CSS-11: `.botao-editar-turno` + `:hover` + `:pressed` |
| `BLL/AuditoriaBLL.java` | `normalizar()` null-safe |

### Estado dos testes

**51 testes — 0 falhas — 0 erros** ✅

---

## Sessão 15 — Auditoria profunda de UX + Melhorias de Interface Intuitiva

**Data:** 2026-06-07

### Objetivo

Auditoria completa de funcionalidades e fluxos de UX, identificação de todos os bugs e melhorias pendentes, implementação de tudo o que foi encontrado.

---

### Bugs corrigidos

#### 1. Tooltips com encoding errado no PainelGerentePedidosController

Os tooltips dos botões de atalho tinham mojibake UTF-8→Latin-1:
- `"Abrir mÃ³dulo de folgas (Ctrl+1)"` → `"Abrir módulo de folgas (Ctrl+1)"`
- `"Abrir mÃ³dulo de permutas (Ctrl+2)"` → `"Abrir módulo de permutas (Ctrl+2)"`
- `"Abrir mÃ³dulo de preferÃªncias (Ctrl+3)"` → `"Abrir módulo de preferências (Ctrl+3)"`
- `"Abrir mÃ³dulo de horÃ¡rios (Ctrl+4)"` → `"Abrir módulo de horários (Ctrl+4)"`

#### 2. Formato de datas ISO em vez de DD/MM/YYYY no PedirFolgaController

As colunas `colDataPedido` e `colDataPendente` usavam `String.valueOf(data)` que produz ISO-8601 (`2026-07-15`). Corrigido para `data.format(DATA_FORMATTER)` com `DateTimeFormatter.ofPattern("dd/MM/yyyy")`. Também corrigido no dialog de confirmação.

#### 3. Confirmação de permuta genérica no PermutasController

O dialog de confirmação mostrava apenas "A aprovação ficará registada no sistema." sem contexto. Agora mostra nome + turno de ambos os colaboradores envolvidos na troca.

---

### Novas funcionalidades

#### 4. Cancelar pedido de folga próprio

Botão `✕` inline na coluna "Estado" da tabela histórico, visível apenas para pedidos pendentes.

- **BLL:** `DayOffBLL.cancelarPedidoProprio(idDayOff, idUtilizador)` — valida posse + estado pendente; muda para `"cancelado"`
- **Controller:** dialog de confirmação contextual, recarrega histórico
- **UI:** `HBox(badge, btnCancelar)` com CSS `.botao-cancelar-pedido`

#### 5. Cancelar pedido de permuta próprio

Mesmo padrão. O estado `cancelado` já existia no enum `EstadoPermuta`.

- **BLL:** `PermutaBLL.cancelarPedidoProprio(idPermuta, idUtilizador)` — verifica solicitante (origem), muda para `EstadoPermuta.cancelado`
- **Controller:** `PermutasController.cancelarPermutaPropria(Permuta)`

#### 6. Badges coloridos nas tabelas de histórico

`TableCell` factories com `Label` colorida por estado em todas as tabelas:

| Tabela | Coluna | Ficheiro |
|--------|--------|----------|
| Histórico folgas | `colEstadoPedido` | `PedirFolgaController` |
| Histórico permutas | `colEstadoPermuta` | `PermutasController` |
| Histórico preferências | `colEstado` | `PreferenciasController` |
| Histórico decisões | `colEstadoHistorico` | `PreferenciasController` |

Pendente → amarelo, Aprovado → verde, Rejeitado → vermelho, Cancelado/outros → cinza.

#### 7. Tooltips nos campos de formulário (3 módulos)

Pedir Folga (`dpData`, `cbTipo`, `txtMotivo`), Permutas (`cbMeuTurno`, `cbColegaElegivel`, `cbTurnoColega`), Preferências (`cbTipo`, `dpDataInicio`, `dpDataFim`, `txtDescricao`).

#### 8. `formatarEstado` actualizado com "cancelado"

Nos controllers `PedirFolgaController` e `PermutasController`.

---

### CSS adicionado ao dashboard.css

Novas classes: `.badge-estado` (base), `.badge-rascunho`, `.badge-folga`, `.badge-enviado`, `.botao-cancelar-pedido` e `:hover`.

---

### Ficheiros alterados

| Ficheiro | Alteração |
|----------|-----------|
| `Controller/PainelGerentePedidosController.java` | Fix encoding tooltips (×4 strings mojibake) |
| `Controller/PedirFolgaController.java` | Data DD/MM/YYYY + badges + tooltips + cancelar + formatarEstado |
| `Controller/PermutasController.java` | Confirmação contextual + badges + tooltips + cancelar + formatarEstado |
| `Controller/PreferenciasController.java` | Badges (colEstado + colEstadoHistorico) + tooltips formulário |
| `BLL/DayOffBLL.java` | `cancelarPedidoProprio(idDayOff, idUtilizador)` |
| `BLL/PermutaBLL.java` | `cancelarPedidoProprio(idPermuta, idUtilizador)` |
| `resources/.../dashboard.css` | `.badge-estado`, `.badge-rascunho`, `.badge-folga`, `.badge-enviado`, `.botao-cancelar-pedido` |

### Estado dos testes

**51 testes — 0 falhas — 0 erros** ✅

---

## Sessão 16 — Interface Intuitiva: 4 Áreas de Melhoria

**Data:** 2026-06-07

### Objetivo

Implementar todas as 4 áreas identificadas na sessão 15 para tornar a interface mais intuitiva e fluída:
- **A** — Empty state CTAs (botões de ação nos estados vazios das tabelas)
- **B** — Rich feedback (auto-dismiss das mensagens de sucesso após 5 s)
- **C** — Navigation shortcuts (atalhos Alt+tecla globais + tooltips na sidebar)
- **D** — Home contextual hub (banner de pedidos pendentes para gestores)

---

### Área B — Auto-dismiss de feedback de sucesso

Adicionado `PauseTransition(Duration.seconds(5))` que esconde automaticamente a mensagem de sucesso em todos os controllers com feedback visual:

| Controller | Método | Comportamento |
|------------|--------|---------------|
| `PainelGerentePedidosController` | `mostrarFeedback()` | Desaparece após 5 s (só sucesso) |
| `PermutasController` | `mostrarMensagem()` | Desaparece após 5 s (só sucesso) |
| `PreferenciasController` | `mostrarFeedback()` | Desaparece após 5 s (só sucesso) |
| `PreferenciasController` | `mostrarFeedbackGestao()` | Desaparece após 5 s (só sucesso) |

---

### Área C — Navigation shortcuts globais + Tooltips sidebar

**DashboardController:**

1. **Tooltips na sidebar** — `configurarTooltipsSidebar()` chamado em `initialize()`:
   - `btnDashboard` → `"Painel inicial  (Alt+1)"`
   - `btnFolgas` → `"Folgas e ausências  (Alt+2)"`
   - `btnPermutas` → `"Trocar turnos  (Alt+3)"`
   - `btnPreferencias` → `"Preferências de horário  (Alt+4)"`
   - `btnPerfil` → `"O teu perfil  (Alt+5)"`
   - Botões de gestão (null-safe): `Alt+H`, `Alt+G`, `Alt+L`, `Alt+F`

2. **FadeTransition** — `mudarEcraCentro()` anima cada módulo com fade de 0→1 em 180 ms

3. **Atalhos Alt+digit/letra globais** — `processarShortcutGlobal(KeyEvent)` registado via `addEventFilter` na scene:
   - Ignorado quando o foco está em `TextInputControl`
   - `Alt+1..5` para módulos base; `Alt+H/G/L/F` para módulos de gestão (só se visíveis)

---

### Área A — Empty state CTAs

Substituiu o simples `Label` estático por um `VBox` programático com título, subtítulo e botão CTA:

| Controller | Tabela | Botão CTA | Ação |
|------------|--------|-----------|------|
| `PedirFolgaController` | `tabelaPedidos` | "Fazer o meu primeiro pedido" | `dpData.requestFocus()` |
| `PermutasController` | `tabelaPedidosPermuta` | "Propor uma troca agora" | `cbMeuTurno.requestFocus()` |

CSS já existente reutilizado: `.empty-state-titulo`, `.empty-state-subtitulo`, `.botao-acao`.

---

### Área D — Home contextual banner (gestores)

**FXML (`home-view.fxml`):** Novo `HBox fx:id="bannerPendentes"` (hidden por default) inserido entre o HERO e a cobertura semanal. Usa `FontIcon mdi2a-alert-circle-outline`.

**HomeController:**
- `@FXML HBox bannerPendentes`, `@FXML Label lblBannerPendentes`, `@FXML Button btnBannerVerPedidos`
- `atualizarBannerPendentes()` — chama `DayOffBLL.listarPedidosPendentesParaAprovacao()` + `PermutaBLL.listarPedidosPendentesParaAprovacao()`, soma totais; se > 0 mostra banner com mensagem `"Tens N pedido(s) pendente(s) a aguardar a tua aprovação."`
- `esconderBannerPendentes()` — `managed=false`, `visible=false`
- `onBannerVerPedidosClick()` — navega para `dashboardNavigation.abrirPainelGerente()`
- Chamado em `configurarVisibilidadePainelOperacao()` — no branch `podeGerirLoja=true`

**CSS (`dashboard.css`):** Novas classes `.home-banner-pendentes` (fundo âmbar suave), `.home-banner-pendentes-icon` (cor #b45309), `.home-banner-pendentes-texto`, `.home-banner-pendentes-btn` e `:hover`.

---

### Ficheiros alterados (Sessão 16)

| Ficheiro | Alteração |
|----------|-----------|
| `Controller/DashboardController.java` | FadeTransition, tooltips sidebar, atalhos globais Alt+tecla |
| `Controller/PainelGerentePedidosController.java` | Auto-dismiss feedback 5 s |
| `Controller/PermutasController.java` | Auto-dismiss feedback 5 s + empty state CTA |
| `Controller/PreferenciasController.java` | Auto-dismiss feedback 5 s (×2 métodos) |
| `Controller/PedirFolgaController.java` | Empty state CTA |
| `Controller/HomeController.java` | Banner pendentes (campos, lógica, handler) |
| `resources/.../home-view.fxml` | `HBox bannerPendentes` com icon + label + botão |
| `resources/.../dashboard.css` | `.home-banner-pendentes*` (5 regras novas) |

### Estado dos testes

**51 testes — 0 falhas — 0 erros** ✅

---

## Sessão 17 — Redesenho Visual Total: Module Banners + Perfil Hero

**Data:** 2026-06-07

### Objetivo

Redesenho completo dos cabeçalhos de todos os módulos da aplicação Desktop, dando a cada ecrã uma identidade cromática própria e uma presença visual "AAA" imediatamente reconhecível. Introduzida uma linguagem de **module banners** de dois tipos:

- **Full-bleed** — ocupa toda a largura do ecrã, sem padding exterior (para módulos cujo VBox título é o primeiro filho direto de `conteudo-pagina`).
- **Floating card** (`modulo-banner-card`) — banner com `border-radius: 16px` e sombra, dentro de containers com padding (para módulos onde o header fica dentro de `VBox spacing="32"` com `padding: 40px`).

---

### CSS adicionado ao `dashboard.css` (fim do ficheiro)

**Bloco `/* Session 17 — Module Banners */`:**

| Classe | Cor / Gradiente |
|--------|----------------|
| `.modulo-banner-folga` | Âmbar `#b45309` → Terracota `#7c2d12` |
| `.modulo-banner-permutas` | Azul `#1d4ed8` → Índigo escuro `#1e1b4b` |
| `.modulo-banner-horarios` | Vermelho Levi's `#c91428` → `#7f0010` |
| `.modulo-banner-loja` | Verde escuro `#065f46` → `#022c22` |
| `.modulo-banner-funcionarios` | Slate `#1e293b` → `#0f172a` |
| `.modulo-banner-pedidos` | Vermelho escuro `#9b1c1c` → `#450a0a` |
| `.modulo-banner-relatorios` | Azul navy `#1e3a5f` → `#0c2340` |
| `.modulo-banner-auditoria` | Cinza escuro `#374151` → `#111827` |

**Classes de texto e estrutura:**
- `.modulo-titulo` — `32px / 900 / #ffffff`
- `.modulo-subtitulo` — `14px / 500 / rgba(255,255,255,0.78)`
- `.modulo-banner-card` — `border-radius: 16px`, `border: 1px rgba(255,255,255,0.15)`, sombra `dropshadow(gaussian,...)`

**Banner do perfil:**
- `.perfil-banner` — gradiente diagonal vermelho Levi's `#c91428 → #7f0010`, `min-height: 200px`
- `.perfil-avatar-circle` — círculo semi-transparente branco com borda e `border-radius: 999px`
- `.perfil-avatar-inicial` — `36px / 900 / #ffffff`

**Melhorias gerais (cards, tabelas, KPIs):**
- `.bento-card:hover` — sombra mais pronunciada + borda mais visível
- `.home-table .column-header` — cabeçalho de tabela: fundo `#f7f2f1`, borda inferior 2px, texto `11px/800/#5c403e`
- `.home-table .table-row-cell:hover` — fundo `rgba(201,20,40,0.04)` (vermelho Levi's ultra-suave)
- `.kpi-tile-pendente` / `.kpi-tile-aprovado` / `.kpi-tile-total` — fundos coloridos por tipo com borda suave
- `.conteudo-scroll .scroll-bar .thumb` — thumb da scrollbar afinado

---

### FXML alterados — Module Banners aplicados

| Módulo | Ficheiro FXML | Tipo de banner | Cor temática |
|--------|---------------|----------------|--------------|
| Pedidos de Ausência | `pedir-folga-view.fxml` | Full-bleed | Âmbar/Terracota |
| Trocar Turno (Permutas) | `permutas-view.fxml` | Full-bleed | Azul/Índigo |
| Geração de Horários | `geracao-horarios-view.fxml` | Full-bleed | Vermelho Levi's |
| Loja e Regras | `gestao-loja-view.fxml` | Full-bleed | Verde escuro |
| Gestão de Funcionários | `gestao-funcionarios-view.fxml` | Full-bleed | Slate/Navy |
| Painel de Pedidos | `painel-gerente-pedidos-view.fxml` | Floating card | Vermelho escuro |
| Relatórios de Horas | `relatorios-horas-view.fxml` | Floating card | Azul navy |
| Auditoria | `painel-auditoria-view.fxml` | Floating card | Cinza escuro |
| Perfil | `perfil-view.fxml` | Perfil hero (existente melhorado) | Vermelho Levi's diagonal |

**Estrutura padrão (full-bleed):**
```xml
<VBox spacing="8.0" styleClass="modulo-banner modulo-banner-XXXX">
    <padding><Insets bottom="36.0" left="48.0" right="48.0" top="36.0" /></padding>
    <Label styleClass="modulo-titulo" text="Título do módulo" />
    <Label styleClass="modulo-subtitulo" text="Descrição breve." wrapText="true" />
</VBox>
```

**Estrutura padrão (floating card):**
```xml
<VBox spacing="8.0" styleClass="modulo-banner modulo-banner-card modulo-banner-XXXX">
    <Label styleClass="modulo-titulo" text="Título do módulo" />
    <Label styleClass="modulo-subtitulo" text="Descrição breve." wrapText="true" />
</VBox>
```

---

### Melhorias no `perfil-view.fxml`

- `StackPane` do hero: `page-banner` → `page-banner perfil-banner`, `min-height: 220` → `200`, margens removidas (edge-to-edge)
- Avatar `StackPane`: adicionada classe `perfil-avatar-circle` (fundo branco semi-transparente, borda, radius 999px); tamanho `88` → `96` / radius `44` → `48`
- Avatar `Label`: adicionada classe `perfil-avatar-inicial` (36px bold branco)

---

### Ficheiros alterados

| Ficheiro | Alteração |
|----------|-----------|
| `resources/.../dashboard.css` | Bloco Session 17: module banners, perfil banner, avatar, cards/tables/KPIs |
| `dashboard/pedir-folga-view.fxml` | Banner full-bleed âmbar/terracota |
| `dashboard/permutas-view.fxml` | Banner full-bleed azul/índigo |
| `dashboard/geracao-horarios-view.fxml` | Banner full-bleed vermelho Levi's |
| `dashboard/gestao-loja-view.fxml` | Banner full-bleed verde escuro |
| `dashboard/gestao-funcionarios-view.fxml` | Banner full-bleed slate/navy |
| `dashboard/painel-gerente-pedidos-view.fxml` | Banner floating card vermelho escuro |
| `dashboard/relatorios-horas-view.fxml` | Banner floating card azul navy |
| `dashboard/painel-auditoria-view.fxml` | Banner floating card cinza escuro |
| `dashboard/perfil-view.fxml` | Hero banner melhorado + avatar estilizado |

### Estado dos testes

**51 testes — 0 falhas — 0 erros** ✅

Nota: alterações são exclusivamente CSS e FXML de módulo. Os 51 testes carregam apenas `dashboard-view.fxml` + `home-view.fxml` — os banners de módulo não estão cobertos por testes automáticos, mas os FXML foram verificados como XML bem-formado.

---

## 2026-06-07 — Sessão 18

### Diagnóstico e fix dos banners de módulo (fundo gradient não renderizava)

**Problema raiz identificado:**  
Os `VBox` e `StackPane` com classes CSS (e.g., `.modulo-banner-folga`, `.perfil-banner`) **não renderizavam o `-fx-background-color`** via CSS class, embora propriedades de texto (`.modulo-titulo`, `.modulo-subtitulo`) funcionassem corretamente.

**Causa provável:** JavaFX CSS engine não aplica `-fx-background-color` a containers (`VBox`, `StackPane`) via classes externas quando carregados como conteúdo de módulo em `BorderPane.center`. O atributo `style=""` inline tem prioridade máxima e funciona sempre.

**Fix aplicado:** Adicionado atributo `style="-fx-background-color: ...; -fx-background-insets: 0; -fx-background-radius: ...;"` diretamente em cada VBox/StackPane de banner nos 9 FXMLs de módulo.

---

### FXMLs atualizados — inline style adicionado

| Módulo | Ficheiro | Tipo | Gradiente inline |
|--------|----------|------|-----------------|
| Folgas | `pedir-folga-view.fxml` | Full-bleed VBox | `#b45309 → #7c2d12` |
| Permutas | `permutas-view.fxml` | Full-bleed VBox | `#1d4ed8 → #1e1b4b` |
| Horários | `geracao-horarios-view.fxml` | Full-bleed VBox | `#c91428 → #7f0010` |
| Loja e Regras | `gestao-loja-view.fxml` | Full-bleed VBox | `#065f46 → #022c22` |
| Funcionários | `gestao-funcionarios-view.fxml` | Full-bleed VBox | `#1e293b → #0f172a` |
| Pedidos | `painel-gerente-pedidos-view.fxml` | Card VBox | `#9b1c1c → #450a0a`, `border-radius: 16px` |
| Relatórios | `relatorios-horas-view.fxml` | Card VBox | `#1e3a5f → #0c2340`, `border-radius: 16px` |
| Auditoria | `painel-auditoria-view.fxml` | Card VBox | `#374151 → #111827`, `border-radius: 16px` |
| Perfil | `perfil-view.fxml` | Hero StackPane | `#c91428 → #7f0010` + textos brancos + avatar inline |

---

### Correcções adicionais no Perfil (`perfil-view.fxml` + `PerfilController.java`)

**FXML:**
- `StackPane` hero: adicionado `style="linear-gradient vermelho"` + `background-insets/radius: 0`
- `Circle` (avatar-bg): adicionado `style="-fx-fill: rgba(255,255,255,0.22); -fx-stroke: rgba(255,255,255,0.35); -fx-stroke-width: 2;"`
- `Label` banner-titulo: adicionado `style="-fx-text-fill: #ffffff;"` (descendant CSS não funcionava)
- `Label` banner-subtitulo: adicionado `style="-fx-text-fill: rgba(255,255,255,0.78);"`
- `Label` avatar-inicial: adicionado `fx:id="lblPerfilAvatar"` + `style="-fx-text-fill: #ffffff; -fx-font-size: 36px; -fx-font-weight: 700;"`

**PerfilController.java:**
- Adicionado campo `@FXML private Label lblPerfilAvatar;`
- Em `setUtilizadorLogado()`: `lblPerfilAvatar.setText(String.valueOf(resumo.nome().charAt(0)).toUpperCase())` — avatar dinâmico com inicial real do utilizador

---

### Resultado visual (confirmado na app em execução)

- **Folgas** — banner âmbar/terracota diagonal ✅
- **Permutas** — banner azul índigo ✅
- **Horários** — banner vermelho Levi's ✅
- **Loja e Regras** — banner verde floresta ✅
- **Funcionários** — banner slate navy escuro ✅
- **Pedidos** — banner card vermelho-escuro arredondado ✅
- **Relatórios** — banner card navy arredondado ✅
- **Perfil** — hero banner vermelho com avatar inicial dinâmico e textos brancos ✅

---

### Estado dos testes

**51 testes — 0 falhas — 0 erros** ✅

---

## Sessão 19 — Web Endpoints T7: W1/W2/W3/W5 implementados

**Data:** 2026-06-07

### Objetivo

Continuar implementação dos endpoints Web em falta do T7. Esta sessão completa W1, W2, W3 e W5.

---

### W1 + W2 — Horários Web: Gerar e Enviar ao Supervisor

**Ficheiros alterados:**
- `WebAppService.java` — Adicionados `podeGerarHorarios` e `podeVerAuditoria` a `WebPermissoes` (record expandido de 8 → 10 campos; `semAcesso()` atualizado)
- `WebHorariosController.java` — Adicionados:
  - `POST /web/horarios/gerar` (W2): chama `geracaoHorariosBLL.gerarProposta(utilizadorId, ano, mes)`, redireciona com mensagem de sucesso ou erro
  - `POST /web/horarios/propostas/{id}/enviar` (W1): chama `geracaoHorariosBLL.enviarPropostasParaValidacao(utilizadorId, List.of(id))`, redireciona
  - GET enriquecido: para gerentes, carrega `listarPropostas(utilizadorId, ano, mes)` → model `propostas`
- `horarios.html` — Novas secções (só visíveis para `webPermissoes.podeGerarHorarios()`):
  - **Gerar proposta mensal** — formulário com seletor ano/mês + botão "Gerar proposta" (POST `/web/horarios/gerar`)
  - **Propostas de X/Y** — tabela com rotulo, estado (badge colorido), qualidade, score, data geração, colaboradores; botão "Enviar ao supervisor" (para propostas em rascunho), indicadores de estado para pendente/aprovado/rejeitado

---

### W3 — Painel de Auditoria Web

**Ficheiros criados:**
- `WebAuditoriaController.java` — `GET /web/auditoria` com filtros opcionais: `tipo`, `colaborador`, `dataInicio`, `dataFim` (ISO date). Chama `auditoriaBLL.carregarPainel()`.
- `src/main/resources/templates/web/auditoria.html` — Página completa:
  - 4 cards de resumo: Total eventos / Falhas / Autenticações / Sensíveis
  - Formulário de filtros (tipo, colaborador, data início, data fim) + botão Filtrar / Limpar
  - Tabela de histórico: Data/Hora, Tipo (badge neu), Resultado (badge ok/err), Colaborador, Origem, Detalhes (truncado com title completo)

**Ficheiros alterados:**
- `WebAppService.java` — `AuditoriaBLL` injetado; `podeVerAuditoria` adicionado a `WebPermissoes`
- `fragments.html` — Adicionado link "Auditoria" (ícone `shield`) na secção "Gestão" do sidebar, visível apenas quando `webPermissoes.podeVerAuditoria()`

---

### W5 — Redefinir password de colaborador pelo gestor

**Ficheiros alterados:**
- `GestaoFuncionariosBLL.java` — Novo método `@Transactional resetarPasswordColaborador(Integer idGestor, Integer idColaborador, String novaPassword)`:
  - Valida permissão (gerente/subgerente)
  - Verifica que o colaborador pertence à loja
  - Chama `segurancaBLL.prepararPasswordParaPersistencia(novaPassword, true)`
  - Salva via `utilizadorRepository.save(colaborador)`
  - Regista evento de auditoria `"alteracao_password"` com contexto "gestao_funcionarios"
- `WebEquipaController.java` — Novo endpoint `POST /web/equipa/{id}/reset-password` (W5), com parâmetro `novaPassword`
- `equipa.html` — Novo card "Redefinir password" (visível apenas quando `podeEditarColaborador`): campo password único + botão "Redefinir" com confirmação

---

### Estado dos testes

**51 testes — 0 falhas — 0 erros** ✅

### W6/W7 — Exportação PDF (Relatórios e Horários)

**Nota:** W6 e W7 foram implementados na mesma sessão 19 mas documentados separadamente.

**Ficheiros criados:**
- `WebPdfService.java` — `@Service` com PDFBox 3.x: dois métodos públicos:
  - `gerarRelatorioHorasPdf(RelatorioResultado)` — tabela com colunas Colaborador, Cargo, Turnos, Folgas, Horas
  - `gerarHorarioMensalPdf(List<Horario>, ano, mes, nomeUtilizador)` — tabela com Data, Dia, Turno, Início, Fim
  - `sanitize()` interno transliteração ISO-8859-1 (Type1 PDFBox não suporta UTF-8)

**Ficheiros alterados:**
- `WebRelatoriosController.java` — Novo endpoint `GET /web/relatorios/exportar.pdf` (W6); `WebPdfService` injetado
- `relatorios.html` — Botões CSV + PDF lado a lado no header
- `WebHorariosController.java` — Novo endpoint `GET /web/horarios/exportar.pdf` (W7); `WebPdfService` injetado
- `horarios.html` — Botão PDF no header da secção de turnos (visível apenas quando turnos não vazios)

---

## Sessão 20 — Auditoria Visual + Polimento Web

**Data:** 2026-06-07

### Objetivo

Verificar testes após Session 19 (W6/W7) + auditoria visual de todas as páginas web + corrigir problemas encontrados.

### Verificação inicial

- **51/51 testes** passam após W6/W7 ✅

### Auditoria Visual — Resultados

Todas as 8 páginas web testadas via curl (com sessão autenticada como `francisco.gomes@levis.com`):

| Página | HTTP | Conteúdo |
|--------|------|----------|
| `/web/painel` | 200 ✅ | Welcome banner "Olá, Francisco!", todos os links nav presentes |
| `/web/horarios` | 200 ✅ | Secção "Gerar proposta mensal" visível (gerente), PDF button condicional |
| `/web/relatorios` | 200 ✅ | PDF + CSV buttons, KPI cards, tabela, ambos exports funcionam |
| `/web/complementares` | 200 ✅ | 3 cards (folga, preferência, permuta) + secção aprovação gestor |
| `/web/equipa` | 200 ✅ | Card "Redefinir password" presente ao selecionar colaborador |
| `/web/gestao-loja` | 200 ✅ | Secções loja, regras, horário funcionamento |
| `/web/auditoria` | 200 ✅ | 170 eventos, 4 falhas, filtros funcionais |
| `/web/perfil` | 200 ✅ | Formulário alteração password |

**Exports PDF verificados:**
- `GET /web/relatorios/exportar.pdf` → HTTP 200, `%PDF` válido, 1140 bytes ✅
- `GET /web/horarios/exportar.pdf` → HTTP 200, `%PDF` válido, 919 bytes ✅

### Problemas Encontrados e Corrigidos

#### Fix 1 — Etiquetas de eventos de auditoria sem acentos (UX)

**Problema:** `formatarEtiqueta("alteracao_password")` → "Alteracao Password" (sem acento).

**Correção em `AuditoriaBLL.java`:** Adicionado `ETIQUETAS_PT` — mapa estático de código → label PT correcto:
- `alteracao_password` → "Alteração de password"
- `login` → "Autenticação"
- `logout` → "Terminar sessão"
- `sessao_expirada` → "Sessão expirada"
- `colaborador_criado/atualizado/desativado` → labels corretas
- (+ horario_publicado, proposta_*, folga_*, permuta_* para futuro)

Fallback genérico mantido para tipos desconhecidos.

**Verificado:** labels correctas em filtro e tabela de eventos ✅

#### Fix 2 — Configurações JPA em `application.properties`

- `spring.jpa.show-sql=false` (era `true` — gerava log excessivo em produção)
- `spring.jpa.open-in-view=false` (elimina WARN e força transações a fechar antes do render)

### Estado dos Testes

**51/51 testes — 0 falhas — 0 erros** ✅ (após todos os fixes)

---

## Sessão 21 — Auditoria visual por screenshots + fix dos empty-states (2026-06-08)

### Contexto

O utilizador entregou **42 screenshots** de toda a app (pasta `printsProjetoTodo`)
e pediu foco **exclusivo no Desktop**: "que fique bom, tudo a funcionar, extremamente
bonito e fácil de usar". Análise visual página a página de todos os módulos.

### Veredicto da auditoria visual

A app está **visualmente excelente e funcionalmente completa**. Sidebar Indigo,
banners por módulo, calendários, tabelas premium, diálogos modais, stepper de fluxo,
edição de turno com z-order correto, auditoria a carregar — tudo confirmado nos prints.
Zero TODOs/handlers vazios nos controllers desktop; todos os placeholders em PT correto.

### 🐞 Bug encontrado e corrigido — empty-states sobrepostos a conteúdo

**Sintoma (visível em 3 prints do módulo Horários):** os cartões de "vazio"
apareciam **por cima da tabela/calendário já preenchidos**, dando sensação de ecrã
partido precisamente no ecrã mais importante da app:
- Tab Propostas → "Sem alternativas geradas" sobre a tabela de alternativas cheia
- Tab Propostas → "Sem dados de distribuição" sobre a distribuição já preenchida
- Tab Calendário → "Sem semana para mostrar" sobre o calendário semanal preenchido

**Causa raiz:** os `VBox` `emptyStatePropostas`, `emptyStateDistribuicao` e
`emptyStateCalendario` estavam declarados no FXML mas **nunca eram alternados** pelo
controller (só `emptyStateCalendarioMensal` o era). Ficavam permanentemente
`visible=true`. Mesma classe de bug em `gestao-loja-view.fxml` (`emptyStateExcecoes`
nem sequer tinha campo `@FXML` no controller).

**Fix:**
- `GeracaoHorariosController` — novos `atualizarEmptyStates()` + `alternarEmptyState()`
  invocados em `atualizarEstadoInterativo()` (ponto central de todos os fluxos:
  `preencher/limparResultado`, `aplicarListaPropostas`). Esconde o empty-state e mostra
  o conteúdo quando há dados; o contrário quando não há (evita o duplo "vazio" com o
  `setPlaceholder` da própria TableView).
- `GestaoLojaController` — novo campo `@FXML VBox emptyStateExcecoes` alternado em
  `preencherHorariosEspeciais()`.

**Ficheiros alterados:**
- `Controller/GeracaoHorariosController.java`
- `Controller/GestaoLojaController.java`

### Estado dos Testes

**51/51 testes — 0 falhas — 0 erros** ✅ + BUILD SUCCESS (compile)

---

## Sessão 22 — Banner de topo uniforme em todos os módulos (2026-06-08)

### Pedido

O utilizador quis que **todas as páginas** usem o mesmo estilo de banner do
"Painel de Gestão de Pedidos" — o **card flutuante vermelho-escuro**
(`#9b1c1c → #450a0a`, raio 16px) — substituindo as cores por módulo
(verde na Loja, azul/navy nos Funcionários, âmbar nas Folgas, etc.).

### Implementação

Antes existiam dois tipos de banner (Sessão 17/18):
- **Full-bleed** (edge-to-edge, raio 0): Loja, Funcionários, Folgas, Permutas, Horários
- **Card flutuante** (raio 16px, inset por padding do pai): Pedidos, Relatórios, Auditoria

Uniformização para o estilo card vermelho-escuro:

| Módulo | Antes | Ação |
|--------|-------|------|
| Pedidos | dark-red card | referência (inalterado) |
| Relatórios | navy card | recolor → dark-red |
| Auditoria | cinza card | recolor → dark-red |
| Loja | verde full-bleed | → card dark-red (+`modulo-banner-card`, raio 16px, `VBox.margin` 40/40/24/40, padding 26/30) |
| Funcionários | navy full-bleed | → card dark-red (idem) |
| Folgas | âmbar full-bleed | → card dark-red (idem) |
| Permutas | azul full-bleed | → card dark-red (idem) |
| Horários | vermelho full-bleed | → card dark-red (idem) |
| Perfil | hero vermelho `#c91428` | recolor → dark-red (mantém hero+avatar) |

Os full-bleed passaram a card adicionando `modulo-banner-card` ao styleClass,
`-fx-background-radius: 16px` no inline style e `<VBox.margin>` para flutuar
(o pai `conteudo-pagina` não tinha padding). Padding interno alinhado a 26/30
para igualar a altura da referência.

**Apenas inline styles + styleClass + VBox.margin/padding.** Zero `fx:id`/handlers
alterados. XML bem-formado validado nos 8 FXML via `[xml]` parse.

### Estado dos Testes

**51/51 testes — 0 falhas — 0 erros** ✅ + BUILD SUCCESS

---

## Sessão 23 — Refactor profundo do algoritmo de geração (2026-06-10)

### Pedido
Focar no **código todo do algoritmo**: ver o que está bem/mal/otimizável, **separar
algoritmo de carregamento de dados**, tornar o algoritmo **o mais inteligente possível**
(gerar várias opções e identificar ele próprio a mais ótima; preferências só contam
quando aprovadas pelo gerente) e **reduzir o número de linhas** subdividindo em mais ficheiros.

### Diagnóstico (verificado com greps, não especulação)

🔴 **Algoritmo:**
1. **As 5 políticas de otimização eram uma fachada** — `pesoPreferencias`, `pesoEquilibrioCarga`,
   etc. **nunca eram lidos**. O `pontuarAtribuicao` usava pesos hardcoded. Alternativas só
   diferiam no nome + semente aleatória.
2. **Scoring recalculado dentro do comparador do `sort`** (2×/comparação) e cada chamada fazia
   `horariosJaGerados.stream().anyMatch` O(H) → ~O(n·log n·H) por slot/dia.
3. **Diagnóstico de falha sempre vazio** (`lancarFalhaCobertura` passava `List.of()`); o painel
   `DiagnosticoGeracaoPanel` existia mas recebia listas vazias.
4. **`ReservaFimDeSemana` e `ContextoPreservacaoFDS`**: construídos e threaded por todo o motor
   mas **conteúdo nunca lido** — só a nulidade de um servia de gate. A flag `aplicarPreservacaoFDS`
   não tinha efeito → "tentativa 3" era repetição idêntica da 2.
5. `diasFolgaPreferidos` sempre vazio; folga preferida = hard block (igual a ausência).

🔴 **Carregamento / código morto:**
6. `HorarioValidatorService`: 8 métodos `ehRegra*` (~58 L) duplicados/mortos (o `RegraGeracaoResolver`
   tem cópias próprias).
7. `GeracaoHorariosService`: `preferenciaAtivaNaData`, `calcularHorasDescanso`, `inicioFimDeSemana`,
   `ehFimDeSemana`, `normalizarTurno`, record `GargaloCobertura` — mortos. `calcularDuracaoEmMinutos`
   triplicado.

### Alterações implementadas (52→53 testes, todos ✅ BUILD SUCCESS em cada fronteira)

**Fase 1 — Limpeza de código morto**
- Removidos os 8 `ehRegra*` + `normalizarTextoComposto` do validator (320→252 L).
- Removidos os 6 métodos/record mortos do service; `calcularDuracaoEmMinutos` unificado em `HorarioFormatters`.

**Fase 2 — Subdivisão (motor 1132→636 L)**
- Novos ficheiros em `geracao/`: `PedidoGeracao`, `ConfiguracaoDia` (value objects do motor, top-level),
  `EstadoColaborador` (estado de domínio, 232 L, extraído de classe interna), `TurnoClassifier`
  (classificação por período, partilhada), `AvaliadorAtribuicao` (função de pontuação, 206 L),
  `FalhaGeracaoHorarioException` + `MotivoFalhaGeracao` + `SugestaoFalhaGeracao` (extraídos da service),
  `diagnostico/DiagnosticoCobertura` (148 L).
- Motor reescrito: removidas as **duas máquinas mortas** (ReservaFimDeSemana + ContextoPreservacaoFDS)
  e a tentativa redundante → planeamento com 4 tentativas reais (base → alargada → rotação FDS relaxada
  → descanso semanal relaxado).
- Comparação de propostas extraída para `ComparacaoPropostasService` (143 L); service 1232→1042 L.

**Fase 3 — Inteligência**
- **Políticas reais**: `PedidoGeracao` carrega a `PoliticaOtimizacao` (com pesos). `AvaliadorAtribuicao`
  multiplica cada componente (carga, FDS, reserva, preferências, consistência, diversificação) pelo peso
  da política → "Preferências" maximiza prefs, "Carga contratual" equilibra horas, "Fins de semana"
  reforça rotação, etc. Alternativas genuinamente diferentes em estratégia.
- **Performance**: score calculado **uma vez** por candidato (decorate-sort) + contexto pré-computado
  (`colaboradoresNoMes`) → fim do O(n·log n·H).
- **Best-pick global**: `listarPropostas` marca automaticamente a alternativa de menor pontuação
  (entre rascunho/pendente) como `recomendada` — o algoritmo identifica ele próprio a mais ótima.
  Surfaçado na tabela de propostas do desktop com "★".
- **Diagnóstico de falha real**: `DiagnosticoCobertura` analisa, por colaborador, o motivo de exclusão
  (carga esgotada, descanso, bloqueio, part-time em dia útil, turno curto, rotação FDS, …) e gera
  sugestões acionáveis. O painel desktop passa a ter conteúdo.

### Ficheiros do subsistema (depois)
Motor 636 · Service 1042 · Validator 252 · Comparação 143 · `geracao/` 19 ficheiros coesos
(AvaliadorAtribuicao 206, EstadoColaborador 232, RegraGeracaoResolver 261, DiagnosticoCobertura 148, …).

### Pendente / decisão do utilizador
- **Preferência de folga soft (1/semana + explicação)**: descoberto que o código trata
  **"folgas" aprovada como ausência concedida (hard block) — por design e testado em 3 testes**
  (`FluxosCriticosIntegrationTest`, `DescansoSemanalValidationTest`, `PreferenciasPermanentesValidationTest`;
  a variável chama-se `bloqueadoPorPreferencia`). A infraestrutura soft está pronta
  (`folgasPreferidasPorColaborador` + penalização +60 no avaliador + códigos de diagnóstico), mas
  ligar `construirDiasFolgaPreferidos` exige decidir: (a) novo subtipo de preferência "folga recorrente"
  soft, mantendo a folga-aprovada hard; ou (b) virar folga-aprovada para soft e reescrever os 3 testes.
- **Extração dos DTOs** de `GeracaoHorariosService` (~220 L de records) para `geracao/dto/`: adiável —
  churn em 22 ficheiros (8 testes) para relocar data records.

### Estado dos Testes
**53/53 testes — 0 falhas — 0 erros** ✅ + BUILD SUCCESS

---

## Sessão 23 (cont.) — Folga preferida SOFT implementada (novo subtipo)

Decisão do utilizador: **novo subtipo soft**, mantendo a folga-aprovada-com-datas como
ausência concedida (hard, não parte os 3 testes existentes).

**Implementado end-to-end:**
- Novo tipo de preferência **`folga_preferida`** = folga recorrente semanal **soft** (1/semana):
  o algoritmo dá-lhe muita atenção (penalização base +60 + reforço por `pesoPreferencias` no
  `AvaliadorAtribuicao`), mas **não bloqueia** — pode escalar se a cobertura exigir.
- **Explicação ("indicando o porquê")**: quando uma folga preferida é quebrada, é gerada uma nota
  anexada ao `resumoGeracao` da proposta ("Folga preferida de X em DD/MM nao foi honrada...").
  Novo campo `avisos` em `PlaneamentoGerado`.
- `PreferenciasGeracaoBuilder.construirDiasFolgaPreferidos` deixa de devolver vazio: extrai o dia da
  semana da `dataInicio` e marca-o em cada semana (uma folga preferida/semana).
- `PreferenciaService.TIPOS_VALIDOS` + prioridade/validação aceitam o novo tipo.
- **SQL**: `sql/issue-16-preferencias.sql` — constraint `chk_preferencias_tipo` agora drop+recreate
  incluindo `folga_preferida` (aplicada também ao DB de teste).
- **UI desktop** (`PreferenciasController`): dropdown + mapeamentos + "sem data fim" + prompt explicativo.
- Novo teste `folgaPreferidaSoftNaoBloqueiaOColaboradorAoContrarioDaFolgaAprovada`.

**Falta (opcional):** oferecer `folga_preferida` também no formulário **web** de complementares.

### Estado dos Testes
**54/54 testes — 0 falhas — 0 erros** ✅ + BUILD SUCCESS

---

## Sessão 23 (cont. 2) — Web folga soft + Extração dos DTOs

**Folga preferida no Web:** `WebComplementaresController` oferece agora `folga_preferida` no
dropdown de tipos; o template `complementares.html` prettifica os labels
(`#strings.capitalize(#strings.replace(tipo,'_',' '))`). Feature soft completa em desktop **e** web.

**Extração dos DTOs (subdivisão estrutural pedida):** os 10 records públicos de
`GeracaoHorariosService` movidos para o novo pacote **`geracao/dto/`**
(`PropostaResultado`, `HorarioLinha`, `ColaboradorElegivel`, `ResumoColaborador`, `ResumoGeral`,
`PropostaResumo`, `ComparacaoPropostas`, `DiferencaColaborador`, `GeracaoContexto`,
`ConfiguracaoDiaEspecial`). Atualizados 22 ficheiros (13 main + 8 testes + a service) — referências
`GeracaoHorariosService.X` → `X` + import do pacote dto; imports órfãos removidos.

`GeracaoHorariosService`: **1232 → 945 linhas** ao longo da sessão (lógica extraída + DTOs movidos +
código morto removido). O `DadosGeracao` (record interno privado) permanece nested.

### Estado dos Testes
**54/54 testes — 0 falhas — 0 erros** ✅ + BUILD SUCCESS

---

## Sessão 24 — UX da app desktop ao detalhe + motor de geração mais inteligente

### A. Grelha "colaboradores × dias" com coluna congelada (Home + Geração)

**Problema:** na vista mensal da grelha, ao arrastar para o lado os nomes dos
colaboradores desapareciam — perdia-se o contexto de quem era cada linha.

**Solução:** novo componente único **`GrelhaHorarioRenderer`** (DESKTOP/support):
- Coluna de colaboradores **fixa à esquerda**; só os dias deslizam num `ScrollPane`
  horizontal próprio (`grelha-dias-scroll`), com alturas de linha fixadas (56/72px)
  para alinhamento perfeito entre as duas metades.
- Hover sincronizado (classe `grelha-row-hover` aplicada às duas metades da linha).
- Células e cabeçalhos de dia **clicáveis** (callback `aoAbrirDia`) → abre "Detalhe do dia".
- Cor de avatar estável por colaborador (derivada do id).

`GrelhaHorarioHelper` (Home, entidades `Horario`) e `VistaGrelhaHorarioRender`
(Geração, DTOs `HorarioLinha`) passaram a **adaptadores finos** do renderer único —
eliminada a duplicação ~90% entre os dois. FXML: `scrollGrelhaEquipaMensal` e
`grelhaScrollPane` passam a `hbarPolicy=NEVER` + `fitToWidth` (o scroll horizontal
vive dentro da grelha; a página gere o vertical).

### B. Calendário mensal da Home: dias clicáveis com detalhe

**Problema:** na página principal o calendário mensal não deixava clicar nos dias
(na página de Horários dava).

**Solução:** `HomeController.renderizarCalendarioMensalLoja` passa agora o callback
`aoSelecionarDia` ao `CalendarioMensalHelper` e guarda os horários do mês carregado;
novo **`DetalheDiaDialog.abrirHorariosPublicados(data, List<Horario>, owner)`** mostra
os turnos publicados do dia (período, colaborador, cargo · estado). O mesmo detalhe
abre a partir das células da grelha (Home e Geração). Formato dos eventos da Home
normalizado para `periodo | nome (cargo)` (igual à Geração).

### C. Menos "caixas brancas" — calendários mais limpos

- Dias **sem eventos** no calendário mensal recuam: classe
  `calendario-mes-dia-card-sem-eventos` (fundo soft, sem sombra, número esbatido) e
  removido o texto itálico repetido por dia ("Sem horário…"); min-height 118→104.
- Calendário semanal: dias vazios recuam (`calendario-dia-card-vazio`) e o dia de
  hoje ganha contorno (`calendario-dia-card-hoje`).
- `CalendarioMensalHelper`: removido código morto (criarCardEvento, abreviarCargo,
  classificarBlocoHorario, normalizarTextoPesquisa).
- Novo bloco CSS-20 em `dashboard.css` (grelha congelada + calendários limpos).

### D. Motor de geração: fase de refinamento por pesquisa local

**Problema:** a construção dia-a-dia (greedy+backtracking) é míope — folgas
preferidas não honradas e carga desequilibrada que uma troca simples resolveria.

**Solução:** novo **`geracao/RefinadorPlaneamento`**, chamado no fim de
`HorarioGeneratorEngine.gerar`:
1. **Honrar folgas preferidas:** reatribui turnos caídos em dia de folga preferida
   a outro colaborador disponível (que não prefira folgar nesse dia).
2. **Equilíbrio de carga:** enquanto o gap de utilização entre extremos > 10%,
   move turnos do mais carregado para os menos carregados (máx. 60 movimentos).

Cada movimento é 1-para-1 (mesmo dia + mesmo turno ⇒ cobertura por slot intacta),
validado por **replay estrito** da agenda completa do recetor via
`EstadoColaborador.podeReceber` (todas as regras hard), preservando a chefia mínima
de fim de semana; respeita o `prazoLimite` e nunca quebra a geração (fallback ao
plano original). Os avisos "folga preferida não honrada" no `resumoGeracao`
melhoram automaticamente (são calculados sobre o plano final).

Novo teste unitário puro `RefinadorPlaneamentoTest` (3 testes, sem Spring).

### Estado dos Testes
**57/57 testes — 0 falhas — 0 erros** ✅ + BUILD SUCCESS
