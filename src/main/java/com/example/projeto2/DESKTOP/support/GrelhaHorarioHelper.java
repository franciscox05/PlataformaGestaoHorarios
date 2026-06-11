package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Horario;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Adaptador da grelha "colaboradores × dias" para o horário publicado da loja
 * ({@link Horario} vindos da base de dados). Agrupa os turnos por colaborador e
 * delega o desenho ao {@link GrelhaHorarioRenderer} (coluna de colaboradores fixa
 * + dias com scroll horizontal próprio).
 */
public final class GrelhaHorarioHelper {

    private GrelhaHorarioHelper() {
    }

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Compatibilidade: grelha sem callback de clique no dia. */
    public static void preencher(VBox container, YearMonth periodo, List<Horario> horarios, LocalDate hoje) {
        preencher(container, periodo, horarios, hoje, null);
    }

    /**
     * Preenche {@code container} com a grelha do mês {@code periodo}. Cada colaborador presente
     * em {@code horarios} gera uma linha; cada dia do mês gera uma célula. Quando
     * {@code aoAbrirDia} é fornecido, clicar numa célula ou no cabeçalho do dia invoca-o.
     */
    public static void preencher(VBox container,
                                 YearMonth periodo,
                                 List<Horario> horarios,
                                 LocalDate hoje,
                                 Consumer<LocalDate> aoAbrirDia) {
        if (container == null) {
            return;
        }

        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();

        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            dias.add(d);
        }

        // Agrupar por colaborador: id → nome, cargo, mapa data→célula
        Map<Integer, String> nomes = new LinkedHashMap<>();
        Map<Integer, String> cargos = new LinkedHashMap<>();
        Map<Integer, Map<LocalDate, GrelhaHorarioRenderer.CelulaTurno>> porColaborador = new LinkedHashMap<>();

        if (horarios != null) {
            for (Horario h : horarios) {
                if (h == null || h.getDataTurno() == null
                        || h.getIdLojautilizador() == null
                        || h.getIdLojautilizador().getIdUtilizador() == null) {
                    continue;
                }
                Integer id = h.getIdLojautilizador().getIdUtilizador().getId();
                if (id == null) {
                    continue;
                }
                nomes.putIfAbsent(id, textoOu(h.getIdLojautilizador().getIdUtilizador().getNome(), "?"));
                cargos.putIfAbsent(id, h.getIdLojautilizador().getIdCargo() != null
                        ? textoOu(h.getIdLojautilizador().getIdCargo().getNome(), "")
                        : "");
                String tipo = h.getIdTurno() != null ? h.getIdTurno().getTipo() : null;
                porColaborador.computeIfAbsent(id, k -> new LinkedHashMap<>())
                        .put(h.getDataTurno(), new GrelhaHorarioRenderer.CelulaTurno(tipo, horasDe(h)));
            }
        }

        // Ordenar colaboradores por nome alfabético (sem acentos)
        List<Map.Entry<Integer, Map<LocalDate, GrelhaHorarioRenderer.CelulaTurno>>> listaOrdenada =
                new ArrayList<>(porColaborador.entrySet());
        listaOrdenada.sort(Comparator.comparing(e ->
                Normalizer.normalize(nomes.getOrDefault(e.getKey(), "").toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")));

        List<GrelhaHorarioRenderer.LinhaGrelha> linhas = new ArrayList<>();
        for (Map.Entry<Integer, Map<LocalDate, GrelhaHorarioRenderer.CelulaTurno>> entry : listaOrdenada) {
            linhas.add(new GrelhaHorarioRenderer.LinhaGrelha(
                    entry.getKey(), nomes.get(entry.getKey()), cargos.get(entry.getKey()), entry.getValue()));
        }

        GrelhaHorarioRenderer.renderizar(container, dias, linhas, hoje, aoAbrirDia);
    }

    /** Constrói "09:00 - 15:00" a partir das horas do turno do {@link Horario}; {@code null} se não houver. */
    private static String horasDe(Horario h) {
        if (h == null || h.getIdTurno() == null) {
            return null;
        }
        LocalTime ini = h.getIdTurno().getHoraInicio();
        LocalTime fim = h.getIdTurno().getHoraFim();
        if (ini == null && fim == null) {
            return null;
        }
        return (ini != null ? ini.format(HORA_FMT) : "--:--")
                + " - "
                + (fim != null ? fim.format(HORA_FMT) : "--:--");
    }

    private static String textoOu(String valor, String fallback) {
        return (valor == null || valor.isBlank()) ? fallback : valor;
    }
}
