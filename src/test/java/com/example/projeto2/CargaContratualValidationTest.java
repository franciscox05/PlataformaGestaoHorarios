package com.example.projeto2;

import com.example.projeto2.API.Services.GeracaoHorariosService;
import com.example.projeto2.API.Enums.EstadoUtilizador;
import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class CargaContratualValidationTest extends FluxosCriticosTestSupport {

    private static final Map<String, Long> LIMITES_MINUTOS_POR_CARGO = Map.of(
            "gerente", 176L * 60L,
            "subgerente", 176L * 60L,
            "supervisor", 176L * 60L,
            "fulltime", 176L * 60L,
            "parttime", 96L * 60L,
            "reforco_parttime", 64L * 60L
    );

    @Test
    void geracaoMensalRespeitaCargaContratualConfiguradaPorPerfil() {
        GeracaoFixture fixture = criarContextoGeracao("carga-contratual");

        GeracaoHorariosService.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertNotNull(proposta);
        assertEquals(fixture.referencia().lengthOfMonth() * contarBlocosCobertura(fixture.turnos()), proposta.resumo().turnos());
        assertFalse(proposta.resumoColaboradores().isEmpty());

        Map<Integer, String> tiposCargoPorUtilizador = mapearTiposCargoDaLoja(fixture.lojaFixture().loja().getId());
        Set<String> tiposComCargaAtribuida = proposta.resumoColaboradores().stream()
                .map(resumo -> tiposCargoPorUtilizador.get(resumo.idColaborador()))
                .collect(Collectors.toSet());

        assertTrue(tiposComCargaAtribuida.contains("fulltime"));
        assertTrue(tiposComCargaAtribuida.contains("parttime"));
        assertTrue(tiposComCargaAtribuida.contains("reforco_parttime"));

        for (GeracaoHorariosService.ResumoColaborador resumo : proposta.resumoColaboradores()) {
            String tipoCargo = tiposCargoPorUtilizador.get(resumo.idColaborador());
            Long limiteMinutos = LIMITES_MINUTOS_POR_CARGO.get(tipoCargo);

            assertNotNull(tipoCargo, "Todos os colaboradores resumidos devem ter um cargo ativo associado.");
            assertNotNull(limiteMinutos, "Faltou mapear um limite contratual para o cargo " + tipoCargo + ".");
            assertTrue(
                    resumo.minutos() <= limiteMinutos,
                    "O colaborador " + resumo.nome() + " ultrapassou o limite contratual do cargo " + tipoCargo
                            + ": " + resumo.minutos() + " > " + limiteMinutos
            );
        }
    }

    @Test
    void reforcoDeFimDeSemanaSoRecebeTurnosAoSabadoOuDomingo() {
        GeracaoFixture fixture = criarContextoGeracao("carga-reforco");

        GeracaoHorariosService.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        List<Horario> horariosReforco = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta()).stream()
                .filter(horario -> horario.getIdLojautilizador() != null)
                .filter(horario -> horario.getIdLojautilizador().getIdCargo() != null)
                .filter(horario -> "reforco_parttime".equalsIgnoreCase(horario.getIdLojautilizador().getIdCargo().getTipo()))
                .toList();

        assertFalse(horariosReforco.isEmpty());
        assertTrue(horariosReforco.stream()
                .map(Horario::getDataTurno)
                .allMatch(this::ehFimDeSemana));
    }

    @Test
    void gerentePodeGerarHorarioApenasComFuncionariosSelecionados() {
        GeracaoFixture fixture = criarContextoGeracao("carga-selecao-funcionarios");
        Utilizador colaboradorExcluido = fixture.lojaFixture().colaboradores().getFirst();

        List<Integer> idsSelecionados = geracaoHorariosBLL.listarColaboradoresElegiveis(
                        fixture.lojaFixture().gerente().getId(),
                        fixture.referencia().getYear(),
                        fixture.referencia().getMonthValue()
                )
                .stream()
                .map(GeracaoHorariosService.ColaboradorElegivel::idColaborador)
                .filter(idColaborador -> !colaboradorExcluido.getId().equals(idColaborador))
                .toList();

        GeracaoHorariosService.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue(),
                idsSelecionados
        );

        assertFalse(contemColaborador(proposta, colaboradorExcluido.getNome()));
        assertEquals(fixture.referencia().lengthOfMonth() * contarBlocosCobertura(fixture.turnos()), proposta.resumo().turnos());
    }

    @Test
    void fullTimeEGestaoNaoRecebemTurnosComMenosDeOitoHoras() {
        GeracaoFixture fixture = criarContextoGeracao("carga-turnos-ft");

        GeracaoHorariosService.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        List<Horario> horarios = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta());
        assertFalse(horarios.isEmpty());
        assertTrue(horarios.stream()
                .filter(horario -> horario.getIdLojautilizador() != null)
                .filter(horario -> horario.getIdLojautilizador().getIdCargo() != null)
                .filter(horario -> {
                    String tipo = horario.getIdLojautilizador().getIdCargo().getTipo();
                    return "fulltime".equalsIgnoreCase(tipo)
                            || "gerente".equalsIgnoreCase(tipo)
                            || "subgerente".equalsIgnoreCase(tipo)
                            || "supervisor".equalsIgnoreCase(tipo);
                })
                .allMatch(horario -> duracaoEmMinutos(horario) >= 8 * 60));
    }

    @Test
    void geracaoRespeitaPeloMenosOitoHorasDeDescansoEntreDiasConsecutivos() {
        GeracaoFixture fixture = criarContextoGeracao("carga-descanso-entre-turnos");

        GeracaoHorariosService.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        Map<Integer, List<Horario>> horariosPorColaborador = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta()).stream()
                .filter(horario -> horario.getIdLojautilizador() != null)
                .filter(horario -> horario.getIdLojautilizador().getIdUtilizador() != null)
                .collect(Collectors.groupingBy(horario -> horario.getIdLojautilizador().getIdUtilizador().getId()));

        for (List<Horario> horariosColaborador : horariosPorColaborador.values()) {
            List<Horario> ordenados = horariosColaborador.stream()
                    .sorted(Comparator
                            .comparing(Horario::getDataTurno)
                            .thenComparing(horario -> horario.getIdTurno().getHoraInicio()))
                    .toList();

            for (int indice = 1; indice < ordenados.size(); indice++) {
                Horario anterior = ordenados.get(indice - 1);
                Horario atual = ordenados.get(indice);
                if (!anterior.getDataTurno().plusDays(1).equals(atual.getDataTurno())) {
                    continue;
                }

                assertTrue(
                        horasDescanso(anterior, atual) >= 8,
                        "Um colaborador nao pode ter menos de 8 horas entre turnos de dias consecutivos."
                );
            }
        }
    }

    @Test
    void geracaoMensalExcluiInativosESemVinculoValidoNoPeriodo() {
        GeracaoFixture fixture = criarContextoGeracao("carga-elegibilidade");
        LocalDate inicioPeriodo = fixture.referencia().withDayOfMonth(1);
        LocalDate fimPeriodo = fixture.referencia().withDayOfMonth(fixture.referencia().lengthOfMonth());

        Cargo cargoFullTime = obterOuCriarCargo("fulltime", "Assistente de Vendas FT");

        Utilizador inativo = criarUtilizadorComPasswordEmTexto(
                "Colaborador Inativo " + inicioPeriodo,
                "inativo." + inicioPeriodo,
                "Colaborador123",
                EstadoUtilizador.inativo
        );
        criarLigacao(inativo, fixture.lojaFixture().loja(), cargoFullTime, inicioPeriodo.minusDays(20), null);

        Utilizador semVinculoNoPeriodo = criarUtilizadorHashado(
                "Colaborador Futuro " + inicioPeriodo,
                "futuro." + inicioPeriodo,
                "Colaborador123"
        );
        criarLigacao(semVinculoNoPeriodo, fixture.lojaFixture().loja(), cargoFullTime, fimPeriodo.plusDays(2), null);

        flushAndClear();

        GeracaoHorariosService.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertFalse(contemColaborador(proposta, inativo.getNome()));
        assertFalse(contemColaborador(proposta, semVinculoNoPeriodo.getNome()));
        assertEquals(fixture.referencia().lengthOfMonth() * contarBlocosCobertura(fixture.turnos()), proposta.resumo().turnos());
    }

    private Map<Integer, String> mapearTiposCargoDaLoja(Integer idLoja) {
        return lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja).stream()
                .filter(ligacao -> ligacao.getIdUtilizador() != null)
                .filter(ligacao -> ligacao.getIdUtilizador().getId() != null)
                .filter(ligacao -> ligacao.getIdCargo() != null)
                .filter(ligacao -> ligacao.getIdCargo().getTipo() != null)
                .filter(ligacao -> ligacao.getDataFim() == null)
                .collect(Collectors.toMap(
                        ligacao -> ligacao.getIdUtilizador().getId(),
                        ligacao -> ligacao.getIdCargo().getTipo().toLowerCase(),
                        (atual, substituto) -> atual
                ));
    }

    private boolean contemColaborador(GeracaoHorariosService.PropostaResultado proposta, String nome) {
        Predicate<GeracaoHorariosService.HorarioLinha> noHorario = linha -> nome.equals(linha.colaborador());
        Predicate<GeracaoHorariosService.ResumoColaborador> noResumo = resumo -> nome.equals(resumo.nome());

        return proposta.linhas().stream().anyMatch(noHorario)
                || proposta.resumoColaboradores().stream().anyMatch(noResumo);
    }

    private boolean ehFimDeSemana(LocalDate data) {
        return data != null
                && (data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY);
    }

    private long duracaoEmMinutos(Horario horario) {
        if (horario.getIdTurno() == null
                || horario.getIdTurno().getHoraInicio() == null
                || horario.getIdTurno().getHoraFim() == null) {
            return 0;
        }

        if (!horario.getIdTurno().getHoraFim().isBefore(horario.getIdTurno().getHoraInicio())) {
            return Duration.between(horario.getIdTurno().getHoraInicio(), horario.getIdTurno().getHoraFim()).toMinutes();
        }
        return Duration.between(horario.getIdTurno().getHoraInicio(), java.time.LocalTime.MAX).plusMinutes(1).toMinutes()
                + Duration.between(java.time.LocalTime.MIN, horario.getIdTurno().getHoraFim()).toMinutes();
    }

    private long horasDescanso(Horario anterior, Horario atual) {
        LocalDateTime fimAnterior = anterior.getDataTurno().atTime(anterior.getIdTurno().getHoraFim());
        if (!anterior.getIdTurno().getHoraFim().isAfter(anterior.getIdTurno().getHoraInicio())) {
            fimAnterior = fimAnterior.plusDays(1);
        }

        LocalDateTime inicioAtual = atual.getDataTurno().atTime(atual.getIdTurno().getHoraInicio());
        return Duration.between(fimAnterior, inicioAtual).toHours();
    }
}
