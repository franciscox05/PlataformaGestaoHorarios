package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Services.geracao.dto.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Diálogo "Detalhe do dia": lista os turnos planeados para um dia da proposta atual,
 * com botão de edição por turno se o utilizador tiver permissões. Também mostra uma
 * secção "Adicionar colaborador" para participantes da proposta sem turno nesse dia.
 * Tem também uma variante para o horário publicado da loja ({@link #abrirHorariosPublicados}).
 */
public final class DetalheDiaDialog {

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern(
            "EEEE, d 'de' MMMM yyyy", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private DetalheDiaDialog() {}

    /**
     * @param onEditarTurno    callback ao clicar "Editar turno" numa linha existente
     * @param onAdicionarTurno callback ao clicar "Adicionar turno" para um colaborador
     *                         sem turno nesse dia — recebe uma HorarioLinha com
     *                         idHorario=null e data=dia a adicionar
     */
    public static void abrir(LocalDate data,
                             List<HorarioLinha> linhasProposta,
                             Window owner,
                             boolean podeEditar,
                             BiConsumer<HorarioLinha, Window> onEditarTurno,
                             BiConsumer<HorarioLinha, Window> onAdicionarTurno) {
        if (data == null || linhasProposta == null) return;

        List<HorarioLinha> turnosDia = linhasProposta.stream()
                .filter(linha -> linha != null && data.equals(linha.data()))
                .sorted(Comparator
                        .comparing(HorarioLinha::periodo,
                                Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(HorarioLinha::colaborador,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        // Colaboradores na proposta sem turno neste dia (para a secção "Adicionar")
        List<HorarioLinha> semTurnoHoje = List.of();
        if (podeEditar && onAdicionarTurno != null) {
            Set<Integer> comTurno = turnosDia.stream()
                    .map(HorarioLinha::idColaborador)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            // Um representante por colaborador (idLojautilizador != null para poder criar Horario)
            Map<Integer, HorarioLinha> porColab = new LinkedHashMap<>();
            for (HorarioLinha l : linhasProposta) {
                if (l == null || l.idColaborador() == null || l.idLojautilizador() == null) continue;
                if (comTurno.contains(l.idColaborador())) continue;
                porColab.putIfAbsent(l.idColaborador(), l);
            }
            semTurnoHoje = new ArrayList<>(porColab.values());
            semTurnoHoje.sort(Comparator.comparing(HorarioLinha::colaborador,
                    Comparator.nullsLast(String::compareToIgnoreCase)));
        }

        if (turnosDia.isEmpty() && semTurnoHoje.isEmpty()) return;

        VBox conteudo = new VBox(10.0);
        conteudo.getStyleClass().add("detalhe-dia-lista");

        // ── Turnos existentes ───────────────────────────────────────────────────
        for (HorarioLinha turno : turnosDia) {
            conteudo.getChildren().add(criarCard(turno, podeEditar, onEditarTurno));
        }

        // ── Secção "Adicionar colaborador a este dia" ───────────────────────────
        if (!semTurnoHoje.isEmpty()) {
            if (!turnosDia.isEmpty()) {
                javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
                sep.setStyle("-fx-padding: 4 0 0 0;");
                conteudo.getChildren().add(sep);
            }
            Label lblTitulo = new Label("ADICIONAR A ESTE DIA");
            lblTitulo.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-padding: 4 0 2 0;");
            conteudo.getChildren().add(lblTitulo);

            for (HorarioLinha colab : semTurnoHoje) {
                HorarioLinha template = new HorarioLinha(
                        null, colab.idColaborador(), colab.idLojautilizador(),
                        data, null, null, null,
                        colab.colaborador(), colab.cargo(), null);
                conteudo.getChildren().add(criarCardAdicionar(template, onAdicionarTurno));
            }
        }

        String subtitulo = turnosDia.isEmpty()
                ? "Nenhum turno atribuído — podes adicionar colaboradores abaixo."
                : turnosDia.size() + " turno(s) planeado(s). Revê a cobertura antes de enviar ao supervisor.";

        // Scroll quando a lista é comprida (ex.: muitos colaboradores sem turno)
        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(440.0);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        DialogosHelper.mostrarConteudo(
                owner,
                "DETALHE DO DIA",
                capitalizar(data.format(FORMATO_DIA)),
                subtitulo,
                scroll
        );
    }

    /**
     * Variante para o horário publicado da loja: lista os turnos de {@code data}
     * presentes em {@code horarios} (entidades {@link Horario}), sem botão de edição.
     */
    public static void abrirHorariosPublicados(LocalDate data,
                                               List<Horario> horarios,
                                               Window owner) {
        if (data == null || horarios == null) return;

        List<Horario> turnosDia = horarios.stream()
                .filter(h -> h != null && data.equals(h.getDataTurno()))
                .sorted(Comparator
                        .comparing((Horario h) -> h.getIdTurno() != null ? h.getIdTurno().getHoraInicio() : null,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DetalheDiaDialog::nomeColaborador,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        if (turnosDia.isEmpty()) return;

        VBox listaTurnos = new VBox(10.0);
        listaTurnos.getStyleClass().add("detalhe-dia-lista");
        for (Horario turno : turnosDia) {
            listaTurnos.getChildren().add(criarCardPublicado(turno));
        }

        DialogosHelper.mostrarConteudo(
                owner,
                "DETALHE DO DIA",
                capitalizar(data.format(FORMATO_DIA)),
                turnosDia.size() + " turno(s) publicado(s) para a equipa neste dia.",
                listaTurnos
        );
    }

    private static VBox criarCardPublicado(Horario turno) {
        VBox card = new VBox(5.0);
        card.getStyleClass().add("detalhe-dia-turno-card");

        Label periodo = new Label(formatarPeriodo(turno));
        periodo.getStyleClass().add("detalhe-dia-periodo");

        Label colaborador = new Label(valorOuTraco(nomeColaborador(turno)));
        colaborador.getStyleClass().add("detalhe-dia-colaborador");

        String cargoNome = turno.getIdLojautilizador() != null && turno.getIdLojautilizador().getIdCargo() != null
                ? turno.getIdLojautilizador().getIdCargo().getNome()
                : null;
        String estadoNome = turno.getEstado() != null ? capitalizar(turno.getEstado().name()) : null;
        Label cargo = new Label(valorOuTraco(cargoNome) + " · " + valorOuTraco(estadoNome));
        cargo.getStyleClass().add("detalhe-dia-cargo");
        cargo.setWrapText(true);

        card.getChildren().addAll(periodo, colaborador, cargo);
        return card;
    }

    private static String nomeColaborador(Horario h) {
        if (h == null || h.getIdLojautilizador() == null
                || h.getIdLojautilizador().getIdUtilizador() == null) {
            return null;
        }
        return h.getIdLojautilizador().getIdUtilizador().getNome();
    }

    private static String formatarPeriodo(Horario h) {
        if (h == null || h.getIdTurno() == null) return "-";
        String inicio = h.getIdTurno().getHoraInicio() != null
                ? h.getIdTurno().getHoraInicio().format(FORMATO_HORA) : "--:--";
        String fim = h.getIdTurno().getHoraFim() != null
                ? h.getIdTurno().getHoraFim().format(FORMATO_HORA) : "--:--";
        return inicio + " - " + fim;
    }

    private static VBox criarCard(HorarioLinha turno,
                                  boolean podeEditar,
                                  BiConsumer<HorarioLinha, Window> onEditarTurno) {
        VBox card = new VBox(5.0);
        card.getStyleClass().add("detalhe-dia-turno-card");

        Label periodo = new Label(valorOuTraco(turno.periodo()));
        periodo.getStyleClass().add("detalhe-dia-periodo");

        Label colaborador = new Label(valorOuTraco(turno.colaborador()));
        colaborador.getStyleClass().add("detalhe-dia-colaborador");

        Label cargo = new Label(valorOuTraco(turno.cargo()) + " · " + valorOuTraco(turno.estado()));
        cargo.getStyleClass().add("detalhe-dia-cargo");
        cargo.setWrapText(true);

        card.getChildren().addAll(periodo, colaborador, cargo);

        if (podeEditar && turno.idHorario() != null && onEditarTurno != null) {
            Button btnEditar = new Button("Editar turno");
            btnEditar.getStyleClass().add("botao-editar-turno");
            btnEditar.setMaxWidth(Double.MAX_VALUE);
            btnEditar.setOnAction(e -> {
                Window janelaBotao = btnEditar.getScene() != null
                        ? btnEditar.getScene().getWindow() : null;
                onEditarTurno.accept(turno, janelaBotao);
            });
            card.getChildren().add(btnEditar);
        }

        return card;
    }

    private static HBox criarCardAdicionar(HorarioLinha template,
                                           BiConsumer<HorarioLinha, Window> onAdicionarTurno) {
        HBox row = new HBox(12.0);
        row.getStyleClass().add("detalhe-dia-turno-card");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 7 10 7 10; -fx-opacity: 0.9;");

        VBox info = new VBox(2.0);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label colaborador = new Label(valorOuTraco(template.colaborador()));
        colaborador.getStyleClass().add("detalhe-dia-colaborador");
        colaborador.setStyle("-fx-font-size: 12px;");

        Label cargo = new Label(valorOuTraco(template.cargo()) + " · Sem turno");
        cargo.getStyleClass().add("detalhe-dia-cargo");
        cargo.setStyle("-fx-font-size: 10.5px;");

        info.getChildren().addAll(colaborador, cargo);

        Button btnAdicionar = new Button("Adicionar");
        btnAdicionar.getStyleClass().add("botao-editar-turno");
        btnAdicionar.setMinWidth(90.0);
        btnAdicionar.setOnAction(e -> {
            Window janelaBotao = btnAdicionar.getScene() != null
                    ? btnAdicionar.getScene().getWindow() : null;
            onAdicionarTurno.accept(template, janelaBotao);
        });

        row.getChildren().addAll(info, btnAdicionar);
        return row;
    }

    private static String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        return texto.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-PT")) + texto.substring(1);
    }
}
