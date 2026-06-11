package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Services.geracao.dto.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Diálogo "Detalhe do dia": lista os turnos planeados para um dia da proposta atual,
 * com botão de edição por turno se o utilizador tiver permissões e o horário já estiver
 * publicado (idHorario != null). Tem também uma variante para o horário publicado da
 * loja ({@link #abrirHorariosPublicados}), usada na página principal.
 */
public final class DetalheDiaDialog {

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern(
            "EEEE, d 'de' MMMM yyyy", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private DetalheDiaDialog() {
        // utilitário
    }

    /**
     * @param onEditarTurno callback chamado ao clicar "Editar turno" — recebe a linha
     *                      e a janela do diálogo de detalhe (para servir de owner ao
     *                      ChoiceDialog que se abre por cima).
     */
    public static void abrir(LocalDate data,
                             List<HorarioLinha> linhasProposta,
                             Window owner,
                             boolean podeEditar,
                             BiConsumer<HorarioLinha, Window> onEditarTurno) {
        if (data == null || linhasProposta == null) return;

        List<HorarioLinha> turnosDia = linhasProposta.stream()
                .filter(linha -> linha != null && data.equals(linha.data()))
                .sorted(Comparator
                        .comparing(HorarioLinha::periodo,
                                Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(HorarioLinha::colaborador,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        if (turnosDia.isEmpty()) return;

        VBox listaTurnos = new VBox(10.0);
        listaTurnos.getStyleClass().add("detalhe-dia-lista");
        for (HorarioLinha turno : turnosDia) {
            listaTurnos.getChildren().add(criarCard(turno, podeEditar, onEditarTurno));
        }

        DialogosHelper.mostrarConteudo(
                owner,
                "DETALHE DO DIA",
                capitalizar(data.format(FORMATO_DIA)),
                turnosDia.size() + " turno(s) planeado(s). Revê a cobertura antes de enviar ao supervisor.",
                listaTurnos
        );
    }

    /**
     * Variante para o horário publicado da loja: lista os turnos de {@code data}
     * presentes em {@code horarios} (entidades {@link Horario}), sem botão de edição.
     * Usada no calendário/grelha mensal da página principal.
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
        if (h == null || h.getIdTurno() == null) {
            return "-";
        }
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

    private static String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        return texto.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-PT")) + texto.substring(1);
    }
}
