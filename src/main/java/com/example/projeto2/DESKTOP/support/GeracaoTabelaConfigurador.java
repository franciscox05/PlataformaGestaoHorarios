package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.DiferencaColaborador;
import com.example.projeto2.API.Services.geracao.dto.PropostaResumo;
import com.example.projeto2.API.Services.geracao.dto.ResumoColaborador;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Configuração das cell-value-factories e cell-factories das tabelas do ecrã de geração
 * de horários. Separa o código de setup das tabelas da lógica de negócio do controller.
 */
public final class GeracaoTabelaConfigurador {

    private GeracaoTabelaConfigurador() {}

    public static void configurarResumoColaboradores(
            TableView<ResumoColaborador> tabela,
            TableColumn<ResumoColaborador, String> colNome,
            TableColumn<ResumoColaborador, String> colCargo,
            TableColumn<ResumoColaborador, String> colTurnos,
            TableColumn<ResumoColaborador, String> colHoras,
            Consumer<ResumoColaborador> onDuploClique) {

        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nome()));
        colCargo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cargo()));
        colTurnos.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().turnos())));
        colHoras.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().horasFormatadas()));

        tabela.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ResumoColaborador selecionado = tabela.getSelectionModel().getSelectedItem();
                if (selecionado != null) onDuploClique.accept(selecionado);
            }
        });
    }

    public static void configurarPropostas(
            TableView<PropostaResumo> tabela,
            TableColumn<PropostaResumo, String> colRotulo,
            TableColumn<PropostaResumo, String> colEstado,
            TableColumn<PropostaResumo, String> colData,
            TableColumn<PropostaResumo, String> colScore,
            TableColumn<PropostaResumo, String> colQualidade,
            TableColumn<PropostaResumo, String> colTurnos,
            BooleanSupplier suprimirSelecao,
            Consumer<Integer> onSelecaoId,
            Runnable onAtualizarEstado) {

        tabela.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        colRotulo.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue().recomendada() ? "★ " : "") + c.getValue().rotulo()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estado()));
        colEstado.setCellFactory(col -> celulaEstado());
        colData.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().dataGeracao()));
        colScore.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().pontuacao())));
        colQualidade.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().qualidade()));
        colQualidade.setCellFactory(col -> celulaQualidade());
        colTurnos.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().turnos())));

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, ant, sel) -> {
            if (sel != null && !suprimirSelecao.getAsBoolean()) {
                onSelecaoId.accept(sel.idProposta());
            }
            onAtualizarEstado.run();
        });
    }

    public static void configurarComparacao(
            TableColumn<DiferencaColaborador, String> colColaborador,
            TableColumn<DiferencaColaborador, String> colBase,
            TableColumn<DiferencaColaborador, String> colAlvo,
            TableColumn<DiferencaColaborador, String> colDiferenca,
            ComboBox<PropostaResumo> cbBase,
            ComboBox<PropostaResumo> cbAlvo,
            ComboBox<PropostaResumo> cbSelecao,
            Runnable onAtualizarEstado,
            Predicate<Integer> deveCarregar,
            Consumer<Integer> onCarregarProposta) {

        colColaborador.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().colaborador()));
        colBase.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().turnosBase() + " turnos · " + c.getValue().horasBase()));
        colBase.setCellFactory(col -> celulaBarra(true));
        colAlvo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().turnosComparada() + " turnos · " + c.getValue().horasComparada()));
        colAlvo.setCellFactory(col -> celulaBarra(false));
        colDiferenca.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue().diferencaTurnos() > 0 ? "+" : "") + c.getValue().diferencaTurnos()
                        + " turnos · " + c.getValue().diferencaHoras()));
        colDiferenca.setCellFactory(col -> celulaDiferenca());

        cbBase.valueProperty().addListener((obs, ant, novo) -> onAtualizarEstado.run());
        cbAlvo.valueProperty().addListener((obs, ant, novo) -> onAtualizarEstado.run());

        if (cbSelecao != null) {
            cbSelecao.valueProperty().addListener((obs, ant, novo) -> {
                if (novo != null && deveCarregar.test(novo.idProposta())) {
                    onCarregarProposta.accept(novo.idProposta());
                }
            });
        }
    }

    // ── cell factories ────────────────────────────────────────────────────────

    private static TableCell<PropostaResumo, String> celulaEstado() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(estado.toUpperCase());
                badge.getStyleClass().add("badge-estado");
                String n = normalizar(estado);
                if (n.contains("aprovad") || n.contains("publicad"))      badge.getStyleClass().add("badge-aprovado");
                else if (n.contains("rejeitad") || n.contains("cancelad")) badge.getStyleClass().add("badge-rejeitado");
                else if (n.contains("pendente") || n.contains("enviado"))  badge.getStyleClass().add("badge-enviado");
                else                                                        badge.getStyleClass().add("badge-rascunho");
                setGraphic(badge);
                setText(null);
            }
        };
    }

    private static TableCell<PropostaResumo, String> celulaQualidade() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String qualidade, boolean empty) {
                super.updateItem(qualidade, empty);
                if (empty || qualidade == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(qualidade);
                String q = qualidade.toLowerCase();
                if (q.contains("alta") || q.contains("excelente") || q.contains("ótima"))
                    chip.getStyleClass().add("chip-qualidade-alta");
                else if (q.contains("média") || q.contains("media") || q.contains("razoável"))
                    chip.getStyleClass().add("chip-qualidade-media");
                else
                    chip.getStyleClass().add("chip-qualidade-baixa");
                setGraphic(chip);
                setText(null);
            }
        };
    }

    private static TableCell<DiferencaColaborador, String> celulaBarra(boolean isBase) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean empty) {
                super.updateItem(texto, empty);
                if (empty || texto == null) { setGraphic(null); setText(null); return; }
                DiferencaColaborador item = getTableRow() != null
                        ? (DiferencaColaborador) getTableRow().getItem() : null;
                if (item == null) { setText(texto); setGraphic(null); return; }

                int maxVal = 1;
                TableView<?> tv = getTableView();
                if (tv != null && tv.getItems() != null) {
                    for (Object obj : tv.getItems()) {
                        if (obj instanceof DiferencaColaborador d) {
                            maxVal = Math.max(maxVal, Math.max(d.turnosBase(), d.turnosComparada()));
                        }
                    }
                }
                int valor = isBase ? item.turnosBase() : item.turnosComparada();
                double progress = (double) valor / maxVal;

                Label lbl = new Label(texto);
                lbl.setStyle("-fx-font-size: 11px;");

                Region track = new Region();
                track.setPrefSize(64, 5);
                track.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 3;");

                Region fill = new Region();
                fill.setPrefSize(Math.max(2, 64 * progress), 5);
                fill.setMaxWidth(Math.max(2, 64 * progress));
                fill.setStyle("-fx-background-color: " + (isBase ? "#3b82f6" : "#f97316")
                        + "; -fx-background-radius: 3;");

                StackPane bar = new StackPane(track, fill);
                StackPane.setAlignment(fill, Pos.CENTER_LEFT);
                bar.setMaxWidth(64);
                bar.setPrefWidth(64);
                bar.setAlignment(Pos.CENTER_LEFT);

                VBox box = new VBox(3, lbl, bar);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        };
    }

    private static TableCell<DiferencaColaborador, String> celulaDiferenca() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean empty) {
                super.updateItem(texto, empty);
                if (empty || texto == null) { setGraphic(null); setText(null); setStyle(""); return; }
                setText(texto);
                setGraphic(null);
                DiferencaColaborador item = getTableRow() != null
                        ? (DiferencaColaborador) getTableRow().getItem() : null;
                if (item != null) {
                    if (item.diferencaTurnos() > 0)      setStyle("-fx-text-fill: #16a34a;");
                    else if (item.diferencaTurnos() < 0) setStyle("-fx-text-fill: #c91428;");
                    else                                  setStyle("");
                }
            }
        };
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
