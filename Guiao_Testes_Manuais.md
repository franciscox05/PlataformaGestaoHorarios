# Guião de Testes Manuais (UAT) — PlataformaGestaoHorarios

Documento de teste de aceitação manual, para executar por Tiago e Francisco
nas interfaces Web e Desktop reais. Construído com base no mapeamento de
código e nas descobertas registadas em `Revisao.md` (cargos, RBAC, bugs de
concorrência multi-loja e o bug do gerente multi-loja no Desktop). Cada passo
abaixo foi verificado contra o código-fonte atual (classes CSS reais, métodos
reais, ficheiros FXML reais) — não é um guião genérico.

**Pré-requisitos:**
- Servidor Web a correr (`java -jar target\Projeto2-0.0.1-SNAPSHOT-web.jar --server.port=8081`) e/ou app Desktop a correr (`AppLauncher`).
- Acesso direto ao PostgreSQL (`gestaohorarios`) ou ao ecrã de Gestão de Funcionários para criar as contas de teste necessárias.
- Para os Cenários 1.1 e 1.2: contas de teste dedicadas (não reutilizar as contas de demonstração, para não poluir os dados da apresentação).

---

## 1. VALIDAÇÃO DOS BUGS DETETADOS (Provas de Fogo na UI)

### 1.1 — Cenário Desktop: crash real `IncorrectResultSizeDataAccessException` no Perfil de um utilizador multi-loja (Revisao.md, ponto 16)

**O que este bug é, em termos simples:** o Desktop deixa qualquer utilizador
entrar no Dashboard **sem nunca perguntar "em que loja estás a trabalhar
hoje?"** — ao contrário da Web, que força essa escolha logo depois do login
sempre que há mais de um vínculo ativo. Para um utilizador com vínculo ativo a
**duas lojas em simultâneo**, isto faz o Hibernate **rebentar com uma
excepção não tratada** assim que se abre o ecrã de Perfil, porque o serviço
tenta adivinhar uma única loja onde não existe nenhuma.

**Caso oficial de reprodução, confirmado em runtime real e validado por
diagnóstico direto à base de dados (usar este caso na defesa):**

```
Conta:  francisco.gomes@levis.com   (Francisco Gomes, id_utilizador=1)
Cargo:  gerente em AMBAS as lojas
Vínculo 1: id_lojautilizador=1  → Loja "Levi's Braga Parque"   dataInicio=2025-03-18  dataFim=NULL
Vínculo 2: id_lojautilizador=11 → Loja "Levi's NorteShopping"  dataInicio=2026-06-15  dataFim=NULL
```

Confirmámos por query direta à tabela `Lojautilizador` que esta é, em toda a
base de dados de demonstração, **a única conta com dois vínculos ativos em
simultâneo** — por isso o bug nunca apareceu a testar com colaboradores
comuns (todos têm exatamente 1 vínculo ativo) e só se manifesta com esta
conta de gerente específica. Não é falta de filtro por cargo no
`PerfilService` (não filtra por cargo) — é puramente um facto dos dados: só
o Francisco acumula duas lojas, porque é o cenário de "gerente regional" da
demo.

**Passo a passo para reproduzir:**

1. Usar a conta **`francisco.gomes@levis.com`** (password `123456`, ver
   `CLAUDE.md`) — não é preciso criar nenhum utilizador de teste novo, esta
   conta de demo já reproduz o bug tal como está.
2. Fazer login no **Desktop** com essa conta.
3. **Comportamento atual (bug):** o login é aceite e o Dashboard abre
   **diretamente, sem nenhum ecrã intermédio de seleção de loja** — a app
   nunca avisa que há ambiguidade.
4. Navegar para o ecrã de **Perfil** (sidebar → "Perfil").
5. **A aplicação crasha / mostra um erro não tratado.** No log da aplicação
   (consola ou ficheiro de log do Desktop), aparece exatamente:
   ```
   IncorrectResultSizeDataAccessException: Query did not return a unique
   result: 2 results were returned
   ```
   lançada por `PerfilService.obterResumoPerfil` ao chamar
   `findLigacaoAtivaByIdUtilizador` — o método tentou devolver "a" ligação
   ativa do utilizador, mas encontrou duas, e o Hibernate recusa-se a
   adivinhar qual.

**Comportamento ESPERADO depois do refactoring proposto (Secção 2 do plano
técnico associado, ver `Revisao.md` ponto 16):**
- Imediatamente depois do login, se o utilizador tiver mais do que um
  vínculo ativo, deve aparecer um **ecrã intermédio "Seleciona a loja onde
  vais trabalhar hoje"**, com a lista das lojas a que tem vínculo, **antes**
  de qualquer outro ecrã abrir.
- Depois de escolher uma loja, essa escolha deve ficar **visivelmente fixa**
  (ex.: label na sidebar/topo, ao lado do nome do utilizador e do cargo)
  durante toda a navegação — não deve ser preciso voltar a escolher ao mudar
  de ecrã.
- O ecrã de **Perfil** deve abrir **sem nenhum erro**, mostrando os dados
  relativos à loja escolhida (cargo, horários, etc. dessa loja
  especificamente).
- Repetir a navegação para outros ecrãs que dependam de uma única loja
  (ex.: Painel do Gerente, Gestão de Loja) e confirmar que nenhum deles
  lança excepções de ambiguidade.

**O que fazer se o comportamento for diferente do esperado:** se ao testar
com `francisco.gomes@levis.com` o Perfil não crashar, confirmar diretamente
na tabela `Lojautilizador` que os dois vínculos (id_lojautilizador 1 e 11)
continuam ambos com `data_fim IS NULL` — se algum tiver sido encerrado
entretanto (ex.: por um teste de Gestão de Funcionários), o bug deixa de
reproduzir com esta conta, e é preciso outro utilizador com 2 vínculos ativos
em simultâneo (confirmar com a query: `SELECT id_utilizador, count(*) FROM
lojautilizador WHERE data_fim IS NULL GROUP BY id_utilizador HAVING
count(*) > 1;`). Registar o resultado exato, incluindo o stack trace completo
se a aplicação crashar de forma diferente da descrita, na Matriz de
Resultados (secção 4).

---

### 1.2 — Cenário Desktop: o caso do Gerente Multi-Loja (Revisao.md, ponto 14.2)

**O que este bug é, em termos simples:** um gerente com acesso a duas lojas
pode ser **incorretamente rejeitado** ao tentar aprovar, pelo Desktop, um
pedido de folga de um colaborador da loja a que está ligado em "segundo
lugar" — porque o ecrã de aprovações do Desktop (`PainelGerentePedidosController`)
nunca pergunta "estás a agir como gerente de qual loja?", ao contrário da Web.

**Passo a passo para reproduzir:**

1. Abrir o ecrã de **Gestão de Funcionários** (Desktop) ou recorrer
   diretamente à base de dados, e criar/confirmar:
   - Um utilizador **"Gerente Teste Multi"** com vínculo ativo de cargo
     `gerente` a **DUAS lojas diferentes** (ex.: "Levi's Braga Parque" e
     "Levi's Guimarães"). É importante que as duas lojas existam e que o
     vínculo a ambas esteja ativo (sem data de fim) ao mesmo tempo.
   - Um colaborador **"Func Teste Guimarães"** com vínculo ativo apenas à
     **segunda** loja (Guimarães, ou a que tiver o nome alfabeticamente
     posterior — é essa que tende a não ser escolhida automaticamente).
2. Como o colaborador "Func Teste Guimarães" (pode ser via Web, é mais rápido):
   submeter um pedido de folga para uma data com pelo menos 24h de
   antecedência, na sua loja (Guimarães).
3. Fazer login no **Desktop** como **"Gerente Teste Multi"**.
4. Navegar para **Painel do Gerente → Pedidos de Folga** (sidebar →
   "Pedidos", separador "Folgas").
5. Localizar o pedido de "Func Teste Guimarães" na tabela de pendentes.
6. Selecionar a linha e clicar **"Aprovar"**.

**Comportamento visual ESPERADO (se o bug estiver presente, tal como
documentado):**
- A aprovação **falha**. Aparece uma mensagem de erro **inline, a vermelho**,
  junto à tabela de folgas (label de feedback do painel — fica visível e
  **não desaparece automaticamente**, ao contrário das mensagens de sucesso
  que se apagam sozinhas depois de alguns segundos).
- O texto da mensagem deve ser equivalente a **"Não tens permissão para gerir
  este pedido."** — apesar do utilizador ser claramente um gerente válido.
- A linha do pedido **permanece na tabela como pendente** — não desaparece,
  não fica marcada como aprovada.
- A aplicação **não deve fechar nem mostrar um ecrã de erro genérico/crash**
  — é um erro de negócio tratado, não uma excepção não tratada.

**O que fazer se o comportamento for diferente do esperado:** se a aprovação
**funcionar** (o pedido desaparece da lista de pendentes e fica "aprovado"),
o bug pode já não existir, ou a ordem das ligações do gerente neste ambiente
de teste calhou a favorecer a loja certa. Repetir o teste invertendo qual loja
é criada primeiro (ou usar nomes de loja deliberadamente ordenados — ex.: uma
loja com nome a começar por "A" e outra por "Z" — para forçar a ligação
"errada" a ser escolhida primeiro). Registar o resultado exato na Matriz de
Resultados (secção 4), incluindo os nomes exatos das duas lojas usadas.

---

### 1.3 — Cenário Web/Desktop: visibilidade de menus por cargo (Revisao.md, ponto 14.1)

**Contexto importante a clarificar antes do teste:** ao reler o código,
confirmei que o botão "Gestão de Loja" da sidebar do Desktop **já tem** uma
verificação de cargo correta — `DashboardController` esconde
(`setVisible(false)` + `setManaged(false)`, ou seja, nem ocupa espaço no
layout) os botões **"Gestão de Loja"**, **"Relatórios"**, **"Gestão de
Funcionários"** e o separador **"Painel do Gerente"** sempre que
`gestaoLojaBLL.utilizadorPodeGerirLoja(...)` devolve `false`. O ponto 14.1 do
`Revisao.md` refere-se especificamente a uma classe diferente
(`DashboardNavigator`) que não tem essa verificação embutida — não significa
que a sidebar esteja, hoje, sem proteção visual. Este teste serve para
**confirmar isso na prática** e, em simultâneo, validar o comportamento de
recurso (defesa em profundidade) caso o botão apareça por engano numa
regressão futura.

**Passo a passo:**

1. Criar (ou usar) um colaborador com cargo `fulltime` (ou `parttime`) — sem
   `gerente`, `subgerente` nem `supervisor` em nenhuma loja.
2. **No Desktop:** fazer login com essa conta.
   - **Resultado esperado (correto):** os botões "Gestão de Loja",
     "Relatórios" e "Gestão de Funcionários" **NÃO aparecem** na sidebar —
     nem como botão desativado (cinzento), simplesmente não ocupam espaço
     nenhum. A secção "Gestão" do menu também não deve aparecer se nenhum dos
     itens dela for visível.
   - **Se, ainda assim, o botão "Gestão de Loja" aparecer** (regressão): clicar
     nele. O ecrã de configuração da loja deve abrir normalmente (o ecrã em
     si não verifica cargo ao carregar), mas ao clicar em **"Guardar"**, deve
     aparecer uma mensagem de erro inline junto ao formulário (não um crash),
     com texto equivalente a "Não tens permissão para gerir a configuração da
     loja." — confirma que a camada de serviço continua a bloquear mesmo que
     a UI falhe em escondê-lo.
3. **Na Web:** fazer login com a mesma conta.
   - **Resultado esperado:** o link "Gestao Loja" na barra superior não
     aparece (controlado por `th:if="${podeGerirLoja}"` no template). Se
     o utilizador tentar aceder diretamente pelo URL `/web/gestao-loja`,
     deve ser redirecionado para `/web/painel?acessoNegado=true` — confirmar
     que aparece alguma indicação visual de acesso negado nessa página de
     destino (e não apenas um redireccionamento silencioso confuso).

---

## 2. TESTES DE NAVEGAÇÃO E BINDINGS FX (Desktop JavaFX)

### 2.1 — Carregamento de ecrãs (FXMLLoader)

Para cada um dos seguintes ecrãs, abrir a partir da sidebar e confirmar que
**carrega sem excepção, sem ecrã em branco, e sem nenhum diálogo de erro
inesperado a aparecer sozinho**:

| Ecrã | Botão na sidebar |
|---|---|
| Visão geral / Home | "Dashboard" |
| Pedir Folga / Preferências / Permutas | "Pedidos" (separador correspondente) |
| Horários | "Horários" |
| Painel do Gerente (Pedidos pendentes) | "Pedidos" (visível só para gerência) |
| Gestão de Loja | "Gestão de Loja" (visível só para gerência) |
| Gestão de Funcionários | "Gestão de Funcionários" (visível só para gerência) |
| Relatórios de Horas | "Relatórios" |
| Perfil | "Perfil" |

Para cada ecrã, verificar adicionalmente:
- Os campos de texto, tabelas e combos aparecem com o conteúdo já carregado
  (não vazios por um erro silencioso de binding) na primeira abertura.
- Trocar de ecrã e voltar ao mesmo ecrã não duplica linhas nem botões na
  tabela.

### 2.2 — Seleção de linha → contexto correto no popup/ação de aprovação

No **Painel do Gerente → Pedidos de Folga / Permutas / Preferências**:

1. Com a tabela de pendentes a mostrar **pelo menos 2 pedidos de
   colaboradores diferentes**, clicar na **primeira** linha.
2. Confirmar que o painel de contexto (nome do colaborador, data, motivo)
   muda para refletir exatamente os dados dessa linha.
3. Clicar na **segunda** linha (sem clicar em nenhum botão de ação entretanto).
4. Confirmar que o contexto atualiza para a segunda linha — **e que, se
   clicar "Aprovar" agora, o pedido afetado é o da segunda linha, não o da
   primeira** (este é o teste crítico: confirma que o ID passado ao
   `aprovarFolga`/`aprovarPermuta` corresponde sempre à seleção atual da
   tabela, não a uma seleção anterior "presa" em memória).
5. Repetir o mesmo teste no separador de **Permutas** e de **Preferências**.

### 2.3 — Botões desativados sem seleção

1. Abrir o Painel do Gerente sem clicar em nenhuma linha da tabela.
2. Confirmar que os botões **"Aprovar"** e **"Rejeitar"** aparecem
   **desativados (cinzentos, não clicáveis)** até uma linha ser selecionada.
3. Selecionar uma linha, confirmar que os botões ficam ativos.
4. Aprovar ou rejeitar esse pedido (ele deve desaparecer da tabela de
   pendentes). Confirmar que os botões voltam a ficar desativados
   automaticamente, já que a seleção deixou de ter um item válido.

### 2.4 — Formulário de Gestão de Loja: validação de campos

No ecrã **Gestão de Loja** (como gerente):

1. **Campo de horas vazio**: limpar o campo de hora de abertura ou de fecho
   (se for um `ComboBox`/`TextField` editável) e clicar "Guardar".
   - **Esperado:** o diálogo de confirmação "Deseja guardar as regras da
     loja?" pode ainda aparecer (a confirmação acontece antes da validação),
     mas depois de confirmar, deve aparecer uma mensagem de erro inline
     (não crash) — algo como "As horas de abertura e fecho são obrigatórias."
2. **Hora de abertura igual à de fecho**: preencher ambos os campos com o
   mesmo valor (ex.: 09:00 e 09:00) e guardar.
   - **Esperado:** mensagem de erro "A hora de abertura e a hora de fecho não
     podem ser iguais." — sem nenhuma alteração visível na loja depois disto.
3. **Formato de hora inválido** (se o campo permitir texto livre em vez de um
   seletor fixo): introduzir algo como `25:99` ou `abc`.
   - **Esperado:** ou o campo rejeita a entrada no próprio `ComboBox`/picker
     (mais provável, já que os campos de hora deste ecrã tendem a ser
     `ComboBox` com valores pré-definidos, não texto livre), ou, se for
     possível introduzir um valor inválido, a aplicação não deve rebentar —
     deve mostrar erro controlado.
4. **Confirmar cancelamento**: repetir o passo 1, mas no diálogo de
   confirmação "Deseja guardar as regras da loja?" clicar **"Cancelar"** (ou
   equivalente) — confirmar que nada é alterado e nenhuma mensagem de erro
   ou sucesso aparece.

---

## 3. TESTES DE FLUXO COMPLETO WEB (Thymeleaf/AJAX)

### 3.1 — Ecrã de Complementares: submissão de preferência com persistência

1. Login na Web como um colaborador comum.
2. Navegar para **Complementares** → separador **Preferências**.
3. Preencher o formulário (tipo de preferência, descrição com pelo menos 5
   caracteres) e submeter.
4. **Resultado esperado imediatamente após submeter:** a página recarrega
   (é um POST tradicional com redirect, não AJAX) e deve aparecer um
   **toast verde** no canto da página, com fundo claro esverdeado e borda
   verde (classe `toast-success` — cor de fundo `#d1fae5`, texto `#065f46`),
   com a mensagem de confirmação.
5. **Premir F5 / atualizar a página manualmente.**
   - **Resultado esperado:** o toast de sucesso desaparece (era uma mensagem
     de uma única vez, não fica preso no ecrã), mas a preferência submetida
     **continua visível na lista de preferências pendentes** — os dados não
     podem desaparecer só por recarregar a página.
6. Repetir o teste forçando um erro deliberado (ex.: descrição com menos de
   5 caracteres, ou nenhum tipo selecionado).
   - **Resultado esperado:** toast **vermelho** (classe `toast-error`, fundo
     `#fee2e2`, texto `#991b1b`) com a mensagem de validação exata devolvida
     pelo serviço (ex.: "A descrição deve ter pelo menos 5 caracteres.").

### 3.2 — Ecrã de Complementares: pedido de folga

1. No mesmo ecrã, separador **Folgas**, submeter um pedido de folga para uma
   data válida (>24h de antecedência, dentro de um mês já com horário
   publicado para este colaborador).
2. Confirmar o toast de sucesso (mesma estética do ponto 3.1).
3. Atualizar a página — confirmar que o pedido aparece na lista "As minhas
   folgas", com estado "pendente".
4. Repetir submetendo para uma data **inválida** (ex.: hoje, ou um dia sem
   antecedência suficiente) — confirmar o toast vermelho com a mensagem de
   erro de antecedência mínima.

### 3.3 — Fluxo de Permuta entre dois colaboradores (A → B)

1. Login na Web como **Colaborador A**.
2. Ir a **Complementares** → separador **Permutas**.
3. Selecionar o seu próprio turno a ceder, depois selecionar o turno de um
   **Colaborador B** (mesma loja, mesmo dia) na lista de turnos elegíveis —
   confirmar que a lista de "colegas elegíveis" só mostra colegas da mesma
   loja.
4. Submeter o pedido de permuta.
   - **Resultado esperado para o Colaborador A:** toast verde de confirmação;
     o pedido aparece na lista "As minhas permutas enviadas" com estado
     "pendente".
5. Fazer logout e login como **Colaborador B** (ou abrir uma janela anónima
   diferente, para manter as duas sessões simultâneas se quiser testar em
   paralelo).
6. Como Colaborador B, ir ao **ícone de notificações** na barra superior
   (visível apenas a aprovadores/gerência — se B não tiver cargo de
   aprovação, este pedido não gera notificação a B, apenas ao gerente da
   loja: confirmar isto também, é parte do comportamento esperado).
7. Como **gerente da loja** (terceira conta), confirmar que o pedido de
   permuta aparece em **Equipa → Permutas pendentes**, com os nomes corretos
   de A (origem) e B (destino), e o turno trocado claramente identificado.
8. Aprovar o pedido como gerente.
   - **Resultado esperado:** o pedido desaparece da lista de pendentes; ao
     consultar os horários de A e B (ecrã de Horários), os turnos devem
     aparecer trocados.
9. **Voltar a verificar como Colaborador A**: o histórico de permutas deve
   mostrar o pedido como "aprovado".

---

## 4. MATRIZ DE RESULTADOS (Checklist)

Preencher a coluna **Status** com `Passou`, `Falhou` ou `Bloqueado` à medida
que cada teste é executado. Usar a coluna **Notas** para registar qualquer
desvio do esperado, incluindo capturas de ecrã se possível.

| ID | Módulo | Ação do Utilizador | Resultado Esperado | Status | Notas |
|----|--------|---------------------|---------------------|--------|-------|
| T1.1 | Desktop | Login com utilizador com vínculo ativo a 2 lojas, abrir Perfil | Sem ecrã de seleção: crash `IncorrectResultSizeDataAccessException` no log | | |
| T1.2 | Desktop | Gerente multi-loja aprova folga de colaborador da loja "secundária" | Erro inline "Não tens permissão...", pedido continua pendente | | |
| T1.3a | Desktop | Login como `fulltime`, observar sidebar | Botões de gestão ausentes (sem espaço reservado) | | |
| T1.3b | Desktop | (Se botão aparecer) Clicar "Gestão de Loja" → "Guardar" | Erro inline de permissão, sem crash | | |
| T1.3c | Web | Login como `fulltime`, observar navbar | Link "Gestao Loja" ausente | | |
| T1.3d | Web | Aceder diretamente a `/web/gestao-loja` por URL | Redireciona para `/web/painel?acessoNegado=true` com indicação visual | | |
| T2.1 | Desktop | Abrir cada ecrã da tabela da secção 2.1 | Carrega sem excepção nem ecrã em branco | | |
| T2.2 | Desktop | Selecionar linha 1, depois linha 2 na tabela de pendentes | Contexto e ação seguem sempre a seleção atual | | |
| T2.3 | Desktop | Painel do Gerente sem seleção | Botões "Aprovar"/"Rejeitar" desativados | | |
| T2.4a | Desktop | Guardar configuração de loja com campo de hora vazio | Erro inline de validação | | |
| T2.4b | Desktop | Guardar com hora abertura = hora fecho | Erro "não podem ser iguais" | | |
| T2.4c | Desktop | Cancelar no diálogo de confirmação | Nada é alterado | | |
| T3.1a | Web | Submeter preferência válida | Toast verde de sucesso | | |
| T3.1b | Web | Atualizar página (F5) após sucesso | Dados persistem, toast desaparece | | |
| T3.1c | Web | Submeter preferência inválida (descrição curta) | Toast vermelho com mensagem exata | | |
| T3.2a | Web | Submeter folga válida | Toast verde, pedido visível como pendente | | |
| T3.2b | Web | Submeter folga sem antecedência mínima | Toast vermelho de erro de antecedência | | |
| T3.3a | Web | Colaborador A submete permuta para B | Toast verde para A, pedido "pendente" | | |
| T3.3b | Web | Gerente vê pedido em Equipa → Permutas | Nomes de A/B e turnos corretos visíveis | | |
| T3.3c | Web | Gerente aprova a permuta | Turnos de A e B trocados nos respetivos horários | | |
| T3.3d | Web | Colaborador A consulta histórico de permutas | Pedido aparece como "aprovado" | | |
