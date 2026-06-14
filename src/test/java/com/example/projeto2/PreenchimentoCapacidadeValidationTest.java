package com.example.projeto2;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Services.geracao.dto.PropostaResultado;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida o comportamento da fase de preenchimento de capacidade do motor de geração:
 * após garantir a cobertura mínima diária, o motor deve reforçar a equipa até
 * esgotar a carga contratual disponível, sem violar nenhuma regra hard.
 */
@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class PreenchimentoCapacidadeValidationTest extends FluxosCriticosTestSupport {

    @Test
    void geracaoProduziuMaisTurnosDoQueOMinimoContratado() {
        GeracaoFixture fixture = criarContextoGeracao("preenchimento-acima-minimo");

        PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        long minimoEsperado = fixture.referencia().lengthOfMonth() * contarBlocosCobertura(fixture.turnos());
        assertTrue(
                proposta.resumo().turnos() > minimoEsperado,
                "A fase de preenchimento deve gerar mais do que o minimo de " + minimoEsperado
                        + " turnos; gerou " + proposta.resumo().turnos() + "."
        );
    }

    @Test
    void colaboradoresFullTimeSaoEscaladosMaisDoQueOMinimoDiario() {
        GeracaoFixture fixture = criarContextoGeracao("preenchimento-ft-utilizacao");

        PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        List<Horario> horariosGerados = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta());

        Map<Integer, Long> turnosPorColaborador = horariosGerados.stream()
                .filter(h -> h.getIdLojautilizador() != null)
                .filter(h -> h.getIdLojautilizador().getIdUtilizador() != null)
                .filter(h -> h.getIdLojautilizador().getIdCargo() != null)
                .filter(h -> "fulltime".equalsIgnoreCase(h.getIdLojautilizador().getIdCargo().getTipo()))
                .collect(Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId(),
                        Collectors.counting()
                ));

        assertFalse(turnosPorColaborador.isEmpty(), "Deve haver colaboradores fulltime escalonados.");

        // Com carga de 176h e turnos de 9h, um FT precisa de ~20 dias/mês para atingir a carga.
        // O mínimo diário (1 FT por bloco) implicaria apenas 3 turnos de FT por dia a partilhar
        // entre 5 FT, ou seja, ~18 dias/FT num mês de 30 dias. O preenchimento deve empurrar
        // a média claramente acima de lengthOfMonth / 3 (10 turnos), que seria só o mínimo.
        long diasNoMes = fixture.referencia().lengthOfMonth();
        long limiteMinimoPorColaborador = diasNoMes / 3;

        long colaboradoresAbaixoDoLimite = turnosPorColaborador.values().stream()
                .filter(contagem -> contagem <= limiteMinimoPorColaborador)
                .count();

        assertTrue(
                colaboradoresAbaixoDoLimite == 0,
                "Todos os FT devem ter mais do que " + limiteMinimoPorColaborador
                        + " turnos (limite minimo sem preenchimento); alguns ficaram aquem."
        );
    }

    @Test
    void preenchimentoNaoViolaDescansoMinimoEntreTurnosConsecutivos() {
        GeracaoFixture fixture = criarContextoGeracao("preenchimento-descanso");

        PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        List<Horario> horariosGerados = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta());
        Map<Integer, List<Horario>> horariosPorColaborador = horariosGerados.stream()
                .filter(h -> h.getIdLojautilizador() != null)
                .filter(h -> h.getIdLojautilizador().getIdUtilizador() != null)
                .filter(h -> h.getIdLojautilizador().getIdUtilizador().getId() != null)
                .collect(Collectors.groupingBy(h -> h.getIdLojautilizador().getIdUtilizador().getId()));

        for (Map.Entry<Integer, List<Horario>> entry : horariosPorColaborador.entrySet()) {
            List<Horario> ordenados = entry.getValue().stream()
                    .sorted(Comparator.comparing(Horario::getDataTurno)
                            .thenComparing(h -> h.getIdTurno().getHoraInicio()))
                    .toList();

            for (int i = 1; i < ordenados.size(); i++) {
                Horario anterior = ordenados.get(i - 1);
                Horario atual = ordenados.get(i);
                if (!anterior.getDataTurno().plusDays(1).equals(atual.getDataTurno())) {
                    continue;
                }
                assertTrue(
                        horasDescansoEntreTurnos(anterior, atual) >= 8,
                        "O preenchimento de capacidade nao deve violar o descanso minimo de 8h entre turnos consecutivos."
                );
            }
        }
    }

    @Test
    void preenchimentoNaoUltrapassaMaximoDeCincoDiasPorSemana() {
        GeracaoFixture fixture = criarContextoGeracao("preenchimento-semanal");

        PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        List<Horario> horariosGerados = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta());

        // Agrupa por colaborador → semana → dias trabalhados
        Map<Integer, Map<LocalDate, Set<LocalDate>>> diasPorSemana = new HashMap<>();
        Map<Integer, String> tiposCargo = horariosGerados.stream()
                .filter(h -> h.getIdLojautilizador() != null)
                .filter(h -> h.getIdLojautilizador().getIdUtilizador() != null)
                .filter(h -> h.getIdLojautilizador().getIdCargo() != null)
                .collect(Collectors.toMap(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId(),
                        h -> h.getIdLojautilizador().getIdCargo().getTipo().toLowerCase(),
                        (a, b) -> a
                ));

        for (Horario h : horariosGerados) {
            if (h.getIdLojautilizador() == null || h.getIdLojautilizador().getIdUtilizador() == null
                    || h.getDataTurno() == null) continue;
            Integer id = h.getIdLojautilizador().getIdUtilizador().getId();
            LocalDate inicioSemana = h.getDataTurno().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            diasPorSemana
                    .computeIfAbsent(id, ignored -> new HashMap<>())
                    .computeIfAbsent(inicioSemana, ignored -> new LinkedHashSet<>())
                    .add(h.getDataTurno());
        }

        for (Map.Entry<Integer, Map<LocalDate, Set<LocalDate>>> entry : diasPorSemana.entrySet()) {
            String tipoCargo = tiposCargo.getOrDefault(entry.getKey(), "");
            // reforco_parttime so trabalha FDS (sabado+domingo = 2 dias/semana max, nunca ultrapassa 5)
            if ("reforco_parttime".equals(tipoCargo)) continue;

            for (Map.Entry<LocalDate, Set<LocalDate>> semana : entry.getValue().entrySet()) {
                assertTrue(
                        semana.getValue().size() <= 5,
                        "Com descanso_semanal=2, nenhum colaborador deve trabalhar mais de 5 dias por semana."
                );
            }
        }
    }

    @Test
    void reforcoFimDeSemanaTrabalhaNoPeloMenosDoisFinsDeSemanaNoMes() {
        GeracaoFixture fixture = criarContextoGeracao("preenchimento-reforco-fds");

        PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        List<Horario> horariosReforco = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta()).stream()
                .filter(h -> h.getIdLojautilizador() != null)
                .filter(h -> h.getIdLojautilizador().getIdCargo() != null)
                .filter(h -> "reforco_parttime".equalsIgnoreCase(h.getIdLojautilizador().getIdCargo().getTipo()))
                .toList();

        assertFalse(horariosReforco.isEmpty(), "O reforco de fim de semana deve ser escalonado.");

        Set<LocalDate> finsDeSemanaDistintos = horariosReforco.stream()
                .map(Horario::getDataTurno)
                .map(data -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)))
                .collect(Collectors.toSet());

        assertTrue(
                finsDeSemanaDistintos.size() >= 2,
                "O reforco deve ser distribuido por pelo menos 2 fins de semana distintos; apenas encontrados: "
                        + finsDeSemanaDistintos.size() + "."
        );

        assertTrue(
                horariosReforco.stream().map(Horario::getDataTurno).allMatch(this::ehFimDeSemana),
                "O reforco de fim de semana nunca deve ser escalado em dias uteis."
        );
    }

    private boolean ehFimDeSemana(LocalDate data) {
        DayOfWeek dow = data.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private long horasDescansoEntreTurnos(Horario anterior, Horario atual) {
        LocalDateTime fimAnterior = anterior.getDataTurno().atTime(anterior.getIdTurno().getHoraFim());
        if (!anterior.getIdTurno().getHoraFim().isAfter(anterior.getIdTurno().getHoraInicio())) {
            fimAnterior = fimAnterior.plusDays(1);
        }
        LocalDateTime inicioAtual = atual.getDataTurno().atTime(atual.getIdTurno().getHoraInicio());
        return Duration.between(fimAnterior, inicioAtual).toHours();
    }
}
