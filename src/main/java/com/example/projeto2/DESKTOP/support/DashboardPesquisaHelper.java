package com.example.projeto2.DESKTOP.support;

import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Encapsula toda a lógica de pesquisa global do dashboard: configuração do
 * campo de texto, filtragem/pontuação de sugestões e navegação para o destino.
 *
 * <p>O controller instancia o helper no {@code initialize()} e fornece um
 * {@link Supplier} de {@link EntradaPesquisa} que é invocado a cada pesquisa,
 * capturando a visibilidade atual dos módulos no momento do pedido.
 */
public final class DashboardPesquisaHelper {

    /**
     * Entrada de destino fornecida pelo controller: etiqueta visível, descrição
     * curta, ação de navegação e palavras-chave de pesquisa adicionais.
     */
    public record EntradaPesquisa(String etiqueta, String descricao, Runnable acao, List<String> chaves) {
        public EntradaPesquisa(String etiqueta, String descricao, Runnable acao, String... chaveVarargs) {
            this(etiqueta, descricao, acao, List.of(chaveVarargs));
        }
    }

    private final TextField txtPesquisa;
    private final HBox boxPesquisa;
    private final Supplier<List<EntradaPesquisa>> destinosSupplier;
    private ContextMenu menuSugestoes;

    public DashboardPesquisaHelper(TextField txtPesquisa,
                                    HBox boxPesquisa,
                                    Supplier<List<EntradaPesquisa>> destinosSupplier) {
        this.txtPesquisa = txtPesquisa;
        this.boxPesquisa = boxPesquisa;
        this.destinosSupplier = destinosSupplier;
    }

    /** Instala os listeners no campo de pesquisa. Chamar em {@code initialize()}. */
    public void configurar() {
        if (txtPesquisa == null) return;
        ContextMenu menu = obterMenu();

        txtPesquisa.setOnAction(event -> abrirPrimeiraSugestao());
        txtPesquisa.focusedProperty().addListener((obs, antes, agora) -> {
            if (boxPesquisa != null) {
                boxPesquisa.getStyleClass().remove("pesquisa-shell-ativo");
                if (agora) boxPesquisa.getStyleClass().add("pesquisa-shell-ativo");
            }
            if (agora) atualizar(txtPesquisa.getText());
        });
        txtPesquisa.textProperty().addListener((obs, ant, novo) -> atualizar(novo));
        txtPesquisa.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) obterMenu().hide();
        });
        txtPesquisa.setOnMouseClicked(ev -> {
            if (txtPesquisa.getText() != null && !txtPesquisa.getText().isBlank()) {
                atualizar(txtPesquisa.getText());
            }
        });

        if (!menu.getStyleClass().contains("pesquisa-sugestoes")) {
            menu.getStyleClass().add("pesquisa-sugestoes");
        }
        menu.setAutoHide(true);
        menu.setHideOnEscape(true);
    }

    // ── Actualização do menu ─────────────────────────────────────────────────

    private void atualizar(String pesquisa) {
        if (txtPesquisa == null) return;
        String termo = normalizar(pesquisa);
        if (termo.isBlank()) { obterMenu().hide(); return; }

        List<DestinoPontuado> sugestoes = filtrarEOrdenar(termo);
        List<CustomMenuItem> itens = new ArrayList<>();
        if (sugestoes.isEmpty()) {
            itens.add(itemSemResultados(pesquisa));
        } else {
            for (DestinoPontuado s : sugestoes) itens.add(itemSugestao(s.entrada()));
        }
        ContextMenu menu = obterMenu();
        menu.getItems().setAll(itens);
        if (!menu.isShowing()) menu.show(txtPesquisa, Side.BOTTOM, 0, 8);
    }

    private List<DestinoPontuado> filtrarEOrdenar(String termo) {
        return destinosSupplier.get().stream()
                .map(e -> new DestinoPontuado(e, pontuar(e, termo)))
                .filter(d -> d.pontos() > 0)
                .sorted(Comparator.comparingInt(DestinoPontuado::pontos).reversed()
                        .thenComparing(d -> d.entrada().etiqueta()))
                .limit(6)
                .toList();
    }

    private void abrirPrimeiraSugestao() {
        if (txtPesquisa == null) return;
        String termo = normalizar(txtPesquisa.getText());
        if (termo.isBlank()) { obterMenu().hide(); return; }
        filtrarEOrdenar(termo).stream().findFirst()
                .ifPresentOrElse(d -> navegar(d.entrada()), () -> atualizar(txtPesquisa.getText()));
    }

    private void navegar(EntradaPesquisa entrada) {
        obterMenu().hide();
        if (txtPesquisa != null) txtPesquisa.clear();
        entrada.acao().run();
    }

    private ContextMenu obterMenu() {
        if (menuSugestoes == null) menuSugestoes = new ContextMenu();
        return menuSugestoes;
    }

    // ── Construção dos itens de menu ─────────────────────────────────────────

    private CustomMenuItem itemSugestao(EntradaPesquisa entrada) {
        Label titulo = new Label(entrada.etiqueta());
        titulo.getStyleClass().add("pesquisa-sugestao-titulo");
        Label desc = new Label(entrada.descricao());
        desc.getStyleClass().add("pesquisa-sugestao-descricao");
        desc.setWrapText(true);
        VBox conteudo = new VBox(4.0, titulo, desc);
        conteudo.getStyleClass().add("pesquisa-sugestao");
        CustomMenuItem item = new CustomMenuItem(conteudo, true);
        item.setOnAction(ev -> navegar(entrada));
        return item;
    }

    private CustomMenuItem itemSemResultados(String pesquisa) {
        Label titulo = new Label("Sem resultados para \"" + pesquisa.trim() + "\"");
        titulo.getStyleClass().add("pesquisa-sugestao-titulo");
        Label desc = new Label("Tenta por exemplo: horários, pedidos, perfil, folga ou relatórios.");
        desc.getStyleClass().add("pesquisa-sugestao-descricao");
        desc.setWrapText(true);
        VBox conteudo = new VBox(4.0, titulo, desc);
        conteudo.getStyleClass().addAll("pesquisa-sugestao", "pesquisa-sugestao-vazia");
        CustomMenuItem item = new CustomMenuItem(conteudo, false);
        item.setHideOnClick(false);
        item.setDisable(true);
        return item;
    }

    // ── Pontuação de relevância ──────────────────────────────────────────────

    private int pontuar(EntradaPesquisa entrada, String termo) {
        int melhor = 0;
        for (String chave : todasAsChaves(entrada)) {
            melhor = Math.max(melhor, pontuacaoChave(chave, termo));
        }
        return melhor;
    }

    private List<String> todasAsChaves(EntradaPesquisa entrada) {
        List<String> chaves = new ArrayList<>();
        adicionarChave(chaves, entrada.etiqueta());
        adicionarChave(chaves, entrada.descricao());
        for (String c : entrada.chaves()) adicionarChave(chaves, c);
        return chaves;
    }

    private static void adicionarChave(List<String> lista, String valor) {
        String n = normalizar(valor);
        if (!n.isBlank() && !lista.contains(n)) lista.add(n);
    }

    private static int pontuacaoChave(String chave, String termo) {
        if (chave.equals(termo))                             return 120;
        if (chave.startsWith(termo))                         return 100;
        if (chave.contains(termo))                           return  84;
        if (termo.contains(chave) && chave.length() >= 4)   return  72;
        String[] tokens = termo.split("\\s+");
        int acertos = 0;
        for (String t : tokens) if (!t.isBlank() && chave.contains(t)) acertos++;
        return acertos == tokens.length && acertos > 0 ? 60 + acertos : 0;
    }

    private static String normalizar(String valor) {
        if (valor == null) return "";
        String s = valor.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private record DestinoPontuado(EntradaPesquisa entrada, int pontos) {}
}
