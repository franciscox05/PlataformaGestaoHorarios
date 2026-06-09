package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.GeracaoHorariosService;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.function.Supplier;

/**
 * Encapsula o painel de diagnóstico que aparece quando a geração de horário falha.
 * Recebe os nodes do FXML por construtor e expõe {@link #mostrar(Throwable)} e
 * {@link #esconder()}.
 */
public final class DiagnosticoGeracaoPanel {

    private final VBox painel;
    private final Label lblTitulo;
    private final Label lblResumo;
    private final Label lblPerfilRecomendado;
    private final VBox boxMotivos;
    private final VBox boxSugestoes;
    private final Supplier<Window> ownerSupplier;

    public DiagnosticoGeracaoPanel(VBox painel,
                                   Label lblTitulo,
                                   Label lblResumo,
                                   Label lblPerfilRecomendado,
                                   VBox boxMotivos,
                                   VBox boxSugestoes,
                                   Supplier<Window> ownerSupplier) {
        this.painel = painel;
        this.lblTitulo = lblTitulo;
        this.lblResumo = lblResumo;
        this.lblPerfilRecomendado = lblPerfilRecomendado;
        this.boxMotivos = boxMotivos;
        this.boxSugestoes = boxSugestoes;
        this.ownerSupplier = ownerSupplier;
    }

    /** Preenche e mostra o painel se a causa-raiz for uma {@link GeracaoHorariosService.FalhaGeracaoHorarioException}. */
    public void mostrar(Throwable erro) {
        Throwable causaRaiz = causaRaiz(erro);
        if (!(causaRaiz instanceof GeracaoHorariosService.FalhaGeracaoHorarioException falha)) {
            esconder();
            return;
        }

        lblTitulo.setText("Turno sem cobertura: " + falha.turno() + " em " + falha.data());
        lblResumo.setText("Foram considerados "
                + falha.colaboradoresConsiderados()
                + " colaboradores. Principal bloqueio: "
                + falha.motivoPrincipal());

        String perfilRecomendado = perfilRecomendado(falha);
        boolean temPerfil = !perfilRecomendado.isBlank();
        lblPerfilRecomendado.setText(temPerfil
                ? "Reforco recomendado: " + perfilRecomendado
                        + ". Este e o perfil que mais ajuda a aliviar este gargalo sem mexer nas restantes regras."
                : "");
        lblPerfilRecomendado.setVisible(temPerfil);
        lblPerfilRecomendado.setManaged(temPerfil);

        boxMotivos.getChildren().clear();
        boxSugestoes.getChildren().clear();

        for (GeracaoHorariosService.MotivoFalhaGeracao motivo : falha.motivos()) {
            boxMotivos.getChildren().add(construirBlocoMotivo(motivo));
        }

        int indice = 0;
        for (GeracaoHorariosService.SugestaoFalhaGeracao sugestao : falha.sugestoes()) {
            boxSugestoes.getChildren().add(construirLinhaSugestao(sugestao, indice++));
        }

        painel.setVisible(true);
        painel.setManaged(true);
    }

    public void esconder() {
        if (painel == null) return;
        painel.setVisible(false);
        painel.setManaged(false);
        if (lblPerfilRecomendado != null) {
            lblPerfilRecomendado.setVisible(false);
            lblPerfilRecomendado.setManaged(false);
            lblPerfilRecomendado.setText("");
        }
        if (boxMotivos != null) boxMotivos.getChildren().clear();
        if (boxSugestoes != null) boxSugestoes.getChildren().clear();
    }

    private VBox construirBlocoMotivo(GeracaoHorariosService.MotivoFalhaGeracao motivo) {
        VBox blocoMotivo = new VBox(6);
        blocoMotivo.getStyleClass().add("diagnostico-bloco-motivo");

        HBox linha = new HBox(10);
        linha.getStyleClass().add("diagnostico-linha");

        Button total = new Button(motivo.total() == 1 ? "1 pessoa" : motivo.total() + " pessoas");
        total.getStyleClass().add("diagnostico-contador");
        total.setOnAction(event -> mostrarDetalheMotivo(motivo));

        Label descricao = new Label(motivo.descricao());
        descricao.getStyleClass().add("diagnostico-descricao");
        descricao.setWrapText(true);

        linha.getChildren().addAll(total, descricao);
        blocoMotivo.getChildren().add(linha);

        if (motivo.nomes() != null && !motivo.nomes().isEmpty()) {
            HBox nomes = new HBox(6);
            nomes.getStyleClass().add("diagnostico-nomes");
            List<String> visiveis = motivo.nomes().stream().limit(4).toList();
            for (String nome : visiveis) {
                Label etiqueta = new Label(nome);
                etiqueta.getStyleClass().add("diagnostico-nome");
                nomes.getChildren().add(etiqueta);
            }
            if (motivo.nomes().size() > visiveis.size()) {
                Label restantes = new Label("+" + (motivo.nomes().size() - visiveis.size()) + " ver no contador");
                restantes.getStyleClass().add("diagnostico-nome-mais");
                nomes.getChildren().add(restantes);
            }
            blocoMotivo.getChildren().add(nomes);
        }
        return blocoMotivo;
    }

    private HBox construirLinhaSugestao(GeracaoHorariosService.SugestaoFalhaGeracao sugestao, int indice) {
        HBox linha = new HBox(10);
        linha.getStyleClass().add("diagnostico-linha");

        Label ordem = new Label(rotuloSugestao(indice));
        ordem.getStyleClass().add("diagnostico-etapa");

        Label descricao = new Label(sugestao.texto());
        descricao.getStyleClass().add("diagnostico-sugestao");
        descricao.setWrapText(true);

        VBox textoSugestao = new VBox(5);
        textoSugestao.getChildren().add(descricao);

        linha.getChildren().addAll(ordem, textoSugestao);
        return linha;
    }

    private void mostrarDetalheMotivo(GeracaoHorariosService.MotivoFalhaGeracao motivo) {
        FlowPane nomes = new FlowPane(8, 8);
        nomes.getStyleClass().add("diagnostico-modal-nomes");
        for (String nome : motivo.nomes()) {
            Label etiqueta = new Label(nome);
            etiqueta.getStyleClass().add("diagnostico-nome");
            nomes.getChildren().add(etiqueta);
        }

        DialogosHelper.mostrarConteudo(
                ownerSupplier != null ? ownerSupplier.get() : null,
                "Diagnóstico da geração",
                motivo.total() == 1 ? "1 pessoa afetada" : motivo.total() + " pessoas afetadas",
                motivo.descricao(),
                nomes
        );
    }

    private static String perfilRecomendado(GeracaoHorariosService.FalhaGeracaoHorarioException falha) {
        return falha.sugestoes().stream()
                .map(GeracaoHorariosService.SugestaoFalhaGeracao::perfilRecomendado)
                .filter(perfil -> perfil != null && !perfil.isBlank())
                .findFirst()
                .orElse("");
    }

    private static String rotuloSugestao(int indice) {
        return switch (indice) {
            case 0 -> "Fazer primeiro";
            case 1 -> "Depois";
            case 2 -> "Verificar";
            default -> "Opcional";
        };
    }

    private static Throwable causaRaiz(Throwable erro) {
        Throwable atual = erro;
        while (atual != null && atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return atual != null ? atual : erro;
    }
}
