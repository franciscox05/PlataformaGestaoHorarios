package com.example.projeto2.DESKTOP.support;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Renderizador único da grelha de horário "colaboradores × dias", partilhado pela Home
 * (horário publicado) e pela Geração de Horários (proposta).
 *
 * <p>Constrói a grelha com <b>coluna de colaboradores congelada</b>: a identificação de
 * cada colaborador fica numa coluna fixa à esquerda, e apenas os dias deslizam num
 * {@link ScrollPane} horizontal próprio — ao percorrer um mês inteiro, nunca se perde
 * de vista quem é o colaborador de cada linha.
 *
 * <p>Opcionalmente, cada célula de dia (e o cabeçalho do dia) é clicável e invoca o
 * callback {@code aoAbrirDia} — usado para abrir o diálogo "Detalhe do dia".
 *
 * <p>Reutiliza as classes CSS {@code grelha-*} de {@code dashboard.css}.
 */
public final class GrelhaHorarioRenderer {

    /** Conteúdo de uma célula: tipo do turno (manhã/tarde/…) e horas ("09:00 - 15:00"). */
    public record CelulaTurno(String tipo, String horas) {
    }

    /** Uma linha da grelha: colaborador + mapa dia→célula. */
    public record LinhaGrelha(Integer idColaborador,
                              String nome,
                              String cargo,
                              Map<LocalDate, CelulaTurno> celulas) {
    }

    private static final String[] AVATAR_CORES = {
            "#dc2626", "#2563eb", "#7c3aed", "#059669",
            "#d97706", "#db2777", "#0891b2", "#65a30d",
            "#0f172a", "#9333ea", "#ea580c", "#0284c7"
    };

    private static final double ALTURA_CABECALHO = 56.0;
    private static final double ALTURA_LINHA = 72.0;
    private static final double NOME_COL_COMPACTA = 190.0;

    private GrelhaHorarioRenderer() {
    }

    /**
     * Reconstrói a grelha dentro de {@code container}. Se não houver linhas, limpa-o
     * e não desenha nada (o empty-state é responsabilidade do chamador).
     *
     * @param aoAbrirDia callback opcional invocado ao clicar numa célula/cabeçalho de dia.
     */
    public static void renderizar(VBox container,
                                  List<LocalDate> dias,
                                  List<LinhaGrelha> linhas,
                                  LocalDate hoje,
                                  Consumer<LocalDate> aoAbrirDia) {
        if (container == null) {
            return;
        }
        container.getChildren().clear();
        if (dias == null || dias.isEmpty() || linhas == null || linhas.isEmpty()) {
            return;
        }

        // ── Coluna fixa: cabeçalho "COLABORADOR" + uma célula por colaborador ──
        VBox colunaFixa = new VBox();
        colunaFixa.getStyleClass().add("grelha-col-fixa");

        HBox headerColabBox = new HBox();
        headerColabBox.getStyleClass().add("grelha-header-row");
        headerColabBox.setAlignment(Pos.CENTER_LEFT);
        fixarAltura(headerColabBox, ALTURA_CABECALHO);
        Label headerColab = new Label("COLABORADOR");
        headerColab.getStyleClass().add("grelha-header-colab");
        headerColabBox.getChildren().add(headerColab);
        colunaFixa.getChildren().add(headerColabBox);

        // ── Parte deslizante: cabeçalho dos dias + linhas de células ──
        VBox parteDias = new VBox();
        parteDias.getStyleClass().add("grelha-dias-conteudo");

        HBox headerDias = new HBox();
        headerDias.getStyleClass().add("grelha-header-row");
        headerDias.setAlignment(Pos.CENTER_LEFT);
        fixarAltura(headerDias, ALTURA_CABECALHO);
        for (LocalDate dia : dias) {
            headerDias.getChildren().add(construirCabecalhoDia(dia, hoje, aoAbrirDia));
        }
        parteDias.getChildren().add(headerDias);

        boolean alternado = false;
        int indice = 0;
        for (LinhaGrelha linha : linhas) {
            String corAvatar = corPara(linha.idColaborador(), indice);
            indice++;

            HBox celulaColab = construirCelulaColaborador(linha.nome(), linha.cargo(), corAvatar);
            celulaColab.getStyleClass().add("grelha-employee-row");
            fixarAltura(celulaColab, ALTURA_LINHA);

            HBox linhaDias = new HBox();
            linhaDias.getStyleClass().add("grelha-employee-row");
            linhaDias.setAlignment(Pos.CENTER_LEFT);
            fixarAltura(linhaDias, ALTURA_LINHA);

            if (alternado) {
                celulaColab.getStyleClass().add("grelha-employee-row-alt");
                linhaDias.getStyleClass().add("grelha-employee-row-alt");
            }
            alternado = !alternado;

            for (LocalDate dia : dias) {
                CelulaTurno celula = linha.celulas() != null ? linha.celulas().get(dia) : null;
                linhaDias.getChildren().add(construirCelulaDia(celula, dia, hoje, aoAbrirDia));
            }

            sincronizarHover(celulaColab, linhaDias);

            colunaFixa.getChildren().add(celulaColab);
            parteDias.getChildren().add(linhaDias);
        }

        ScrollPane scrollDias = new ScrollPane(parteDias);
        scrollDias.getStyleClass().add("grelha-dias-scroll");
        scrollDias.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollDias.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollDias.setFitToHeight(false);
        scrollDias.setFitToWidth(false);
        scrollDias.setPannable(true);
        HBox.setHgrow(scrollDias, Priority.ALWAYS);

        HBox raiz = new HBox(colunaFixa, scrollDias);
        raiz.getStyleClass().add("grelha-raiz");
        container.getChildren().add(raiz);
    }

    // ── Construção de nós ───────────────────────────────────────────────────

    private static VBox construirCabecalhoDia(LocalDate dia, LocalDate hoje, Consumer<LocalDate> aoAbrirDia) {
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
            circulo.getStyleClass().add("grelha-header-hoje-circulo");
            circulo.setMinSize(34, 34);
            circulo.setPrefSize(34, 34);
            circulo.setMaxSize(34, 34);
            Label lblNumHoje = new Label(String.valueOf(dia.getDayOfMonth()));
            lblNumHoje.getStyleClass().add("grelha-header-hoje-num");
            circulo.getChildren().add(lblNumHoje);
            hDia.getChildren().addAll(lblSem, circulo);
        } else {
            Label lblNum = new Label(String.valueOf(dia.getDayOfMonth()));
            lblNum.getStyleClass().add("grelha-header-dia-num");
            hDia.getChildren().addAll(lblSem, lblNum);
        }

        if (aoAbrirDia != null) {
            hDia.getStyleClass().add("grelha-dia-clicavel");
            hDia.setOnMouseClicked(event -> aoAbrirDia.accept(dia));
        }
        return hDia;
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

    private static StackPane construirCelulaDia(CelulaTurno celula,
                                                LocalDate dia,
                                                LocalDate hoje,
                                                Consumer<LocalDate> aoAbrirDia) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("grelha-dia-cell");

        boolean fds = dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (fds) {
            cell.getStyleClass().add("grelha-dia-cell-fim-semana");
        }
        if (dia.equals(hoje)) {
            cell.getStyleClass().add("grelha-dia-cell-hoje");
        }

        String tipoTurno = celula != null ? celula.tipo() : null;
        String horasTurno = celula != null ? celula.horas() : null;

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

        if (aoAbrirDia != null) {
            cell.getStyleClass().add("grelha-dia-clicavel");
            cell.setOnMouseClicked(event -> aoAbrirDia.accept(dia));
        }
        return cell;
    }

    /** Realce simultâneo da célula fixa e da linha de dias quando o rato passa por cima. */
    private static void sincronizarHover(HBox celulaColab, HBox linhaDias) {
        Runnable entrar = () -> {
            adicionarClasse(celulaColab, "grelha-row-hover");
            adicionarClasse(linhaDias, "grelha-row-hover");
        };
        Runnable sair = () -> {
            celulaColab.getStyleClass().remove("grelha-row-hover");
            linhaDias.getStyleClass().remove("grelha-row-hover");
        };
        celulaColab.setOnMouseEntered(event -> entrar.run());
        celulaColab.setOnMouseExited(event -> sair.run());
        linhaDias.setOnMouseEntered(event -> entrar.run());
        linhaDias.setOnMouseExited(event -> sair.run());
    }

    private static void adicionarClasse(Region nodo, String classe) {
        if (!nodo.getStyleClass().contains(classe)) {
            nodo.getStyleClass().add(classe);
        }
    }

    private static void fixarAltura(Region nodo, double altura) {
        nodo.setMinHeight(altura);
        nodo.setPrefHeight(altura);
        nodo.setMaxHeight(altura);
    }

    /** Cor de avatar estável por colaborador (deriva do id; índice como fallback). */
    private static String corPara(Integer idColaborador, int indice) {
        int base = idColaborador != null ? Math.abs(idColaborador) : indice;
        return AVATAR_CORES[base % AVATAR_CORES.length];
    }

    // ── Vista compacta (mês sem scroll horizontal) ─────────────────────────

    /**
     * Grelha compacta para vista mensal: todos os dias visíveis de uma vez, sem scroll
     * horizontal. Cada dia é representado por um pequeno tile colorido com a inicial do
     * turno. Os colaboradores são ordenados alfabeticamente.
     */
    public static void renderizarCompacto(VBox container,
                                          List<LocalDate> dias,
                                          List<LinhaGrelha> linhas,
                                          LocalDate hoje,
                                          Consumer<LocalDate> aoAbrirDia) {
        if (container == null) return;
        container.getChildren().clear();
        if (dias == null || dias.isEmpty() || linhas == null || linhas.isEmpty()) return;

        List<LinhaGrelha> ordenadas = new ArrayList<>(linhas);
        ordenadas.sort(Comparator.comparing(l ->
                Normalizer.normalize(l.nome() != null ? l.nome().toLowerCase(Locale.ROOT) : "",
                        Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")));

        // ── Cabeçalho ─────────────────────────────────────────────────────
        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("grelha-compacta-header");

        Label headerNome = new Label("COLABORADOR");
        headerNome.getStyleClass().add("grelha-compacta-header-nome");
        fixarLargura(headerNome, NOME_COL_COMPACTA);
        headerRow.getChildren().add(headerNome);

        for (LocalDate dia : dias) {
            boolean fds = dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
            VBox hDia = new VBox(1);
            hDia.setAlignment(Pos.CENTER);
            hDia.getStyleClass().add("grelha-compacta-header-dia");
            if (fds) hDia.getStyleClass().add("grelha-compacta-header-dia-fds");

            Label lblSem = new Label(diaSemanaAbrev(dia.getDayOfWeek()).substring(0, 1).toUpperCase(Locale.ROOT));
            lblSem.getStyleClass().add("grelha-compacta-dia-sem");
            Label lblNum = new Label(String.valueOf(dia.getDayOfMonth()));
            lblNum.getStyleClass().add("grelha-compacta-dia-num");
            hDia.getChildren().addAll(lblSem, lblNum);
            HBox.setHgrow(hDia, Priority.ALWAYS);
            hDia.setMaxWidth(Double.MAX_VALUE);

            if (aoAbrirDia != null) {
                hDia.getStyleClass().add("grelha-compacta-header-dia-clicavel");
                hDia.setOnMouseClicked(e -> aoAbrirDia.accept(dia));
            }
            headerRow.getChildren().add(hDia);
        }
        container.getChildren().add(headerRow);

        // ── Linhas por colaborador ─────────────────────────────────────────
        boolean alternado = false;
        int indice = 0;
        for (LinhaGrelha linha : ordenadas) {
            String corAvatar = corPara(linha.idColaborador(), indice++);
            HBox row = new HBox();
            row.getStyleClass().add("grelha-compacta-row");
            if (alternado) row.getStyleClass().add("grelha-compacta-row-alt");
            alternado = !alternado;

            // Célula do nome
            StackPane avatar = new StackPane();
            avatar.getStyleClass().add("grelha-compacta-avatar");
            avatar.setStyle("-fx-background-color: " + corAvatar + ";");
            Label lblIni = new Label(gerarIniciais(linha.nome()));
            lblIni.getStyleClass().add("grelha-compacta-avatar-iniciais");
            avatar.getChildren().add(lblIni);

            Label lblNome = new Label(linha.nome() != null ? linha.nome() : "?");
            lblNome.getStyleClass().add("grelha-compacta-nome");
            lblNome.setMaxWidth(NOME_COL_COMPACTA - 42);

            HBox nomeCell = new HBox(6, avatar, lblNome);
            nomeCell.setAlignment(Pos.CENTER_LEFT);
            fixarLargura(nomeCell, NOME_COL_COMPACTA);
            nomeCell.getStyleClass().add("grelha-compacta-nome-cell");
            row.getChildren().add(nomeCell);

            // Tiles de dia
            for (LocalDate dia : dias) {
                CelulaTurno celula = linha.celulas() != null ? linha.celulas().get(dia) : null;
                StackPane tile = construirTileCompacto(celula, dia, hoje, aoAbrirDia);
                HBox.setHgrow(tile, Priority.ALWAYS);
                tile.setMaxWidth(Double.MAX_VALUE);
                row.getChildren().add(tile);
            }
            container.getChildren().add(row);
        }
    }

    private static StackPane construirTileCompacto(CelulaTurno celula,
                                                   LocalDate dia,
                                                   LocalDate hoje,
                                                   Consumer<LocalDate> aoAbrirDia) {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("grelha-compacta-tile");
        boolean fds = dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (fds)         tile.getStyleClass().add("grelha-compacta-tile-fds");
        if (dia.equals(hoje)) tile.getStyleClass().add("grelha-compacta-tile-hoje");

        String tipo  = celula != null ? celula.tipo() : null;
        String chave = turnoChave(tipo != null ? tipo : "folga");
        boolean ehFolga = celula == null || "folga".equals(chave);

        StackPane pilula = new StackPane();
        pilula.getStyleClass().addAll("grelha-compacta-pilula", "grelha-compacta-pilula-" + chave);

        Label letra = new Label(turnoLetraCompacta(tipo));
        letra.getStyleClass().addAll("grelha-compacta-tile-letra", "grelha-compacta-tile-letra-" + chave);
        pilula.getChildren().add(letra);
        tile.getChildren().add(pilula);

        if (!ehFolga && celula != null && celula.horas() != null) {
            String tooltipTxt = turnoNomeDisplay(tipo) + "\n" + celula.horas();
            Tooltip tooltip = new Tooltip(tooltipTxt);
            tooltip.setShowDelay(Duration.millis(400));
            Tooltip.install(tile, tooltip);
        }

        if (aoAbrirDia != null) {
            tile.getStyleClass().add("grelha-compacta-tile-clicavel");
            tile.setOnMouseClicked(e -> aoAbrirDia.accept(dia));
        }
        return tile;
    }

    private static void fixarLargura(Region nodo, double largura) {
        nodo.setMinWidth(largura);
        nodo.setPrefWidth(largura);
        nodo.setMaxWidth(largura);
    }

    private static String turnoLetraCompacta(String tipo) {
        if (tipo == null) return "–";
        return switch (turnoChave(tipo)) {
            case "manha"      -> "M";
            case "tarde"      -> "T";
            case "noite"      -> "N";
            case "intermedio" -> "I";
            case "folga"      -> "–";
            default           -> tipo.isBlank() ? "?" : tipo.substring(0, 1).toUpperCase(Locale.ROOT);
        };
    }

    // ── Formatadores puros ──────────────────────────────────────────────────

    static String turnoChave(String tipo) {
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

    static String turnoNomeDisplay(String tipo) {
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
    static String formatarHorasGrelha(String horas) {
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

    static String diaSemanaAbrev(DayOfWeek dow) {
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

    static String gerarIniciais(String nome) {
        if (nome == null || nome.isBlank()) {
            return "?";
        }
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase(Locale.ROOT);
        }
        return (String.valueOf(partes[0].charAt(0)) + partes[partes.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }
}
