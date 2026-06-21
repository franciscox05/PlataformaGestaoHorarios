package com.example.projeto2;

import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Loja;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.PropostaHorarioMensal;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Repositories.PropostaHorarioMensalRepository;
import com.example.projeto2.API.Services.HorarioValidatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite de conformidade Multi-Store — valida cinco invariantes de negócio
 * introduzidos com a arquitectura de loja múltipla (Henrique scenario):
 *
 * <ol>
 *   <li>A guarda JPQL detecta sobreposição de turnos entre lojas distintas.</li>
 *   <li>Turnos adjacentes (fronteira exacta) não são contados como sobreposição.</li>
 *   <li>HorarioService.adicionarTurno() rejeita o turno via IllegalArgumentException.</li>
 *   <li>HorarioValidatorService detecta violação do descanso mínimo legal de 11h.</li>
 *   <li>Nenhum controlador web expõe um endpoint de geração de horários.</li>
 * </ol>
 */
@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class MultiStoreComplianceTests extends FluxosCriticosTestSupport {

    @Autowired
    private PropostaHorarioMensalRepository propostaRepository;

    // Instanciação direta — HorarioValidatorService não tem dependências Spring
    private final HorarioValidatorService validador = new HorarioValidatorService();

    // ── T1: JPQL detecta sobreposição entre lojas ─────────────────────────────

    @Test
    void repositorioDetectaSobreposicaoDeHorarioEntreLojasDistintas() {
        LocalDate dia = LocalDate.now().plusDays(30);

        Loja lojaA = criarLoja("T1-A");
        Utilizador colaborador = criarUtilizadorHashado(
                "Colab-T1", "colab-t1-" + novoUuid(), "Pass123");
        Lojautilizador ligacaoA = criarLigacaoAtiva(
                colaborador, lojaA, obterOuCriarCargo("fulltime", "Assistente FT"));

        Turno turnoExistente = salvarTurno("manha", LocalTime.of(10, 0), LocalTime.of(15, 0));

        Horario horario = new Horario();
        horario.setIdLojautilizador(ligacaoA);
        horario.setIdTurno(turnoExistente);
        horario.setDataTurno(dia);
        horario.setEstado(EstadoHorario.aprovado);
        horarioRepository.save(horario);
        flushAndClear();

        // Allen predicate: 14:00 < 15:00 = true  AND  19:00 > 10:00 = true → overlap
        long sobreposicoes = horarioRepository.countGlobalOverlappingShifts(
                colaborador.getId(), dia, LocalTime.of(14, 0), LocalTime.of(19, 0));

        assertTrue(sobreposicoes > 0,
                "countGlobalOverlappingShifts deve retornar > 0 quando 14-19 se sobrepo"
                        + "e a 10-15 do mesmo colaborador noutra loja");
    }

    // ── T2: Turnos adjacentes na fronteira exacta não sobrepõem ───────────────

    @Test
    void repositorioPermiteTurnosAdjacentesEntreLojasDistintas() {
        LocalDate dia = LocalDate.now().plusDays(30);

        Loja lojaA = criarLoja("T2-A");
        Utilizador colaborador = criarUtilizadorHashado(
                "Colab-T2", "colab-t2-" + novoUuid(), "Pass123");
        Lojautilizador ligacaoA = criarLigacaoAtiva(
                colaborador, lojaA, obterOuCriarCargo("fulltime", "Assistente FT"));

        Turno turnoExistente = salvarTurno("manha", LocalTime.of(10, 0), LocalTime.of(15, 0));

        Horario horario = new Horario();
        horario.setIdLojautilizador(ligacaoA);
        horario.setIdTurno(turnoExistente);
        horario.setDataTurno(dia);
        horario.setEstado(EstadoHorario.aprovado);
        horarioRepository.save(horario);
        flushAndClear();

        // Allen predicate: 15:00 < 15:00 = FALSE → strict inequality → no overlap
        long sobreposicoes = horarioRepository.countGlobalOverlappingShifts(
                colaborador.getId(), dia, LocalTime.of(15, 0), LocalTime.of(20, 0));

        assertEquals(0L, sobreposicoes,
                "Turnos adjacentes (15-20 imediatamente apos 10-15) nao devem ser "
                        + "considerados sobrepostos pelo predicado de Allen");
    }

    // ── T3: HorarioService.adicionarTurno rejeita conflito cross-store ────────

    @Test
    void servicoAdicionarTurnoRejeitaSobreposicaoEntreLojasDistintas() {
        LocalDate dia = LocalDate.now().plusDays(30);
        String uid = novoUuid();

        Loja lojaA = criarLoja("T3-A");
        Loja lojaB = criarLoja("T3-B");

        Utilizador colaborador = criarUtilizadorHashado(
                "Colab-T3", "colab-t3-" + uid, "Pass123");
        Utilizador gerente = criarUtilizadorHashado(
                "Gerente-T3", "gerente-t3-" + uid, "Pass123");

        Lojautilizador ligacaoA = criarLigacaoAtiva(
                colaborador, lojaA, obterOuCriarCargo("fulltime", "Assistente FT"));
        Lojautilizador ligacaoB = criarLigacaoAtiva(
                colaborador, lojaB, obterOuCriarCargo("parttime", "Assistente PT"));
        criarLigacaoAtiva(gerente, lojaB, obterOuCriarCargo("gerente", "Gerente de Loja"));

        // Horario já existente em Loja A: 10:00-15:00
        Turno turnoLojaA = salvarTurno("manha", LocalTime.of(10, 0), LocalTime.of(15, 0));
        Horario horarioExistente = new Horario();
        horarioExistente.setIdLojautilizador(ligacaoA);
        horarioExistente.setIdTurno(turnoLojaA);
        horarioExistente.setDataTurno(dia);
        horarioExistente.setEstado(EstadoHorario.aprovado);
        horarioRepository.save(horarioExistente);

        // Turno conflituoso para Loja B: 14:00-19:00 (sobrepõe-se a 10:00-15:00)
        Turno turnoConflito = salvarTurno("intermedio", LocalTime.of(14, 0), LocalTime.of(19, 0));

        // Proposta em bruto para Loja B (o serviço precisa de uma PropostaHorarioMensal existente)
        PropostaHorarioMensal proposta = new PropostaHorarioMensal();
        proposta.setIdLoja(lojaB);
        proposta.setIdUtilizadorGeracao(gerente);
        proposta.setAno(dia.getYear());
        proposta.setMes(dia.getMonthValue());
        proposta.setEstado("rascunho");
        proposta.setDataGeracao(LocalDateTime.now());
        proposta = propostaRepository.save(proposta);

        flushAndClear();

        final Integer idProposta = proposta.getId();
        final Integer idLigacaoB = ligacaoB.getId();
        final Integer idTurnoConflito = turnoConflito.getId();
        final Integer idGerente = gerente.getId();

        assertThrows(IllegalArgumentException.class,
                () -> horarioBLL.adicionarTurno(idProposta, idLigacaoB, dia, idTurnoConflito, idGerente),
                "adicionarTurno deve lancar IllegalArgumentException ao tentar atribuir "
                        + "turno 14-19 em Loja B quando colaborador ja tem turno 10-15 em Loja A");
    }

    // ── T6: regressão do crash HTTP 500 em /web/painel para gerente multi-loja ─

    @Test
    void listarEquipaDeHojeNaoRebentaParaUtilizadorMultiLoja() {
        // Regressão do crash HTTP 500 em /web/painel (Revisao.md, ponto 19): a query antiga
        // findEquipaDeHojeNaLojaDoUtilizador resolvia a loja com uma SUBQUERY ESCALAR que
        // devolvia 2 linhas para um utilizador com vínculo ativo a 2 lojas ("more than one
        // row returned by a subquery used as an expression"). A query passou a receber idLoja
        // explícito; o overload de 1 argumento resolve a primeira ligação activa — nunca mais
        // a subquery multi-linha. Antes do fix, esta chamada lançava DataIntegrityViolation.
        String uid = novoUuid();
        Loja lojaA = criarLoja("T6-A-" + uid);
        Loja lojaB = criarLoja("T6-B-" + uid);
        Utilizador multiLoja = criarUtilizadorHashado("Multi-T6", "multi-t6-" + uid, "Pass123");
        criarLigacaoAtiva(multiLoja, lojaA, obterOuCriarCargo("gerente", "Gerente de Loja"));
        criarLigacaoAtiva(multiLoja, lojaB, obterOuCriarCargo("gerente", "Gerente de Loja"));
        flushAndClear();

        List<Horario> equipa = horarioBLL.listarEquipaDeHoje(multiLoja.getId());
        assertTrue(equipa != null,
                "listarEquipaDeHoje deve devolver uma lista (vazia ou nao), nunca rebentar com "
                        + "'more than one row returned by a subquery' para um utilizador multi-loja.");
    }

    // ── T4: HorarioValidatorService rejeita gap de 9h (< 11h legal) ──────────

    @Test
    void validadorDetectaViolacaoDescansoMinimoNoiteManha() {
        LocalDate diaAnterior = LocalDate.now();
        LocalDate diaSeguinte = diaAnterior.plusDays(1);

        // Turno de tarde/noite: 14:00-22:00
        Turno noite = turnoTransiente("noite", LocalTime.of(14, 0), LocalTime.of(22, 0));
        // Turno de manhã seguinte: 07:00-15:00 — gap = 9h < 11h legal
        Turno manha = turnoTransiente("manha", LocalTime.of(7, 0), LocalTime.of(15, 0));

        long horasGap = validador.calcularHorasDescanso(diaAnterior, noite, diaSeguinte, manha);
        assertEquals(9L, horasGap,
                "Gap entre 22:00 e 07:00 do dia seguinte deve ser exatamente 9 horas");

        boolean respeitaDescanso = validador.respeitaDescansoMinimo(
                diaAnterior, noite, diaSeguinte, manha, 11);

        assertFalse(respeitaDescanso,
                "Gap de 9h viola o descanso minimo legal de 11h (CT art. 214.o) — "
                        + "respeitaDescansoMinimo deve retornar false");
    }

    // ── T5: Nenhum endpoint web de geração de horários ────────────────────────

    @Test
    void webCamadaNaoExpoeEndpointDeGeracaoDeHorarios() {
        List<Class<?>> controladoresWeb = List.of(
                com.example.projeto2.WEB.WebHorariosController.class,
                com.example.projeto2.WEB.WebComplementaresController.class,
                com.example.projeto2.WEB.WebEquipaController.class,
                com.example.projeto2.WEB.WebPainelController.class,
                com.example.projeto2.WEB.WebPerfilController.class,
                com.example.projeto2.WEB.WebNotificacoesController.class,
                com.example.projeto2.WEB.WebLoginController.class,
                com.example.projeto2.WEB.WebModulosController.class,
                com.example.projeto2.WEB.WebPermutasApiController.class,
                com.example.projeto2.WEB.WebHorariosApiController.class
        );

        List<String> endpointsDeGeracao = new ArrayList<>();
        for (Class<?> controlador : controladoresWeb) {
            for (Method metodo : controlador.getDeclaredMethods()) {
                extrairPathsMapeados(metodo).stream()
                        .filter(path -> path.toLowerCase().contains("gerar"))
                        .forEach(path -> endpointsDeGeracao.add(
                                controlador.getSimpleName() + "#" + metodo.getName() + " → " + path));
            }
        }

        assertTrue(endpointsDeGeracao.isEmpty(),
                "A geracao de horarios e funcionalidade desktop-only. A camada web NAO deve "
                        + "expor endpoints de geracao. Encontrados: " + endpointsDeGeracao);
    }

    // ── helpers privados ──────────────────────────────────────────────────────

    private Loja criarLoja(String label) {
        Loja loja = new Loja();
        loja.setNome("Loja Teste " + label + " " + novoUuid());
        loja.setLocalizacao("Ambiente de testes");
        loja.setHoraAbertura(LocalTime.of(9, 0));
        loja.setHoraFecho(LocalTime.of(22, 0));
        return lojaRepository.save(loja);
    }

    private Turno salvarTurno(String tipo, LocalTime inicio, LocalTime fim) {
        Turno t = new Turno();
        t.setTipo(tipo);
        t.setHoraInicio(inicio);
        t.setHoraFim(fim);
        return turnoRepository.save(t);
    }

    /** Turno em memória apenas — para testes que não tocam a base de dados. */
    private Turno turnoTransiente(String tipo, LocalTime inicio, LocalTime fim) {
        Turno t = new Turno();
        t.setTipo(tipo);
        t.setHoraInicio(inicio);
        t.setHoraFim(fim);
        return t;
    }

    private String novoUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private List<String> extrairPathsMapeados(Method metodo) {
        List<String> paths = new ArrayList<>();
        PostMapping pm = metodo.getAnnotation(PostMapping.class);
        if (pm != null) {
            paths.addAll(List.of(pm.value()));
            paths.addAll(List.of(pm.path()));
        }
        GetMapping gm = metodo.getAnnotation(GetMapping.class);
        if (gm != null) {
            paths.addAll(List.of(gm.value()));
            paths.addAll(List.of(gm.path()));
        }
        RequestMapping rm = metodo.getAnnotation(RequestMapping.class);
        if (rm != null) {
            paths.addAll(List.of(rm.value()));
        }
        return paths;
    }
}
