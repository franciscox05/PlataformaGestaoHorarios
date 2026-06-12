package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.CriteriosGeracao;
import com.example.projeto2.API.Services.geracao.dto.HorarioLinha;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Valida uma lista de linhas de horário face às regras configuradas. */
public final class ValidadorHorarioProposta {

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA_FMT  =
            DateTimeFormatter.ofPattern("d 'de' MMM", Locale.forLanguageTag("pt-PT"));

    private ValidadorHorarioProposta() {}

    public static ValidacaoHorarioResultado validar(List<HorarioLinha> linhas, CriteriosGeracao regras) {
        if (linhas == null || linhas.isEmpty()) return ValidacaoHorarioResultado.vazio();

        Map<Integer, List<HorarioLinha>> porColaborador = linhas.stream()
                .filter(l -> l.idColaborador() != null && l.data() != null)
                .collect(Collectors.groupingBy(HorarioLinha::idColaborador,
                        LinkedHashMap::new, Collectors.toList()));
        porColaborador.values().forEach(lista ->
                lista.sort(Comparator.comparing(HorarioLinha::data)));

        List<ValidacaoHorarioResultado.CategoriaValidacao> categorias = new ArrayList<>();
        categorias.add(validarDescansoMinimo(porColaborador, regras.descansoMinimoHoras()));
        categorias.add(validarDiasConsecutivos(porColaborador, regras.maxDiasConsecutivos()));
        categorias.add(validarFolgasSemanais(porColaborador, regras.descansoSemanalMinimoDias()));
        categorias.add(validarRotacaoFDS(porColaborador, regras.janelaRotacaoFimDeSemana()));
        if (regras.exigirChefiaAoSabado()) {
            categorias.add(validarChefiaAoSabado(linhas));
        }

        boolean hasViolacoes = categorias.stream()
                .anyMatch(c -> !c.semViolacoes());
        return new ValidacaoHorarioResultado(
                hasViolacoes ? ValidacaoHorarioResultado.Estado.VIOLACAO : ValidacaoHorarioResultado.Estado.OK,
                categorias);
    }

    // ── Regras individuais ────────────────────────────────────────────────────

    private static ValidacaoHorarioResultado.CategoriaValidacao validarDescansoMinimo(
            Map<Integer, List<HorarioLinha>> porColaborador, int minimoHoras) {
        List<String> violacoes = new ArrayList<>();
        for (List<HorarioLinha> turnos : porColaborador.values()) {
            for (int i = 1; i < turnos.size(); i++) {
                HorarioLinha ontem = turnos.get(i - 1);
                HorarioLinha hoje  = turnos.get(i);
                if (!ontem.data().plusDays(1).equals(hoje.data())) continue;

                Optional<Long> descanso = calcularDescansoHoras(ontem, hoje);
                if (descanso.isPresent() && descanso.get() < minimoHoras) {
                    violacoes.add(String.format("%s: %s %s → %s %s (%dh de descanso)",
                            ontem.colaborador(),
                            DATA_FMT.format(ontem.data()), nomeTurno(ontem.turno()),
                            DATA_FMT.format(hoje.data()), nomeTurno(hoje.turno()),
                            descanso.get()));
                }
            }
        }
        String resumo = violacoes.isEmpty()
                ? "Mínimo de " + minimoHoras + "h entre turnos respeitado em toda a equipa"
                : violacoes.size() + " par(es) de turnos com menos de " + minimoHoras + "h de descanso";
        return new ValidacaoHorarioResultado.CategoriaValidacao(
                "Descanso entre turnos (≥ " + minimoHoras + "h)",
                violacoes.isEmpty() ? ValidacaoHorarioResultado.Estado.OK : ValidacaoHorarioResultado.Estado.VIOLACAO,
                resumo, violacoes);
    }

    private static ValidacaoHorarioResultado.CategoriaValidacao validarDiasConsecutivos(
            Map<Integer, List<HorarioLinha>> porColaborador, int maximo) {
        List<String> violacoes = new ArrayList<>();
        for (List<HorarioLinha> turnos : porColaborador.values()) {
            int streak = 1;
            LocalDate inicioStreak = turnos.isEmpty() ? null : turnos.get(0).data();
            for (int i = 1; i < turnos.size(); i++) {
                if (turnos.get(i).data().equals(turnos.get(i - 1).data().plusDays(1))) {
                    streak++;
                } else {
                    if (streak > maximo) {
                        violacoes.add(String.format("%s: %d dias seguidos (%s – %s)",
                                turnos.get(0).colaborador(), streak,
                                DATA_FMT.format(inicioStreak),
                                DATA_FMT.format(turnos.get(i - 1).data())));
                    }
                    streak = 1;
                    inicioStreak = turnos.get(i).data();
                }
            }
            if (streak > maximo && !turnos.isEmpty()) {
                violacoes.add(String.format("%s: %d dias seguidos (%s – %s)",
                        turnos.getLast().colaborador(), streak,
                        DATA_FMT.format(inicioStreak),
                        DATA_FMT.format(turnos.getLast().data())));
            }
        }
        String resumo = violacoes.isEmpty()
                ? "Nenhum colaborador excede o máximo de " + maximo + " dias seguidos"
                : violacoes.size() + " situação(ões) com mais de " + maximo + " dias seguidos";
        return new ValidacaoHorarioResultado.CategoriaValidacao(
                "Dias consecutivos (máx. " + maximo + ")",
                violacoes.isEmpty() ? ValidacaoHorarioResultado.Estado.OK : ValidacaoHorarioResultado.Estado.VIOLACAO,
                resumo, violacoes);
    }

    private static ValidacaoHorarioResultado.CategoriaValidacao validarFolgasSemanais(
            Map<Integer, List<HorarioLinha>> porColaborador, int minFolgas) {
        int maxDiasTrabalhados = 7 - minFolgas;
        List<String> violacoes = new ArrayList<>();
        for (List<HorarioLinha> turnos : porColaborador.values()) {
            Map<LocalDate, Long> porSemana = turnos.stream()
                    .collect(Collectors.groupingBy(
                            l -> l.data().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                            Collectors.counting()));
            for (Map.Entry<LocalDate, Long> entry : porSemana.entrySet()) {
                if (entry.getValue() > maxDiasTrabalhados) {
                    violacoes.add(String.format("%s: semana de %s — %d dias trabalhados (máx. %d)",
                            turnos.getFirst().colaborador(),
                            DATA_FMT.format(entry.getKey()),
                            entry.getValue(), maxDiasTrabalhados));
                }
            }
        }
        String resumo = violacoes.isEmpty()
                ? "Todas as semanas têm pelo menos " + minFolgas + " dia(s) de folga"
                : violacoes.size() + " semana(s) com menos de " + minFolgas + " folgas";
        return new ValidacaoHorarioResultado.CategoriaValidacao(
                "Folgas semanais (mín. " + minFolgas + " dias)",
                violacoes.isEmpty() ? ValidacaoHorarioResultado.Estado.OK : ValidacaoHorarioResultado.Estado.VIOLACAO,
                resumo, violacoes);
    }

    private static ValidacaoHorarioResultado.CategoriaValidacao validarRotacaoFDS(
            Map<Integer, List<HorarioLinha>> porColaborador, int janelaFDS) {
        List<String> violacoes = new ArrayList<>();
        for (List<HorarioLinha> turnos : porColaborador.values()) {
            String nome = turnos.isEmpty() ? "?" : turnos.getFirst().colaborador();
            String cargo = turnos.isEmpty() ? "" : turnos.getFirst().cargo();
            if (cargo != null && (cargo.toLowerCase(Locale.ROOT).contains("gerente")
                    || cargo.toLowerCase(Locale.ROOT).contains("subgerente")
                    || cargo.toLowerCase(Locale.ROOT).contains("reforco_parttime")
                    || cargo.toLowerCase(Locale.ROOT).contains("reforço_parttime"))) {
                continue;
            }
            List<LocalDate> sabadosTrabalhados = turnos.stream()
                    .filter(l -> l.data().getDayOfWeek() == DayOfWeek.SATURDAY
                            || l.data().getDayOfWeek() == DayOfWeek.SUNDAY)
                    .map(l -> l.data().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)))
                    .distinct()
                    .sorted()
                    .toList();
            for (int i = 1; i < sabadosTrabalhados.size(); i++) {
                LocalDate anterior = sabadosTrabalhados.get(i - 1);
                LocalDate atual    = sabadosTrabalhados.get(i);
                long semanasEntre  = (atual.toEpochDay() - anterior.toEpochDay()) / 7;
                if (semanasEntre > 0 && semanasEntre < janelaFDS) {
                    violacoes.add(String.format("%s: FDS consecutivos — %s e %s (janela: %d sem.)",
                            nome, DATA_FMT.format(anterior), DATA_FMT.format(atual), janelaFDS));
                }
            }
        }
        String resumo = violacoes.isEmpty()
                ? "Rotação de fins de semana respeitada (janela " + janelaFDS + " semanas)"
                : violacoes.size() + " colaborador(es) com fins de semana demasiado próximos";
        return new ValidacaoHorarioResultado.CategoriaValidacao(
                "Rotação de fins de semana (" + janelaFDS + " sem.)",
                violacoes.isEmpty() ? ValidacaoHorarioResultado.Estado.OK : ValidacaoHorarioResultado.Estado.VIOLACAO,
                resumo, violacoes);
    }

    private static ValidacaoHorarioResultado.CategoriaValidacao validarChefiaAoSabado(
            List<HorarioLinha> linhas) {
        Map<LocalDate, List<HorarioLinha>> porData = linhas.stream()
                .filter(l -> l.data() != null && l.data().getDayOfWeek() == DayOfWeek.SATURDAY)
                .collect(Collectors.groupingBy(HorarioLinha::data, LinkedHashMap::new, Collectors.toList()));

        List<String> violacoes = new ArrayList<>();
        porData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    boolean temChefia = entry.getValue().stream().anyMatch(l -> {
                        String c = l.cargo();
                        if (c == null) return false;
                        String cn = c.toLowerCase(Locale.ROOT);
                        return cn.contains("gerente") || cn.contains("subgerente");
                    });
                    if (!temChefia) {
                        violacoes.add("Sábado " + DATA_FMT.format(entry.getKey()) + " — sem gerente/subgerente");
                    }
                });

        String resumo = violacoes.isEmpty()
                ? "Todos os sábados têm presença de gerente ou subgerente"
                : violacoes.size() + " sábado(s) sem cobertura de chefia";
        return new ValidacaoHorarioResultado.CategoriaValidacao(
                "Chefia ao sábado",
                violacoes.isEmpty() ? ValidacaoHorarioResultado.Estado.OK : ValidacaoHorarioResultado.Estado.VIOLACAO,
                resumo, violacoes);
    }

    // ── Auxiliares ────────────────────────────────────────────────────────────

    private static Optional<Long> calcularDescansoHoras(HorarioLinha ontem, HorarioLinha hoje) {
        Optional<LocalTime[]> timesOntem = parsePeriodo(ontem.periodo());
        Optional<LocalTime[]> timesHoje  = parsePeriodo(hoje.periodo());
        if (timesOntem.isEmpty() || timesHoje.isEmpty()) return Optional.empty();

        LocalTime inicioOntem = timesOntem.get()[0];
        LocalTime fimOntem    = timesOntem.get()[1];
        LocalTime inicioHoje  = timesHoje.get()[0];

        long minFim   = ontem.data().toEpochDay() * 24 * 60 + fimOntem.toSecondOfDay() / 60;
        long minInicio = hoje.data().toEpochDay() * 24 * 60 + inicioHoje.toSecondOfDay() / 60;
        if (fimOntem.isBefore(inicioOntem)) minFim += 24 * 60; // turno de noite cruza meia-noite
        return Optional.of((minInicio - minFim) / 60);
    }

    private static Optional<LocalTime[]> parsePeriodo(String periodo) {
        if (periodo == null || periodo.isBlank()) return Optional.empty();
        String[] partes = periodo.trim().split(" - | – ", 2);
        if (partes.length < 2) return Optional.empty();
        try {
            LocalTime inicio = LocalTime.parse(partes[0].trim(), HORA_FMT);
            LocalTime fim    = LocalTime.parse(partes[1].trim(), HORA_FMT);
            return Optional.of(new LocalTime[]{inicio, fim});
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String nomeTurno(String tipo) {
        if (tipo == null) return "Folga";
        return switch (GrelhaHorarioRenderer.turnoChave(tipo)) {
            case "manha"      -> "Manhã";
            case "tarde"      -> "Tarde";
            case "noite"      -> "Noite";
            case "intermedio" -> "Interm.";
            default           -> tipo;
        };
    }
}
