package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarDuracao;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.normalizarTexto;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.valorOuTraco;

@Component
public class MetricasPlaneamentoCalculator {

    public MetricasPlaneamento calcular(PlaneamentoGerado planeamento, PoliticaOtimizacao politica) {
        Map<Integer, CargaColaborador> cargas = new LinkedHashMap<>();
        for (EstadoColaboradorResumo estado : planeamento.estados()) {
            cargas.put(
                    estado.idUtilizador(),
                    new CargaColaborador(
                            estado.idUtilizador(),
                            valorOuTraco(estado.ligacao().getIdUtilizador().getNome()),
                            estado.minutosAtribuidos,
                            estado.cargaMaximaMinutos,
                            estado.totalFinsDeSemanaTrabalhados
                    )
            );
        }
        return calcular(planeamento.horarios(), cargas, politica);
    }

    public MetricasPlaneamento calcular(List<Horario> horarios, PoliticaOtimizacao politica) {
        Map<Integer, CargaColaborador> cargas = new LinkedHashMap<>();
        Map<Integer, Set<LocalDate>> finsDeSemanaPorColaborador = new HashMap<>();

        for (Horario horario : horarios) {
            if (horario.getIdLojautilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador().getId() == null) {
                continue;
            }

            Integer idColaborador = horario.getIdLojautilizador().getIdUtilizador().getId();
            long minutosTurno = HorarioFormatters.calcularDuracaoEmMinutos(horario.getIdTurno());
            CargaColaborador atual = cargas.get(idColaborador);
            long minutos = (atual != null ? atual.minutos() : 0) + minutosTurno;
            long cargaMaxima = PerfilContratual.fromCargoTipo(
                            horario.getIdLojautilizador().getIdCargo() != null
                                    ? horario.getIdLojautilizador().getIdCargo().getTipo()
                                    : null)
                    .map(PerfilContratual::cargaMensalHorasPadrao)
                    .map(horas -> horas * 60L)
                    .orElse(0L);

            if (horario.getDataTurno() != null && ehFimDeSemana(horario.getDataTurno())) {
                finsDeSemanaPorColaborador
                        .computeIfAbsent(idColaborador, ignored -> new LinkedHashSet<>())
                        .add(inicioFimDeSemana(horario.getDataTurno()));
            }

            cargas.put(
                    idColaborador,
                    new CargaColaborador(
                            idColaborador,
                            valorOuTraco(horario.getIdLojautilizador().getIdUtilizador().getNome()),
                            minutos,
                            cargaMaxima,
                            finsDeSemanaPorColaborador.getOrDefault(idColaborador, Set.of()).size()
                    )
            );
        }
        return calcular(horarios, cargas, politica);
    }

    public MetricasPlaneamento calcular(List<Horario> horarios,
                                        Map<Integer, CargaColaborador> cargas,
                                        PoliticaOtimizacao politica) {
        PoliticaOtimizacao politicaSegura = politica != null ? politica : PoliticaOtimizacao.EQUILIBRIO;
        if (cargas.isEmpty()) {
            return new MetricasPlaneamento(
                    1000, "Sem dados",
                    politicaSegura.nome(), politicaSegura.descricao(),
                    "0h 0m", "0h 0m", 0,
                    "Sem colaboradores elegiveis para avaliar."
            );
        }

        List<Long> minutos = cargas.values().stream().map(CargaColaborador::minutos).toList();
        long minimo = minutos.stream().mapToLong(Long::longValue).min().orElse(0);
        long maximo = minutos.stream().mapToLong(Long::longValue).max().orElse(0);
        double media = minutos.stream().mapToLong(Long::longValue).average().orElse(0);
        long desvioMedio = Math.round(minutos.stream()
                .mapToDouble(valor -> Math.abs(valor - media))
                .average().orElse(0));

        List<Integer> finsDeSemana = cargas.values().stream()
                .map(CargaColaborador::finsDeSemanaTrabalhados).toList();
        int minimoFDS = finsDeSemana.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maximoFDS = finsDeSemana.stream().mapToInt(Integer::intValue).max().orElse(0);
        int amplitudeFinsDeSemana = maximoFDS - minimoFDS;

        long amplitude = maximo - minimo;
        double penIdleStreaks = penalizacaoIdleStreaks(horarios, cargas.keySet());
        int pontuacao = (int) Math.round(
                (amplitude / 30.0) + (desvioMedio / 15.0)
                + (amplitudeFinsDeSemana * 20.0)
                + penalizacaoUtilizacaoContratual(cargas.values())
                + penalizacaoSubutilizacao(cargas.values())
                + penIdleStreaks
        );
        String qualidade = pontuacao <= 60 ? "Alta" : pontuacao <= 120 ? "Boa" : "A rever";

        String resumo = qualidade + " · score " + pontuacao
                + " · desvio medio " + formatarDuracao(desvioMedio)
                + " · amplitude " + formatarDuracao(amplitude)
                + " · amplitude FDS " + amplitudeFinsDeSemana;

        return new MetricasPlaneamento(
                pontuacao, qualidade,
                politicaSegura.nome(), politicaSegura.descricao(),
                formatarDuracao(amplitude), formatarDuracao(desvioMedio),
                amplitudeFinsDeSemana, resumo
        );
    }

    public PoliticaOtimizacao extrairPolitica(String resumoGeracao) {
        String resumoNorm = normalizarTexto(resumoGeracao);
        for (PoliticaOtimizacao politica : PoliticaOtimizacao.values()) {
            if (resumoNorm.contains(normalizarTexto(politica.nome()))) {
                return politica;
            }
        }
        return PoliticaOtimizacao.EQUILIBRIO;
    }

    /**
     * Penaliza planos que desperdiçam a capacidade contratual da equipa: quanto mais
     * abaixo de 100% ficar a utilização média, pior o plano. Mantém comparáveis as
     * alternativas do mesmo mês e faz a recomendação preferir horários completos a
     * horários desfalcados.
     */
    private double penalizacaoSubutilizacao(Collection<CargaColaborador> cargas) {
        List<Double> utilizacoes = cargas.stream()
                .filter(carga -> carga.cargaMaximaMinutos() > 0)
                .map(carga -> Math.min(1.0, carga.minutos() / (double) carga.cargaMaximaMinutos()))
                .toList();
        if (utilizacoes.isEmpty()) return 0;
        double media = utilizacoes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return (1.0 - media) * 80;
    }

    private double penalizacaoUtilizacaoContratual(Collection<CargaColaborador> cargas) {
        List<Double> utilizacoes = cargas.stream()
                .filter(carga -> carga.cargaMaximaMinutos() > 0)
                .map(carga -> carga.minutos() / (double) carga.cargaMaximaMinutos())
                .toList();
        if (utilizacoes.isEmpty()) return 0;
        double media = utilizacoes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return utilizacoes.stream().mapToDouble(valor -> Math.abs(valor - media) * 35).sum();
    }

    private static boolean ehFimDeSemana(LocalDate data) {
        return switch (data.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> true;
            default -> false;
        };
    }

    private static LocalDate inicioFimDeSemana(LocalDate data) {
        return data.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SATURDAY));
    }

    /**
     * Penaliza horários onde algum colaborador tem uma sequência longa de dias sem
     * trabalhar (idle streak). Períodos de inatividade ≥ 4 dias úteis consecutivos
     * (excluindo fins de semana, que têm rotação própria) são sintoma de desfalque.
     * Penalização escala quadraticamente para tornar streaks longos especialmente maus.
     */
    private double penalizacaoIdleStreaks(List<Horario> horarios, Set<Integer> idColaboradores) {
        Map<Integer, TreeSet<LocalDate>> diasTrabalhadosPorColaborador = new HashMap<>();
        LocalDate minData = null;
        LocalDate maxData = null;

        for (Horario h : horarios) {
            if (h.getIdLojautilizador() == null
                    || h.getIdLojautilizador().getIdUtilizador() == null
                    || h.getDataTurno() == null) continue;
            Integer id = h.getIdLojautilizador().getIdUtilizador().getId();
            if (id == null) continue;
            diasTrabalhadosPorColaborador
                    .computeIfAbsent(id, ignored -> new TreeSet<>())
                    .add(h.getDataTurno());
            if (minData == null || h.getDataTurno().isBefore(minData)) minData = h.getDataTurno();
            if (maxData == null || h.getDataTurno().isAfter(maxData)) maxData = h.getDataTurno();
        }
        if (minData == null) return 0;

        double penalizacao = 0;
        for (Integer id : idColaboradores) {
            TreeSet<LocalDate> diasTrabalhados = diasTrabalhadosPorColaborador.getOrDefault(id, new TreeSet<>());
            int maxIdleConsec = calcularMaxIdleConsecutivo(diasTrabalhados, minData, maxData);
            // Apenas penaliza idle ≥ 4 dias; escala quadrática para desincentivar fortemente
            if (maxIdleConsec >= 4) {
                penalizacao += Math.pow(maxIdleConsec - 3, 2) * 5.0;
            }
        }
        return penalizacao;
    }

    private int calcularMaxIdleConsecutivo(TreeSet<LocalDate> diasTrabalhados, LocalDate inicio, LocalDate fim) {
        if (diasTrabalhados.isEmpty()) {
            // Colaborador sem qualquer turno: o streak é todo o período
            return (int) java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1;
        }
        List<LocalDate> ordenados = new ArrayList<>(diasTrabalhados);
        int maxIdle = 0;

        // Gap inicial (do início do período até o primeiro turno)
        int gapInicial = (int) java.time.temporal.ChronoUnit.DAYS.between(inicio, ordenados.getFirst());
        maxIdle = Math.max(maxIdle, gapInicial);

        // Gaps entre turnos consecutivos
        for (int i = 1; i < ordenados.size(); i++) {
            int gap = (int) java.time.temporal.ChronoUnit.DAYS.between(ordenados.get(i - 1), ordenados.get(i)) - 1;
            maxIdle = Math.max(maxIdle, gap);
        }

        // Gap final (do último turno até ao fim do período)
        int gapFinal = (int) java.time.temporal.ChronoUnit.DAYS.between(ordenados.getLast(), fim);
        maxIdle = Math.max(maxIdle, gapFinal);

        return maxIdle;
    }
}
