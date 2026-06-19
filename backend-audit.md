# Backend Audit — PlataformaGestaoHorarios
**Date:** 2026-06-13  
**Scope:** WEB controllers, API Services, Repositories  
**Authors:** Claude Code (automated audit)  
**Status:** Read-only — no source code was modified

---

## Executive Summary

The codebase is generally well-structured: constructor injection throughout, `@Transactional(readOnly = true)` on queries, JPQL with `JOIN FETCH` to prevent the classic N+1 pattern, and a clean three-layer (WEB → Service → Repository) separation. The issues below are real but not architectural emergencies. Priority is assigned as **High / Medium / Low**.

---

## 1. Performance Bottlenecks

### 1.1 [HIGH] `WebPainelController` uses full-list fetches to get counts

**File:** `WEB/WebPainelController.java` — lines 68–75

```java
// CURRENT — loads every pending record into the JVM just to call .size()
long folgasParaAprovar = permissoes.podeAprovarFolgas()
        ? dayOffBLL.listarPedidosPendentesParaAprovacao(utilizadorId).size()
        : 0;
long preferenciasParaAprovar = permissoes.podeAprovarPreferencias()
        ? preferenciaBLL.listarPreferenciasPendentesParaAprovacao(utilizadorId).size()
        : 0;
long permutasParaAprovar = permissoes.podeAprovarPermutas()
        ? permutaBLL.listarPedidosPendentesParaAprovacao(utilizadorId).size()
        : 0;
```

Each service already exposes a `contarPendentesParaAprovacao()` method backed by a `COUNT` query. The dashboard never renders the individual records — it only shows the badge numbers.

**Proposed fix:**
```java
// PROPOSED — single COUNT query per category, no list hydration
long folgasParaAprovar = permissoes.podeAprovarFolgas()
        ? dayOffBLL.contarPendentesParaAprovacao(utilizadorId)
        : 0;
long preferenciasParaAprovar = permissoes.podeAprovarPreferencias()
        ? preferenciaBLL.contarPendentesParaAprovacao(utilizadorId)
        : 0;
long permutasParaAprovar = permissoes.podeAprovarPermutas()
        ? permutaBLL.contarPendentesParaAprovacao(utilizadorId)
        : 0;
```

---

### 1.2 [HIGH] `WebAppService.obterPermissoes()` fires 5+ independent `lojautilizador` queries per page render

**File:** `WEB/WebAppService.java` — lines 86–102  
**Called by:** `preencherModeloBase()`, which is the first call in every single controller handler.

```java
// CURRENT — each line below triggers a separate DB round-trip to lojautilizador
return new WebPermissoes(
        true,
        gestaoLojaBLL.utilizadorPodeGerirLoja(idUtilizador),         // → DB
        relatorioHorasBLL.utilizadorPodeConsultarRelatorios(...),     // → DB
        true,
        dayOffBLL.utilizadorPodeAprovarFolgas(idUtilizador),         // → DB
        preferenciaBLL.utilizadorPodeAprovarPreferencias(...),       // → DB
        permutaBLL.utilizadorPodeAprovarPermutas(idUtilizador),      // → DB
        geracaoHorariosBLL.utilizadorPodeValidarHorarios(...),       // → DB
        geracaoHorariosBLL.utilizadorPodeGerarHorarios(...)          // → DB
);
```

Additionally, `obterCargoAtual()` (also called in `preencherModeloBase()`) issues a separate `findLigacaoAtivaByIdUtilizador` query. Total: **6–8 SQL hits to the same `lojautilizador` table per page load**.

All permission checks ultimately reduce to: fetch the active `Lojautilizador` → inspect the `cargo.tipo`. The single record can be fetched once and all flags computed in Java.

**Proposed fix:**
```java
// PROPOSED — single DB query, all flags computed in-memory
public WebPermissoes obterPermissoes(Integer idUtilizador) {
    if (idUtilizador == null) return WebPermissoes.semAcesso();

    Optional<Lojautilizador> opt =
            lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador);
    if (opt.isEmpty()) return WebPermissoes.semAcesso();

    Lojautilizador lu = opt.get();
    String cargo = lu.getIdCargo() != null
            ? lu.getIdCargo().getTipo().toLowerCase()
            : "";

    boolean gerente    = cargo.equals("gerente") || cargo.equals("subgerente");
    boolean supervisor = cargo.equals("supervisor");
    boolean aprovacao  = gerente || supervisor;

    return new WebPermissoes(
            true, gerente, gerente, true,
            aprovacao, gerente, aprovacao, supervisor, gerente
    );
}
```

> **Note:** This assumes `cargo.tipo` values match the constants in `LojautilizadorHelper`. Verify against `APROVACAO`, `GESTAO`, and `VALIDACAO` sets before applying.

---

### 1.3 [HIGH] `PreferenciaService.existePreferenciaDuplicada()` ignores the dedicated repository query

**File:** `API/Services/PreferenciaService.java` — lines 342–358  
**Repository method available:** `PreferenciaRepository.existsPreferenciaDuplicada()`

```java
// CURRENT — loads ALL preferences into memory, filters in Java
private boolean existePreferenciaDuplicada(...) {
    return preferenciaRepository
            .findByIdUtilizadorIdOrderByDataInicioAscIdDesc(idUtilizador)
            .stream()
            .filter(p -> idIgnorado == null || !Objects.equals(p.getId(), idIgnorado))
            .anyMatch(p -> equalsIgnoreCase(p.getTipo(), tipo) && ...);
}
```

The repository already has an `existsPreferenciaDuplicada()` JPQL query that does this entirely in the database.

**Proposed fix:**
```java
// PROPOSED — single EXISTS query in DB, no list hydration
private boolean existePreferenciaDuplicada(Integer idUtilizador, String tipo,
        String descricao, Integer prioridade,
        LocalDate dataInicio, LocalDate dataFim, Integer idIgnorado) {
    return preferenciaRepository.existsPreferenciaDuplicada(
            idUtilizador, tipo, descricao, prioridade, dataInicio, dataFim, idIgnorado);
}
```

---

### 1.4 [MEDIUM] `DayOffService.atualizarEstadoPedido()` re-fetches the full pending list to verify visibility

**File:** `API/Services/DayOffService.java` — lines 196–204

```java
// CURRENT — loads every pending DayOff for the loja just to check one
boolean pedidoVisivelAoAprovador = dayOffRepository
        .findPedidosPendentesDaLoja(ligacaoAtiva.getIdLoja().getId(), idUtilizadorAprovador)
        .stream()
        .anyMatch(d -> d.getIdDayoff().equals(idDayOff));
```

`DayOffRepository` already has `findPedidoDaLojaById(idLoja, idDayOff)` which returns exactly one record.

**Proposed fix:**
```java
// PROPOSED — single targeted query
boolean pedidoVisivelAoAprovador = dayOffRepository
        .findPedidoDaLojaById(ligacaoAtiva.getIdLoja().getId(), idDayOff)
        .isPresent();
```

---

### 1.5 [MEDIUM] `WebComplementaresController.registarPermuta()` double-fetches shifts for validation

**File:** `WEB/WebComplementaresController.java` — lines 274–284

```java
// CURRENT — fetches the full available-for-swap list of the logged-in user,
// then fetches the full eligible-for-swap list, and streams both to find one record
List<Horario> meusTurnos = horarioBLL.listarMeusTurnosDisponiveisParaPermuta(utilizadorId);
Horario turnoOrigem = meusTurnos.stream()
        .filter(item -> item.getId().equals(idHorarioOrigem))
        .findFirst()
        .orElseThrow(...);

List<Horario> turnosElegiveis = horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, idHorarioOrigem);
Horario turnoDestino = turnosElegiveis.stream()
        .filter(item -> item.getId().equals(idHorarioDestino))
        .findFirst()
        .orElseThrow(...);
```

`listarTurnosElegiveisParaPermuta()` already enforces that the origin shift belongs to the user's loja and is not in a pending permuta. The ownership check on `turnoOrigem` is defensive duplication. The service method `registarPedidoTroca()` validates ownership again internally.

**Proposed fix:**
```java
// PROPOSED — load only the eligible list for the specific pair, let the service validate ownership
List<Horario> turnosElegiveis = horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, idHorarioOrigem);
Horario turnoDestino = turnosElegiveis.stream()
        .filter(item -> item.getId() != null && item.getId().equals(idHorarioDestino))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("O turno de destino selecionado nao e elegivel."));

// Origin shift: single DB lookup by ID (add a HorarioService.findById or use the existing repo)
Horario turnoOrigem = horarioBLL.obterHorarioPorId(idHorarioOrigem)
        .orElseThrow(() -> new IllegalArgumentException("O turno de origem nao foi encontrado."));

permutaBLL.registarPedidoTroca(utilizadorId, turnoOrigem, turnoDestino);
```

> This requires adding a thin `obterHorarioPorId(Integer)` method to `HorarioService`.

---

### 1.6 [MEDIUM] `PreferenciaService.listarColegasDaLoja()` over-filters in memory

**File:** `API/Services/PreferenciaService.java` — lines 96–111

```java
// CURRENT — fetches ALL lojautilizadors for the store, applies 4 filters in Java
return lojautilizadorRepository
        .findByIdLojaWithUtilizadorCargo(ligacaoAtiva.getIdLoja().getId())
        .stream()
        .filter(l -> l.getIdUtilizador() != null && l.getIdUtilizador().getId() != null)
        .filter(l -> !Objects.equals(l.getIdUtilizador().getId(), idUtilizador))
        .filter(l -> l.getDataFim() == null)
        .filter(l -> EstadoUtilizador.ativo == l.getIdUtilizador().getEstado())
        ...
```

The `dataFim IS NULL` and `estado = ativo` filters should be pushed into the repository query to reduce the result set before it reaches the JVM.

**Proposed fix (repository addition):**
```java
// In LojautilizadorRepository
@Query("SELECT lu FROM Lojautilizador lu " +
       "JOIN FETCH lu.idUtilizador u " +
       "JOIN FETCH lu.idCargo c " +
       "WHERE lu.idLoja.id = :idLoja " +
       "AND lu.dataFim IS NULL " +
       "AND u.id <> :idExcluir " +
       "AND u.estado = com.example.projeto2.API.Enums.EstadoUtilizador.ativo " +
       "ORDER BY u.nome ASC")
List<Lojautilizador> findActivosNaLoja(
        @Param("idLoja") Integer idLoja,
        @Param("idExcluir") Integer idExcluir);
```

---

### 1.7 [LOW] `HorarioRepository.findTurnosDosColegas()` has no store scope

**File:** `API/Repositories/HorarioRepository.java` — lines 71–80

```java
// WHERE u.id != :meuId AND h.dataTurno >= CURRENT_DATE
// — no loja filter: can return shifts from other stores
```

If a user ever transfers stores (or if `Lojautilizador` has multiple active rows), this query returns schedules from all stores. A correlated sub-query matching the approver's current store (like `findEquipaDeHojeNaLojaDoUtilizador` uses) should be added.

---

## 2. Missing Edge-Case Validations

### 2.1 [HIGH] `WebHorariosController.exportarPdf()` has no exception handling

**File:** `WEB/WebHorariosController.java` — lines 125–140

Both `geracaoHorariosBLL.obterMeusHorarios()` and `webPdfService.gerarHorarioMensalPdf()` can throw `IllegalArgumentException` (or any runtime exception if PDF generation fails). There is no try/catch, so the user receives an unformatted 500 error page instead of a friendly redirect.

**Proposed fix:**
```java
@GetMapping(value = "/exportar.pdf", produces = "application/pdf")
public Object exportarPdf(@RequestParam("ano") Integer ano,
                          @RequestParam("mes") Integer mes,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
    Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
    try {
        List<Horario> turnos = geracaoHorariosBLL.obterMeusHorarios(utilizadorId, ano, mes);
        String nome = (String) session.getAttribute(WebSession.UTILIZADOR_NOME);
        byte[] conteudo = webPdfService.gerarHorarioMensalPdf(
                turnos, ano, mes, nome != null ? nome : "");
        String ficheiro = "horario-" + ano + "-" + String.format("%02d", mes) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ficheiro + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(conteudo);
    } catch (IllegalArgumentException ex) {
        redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        return "redirect:/web/horarios?ano=" + ano + "&mes=" + mes;
    }
}
```

> The return type changes to `Object` (or a common supertype) because of the redirect branch; alternatively, use `ResponseEntity<?>`.

---

### 2.2 [MEDIUM] `DayOffService.aprovarPedidoFolgaComCobertura()` swallows all exceptions silently

**File:** `API/Services/DayOffService.java` — lines 302–328

```java
} catch (Exception e) {
    // Falha na análise de cobertura não deve impedir a aprovação
    aviso = "Não foi possível calcular o impacto de cobertura.";
}
```

A broad `catch (Exception e)` with no logging means any `NullPointerException` or repository failure in the coverage block is invisible. The intention (don't block the approval) is correct, but silent failure makes debugging very hard.

**Proposed fix:**
```java
} catch (Exception e) {
    // Coverage analysis failure must not block the approval,
    // but log it so it doesn't disappear silently
    log.warn("Coverage analysis failed for DayOff {}: {}", idDayOff, e.getMessage());
    aviso = "Não foi possível calcular o impacto de cobertura.";
}
```

> Add `private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DayOffService.class);`

---

### 2.3 [MEDIUM] Shift-state comparison uses `LOWER(CAST(... AS string))` instead of a proper Enum

**Files:** `DayOffRepository.java`, `HorarioRepository.java`, `PermutaRepository.java`

`DayOff.estado` and `Preferencia.estado` are stored as raw `String`. Every query uses:
```sql
LOWER(CAST(d.estado AS string)) = 'pendente'
```

This prevents the database from using a standard B-tree index on the `estado` column (since function calls on a column usually block index scans, unless a functional index is created). It also creates a type-safety gap — a typo like `"Pendente"` compiles fine.

`EstadoHorario` and `EstadoPermuta` are already enums. The pattern exists. Applying it to `DayOff` and `Preferencia` eliminates both the safety gap and the LOWER/CAST noise from all queries.

**Proposed change:**
```java
// New enum (API/Enums/EstadoDayOff.java)
public enum EstadoDayOff { pendente, aprovado, rejeitado, cancelado }

// In DayOff entity
@Enumerated(EnumType.STRING)
@Column(name = "estado")
private EstadoDayOff estado;

// In DayOffRepository — clean, index-friendly
@Query("SELECT d FROM DayOff d WHERE d.estado = 'pendente' AND ...")
```

---

### 2.4 [LOW] `PermutaService.validarDescansoMinimoPosPermuta()` — magic constant duplicated

**Files:** `API/Services/HorarioService.java` line 208, `API/Services/PermutaService.java` line 228

`final int DESCANSO_MINIMO_HORAS = 11` is defined as a local variable in both methods. A shared constant in `LojautilizadorHelper` or a dedicated `RegrasNegocio` constants class would make the business rule explicit and avoid drift.

**Proposed fix:**
```java
// In a shared location, e.g. LojautilizadorHelper or a new RegrasNegocio class
public static final int DESCANSO_MINIMO_HORAS = 11;

// Usage in both services
if (gap < LojautilizadorHelper.DESCANSO_MINIMO_HORAS) { ... }
```

---

### 2.5 [LOW] `WebComplementaresController` — `origemPermuta` redirect leaks on validation failure

**File:** `WEB/WebComplementaresController.java` — line 291

```java
return "redirect:/web/complementares"
        + (idHorarioOrigem != null ? "?origemPermuta=" + idHorarioOrigem : "");
```

When the swap fails because `idHorarioOrigem` is null (caught at line 272), this evaluates to `"redirect:/web/complementares"` — which is correct. However, when the failure is a date/ownership error, the redirect still carries `?origemPermuta=<id>`, reopening the swap form pre-selected with the invalid origin. This can confuse the user into thinking their selection persists. Consider not carrying the param on failure, or flash-attributing the origin separately.

---

## 3. Deprecated / Outdated Spring Boot Patterns

### 3.1 [LOW] No deprecated annotations found

All controllers use `@Controller` + `@RequestMapping` / `@GetMapping` / `@PostMapping` — correct for Spring MVC 6.x. No `@Autowired` on fields; all injection is via constructor — best practice. No `@SpringBootApplication(exclude=...)` workarounds spotted.

### 3.2 [LOW] `@Query` JPQL with string-literal enum comparisons

Several queries use `LOWER(CAST(h.estado AS string)) = 'aprovado'`. When the field is later migrated to a proper `@Enumerated(EnumType.STRING)` column (see §2.3), these queries must also be updated to use `h.estado = com.example.projeto2.API.Enums.EstadoHorario.aprovado` or the equivalent SpEL binding.

---

## 4. Code Quality / Clean Code

### 4.1 [LOW] Comparator lambda in `listarColegasDaLoja()` 

**File:** `API/Services/PreferenciaService.java` — line 109

```java
// CURRENT
.sorted(Comparator.comparing(nome -> nome.toLowerCase()))

// PROPOSED (cleaner, avoids lambda capture)
.sorted(String.CASE_INSENSITIVE_ORDER)
```

### 4.2 [LOW] `WebPainelController` in-memory status checks with inline helpers

**File:** `WEB/WebPainelController.java` — lines 62–64

The controller loads all `folgas`, `preferencias`, and `permutas` for the user just to count pending ones. The same `contarPendentesParaAprovacao()` strategy from §1.1 applies here for the employee-perspective counts (they need pending counts too, not full lists). But for the badge on the employee's own items, a COUNT endpoint per service would eliminate the list loads.

---

## 5. Summary Table

| # | File / Location | Severity | Category | Quick Fix? |
|---|---|---|---|---|
| 1.1 | `WebPainelController` lines 68–75 | **HIGH** | N+1 / redundant query | Yes — swap to `contarPendentes*()` |
| 1.2 | `WebAppService.obterPermissoes()` | **HIGH** | Redundant DB round-trips | Yes — single query + Java flags |
| 1.3 | `PreferenciaService.existePreferenciaDuplicada()` | **HIGH** | Ignores dedicated DB query | Yes — delegate to `existsPreferenciaDuplicada()` |
| 1.4 | `DayOffService.atualizarEstadoPedido()` | MEDIUM | Full list fetch for existence check | Yes — use `findPedidoDaLojaById()` |
| 1.5 | `WebComplementaresController.registarPermuta()` | MEDIUM | Double shift fetch | Yes — drop first fetch |
| 1.6 | `PreferenciaService.listarColegasDaLoja()` | MEDIUM | Java-side filtering | Medium — new repo query |
| 1.7 | `HorarioRepository.findTurnosDosColegas()` | LOW | Missing store scope | Medium — add loja subquery |
| 2.1 | `WebHorariosController.exportarPdf()` | **HIGH** | No exception handling | Yes — add try/catch |
| 2.2 | `DayOffService.aprovarPedidoFolgaComCobertura()` | MEDIUM | Silent exception swallow | Yes — add log.warn |
| 2.3 | `DayOff.estado` / `Preferencia.estado` as String | MEDIUM | Type safety + index | Medium — migrate to Enum |
| 2.4 | `DESCANSO_MINIMO_HORAS` duplicated | LOW | Magic constant | Yes — extract to shared constant |
| 2.5 | `registarPermuta()` redirect on failure | LOW | UX edge case | Yes — conditional redirect |
| 4.1 | `listarColegasDaLoja()` comparator | LOW | Code style | Yes — `String.CASE_INSENSITIVE_ORDER` |

---

## 6. Recommended Implementation Order

1. **§1.1 + §1.3 first** — both are single-line changes with no risk; immediately reduce DB pressure on the dashboard and preference form.
2. **§2.1** — `exportarPdf` exception handling; prevents 500 errors reaching end users.
3. **§1.2** — Refactor `obterPermissoes()`; larger change but affects every page load.
4. **§2.3** — Enum migration for `DayOff`/`Preferencia` states; requires a schema migration (`ALTER TABLE` to ensure uppercase/lowercase consistency) and updating all JPQL queries.
5. **§1.4, §1.5, §1.6, §2.2** — medium-effort improvements, plan for a follow-up PR.
6. **§1.7** — needs a test-case to confirm whether cross-store data is actually possible in your deployment before prioritising.
