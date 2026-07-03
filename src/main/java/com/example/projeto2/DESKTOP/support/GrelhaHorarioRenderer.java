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
import javafx.stage.Screen;
import javafx.util.Duration;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
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
    private static final double ESPACO_ANTES_SCROLL_HORIZONTAL = 24.0;
    private static final double NOME_COL_COMPACTA = 190.0;

    private GrelhaHorarioRenderer() {
    }

    /** Chave estável (idColaborador|data ISO) usada para marcar uma célula como recém-alterada manualmente. */
    public static String chaveCelula(Integer idColaborador, LocalDate dia) {
        return idColaborador + "|" + dia;
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
        renderizar(container, dias, linhas, hoje, aoAbrirDia, null, null);
    }

    /** Variante ciente do colaborador: ao clicar numa célula passa (dia, idColaborador). */
    public static void renderizar(VBox container,
                                  List<LocalDate> dias,
                                  List<LinhaGrelha> linhas,
                                  LocalDate hoje,
                                  Consumer<LocalDate> aoAbrirDia,
                                  BiConsumer<LocalDate, Integer> aoAbrirDiaPorColaborador) {
        renderizar(container, dias, linhas, hoje, aoAbrirDia, null, aoAbrirDiaPorColaborador);
    }

    /**
     * @param celulasDestacadas chaves (ver {@link #chaveCelula}) de células a destacar
     *                          visualmente por terem sido alteradas manualmente nesta sessão de revisão.
     */
    public static void renderizar(VBox container,
                                  List<LocalDate> dias,
                                  List<LinhaGrelha> linhas,
                                  LocalDate hoje,
                                  Consumer<LocalDate> aoAbrirDia,
                                  Set<String> celulasDestacadas) {
        renderizar(container, dias, linhas, hoje, aoAbrirDia, celulasDestacadas, null);
    }

    public static void renderizar(VBox container,
                                  List<LocalDate> dias,
                                  List<LinhaGrelha> linhas,
                                  LocalDate hoje,
                                  Consumer<LocalDate> aoAbrirDia,
                                  Set<String> celulasDestacadas,
                                  BiConsumer<LocalDate, Integer> aoAbrirDiaPorColaborador) {
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

            final Integer idColab = linha.idColaborador();
            Consumer<LocalDate> cliqueCelula = (aoAbrirDiaPorColaborador != null && idColab != null)
                    ? d -> aoAbrirDiaPorColaborador.accept(d, idColab)
                    : aoAbrirDia;
            for (LocalDate dia : dias) {
                CelulaTurno celula = linha.celulas() != null ? linha.celulas().get(dia) : null;
                boolean destacada = celulasDestacadas != null
                        && celulasDestacadas.contains(chaveCelula(idColab, dia));
                linhaDias.getChildren().add(construirCelulaDia(celula, dia, hoje, cliqueCelula, destacada));
            }

            sincronizarHover(celulaColab, linhaDias);

            colunaFixa.getChildren().add(celulaColab);
            parteDias.getChildren().add(linhaDias);
        }

        Region folgaColunaFixa = new Region();
        fixarAltura(folgaColunaFixa, ESPACO_ANTES_SCROLL_HORIZONTAL);
        colunaFixa.getChildren().add(folgaColunaFixa);

        Region folgaDias = new Region();
        fixarAltura(folgaDias, ESPACO_ANTES_SCROLL_HORIZONTAL);
        parteDias.getChildren().add(folgaDias);

        ScrollPane scrollDias = new ScrollPane(parteDias);
        scrollDias.getStyleClass().add("grelha-dias-scroll");
        scrollDias.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollDias.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollDias.setFitToHeight(false);
        scrollDias.setFitToWidth(false);
        // Sem pan por arrasto: evita "puxar" o conteúdo por baixo do cabeçalho fixo (a
        // desformatação observada). O cabeçalho dos dias vive dentro do mesmo scroll, pelo
        // que o scroll horizontal por barra/roda mantém sempre a grelha alinhada.
        scrollDias.setPannable(false);
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
                                                Consumer<LocalDate> aoAbrirDia,
                                                boolean destacada) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("grelha-dia-cell");

        boolean fds = dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (fds) {
            cell.getStyleClass().add("grelha-dia-cell-fim-semana");
        }
        if (dia.equals(hoje)) {
            cell.getStyleClass().add("grelha-dia-cell-hoje");
        }
        if (destacada) {
            cell.getStyleClass().add("grelha-dia-cell-destaque");
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

    // ── Vista detalhada (ecrã inteiro, chip colorido + horas) ─────────────

    private static final double ALTURA_HEADER_DET  = 58.0;
    private static final double NOME_COL_DET       = 210.0;
    private static final double LARGURA_DIA_DET    = 62.0;
    // Altura de linha calculada dinamicamente em renderizarDetalhado()

    // cores por tipo: [chip-bg, chip-text, cell-bg, cell-bg-fds]
    // Para tipos combinados, chip-bg é um gradiente CSS inline
    private static final Map<String, String[]> CORES_DET = Map.ofEntries(
            Map.entry("manha",       new String[]{"#2563eb", "white",   "#eff6ff", "#dbeafe"}),
            Map.entry("tarde",       new String[]{"#d97706", "white",   "#fffbeb", "#fef3c7"}),
            Map.entry("noite",       new String[]{"#7c3aed", "white",   "#f5f3ff", "#ede9fe"}),
            Map.entry("intermedio",  new String[]{"#d97706", "white",   "#fffbeb", "#fef3c7"}),
            Map.entry("folga",       new String[]{"#6b7280", "#9ca3af", "#f9fafb", "#f3f4f6"}),
            Map.entry("outro",       new String[]{"#374151", "white",   "#f9fafb", "#f3f4f6"}),
            Map.entry("manha_tarde", new String[]{"linear-gradient(from 0% 0% to 100% 0%, #2563eb 50%, #d97706 50%)", "white", "#eff6ff", "#dbeafe"}),
            Map.entry("tarde_noite", new String[]{"linear-gradient(from 0% 0% to 100% 0%, #d97706 50%, #7c3aed 50%)", "white", "#fffbeb", "#fef3c7"}),
            Map.entry("manha_intermedio", new String[]{"linear-gradient(from 0% 0% to 100% 0%, #2563eb 50%, #d97706 50%)", "white", "#eff6ff", "#dbeafe"}),
            Map.entry("intermedio_noite", new String[]{"linear-gradient(from 0% 0% to 100% 0%, #d97706 50%, #7c3aed 50%)", "white", "#fffbeb", "#fef3c7"}),
            Map.entry("manha_intermedio_noite", new String[]{"linear-gradient(from 0% 0% to 100% 0%, #2563eb 0%, #2563eb 33%, #d97706 33%, #d97706 66%, #7c3aed 66%, #7c3aed 100%)", "white", "#f8fafc", "#eef2ff"})
    );

    /**
     * Vista detalhada para a janela em ecrã inteiro: coluna fixa de colaboradores
     * + dias em scroll horizontal. Cada célula mostra um chip colorido com a letra
     * do turno e as horas por baixo. Mais legível que a vista compacta para análise.
     */
    public static void renderizarDetalhado(VBox container,
                                           List<LocalDate> dias,
                                           List<LinhaGrelha> linhas,
                                           LocalDate hoje,
                                           Consumer<LocalDate> aoAbrirDia) {
        renderizarDetalhado(container, dias, linhas, hoje, aoAbrirDia, null);
    }

    public static void renderizarDetalhado(VBox container,
                                           List<LocalDate> dias,
                                           List<LinhaGrelha> linhas,
                                           LocalDate hoje,
                                           Consumer<LocalDate> aoAbrirDia,
                                           Set<String> celulasDestacadas) {
        renderizarDetalhado(container, dias, linhas, hoje, aoAbrirDia, celulasDestacadas,
                Screen.getPrimary().getVisualBounds().getWidth());
    }

    /**
     * Variante que recebe explicitamente a largura disponível para a grelha completa
     * (coluna fixa + dias). Permite reutilizar exatamente esta vista — o estilo do Painel —
     * em contentores mais estreitos que o ecrã (ex.: comparação de alternativas empilhada),
     * dimensionando a largura de cada dia ao espaço real em vez de assumir o ecrã inteiro.
     *
     * @param larguraDisponivel largura em px que a grelha pode ocupar; evita cortar o último dia.
     */
    public static void renderizarDetalhado(VBox container,
                                           List<LocalDate> dias,
                                           List<LinhaGrelha> linhas,
                                           LocalDate hoje,
                                           Consumer<LocalDate> aoAbrirDia,
                                           Set<String> celulasDestacadas,
                                           double larguraDisponivel) {
        renderizarDetalhado(container, dias, linhas, hoje, aoAbrirDia, celulasDestacadas,
                larguraDisponivel, null, -1);
    }

    /**
     * Variante com clique <b>por célula ciente do colaborador</b>: ao clicar numa célula
     * dia×colaborador, {@code aoAbrirDiaPorColaborador} recebe (dia, idColaborador). Permite
     * que o detalhe do dia destaque exatamente o colaborador cuja linha foi clicada. O clique
     * no cabeçalho do dia continua a usar {@code aoAbrirDia} (sem colaborador específico).
     */
    public static void renderizarDetalhado(VBox container,
                                           List<LocalDate> dias,
                                           List<LinhaGrelha> linhas,
                                           LocalDate hoje,
                                           Consumer<LocalDate> aoAbrirDia,
                                           Set<String> celulasDestacadas,
                                           double larguraDisponivel,
                                           BiConsumer<LocalDate, Integer> aoAbrirDiaPorColaborador) {
        renderizarDetalhado(container, dias, linhas, hoje, aoAbrirDia, celulasDestacadas,
                larguraDisponivel, aoAbrirDiaPorColaborador, -1);
    }

    /**
     * Variante que, além da largura, recebe a altura disponível para a grelha (inclui header +
     * linhas). Quando {@code alturaDisponivel > 0}, substitui {@code screenH} no cálculo do
     * chip — imprescindível em modo "Ambas" onde cada grelha ocupa metade do ecrã.
     */
    public static void renderizarDetalhado(VBox container,
                                           List<LocalDate> dias,
                                           List<LinhaGrelha> linhas,
                                           LocalDate hoje,
                                           Consumer<LocalDate> aoAbrirDia,
                                           Set<String> celulasDestacadas,
                                           double larguraDisponivel,
                                           BiConsumer<LocalDate, Integer> aoAbrirDiaPorColaborador,
                                           double alturaDisponivel) {
        if (container == null) return;
        container.getChildren().clear();
        if (dias == null || dias.isEmpty() || linhas == null || linhas.isEmpty()) return;

        List<LinhaGrelha> ordenadas = new ArrayList<>(linhas);
        ordenadas.sort(Comparator.comparing(l ->
                Normalizer.normalize(l.nome() != null ? l.nome().toLowerCase(Locale.ROOT) : "",
                        Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")));

        // ── Largura adaptativa: todos os dias do mês cabem sem scroll horizontal ──
        double screenW = larguraDisponivel > 0
                ? larguraDisponivel
                : Screen.getPrimary().getVisualBounds().getWidth();
        double larguraDia = Math.max(34.0, Math.min(72.0,
                (screenW - NOME_COL_DET) / Math.max(1, dias.size())));

        // Altura fixa por linha: confortável para mostrar avatar + nome + cargo sem sobrepor.
        // O scroll vertical do corpoDias trata do resto — não tentamos comprimir N linhas no ecrã.
        // Em modo "Ambas" (duas grelhas empilhadas) usamos uma altura ligeiramente menor para
        // deixar mais linhas visíveis sem scroll, mas nunca abaixo do mínimo legível.
        double alturaEstimada = alturaDisponivel > 0 ? 52.0 : 64.0;

        boolean mostrarHoras = larguraDia >= 38.0;
        double chipH    = Math.max(18.0, alturaEstimada * 0.36);
        double chipW    = Math.max(24.0, Math.min(larguraDia - 14.0, chipH * 1.45));
        double fntLetra = Math.max(9.0,  Math.min(14.0, chipW * 0.44));
        double fntHoras = Math.max(8.0,  Math.min(9.0, larguraDia * 0.17));

        // ── Cabeçalho "COLABORADOR" — fixo, nunca se desloca (nem vertical nem horizontalmente) ──
        HBox hdrColab = new HBox();
        hdrColab.setAlignment(Pos.CENTER_LEFT);
        fixarAltura(hdrColab, ALTURA_HEADER_DET);
        hdrColab.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; "
                + "-fx-border-width: 0 2 2 0; -fx-padding: 0 12 0 18;");
        Label lblColab = new Label("COLABORADOR");
        lblColab.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        hdrColab.getChildren().add(lblColab);
        fixarLargura(hdrColab, NOME_COL_DET);

        // ── Cabeçalho dos dias — fixo verticalmente; acompanha apenas o scroll
        //    horizontal do corpo (nunca tem barra própria, é só um "espelho") ──
        HBox hdrDias = new HBox();
        hdrDias.setAlignment(Pos.CENTER_LEFT);
        fixarAltura(hdrDias, ALTURA_HEADER_DET);
        hdrDias.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 2 0;");
        for (LocalDate dia : dias) {
            hdrDias.getChildren().add(construirCabecalhoDetalhado(dia, hoje, larguraDia, aoAbrirDia));
        }
        ScrollPane scrollHdrDias = new ScrollPane(hdrDias);
        scrollHdrDias.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollHdrDias.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollHdrDias.setFitToHeight(true);
        scrollHdrDias.setPannable(false);
        scrollHdrDias.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        HBox.setHgrow(scrollHdrDias, Priority.ALWAYS);

        HBox linhaCabecalho = new HBox(hdrColab, scrollHdrDias);
        fixarAltura(linhaCabecalho, ALTURA_HEADER_DET);

        // ── Coluna de colaboradores do corpo — acompanha apenas o scroll vertical
        //    do corpo (sem barra própria); a largura é sempre a mesma do cabeçalho ──
        VBox colunaFixaBody = new VBox();

        // ── Linhas de células — vivem no único ScrollPane com barras visíveis ──
        VBox corpoDias = new VBox();

        boolean alt = false;
        int idx = 0;
        for (LinhaGrelha linha : ordenadas) {
            String corAvatar = corPara(linha.idColaborador(), idx++);
            String bg = alt ? "#f8fafc" : "white";
            alt = !alt;

            HBox nomeCell = construirCelulaColabDetalhada(
                    linha.nome(), linha.cargo(), corAvatar, bg, alturaEstimada);
            fixarAltura(nomeCell, alturaEstimada);
            fixarLargura(nomeCell, NOME_COL_DET);

            HBox rowDias = new HBox();
            rowDias.setAlignment(Pos.CENTER_LEFT);
            fixarAltura(rowDias, alturaEstimada);
            rowDias.setStyle("-fx-background-color: " + bg
                    + "; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");

            // Clique na célula: se houver callback ciente do colaborador, leva o id desta linha;
            // caso contrário, recai no callback de dia simples (cabeçalho/uso legado).
            final Integer idColabLinha = linha.idColaborador();
            Consumer<LocalDate> cliqueCelula = (aoAbrirDiaPorColaborador != null)
                    ? d -> aoAbrirDiaPorColaborador.accept(d, idColabLinha)
                    : aoAbrirDia;
            for (LocalDate dia : dias) {
                CelulaTurno celula = linha.celulas() != null ? linha.celulas().get(dia) : null;
                boolean destacada = celulasDestacadas != null
                        && celulasDestacadas.contains(chaveCelula(linha.idColaborador(), dia));
                rowDias.getChildren().add(construirCelulaDetalhada(
                        celula, dia, hoje, chipH, chipW, fntLetra, fntHoras,
                        larguraDia, mostrarHoras, cliqueCelula, destacada));
            }

            sincronizarHoverDet(nomeCell, rowDias);
            colunaFixaBody.getChildren().add(nomeCell);
            corpoDias.getChildren().add(rowDias);
        }

        // Espaçador reativo no fundo da coluna fixa: compensa a diferença de altura de viewport
        // causada pela barra horizontal do scrollCorpo (~15px). Quando os dois ranges forem iguais,
        // o sync de vvalue pode ser feito por cópia directa sem conversão de offset de pixel.
        Region folgaInferior = new Region();
        folgaInferior.setMinHeight(0);
        folgaInferior.setPrefHeight(0);
        folgaInferior.setMaxHeight(0);
        colunaFixaBody.getChildren().add(folgaInferior);

        colunaFixaBody.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 2 0 0; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 2, 0);");
        ScrollPane scrollColunaFixa = new ScrollPane(colunaFixaBody);
        scrollColunaFixa.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollColunaFixa.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollColunaFixa.setFitToWidth(true);
        scrollColunaFixa.setFitToHeight(false);
        scrollColunaFixa.setPannable(false);
        scrollColunaFixa.setStyle("-fx-background-color: white; -fx-background: white;");
        fixarLargura(scrollColunaFixa, NOME_COL_DET);

        ScrollPane scrollCorpo = new ScrollPane(corpoDias);
        scrollCorpo.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollCorpo.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollCorpo.setFitToHeight(false);
        scrollCorpo.setFitToWidth(false);
        scrollCorpo.setPannable(false);
        scrollCorpo.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        HBox.setHgrow(scrollCorpo, Priority.ALWAYS);

        scrollCorpo.hvalueProperty().addListener((obs, ov, nv) -> scrollHdrDias.setHvalue(nv.doubleValue()));

        // Quando o viewport do scrollCorpo muda de tamanho (barra horizontal aparece/desaparece),
        // ajustamos o espaçador inferior da coluna fixa para que os dois ranges verticais fiquem iguais.
        // Range igual → vvalue idêntico → pixel offset idêntico → alinhamento perfeito em todo o scroll.
        javafx.beans.value.ChangeListener<javafx.geometry.Bounds> ajustarEspacador = (obs, ov, nv) -> {
            javafx.geometry.Bounds vpC = scrollCorpo.getViewportBounds();
            javafx.geometry.Bounds vpF = scrollColunaFixa.getViewportBounds();
            if (vpC == null || vpF == null || vpC.getHeight() <= 0 || vpF.getHeight() <= 0) return;
            double delta = Math.max(0.0, vpF.getHeight() - vpC.getHeight());
            folgaInferior.setMinHeight(delta);
            folgaInferior.setPrefHeight(delta);
            folgaInferior.setMaxHeight(delta);
        };
        scrollCorpo.viewportBoundsProperty().addListener(ajustarEspacador);
        scrollColunaFixa.viewportBoundsProperty().addListener(ajustarEspacador);

        // Com ranges iguais a cópia directa do vvalue garante alinhamento em toda a gama de scroll.
        scrollCorpo.vvalueProperty().addListener((obs, ov, nv) -> scrollColunaFixa.setVvalue(nv.doubleValue()));

        HBox linhaCorpo = new HBox(scrollColunaFixa, scrollCorpo);
        linhaCorpo.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(linhaCorpo, Priority.ALWAYS);

        container.getChildren().addAll(linhaCabecalho, linhaCorpo);
    }

    private static VBox construirCabecalhoDetalhado(LocalDate dia, LocalDate hoje,
                                                     double larguraDia, Consumer<LocalDate> aoAbrirDia) {
        boolean fds = dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean eHoje = dia.equals(hoje);

        VBox hDia = new VBox(2);
        hDia.setAlignment(Pos.CENTER);
        fixarLargura(hDia, larguraDia);
        String bgHdr = eHoje ? "#fff1f2" : (fds ? "#f0f4ff" : "transparent");
        hDia.setStyle("-fx-background-color: " + bgHdr + "; -fx-border-color: "
                + (eHoje ? "transparent #fecdd3 #fecdd3 #dc2626" : "#e5e7eb") + "; "
                + (eHoje ? "-fx-border-width: 0 1 2 2;" : "-fx-border-width: 0 1 0 0;")
                + " -fx-cursor: hand;");

        Label lblSem = new Label(diaSemanaAbrev(dia.getDayOfWeek()).toUpperCase(Locale.ROOT));
        lblSem.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: " + (fds ? "#4f46e5" : "#9ca3af") + ";");

        if (eHoje) {
            StackPane circulo = new StackPane();
            circulo.setStyle("-fx-background-color: #dc2626; -fx-background-radius: 100;"
                    + " -fx-effect: dropshadow(gaussian, rgba(220,38,38,0.22), 8, 0, 0, 2);");
            circulo.setMinSize(30, 30); circulo.setPrefSize(30, 30); circulo.setMaxSize(30, 30);
            Label num = new Label(String.valueOf(dia.getDayOfMonth()));
            num.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: white;");
            circulo.getChildren().add(num);
            hDia.getChildren().addAll(lblSem, circulo);
        } else {
            Label lblNum = new Label(String.valueOf(dia.getDayOfMonth()));
            lblNum.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + (fds ? "#4338ca" : "#374151") + ";");
            hDia.getChildren().addAll(lblSem, lblNum);
        }

        if (aoAbrirDia != null) {
            hDia.setOnMouseClicked(e -> aoAbrirDia.accept(dia));
        }
        return hDia;
    }

    private static HBox construirCelulaColabDetalhada(String nome, String cargo, String corAvatar,
                                                       String bg, double alturaLinha) {
        HBox cell = new HBox(10);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setStyle("-fx-background-color: " + bg + "; -fx-border-color: #e2e8f0; "
                + "-fx-border-width: 0 2 1 0; -fx-padding: 0 12 0 18;");

        double avatarSz = Math.max(28.0, Math.min(38.0, alturaLinha * 0.48));
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: " + corAvatar + "; -fx-background-radius: 100;");
        avatar.setMinSize(avatarSz, avatarSz); avatar.setPrefSize(avatarSz, avatarSz); avatar.setMaxSize(avatarSz, avatarSz);
        Label ini = new Label(gerarIniciais(nome));
        ini.setStyle("-fx-font-size: " + Math.max(10.0, avatarSz * 0.38) + "px; -fx-font-weight: 700; -fx-text-fill: white;");
        avatar.getChildren().add(ini);

        VBox nomeBox = new VBox(1);
        nomeBox.setAlignment(Pos.CENTER_LEFT);
        Label lblNome = new Label(nome != null ? nome : "?");
        double fntNome = Math.max(11.0, Math.min(14.0, alturaLinha * 0.185));
        lblNome.setStyle("-fx-font-size: " + fntNome + "px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
        lblNome.setMaxWidth(NOME_COL_DET - 70);
        if (cargo != null && !cargo.isBlank()) {
            Label lblCargo = new Label(cargo);
            double fntCargo = Math.max(9.0, Math.min(11.0, alturaLinha * 0.135));
            lblCargo.setStyle("-fx-font-size: " + fntCargo + "px; -fx-text-fill: #64748b;");
            nomeBox.getChildren().addAll(lblNome, lblCargo);
        } else {
            nomeBox.getChildren().add(lblNome);
        }

        cell.getChildren().addAll(avatar, nomeBox);
        return cell;
    }

    private static StackPane construirCelulaDetalhada(CelulaTurno celula,
                                                       LocalDate dia,
                                                       LocalDate hoje,
                                                       double chipH,
                                                       double chipW,
                                                       double fntLetra,
                                                       double fntHoras,
                                                       double larguraDia,
                                                       boolean mostrarHoras,
                                                       Consumer<LocalDate> aoAbrirDia,
                                                       boolean destacada) {
        StackPane cell = new StackPane();
        fixarLargura(cell, larguraDia);
        cell.setMaxHeight(Double.MAX_VALUE);
        cell.setPadding(new javafx.geometry.Insets(0, 3, 0, 3));

        boolean fds   = dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean eHoje = dia.equals(hoje);

        String tipo  = celula != null ? celula.tipo() : null;
        String horas = celula != null ? celula.horas() : null;
        String chave = turnoChave(tipo != null ? tipo : "folga");
        boolean ehFolga = celula == null || "folga".equals(chave);

        String[] cores = CORES_DET.getOrDefault(chave, CORES_DET.get("outro"));
        String bgCell = eHoje ? "#fff1f2" : (fds ? cores[3] : cores[2]);
        String bordaTop = eHoje
                ? "-fx-border-color: transparent #fecdd3 #fecdd3 #dc2626; -fx-border-width: 0 1 1 2;"
                : "-fx-border-color: #f1f5f9; -fx-border-width: 0 1 1 0;";
        if (destacada) {
            bordaTop = "-fx-border-color: #f59e0b; -fx-border-width: 3;";
            bgCell = "#fffbeb";
        }
        cell.setStyle("-fx-background-color: " + bgCell + "; " + bordaTop + " -fx-cursor: hand;");

        if (ehFolga) {
            Label dash = new Label("–");
            dash.setStyle("-fx-font-size: " + (fntLetra + 2) + "px; -fx-font-weight: 300; -fx-text-fill: #cbd5e1;");
            cell.getChildren().add(dash);
        } else {
            VBox content = new VBox(2);
            content.setAlignment(Pos.CENTER);

            StackPane chip = new StackPane();
            chip.setStyle("-fx-background-color: " + cores[0] + "; -fx-background-radius: 5;"
                    + " -fx-min-width: " + chipW + "; -fx-min-height: " + chipH
                    + "; -fx-pref-width: " + chipW + "; -fx-pref-height: " + chipH + ";");
            String letraTurno = turnoLetraCompacta(tipo);
            double fntLetraAjustada = letraTurno.length() > 1 ? Math.max(8.5, fntLetra - 1.5) : fntLetra;
            Label letra = new Label(letraTurno);
            letra.setStyle("-fx-font-size: " + fntLetraAjustada + "px; -fx-font-weight: 700; -fx-text-fill: " + cores[1] + ";");
            chip.getChildren().add(letra);
            content.getChildren().add(chip);

            if (mostrarHoras && horas != null && !horas.isBlank()) {
                // cores[0] pode ser um gradiente para tipos combinados — usar cor sólida para texto
                String horasCorTexto = switch (chave) {
                    case "manha_tarde", "manha_intermedio" -> "#1d4ed8";
                    case "tarde_noite", "intermedio_noite" -> "#b45309";
                    case "manha_intermedio_noite" -> "#4f46e5";
                    default            -> cores[0];
                };
                String horasFormatadas = formatarHorasGrelha(horas);
                Label lblHoras = new Label(horasFormatadas);
                lblHoras.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                lblHoras.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                lblHoras.setStyle("-fx-font-size: " + fntHoras + "px; -fx-font-weight: 600; -fx-text-fill: " + horasCorTexto + ";");
                content.getChildren().add(lblHoras);
            }
            cell.getChildren().add(content);

            // Tooltip com horas quando as células são demasiado estreitas para mostrá-las
            if (!mostrarHoras && horas != null && !horas.isBlank()) {
                javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(
                        turnoLetraCompacta(tipo) + " " + formatarHorasGrelha(horas));
                tip.setStyle("-fx-font-size: 11px;");
                javafx.scene.control.Tooltip.install(cell, tip);
            }
        }

        if (aoAbrirDia != null) {
            // Realce do "quadradinho" sob o rato: brilho vermelho na própria célula dia×colaborador,
            // para o utilizador saber exatamente onde vai clicar (além do realce da linha inteira).
            final String estiloBaseCelula = cell.getStyle();
            final String estiloHoverCelula = estiloBaseCelula
                    + " -fx-effect: dropshadow(gaussian, rgba(201,20,40,0.38), 9, 0.25, 0, 0);";
            cell.setOnMouseEntered(e -> cell.setStyle(estiloHoverCelula));
            cell.setOnMouseExited(e -> cell.setStyle(estiloBaseCelula));
            cell.setOnMouseClicked(e -> aoAbrirDia.accept(dia));
        }
        return cell;
    }

    private static void sincronizarHoverDet(HBox nomeCell, HBox rowDias) {
        Runnable entrar = () -> {
            adicionarClasse(nomeCell, "grelha-row-hover");
            adicionarClasse(rowDias, "grelha-row-hover");
        };
        Runnable sair = () -> {
            nomeCell.getStyleClass().remove("grelha-row-hover");
            rowDias.getStyleClass().remove("grelha-row-hover");
        };
        nomeCell.setOnMouseEntered(e -> entrar.run());
        nomeCell.setOnMouseExited(e -> sair.run());
        rowDias.setOnMouseEntered(e -> entrar.run());
        rowDias.setOnMouseExited(e -> sair.run());
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
        renderizarCompacto(container, dias, linhas, hoje, aoAbrirDia, null);
    }

    public static void renderizarCompacto(VBox container,
                                          List<LocalDate> dias,
                                          List<LinhaGrelha> linhas,
                                          LocalDate hoje,
                                          Consumer<LocalDate> aoAbrirDia,
                                          Set<String> celulasDestacadas) {
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
                boolean destacada = celulasDestacadas != null
                        && celulasDestacadas.contains(chaveCelula(linha.idColaborador(), dia));
                StackPane tile = construirTileCompacto(celula, dia, hoje, aoAbrirDia, destacada);
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
                                                   Consumer<LocalDate> aoAbrirDia,
                                                   boolean destacada) {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("grelha-compacta-tile");
        boolean fds = dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (fds)         tile.getStyleClass().add("grelha-compacta-tile-fds");
        if (dia.equals(hoje)) tile.getStyleClass().add("grelha-compacta-tile-hoje");
        if (destacada)   tile.getStyleClass().add("grelha-compacta-tile-destaque");

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
            case "manha"       -> "M";
            case "tarde"       -> "T";
            case "noite"       -> "N";
            case "intermedio"  -> "I";
            case "manha_tarde", "manha_intermedio" -> "M/I";
            case "tarde_noite", "intermedio_noite" -> "I/N";
            case "manha_intermedio_noite" -> "M/I/N";
            case "folga"       -> "–";
            default            -> tipo.isBlank() ? "?" : tipo.substring(0, 1).toUpperCase(Locale.ROOT);
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
            case "manha"       -> "manha";
            case "tarde"       -> "tarde";
            case "noite"       -> "noite";
            case "folga"       -> "folga";
            case "intermedio"  -> "intermedio";
            case "manha_tarde" -> "manha_tarde";
            case "tarde_noite" -> "tarde_noite";
            case "manha_intermedio" -> "manha_intermedio";
            case "intermedio_noite" -> "intermedio_noite";
            case "manha_intermedio_noite" -> "manha_intermedio_noite";
            default            -> "outro";
        };
    }

    static String turnoNomeDisplay(String tipo) {
        if (tipo == null) {
            return "–";
        }
        return switch (turnoChave(tipo)) {
            case "manha"       -> "Manhã";
            case "tarde"       -> "Tarde";
            case "noite"       -> "Noite";
            case "folga"       -> "Folga";
            case "intermedio"  -> "Interm.";
            case "manha_intermedio" -> "Manha+Interm.";
            case "intermedio_noite" -> "Interm.+Noite";
            case "manha_intermedio_noite" -> "Manha+Interm.+Noite";
            case "manha_tarde" -> "Manhã+Tarde";
            case "tarde_noite" -> "Tarde+Noite";
            default            -> tipo.length() > 8 ? tipo.substring(0, 7) + "." : tipo;
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
