package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.*;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioService;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Diálogo para o gestor mudar o turno de um horário publicado.
 * Mostra um ChoiceDialog formatado com a lista de turnos, e — em caso de sucesso —
 * chama os callbacks de notificação (sucesso/erro) e de recarregamento.
 */
public final class EdicaoTurnoDialog {

    private EdicaoTurnoDialog() {
        // utilitário
    }

    public static void abrir(HorarioLinha linha,
                             Window owner,
                             HorarioService horarioBLL,
                             Integer idUtilizador,
                             Consumer<String> onSucesso,
                             Consumer<String> onErro,
                             Runnable onRecarregar) {
        try {
            List<Turno> turnos = horarioBLL.listarTodosOsTurnos();
            if (turnos.isEmpty()) {
                onErro.accept("Sem turnos disponíveis.");
                return;
            }

            ChoiceDialog<Turno> dialogo = new ChoiceDialog<>(null, turnos);
            dialogo.setTitle("Editar turno");
            dialogo.setHeaderText("Colaborador: " + (linha.colaborador() != null ? linha.colaborador() : "-")
                    + "\nDia: " + (linha.data() != null ? linha.data() : "-")
                    + "\nTurno atual: " + (linha.turno() != null ? linha.turno() : "-"));
            dialogo.setContentText("Novo turno:");

            StringConverter<Turno> conversor = new StringConverter<>() {
                @Override
                public String toString(Turno t) {
                    if (t == null) return "-";
                    return (t.getTipo() != null ? t.getTipo() + " " : "")
                            + t.getHoraInicio() + " — " + t.getHoraFim();
                }
                @Override
                public Turno fromString(String s) { return null; }
            };

            if (dialogo.getDialogPane().lookupAll(".combo-box").stream()
                    .findFirst().orElse(null) instanceof ComboBox<?> combo) {
                @SuppressWarnings("unchecked")
                ComboBox<Turno> turnoCombo = (ComboBox<Turno>) combo;
                turnoCombo.setConverter(conversor);
                turnoCombo.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(Turno t, boolean empty) {
                        super.updateItem(t, empty);
                        setText(empty || t == null ? "-" : conversor.toString(t));
                    }
                });
                turnoCombo.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(Turno t, boolean empty) {
                        super.updateItem(t, empty);
                        setText(empty || t == null ? "-" : conversor.toString(t));
                    }
                });
            }

            if (owner != null) {
                dialogo.initOwner(owner);
            }
            Optional<Turno> resultado = dialogo.showAndWait();
            resultado.ifPresent(novoTurno -> {
                try {
                    horarioBLL.editarTurnoPublicado(
                            linha.idHorario(),
                            novoTurno.getId(),
                            idUtilizador,
                            null
                    );
                    if (owner instanceof Stage ownerStage) {
                        ownerStage.close();
                    }
                    onSucesso.accept("Turno alterado para " + conversor.toString(novoTurno) + " com sucesso.");
                    if (onRecarregar != null) onRecarregar.run();
                } catch (IllegalArgumentException ex) {
                    onErro.accept(ex.getMessage());
                } catch (Exception ex) {
                    onErro.accept("Não foi possível alterar o turno.");
                }
            });
        } catch (Exception e) {
            onErro.accept("Não foi possível abrir o editor de turno.");
        }
    }
}
