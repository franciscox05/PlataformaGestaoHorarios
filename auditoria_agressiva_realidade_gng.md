# Auditoria Agressiva — Realidade Operacional GNG (85 lojas)

Auditoria conduzida por leitura direta do código-fonte atual (sem assumir nada
do relatório anterior). Cobre os 4 cenários pedidos: trocas cross-store,
flag `ativo`, IDOR no portal web, e performance do motor de geração.

---

## 1. Conflitos de trocas inter-lojas (cross-store edge cases)

**Veredito: SEM bug de conformidade — mas com uma descoberta relevante de produto.**

### 1.1 — Permutas de turno (`Permuta`) são bloqueadas entre lojas diferentes
`PermutaService.validarPedido()`, `PermutaService.java:292-297`:
```java
Integer idLojaOrigem = meuTurno.getIdLojautilizador().getIdLoja().getId();
Integer idLojaDestino = turnoColega.getIdLojautilizador().getIdLoja().getId();

if (idLojaOrigem == null || !idLojaOrigem.equals(idLojaDestino)) {
    throw new IllegalArgumentException("A permuta so pode ser feita com turnos da mesma loja.");
}
```
O mesmo bloqueio existe em `PermutaFolgaService.validar()`, `PermutaFolgaService.java:208-212`.

**Implicação para o pitch:** o slide 3 do pitch de negócio fala em "permutas cruzadas
inter-lojas em tempo real" — isto **não corresponde ao código atual**. O sistema
desenhado e implementado é deliberadamente **same-store only** para trocas de turno
e de folga (decisão de negócio sensata: evita que um gerente perca visibilidade sobre
quem está escalado na sua própria loja). O termo "cross-store" no código refere-se à
**guarda** que impede a trocas entre lojas, não a uma funcionalidade que as permite.

`[Risco Identificado]` Inconsistência de marketing/produto, não de segurança ou de
conformidade legal. O guião de pitch describe uma funcionalidade que o sistema
ativamente impede.
`[Ficheiro e Linha]` `guiao_apresentacoes_25junho.txt` (Slide 3) vs.
`PermutaService.java:295-296` / `PermutaFolgaService.java:210-212`.
`[Proposta de Correção Direta]` Reformular o bullet do Slide 3 para
"Pedidos de folgas e preferências em tempo real, com permutas de turno e de folga
validadas e contidas dentro da mesma loja — preservando a responsabilidade do
gerente local" — ou, alternativamente, decidir como equipa se cross-store permutas
deve ser uma funcionalidade real de Projeto 3, dado que a base de dados e os
repositórios já suportam consultas globais por utilizador (ver 1.2).

### 1.2 — Onde o sistema JÁ soma turnos de ambas as lojas: descanso mínimo e sobreposição
Apesar do bloqueio de trocas entre lojas diferentes, as validações de **descanso
mínimo de 11h** e de **sobreposição de turnos** para um colaborador com vínculos em
múltiplas lojas (multi-loja) **não estão isoladas por `idLoja`**:

- `PermutaService.validarDescansoMinimoPosPermuta()` chama
  `horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(idColaborador, ...)`
  — filtra apenas por `idUtilizador`, sem `idLoja` (`HorarioRepository.java:51-53`).
- `countGlobalOverlappingShifts` / `countGlobalOverlappingShiftsExcluding`
  (`HorarioRepository.java:369-372`, `400-404`) — o próprio nome e os comentários em
  `PermutaFolgaService.java:331` ("incluindo turnos noutras lojas") confirmam que
  não há filtro de loja.

**Conclusão:** se um colaborador multi-loja tivesse turnos em Braga Parque e em
Guimarães no mesmo período, o sistema **já detetaria** a violação de descanso/
sobreposição corretamente, porque a query agrega por utilizador, não por loja.
Isto é exatamente o comportamento correto exigido pela ACT — o descanso é um
direito da pessoa, não da loja.

`[Risco Identificado]` Nenhum. Arquitetura resiliente por desenho: a unidade de
agregação para regras de descanso é o `idUtilizador`, nunca o `idLoja`.

---

## 2. Comportamento da flag `ativo` em Regras e Turnos

**Veredito: blindado. Sem NullPointerException, sem regras fantasma, sem quebra de integridade.**

### 2.1 — Regras desativadas são corretamente ignoradas, com fallback null-safe
`RegraGeracaoResolver.obterRegrasAplicadas()`, `RegraGeracaoResolver.java:51-56`:
```java
for (RegrasLoja regraLoja : regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(idLoja)) {
    if (regraLoja.getIdRegra() != null && regraLoja.getIdRegra().getId() != null
            && !Boolean.FALSE.equals(regraLoja.getAtivo())) {
        overrides.put(regraLoja.getIdRegra().getId(), regraLoja);
    }
}
```
O padrão `!Boolean.FALSE.equals(x)` é deliberadamente null-safe: trata `ativo == null`
como "ativo" (default seguro, coerente com `private Boolean ativo = true;` na entidade)
e só exclui quando `ativo` é explicitamente `false`. Uma `RegrasLoja` desativada
simplesmente não entra no mapa `overrides`, pelo que `resolverParametrosGeracao()`
cai automaticamente para o valor padrão da `Regra` global (`regra.getValorPadrao()`,
linha 64) — sem qualquer risco de NPE, porque o valor nunca é lido da entidade
desativada.

O mesmo padrão repete-se em `GestaoLojaService.java:178` (ao gravar) e `:564`
(ao construir o resumo para a UI).

### 2.2 — Turnos desativados são filtrados na própria query SQL, não em memória
`TurnoRepository.java:15`:
```java
@Query("SELECT t FROM Turno t WHERE t.ativo = true ORDER BY t.horaInicio ASC")
List<Turno> findAllAtivosOrderByHoraInicioAsc();
```
Usado em `GeracaoHorariosService.java:556` e `:772` — os únicos dois pontos onde o
motor de geração obtém a lista de turnos candidatos. Um turno com `ativo = false`
**nunca chega a `HorarioGeneratorEngine`**, porque a exclusão acontece na base de
dados, antes de qualquer lógica Java correr. Não há caminho de código onde o motor
veja um turno desativado.

### 2.3 — Soft-delete real, com hard-delete guardado contra quebra de FK
`GestaoLojaService.java:405-414` (`desativarTurno`) confirma o soft-delete:
```java
/** Desativa um turno — fica invisível para futuras gerações mas os registos históricos são preservados. */
@Transactional
public void desativarTurno(Integer idUtilizador, Integer idTurno) {
    ...
    turno.setAtivo(false);
    turnoRepository.save(turno);
}
```
Apenas atualiza um campo booleano — os `Horario` existentes que referenciam esse
`Turno` por FK (`Horario.java:23-25`, `@JoinColumn(name = "id_turno", nullable = false)`)
**não são tocados**. Histórico intacto, garantido por desenho.

Existe também um **hard-delete** físico (`removerTurno`, `GestaoLojaService.java:429-441`),
mas está corretamente guardado:
```java
if (turnoRepository.existeEmHorarios(idTurno)) {
    throw new IllegalArgumentException(
            "Não é possível eliminar um turno com horários atribuídos. Desativa-o em vez de eliminar.");
}
turnoRepository.deleteById(idTurno);
```
Um turno só pode ser fisicamente eliminado se **nunca** tiver sido usado num
`Horario` — elimina o risco de violação de FK em cascata ou de apagar histórico.

`[Risco Identificado]` Nenhum nos três sub-cenários (regras, geração, integridade
referencial). O padrão `Boolean.FALSE.equals(...)` / `Boolean.TRUE.equals(...)`
usado consistentemente em toda a base de código é a defesa correta contra NPE em
campos `Boolean` (em vez de `boolean`) que podem ser `null` por dados legados.

---

## 3. Concorrência e IDOR no Portal Web

**Veredito: sem IDOR explorável nos caminhos auditados — contexto de autorização é sempre derivado da sessão, nunca do request.**

### 3.1 — `idUtilizador` e `idLoja` de autorização vêm sempre da sessão, nunca do cliente
`WebAppService.java:54-59` e `:92-95`:
```java
public Integer obterUtilizadorId(HttpSession session) {
    return (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
}
public Integer obterLojaAtual(HttpSession session) {
    return (Integer) session.getAttribute(WebSession.LOJA_ID);
}
```
Nenhum destes dois valores é lido de `@RequestParam` em nenhum endpoint de
aprovação/rejeição (`WebEquipaController`, `WebComplementaresController`,
`WebPermutasApiController`). Confirmado por busca exaustiva: os únicos
`@RequestParam idLoja`/`idUtilizador` que existem na camada WEB
(`WebLoginController.java:124`, `WebEquipaController.java:246`) são tratados como
**inputs a validar**, não como afirmações de identidade — ver 3.2 e 3.3.

### 3.2 — `escolherLoja` (o único `idLoja` vindo do cliente) é validado contra a posse real
`WebLoginController.java:132-139`:
```java
boolean pertence = lojautilizadorRepository
        .findLigacaoAtivaByIdUtilizadorAndIdLoja(idUtilizador, idLoja)
        .isPresent();
if (!pertence) {
    redirectAttributes.addFlashAttribute("erro", "Loja inválida.");
    return "redirect:/web/selecionar-loja";
}
```
Um utilizador não pode forjar `idLoja` para entrar numa loja a que não tem vínculo
ativo — a sessão só é atualizada após confirmação na base de dados.

### 3.3 — `idUtilizador` em `WebEquipaController.guardarColaborador` é re-validado no service
`GestaoFuncionariosService.atualizarColaborador()`, linhas 209-212:
```java
List<Lojautilizador> historicoNaLoja = lojautilizadorRepository.findHistoricoByIdLojaAndIdUtilizador(idLoja, idColaborador);
if (historicoNaLoja.isEmpty()) {
    throw new IllegalArgumentException("Este colaborador nao pertence a loja que estas a gerir.");
}
```
Mesmo que um gestor malicioso altere o `idUtilizador` no formulário POST para o ID
de um colaborador de outra loja, o `idLoja` continua a vir da sessão do gestor
(nunca do request), e o service rejeita a operação porque o colaborador alvo não
tem histórico nessa loja.

### 3.4 — Aprovações cross-store: dupla verificação na camada de serviço
`PermutaService.obterPedidoPendenteGerivel()`, linhas 394-401:
```java
Integer idLojaAprovador = ligacaoAtiva.getIdLoja().getId();
Integer idLojaOrigem = pedido.getIdHorarioOrigem().getIdLojautilizador().getIdLoja().getId();
Integer idLojaDestino = pedido.getIdHorarioDestino().getIdLojautilizador().getIdLoja().getId();
Integer idSolicitante = pedido.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getId();

if (!idLojaAprovador.equals(idLojaOrigem) || !idLojaAprovador.equals(idLojaDestino) || idUtilizadorAprovador.equals(idSolicitante)) {
    throw new IllegalArgumentException("Nao tens permissao para gerir este pedido de permuta.");
}
```
Um gerente da Loja A não pode aprovar/rejeitar um pedido de permuta de outra loja
mesmo manipulando o `idPermuta` no POST, porque `idLojaAprovador` vem da
`ligacaoAtiva` da sessão, não do request. O mesmo padrão existe em
`PermutaFolgaService.obterPendente()` (linhas 411-413) e nos métodos análogos de
`DayOffService`/`PreferenciaService` (não reproduzidos aqui por brevidade, mas
seguem a mesma estrutura `lojautilizadorHelper.obterLigacaoAtivaComCargo`).

### 3.5 — Único ponto de atenção real: o `WebGuardInterceptor` não valida `idLoja`, só módulo
`WebGuardInterceptor.podeAcederAoModulo()` (linhas 45-55) decide acesso a
`/web/gestao-loja` e `/web/relatorios` com base em **permissões de cargo**
(`podeGerirLoja()`, `podeVerRelatorios()`), mas não impede o acesso a
`/web/equipa`, `/web/complementares`, etc. — esse controlo fica inteiramente a cargo
de cada controller/service individual (que, como mostrado em 3.1-3.4, fazem o
scoping corretamente). Isto não é um bug, mas é um ponto de fragilidade
arquitetural: a autorização "por loja" não é centralizada num único guard, está
distribuída por ~6 services diferentes que repetem o mesmo padrão manualmente.

`[Risco Identificado]` Duplicação do padrão de verificação de loja
(`lojautilizadorHelper.obterLigacaoAtivaComCargo`) em vários services, sem um
mecanismo central que force essa verificação (ex.: um aspeto/anotação). É o mesmo
tipo de causa raiz que gerou o bug A01:2021 original no `WebGuardInterceptor`
(rotas duplicadas em vez de centralizadas) — risco de regressão se um novo endpoint
for adicionado e o autor se esquecer de chamar o helper de scoping.
`[Ficheiro e Linha]` Padrão repetido em `PermutaService.java:394-401`,
`PermutaFolgaService.java:411-413`, `GestaoFuncionariosService.java:209-212`, etc.
`[Proposta de Correção Direta]` Não é urgente corrigir agora (todos os pontos
auditados estão corretos), mas registar como débito técnico para Projeto 3: extrair
um `@LojaScoped` (interceptor/aspect) ou um wrapper de serviço único que centralize
"verifica que o recurso X pertence à loja ativa do utilizador", para que novos
endpoints não possam esquecer-se da verificação.

---

## 4. Performance do motor de geração com escala real (20+ colaboradores)

**Veredito: protegido por orçamento de nós E por prazo (deadline) — sem risco de loop infinito ou StackOverflow.**

`HorarioGeneratorEngine.java` implementa backtracking com **dois mecanismos de
corte independentes**, ambos verificados durante a recursão (não apenas no início):

### 4.1 — Orçamento progressivo de nós de pesquisa
```java
private static final int LIMITE_NOS_PESQUISA_BASE     = 12_000;
private static final int LIMITE_NOS_PESQUISA_ALARGADO = 24_000;
private static final int LIMITE_NOS_PESQUISA_EXCECAO  = 40_000;
```
(`HorarioGeneratorEngine.java:80-82`). O motor tenta primeiro com o orçamento base
(sem relaxar regras); se não encontrar solução viável, escala para o orçamento
alargado e, em último recurso, relaxa restrições soft (rotação de fins de semana,
descanso semanal) apenas ao fim de semana, com o orçamento de excecão — nunca corre
sem limite.

### 4.2 — Deadline de parede (`prazoLimite`) verificado em múltiplos pontos da recursão
```java
if (pedido.prazoLimite() != null && Instant.now().isAfter(pedido.prazoLimite())) break;   // linha 618
if (prazo != null && Instant.now().isAfter(prazo)) { ... }                                // linhas 1092, 1264
```
O deadline é passado para o `PesquisaContexto` (linha 820) e consultado tanto no
loop externo como dentro da própria recursão de backtracking — significa que mesmo
com 20 colaboradores e múltiplas restrições cruzadas, a pesquisa aborta de forma
controlada (lançando `FalhaGeracaoHorarioException` com diagnóstico, não um erro
não tratado) em vez de correr indefinidamente.

### 4.3 — Multi-start estocástico é condicionado por margem de segurança
```java
// Só em produção (prazoLimite definido) e apenas se há ≥ 4 s de margem.
if (pedido.prazoLimite() != null) { ... }
```
(`HorarioGeneratorEngine.java:135-136`) — as tentativas adicionais com ruído gaussiano
(linha 305-306) só correm se sobrar pelo menos 4 segundos de margem antes do
deadline, evitando que a exploração de soluções alternativas ultrapasse o tempo
disponível.

### 4.4 — Risco residual: profundidade de recursão vs. StackOverflowError
Não foi encontrada nenhuma conversão explícita de recursão para iteração (ex.: pilha
manual). Para uma loja com 20 colaboradores e um mês de 30 dias × ~3 turnos/dia, a
profundidade máxima teórica de recursão (uma chamada por slot dia×turno) ronda as
poucas centenas de níveis — muito abaixo do limite típico de stack da JVM
(tipicamente milhares de frames antes de `StackOverflowError`, dependendo de
`-Xss`). Não é um risco prático à escala atual de uma loja, mas **pode tornar-se um
problema se o motor for reaproveitado para gerar várias lojas em simultâneo na
mesma thread/contexto, ou se o horizonte de geração for alargado de "1 mês" para
"1 ano"**.

`[Risco Identificado]` Risco residual de baixa probabilidade, não materializado nos
cenários atuais (uma loja, um mês). Não há orçamento de nós que limite diretamente a
*profundidade* da recursão, apenas o *número total de nós visitados* — em teoria, uma
combinação especialmente perversa de restrições poderia gerar uma cadeia profunda
antes de esgotar o orçamento de nós.
`[Ficheiro e Linha]` `HorarioGeneratorEngine.java:886` (comentário já identifica
"Backtracking recursivo com poda por orçamento de nós, prazo e viabilidade" — falta
explicitamente um limite de profundidade).
`[Proposta de Correção Direta]` Adicionar um contador de profundidade passado ao
método recursivo, com um limite explícito (ex.: `numDias × numTurnosPorDia × 2`) que
lança `FalhaGeracaoHorarioException` de forma controlada caso seja ultrapassado —
defesa em profundidade adicional, independente do orçamento de nós já existente.
Prioridade baixa: não há evidência de que isto ocorra com os volumes reais da GNG.

---

## Resumo Executivo

| # | Cenário | Risco real encontrado | Severidade |
|---|---|---|---|
| 1 | Cross-store rest/overlap | Nenhum — agregação é por utilizador, não por loja | — |
| 1b | Cross-store permuta | Inconsistência **marketing vs. código** (slide do pitch promete o que o sistema bloqueia) | Média (reputacional, não técnica) |
| 2 | Flag `ativo` | Nenhum — null-safe em todo o codebase, soft-delete real, hard-delete guardado | — |
| 3 | IDOR no portal web | Nenhum nos caminhos auditados — autorização sempre via sessão + re-validação no service | — |
| 3b | Padrão de scoping disperso | Débito técnico — sem guard central por loja, risco de regressão futura | Baixa (preventiva) |
| 4 | Performance do motor | Nenhum — orçamento de nós + deadline duplo; risco residual de profundidade de recursão não materializado à escala atual | Baixa (preventiva) |

**Conclusão geral:** a arquitetura é tecnicamente resiliente aos quatro cenários de
ataque/abuso propostos. O único ponto que precisa de ação **antes do dia 25** não é
de código — é de **conteúdo do pitch**: o Slide 3 do guião comercial reivindica uma
funcionalidade de permuta cross-store que o sistema atual bloqueia deliberadamente.
Recomenda-se corrigir esse slide antes da apresentação ao CEO da GNG, para evitar
uma pergunta incómoda em público sobre uma funcionalidade que, na demonstração
técnica aos professores, será visivelmente impossível de reproduzir.
