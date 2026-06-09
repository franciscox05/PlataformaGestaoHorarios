package com.example.projeto2.DESKTOP.support;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public final class TabelaHelper {

    private static final String TABELA_PREPARADA = "codex.tabela.preparada";
    private static final String CABECALHO_BLOQUEADO = "codex.tabela.cabecalho.bloqueado";

    private TabelaHelper() {
    }

    public static void prepararArvore(Node raiz) {
        if (raiz == null) {
            return;
        }

        for (TableView<?> tabela : recolherTabelas(raiz)) {
            prepararTabela(tabela);
        }
    }

    public static void prepararTabela(TableView<?> tabela) {
        if (tabela == null || Boolean.TRUE.equals(tabela.getProperties().get(TABELA_PREPARADA))) {
            return;
        }

        tabela.getProperties().put(TABELA_PREPARADA, Boolean.TRUE);
        prepararColunas(tabela.getColumns());
        tabela.skinProperty().addListener((observavel, anterior, atual) -> Platform.runLater(() -> bloquearReordenacao(tabela)));
        Platform.runLater(() -> {
            tabela.applyCss();
            bloquearReordenacao(tabela);
        });
    }

    private static void prepararColunas(ObservableList<? extends TableColumnBase<?, ?>> colunas) {
        for (TableColumnBase<?, ?> coluna : colunas) {
            coluna.setSortable(true);
            coluna.setResizable(true);
            prepararColunas(coluna.getColumns());
        }
    }

    private static void bloquearReordenacao(TableView<?> tabela) {
        if (tabela == null) {
            return;
        }

        for (Node cabecalho : tabela.lookupAll(".column-header")) {
            if (Boolean.TRUE.equals(cabecalho.getProperties().get(CABECALHO_BLOQUEADO))) {
                continue;
            }

            cabecalho.getProperties().put(CABECALHO_BLOQUEADO, Boolean.TRUE);
            cabecalho.addEventFilter(MouseEvent.DRAG_DETECTED, Event::consume);
            cabecalho.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
                if (event.isPrimaryButtonDown()) {
                    event.consume();
                }
            });
        }
    }

    private static List<TableView<?>> recolherTabelas(Node node) {
        List<TableView<?>> tabelas = new ArrayList<>();
        recolherTabelas(node, tabelas);
        return tabelas;
    }

    private static void recolherTabelas(Node node, List<TableView<?>> tabelas) {
        if (node == null) {
            return;
        }

        if (node instanceof TableView<?> tabela) {
            tabelas.add(tabela);
        }

        if (node instanceof ScrollPane scrollPane) {
            recolherTabelas(scrollPane.getContent(), tabelas);
        }

        if (node instanceof Parent parent) {
            for (Node filho : parent.getChildrenUnmodifiable()) {
                recolherTabelas(filho, tabelas);
            }
        }
    }
}
