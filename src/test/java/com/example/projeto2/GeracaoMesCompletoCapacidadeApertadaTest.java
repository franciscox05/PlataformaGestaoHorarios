package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.EstadoColaborador;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do cenário reportado pelo Francisco: equipa de 13 com capacidade contratual
 * (~1900h) acima da necessidade mínima do mês (~1674h), e mesmo assim a geração falhava
 * no dia 30/08 com "carga contratual esgotada". A causa era o top-up interleaved a gastar
 * a carga das pessoas necessárias no fim do mês.
 *
 * <p>Estes testes verificam que, com capacidade suficiente, a geração de um mês completo
 * (agosto 2026 — começa a um sábado, 5 fins de semana) conclui sem falhar e respeita as
 * regras hard.
 */
class GeracaoMesCompletoCapacidadeApertadaTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // Agosto 2026 — exatamente o mês do bug. 1 de agosto é sábado → 5 fins de semana.
    private final LocalDate inicio = LocalDate.of(2026, 8, 1);
    private final LocalDate fim = LocalDate.of(2026, 8, 31);

    @Test
    void geraMesCompletoComCapacidadeApertadaSemFalhar() {
        PedidoGeracao pedido = cenarioRealista();

        // Capacidade total vs necessidade mínima — confirma que o cenário É viável.
        long capacidade = pedido.colaboradores().stream()
                .mapToLong(l -> pedido.cargaMaximaPorColaborador()
                        .getOrDefault(l.getIdUtilizador().getId(), 0L))
                .sum();
        long necessidade = necessidadeMinimaMinutos(pedido);
        assertTrue(capacidade >= necessidade,
                "Pré-condição do teste: capacidade (" + capacidade / 60 + "h) deve cobrir a necessidade ("
                        + necessidade / 60 + "h).");

        List<Horario> horarios = engine.gerar(pedido);
        assertFalse(horarios.isEmpty(), "A geração de um mês completo viável não deve falhar.");

        // Todos os dias do mês têm de cumprir a cobertura mínima (6 = 2 por cada um dos 3 turnos).
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            LocalDate dia = data;
            long cobertura = horarios.stream().filter(h -> dia.equals(h.getDataTurno())).count();
            assertTrue(cobertura >= 6,
                    "O dia " + dia + " ficou com cobertura " + cobertura + " (< mínimo 6).");
        }
    }

    @Test
    void nenhumColaboradorUltrapassaACargaContratual() {
        PedidoGeracao pedido = cenarioRealista();
        List<Horario> horarios = engine.gerar(pedido);

        Map<Integer, Long> minutosPorColaborador = horarios.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId(),
                        Collectors.summingLong(h -> validator.calcularDuracaoEmMinutos(h.getIdTurno()))));

        minutosPorColaborador.forEach((id, minutos) -> {
            long carga = pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L);
            assertTrue(minutos <= carga,
                    "Colaborador " + id + " escalado para " + minutos / 60 + "h, acima da carga de " + carga / 60 + "h.");
        });
    }

    @Test
    void respeitaDescansoDe11hEntreTurnosConsecutivos() {
        PedidoGeracao pedido = cenarioRealista();
        List<Horario> horarios = engine.gerar(pedido);

        Map<Integer, List<Horario>> porColaborador = horarios.stream()
                .collect(Collectors.groupingBy(h -> h.getIdLojautilizador().getIdUtilizador().getId()));

        for (List<Horario> doColaborador : porColaborador.values()) {
            List<Horario> ordenados = doColaborador.stream()
                    .sorted(Comparator.comparing(Horario::getDataTurno))
                    .toList();
            for (int i = 1; i < ordenados.size(); i++) {
                Horario anterior = ordenados.get(i - 1);
                Horario atual = ordenados.get(i);
                if (!anterior.getDataTurno().plusDays(1).equals(atual.getDataTurno())) continue;
                long gap = gapHoras(anterior, atual);
                assertTrue(gap >= 11,
                        "Descanso de " + gap + "h entre " + anterior.getDataTurno() + " e " + atual.getDataTurno()
                                + " viola as 11h legais.");
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * 13 colaboradores: 1 gerente, 1 subgerente, 7 fulltime, 2 parttime, 2 reforço FDS.
     * 3 turnos de 9h sem sobreposição de descanso (08–17, mínimo 2 cada).
     * Capacidade ≈ 1904h; necessidade mínima ≈ 1674h.
     */
    private PedidoGeracao cenarioRealista() {
        List<Lojautilizador> colaboradores = new ArrayList<>();
        colaboradores.add(colaborador(1, "Gerente", "gerente"));
        colaboradores.add(colaborador(2, "Subgerente", "subgerente"));
        for (int i = 3; i <= 9; i++) colaboradores.add(colaborador(i, "FT" + i, "fulltime"));
        colaboradores.add(colaborador(10, "PT10", "parttime"));
        colaboradores.add(colaborador(11, "PT11", "parttime"));
        colaboradores.add(colaborador(12, "Reforco12", "reforco_parttime"));
        colaboradores.add(colaborador(13, "Reforco13", "reforco_parttime"));

        // 3 turnos de 9h. Para não criar conflito noite→manhã hard, mantemos as horas de
        // início afastadas mas todos com 9h de duração.
        Turno manha = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(17, 0));
        Turno inter = turno(2, "intermedio", LocalTime.of(10, 0), LocalTime.of(19, 0));
        Turno tarde = turno(3, "tarde", LocalTime.of(12, 0), LocalTime.of(21, 0));

        Map<Integer, Long> cargas = new HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            String tipo = lig.getIdCargo().getTipo();
            long horas = switch (tipo) {
                case "parttime" -> 96;
                case "reforco_parttime" -> 64;
                default -> 176;
            };
            cargas.put(lig.getIdUtilizador().getId(), horas * 60L);
        }

        return new PedidoGeracao(
                colaboradores,
                List.of(manha, inter, tarde),
                inicio,
                fim,
                Map.of(1, 2, 2, 2, 3, 2), // mínimo 2 por turno → 6/dia
                31,
                11,
                2,
                7,    // rotação longa — o cenário do bug
                true, // chefia obrigatória ao sábado
                Set.of(1, 2),
                cargas,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                null,
                PoliticaOtimizacao.porIndice(0),
                Map.of(),
                Map.of(),
                42L);
    }

    private long necessidadeMinimaMinutos(PedidoGeracao pedido) {
        // 3 turnos × 2 mínimo × 9h × 31 dias
        long porDia = 0;
        for (Turno t : pedido.turnos()) {
            porDia += (long) pedido.minimosPorTurno().getOrDefault(t.getId(), 0)
                    * validator.calcularDuracaoEmMinutos(t);
        }
        // Necessidade real: soma por tipo (manhã/intermédio/tarde são tipos distintos)
        long total = 0;
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) total += porDia;
        return total;
    }

    private long gapHoras(Horario anterior, Horario atual) {
        LocalDateTime fimAnterior = anterior.getDataTurno().atTime(anterior.getIdTurno().getHoraFim());
        if (!anterior.getIdTurno().getHoraFim().isAfter(anterior.getIdTurno().getHoraInicio())) {
            fimAnterior = fimAnterior.plusDays(1);
        }
        LocalDateTime inicioAtual = atual.getDataTurno().atTime(atual.getIdTurno().getHoraInicio());
        return Duration.between(fimAnterior, inicioAtual).toHours();
    }

    private Lojautilizador colaborador(int id, String nome, String tipoCargo) {
        Utilizador u = new Utilizador();
        u.setId(id);
        u.setNome(nome);
        Cargo c = new Cargo();
        c.setNome(nome);
        c.setTipo(tipoCargo);
        Lojautilizador lig = new Lojautilizador();
        lig.setId(id);
        lig.setIdUtilizador(u);
        lig.setIdCargo(c);
        return lig;
    }

    private Turno turno(int id, String tipo, LocalTime horaInicio, LocalTime horaFim) {
        Turno t = new Turno();
        t.setId(id);
        t.setTipo(tipo);
        t.setHoraInicio(horaInicio);
        t.setHoraFim(horaFim);
        return t;
    }
}
