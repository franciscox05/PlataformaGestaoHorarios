package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.PropostaResultado;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

import java.text.Normalizer;

/**
 * Encapsula o stepper de fluxo de 4 passos e a tab de navegação do assistente.
 * Recebe os nodes FXML no construtor e expõe métodos de navegação e actualização de estado.
 */
public final class GeracaoStepperPanel {

    private final VBox stepperPasso1;
    private final VBox stepperPasso2;
    private final VBox stepperPasso3;
    private final VBox stepperPasso4;
    private final TabPane tabPaneHorarios;
    private final Label lblGuiaFluxo;
    private final Runnable onEntrarPasso2;

    public GeracaoStepperPanel(VBox p1, VBox p2, VBox p3, VBox p4,
                                TabPane tabPane, Label lblGuia,
                                Runnable onEntrarPasso2) {
        this.stepperPasso1 = p1;
        this.stepperPasso2 = p2;
        this.stepperPasso3 = p3;
        this.stepperPasso4 = p4;
        this.tabPaneHorarios = tabPane;
        this.lblGuiaFluxo = lblGuia;
        this.onEntrarPasso2 = onEntrarPasso2;
    }

    /** Liga os cliques do stepper à navegação e o listener de mudança de tab. */
    public void configurarNavegacao() {
        VBox[] passos = { stepperPasso1, stepperPasso2, stepperPasso3, stepperPasso4 };
        for (int i = 0; i < passos.length; i++) {
            if (passos[i] == null) continue;
            final int idx = i;
            passos[i].setOnMouseClicked(e -> irParaPasso(idx));
        }
        if (tabPaneHorarios != null) {
            tabPaneHorarios.getSelectionModel().selectedIndexProperty().addListener(
                    (obs, antigo, novo) -> {
                        int idx = novo.intValue();
                        marcarPassoAtual(idx);
                        if (idx == 2 && onEntrarPasso2 != null) {
                            onEntrarPasso2.run();
                        }
                    });
            marcarPassoAtual(tabPaneHorarios.getSelectionModel().getSelectedIndex());
        }
    }

    public void irParaPasso(int indice) {
        if (tabPaneHorarios == null) return;
        if (indice < 0 || indice >= tabPaneHorarios.getTabs().size()) return;
        tabPaneHorarios.getSelectionModel().select(indice);
    }

    public void marcarPassoAtual(int indice) {
        VBox[] passos = { stepperPasso1, stepperPasso2, stepperPasso3, stepperPasso4 };
        for (int i = 0; i < passos.length; i++) {
            if (passos[i] == null) continue;
            passos[i].getStyleClass().remove("stepper-passo-atual");
            if (i == indice) {
                passos[i].getStyleClass().add("stepper-passo-atual");
            }
        }
    }

    /**
     * Actualiza o estado visual do stepper e o guia de fluxo contextual.
     *
     * @param emProcessamento tarefa de fundo em curso
     * @param podeGerar       utilizador tem permissão de geração
     * @param propostaAtual   proposta actualmente carregada (pode ser null)
     * @param temRascunho     existem propostas na lista (mesmo que nenhuma seleccionada)
     */
    public void atualizar(boolean emProcessamento, boolean podeGerar,
                           PropostaResultado propostaAtual, boolean temRascunho) {
        if (stepperPasso1 == null) return;

        boolean temProposta = propostaAtual != null;
        boolean enviada = temProposta && propostaAtual.estado() != null
                && normalizar(propostaAtual.estado()).contains("enviado");
        boolean aprovada = temProposta && propostaAtual.estado() != null
                && normalizar(propostaAtual.estado()).contains("aprovad");
        boolean decidida = enviada || aprovada
                || (temProposta && propostaAtual.estado() != null
                        && normalizar(propostaAtual.estado()).contains("rejeitad"));

        aplicarEstado(stepperPasso1, true, temRascunho || temProposta);
        aplicarEstado(stepperPasso2, temRascunho || temProposta || emProcessamento, temRascunho || temProposta);
        aplicarEstado(stepperPasso3, temProposta, decidida || aprovada);
        aplicarEstado(stepperPasso4, decidida, aprovada);

        atualizarGuiaFluxo(temRascunho, temProposta, enviada, aprovada, emProcessamento, podeGerar);
    }

    // ── privados ─────────────────────────────────────────────────────────────

    private void aplicarEstado(VBox passo, boolean ativo, boolean concluido) {
        if (passo == null) return;
        passo.getStyleClass().removeAll("stepper-passo-ativo", "stepper-passo-concluido", "stepper-passo-inativo");
        if (concluido) {
            passo.getStyleClass().add("stepper-passo-concluido");
        } else if (ativo) {
            passo.getStyleClass().add("stepper-passo-ativo");
        } else {
            passo.getStyleClass().add("stepper-passo-inativo");
        }
    }

    private void atualizarGuiaFluxo(boolean temRascunho, boolean temProposta,
                                     boolean enviada, boolean aprovada,
                                     boolean emProcessamento, boolean podeGerar) {
        if (lblGuiaFluxo == null) return;
        String guia;
        if (emProcessamento) {
            guia = "A processar... aguarda.";
        } else if (aprovada) {
            guia = "✔ Proposta aprovada e publicada. O calendário fica disponível no passo 3 (Rever).";
        } else if (enviada) {
            guia = "Proposta enviada ao supervisor. Aguarda a decisão ou gera mais alternativas.";
        } else if (temProposta) {
            guia = "Já tens uma proposta. Usa 'Ver alternativas' para comparar, ou 'Rever proposta' para ver o calendário e enviar.";
        } else if (temRascunho) {
            guia = "Alternativas geradas. Avança para o passo 2 (Alternativas) para analisar e comparar.";
        } else if (podeGerar) {
            guia = "Configura o período, seleciona a equipa e clica em 'Gerar horário'.";
        } else {
            guia = "Seleciona o período para consultar o planeamento.";
        }
        lblGuiaFluxo.setText(guia);
        lblGuiaFluxo.setVisible(true);
        lblGuiaFluxo.setManaged(true);
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT)
                .trim();
    }
}
