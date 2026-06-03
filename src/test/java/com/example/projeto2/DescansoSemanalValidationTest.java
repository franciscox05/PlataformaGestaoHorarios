package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.Enums.EstadoHorario;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Turno;
import com.example.projeto2.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class DescansoSemanalValidationTest extends FluxosCriticosTestSupport {

    @Test
    void geracaoMensalGaranteDuasFolgasPorSemanaMesmoComBloqueiosEAjustesDaLoja() {
        GeracaoFixture fixture = criarContextoGeracao("descanso-semanal");
        Utilizador gerente = fixture.lojaFixture().gerente();
        Utilizador decisor = fixture.lojaFixture().supervisor();
        Utilizador colaboradorComDayOff = fixture.lojaFixture().colaboradores().get(0);
        Utilizador colaboradorComPreferencia = fixture.lojaFixture().colaboradores().get(1);

        LocalDate diaBloqueado = fixture.referencia().plusDays(2);
        LocalDate domingoFechado = fixture.referencia().with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY));

        criarDayOffAprovado(colaboradorComDayOff, diaBloqueado, "Folga semanal protegida.");
        criarPreferenciaAprovada(
                colaboradorComPreferencia,
                decisor,
                "folgas",
                diaBloqueado.plusDays(1),
                diaBloqueado.plusDays(1),
                4,
                "Prefere descansar neste dia."
        );
        criarHorarioEspecial(
                gerente.getId(),
                "Encerramento dominical de teste",
                domingoFechado,
                domingoFechado,
                true,
                null,
                null,
                null,
                "Compatibilidade com descanso semanal."
        );

        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                gerente.getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertNotNull(proposta);
        assertFalse(proposta.linhas().stream()
                .anyMatch(linha -> diaBloqueado.equals(linha.data()) && colaboradorComDayOff.getNome().equals(linha.colaborador())));
        assertFalse(proposta.linhas().stream()
                .anyMatch(linha -> diaBloqueado.plusDays(1).equals(linha.data()) && colaboradorComPreferencia.getNome().equals(linha.colaborador())));

        List<Horario> horariosGerados = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta());
        Map<Integer, Map<LocalDate, Set<LocalDate>>> diasPorSemana = new HashMap<>();
        for (Horario horario : horariosGerados) {
            if (horario.getIdLojautilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador().getId() == null
                    || horario.getDataTurno() == null) {
                continue;
            }

            Integer idColaborador = horario.getIdLojautilizador().getIdUtilizador().getId();
            LocalDate inicioSemana = inicioSemana(horario.getDataTurno());
            diasPorSemana
                    .computeIfAbsent(idColaborador, ignored -> new HashMap<>())
                    .computeIfAbsent(inicioSemana, ignored -> new LinkedHashSet<>())
                    .add(horario.getDataTurno());
        }

        for (Map<LocalDate, Set<LocalDate>> semanasDoColaborador : diasPorSemana.values()) {
            for (Set<LocalDate> diasTrabalhados : semanasDoColaborador.values()) {
                assertTrue(diasTrabalhados.size() <= 5, "Cada colaborador deve manter pelo menos duas folgas em cada semana.");
            }
        }
    }

    @Test
    void geracaoMensalGaranteRotacaoDeFimDeSemanaEmJanelaDeDuasSemanas() {
        GeracaoFixture fixture = criarContextoGeracao("rotacao-fds");

        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        List<Horario> horariosGerados = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta());
        List<LocalDate> finsDeSemanaNoPeriodo = listarFinsDeSemanaDoPeriodo(
                fixture.referencia().withDayOfMonth(1),
                fixture.referencia().withDayOfMonth(fixture.referencia().lengthOfMonth())
        );
        Map<Integer, String> tiposCargoPorColaborador = lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(fixture.lojaFixture().loja().getId()).stream()
                .filter(ligacao -> ligacao.getDataFim() == null)
                .filter(ligacao -> ligacao.getIdUtilizador() != null)
                .filter(ligacao -> ligacao.getIdUtilizador().getId() != null)
                .filter(ligacao -> ligacao.getIdCargo() != null)
                .filter(ligacao -> ligacao.getIdCargo().getTipo() != null)
                .collect(Collectors.toMap(
                        ligacao -> ligacao.getIdUtilizador().getId(),
                        ligacao -> ligacao.getIdCargo().getTipo().toLowerCase(),
                        (atual, substituto) -> atual
                ));

        Map<Integer, Set<LocalDate>> finsDeSemanaTrabalhadosPorColaborador = new HashMap<>();
        for (Horario horario : horariosGerados) {
            if (horario.getIdLojautilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador().getId() == null
                    || horario.getDataTurno() == null
                    || !ehFimDeSemana(horario.getDataTurno())) {
                continue;
            }

            finsDeSemanaTrabalhadosPorColaborador
                    .computeIfAbsent(horario.getIdLojautilizador().getIdUtilizador().getId(), ignored -> new LinkedHashSet<>())
                    .add(inicioFimDeSemana(horario.getDataTurno()));
        }

        for (Map.Entry<Integer, Set<LocalDate>> entry : finsDeSemanaTrabalhadosPorColaborador.entrySet()) {
            String tipoCargo = tiposCargoPorColaborador.get(entry.getKey());
            if ("gerente".equals(tipoCargo) || "subgerente".equals(tipoCargo)) {
                continue;
            }

            Set<LocalDate> finsDeSemanaTrabalhados = entry.getValue();
            for (int indice = 0; indice < finsDeSemanaNoPeriodo.size() - 1; indice++) {
                LocalDate fimDeSemanaAtual = finsDeSemanaNoPeriodo.get(indice);
                LocalDate fimDeSemanaSeguinte = finsDeSemanaNoPeriodo.get(indice + 1);
                boolean trabalhaNaJanelaCompleta = finsDeSemanaTrabalhados.contains(fimDeSemanaAtual)
                        && finsDeSemanaTrabalhados.contains(fimDeSemanaSeguinte);
                assertFalse(
                        trabalhaNaJanelaCompleta,
                        "Cada colaborador deve ter pelo menos um fim de semana de descanso em cada janela de duas semanas."
                );
            }
        }
    }

    @Test
    void rotacaoDeFimDeSemanaConsideraHistoricoAnteriorAoPeriodo() {
        GeracaoFixture fixture = criarContextoGeracao("rotacao-historico");
        Utilizador gerente = fixture.lojaFixture().gerente();
        Utilizador colaboradorHistorico = fixture.lojaFixture().colaboradores().get(0);
        LocalDate primeiroSabadoDoPeriodo = fixture.referencia().with(TemporalAdjusters.firstInMonth(DayOfWeek.SATURDAY));
        LocalDate fimDeSemanaAnterior = primeiroSabadoDoPeriodo.minusWeeks(1);

        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(fixture.lojaFixture().loja().getId(), colaboradorHistorico.getId());
        criarHorarioAprovado(ligacaoAtiva, fixture.turnos().get(0), fimDeSemanaAnterior);
        criarHorarioAprovado(ligacaoAtiva, fixture.turnos().get(1), fimDeSemanaAnterior.plusDays(1));
        flushAndClear();

        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                gerente.getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertFalse(proposta.linhas().stream()
                .anyMatch(linha -> colaboradorHistorico.getNome().equals(linha.colaborador())
                        && (primeiroSabadoDoPeriodo.equals(linha.data()) || primeiroSabadoDoPeriodo.plusDays(1).equals(linha.data()))));
    }

    private Lojautilizador obterLigacaoAtiva(Integer idLoja, Integer idUtilizador) {
        return lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja).stream()
                .filter(ligacao -> ligacao.getDataFim() == null)
                .filter(ligacao -> ligacao.getIdUtilizador() != null)
                .filter(ligacao -> idUtilizador.equals(ligacao.getIdUtilizador().getId()))
                .findFirst()
                .orElseThrow();
    }

    private void criarHorarioAprovado(Lojautilizador ligacao, Turno turno, LocalDate dataTurno) {
        Horario horario = new Horario();
        horario.setIdLojautilizador(ligacao);
        horario.setIdTurno(turno);
        horario.setDataTurno(dataTurno);
        horario.setEstado(EstadoHorario.aprovado);
        horarioRepository.save(horario);
    }

    private List<LocalDate> listarFinsDeSemanaDoPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        List<LocalDate> finsDeSemana = new ArrayList<>();
        LocalDate cursor = dataInicio.with(TemporalAdjusters.firstInMonth(DayOfWeek.SATURDAY));
        while (!cursor.isAfter(dataFim)) {
            finsDeSemana.add(cursor);
            cursor = cursor.plusWeeks(1);
        }
        return finsDeSemana;
    }

    private LocalDate inicioSemana(LocalDate data) {
        return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate inicioFimDeSemana(LocalDate data) {
        return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
    }

    private boolean ehFimDeSemana(LocalDate data) {
        DayOfWeek dayOfWeek = data.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
