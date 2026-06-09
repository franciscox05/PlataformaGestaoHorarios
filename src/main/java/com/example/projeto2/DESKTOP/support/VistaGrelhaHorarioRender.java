package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.GeracaoHorariosService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Desenha a vista em grelha (semana ou mês) do horário no painel passado pelo controller.
 * Encapsula a paleta de cores dos avatares, os cabeçalhos por dia, as células de turno e
 * todos os formatadores específicos da grelha (chave/CSS, nome de display, horas resumidas).
 */
public final class VistaGrelhaHorarioRender {

    private static final String[] AVATAR_CORES = {
            "#dc2626", "#2563eb", "#7c3aed", "#059669",
            "#d97706", "#db2777", "#0891b2", "#65a30d",
            "#0f172a", "#9333ea", "#ea580c", "#0284c7"
    };

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern(
            "d MMM", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern(
            "MMMM yyyy", Locale.forLanguageTag("pt-PT"));

    private final VBox grelhaContainer;
    private final ScrollPane grelhaScrollPane;
    private final VBox emptyStateGrelha;
    private final Label lblGrelhaPeriodo;
    private final Map<Integer, String> coresPorColaborador = new LinkedHashMap<>();

    public VistaGrelhaHorarioRender(VBox grelhaContainer,
                                    ScrollPane grelhaScrollPane,
                                    VBox emptyStateGrelha,
                                    Label lblGrelhaPeriodo) {
        this.grelhaContainer = grelhaContainer;
        this.grelhaScrollPane = grelhaScrollPane;
        this.emptyStateGrelha = emptyStateGrelha;
        this.lblGrelhaPeriodo = lblGrelhaPeriodo;
    }

    /**
     * Reconstrói a grelha completa. Se {@code linhas} for nulo/vazio, mostra o empty-state
     * e esconde o scroll. Caso contrário, desenha cabeçalho + uma linha por colaborador.
     */
    public void renderizar(boolean vistaSemanais, LocalDate dataInicio,
                           List<GeracaoHorariosService.HorarioLinha> linhas) {
        if (grelhaContainer == null) return;
        grelhaContainer.getChildren().clear();

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
        if (!temDados) return;

        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) dias.add(d);

        Map<Integer, String> nomesColab = new LinkedHashMap<>();
        Map<Integer, String> cargosColab = new LinkedHashMap<>();
        Map<Integer, Map<LocalDate, GeracaoHorariosService.HorarioLinha>> porColaborador = new LinkedHashMap<>();
        for (GeracaoHorariosService.HorarioLinha linha : linhas) {
            if (linha == null || linha.data() == null) continue;
            if (linha.data().isBefore(inicio) || linha.data().isAfter(fim)) continue;
            Integer id = linha.idColaborador();
            nomesColab.put(id, linha.colaborador() != null ? linha.colaborador() : "?");
            cargosColab.put(id, linha.cargo() != null ? linha.cargo() : "");
            porColaborador.computeIfAbsent(id, k -> new LinkedHashMap<>()).put(linha.data(), linha);
        }

        if (porColaborador.isEmpty()) {
            alternarEmptyState(false);
            return;
        }

        int corIdx = 0;
        for (Integer id : porColaborador.keySet()) {
            coresPorColaborador.putIfAbsent(id, AVATAR_CORES[corIdx % AVATAR_CORES.length]);
            corIdx++;
        }

        LocalDate hoje = LocalDate.now();
        grelhaContainer.getChildren().add(construirCabecalho(dias, hoje));

        boolean alternado = false;
        for (Map.Entry<Integer, Map<LocalDate, GeracaoHorariosService.HorarioLinha>> entry
                : porColaborador.entrySet()) {
            Integer idColab = entry.getKey();
            Map<LocalDate, GeracaoHorariosService.HorarioLinha> diaParaLinha = entry.getValue();

            HBox empRow = new HBox();
            empRow.getStyleClass().add("grelha-employee-row");
            if (alternado) empRow.getStyleClass().add("grelha-employee-row-alt");
            alternado = !alternado;

            String corAvatar = coresPorColaborador.getOrDefault(idColab, "#6b7280");
            empRow.getChildren().add(construirCelulaColaborador(
                    nomesColab.get(idColab), cargosColab.get(idColab), corAvatar));

            for (LocalDate dia : dias) {
                GeracaoHorariosService.HorarioLinha linha = diaParaLinha.get(dia);
                String tipoTurno = (linha != null && linha.turno() != null) ? linha.turno() : null;
                String horasTurno = (linha != null && linha.periodo() != null
                        && !"-".equals(linha.periodo())) ? linha.periodo() : null;
                empRow.getChildren().add(construirCelulaDia(
                        tipoTurno, horasTurno, dia.getDayOfWeek(), dia.equals(hoje)));
            }
            grelhaContainer.getChildren().add(empRow);
        }
    }

    private void atualizarLabelPeriodo(boolean vistaSemanais, LocalDate inicio, LocalDate fim,
                                       List<GeracaoHorariosService.HorarioLinha> linhas) {
        if (lblGrelhaPeriodo == null) return;
        String periodoTexto = vistaSemanais
                ? FORMATO_DIA.format(inicio) + " – " + FORMATO_DIA.format(fim)
                : capitalizar(YearMonth.from(inicio).format(FORMATO_MES));
        long nPessoas = linhas == null ? 0 : linhas.stream()
                .filter(l -> l != null && l.data() != null
                        && !l.data().isBefore(inicio) && !l.data().isAfter(fim))
                .map(GeracaoHorariosService.HorarioLinha::idColaborador)
                .distinct().count();
        lblGrelhaPeriodo.setText(periodoTexto + (nPessoas > 0 ? "   · " + nPessoas + " pessoas" : ""));
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

    private HBox construirCabecalho(List<LocalDate> dias, LocalDate hoje) {
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
            boolean fds = dia.getDayOfWeek() == DayOfWeek.SATURDAY
                    || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (fds) hDia.getStyleClass().add("grelha-header-dia-fim-semana");

            Label lblSem = new Label(diaSemanaAbrev(dia.getDayOfWeek()).toUpperCase(Locale.ROOT));
            lblSem.getStyleClass().add("grelha-header-dia-sem");

            if (dia.equals(hoje)) {
                StackPane circulo = new StackPane();
                circulo.setMinSize(34, 34); circulo.setPrefSize(34, 34); circulo.setMaxSize(34, 34);
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

    private HBox construirCelulaColaborador(String nome, String cargo, String corAvatar) {
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

    private StackPane construirCelulaDia(String tipoTurno, String horasTurno,
                                         DayOfWeek diaSemana, boolean eHoje) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("grelha-dia-cell");

        boolean fds = diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY;
        if (fds) cell.getStyleClass().add("grelha-dia-cell-fim-semana");
        if (eHoje) cell.setStyle("-fx-background-color: #fff5f5;");

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

    private static String turnoChave(String tipo) {
        if (tipo == null) return "outro";
        String p = Normalizer.normalize(tipo.trim().toLowerCase(), Normalizer.Form.NFD)
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
        if (tipo == null) return "–";
        String chave = turnoChave(tipo);
        return switch (chave) {
            case "manha"      -> "Manhã";
            case "tarde"      -> "Tarde";
            case "noite"      -> "Noite";
            case "folga"      -> "Folga";
            case "intermedio" -> "Interm.";
            default           -> tipo.length() > 8 ? tipo.substring(0, 7) + "." : tipo;
        };
    }

    private static String formatarHorasGrelha(String horas) {
        if (horas == null) return "";
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
        if (nome == null || nome.isBlank()) return "?";
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase(Locale.ROOT);
        }
        return (String.valueOf(partes[0].charAt(0))
                + partes[partes.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        return texto.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-PT"))
                + texto.substring(1);
    }
}
