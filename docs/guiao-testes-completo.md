# Guião de Testes Completo — PlataformaGestaoHorarios (Desktop)

> **Versão:** Junho 2026  
> **Scope:** Apenas o módulo desktop (JavaFX). O módulo web é da responsabilidade do Tiago.  
> **Objectivo:** Testar ABSOLUTAMENTE todas as funcionalidades do desktop — do login ao PDF — sem deixar nenhum caminho por verificar.

---

## Preparação

### 1. Reset da base de dados

```powershell
# Terminal → apaga tudo excepto os teus 2 utilizadores
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -d gestaohorarios -f .\sql\reset-para-testes.sql
```

Verificar: a query de verificação no final deve mostrar exactamente **2 utilizadores** (`francisco@levis.com` e `tiago.costa@levis.com`).

### 2. Arrancar a app desktop

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.mainClass=com.example.projeto2.AppLauncher"
```

---

## BLOCO 1 — Autenticação e Sessão

### T01 — Login com credenciais inválidas
1. Abre a app → ecrã de login.
2. Introduz email `abc@teste.com` + password `errada` → clica "Entrar".
3. **Esperado:** mensagem de erro visível ("Credenciais inválidas" ou similar); app NÃO avança.

### T02 — Login com campo em branco
1. Deixa o email em branco, password qualquer → clica "Entrar".
2. **Esperado:** mensagem de erro; sem crash.

### T03 — Login com conta existente (tu)
1. Email: `francisco@levis.com` | Password: `123456` → clica "Entrar".
2. **Esperado:** avança para o dashboard sem erros.
3. **Verificar:** nome/cargo visível na barra lateral; loja visível.

### T04 — Logout
1. Estando no dashboard → clica no botão de logout (ou menu).
2. **Esperado:** regressa ao ecrã de login; sessão limpa (tentar Back no teclado não deve re-entrar).

---

## BLOCO 2 — Setup da Loja (pré-requisito para todos os outros testes)

> **Atenção:** sem loja e cargos configurados, a maioria das funcionalidades não funciona. Fazer este bloco antes de qualquer outro.

### T05 — Criar cargos necessários via SQL (directo)

Como a gestão de cargos pode não ter UI de criação nativa, insere via psql:

```sql
INSERT INTO public.cargos (nome, tipo, descricao) VALUES
  ('Gerente de Loja',        'gerente',           'Responsável máximo'),
  ('Sub-Gerente',            'subgerente',        'Apoio à gerência'),
  ('Supervisor de Equipa',   'supervisor',        'Validação operacional'),
  ('Assistente FT',          'fulltime',          'Tempo inteiro'),
  ('Assistente PT',          'parttime',          'Tempo parcial'),
  ('Reforço FDS',            'reforco_parttime',  'Só fins de semana');
```

Verificar: `SELECT * FROM public.cargos;` → 6 registos.

### T06 — Criar loja via ecrã Gestão de Loja
1. No dashboard → navega para **Gestão de Loja**.
2. Clica "Nova Loja" (ou equivalente) → preenche:
   - Nome: `Levi's Braga Parque`
   - Localização: `Braga`
   - Hora abertura: `10:00` | Hora fecho: `23:00`
3. Guarda → **Esperado:** loja aparece na lista/selector.

> **Se a UI não tiver botão de criação de loja**, inserir via SQL:
> ```sql
> INSERT INTO public.lojas (nome, localizacao, hora_abertura, hora_fecho)
> VALUES ('Levi''s Braga Parque', 'Braga', '10:00', '23:00');
> ```

### T07 — Configurar turnos
1. Gestão de Loja → secção **Turnos**.
2. Criar 3 turnos:
   - Manhã: `10:00 – 14:00` (tipo: manha)
   - Tarde: `14:00 – 19:00` (tipo: tarde)
   - Noite: `19:00 – 23:00` (tipo: noite)
3. Gravar cada um → **Esperado:** os 3 aparecem na lista sem erros.

### T08 — Configurar regras da loja
1. Gestão de Loja → secção **Regras**.
2. **Verificar separação:** regras fixas por lei (descanso 11h) devem aparecer como bloqueadas (não editáveis) com o selo "FIXO POR LEI".
3. Editar regras operacionais:
   - Mínimo de funcionários por turno: **2**
   - Janela de rotação FDS: **2** semanas
   - Presença gerente ao sábado: **activa** (checkbox)
4. Gravar → **Esperado:** valores persistem ao reabrir a página.

### T09 — Verificar cargas contratuais nas regras
1. Na secção Regras → verificar que existem entradas para carga contratual de cada perfil:
   - Gerência: 176h/mês | Full-time: 176h | Part-time: 96h | Reforço: 64h
2. Editar um valor (ex: PT para 90h) → gravar → reabrir → **Esperado:** valor guardado.
3. Repor para 96h.

### T10 — Criar horário especial (dia encerrado)
1. Gestão de Loja → secção **Dias Especiais**.
2. Criar entrada:
   - Descrição: `Inventário`
   - Data: próximo mês +30 dias
   - Loja encerrada: **sim**
3. Gravar → **Esperado:** dia aparece na lista; deve aparecer no painel de geração como "dia encerrado".

---

## BLOCO 3 — Gestão de Funcionários

> **Pré-requisito:** loja e cargos criados (Bloco 2).

### T11 — Adicionar colaborador (tu próprio como gerente)
1. Dashboard → **Gestão de Funcionários** → "Adicionar Colaborador".
2. Selecciona utilizador: `francisco@levis.com` (Francisco - Tu).
3. Cargo: **Gerente de Loja** | Loja: Braga Parque | Data início: hoje.
4. Confirmar → **Esperado:** apareces na lista da equipa com cargo Gerente.

### T12 — Adicionar Tiago Costa como sub-gerente
1. Adicionar Colaborador → utilizador: `tiago.costa@levis.com`.
2. Cargo: **Sub-Gerente** | Loja: Braga Parque | Data início: hoje.
3. Confirmar → **Esperado:** Tiago na lista.

### T13 — Criar novos utilizadores via SQL (funcionários para os testes)
Como a UI pode não ter criação de utilizador (só associação loja-cargo), inserir via psql:

```sql
INSERT INTO public.utilizadores (nome, email, telemovel, password_hash, estado) VALUES
  ('Ana Silva',       'ana@levis.com',      '910000001', '123456', 'ativo'),
  ('Bruno Costa',     'bruno@levis.com',    '910000002', '123456', 'ativo'),
  ('Carla Mendes',    'carla@levis.com',    '910000003', '123456', 'ativo'),
  ('David Ferreira',  'david@levis.com',    '910000004', '123456', 'ativo'),
  ('Eva Rodrigues',   'eva@levis.com',      '910000005', '123456', 'ativo'),
  ('Fábio Santos',    'fabio@levis.com',    '910000006', '123456', 'ativo');
```

### T14 — Associar os 6 funcionários à loja
1. Para cada um: Gestão de Funcionários → Adicionar Colaborador.
2. Ana Silva → **Assistente FT** | Bruno Costa → **Assistente FT**
3. Carla Mendes → **Assistente PT** | David Ferreira → **Assistente PT**
4. Eva Rodrigues → **Reforço FDS** | Fábio Santos → **Supervisor**
5. **Esperado:** lista da equipa mostra 8 pessoas (2 gerência + 6 novos).

### T15 — Ver calendário de um funcionário (vista mensal/semanal)
1. Gestão de Funcionários → clica no nome de "Ana Silva".
2. **Esperado:** abre o painel/calendário individual desta pessoa.
3. Alternar entre vista **Semana** e **Mês** → **Esperado:** ambas carregam sem erros (de momento sem turnos).

### T16 — Inactivar um funcionário
1. Gestão de Funcionários → selecciona "Fábio Santos".
2. Define data de saída: hoje + 60 dias (ainda activo para os testes).
3. **Verificar:** Fábio ainda aparece na lista.
4. Repor sem data de saída (opcional, dependendo da necessidade dos testes seguintes).

---

## BLOCO 4 — Pedidos de Folga (perspectiva do funcionário)

> **Login como funcionário:** faz logout → entra como `ana@levis.com` | `123456`.

### T17 — Pedir folga (tipo: folga normal)
1. Dashboard → **Pedir Folga**.
2. Selecciona data: próximo mês + 5 dias.
3. Tipo: **folgas** | Motivo: "Consulta médica marcada".
4. Submete → **Esperado:** pedido aparece na lista com estado **pendente**; notificação enviada ao gerente.

### T18 — Pedir férias
1. Pedir Folga → data início: próximo mês + 10 | data fim: próximo mês + 12.
2. Tipo: **ferias** | Motivo: "Férias de verão".
3. Submete → **Esperado:** estado pendente.

### T19 — Pedir folga em data passada (validação)
1. Pedir Folga → data: ontem.
2. **Esperado:** erro de validação; pedido NÃO é submetido.

### T20 — Ver estado dos pedidos
1. Na vista de Pedir Folga → ver lista dos pedidos existentes.
2. **Verificar:** os pedidos submetidos em T17 e T18 estão visíveis com estado "pendente".

---

## BLOCO 5 — Aprovação de Pedidos (perspectiva do gerente)

> **Login como gerente:** logout → entra como `francisco@levis.com` | `123456`.

### T21 — Ver painel de pedidos pendentes
1. Dashboard → **Painel Gerente** (ou botão pedidos no dashboard).
2. **Esperado:** folgas pendentes de Ana Silva visíveis (T17 + T18); badge com número de pendentes.

### T22 — Aprovar folga (folga normal)
1. Painel → selecciona o pedido de folga de Ana Silva (T17).
2. Clica **Aprovar** → **Esperado:** estado muda para "aprovado"; badge actualiza; notificação enviada a Ana.

### T23 — Rejeitar férias com motivo obrigatório
1. Painel → selecciona o pedido de férias de Ana Silva (T18).
2. Clica **Rejeitar** → **Esperado:** aparece dialog a pedir motivo de rejeição.
3. Tenta confirmar sem escrever motivo → **Esperado:** não deixa avançar.
4. Escreve motivo: "Período conflituante com outro colega" → confirma.
5. **Esperado:** estado muda para "recusado"; notificação com motivo enviada a Ana.

---

## BLOCO 6 — Preferências (perspectiva do funcionário)

> Login como `bruno@levis.com` | `123456`.

### T24 — Submeter preferência de turno (manhã)
1. Dashboard → **Preferências**.
2. Tipo: **turnos** | Descrição: "Manhã" | Prioridade: 4.
3. Submete → **Esperado:** estado pendente.

### T25 — Submeter preferência de colega
1. Preferências → nova preferência.
2. Tipo: **colegas** | Descrição: "Ana Silva" | Prioridade: 2.
3. Submete → **Esperado:** estado pendente.

### T26 — Submeter folga preferida
1. Preferências → nova preferência.
2. Tipo: **folga_preferida** | Data início/fim: próximo mês + 5 (mesmo dia).
3. Submete → estado pendente.

### T27 — Aprovação de preferências pelo gerente
> Login como `francisco@levis.com`.

1. Painel Gerente → aba Preferências.
2. Aprovar a preferência de turno de Bruno → **Esperado:** aprovado; notificação.
3. Rejeitar a preferência de colega → **Esperado:** dialog de motivo → escreve motivo → rejeita.

---

## BLOCO 7 — Permutas de Turno

> **Pré-requisito:** ter turnos atribuídos. Como ainda não geramos horários, inserir turnos demo via SQL:

```sql
-- Obter IDs de lojautilizador de Ana e Bruno
-- Substitui os IDs correctos nos VALUES abaixo
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado)
SELECT lu.id_lojautilizador, 1, CURRENT_DATE + 7, 'aprovado'
FROM public.lojautilizador lu
JOIN public.utilizadores u ON u.id_utilizador = lu.id_utilizador
WHERE u.email = 'ana@levis.com';

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado)
SELECT lu.id_lojautilizador, 2, CURRENT_DATE + 7, 'aprovado'
FROM public.lojautilizador lu
JOIN public.utilizadores u ON u.id_utilizador = lu.id_utilizador
WHERE u.email = 'bruno@levis.com';
```

### T28 — Pedir permuta de turno
> Login como `ana@levis.com`.

1. Dashboard → **Permutas**.
2. Selecciona o teu turno do dia CURRENT_DATE + 7 (manhã).
3. Selecciona o turno de Bruno do mesmo dia (tarde).
4. Clica "Pedir Permuta" → **Esperado:** pedido criado com estado pendente; notificação ao Bruno.

### T29 — Bruno aceitar a permuta
> Login como `bruno@levis.com`.

1. Dashboard → Permutas → ver pedido de Ana.
2. Clica **Aceitar** → **Esperado:** estado muda; notificação ao gerente para aprovação.

### T30 — Gerente aprovar a permuta
> Login como `francisco@levis.com`.

1. Painel Gerente → aba Permutas.
2. Aprovação da permuta Ana-Bruno → **Esperado:** estado muda para aprovada; horários trocados.

### T31 — Pedir permuta de folga (troca de dias livres)
> Login como `carla@levis.com`.

1. Permutas → seleccionar tipo "Permuta de Folga".
2. Seleccionar o teu dia de folga e o dia de folga de David.
3. Submeter → **Esperado:** pedido criado.

---

## BLOCO 8 — Geração de Horários

> **Pré-requisito:** loja configurada com turnos e regras + equipa de pelo menos 4 pessoas. Login como gerente.

### T32 — Verificar ecrã de geração (passo 1 — Configurar)
1. Dashboard → **Geração de Horários**.
2. Seleccionar mês: próximo mês | ano: actual.
3. **Esperado:** lista de colaboradores elegíveis aparece com checkboxes.
4. **Verificar:** o resumo diz "X de Y colaboradores selecionados".

### T33 — Botão "O que é considerado?" com todos seleccionados
1. Com todos os colaboradores seleccionados → clica "O que é considerado?".
2. **Esperado:** dialog abre com:
   - Balanço de capacidade (verde = suficiente)
   - Regras de trabalho
   - Cobertura mínima por turno
   - Todos os colaboradores na secção "Equipa elegível"
   - Ausências aprovadas (folga de Ana do T22)
   - Preferências aprovadas (turno de Bruno do T27)

### T34 — Botão "O que é considerado?" com selecção parcial (FIX TESTADO HOJE)
1. **Desseleccionar** Ana Silva e Bruno Costa (uncheckar as checkboxes).
2. Clica "O que é considerado?" → **Esperado crítico:**
   - A secção "Equipa elegível" mostra apenas os colaboradores seleccionados (SEM Ana e Bruno).
   - A folga de Ana NÃO aparece na secção "Ausências aprovadas".
   - A preferência de Bruno NÃO aparece em "Preferências de turno".
   - O balanço de capacidade reflecte APENAS a equipa seleccionada (capacidade menor).
3. Voltar a seleccionar todos antes de continuar.

### T35 — Configurar objetivo da geração
1. No passo 1 → ComboBox "O que priorizar": selecciona **"Preferências"**.
2. ComboBox "Pessoas por turno": selecciona **2**.
3. **Verificar:** os dois campos respondem ao clique sem erro.

### T36 — Gerar proposta única
1. Clica **"Gerar Proposta"** → **Esperado:**
   - Overlay de carregamento aparece com texto (ex: "A gerar alternativa 1 de 1...").
   - Após geração: grelha do horário aparece com turnos distribuídos.
   - Score de qualidade visível.
   - Sem mensagem de erro.

### T37 — Verificar grelha do horário gerado
1. Após T36: analisar a grelha visualmente.
2. **Verificar:**
   - Cada dia tem pelo menos 2 pessoas escaladas por turno.
   - Sábados têm o gerente (Francisco) escalado.
   - Nenhum colaborador trabalha mais de 5 dias seguidos.
3. Toggle **Semana/Mês** → ambas as vistas carregam sem erro.

### T38 — Expandir grelha em janela separada
1. Botão "Expandir" (ou similar) → **Esperado:** janela maximizada com a grelha detalhada (M/T/N por célula, horas, FDS sombreados, legenda).

### T39 — Gerar múltiplas alternativas (lote)
1. Spinner "Nº alternativas" → colocar **3**.
2. Clica "Gerar Alternativas" → **Esperado:**
   - Overlay mostra "A gerar alternativa 1 de 3...", "2 de 3...", "3 de 3...".
   - Tabela de alternativas aparece com 3 linhas (score, qualidade, nº turnos).
   - A melhor alternativa tem badge "RECOMENDADO" ou semelhante.

### T40 — Comparar duas propostas
1. Com 3 alternativas na tabela → selecciona 2 para comparação.
2. Clica "Comparar" → **Esperado:** tabela de comparação com diferenças por colaborador (barras visuais).

### T41 — Verificar horário (sub-página)
1. Com proposta carregada → clica "Verificar".
2. **Esperado:** sub-página abre com:
   - Grelha compacta à esquerda.
   - Painel de cumprimento à direita (scorecard 4 tiles: Regras/Preferências/Carga/FDS).
   - Veredicto global (verde/amarelo/vermelho).
   - Lista de colaboradores com badges CUMPRIDO/PARCIAL/NÃO CUMPRIDO.

### T42 — Ajuste manual na verificação
1. Na sub-página de verificação → clica numa célula da grelha (num dia de um colaborador).
2. Dialog de edição → selecciona outro turno ou "Folga (remover turno)".
3. Confirma → **Esperado:** a verificação re-executa automaticamente; scorecard actualiza.

### T43 — Exportar relatório de verificação em PDF
1. Na sub-página → clica "Exportar relatório (PDF)".
2. **Esperado:** file-chooser abre → selecciona destino → PDF gerado sem erros.
3. Abrir o PDF → **Verificar:** pág.1 tem a grelha; págs. seguintes têm veredicto + indicadores + regras + cumprimento por colaborador.

### T44 — Exportar horário CSV
1. Voltar à vista da proposta → botão "Exportar CSV".
2. **Esperado:** file-chooser → CSV gerado → abrir no Excel: linhas com colaborador, data, turno.

### T45 — Exportar horário PDF (simples)
1. Botão "Exportar PDF" → **Esperado:** PDF gerado com o horário mensal do mês.

### T46 — Ver horário individual de um colaborador
1. Na tabela de resumo da proposta → clica no nome de "Ana Silva".
2. **Esperado:** dialog ou painel com o calendário pessoal de Ana para o mês (dias/turnos atribuídos).

### T47 — Enviar proposta para supervisor
1. Com proposta em estado rascunho → botão "Enviar para Supervisor".
2. **Esperado:** estado muda para "aguarda validação"; botões de aprovação/rejeição ficam activos.

### T48 — Aprovar proposta (como gerente/supervisor)
1. Escreve observações (opcional) → clica "Aprovar".
2. **Esperado:** estado muda para "aprovado"; horário fica publicado; visível no HomeController.

### T49 — Rejeitar proposta
1. Gerar nova proposta → enviar → clica "Rejeitar".
2. **Esperado:** estado muda para "rejeitado"; possibilidade de gerar nova.

---

## BLOCO 9 — Home / Painel Principal

> Login como gerente.

### T50 — Visualizar escala publicada (grelha semanal/mensal)
1. Dashboard → Home.
2. **Verificar:** a escala do mês actual (com a proposta aprovada no T48) está visível.
3. Toggle **Semana/Mês** → ambas as vistas funcionam.
4. Navegar entre semanas → **Esperado:** grelha actualiza.

### T51 — Clicar num dia da grelha
1. Home → clica num dia com turnos.
2. **Esperado:** dialog de detalhe do dia abre com lista de colaboradores escalados.

### T52 — Pedidos pendentes no Home (badge)
1. Verifica que o badge com número de pedidos pendentes está visível no atalho do Painel Gerente.
2. **Esperado:** número coincide com os pedidos pendentes reais.

---

## BLOCO 10 — Perfil de Utilizador

### T53 — Ver perfil próprio
1. Menu lateral → **Perfil**.
2. **Esperado:** nome, email, cargo, loja visíveis.

### T54 — Alterar password
1. Perfil → campo nova password: `654321` | confirmar: `654321`.
2. Gravar → **Esperado:** mensagem de sucesso.
3. Logout → login com `654321` → **Esperado:** acesso concedido.
4. Repor para `123456` (repetir com a nova password).

### T55 — Alterar telemovel
1. Perfil → campo telemovel: `931999999`.
2. Gravar → **Esperado:** valor guardado.

---

## BLOCO 11 — Relatórios e Horas

### T56 — Relatório de horas (perspectiva gerente)
1. Dashboard → **Relatórios de Horas** (se existir módulo separado).
2. Seleccionar mês → **Esperado:** tabela com horas por colaborador.
3. **Verificar:** horas coerentes com os turnos publicados (T48).

### T57 — Relatório de horas (perspectiva funcionário)
> Login como `ana@levis.com`.

1. Ver as suas próprias horas → **Esperado:** só vê as suas (não as dos outros).
2. **Verificar:** total de horas ≤ carga contratual.

---

## BLOCO 12 — Navegação e Atalhos

### T58 — Atalhos de teclado (estando no dashboard)
Testar todos os atalhos definidos:
- `Alt+H` → navega para Horários/Home
- `Alt+G` ou `Alt+P` → Painel Gerente (Pedidos)
- `Alt+L` → Gestão de Loja
- `Alt+F` → Gestão de Funcionários
- `Ctrl+G` → inicia geração (dentro do ecrã de geração)
- **Esperado:** cada atalho navega para o ecrã correcto sem crash.

### T59 — Navegação por separadores do dashboard
1. Clica em cada item da barra lateral (Home, Horários, Loja, Funcionários, Painel, Folga, Permutas, Preferências, Perfil).
2. **Esperado:** cada ecrã carrega sem erros; sem NullPointerException nem ecrã em branco.

### T60 — Scroll em ecrãs com muita informação
1. Gestão de Funcionários com 8 pessoas → verificar que o scroll funciona.
2. Painel de geração com 8 colaboradores na lista → verificar scroll da secção de checkboxes.
3. Painel Gerente com múltiplos pedidos → verificar scroll da lista.

---

## BLOCO 13 — Multi-Loja (se aplicável)

> **Pré-requisito:** criar uma 2ª loja.

```sql
INSERT INTO public.lojas (nome, localizacao, hora_abertura, hora_fecho)
VALUES ('Levi''s NorteShopping', 'Porto', '10:00', '23:00');
```

### T61 — Associar Ana Silva a uma segunda loja
1. Gestão de Funcionários → Ana Silva → adicionar ligação à loja 2 (NorteShopping) com cargo PT.
2. **Esperado:** Ana aparece como activa em ambas as lojas.

### T62 — Verificar aviso de multi-loja na geração
1. Geração → secção de selecção de colaboradores.
2. **Esperado:** Ana Silva aparece com o aviso "⚠ turnos noutras lojas".
3. No botão "O que é considerado?" → tooltip do aviso deve explicar o risco de sobreposição.

### T63 — Geração com colaborador multi-loja bloqueada se houver sobreposição
1. Inserir um turno aprovado para Ana na loja 2 no mesmo dia/hora em que a loja 1 vai gerar.
2. Gerar horário loja 1 com Ana seleccionada → **Esperado:** geração falha com mensagem de sobreposição de horários.

---

## BLOCO 14 — Casos Extremos e Robustez

### T64 — Geração com equipa insuficiente
1. Desseleccionar todos os colaboradores excepto 1 (ex: só o gerente).
2. Gerar → **Esperado:** falha com mensagem clara: "Capacidade insuficiente" + sugestões do que fazer.

### T65 — Geração com mês sem turnos configurados (0 turnos)
1. Apagar os 3 turnos temporariamente via SQL: `TRUNCATE public.turnos CASCADE;`
2. Tentar gerar → **Esperado:** falha com mensagem clara (não crash).
3. Repor os turnos.

### T66 — Pedir folga em data com turno já aprovado
1. Ana tem turno aprovado no dia D → como Ana, pede folga no dia D.
2. **Esperado:** aviso ou bloqueio (dia já tem turno; folga conflituante).

### T67 — Submeter preferência duplicada
1. Bruno já tem preferência de turno aprovada → tenta submeter outra igual.
2. **Esperado:** aviso de duplicado ou sistema aceita e o gerente vê as duas.

### T68 — Verificar comportamento com utilizador sem loja
1. Criar utilizador só em `utilizadores` sem entrada em `lojautilizador`.
2. Fazer login com esse utilizador → **Esperado:** mensagem clara "sem loja atribuída"; não crash.

### T69 — Sessão de geração com timeout (prazo limite)
1. Definir equipa grande (8 pessoas) com preferências conflituantes complexas.
2. Gerar → **Esperado:** geração conclui dentro do prazo razoável (<30s); ou falha com mensagem de timeout, nunca fica presa infinitamente.

---

## BLOCO 15 — Auditoria (registo de acções)

### T70 — Verificar registo de auditoria após aprovações
Após completar T22 (aprovação de folga) e T23 (rejeição com motivo):

```sql
SELECT tipo_evento, resultado, origem, detalhes, data_evento
FROM public.eventos_auditoria
ORDER BY data_evento DESC
LIMIT 20;
```

**Esperado:**
- Linha com `tipo_evento = 'FOLGA_APROVADA'` para a aprovação de T22.
- Linha com `tipo_evento = 'FOLGA_REJEITADA'` com o motivo no campo `detalhes`.
- `origem` = `desktop` (ou similar).

### T71 — Verificar registo de auditoria de preferências
Após T27:
```sql
SELECT tipo_evento, detalhes FROM public.eventos_auditoria
WHERE tipo_evento LIKE '%PREFERENCIA%'
ORDER BY data_evento DESC;
```
**Esperado:** entradas para aprovação e rejeição de preferências.

---

## BLOCO 16 — Notificações

### T72 — Ver notificações (perspectiva do funcionário)
> Login como `ana@levis.com`.

1. Dashboard → ícone de notificações (sino ou badge).
2. **Esperado:** lista de notificações inclui:
   - "A tua folga foi aprovada" (T22).
   - "As tuas férias foram rejeitadas: Período conflituante..." (T23).

### T73 — Marcar notificação como lida
1. Clica numa notificação → **Esperado:** fica marcada como lida (visual muda); contador decresce.

---

## BLOCO 17 — Validação Final End-to-End

### T74 — Fluxo completo: geração → aprovação → publicação → visualização
1. Login como gerente.
2. Gerar horário para o próximo mês (com toda a equipa e preferências).
3. Verificar a proposta → fazer 1 ajuste manual.
4. Exportar PDF de verificação.
5. Enviar para supervisor → aprovar.
6. Login como Ana → verificar que o horário publicado está visível no seu perfil/home.
7. **Esperado:** toda a cadeia funciona sem erros.

### T75 — Consistência de dados após todas as operações
```sql
-- Verificar que não há horários órfãos (sem lojautilizador válido)
SELECT COUNT(*) FROM public.horarios h
LEFT JOIN public.lojautilizador lu ON lu.id_lojautilizador = h.id_lojautilizador
WHERE lu.id_lojautilizador IS NULL;
-- Esperado: 0

-- Verificar que permutas aprovadas trocaram os lojautilizadores correctamente
SELECT p.id_permuta, h1.id_lojautilizador as orig, h2.id_lojautilizador as dest, p.estado
FROM public.permutas p
JOIN public.horarios h1 ON h1.id_horario = p.id_horario_origem
JOIN public.horarios h2 ON h2.id_horario = p.id_horario_destino;
-- Verificar manualmente: estado='aprovada' deve reflectir troca nos horários.
```

---

## Checklist Rápida (resumo)

| # | Funcionalidade | Testado | OK |
|---|---|---|---|
| T01-T04 | Autenticação e sessão | | |
| T05-T10 | Setup loja (cargos, turnos, regras, dias especiais) | | |
| T11-T16 | Gestão de funcionários | | |
| T17-T20 | Pedir folga (funcionário) | | |
| T21-T23 | Aprovar/rejeitar folgas (gerente) + motivo obrigatório | | |
| T24-T27 | Preferências (submit + aprovação) | | |
| T28-T31 | Permutas de turno e de folga | | |
| T32-T35 | Geração: configurar, selecção colaboradores, objetivo | | |
| T36-T40 | Geração: gerar, comparar alternativas | | |
| T41-T46 | Verificação, ajuste manual, exportar PDF/CSV | | |
| T47-T49 | Envio para supervisor e aprovação/rejeição de proposta | | |
| T50-T52 | Home: grelha publicada, detalhe dia, badges | | |
| T53-T55 | Perfil: password, telemovel | | |
| T56-T57 | Relatórios de horas | | |
| T58-T60 | Navegação, atalhos, scroll | | |
| T61-T63 | Multi-loja e sobreposição | | |
| T64-T69 | Casos extremos e robustez | | |
| T70-T71 | Auditoria de acções | | |
| T72-T73 | Notificações | | |
| T74-T75 | End-to-end e consistência de dados | | |

---

*Guião gerado em 2026-06-17. Actualizar se forem adicionadas novas funcionalidades.*
