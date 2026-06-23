package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Vista em grelha (semana ou mês) do horário da proposta, no painel passado pelo controller.
 * Converte as {@link HorarioLinha} da proposta no modelo neutro do
 * {@link GrelhaHorarioRenderer} — que desenha a grelha com a coluna de colaboradores
 * congelada — e gere o label de período e o empty-state.
 */
public final class VistaGrelhaHorarioRender {

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern(
            "d MMM", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern(
            "MMMM yyyy", Locale.forLanguageTag("pt-PT"));

    private final VBox grelhaContainer;
    private final ScrollPane grelhaScrollPane;
    private final VBox emptyStateGrelha;
    private final Label lblGrelhaPeriodo;
    private final Consumer<LocalDate> aoAbrirDia;

    public VistaGrelhaHorarioRender(VBox grelhaContainer,
                                    ScrollPane grelhaScrollPane,
                                    VBox emptyStateGrelha,
                                    Label lblGrelhaPeriodo) {
        this(grelhaContainer, grelhaScrollPane, emptyStateGrelha, lblGrelhaPeriodo, null);
    }

    /**
     * @param aoAbrirDia callback opcional invocado ao clicar numa célula/cabeçalho de dia
     *                   (abre o detalhe do dia).
     */
    public VistaGrelhaHorarioRender(VBox grelhaContainer,
                                    ScrollPane grelhaScrollPane,
                                    VBox emptyStateGrelha,
                                    Label lblGrelhaPeriodo,
                                    Consumer<LocalDate> aoAbrirDia) {
        this.grelhaContainer = grelhaContainer;
        this.grelhaScrollPane = grelhaScrollPane;
        this.emptyStateGrelha = emptyStateGrelha;
        this.lblGrelhaPeriodo = lblGrelhaPeriodo;
        this.aoAbrirDia = aoAbrirDia;
    }

    /**
     * Reconstrói a grelha completa. Se {@code linhas} for nulo/vazio, mostra o empty-state
     * e esconde o scroll. Caso contrário, desenha cabeçalho + uma linha por colaborador.
     */
    public void renderizar(boolean vistaSemanais, LocalDate dataInicio,
                           List<HorarioLinha> linhas) {
        renderizar(vistaSemanais, dataInicio, linhas, null);
    }

    /**
     * @param celulasDestacadas chaves (ver {@link GrelhaHorarioRenderer#chaveCelula}) de células
     *                          a destacar por terem sido alteradas manualmente nesta sessão.
     */
    public void renderizar(boolean vistaSemanais, LocalDate dataInicio,
                           List<HorarioLinha> linhas, Set<String> celulasDestacadas) {
        if (grelhaContainer == null) return;

        LocalDate inicio;
        LocalDate fim;
        if (vistaSemanais) {
            inicio = dataInicio.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            fim = inicio.plusDays(6);
        } else {
            YearMonth ym = YearMonth.of(dataInicio.getYear(), dataInicio.getMonth());
            inicio = ym.atDay(1);
            fim = ym.atEndOfMonth();
        }

        atualizarLabelPeriodo(vistaSemanais, inicio, fim, linhas);

        boolean temDados = linhas != null && !linhas.isEmpty();
        alternarEmptyState(temDados);
        if (!temDados) {
            grelhaContainer.getChildren().clear();
            return;
        }

        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) dias.add(d);

        Map<Integer, String> nomesColab = new LinkedHashMap<>();
        Map<Integer, String> cargosColab = new LinkedHashMap<>();
        Map<Integer, Map<LocalDate, GrelhaHorarioRenderer.CelulaTurno>> porColaborador = new LinkedHashMap<>();
        for (HorarioLinha linha : linhas) {
            if (linha == null || linha.data() == null) continue;
            if (linha.data().isBefore(inicio) || linha.data().isAfter(fim)) continue;
            Integer id = linha.idColaborador();
            nomesColab.put(id, linha.colaborador() != null ? linha.colaborador() : "?");
            cargosColab.put(id, linha.cargo() != null ? linha.cargo() : "");
            String horas = (linha.periodo() != null && !"-".equals(linha.periodo())) ? linha.periodo() : null;
            Map<LocalDate, GrelhaHorarioRenderer.CelulaTurno> diaMap =
                    porColaborador.computeIfAbsent(id, k -> new LinkedHashMap<>());
            GrelhaHorarioRenderer.CelulaTurno existente = diaMap.get(linha.data());
            diaMap.put(linha.data(), existente != null
                    ? mesclarCelulas(existente, linha.turno(), horas)
                    : new GrelhaHorarioRenderer.CelulaTurno(linha.turno(), horas));
        }

        if (porColaborador.isEmpty()) {
            alternarEmptyState(false);
            grelhaContainer.getChildren().clear();
            return;
        }

        List<GrelhaHorarioRenderer.LinhaGrelha> linhasGrelha =
                construirLinhasGrelha(nomesColab, cargosColab, porColaborador);

        if (!vistaSemanais) {
            ajustarScrollParaCompacto();
            GrelhaHorarioRenderer.renderizarCompacto(grelhaContainer, dias, linhasGrelha, LocalDate.now(), aoAbrirDia, celulasDestacadas);
        } else {
            restaurarScrollPadrão();
            GrelhaHorarioRenderer.renderizar(grelhaContainer, dias, linhasGrelha, LocalDate.now(), aoAbrirDia, celulasDestacadas);
        }
    }

    private void atualizarLabelPeriodo(boolean vistaSemanais, LocalDate inicio, LocalDate fim,
                                       List<HorarioLinha> linhas) {
        if (lblGrelhaPeriodo == null) return;
        String periodoTexto = vistaSemanais
                ? FORMATO_DIA.format(inicio) + " – " + FORMATO_DIA.format(fim)
                : capitalizar(YearMonth.from(inicio).format(FORMATO_MES));
        long nPessoas = linhas == null ? 0 : linhas.stream()
                .filter(l -> l != null && l.data() != null
                        && !l.data().isBefore(inicio) && !l.data().isAfter(fim))
                .map(HorarioLinha::idColaborador)
                .distinct().count();
        lblGrelhaPeriodo.setText(periodoTexto + (nPessoas > 0 ? "   · " + nPessoas + " pessoas" : ""));
    }

    /** Converte HorarioLinha → LinhaGrelha, filtradas para o intervalo [inicio,fim], ordenadas alfabeticamente. */
    public static List<GrelhaHorarioRenderer.LinhaGrelha> construirLinhasGrelha(
            List<HorarioLinha> linhas, LocalDate inicio, LocalDate fim) {
        Map<Integer, String> nomesColab = new LinkedHashMap<>();
        Map<Integer, String> cargosColab = new LinkedHashMap<>();
        Map<Integer, Map<LocalDate, GrelhaHorarioRenderer.CelulaTurno>> porColab = new LinkedHashMap<>();
        for (HorarioLinha linha : linhas) {
            if (linha == null || linha.data() == null) continue;
            if (linha.data().isBefore(inicio) || linha.data().isAfter(fim)) continue;
            Integer id = linha.idColaborador();
            nomesColab.put(id, linha.colaborador() != null ? linha.colaborador() : "?");
            cargosColab.put(id, linha.cargo() != null ? linha.cargo() : "");
            String horas = (linha.periodo() != null && !"-".equals(linha.periodo())) ? linha.periodo() : null;
            Map<LocalDate, GrelhaHorarioRenderer.CelulaTurno> diaMap =
                    porColab.computeIfAbsent(id, k -> new LinkedHashMap<>());
            GrelhaHorarioRenderer.CelulaTurno existente = diaMap.get(linha.data());
            diaMap.put(linha.data(), existente != null
                    ? mesclarCelulas(existente, linha.turno(), horas)
                    : new GrelhaHorarioRenderer.CelulaTurno(linha.turno(), horas));
        }
        return construirLinhasGrelha(nomesColab, cargosColab, porColab);
    }

    private static List<GrelhaHorarioRenderer.LinhaGrelha> construirLinhasGrelha(
            Map<Integer, String> nomesColab,
            Map<Integer, String> cargosColab,
            Map<Integer, Map<LocalDate, GrelhaHorarioRenderer.CelulaTurno>> porColaborador) {
        List<GrelhaHorarioRenderer.LinhaGrelha> lista = new ArrayList<>();
        for (Map.Entry<Integer, Map<LocalDate, GrelhaHorarioRenderer.CelulaTurno>> entry
                : porColaborador.entrySet()) {
            lista.add(new GrelhaHorarioRenderer.LinhaGrelha(
                    entry.getKey(),
                    nomesColab.get(entry.getKey()),
                    cargosColab.get(entry.getKey()),
                    entry.getValue()));
        }
        lista.sort(Comparator.comparing(l ->
                Normalizer.normalize(l.nome() != null ? l.nome().toLowerCase(Locale.ROOT) : "",
                        Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")));
        return lista;
    }

    private void ajustarScrollParaCompacto() {
        if (grelhaScrollPane == null) return;
        grelhaScrollPane.setFitToWidth(true);
        grelhaScrollPane.setFitToHeight(false);
        grelhaScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        grelhaScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    private void restaurarScrollPadrão() {
        if (grelhaScrollPane == null) return;
        grelhaScrollPane.setFitToWidth(true);
        grelhaScrollPane.setFitToHeight(true);
        grelhaScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        grelhaScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    private void alternarEmptyState(boolean temDados) {
        if (emptyStateGrelha != null) {
            emptyStateGrelha.setVisible(!temDados);
            emptyStateGrelha.setManaged(!temDados);
        }
        if (grelhaScrollPane != null) {
            grelhaScrollPane.setVisible(temDados);
            grelhaScrollPane.setManaged(temDados);
        }
    }

    private static GrelhaHorarioRenderer.CelulaTurno mesclarCelulas(
            GrelhaHorarioRenderer.CelulaTurno existente, String novoTipo, String novasHoras) {
        String tipoE = chaveTurno(existente.tipo());
        String tipoN = chaveTurno(novoTipo);
        String tipoCombinado = combinarTipos(tipoE, tipoN);
        return new GrelhaHorarioRenderer.CelulaTurno(tipoCombinado, combinarHoras(existente.horas(), novasHoras));
    }

    private static String combinarTipos(String a, String b) {
        boolean temManha = "manha".equals(a) || "manha".equals(b)
                || "manha_intermedio".equals(a) || "manha_intermedio".equals(b)
                || "manha_tarde".equals(a) || "manha_tarde".equals(b);
        boolean temIntermedio = "intermedio".equals(a) || "intermedio".equals(b)
                || "tarde".equals(a) || "tarde".equals(b)
                || "manha_intermedio".equals(a) || "manha_intermedio".equals(b)
                || "manha_tarde".equals(a) || "manha_tarde".equals(b)
                || "intermedio_noite".equals(a) || "intermedio_noite".equals(b)
                || "tarde_noite".equals(a) || "tarde_noite".equals(b);
        boolean temNoite = "noite".equals(a) || "noite".equals(b)
                || "intermedio_noite".equals(a) || "intermedio_noite".equals(b)
                || "tarde_noite".equals(a) || "tarde_noite".equals(b);

        if (temManha && temIntermedio && temNoite) return "manha_intermedio_noite";
        if (temManha && temIntermedio) return "manha_intermedio";
        if (temIntermedio && temNoite) return "intermedio_noite";
        if (temManha) return "manha";
        if (temNoite) return "noite";
        return temIntermedio ? "intermedio" : b;
    }

    private static String chaveTurno(String tipo) {
        if (tipo == null || tipo.isBlank()) return "";
        String p = Normalizer.normalize(tipo.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return switch (p) {
            case "manha" -> "manha";
            case "tarde", "intermedio" -> "intermedio";
            case "noite" -> "noite";
            case "manha_tarde", "manha_intermedio" -> "manha_intermedio";
            case "tarde_noite", "intermedio_noite" -> "intermedio_noite";
            case "folga" -> "folga";
            default -> p;
        };
    }

    private static String combinarHoras(String horasExistentes, String horasNovas) {
        LocalTime[] a = intervalo(horasExistentes);
        LocalTime[] b = intervalo(horasNovas);
        if (a == null) return horasNovas;
        if (b == null) return horasExistentes;
        LocalTime inicio = a[0].isBefore(b[0]) ? a[0] : b[0];
        LocalTime fim = a[1].isAfter(b[1]) ? a[1] : b[1];
        return inicio + " - " + fim;
    }

    private static LocalTime[] intervalo(String horas) {
        if (horas == null || horas.isBlank() || "-".equals(horas.trim())) return null;
        String normalizado = horas.trim().replace("–", "-").replace("—", "-");
        String[] partes = normalizado.split("\\s*-\\s*", 2);
        if (partes.length != 2) return null;
        try {
            return new LocalTime[]{LocalTime.parse(partes[0].trim()), LocalTime.parse(partes[1].trim())};
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        return texto.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-PT"))
                + texto.substring(1);
    }
}
