package com.example.projeto2.DESKTOP.support;

import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Pagina uma lista de itens sobre uma TableView, 10 registos de cada vez, com
 * navegação "Anterior"/"Próxima" — mesmo padrão dos diálogos de histórico.
 */
public final class PaginadorTabela<T> {

    private static final int TAMANHO_PAGINA = 10;
    /** Altura de cada linha (incluindo o cabeçalho), em pixels — ver {@link #dimensionarTabela}. */
    private static final double ALTURA_LINHA = 40.0;

    private final TableView<T> tabela;
    private final Label lblPagina;
    private final Button btnAnterior;
    private final Button btnProxima;

    private List<T> itens = List.of();
    private int paginaAtual = 0;

    public PaginadorTabela(TableView<T> tabela, Label lblPagina, Button btnAnterior, Button btnProxima) {
        this.tabela = tabela;
        this.lblPagina = lblPagina;
        this.btnAnterior = btnAnterior;
        this.btnProxima = btnProxima;
        btnAnterior.setOnAction(e -> irParaPagina(paginaAtual - 1));
        btnProxima.setOnAction(e -> irParaPagina(paginaAtual + 1));
        dimensionarTabela();
    }

    /**
     * Fixa a altura de linha e calcula a altura da tabela para que os 10 registos de uma
     * página caibam sempre sem scroll vertical (cabeçalho + 10 linhas + folga para a borda).
     */
    private void dimensionarTabela() {
        tabela.setFixedCellSize(ALTURA_LINHA);
        double altura = ALTURA_LINHA * (TAMANHO_PAGINA + 1) + 2;
        tabela.setMinHeight(altura);
        tabela.setPrefHeight(altura);
        tabela.setMaxHeight(altura);
    }

    public void definirItens(List<T> itens) {
        this.itens = itens != null ? itens : List.of();
        irParaPagina(0);
    }

    private void irParaPagina(int pagina) {
        int total = itens.size();
        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) TAMANHO_PAGINA));
        paginaAtual = Math.max(0, Math.min(pagina, totalPaginas - 1));

        int inicio = paginaAtual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, total);
        tabela.setItems(FXCollections.observableArrayList(total > 0 ? itens.subList(inicio, fim) : List.of()));
        tabela.refresh();

        lblPagina.setText("Pág. " + (paginaAtual + 1) + " / " + totalPaginas + "  (" + total + " registos)");
        btnAnterior.setDisable(paginaAtual == 0);
        btnProxima.setDisable(paginaAtual >= totalPaginas - 1);
    }
}
