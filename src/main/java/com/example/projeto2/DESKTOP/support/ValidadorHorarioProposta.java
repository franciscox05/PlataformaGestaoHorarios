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

    private static final DateTimeFormatter HORA_FMT    = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA_FMT    = DateTimeFormatter.ofPattern("d 'de' MMM", Locale.forLanguageTag("pt-PT"));

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
        categorias.add(validarFinsDeSemanaLivres(porColaborador, regras));
        if (regras.exigirChefiaAoSabado()) {
            categorias.add(validarChefiaAoSabado(linhas));
        }
        // Nota: as folgas preferidas e demais preferências (turno, colegas, carga horária)
        // são analisadas por AnaliseCumprimentoHorario — aqui ficam só as regras obrigatórias.

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
            Map<LocalDate, java.util.Set<LocalDate>> porSemana = new java.util.LinkedHashMap<>();
            for (HorarioLinha l : turnos) {
                LocalDate inicioSemana = l.data().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                porSemana.computeIfAbsent(inicioSemana, k -> new java.util.LinkedHashSet<>()).add(l.data());
            }
            for (Map.Entry<LocalDate, java.util.Set<LocalDate>> entry : porSemana.entrySet()) {
                int diasUnicos = entry.getValue().size();
                if (diasUnicos > maxDiasTrabalhados) {
                    violacoes.add(String.format("%s: semana de %s — %d dias trabalhados (máx. %d)",
                            turnos.getFirst().colaborador(),
                            DATA_FMT.format(entry.getKey()),
                            diasUnicos, maxDiasTrabalhados));
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

    /**
     * Fins de semana livres: verifica a regra de rotação configurada (janela de N semanas).
     * Dentro de um único mês (4-5 FDS), a violação só é possível quando o mês tem pelo menos
     * tantos fins de semana quanto a janela configurada. Ex.: janela=7 e mês com 5 FDS nunca
     * viola (5 < 7) — o trabalhador pode compensar no mês seguinte.
     *
     * <p>Isenta gerência/subgerência e reforço parttime.
     */
    private static ValidacaoHorarioResultado.CategoriaValidacao validarFinsDeSemanaLivres(
            Map<Integer, List<HorarioLinha>> porColaborador, CriteriosGeracao regras) {

        // Fins de semana (âncora = sábado) existentes no período.
        java.util.Set<LocalDate> todosFds = porColaborador.values().stream()
                .flatMap(List::stream)
                .filter(l -> l.data().getDayOfWeek() == DayOfWeek.SATURDAY
                        || l.data().getDayOfWeek() == DayOfWeek.SUNDAY)
                .map(l -> l.data().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        int totalFds = todosFds.size();
        int janela = regras.janelaRotacaoFimDeSemana();

        List<String> violacoes = new ArrayList<>();
        // Só é possível violar a regra de rotação num único mês se o mês tiver
        // pelo menos tantos FDS quanto a janela configurada.
        if (totalFds >= 2 && totalFds >= janela) {
            for (List<HorarioLinha> turnos : porColaborador.values()) {
                if (turnos.isEmpty()) continue;
                String nome = turnos.getFirst().colaborador();
                String cargo = turnos.getFirst().cargo();
                if (cargo != null && (cargo.toLowerCase(Locale.ROOT).contains("gerente")
                        || cargo.toLowerCase(Locale.ROOT).contains("subgerente")
                        || cargo.toLowerCase(Locale.ROOT).contains("reforco_parttime")
                        || cargo.toLowerCase(Locale.ROOT).contains("reforço_parttime")
                        || cargo.toLowerCase(Locale.ROOT).contains("reforço")
                        || cargo.toLowerCase(Locale.ROOT).contains("reforco"))) {
                    continue;
                }
                long fdsTrabalhados = turnos.stream()
                        .filter(l -> l.data().getDayOfWeek() == DayOfWeek.SATURDAY
                                || l.data().getDayOfWeek() == DayOfWeek.SUNDAY)
                        .map(l -> l.data().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)))
                        .distinct().count();
                if (fdsTrabalhados >= totalFds) {
                    violacoes.add(String.format(
                            "%s: trabalhou os %d fins de semana do período sem folga (janela configurada: %d sem.)",
                            nome, totalFds, janela));
                }
            }
        }

        String titulo = "Fins de semana livres (rotação: 1 livre/" + janela + " sem.)";
        String resumo;
        if (totalFds < 2) {
            resumo = "Período com menos de 2 fins de semana — sem rotação a verificar";
        } else if (totalFds < janela) {
            resumo = "Período com " + totalFds + " FDS < janela de " + janela
                    + " sem. — nenhum colaborador pode violar a regra neste mês";
        } else {
            resumo = violacoes.isEmpty()
                    ? "Todos os colaboradores têm pelo menos um fim de semana livre no período"
                    : violacoes.size() + " colaborador(es) sem nenhum fim de semana livre";
        }
        return new ValidacaoHorarioResultado.CategoriaValidacao(
                titulo,
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
