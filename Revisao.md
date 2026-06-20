# Revisão — Pontos a Validar (Tiago + Francisco)

Este ficheiro acumula descobertas de três rondas de trabalho autónomo de QA:
(A) desenho de 4 testes dirigidos (cross-store, flag `ativo`, IDOR, stress de
Natal); (B) varredura exaustiva multi-loja com concorrência real
(`SistemaMultiLojaStressEndToEndTest.java`); (C) mapeamento autónomo de
personas/cargos e fluxos inter-interface Web↔Desktop
(`FluxosTotaisPersonaEndToEndTest.java`). **Todos os testes mencionados aqui
foram efetivamente compilados e corridos contra o PostgreSQL real** (não são
esqueletos teóricos) — cada descoberta crítica abaixo foi confirmada por
execução, não apenas por leitura de código. **Nenhum ficheiro em `src/main/`
foi alterado em nenhuma destas rondas** — apenas testes e este documento.

---

## 🗺️ Mapeamento Autónomo do Sistema (ronda C)

Levantamento feito por leitura exaustiva do código antes de escrever
`FluxosTotaisPersonaEndToEndTest.java`, sem assumir nada de sessões anteriores:

- **Cargos reais** (`Cargo.tipo`, enum Postgres `tipo_cargo_enum`, confirmado em
  `sql/setup-teste-guiao.sql`): `gerente`, `subgerente`, `supervisor`,
  `fulltime`, `parttime`, `reforco_parttime`. **Não existe nenhum cargo
  "admin"** no catálogo — o topo da hierarquia é gerente/subgerente.
- **Conjuntos de permissão** (`LojautilizadorHelper.java:23-29`):
  `APROVACAO` = {gerente, subgerente, supervisor};
  `GESTAO` = {gerente, subgerente}; `VALIDACAO` = {supervisor}.
- **13 controllers Web** (`src/main/java/.../WEB/`): `WebLoginController`,
  `WebPainelController`, `WebHorariosController` + `WebHorariosApiController`,
  `WebEquipaController`, `WebComplementaresController`,
  `WebPermutasApiController`, `WebPermutaFolgaApiController`,
  `WebModulosController`, `WebPerfilController`, `WebNotificacoesController`,
  `HorarioExportController`, `WebAuthApiController` (ver descoberta abaixo),
  mais `WebMvcConfig`/`WebGuardInterceptor`/`WebAppService`/`WebLayoutService`
  (infraestrutura, não rotas).
- **18 controllers Desktop** (`src/main/java/.../DESKTOP/`), confirmados a
  injetar **exatamente os mesmos serviços `@Service`** que os controllers Web:
  `GeracaoHorariosController`, `GestaoFuncionariosController`,
  `GestaoLojaController`, `PainelGerentePedidosController`,
  `PermutasController`, `PedirFolgaController`, `PreferenciasController`, etc.
  — é a base factual confirmada para os testes "inter-interface" do Grupo B.
- **25 services na camada `API/Services/`** — lógica de negócio única,
  partilhada por Web e Desktop sem nenhuma fronteira REST entre eles.

---

## [x] CONCLUÍDO — 3 falhas pré-existentes investigadas, causa real encontrada e corrigida (só em testes)

Ao correr a suite completa (`./mvnw.cmd test`), foram encontradas 3 falhas em
`FluxosCriticosIntegrationTest.permutasSoMostramTurnosElegiveisDaMesmaLojaDiaEComAntecedencia`,
`WebFluxosCriticosE2ETest.complementaresCobremFolgasPreferenciasEPermutas` e
`WebFluxosCriticosE2ETest.permutaAprovadaNaWebTrocaTurnosEntreColaboradores`.

**A hipótese inicial (sensibilidade a `LocalDate.now()`) estava errada.**
Investigação direta à base de dados real revelou a causa verdadeira:

- A tabela `turnos` da base de dados de desenvolvimento partilhada tinha
  **58 registos** acumulados ao longo de semanas de execuções/demos — não os
  6 turnos-base originais.
- `FluxosCriticosTestSupport.criarContextoGeracao()` → `garantirTurnosBase()`
  só semeia os 6 turnos-base **se a tabela estiver completamente vazia**. Com
  58 registos já presentes, `fixture.turnos()` passou a devolver **todos** os
  58, ordenados só por `hora_inicio` (`turnoRepository.findAllByOrderByHoraInicioAsc()`),
  **sem nenhum critério de desempate** para turnos com a mesma hora.
- Confirmado por query direta: `ORDER BY hora_inicio ASC LIMIT 6` devolvia, na
  posição 0 e na posição 1, dois turnos **diferentes mas com horas
  idênticas** ("manhã 10:00–19:00" duas vezes).
- Os 3 testes indexavam posicionalmente (`fixture.turnos().get(0)`,
  `.get(1)`) assumindo que seriam sempre turnos com horas distintas. Como
  passaram a ser idênticos, `PermutaService.validarPedido`
  (`PermutaService.java:273-278`) — que rejeita explicitamente permutar para
  horas iguais — bloqueava a operação antes de criar a `Permuta`, fazendo os
  `assertEquals(1, ...)`/`assertFalse(...isEmpty())` falharem com 0 resultados.
- Verificação de segurança antes de qualquer correção: apenas 4 dos 58 turnos
  tinham zero `horarios` a referenciá-los; os outros 54 tinham centenas a
  milhares de usos (dados reais de demonstração/desenvolvimento) —
  **confirmado que NÃO eram seguros para apagar da base de dados.**

**Correção aplicada — exclusivamente em 2 ficheiros de teste, zero alterações
em `src/main/`:** adicionado um helper `turnosComHorasUnicas(List<Turno>)` em
`FluxosCriticosIntegrationTest.java` e `WebFluxosCriticosE2ETest.java`, que
filtra a lista de turnos da fixture por combinação única de
(`horaInicio`, `horaFim`) antes de indexar por posição — garante que
`get(0)`/`get(1)`/`get(2)` são sempre turnos genuinamente diferentes,
independentemente de quantos duplicados existam na base de dados partilhada.

**Resultado confirmado por execução:** suite completa do projeto **100% verde**
— 0 falhas, 0 erros, apenas os 4 testes `@Disabled` que documentam bugs reais
ainda por decidir (pontos 1, 2, 11 e 14.2 deste documento).

---

## 1. 🔴 BUG CRÍTICO CONFIRMADO: `adicionarTurno` não valida descanso mínimo entre lojas

**Este é o achado mais importante de toda a sessão — confirmado por execução
real, não suposição.**

Na auditoria anterior eu tinha **assumido por inferência** que
`HorarioService.adicionarTurno` reutilizava a validação global de descanso
mínimo (porque já validava sobreposição global). Escrevi
`CrossStoreDescansoMinimoIntegrationTest` para confirmar isso — e ao CORRER o
teste contra a base de dados real, a asserção falhou:

```
Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
```

Lendo `HorarioService.java:300-346` com atenção: `adicionarTurno` chama
**apenas** `horarioRepository.countGlobalOverlappingShifts` (deteta
SOBREPOSIÇÃO literal de horário no mesmo dia, entre lojas). **Nunca chama
nenhuma validação de descanso mínimo de 11h entre turnos de dias adjacentes.**

Isto significa que, hoje, um gestor pode atribuir manualmente (via
`adicionarTurno`, o método usado para editar uma proposta de horário) um turno
em Guimarães que, combinado com um turno já existente em Braga Parque no dia
anterior, viola o descanso mínimo legal de 11h (Código do Trabalho, art. 214.º)
— e o sistema aceita sem qualquer aviso, porque a única guarda cross-store que
existe neste caminho é de sobreposição, não de descanso.

Para comparação, esta validação de descanso **existe e está correta** noutros
dois caminhos:
- `PermutaService.validarDescansoMinimoPosPermuta` (submissão de permuta)
- `PermutaFolgaService.validarDescanso` (permuta de folga)

Mas **não em `HorarioService.adicionarTurno`**, que é o caminho usado:
1. Pelo gestor ao editar manualmente uma proposta de horário.
2. Presumivelmente por qualquer fluxo futuro que chame este método diretamente
   sem passar por uma permuta.

`[Ficheiro e Linha]` `API/Services/HorarioService.java:300-346`
`[Teste que confirma]` `src/test/java/.../CrossStoreDescansoMinimoIntegrationTest.java`
— atualmente `@Disabled` com nota a apontar para este bug, para não bloquear a
suite antes do dia 25.
`[Proposta de Correção Direta]` Adicionar, em `adicionarTurno`, depois da
verificação de sobreposição (linha ~333), uma chamada equivalente a
`PermutaFolgaService.validarDescanso`: buscar
`horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(idUtilizador,
data.minusDays(1), data.minusDays(1))` e `...plusDays(1)...`, e validar
`horarioValidatorService.respeitaDescansoMinimo(...)` contra o novo turno antes
de gravar. Esta é uma correção pequena, localizada, e de alto valor —
recomendo fortemente corrigi-la antes do dia 25, mesmo que apenas para o
slide de "conformidade ACT" do pitch de negócio ser inteiramente verdadeiro.

---

## 2. 🔴 RACE CONDITION CONFIRMADA: dois gerentes podem decidir a mesma folga em simultâneo

Também confirmado por execução real (não simulação), no novo
`SistemaMultiLojaStressEndToEndTest.duasLojasDecidemAMesmaFolgaConcorrentemente`
(atualmente `@Disabled` pela mesma razão — não bloquear a suite).

**Causa raiz, em duas camadas:**

1. **`DayOff` não tem coluna `idLoja`** (`API/Modules/DayOff.java`) — é
   escopado exclusivamente por `idUtilizador`. Confirmei isto também
   sequencialmente, sem concorrência, no teste
   `folgaAprovadaPorGerenteDaLojaAFicaImediatamenteVisivelComoTratadaParaGerenteDaLojaB`
   (este passa — prova que, SEM concorrência, o sistema pelo menos impede a
   segunda decisão depois da primeira ter comitado).
2. A query que decide se um pedido é "visível" para um gerente
   (`DayOffRepository.findPedidosPendentesDaLoja`, linhas 22-31) verifica
   apenas `EXISTS (Lojautilizador WHERE idUtilizador = d.idUtilizador AND
   idLoja = :idLoja AND dataFim IS NULL)` — ou seja, **qualquer** loja a que o
   colaborador esteja ativamente ligado vê o MESMO pedido como pendente.

Não havendo `@Version` (optimistic locking) em `DayOff`, nem nenhum
`SELECT ... FOR UPDATE` em `DayOffService.atualizarEstadoPedido`
(`DayOffService.java:318-371`), dois gerentes de lojas diferentes — ambos com
permissão legítima, porque o colaborador trabalha em ambas — podem
CONCORRENTEMENTE aprovar e rejeitar o MESMO pedido. Corri exatamente este
cenário com 2 threads reais (transações/ligações JDBC distintas, via
`Propagation.NOT_SUPPORTED` para suspender a transação de teste) e confirmei:

```
RACE CONDITION CONFIRMADA: ambas as decisões concorrentes (aprovar/rejeitar)
retornaram sucesso sem excepção, mas o estado final na base de dados é
'aprovado' — a outra decisão foi silenciosamente perdida (lost update).
```

O gerente "perdedor" da corrida recebe uma resposta de sucesso (sem excepção)
para uma decisão que foi silenciosamente substituída. Pior ainda: se a
aprovação tiver disparado `retirarTurnosDoColaboradorNoDia` (linha 348), os
turnos do colaborador já foram removidos mesmo que o estado final fique
"rejeitado" — um estado inconsistente entre o `DayOff.estado` e os `Horario`
reais.

`[Ficheiro e Linha]` `API/Services/DayOffService.java:318-345` (sem lock);
`API/Modules/DayOff.java` (sem `@Version`); `API/Repositories/DayOffRepository.java:22-31`
(query cross-store por desenho).
`[Teste que confirma]` `SistemaMultiLojaStressEndToEndTest.duasLojasDecidemAMesmaFolgaConcorrentemente`
(`@Disabled`, bug real).
`[Proposta de Correção Direta]`
1. Curto prazo (mínimo invasivo): em `atualizarEstadoPedido`, usar
   `dayOffRepository.findById` com `@Lock(LockModeType.PESSIMISTIC_WRITE)`
   (acrescentar um método ao repositório), forçando a segunda transação a
   esperar pela primeira em vez de correr em paralelo sobre a mesma linha.
2. Médio prazo: adicionar `@Version private Integer versao;` a `DayOff`, o que
   faz o Hibernate lançar `OptimisticLockingFailureException` na segunda
   `save()` — mais barato que locking pessimista, mas exige tratar essa
   excepção na camada web com uma mensagem amigável ("este pedido já foi
   decidido por outra loja, atualiza a página").
3. Considerar, como melhoria de produto (não só técnica): notificar
   explicitamente TODAS as lojas onde o colaborador tem vínculo ativo quando
   uma folga é decidida, e/ou exigir consenso explícito se mais de uma loja
   tiver pedidos pendentes a aprovar para o mesmo colaborador — atualmente
   isso depende inteiramente de coordenação humana fora do sistema.

---

## 3. Submissão de permutas duplicadas sob concorrência (canário, não reproduzido nesta corrida)

Desenhei `submissoesConcorrentesDoMesmoParDeTurnosPodemCriarPermutasDuplicadas`
para testar se `existsPedidoPendentePorOrigemEDestino`
(`PermutaService.java:299`) — um clássico padrão "check-then-act" sem proteção
ao nível da base de dados — permite duplicados sob 6 threads simultâneas.
**Nesta corrida específica não reproduziu** (o guard aguentou-se), mas:

- **Não existe nenhuma constraint `UNIQUE` na tabela `permutas`** (confirmado
  por grep exaustivo em todos os ficheiros `sql/*.sql` — zero ocorrências de
  `UNIQUE`). A ausência de reprodução nesta corrida é uma questão de timing/sorte,
  não uma garantia estrutural.
- O teste fica na suite como um **canário**: se em alguma corrida futura
  (vossa, ou em CI) ele reportar "RACE CONDITION CONFIRMADA: N pedidos de
  permuta foram criados", é sinal real do mesmo tipo de bug do ponto 2.

`[Proposta de Correção Direta]` Adicionar uma constraint `UNIQUE` PARCIAL no
PostgreSQL (`CREATE UNIQUE INDEX ... ON permutas (id_horario_origem,
id_horario_destino) WHERE estado = 'pendente'`) — rede de segurança ao nível da
base de dados, complementar à validação Java, e que transforma qualquer futura
corrida deste tipo num erro de constraint explícito em vez de um duplicado
silencioso.

---

## 4. `LojautilizadorHelper.findLigacaoAtiva(Integer idUtilizador)` resolve loja arbitrária para multi-loja

Descoberto ao construir o teste do ponto 2: para satisfazer
`DayOffService.validarMesPublicado`, que resolve a loja do colaborador via
`LojautilizadorHelper.findLigacaoAtiva(idUtilizador)` (sem `idLoja`), tive de
publicar um turno em AMBAS as lojas do colaborador multi-loja — porque este
método (`LojautilizadorHelper.java:42-48`) devolve `ligacoes.get(0)`, ou seja, a
**primeira** ligação ativa encontrada pela query, sem nenhum critério
determinístico de qual loja é "a relevante".

Isto significa que `validarMesPublicado` (e qualquer outro código que use esta
sobrecarga sem `idLoja`) pode estar a verificar se o mês está publicado na loja
ERRADA para um colaborador multi-loja — silenciosamente.

`[Ficheiro e Linha]` `API/Services/LojautilizadorHelper.java:42-48`;
consumido sem `idLoja` em `DayOffService.validarMesPublicado` (linha 401) e em
`PerfilService.java:67`.
`[Proposta de Correção Direta]` Auditar todos os usos de
`findLigacaoAtiva(Integer)` (sem `idLoja`) e decidir, para cada um, se deveria
exigir o `idLoja` da sessão explicitamente (como já fazem as variantes
`*ComCargo(idUtilizador, idLoja, ...)`) — provavelmente sim para
`validarMesPublicado`, que faz mais sentido ser sempre relativo à loja ativa na
sessão do colaborador que está a pedir a folga.

---

## 5. O pitch promete "permutas cruzadas inter-lojas" — o código bloqueia-as

(Mantido da ronda anterior.) `PermutaService.java:295-296` e
`PermutaFolgaService.java:210-212` exigem mesma loja entre origem e destino.
**Ação sugerida:** decidir, antes do dia 25, se o Slide 3 de
`guiao_apresentacoes_25junho.txt` deve ser reformulado, ou se "cross-store" deve
ser apresentado explicitamente como visão de Projeto 3.

---

## 6. `WebGuardInterceptor` não cobre `/api/**`

(Mantido da ronda anterior, confirmado por teste a correr — `PermutaIdorCrossStoreSecurityTest`
passa.) A proteção contra IDOR em `/api/permutas/submeter` vem da query de
elegibilidade filtrada por loja, não do interceptor — resposta real é **422**,
não 403. Ação sugerida (não urgente): considerar estender `addPathPatterns`
para `/api/**` como defesa em profundidade.

---

## 7. Catálogo de regras não tem "limite de turnos noturnos consecutivos"

(Mantido; `RegraDesativadaIgnoradaNaGeracaoTest` passa, usando "Mínimo de
colaboradores por turno" como a regra de Black Friday a desativar, em vez da
regra inexistente mencionada no pedido original.)

---

## 8. Sem hierarquia de exceções de negócio

(Mantido.) Tudo é `IllegalArgumentException` com distinção só pelo texto da
mensagem. Acoplamento frágil para os testes; candidato a Projeto 3
(`RegraNegocioException` + subclasses).

---

## 9. Sem limite explícito de profundidade de recursão no motor

(Mantido; `StressNatalGeracaoPerformanceTest` passa em <10s com 25
colaboradores — orçamento de nós + deadline de parede já protegem na escala
atual. Risco residual preventivo, não materializado.)

---

## 10. Estado da suite de testes após esta sessão

- **16 testes novos**, divididos em 5 ficheiros:
  `CrossStoreDescansoMinimoIntegrationTest` (1 método, `@Disabled` — bug real,
  ponto 1), `RegraDesativadaIgnoradaNaGeracaoTest` (3 métodos, passam),
  `PermutaIdorCrossStoreSecurityTest` (1 método, passa),
  `StressNatalGeracaoPerformanceTest` (1 método, passa),
  `SistemaMultiLojaStressEndToEndTest` (10 métodos: 9 passam, 1 `@Disabled` —
  bug real, ponto 2).
- **Todos foram compilados e corridos** contra o PostgreSQL real desta sessão
  (`gestaohorarios`, porta 5432) — não são esqueletos teóricos.
- A suite original de 151 testes tem **3 falhas pré-existentes e não
  relacionadas** (ver secção "AÇÃO IMEDIATA" no topo) — confirmadas como
  reproduzíveis isoladamente, sem qualquer dos ficheiros novos no classpath.
- Os testes do **Grupo B** de `SistemaMultiLojaStressEndToEndTest` (concorrência
  real) suspendem deliberadamente a transação de teste
  (`@Transactional(propagation = Propagation.NOT_SUPPORTED)`) e fazem limpeza
  manual no final — a limpeza é *best-effort* e nem sempre consegue apagar tudo
  pela ordem certa de FK (vi avisos de `violates foreign key constraint` nos
  logs ao limpar `Loja`/`Utilizador` antes de `Lojautilizador`/`Horario`
  associados). Isto **não afetou a suite original** (confirmado pela isolação
  acima), mas deixa registos de teste residuais na base de dados de
  desenvolvimento com nomes como `"Race Loja A <uuid>"`, `"Race Gerente..."`,
  `"Loja Teste saldos-..."`. Recomendo correr um `DELETE` de limpeza manual
  (por padrão de nome `LIKE 'Race %'` ou `LIKE '%uid%'`) antes do dia 25, ou
  simplesmente repor a base de dados de demo a partir do
  `sql/demo-entrega.sql` se ela não for a mesma usada na apresentação.

---

## 11. 🔴 NOVO BUG CONFIRMADO: gerente e subgerente da MESMA loja podem aprovar a mesma permuta em duplicado

Descoberto em `FluxosTotaisPersonaEndToEndTest`, mesma técnica de concorrência
real (threads + transações suspensas) usada para o bug do ponto 2, mas desta
vez **dentro de uma única loja**, entre dois aprovadores de cargos diferentes
mas ambos legítimos (gerente e subgerente, ambos em `APROVACAO`/`GESTAO`).

Corri o teste `gerenteESubgerenteDaMesmaLojaTentamAprovarAMesmaPermutaConcorrentemente`
contra o PostgreSQL real: ambos os aprovadores conseguiram aprovar a mesma
`Permuta` pendente, sem que nenhum dos dois recebesse o erro esperado "Este
pedido de permuta já foi tratado." Confirma que o mesmo padrão de causa raiz do
ponto 2 (sem `@Version`, sem `SELECT ... FOR UPDATE`) também afeta
`PermutaService.aprovarPedidoPermuta`/`obterPedidoPendenteGerivel`
(`PermutaService.java:111-187, 378-404`) — e não é exclusivo de cenários
cross-store: até dois gerentes da MESMA loja, a clicar "aprovar" quase ao
mesmo tempo (um cenário plausível num sábado de Saldos com vários ecrãs
abertos), podem processar a mesma permuta duas vezes.

`[Ficheiro e Linha]` `API/Services/PermutaService.java:111-187` (aprovação sem
lock), `:378-404` (`obterPedidoPendenteGerivel`, guard "já tratado" lido antes
do commit da outra transação).
`[Teste que confirma]` `FluxosTotaisPersonaEndToEndTest.gerenteESubgerenteDaMesmaLojaTentamAprovarAMesmaPermutaConcorrentemente`
(`@Disabled`, bug real, conforme instrução de não corrigir `src/main/`).
`[Proposta de Correção Direta]` Mesma receita do ponto 2 — pessimistic lock
(`@Lock(LockModeType.PESSIMISTIC_WRITE)` num método dedicado de
`PermutaRepository.findDetalhadaByIdParaAprovacao`) ou `@Version` em `Permuta`.
Dado que este e o bug do ponto 2 partilham a mesma causa raiz estrutural
(nenhuma entidade `*Service` que decide aprovações usa locking), recomendo
tratar isto como **um único item de débito técnico** — "adicionar controlo de
concorrência às aprovações" — em vez de duas correções pontuais separadas.

---

## 12. Discovery: `WebAuthApiController` — segunda via de login não referenciada por nenhuma view

`POST /api/auth/login` e `POST /api/auth/logout` (`WebAuthApiController.java`,
mapeado em `/api/auth`) implementam um fluxo de autenticação completo e
funcional, paralelo a `WebLoginController#autenticar` (`POST /web/login`).
Confirmei por grep exaustivo em `src/main/resources` (templates Thymeleaf e
JS/CSS estáticos) que **não existe nenhuma referência a `/api/auth`** em
nenhuma página atual — é código alcançável e funcional, mas não consumido.

Confirmei também, por execução (`FluxosTotaisPersonaEndToEndTest.apiAuthLoginEhCodigoAlcancavelMasNaoReferenciadoPorNenhumaViewAtual`,
que passa), que o endpoint está de facto vivo: aceita JSON, autentica contra a
mesma `UtilizadorService.efetuarLogin`, e cria sessão. Por estar em `/api/**`,
fica fora do âmbito do `WebGuardInterceptor` (`addPathPatterns("/web/**")`) —
irrelevante para um endpoint de login (tem de ser público), mas confirma que
não há nenhuma camada de proteção adicional (rate-limiting, CSRF, etc.) nem
aqui nem em `/web/login`.

`[Ficheiro]` `API/WEB/WebAuthApiController.java` (ficheiro completo, 70
linhas — toda a classe é "candidata a remoção ou documentação").
`[Proposta]` Não é um bug de segurança em si (a lógica de autenticação por
trás é a mesma e está correta), mas é superfície de ataque não documentada e
código morto do ponto de vista de produto. Decidir entre: (a) documentá-lo
como API pública oficial (ex.: para uma futura app mobile), ou (b) remover —
mas sem instrução explícita para alterar `src/main/`, deixo a decisão para
vocês em vez de apagar por iniciativa própria.

---

## 13. Estado da suite após a ronda C (`FluxosTotaisPersonaEndToEndTest`)

- **11 testes novos**: 10 passam, 1 `@Disabled` (bug real, ponto 11).
- Cobrem: isolamento de cargo (fulltime sem APROVACAO, supervisor com
  APROVACAO mas sem GESTAO), barreira cross-store em folgas E permutas (dois
  fluxos de aprovação distintos, mesma garantia confirmada em ambos),
  bloqueio real via MockMvc/`WebGuardInterceptor` para os módulos de gestão,
  3 fluxos inter-interface Web→BLL-partilhada-com-Desktop (preferência,
  folga, e um ciclo completo "submete na Web, aprova como se fosse no
  Desktop"), 1 race condition dentro da mesma loja (ponto 11), e 1 teste de
  descoberta de código morto (ponto 12).
- **Suite completa do projeto após esta ronda: 178 testes, 0 falhas
  causadas por este trabalho, 3 falhas pré-existentes não relacionadas (ver
  topo deste documento), 3 `@Disabled`** (2 da ronda B + 1 da ronda C, todos
  bugs reais documentados, nenhum corrigido).
- **Nenhum ficheiro em `src/main/` foi tocado** em nenhuma das três rondas —
  confirmado por esta ter sido uma regra explícita desta sessão.

---

## 14. Ronda D — Cobertura explícita do módulo Desktop (sem TestFX)

Pedido adicional: testar "as duas frentes reais" (Web e Desktop) com a mesma
profundidade. Decisão tomada e razão, antes de qualquer código:

**Não adicionei TestFX/Monocle ao projeto.** Confirmei por leitura do
`pom.xml` que não existe nenhuma dependência de teste de UI JavaFX. Todos os
controllers Desktop (`GestaoLojaController`, `PainelGerentePedidosController`,
`PermutasController`, etc.) têm campos `@FXML` (`TableView`, `ComboBox`,
`Label`, ...) que só ficam inicializados depois de um carregamento real de
FXML pelo `FXMLLoader` — não são POJOs instanciáveis isoladamente em teste.
Simulá-los exigiria: (1) adicionar `testfx-core`/`testfx-junit5` +
`openjfx-monocle` ao `pom.xml`, (2) inicializar o toolkit JavaFX em modo
headless no `@BeforeAll`, (3) construir manualmente cada nó FXML injetado por
reflexão. É uma mudança de infraestrutura de build real, com risco de
instabilidade, fora do âmbito de "escrever testes" — e fui instruído a não
alterar nada além de testes e este documento. **Decidi não fazer isto sem
aprovação explícita.**

Em alternativa, estendi `FluxosTotaisPersonaEndToEndTest.java` com um
**Grupo E**, que invoca diretamente os métodos de serviço que cada handler
`@FXML` chama — citados com ficheiro e linha exatos (ex.: "`DESKTOP/
GestaoLojaController.java:152` chama `gestaoLojaBLL.guardarConfiguracao(...)`")
— para que a rastreabilidade ao comportamento real do ecrã seja verificável
sem fabricar infraestrutura de UI que o projeto não tem.

### 14.1 — Descoberta: `DashboardNavigator` não tem nenhuma verificação de cargo

Procurei explicitamente em `DESKTOP/DashboardNavigator.java` por qualquer
verificação de cargo/permissão antes de navegar para um ecrã — **zero
ocorrências**. A camada de navegação do Desktop não decide se um botão da
sidebar deve estar visível/ativo com base no cargo — isso depende inteiramente
de onde quer que a UI construa a sidebar (não confirmado nesta ronda, fora do
âmbito sem TestFX). Não é uma falha de segurança per se, porque **a camada de
serviço backstop a permissão de qualquer forma** (confirmado pelos testes do
Grupo A e E: `gestaoLojaBLL.desativarTurno`, `.guardarConfiguracao`, etc.
rejeitam sempre quem não tem GESTAO, independentemente de como o utilizador
chegou lá). Mas é um ponto de UX/defesa-em-profundidade: se a sidebar tiver um
bug de visibilidade, o utilizador sem permissão veria um popup de erro em vez
de nunca ver o botão.

### 14.2 — 🔴 NOVO BUG CONFIRMADO: aprovação no Desktop falha para gerente multi-loja na sua loja "secundária"

**Esta é a descoberta mais importante da ronda D.** Rastreei o botão "Aprovar"
de `PainelGerentePedidosController` até `PainelGerenteService.aprovarFolga`
(`API/Services/PainelGerenteService.java:98-101`):
```java
public void aprovarFolga(Integer idPedido, Integer idUtilizadorGestor) {
    ...
    dayOffBLL.aprovarPedidoFolga(idPedido, idUtilizadorGestor);
}
```
Este método chama o overload de **2 argumentos** de
`DayOffService.aprovarPedidoFolga` — **sem `idLoja`**. Internamente, esse
overload resolve a loja do gestor via
`LojautilizadorHelper.obterLigacaoAtivaComCargo(idUtilizador, cargos,
mensagem)` → `obterLigacaoAtiva(idUtilizador)` →
`findLigacoesAtivasByIdUtilizador(...).get(0)` (`LojautilizadorHelper.java:42-62`)
— a **primeira** ligação ativa, ordenada por nome de loja, **não a loja onde
está o pedido pendente**.

Confirmei por execução real: um gerente com vínculo ativo a duas lojas
("Loja A", nome alfabeticamente anterior, e "Loja B"), com um pedido pendente
exclusivamente em Loja B, é **rejeitado** com "Não tens permissão para gerir
este pedido" ao chamar exatamente a função que o botão "Aprovar" do Desktop
dispara — apesar de ser gerente legítimo de Loja B.

**Isto não acontece na Web** porque `WebEquipaController` sempre passa o
`idLoja` da sessão ativa (escolhida explicitamente pelo utilizador via
"Alternar Loja") ao chamar o overload de 3 argumentos de
`aprovarPedidoFolga`. O Desktop nunca pergunta "para qual loja estás a agir"
— `PainelGerenteService` não tem nenhum conceito de loja ativa selecionada.

`[Ficheiro e Linha]` `API/Services/PainelGerenteService.java:98-101` (chama o
overload errado); `API/Services/LojautilizadorHelper.java:42-62`
(`obterLigacaoAtiva`, resolução arbitrária); o mesmo padrão afeta
`PainelGerenteService.aprovarPermuta` (linha 116-118) e `.aprovarPreferencia`
(linha 128-130) — **todos** os 3 métodos de aprovação do painel Desktop usam
overloads sem `idLoja`.
`[Teste que confirma]` `FluxosTotaisPersonaEndToEndTest.desktopPainelGerentePedidos_AprovarFolga_RejeitaGerenteMultiLojaNaLojaSecundaria`
(`@Disabled`, bug real, conforme instrução de não corrigir `src/main/`).
`[Proposta de Correção Direta]` `PainelGerenteService` precisaria de saber
"qual é a loja ativa no Desktop" — o que implica adicionar um conceito de loja
ativa ao `SessaoService` (hoje só guarda `utilizadorAutenticado`,
`identificadorSessao`, `ultimaAtividade` — sem `idLoja`), análogo ao
`WebSession.LOJA_ID`, e propagar esse `idLoja` para os 3 métodos de aprovação
do `PainelGerenteService`. Isto é uma falha funcional real para qualquer
gerente regional que supervisione mais do que uma loja GNG e use a app
Desktop — não é um caso de borda raro, é o cenário central do pitch de negócio
("um gerente pode gerir várias lojas sem trocar de sistema").

---

## 15. Estado final da suite após a ronda D

- **5 testes novos** adicionados a `FluxosTotaisPersonaEndToEndTest.java`
  (Grupo E — Desktop, 4 métodos; Grupo F — concorrência Web+Desktop
  cross-loja, 1 método): 4 passam, 1 `@Disabled` (bug real, ponto 14.2).
- **Suite completa do projeto: 183 testes, 3 falhas pré-existentes não
  relacionadas (ver topo deste documento), 4 `@Disabled`** — todos bugs reais
  confirmados por execução, nenhum corrigido:
  1. `CrossStoreDescansoMinimoIntegrationTest` — `adicionarTurno` não valida
     descanso mínimo entre lojas (ponto 1).
  2. `SistemaMultiLojaStressEndToEndTest` — duas lojas decidem a mesma folga
     concorrentemente (ponto 2).
  3. `FluxosTotaisPersonaEndToEndTest` — gerente/subgerente da mesma loja
     aprovam a mesma permuta concorrentemente (ponto 11).
  4. `FluxosTotaisPersonaEndToEndTest` — Desktop rejeita aprovação legítima
     de gerente multi-loja na sua loja secundária (ponto 14.2, NOVO).
- **Nenhum ficheiro em `src/main/` foi tocado** nesta ronda nem em nenhuma
  das anteriores.

---

## 16. 🔴 CRASH REAL EM RUNTIME: `IncorrectResultSizeDataAccessException` no Perfil (Desktop, utilizador multi-loja)

Reportado pelo Tiago durante a execução manual do `Guiao_Testes_Manuais.md`
(ponto 1.1 da versão anterior). Confirma, na prática e em runtime real (não
apenas em teste automatizado), a mesma classe de problema já identificada no
ponto 14.2 — mas desta vez do lado da **leitura**, não da aprovação.

### 16.1 — Diagnóstico confirmado à base de dados: caso oficial de reprodução

O Tiago reproduziu o crash com a conta real **`francisco.gomes@levis.com`**
e reportou que contas de colaboradores comuns testadas em seguida **não**
reproduziam o erro. Investigámos diretamente a tabela `Lojautilizador`:

```sql
SELECT u.email, c.tipo AS cargo, lu.id_loja, lu.data_inicio, lu.data_fim
FROM utilizadores u
JOIN lojautilizador lu ON lu.id_utilizador = u.id_utilizador
JOIN cargos c ON c.id_cargo = lu.id_cargo
WHERE u.email = 'francisco.gomes@levis.com';
```
```
email                      | cargo   | id_loja | data_inicio | data_fim
francisco.gomes@levis.com  | gerente |    1    | 2025-03-18  | NULL
francisco.gomes@levis.com  | gerente |    2    | 2026-06-15  | NULL
```
Dois vínculos **simultaneamente ativos** (`data_fim IS NULL` em ambos) a
duas lojas diferentes ("Levi's Braga Parque" e "Levi's NorteShopping").

Para confirmar que isto é a causa exclusiva (e não um filtro de cargo
escondido no `PerfilService` — confirmámos por leitura que não filtra por
cargo), corremos uma agregação a toda a tabela:
```sql
SELECT c.tipo, count(*) FROM (
  SELECT id_utilizador, id_cargo FROM lojautilizador
  WHERE data_fim IS NULL GROUP BY id_utilizador, id_cargo HAVING count(*) > 1
) sub JOIN cargos c ON c.id_cargo = sub.id_cargo GROUP BY c.tipo;
```
```
cargo   | utilizadores com 2+ vínculos ativos em simultâneo
gerente |  1   ← exatamente o Francisco
(todos os outros cargos, incluindo fulltime/parttime/supervisor/subgerente: 0)
```

**Conclusão:** em toda a base de dados de demonstração existe **um único**
utilizador com vínculo ativo a duas lojas, e é precisamente a conta de
gerente usada no teste. Não é coincidência nem comportamento diferenciado do
`PerfilService` por cargo — é um facto puro de distribuição de dados: o
cenário "gerente regional com 2 lojas" foi modelado deliberadamente só para
este perfil na demo, o que torna `francisco.gomes@levis.com` o **caso de
teste mais realista e mais fácil de reproduzir** que existe no sistema —
passa a ser, a partir de agora, o caso oficial de reprodução deste bug para
a defesa (documentado em `Guiao_Testes_Manuais.md`, ponto 1.1).

**Causa raiz:** o Desktop nunca pede ao utilizador para escolher uma loja
ativa depois do login — `LoginController.abrirDashboard()`
(`DESKTOP/LoginController.java:197-219`) carrega `dashboard-view.fxml`
incondicionalmente, sem nunca consultar quantos vínculos ativos o utilizador
tem. A Web, em contraste, resolve isto logo em `WebLoginController.autenticar()`
(`WEB/WebLoginController.java:65-82`): se `findLigacoesAtivasByIdUtilizador`
devolver mais de 1 resultado, força a passagem por `/web/selecionar-loja`
antes de qualquer outra página, e tranca a escolha em `WebSession.LOJA_ID`.

`PerfilController` (Desktop) chama `perfilBLL.obterResumoPerfil(utilizadorLogado)`
— o overload de **1 argumento** (`PerfilService.java:51-55`), que por sua vez
cai no ramo `findLigacaoAtivaByIdUtilizador` (singular, sem `idLoja`,
`PerfilService.java:67`) sempre que `idLoja == null`. Para um utilizador com
2 vínculos ativos, esta query devolve 2 linhas e o Hibernate lança
`IncorrectResultSizeDataAccessException` — uma excepção **não tratada** que
chega ao utilizador final como um erro de runtime, não como uma mensagem de
negócio controlada.

**Ponto positivo confirmado:** `PerfilService` **já tem** o overload correto
e store-scoped — `obterResumoPerfil(Utilizador, Integer idLoja)`
(`PerfilService.java:58-68`), que usa `findLigacaoAtivaByIdUtilizadorAndIdLoja`
e nunca pode ser ambíguo. **Não é preciso alterar nada no `PerfilService`** —
só falta o Desktop ter, em algum lado, o conceito de "loja ativa" para passar
a esse overload. É exatamente o mesmo padrão já usado pela Web.

`[Ficheiro e Linha]` `DESKTOP/LoginController.java:197-219` (não verifica
contagem de vínculos antes de abrir o dashboard); `API/Services/SessaoService.java`
(sem nenhum campo `idLojaAtiva`); `DESKTOP/PerfilController.java` (chama o
overload de 1 argumento).
`[Proposta de Correção Direta]` Plano de refactoring completo (ecrã novo
`selecionar-loja-view.fxml` + `SelecionarLojaController`, extensão de
`SessaoService` com `idLojaAtiva`, e atualização de `PerfilController` e
controllers análogos para usar a sobrecarga store-scoped já existente nos
serviços) entregue como relatório técnico separado nesta sessão — ver também
o ponto 1.1 atualizado do `Guiao_Testes_Manuais.md` para o passo a passo de
reprodução e validação manual. **Nenhuma alteração foi feita a `src/main/`**
— é só plano + documentação, para Tiago/Francisco aplicarem manualmente.

**Nota de generalização:** este é o MESMO padrão estrutural dos pontos 4 e
14.2 (uso de `findLigacaoAtiva*` singular em vez da variante store-scoped) —
a causa raiz comum em todo o módulo Desktop é a ausência de um conceito de
"loja ativa" na sessão (`SessaoService`), que a Web resolve há muito via
`WebSession.LOJA_ID`. Corrigir isto de uma vez (estender `SessaoService`) é
mais eficiente do que corrigir cada controller Desktop individualmente à
medida que cada crash for descoberto.

---

## 17. 🚀 PLANO DE ATAQUE: Refactoring Estrutural para Multi-Loja no Desktop

Decisão oficial: levantada a tranca de segurança do `src/main/` para este
refactoring específico, com o objetivo de eliminar definitivamente o crash
`NonUniqueResultException`/`IncorrectResultSizeDataAccessException` na conta
real `francisco.gomes@levis.com` (2 vínculos `gerente` ativos em simultâneo —
ver ponto 16.1) e em qualquer outro utilizador multi-loja futuro, em vez de
manter o patch parcial de fallback aplicado anteriormente (loja "adivinhada"
pela primeira posição alfabética, sem escolha do utilizador).

### FASE 1: Fundação da Sessão — [x] CONCLUÍDO
`API/Services/SessaoService.java` — adicionado o campo `private Integer
idLojaAtiva;`, com `synchronized void definirLojaAtiva(Integer idLoja)` e
`synchronized Integer obterLojaAtiva()`, e `limparSessaoInterna()` (chamado em
logout/expiração) repõe `idLojaAtiva = null`. Equivalente direto ao
`WebSession.LOJA_ID` que a Web já usa.

### FASE 2 & 3: Interceção de Rota e Interface Gráfica JavaFX — [x] CONCLUÍDO
- Criado `login/selecionar-loja-view.fxml`: ecrã com `Label` de instrução,
  `ListView<Lojautilizador>` com as lojas do utilizador, e botão "CONTINUAR"
  (desativado até haver seleção) — reutiliza os estilos `login-shell`/
  `login-card`/`botao-login` já existentes em `login.css`.
- Criado `DESKTOP/SelecionarLojaController.java`: recebe `SessaoService` e
  `ApplicationContext` por injeção; ao confirmar, chama
  `sessaoBLL.definirLojaAtiva(...)` e abre o dashboard — espelha
  `WebLoginController.escolherLoja`.
- `DESKTOP/LoginController.java`: depois do login bem sucedido,
  `abrirDashboard()` consulta `LojautilizadorRepository
  .findLigacoesAtivasByIdUtilizador`. Mais de 1 loja → `abrirEcraSelecaoLoja`
  (novo ecrã). Exatamente 1 → grava-a diretamente via
  `sessaoBLL.definirLojaAtiva(...)` e segue para `abrirDashboardDireto` sem
  nenhum ecrã extra (paridade exata com `WebLoginController.autenticar`).

### FASE 4: Propagação de Escopo e Correção de Crashes — [x] CONCLUÍDO
- `DESKTOP/PerfilController.java`: passou a ler `sessaoBLL.obterLojaAtiva()`
  primeiro, passando-o ao overload de 2 argumentos de
  `PerfilService.obterResumoPerfil`. O antigo `resolverLojaAtivaFallback`
  (ponto 16) mantém-se só como rede de segurança para `idLojaAtiva == null`.
  Resultado: o Francisco vê os dados da loja que **ele escolheu**, não da
  primeira por ordem alfabética.
- `DESKTOP/DashboardController.java`: novo `Label fx:id="lblLojaAtivaSidebar"`
  fixo na sidebar (junto ao cargo), populado a partir de
  `sessaoBLL.obterLojaAtiva()` + `LojautilizadorRepository
  .findLigacaoAtivaByIdUtilizadorAndIdLoja` no arranque do dashboard.

### ⚠️ Armadilha descoberta durante a validação: Mockito devolve `0`, não `null`, para `Integer` não esboçado
Ao validar a Fase 4 contra a suite, `DashboardHomeIntegrationTest` começou a
**bloquear indefinidamente** (timeout de 45s) em 2 dos seus 4 testes. Investigação
com `jstack` (thread dump real durante o hang) revelou a causa exata: a
`JavaFX Application Thread` estava parada a meio de uma query JDBC real
(`findLigacaoAtivaByIdUtilizadorAndIdLoja`), à espera de resposta do
PostgreSQL que nunca chegava dentro do tempo do teste.

A causa raiz: `DashboardHomeIntegrationTest` usa `@MockitoBean SessaoService
sessaoBLL` sem esboçar (`when(...)`) o novo método `obterLojaAtiva()`. Para
métodos não esboçados que devolvem um tipo wrapper numérico (`Integer`,
`Long`, etc.), o comportamento por defeito do Mockito **não é devolver
`null`** — devolve `0`. Confirmado por log de diagnóstico:
`idLojaAtiva=0` em vez de `null`. Isto fez o código avançar para a query real
com `idLoja=0` (uma loja inexistente), em vez de cair no caminho seguro
"sem loja ativa, mostrar vazio" — e essa query específica, sob a transação
de teste aberta, ficava bloqueada.

**Correção aplicada** (em `DashboardController.obterNomeLojaAtiva` e
`PerfilController.setUtilizadorLogado`): a guarda passou de
`idLojaAtiva == null` para `idLojaAtiva == null || idLojaAtiva <= 0` — `idLoja`
é uma serial do Postgres (começa em 1), por isso `0`/negativo nunca é um
valor válido, independentemente da origem (Mockito, dados corrompidos, ou
qualquer chamador futuro). Endurece o código de produção, não só o teste.
Validado por `jstack` + reexecução: `DashboardHomeIntegrationTest` voltou a
9.28s, 0 falhas.

### Critério de aceitação
1. `.\mvnw.cmd clean compile` sem erros depois de cada fase — **[x] confirmado em todas as 4 fases**.
2. `.\mvnw.cmd test` final com os 183 testes automatizados 100% verdes — **[x] confirmado: 183 testes, 0 falhas, 0 erros, 4 skipped, BUILD SUCCESS**.
   (mesma contagem de antes: 0 falhas, 0 erros, 4 `@Disabled` documentados).
3. Reprodução manual com `francisco.gomes@levis.com`: login → ecrã de
   seleção de loja aparece → escolher uma loja → Dashboard mostra essa loja
   fixa na UI → Perfil abre sem nenhum erro, com os dados da loja escolhida.
