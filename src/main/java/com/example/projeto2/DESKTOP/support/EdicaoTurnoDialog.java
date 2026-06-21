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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Diálogo para o gestor ajustar o turno de um colaborador num dia: trocar o tipo de
 * turno ou <b>marcar folga</b> (remover o turno). Em caso de sucesso chama os callbacks
 * de notificação e de recarregamento (que re-verifica o horário).
 */
public final class EdicaoTurnoDialog {

    private EdicaoTurnoDialog() {
        // utilitário
    }

    /** Callback invocado quando a alteração foi aplicada com sucesso, reportando o turno escolhido (ou folga). */
    @FunctionalInterface
    public interface AplicacaoCallback {
        void aplicado(String mensagem, Turno turnoEscolhido, boolean folga);
    }

    public static void abrir(HorarioLinha linha,
                             Window owner,
                             HorarioService horarioBLL,
                             Integer idUtilizador,
                             AplicacaoCallback onSucesso,
                             Consumer<String> onErro,
                             Runnable onRecarregar) {
        try {
            List<Turno> turnos = horarioBLL.listarTodosOsTurnos();
            if (turnos.isEmpty()) {
                onErro.accept("Sem turnos disponíveis.");
                return;
            }

            // Primeira opção: folga (remover); seguidas dos turnos disponíveis.
            List<Opcao> opcoes = new ArrayList<>();
            opcoes.add(Opcao.folga());
            for (Turno t : turnos) opcoes.add(Opcao.de(t));

            ChoiceDialog<Opcao> dialogo = new ChoiceDialog<>(null, opcoes);
            dialogo.setTitle("Ajustar turno");
            dialogo.setHeaderText("Colaborador: " + (linha.colaborador() != null ? linha.colaborador() : "-")
                    + "\nDia: " + (linha.data() != null ? linha.data() : "-")
                    + "\nTurno atual: " + (linha.turno() != null ? linha.turno() : "-"));
            dialogo.setContentText("Novo turno:");

            StringConverter<Opcao> conversor = new StringConverter<>() {
                @Override public String toString(Opcao o) { return o == null ? "-" : o.label(); }
                @Override public Opcao fromString(String s) { return null; }
            };

            if (dialogo.getDialogPane().lookupAll(".combo-box").stream()
                    .findFirst().orElse(null) instanceof ComboBox<?> combo) {
                @SuppressWarnings("unchecked")
                ComboBox<Opcao> opcaoCombo = (ComboBox<Opcao>) combo;
                opcaoCombo.setConverter(conversor);
                opcaoCombo.setButtonCell(new ListCell<>() {
                    @Override protected void updateItem(Opcao o, boolean empty) {
                        super.updateItem(o, empty);
                        setText(empty || o == null ? "-" : conversor.toString(o));
                    }
                });
                opcaoCombo.setCellFactory(lv -> new ListCell<>() {
                    @Override protected void updateItem(Opcao o, boolean empty) {
                        super.updateItem(o, empty);
                        setText(empty || o == null ? "-" : conversor.toString(o));
                    }
                });
            }

            if (owner != null) {
                dialogo.initOwner(owner);
            }
            Optional<Opcao> resultado = dialogo.showAndWait();
            resultado.ifPresent(opcao -> {
                try {
                    if (opcao.ehFolga()) {
                        horarioBLL.removerTurno(linha.idHorario(), idUtilizador);
                        if (owner instanceof Stage ownerStage) ownerStage.close();
                        onSucesso.aplicado("Troca de turno efetuada com sucesso — colaborador fica de folga.", null, true);
                    } else {
                        horarioBLL.editarTurnoPublicado(
                                linha.idHorario(), opcao.turno().getId(), idUtilizador, null);
                        if (owner instanceof Stage ownerStage) ownerStage.close();
                        onSucesso.aplicado("Troca de turno efetuada com sucesso.", opcao.turno(), false);
                    }
                    if (onRecarregar != null) onRecarregar.run();
                } catch (IllegalArgumentException ex) {
                    onErro.accept(ex.getMessage());
                } catch (Exception ex) {
                    onErro.accept("Não foi possível aplicar a alteração.");
                }
            });
        } catch (Exception e) {
            onErro.accept("Não foi possível abrir o editor de turno.");
        }
    }

    /** Uma opção do seletor: um turno concreto ou "folga" (remover). */
    private record Opcao(Turno turno, boolean ehFolga) {
        static Opcao folga() { return new Opcao(null, true); }
        static Opcao de(Turno t) { return new Opcao(t, false); }

        String label() {
            if (ehFolga) return "— Folga (remover turno) —";
            return (turno.getTipo() != null ? turno.getTipo() + " " : "")
                    + turno.getHoraInicio() + " — " + turno.getHoraFim();
        }
    }
}
