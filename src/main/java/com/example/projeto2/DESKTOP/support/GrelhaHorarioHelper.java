package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Horario;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Desenha a grelha de horário "colaboradores × dias" (cards coloridos por tipo de turno,
 * avatares com iniciais, "Folga" nos dias vazios) a partir de uma lista de {@link Horario}.
 *
 * <p>Reutiliza as classes CSS {@code grelha-*} já definidas em {@code dashboard.css}. A mesma
 * grelha é usada na página "Geração de Horários"; este helper permite reaproveitá-la no Painel
 * a partir do horário publicado da loja.</p>
 */
public final class GrelhaHorarioHelper {

    private GrelhaHorarioHelper() {
    }

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] AVATAR_CORES = {
        "#dc2626", "#2563eb", "#7c3aed", "#059669",
        "#d97706", "#db2777", "#0891b2", "#65a30d",
        "#0f172a", "#9333ea", "#ea580c", "#0284c7"
    };

    /**
     * Preenche {@code container} com a grelha do mês {@code periodo}. Cada colaborador presente
     * em {@code horarios} gera uma linha; cada dia do mês gera uma célula.
     */
    public static void preencher(VBox container, YearMonth periodo, List<Horario> horarios, LocalDate hoje) {
        if (container == null) {
            return;
        }
        container.getChildren().clear();

        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();

        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            dias.add(d);
        }

        // Agrupar por colaborador: id → nome, cargo, mapa data→Horario (ordem de aparecimento)
        Map<Integer, String> nomes = new LinkedHashMap<>();
        Map<Integer, String> cargos = new LinkedHashMap<>();
        Map<Integer, Map<LocalDate, Horario>> porColaborador = new LinkedHashMap<>();

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
                porColaborador.computeIfAbsent(id, k -> new LinkedHashMap<>()).put(h.getDataTurno(), h);
            }
        }

        // Cabeçalho (COLABORADOR + dias)
        container.getChildren().add(construirCabecalho(dias, hoje));

        // Uma linha por colaborador
        boolean alternado = false;
        int corIdx = 0;
        for (Map.Entry<Integer, Map<LocalDate, Horario>> entry : porColaborador.entrySet()) {
            Integer id = entry.getKey();
            Map<LocalDate, Horario> diaParaHorario = entry.getValue();

            HBox empRow = new HBox();
            empRow.getStyleClass().add("grelha-employee-row");
            if (alternado) {
                empRow.getStyleClass().add("grelha-employee-row-alt");
            }
            alternado = !alternado;

            String corAvatar = AVATAR_CORES[corIdx % AVATAR_CORES.length];
            corIdx++;
            empRow.getChildren().add(construirCelulaColaborador(nomes.get(id), cargos.get(id), corAvatar));

            for (LocalDate dia : dias) {
                Horario h = diaParaHorario.get(dia);
                String tipo = (h != null && h.getIdTurno() != null) ? h.getIdTurno().getTipo() : null;
                String horas = horasDe(h);
                empRow.getChildren().add(construirCelulaDia(tipo, horas, dia.getDayOfWeek(), dia.equals(hoje)));
            }
            container.getChildren().add(empRow);
        }
    }

    // ── Construção de nós ───────────────────────────────────────────────────

    private static HBox construirCabecalho(List<LocalDate> dias, LocalDate hoje) {
        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("grelha-header-row");

        Label headerColab = new Label("COLABORADOR");
        headerColab.getStyleClass().add("grelha-header-colab");
        headerRow.getChildren().add(headerColab);

        for (LocalDate dia : dias) {
            VBox hDia = new VBox();
            hDia.getStyleClass().add("grelha-header-dia");
            hDia.setAlignment(Pos.CENTER);
            hDia.setSpacing(2);

            boolean fds = dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (fds) {
                hDia.getStyleClass().add("grelha-header-dia-fim-semana");
            }

            Label lblSem = new Label(diaSemanaAbrev(dia.getDayOfWeek()).toUpperCase(Locale.ROOT));
            lblSem.getStyleClass().add("grelha-header-dia-sem");

            if (dia.equals(hoje)) {
                StackPane circulo = new StackPane();
                circulo.setMinSize(34, 34);
                circulo.setPrefSize(34, 34);
                circulo.setMaxSize(34, 34);
                circulo.setStyle("-fx-background-color: #dc2626; -fx-background-radius: 50%;");
                Label lblNumHoje = new Label(String.valueOf(dia.getDayOfMonth()));
                lblNumHoje.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: white;");
                circulo.getChildren().add(lblNumHoje);
                hDia.getChildren().addAll(lblSem, circulo);
            } else {
                Label lblNum = new Label(String.valueOf(dia.getDayOfMonth()));
                lblNum.getStyleClass().add("grelha-header-dia-num");
                hDia.getChildren().addAll(lblSem, lblNum);
            }
            headerRow.getChildren().add(hDia);
        }
        return headerRow;
    }

    private static HBox construirCelulaColaborador(String nome, String cargo, String corAvatar) {
        HBox cell = new HBox(10);
        cell.getStyleClass().add("grelha-employee-info");
        cell.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("grelha-avatar");
        avatar.setStyle("-fx-background-color: " + corAvatar + ";");
        Label lblIniciais = new Label(gerarIniciais(nome));
        lblIniciais.getStyleClass().add("grelha-avatar-iniciais");
        avatar.getChildren().add(lblIniciais);

        VBox nomeBox = new VBox(2);
        nomeBox.setAlignment(Pos.CENTER_LEFT);
        Label lblNome = new Label(nome != null ? nome : "?");
        lblNome.getStyleClass().add("grelha-employee-nome");
        lblNome.setMaxWidth(128);
        Label lblCargo = new Label(cargo != null ? cargo : "");
        lblCargo.getStyleClass().add("grelha-employee-cargo");
        nomeBox.getChildren().addAll(lblNome, lblCargo);

        cell.getChildren().addAll(avatar, nomeBox);
        return cell;
    }

    private static StackPane construirCelulaDia(String tipoTurno, String horasTurno, DayOfWeek diaSemana, boolean eHoje) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("grelha-dia-cell");

        boolean fds = diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY;
        if (fds) {
            cell.getStyleClass().add("grelha-dia-cell-fim-semana");
        }
        if (eHoje) {
            cell.setStyle("-fx-background-color: #fff5f5;");
        }

        // Sem turno neste dia → mostrar Folga
        if (tipoTurno == null || tipoTurno.isBlank() || "-".equals(tipoTurno)) {
            tipoTurno = "Folga";
            horasTurno = null;
        }

        String chave = turnoChave(tipoTurno);

        VBox card = new VBox(3);
        card.getStyleClass().addAll("grelha-turno-card", "grelha-turno-card-" + chave);
        card.setAlignment(Pos.CENTER);

        Label lblNome = new Label(turnoNomeDisplay(tipoTurno));
        lblNome.getStyleClass().addAll("grelha-turno-nome", "grelha-turno-nome-" + chave);
        card.getChildren().add(lblNome);

        if (horasTurno != null && !horasTurno.isBlank() && !"folga".equals(chave)) {
            Label lblHora = new Label(formatarHorasGrelha(horasTurno));
            lblHora.getStyleClass().addAll("grelha-turno-hora", "grelha-turno-hora-" + chave);
            card.getChildren().add(lblHora);
        }

        cell.getChildren().add(card);
        return cell;
    }

    // ── Utilitários puros ───────────────────────────────────────────────────

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

    private static String turnoChave(String tipo) {
        if (tipo == null) {
            return "outro";
        }
        String p = Normalizer.normalize(tipo.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return switch (p) {
            case "manha"      -> "manha";
            case "tarde"      -> "tarde";
            case "noite"      -> "noite";
            case "folga"      -> "folga";
            case "intermedio" -> "intermedio";
            default           -> "outro";
        };
    }

    private static String turnoNomeDisplay(String tipo) {
        if (tipo == null) {
            return "–";
        }
        return switch (turnoChave(tipo)) {
            case "manha"      -> "Manhã";
            case "tarde"      -> "Tarde";
            case "noite"      -> "Noite";
            case "folga"      -> "Folga";
            case "intermedio" -> "Interm.";
            default           -> tipo.length() > 8 ? tipo.substring(0, 7) + "." : tipo;
        };
    }

    /** Formata "09:00 - 15:00" → "09-15", mantendo minutos quando não são "00". */
    private static String formatarHorasGrelha(String horas) {
        if (horas == null) {
            return "";
        }
        String s = horas.trim().replace(" ", "").replace("–", "-");
        String[] partes = s.split("-", 2);
        if (partes.length == 2) {
            String p1 = partes[0].endsWith(":00") ? partes[0].replace(":00", "") : partes[0];
            String p2 = partes[1].endsWith(":00") ? partes[1].replace(":00", "") : partes[1];
            return p1 + "-" + p2;
        }
        return s;
    }

    private static String diaSemanaAbrev(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY    -> "Seg";
            case TUESDAY   -> "Ter";
            case WEDNESDAY -> "Qua";
            case THURSDAY  -> "Qui";
            case FRIDAY    -> "Sex";
            case SATURDAY  -> "Sáb";
            case SUNDAY    -> "Dom";
        };
    }

    private static String gerarIniciais(String nome) {
        if (nome == null || nome.isBlank()) {
            return "?";
        }
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase(Locale.ROOT);
        }
        return (String.valueOf(partes[0].charAt(0)) + partes[partes.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }

    private static String textoOu(String valor, String fallback) {
        return (valor == null || valor.isBlank()) ? fallback : valor;
    }
}
