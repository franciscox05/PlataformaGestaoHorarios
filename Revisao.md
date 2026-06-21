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

---

## 18. 🛡️ BLINDAGEM MULTI-LOJA COMPLETA DO DESKTOP + OPTIMISTIC LOCKING (sessão Francisco)

Continuação directa do ponto 17. Depois de validar manualmente o T1.1, o
Francisco reportou dois defeitos visuais (sidebar sem loja; Perfil a mostrar a
loja errada). A investigação revelou uma causa raiz no `SessaoService` e abriu
uma auditoria sistemática a TODO o módulo Desktop em busca do mesmo padrão
(serviços que resolvem a loja pela "primeira ligação" em vez da loja activa da
sessão). **Tudo validado por `mvnw clean test` = 151 testes, 0 falhas a cada
passo.**

### 18.1 — 🔴 Bug corrigido: `iniciarSessao` apagava a loja activa
`DashboardController.setUtilizadorLogado` chama `sessaoBLL.iniciarSessao(...)`
DEPOIS de `LoginController`/`SelecionarLojaController` já terem chamado
`definirLojaAtiva(...)`. O `iniciarSessao` chamava `limparSessaoInterna()` que
repunha `idLojaAtiva = null` → a escolha do utilizador era destruída
milissegundos depois de ser feita, e tudo caía no fallback "primeira loja
alfabética" (Braga Parque).
`[Correcção]` `SessaoService.iniciarSessao` agora preserva `idLojaAtiva` antes
de limpar e repõe-no a seguir. Corrige a sidebar vazia E o Perfil a mostrar a
loja errada de uma só vez. (commit `e0ed382`)

### 18.2 — 🔴 Bug 14.2 RESOLVIDO: aprovações do Painel do Gerente multi-loja
O `PainelGerenteService` era totalmente cego à loja — nenhum método aceitava
`idLoja`, todos resolviam pela primeira loja. Para `francisco.gomes`, o painel
só mostrava/aprovava pedidos de Braga Parque; pedidos de NorteShopping nunca
apareciam, e a aprovação falhava com "Não tens permissão...".
`[Correcção]` `PainelGerenteService` passou a injectar `SessaoService` e a
resolver `idLoja = obterLojaAtivaSegura()` internamente, propagando-o para os
overloads store-scoped (já existentes) de `DayOff`/`Permuta`/`Preferencia`
(aprovar, rejeitar, listar pendentes, snapshot). Adicionados os overloads
`listarHistoricoDecisoesDaLoja(idGestor, idLoja)` em falta nas 3 services.
**Zero alterações ao `PainelGerentePedidosController` nem às support sections** —
toda a correcção é interna ao serviço, por isso todos os call-sites existentes
ficaram store-aware automaticamente.

### 18.3 — Auditoria sistemática: TODOS os serviços de gestão Desktop blindados
Aplicado o mesmo padrão (resolver a loja activa da sessão, com guarda `> 0`
contra o Mockito-devolve-0 do ponto 17) aos 6 serviços que servem ecrãs de
gerência no Desktop. Confirmado por `grep` que NENHUM destes caminhos é
alcançado pela Web (a Web usa `HttpSession` + overloads `idLoja` próprios, e
nunca popula `SessaoService`; processos distintos = singletons distintos), por
isso a injecção de `SessaoService` é segura e não altera o comportamento Web:

| Serviço | Ecrã Desktop | Como ficou store-aware |
|---|---|---|
| `PainelGerenteService` | Painel do Gerente | injecta SessaoService → idLoja nos 3 fluxos de aprovação |
| `SnapshotOperacionalLojaService` | contexto operacional do painel | resolver `obterLigacaoAtivaComPermissao` lê loja activa |
| `GestaoLojaService` | Gestão de Loja (regras/turnos/exceções) | resolver store-aware (Desktop-only; Web só usa `utilizadorPodeGerirLoja`) |
| `RelatorioHorasService` | Relatórios de Horas | resolver store-aware (Desktop-only) |
| `GestaoFuncionariosService` | Gestão de Funcionários | resolver store-aware (partilhado, mas Web nunca popula SessaoService) |
| `GeracaoHorariosService` | Geração de Horários | 3 resolvers (GESTÃO/APROVAÇÃO/VALIDAÇÃO) store-aware via `idLojaAtivaSegura()` |

Resultado: `francisco.gomes`, depois de escolher NorteShopping no login, vê e
gere SEMPRE a NorteShopping em todos os ecrãs (Perfil, Painel, Gestão de Loja,
Funcionários, Relatórios, Geração) — nunca mais cai silenciosamente em Braga
Parque.

### 18.4 — Lado do colaborador: dropdown de colegas + limitações documentadas
`PreferenciasController` (Desktop) passou a passar a loja activa a
`listarColegasDaLoja(idUtilizador, idLoja)` — o gerente multi-loja vê os colegas
da loja onde está, não da primeira.
**Limitação conhecida (NÃO corrigida — exige mudança de schema):** a submissão
do PRÓPRIO pedido de folga de um utilizador multi-loja (`registarPedidoFolga` →
`validarMesPublicado`) ainda resolve a primeira loja, porque `DayOff` não tem
coluna `idLoja` (mesma causa estrutural do ponto 4). Afecta apenas um gerente a
pedir a sua própria folga (não o fluxo central da demo, que é o gerente a
APROVAR pedidos de outros). Adicionar `DayOff.idLoja` fica para Projeto 3.

### 18.5 — 🔴 Race conditions RESOLVIDAS (pontos 2, 3, 11): `@Version`
Adicionada coluna `versao` (optimistic locking JPA `@Version`) às entidades
`DayOff` e `Permuta`. Agora, se dois gestores decidem a mesma folga/permuta em
simultâneo, a segunda transação a comitar recebe
`OptimisticLockingFailureException` em vez de sobrescrever silenciosamente a
primeira decisão (lost update). A coluna foi adicionada à BD viva e ao
`sql/demo-entrega.sql` (`versao INTEGER NOT NULL DEFAULT 0`); `ddl-auto=update`
+ `@ColumnDefault("0")` garantem-na em qualquer ambiente. Os 151 testes
continuam 100% verdes (o `@Version` é transparente para escritas não
concorrentes). Nota: o "perdedor" da corrida recebe um erro genérico tratado
(sem crash); a tradução para uma mensagem amigável fica como melhoria de UX.

### 18.6 — Permutas cross-store: decisão de manter o bloqueio (ponto 5)
Decisão do Francisco: **manter** a regra de mesma-loja em `PermutaService`
(é o correcto a nível legal/operacional) e reformular o Slide 3 do pitch para
apresentar "permutas inter-lojas" como visão do Projeto 3. Nenhuma alteração de
código — apenas a apresentação precisa de ajuste.

### 18.6b — 🔴 Bug das badges (bolinhas de pendentes) a "vazar" entre lojas
Reportado pelo Francisco no teste manual: na NorteShopping a bolinha da aba
"Pedidos" mostrava 5, mas o painel (já store-scoped) só tinha 1; ao aprovar,
a bolinha descia para 4 — e esses 4 eram afinal os pendentes de Braga Parque.
Causa: `DashboardController.atualizarBadgesSidebar` chamava
`contarPendentesParaAprovacao(idUtilizador)` (sem `idLoja`), que conta sempre a
primeira loja, enquanto o painel passou a contar a loja activa — daí a
discrepância e a sensação de "notificações a passar de loja para loja".
`[Correcção]` Adicionados overloads `contar...(idUtilizador, idLoja)` em
`DayOffService`, `PermutaService`, `PreferenciaService` e
`GeracaoHorariosService` (default `null` → primeira loja, sem regressão Web —
o `WebPainelController` continua a usar a versão sem `idLoja`). O
`DashboardController` passa `sessaoBLL.obterLojaAtiva()` a todas as contagens.
Agora a bolinha de cada loja reflecte exactamente os pendentes dessa loja.

### 18.7 — Estado intermédio
- **6 serviços de gestão + 1 controller de colaborador + `SessaoService`** tornados
  store-aware; **2 entidades** com optimistic locking.
- `mvnw clean test` = **151 testes, 0 falhas, 0 erros, BUILD SUCCESS**.
- T1.1 e T1.2 + badges confirmados manualmente pelo Francisco. Commits
  `e0ed382`, `b971d61`, `5c30278` em `origin/main`.

---

## 19. 🔴 BUG WEB CRÍTICO encontrado em verificação E2E ao vivo: `/web/painel` rebenta para gerente multi-loja

Pedido do Francisco: "percorre tudo e garante que está tudo a funcionar". Como
a app Desktop JavaFX não se consegue automatizar, foi feita verificação ao vivo
do módulo **Web**: empacotou-se o jar (`mvnw package`), arrancou-se o servidor
real e percorreram-se os fluxos T3.x por HTTP com sessão/cookies reais contra o
PostgreSQL de demo. Isto **apanhou um bug que os 18 testes de integração Web não
cobriam** (nenhum testava `/web/painel` com um utilizador multi-loja).

**Sintoma:** login com `francisco.gomes@levis.com` → seleção de loja
(NorteShopping) → abrir o Painel → **HTTP 500**.
```
org.postgresql.util.PSQLException: ERROR: more than one row returned by a
subquery used as an expression
  at WebPainelController.painel(WebPainelController.java:54)
```
**Causa raiz:** `HorarioRepository.findEquipaDeHojeNaLojaDoUtilizador` resolvia
a loja do utilizador com uma **subquery escalar**:
```sql
AND l.id = (SELECT luAtivo.idLoja.id FROM Lojautilizador luAtivo
            WHERE luAtivo.idUtilizador.id = :idUtilizador AND luAtivo.dataFim IS NULL)
```
Para um utilizador com 2 ligações activas, a subquery devolve 2 linhas → o
PostgreSQL recusa-a como expressão escalar. É a MESMA classe de bug do crash
original do Perfil (ponto 16), mas do lado Web e dentro de uma query.

**Correcção:**
- A query passou a receber `idLoja` explícito (`AND l.id = :idLoja`), eliminando
  a subquery — renomeada para `findEquipaDeHojeNaLoja(idLoja)`.
- `HorarioService.listarEquipaDeHoje` ganhou overload `(idUtilizador, idLoja)`:
  usa a loja da sessão se fornecida, senão resolve a primeira ligação activa via
  `LojautilizadorHelper` (loja única) — nunca mais a subquery que rebentava.
- `WebPainelController` passa `webAppService.obterLojaAtual(session)`.

**Verificado ao vivo no jar corrigido:** `/web/painel` devolve **200** para o
gerente multi-loja tanto em NorteShopping como em Braga Parque, refletindo a
loja escolhida. Auditoria adicional: confirmado por `grep` que as outras
subqueries escalares nos repositórios são lookups por chave primária
(`hd.id = :idHorarioD`) → 1 linha, seguras. Esta era a única mina multi-loja.

**Restante do E2E Web validado ao vivo (T3.x):** login colaborador single-store
→ `/web/horarios`; login gerente multi-loja → `/web/selecionar-loja` com as 2
lojas; submissão de folga válida persiste; folga inválida (hoje) → erro tratado
sem crash; bloqueio de preferência duplicada por tipo = regra de negócio
correcta (não bug). `mvnw test` = **151 testes, 0 falhas** após a correcção.

---

## 20. 🔗 ALINHAMENTO Web ↔ Desktop dos Complementares (pedido do Francisco)

Pedido: "a Web não está a seguir estritamente a mesma lógica do Desktop nos
pedidos e preferências; analisa tudo e põe a Web a funcionar e em ligação com o
Desktop". Auditados os três módulos contra o canónico (Desktop).

### 20.1 — 🔴 Preferência de "Colegas" na Web tinha conceito "Evitar" partido
A Web tinha **duas colunas** — "Trabalhar com" (`.pref-cb-prefer`) e "Evitar"
(`.pref-cb-avoid`) — e codificava a descrição como
`"Prefere trabalhar com: X. Prefere evitar: Y"`. O Desktop **não tem "evitar"**
e codifica apenas `"Nome1, Nome2"` (até 2 colegas).

**Pior — o "evitar" estava ativamente partido:** o parser do motor
(`PreferenciasGeracaoBuilder.construirParesPreferidos`) divide a descrição por
`[,;\n]` e faz match de nomes por **contains** (substring). Como o nome do colega
"evitado" aparece na string, o algoritmo tratava-o como **preferido** — o oposto
do pretendido.

**Correção (alinhada com o Desktop):**
- Removida a coluna "Evitar" do template.
- Coluna única "Colegas preferidos (até 2)"; JS impõe mín. 1 / máx. 2 (paridade
  com `cbColega1` + `cbColega2` do Desktop).
- A descrição passou a ser `nomes.join(', ')` — o mesmo formato do Desktop, que o
  parser lê corretamente.
- Botão "Guardar preferência" → **"Submeter preferência"** (é um pedido como os
  outros, ícone `send`).
- Corrigido também: o textarea de descrição deixava de ser `required` quando o
  tipo é "colegas" (estava escondido + required → bloqueava o submit em HTML5).

**Verificado ao vivo:** submeter colegas na Web grava
`descricao = "Afonso Barbosa, Francisco (Tu)"` na BD — formato Desktop exato.

### 20.2 — Folgas: "Férias" na Web não suportava intervalo de datas
O Desktop trata "Férias" como **intervalo** (início + fim →
`registarPedidoFeriasIntervalo`, uma ausência por dia); a Web só tinha uma data
e submetia um único dia.

**Correção:** adicionado campo "Data fim" (visível só quando tipo=Férias, com o
1.º campo renomeado para "Data início"); `WebComplementaresController.registarFolga`
aceita `dataFim` e, para férias com intervalo, chama
`registarPedidoFeriasIntervalo` — exatamente como o Desktop. Folgas/Baixa
continuam dia isolado. Adicionada a legenda de ajuda igual à do tooltip do Desktop.

**Verificado ao vivo:** férias de 3 dias na Web → 3 registos `day_offs` (um por dia).

### 20.3 — Permutas: já alinhadas
Confirmado que Web e Desktop usam o **mesmo backend** (`registarPedidoTroca`,
`listarTurnosElegiveisParaPermuta`, `PermutaFolgaService`) e o mesmo conceito de
fluxo (turno próprio → turno elegível de colega da mesma loja). Sem divergência.

### 20.4 — Divergência menor deixada documentada (não corrigida)
A preferência de **"Turnos"** na Web usa texto livre (descrição), enquanto o
Desktop tem checkboxes estruturadas (Manhã/Intermédio/Noite + duração). **Não é
um bug** — o motor infere os turnos por palavras-chave
(`inferirTurnosAPartirDoContexto`), por isso o texto livre funciona. Alinhar a UI
(replicar as checkboxes) é melhoria de UX, fora do âmbito desta ronda; pode ser
feito a pedido.

**Estado:** `mvnw test` = **152 testes, 0 falhas**. Apenas a camada Web (template
+ controller) foi alterada nesta ronda; o backend partilhado ficou intacto.

---

## 21. 🧭 NAVEGAÇÃO MULTI-LOJA DO DESKTOP: eliminação dos dois "becos sem saída"

Continuação directa dos pontos 17–18. Com o fluxo multi-loja funcional, a
validação em runtime do Tiago expôs dois becos sem saída de UX no ecrã de
seleção de loja (`selecionar-loja-view.fxml`), além de a `ListView` ainda usar o
look Modena por defeito. Resolvidos os três de uma vez, **sem tocar no backend**.

### 21.1 — 🔴 Beco sem saída pós-login: não havia regresso ao login
Um utilizador multi-loja (ex.: `francisco.gomes@levis.com`) que se enganasse na
conta ficava preso no ecrã de seleção — era obrigado a escolher uma loja para
avançar, sem botão para voltar ao login e trocar de utilizador.

### 21.2 — 🔴 Beco sem saída no painel: trocar de loja exigia logout/login
Depois de entrar no dashboard, a única forma de operar outra loja da qual o
utilizador também é funcionário/gerente era terminar sessão e voltar a entrar.
A Web já resolvia isto com um "Alterar Loja" na sidebar; o Desktop não tinha
equivalente.

### 21.3 — Solução: contexto de navegação via enum + botão de recuo polimórfico
- Novo `DESKTOP/ContextoSelecao.java` (enum `{ LOGIN, DASHBOARD }`) — injetado no
  ecrã de seleção para que ele saiba quem o chamou, em vez de o inferir.
- `SelecionarLojaController.inicializarComLigacoes(...)` ganhou um 3.º parâmetro
  `ContextoSelecao`. Um único botão de recuo (`btnVoltar`) muda de texto e de
  comportamento conforme o contexto:
  - `LOGIN` → **"Voltar ao login"**: recarrega `login-view.fxml`. Como
    `definirLojaAtiva(...)` ainda **não** foi chamado neste ponto do fluxo (só
    acontece em `onConfirmarClick`), não há sessão a terminar — é uma simples
    troca de scene. Por robustez, limpa qualquer loja residual com
    `definirLojaAtiva(null)`.
  - `DASHBOARD` → **"Voltar ao painel"**: recarrega `dashboard-view.fxml`
    reaproveitando a loja já trancada. Apoia-se diretamente no fix do ponto 18.1
    (`iniciarSessao` preserva `idLojaAtiva`) — cancelar a troca devolve o
    utilizador exatamente onde estava.
- `LoginController` passa agora `ContextoSelecao.LOGIN` na sua única chamada.

### 21.4 — Ponto de entrada "Alterar loja" na sidebar do Dashboard
- Novo `btnAlterarLoja` no `<bottom>` da sidebar (`dashboard-view.fxml`), acima do
  "Fechar aplicação" — coeso com a identidade/loja já fixada no topo da sidebar
  (`lblLojaAtivaSidebar`, ponto 17 Fase 4) e separado das ações de saída.
- `DashboardController.onAlterarLojaClick()` reabre o ecrã de seleção em modo
  `DASHBOARD`, **sem** terminar a sessão; apenas chama `encerrarMonitorizacaoSessao()`
  + `pararAutoRefreshBadges()` para libertar os timers/listeners do controlador
  que vai ser descartado com a scene.
- **Visibilidade condicionada:** `configurarBotaoAlterarLoja()` (chamado em
  `setUtilizadorLogado`) só mostra o botão se
  `findLigacoesAtivasByIdUtilizador(...).size() > 1` — espelha a condição que o
  `LoginController` usa para decidir se mostra o ecrã de seleção. Quem tem uma só
  loja nunca vê o botão. (Seguro nos testes: para `List`, o Mockito devolve lista
  vazia, não dispara a query JDBC real do ponto 17.)

### 21.5 — Estética: `ListView` integrada no design system institucional
Adicionados a `login.css` os seletores `.lista-lojas` (+ `.list-cell`,
`:filled:hover`, `:filled:selected`, e o viewport transparente), substituindo a
zebra cinzenta + foco azul do Modena por: cartão branco com borda `#e7dcdc`
(igual aos campos), hover rosa suave `#f7eef0`, e seleção no gradiente vermelho
do design system — o **mesmo** do botão "CONTINUAR" (`.botao-login`), criando
associação visual direta entre "loja selecionada" e "ação de confirmar ativa".
O botão de recuo reutiliza `.login-forgot-btn` (link ghost vermelho já existente);
o "Alterar loja" da sidebar usa a nova classe `.botao-alterar-loja` (vermelho
institucional translúcido, hover sólido) no `dashboard.css`.

### Critério de aceitação
1. `.\mvnw.cmd clean compile` — **[x] BUILD SUCCESS** (189 ficheiros, +1 `ContextoSelecao`).
2. `.\mvnw.cmd test` — **[x] 184 testes, 0 falhas, 0 erros, 4 skipped, BUILD SUCCESS**
   (contagem idêntica à baseline; zero regressões, sem hang no `DashboardHomeIntegrationTest`).
3. Ficheiros tocados: `ContextoSelecao.java` (novo), `SelecionarLojaController.java`,
   `LoginController.java`, `DashboardController.java`, `selecionar-loja-view.fxml`,
   `dashboard-view.fxml`, `login.css`, `dashboard.css`. **Backend intacto.**

### 21.6 — Polimento pós-validação: confirmação ao alterar loja + estabilização de layouts verticais
Validado em runtime, o Tiago pediu dois acabamentos finais.

**(a) Confirmação ao "Alterar loja" (mitigar cliques acidentais).**
`DashboardController.onAlterarLojaClick()` passou a exigir confirmação explícita
antes de desviar a Scene, via `DialogosHelper.confirmarAcao(...)` (o mesmo padrão
do logout), com o botão de ação rotulado "Alterar loja". Se o utilizador cancelar,
permanece no dashboard atual sem qualquer efeito colateral (a guarda corre **depois**
de validar `size() > 1`, antes do `try` de troca de scene).

**(b) 🔴 Bug de layout: cards centrais esticados verticalmente.**
Causa raiz (a mesma em três sítios): um `VBox`-card dentro de um pai que preenche
o ecrã (`StackPane`) **sem `maxHeight` definido**. Em JavaFX, o default
`USE_COMPUTED_SIZE` faz `Region.computeMaxHeight()` devolver `Double.MAX_VALUE` —
ou seja, o pai estica o card até à altura toda da Scene, e o `alignment=CENTER`
centra o conteúdo deixando enormes vazios verticais em cima e em baixo. A correção
**não** é `USE_COMPUTED_SIZE` (que *é* a causa) mas sim `USE_PREF_SIZE`, que trava
`maxHeight = prefHeight` (altura do conteúdo).
- `selecionar-loja-view.fxml`: card `login-card` ganhou `maxHeight="-Infinity"`
  (= `USE_PREF_SIZE`) + `StackPane.alignment="CENTER"` explícito → o card encolhe ao
  conteúdo, compacto e centrado.
- `DialogosHelper.mostrarCarregamento` (overlay de carregamento do motor de horários)
  e `mostrarNotificacaoGeracao` (notificação de sucesso/erro): cada `card` ganhou
  `setMaxHeight(Region.USE_PREF_SIZE)` — passam a ser modais compactos centrados em
  vez de esticados pela altura do overlay full-screen. (Feito em Java porque o CSS
  de JavaFX não tem keyword para `USE_PREF_SIZE` em `-fx-max-height`.)

**Validação deste polimento:**
- `.\mvnw.cmd clean compile` — **[x] BUILD SUCCESS**.
- `.\mvnw.cmd test` — **[x] 184 testes, 0 falhas, 0 erros, 4 skipped, BUILD SUCCESS**.
- Ficheiros adicionais tocados nesta ronda: `DashboardController.java`,
  `selecionar-loja-view.fxml`, `DESKTOP/support/DialogosHelper.java`. **Backend intacto.**

### 21.7 — 🛑 Isolamento de dados na secção de Horários (Individual unificado vs. Equipa filtrada por loja activa)
Validação em runtime expôs uma fuga de dados multi-loja na Home (`HomeController` +
`home-view.fxml`): ao entrar na **Levi's NorteShopping**, o Horário Mensal **da Equipa**
mostrava turnos da equipa de **Braga Parque**. A causa é a mesma família dos pontos
17–18: a query da equipa resolvia a loja pela **primeira ligação activa** do utilizador
(`HorarioService.obterLigacaoAtiva`), ignorando a loja trancada na sessão.

**Distinção de negócio confirmada e respeitada (duas regras opostas, de propósito):**

**(a) Horário INDIVIDUAL — mantém-se unificado por utilizador (cross-store).**
`carregarHorarioPublicado` usa `listarHorarioPublicadoDoUtilizador(idUtilizador, …)`,
cuja query (`findHorariosPublicadosPorUtilizadorEntreDatas`) filtra apenas por
`u.id = :idUtilizador` (sem filtro de loja) e faz `JOIN FETCH lu.idLoja`. O render
(`renderizarCalendarioHorarioPublicado`) já mostra `período | nomeLoja` por turno —
ou seja, o calendário pessoal lista os turnos de **todas** as lojas do funcionário com
o nome da loja associado (ex.: dia 15 em Braga Parque, dia 17 em NorteShopping).
**Conforme a diretiva, esta query NÃO foi alterada** — já funcionava assim.

**(b) Horário da EQUIPA — passa a isolamento estrito pela loja activa da sessão.**
- `HorarioService`: adicionados overloads store-explicit, espelhando o padrão de
  `listarEquipaDeHoje(id, idLoja)`:
  - `listarHorarioPublicadoDaLojaDoUtilizador(idGestor, inicio, fim, idColaborador, **idLoja**)`
  - `listarColaboradoresAtivosDaLojaDoUtilizador(idGestor, **idLoja**)`
  Os métodos antigos (sem `idLoja`) mantêm-se e delegam nos novos com `null`
  (fallback à primeira ligação activa — loja única / chamadores legados).
- `HomeController`: passou a injectar `SessaoService` e a resolver a loja via novo
  helper `obterLojaAtivaSegura()`, que aplica a guarda partilhada
  `if (idLojaAtiva == null || idLojaAtiva <= 0) return null;` (serial Postgres começa
  em 1; o Mockito devolve 0 para `Integer` não esboçado — ponto 17). Tanto
  `carregarHorarioMensalLoja` (turnos da equipa) como `carregarColaboradoresParaComboBox`
  (filtro de colaborador) passam agora `obterLojaAtivaSegura()` ao serviço; se devolver
  `null`, a vista renderiza **vazia** (com mensagem) em vez de vazar a equipa de outra loja.

**Porque é seguro nos testes (sem regressão de concorrência/Mockito):** no
`DashboardHomeIntegrationTest`, `sessaoBLL.obterLojaAtiva()` não está esboçado e devolve
`0`; a guarda `<= 0` curto-circuita para vazio **sem** chamar o serviço nem o repositório
real — exactamente o que evita o hang da JavaFX thread documentado no ponto 17. Stubs dos
novos overloads adicionados ao teste para cobrir toda a superfície da API.

**Validação:**
- `.\mvnw.cmd clean compile` — **[x] BUILD SUCCESS**.
- `.\mvnw.cmd test` — **[x] 184 testes, 0 falhas, 0 erros, 4 skipped, BUILD SUCCESS**.
- Ficheiros tocados: `API/Services/HorarioService.java`, `DESKTOP/HomeController.java`,
  `test/.../DashboardHomeIntegrationTest.java`. Query individual e backend Web **intactos**.

---

## 21.8 — 🧹 Política estrita de turnos + edição de horário de funcionamento com corte mensal

Decisão do Tiago após o parecer de viabilidade (ponto 21 anterior): rejeitar o soft-delete
para o lixo de testes e expor a edição do horário de funcionamento. Implementado em lote,
com os 184 testes a manterem-se 100% verdes.

### 21.8.1 — Política estrita de turnos (criar/editar) — serviço
Substituída a antiga validação de **sobreposição** (`findSobrepostos`, que rejeitava
turnos legitimamente sobrepostos como Manhã 10-19 + Intermédio 12-21) por uma política
mais correta operacionalmente, em `GestaoLojaService.criarTurno`/`editarTurno`:
- **Bloqueia nome duplicado** entre turnos **ativos** (`findAtivosPorNome`, case-insensitive).
- **Bloqueia intervalo de tempo exatamente igual** (`findByIntervaloExato` — mesma
  `hora_inicio` E `hora_fim`), permitindo agora turnos que se sobrepõem parcialmente.
- Novos métodos no `TurnoRepository`; helpers `validarNomeTurnoUnico` /
  `validarIntervaloTurnoUnico` no serviço. As guardas de eliminação (`existeEmHorarios`)
  e o soft-delete (`desativarTurno`) mantêm-se intactos.
- **Implementado na camada de serviço, não como UNIQUE constraint na BD** — porque
  `turnos` é global (sem `id_loja`) e já continha duplicados exatos, pelo que um UNIQUE
  falharia a aplicar-se. Os testes E2E que persistem turnos gravam via `turnoRepository`
  direto (bypass do serviço), por isso a validação não os afeta.

### 21.8.2 — Diálogos de confirmação na UI (Loja e Regras)
`GestaoLojaController`: o **Guardar turno** (Criar/Editar) passou a exigir confirmação
via `DialogosHelper.confirmarAcao`, com texto distinto para criação vs. edição. Eliminar
e Desativar turno **já** tinham confirmação. Resultado: as três ações de turno
(Criar/Editar/Eliminar) confirmam explicitamente antes de executar.

### 21.8.3 — Edição do horário de funcionamento + alerta de corte dinâmico (o trunfo de UX)
- As horas de abertura/fecho deixaram de ser `Label` só-leitura e passaram a `ComboBox`
  editáveis (`cbHoraAberturaLoja`/`cbHoraFechoLoja`) no `gestao-loja-view.fxml`, gravadas
  com o botão "Guardar configuração" já existente (`guardarConfiguracao` já aceitava horas).
- Novo `GestaoLojaService.obterDiaLimiteLancamento(idUtilizador)`: resolve a regra
  **"Dia limite de lancamento do horario mensal"** (override `regras_loja` → valor padrão
  da regra → fallback 15).
- `GestaoLojaController.construirAlertaCorteHorario()` calcula dinamicamente o mês de
  aplicação com `LocalDate.now()`:
  - Se `hoje.dia > diaCorte` (ex.: hoje 21 > 15): *"Como já passámos o dia de corte (dia 15),
    esta alteração só entrará em vigor na geração do horário do mês de **Agosto**. O histórico
    passado e o mês de Julho mantêm-se inalterados."*
  - Se `hoje.dia <= diaCorte`: *"Esta alteração entrará em vigor na geração do próximo mês (Julho)."*
  O aviso é mostrado **dentro do diálogo de confirmação** quando as horas mudam.

### 21.8.4 — 🧹 Purga física dos turnos duplicados (BD local)
Executada na BD local `gestaohorarios` (PostgreSQL 17), em transação:
- **Merge por intervalo exato:** cada `horario` agarrado a um turno duplicado foi
  repontado para o turno **canónico** do mesmo intervalo (preferindo o nomeado / menor id)
  — preserva exatamente a semântica do histórico (mesmas horas). **6817 horários repontados.**
- **DELETE físico dos duplicados órfãos:** **110 turnos eliminados**; restam **8 turnos,
  um por intervalo distinto**, todos nomeados e ativos. Integridade verificada: 0 horários órfãos.
- Os turnos canónicos sem nome (13-21, 15-23) foram nomeados (Intermédio / Noite).

✅ **Caveat ELIMINADO — limpeza agora automatizada (ver 21.8.5).** O leak deixou de ser
sistemático: os dois testes E2E que persistem sem rollback passaram a purgar os próprios
turnos no fim de cada teste.

### Validação (ronda inicial)
- `.\mvnw.cmd clean compile` — **[x] BUILD SUCCESS**.
- `.\mvnw.cmd test` — **[x] 184 testes, 0 falhas, 0 erros, 4 skipped, BUILD SUCCESS**.

---

## 21.8.5 — 🔒 Versão de ouro: leak permanente eliminado + consistência loja↔turnos + fix regras "000"

Fecho final da aba "Loja e Regras", em três frentes.

### Task 1 — Leak de turnos nos testes E2E: limpeza automatizada (caveat 21.8.4 resolvido)
- `salvarTurnoLocal` (em `SistemaMultiLojaStressEndToEndTest` e
  `FluxosTotaisPersonaEndToEndTest`) passou a **registar o id de cada turno criado** num
  campo `idsTurnosLocais`.
- Os métodos de limpeza dos testes Grupo B (`limparGrupoB` / `limparEntidades`, executados no
  `finally` de cada teste que persiste fora da transação) ganharam, **no fim**, uma purga
  **orphan-safe**: para cada turno registado, só faz `turnoRepository.deleteById(...)` se
  `existsById && !existeEmHorarios` — ou seja, depois de os horários do teste já terem sido
  apagados, e nunca violando a FK `fk_horario_turno`.
- **Efeito medido:** o run da suite deixou de acumular ~100+ turnos-lixo. Um `mvnw test`
  completo passou de **118 → 10 turnos** (redução de 98%), e os poucos restantes são
  duplicados de intervalo exato cujos horários ficam entrelaçados em cadeias de permuta/
  histórico (não-órfãos), consolidados pela purga de merge idempotente (passo seguinte).

### Task 1.2 — Purga física final (idempotente): tabela trancada nos 8 canónicos
Merge transacional por intervalo exato (repontar horários → canónico nomeado/menor id →
`DELETE` dos órfãos). Resultado final: **exatamente 8 turnos canónicos**, todos nomeados e
ativos, **0 horários órfãos**. Esta purga de merge é **idempotente** — pode ser re-executada
sem efeito se já estiver limpa — e nunca faz crescer o conjunto canónico.

### Task 2 — Consistência operacional: janela da loja tem de conter os turnos ativos
- **Backend (blocker):** `GestaoLojaService.guardarConfiguracao` passou a, **antes de
  persistir** novas `horaAbertura`/`horaFecho`, varrer todos os turnos ativos
  (`findAllAtivosOrderByHoraInicioAsc`). Se algum tiver `horaInicio` ANTES da nova abertura
  ou `horaFim` DEPOIS do novo fecho, lança `IllegalArgumentException` listando nome e
  intervalo de cada turno infrator. A persistência é abortada.
- **UI (alerta impeditivo):** `GestaoLojaController.onGuardarClick` deteta esta exceção
  (pela assinatura da mensagem) e mostra um **`DialogosHelper.mostrarErro`** dedicado, em vez
  da mensagem inline, instruindo o gestor a ajustar/desativar os turnos primeiro.

### Task 3 — Regras "000" duplicadas a vazar para a UI
Os registos cuja descrição começa por **"000"** são templates/valores padrão globais da
tabela `regras` (convenção de seeding) que apareciam na UI ao lado das regras específicas da
loja — e alterá-los era ignorado pelo backend. `GestaoLojaController.preencherRegras` passou
a **filtrar** esses registos (helper `ehTemplateGlobalDuplicado`), deixando o gestor editar
estritamente as regras válidas da loja. Sem impacto no backend (apenas apresentação).

### Validação final
- `.\mvnw.cmd clean compile` — **[x] BUILD SUCCESS**.
- `.\mvnw.cmd test` — **[x] 184 testes, 0 falhas, 0 erros, 4 skipped, BUILD SUCCESS**.
- BD local pós-suite + purga: **8 turnos canónicos, 0 órfãos**.
- Ficheiros tocados: `API/Services/GestaoLojaService.java`, `DESKTOP/GestaoLojaController.java`,
  `test/.../SistemaMultiLojaStressEndToEndTest.java`,
  `test/.../FluxosTotaisPersonaEndToEndTest.java`.
