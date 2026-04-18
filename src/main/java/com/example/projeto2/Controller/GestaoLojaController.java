package com.example.projeto2.Controller;

import com.example.projeto2.BLL.GestaoLojaBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class GestaoLojaController {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label lblNomeLoja;

    @FXML
    private Label lblLocalizacao;

    @FXML
    private Label lblCargoGestor;

    @FXML
    private TextField txtHoraAbertura;

    @FXML
    private TextField txtHoraFecho;

    @FXML
    private Label lblMensagem;

    @FXML
    private VBox regrasContainer;

    private final GestaoLojaBLL gestaoLojaBLL;
    private final Map<Integer, TextField> camposValor = new LinkedHashMap<>();
    private final Map<Integer, TextArea> camposObservacoes = new LinkedHashMap<>();
    private Utilizador utilizadorLogado;

    public GestaoLojaController(GestaoLojaBLL gestaoLojaBLL) {
        this.gestaoLojaBLL = gestaoLojaBLL;
    }

    @FXML
    public void initialize() {
        esconderMensagem();
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarDados();
    }

    @FXML
    public void onGuardarClick() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            LocalTime horaAbertura = parseHora(txtHoraAbertura.getText(), "abertura");
            LocalTime horaFecho = parseHora(txtHoraFecho.getText(), "fecho");

            List<GestaoLojaBLL.ConfiguracaoRegraRequest> regras = new ArrayList<>();
            for (Map.Entry<Integer, TextField> entry : camposValor.entrySet()) {
                Integer idRegra = entry.getKey();
                Integer valorEspecifico = parseInteiroOpcional(entry.getValue().getText(), idRegra);
                TextArea campoObservacoes = camposObservacoes.get(idRegra);
                String observacoes = campoObservacoes != null ? campoObservacoes.getText() : null;
                regras.add(new GestaoLojaBLL.ConfiguracaoRegraRequest(idRegra, valorEspecifico, observacoes));
            }

            gestaoLojaBLL.guardarConfiguracao(
                    utilizadorLogado.getId(),
                    new GestaoLojaBLL.ConfiguracaoLojaRequest(horaAbertura, horaFecho, regras)
            );

            mostrarMensagem("Configuracao da loja guardada com sucesso.", true);
            carregarDados();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Nao foi possivel guardar a configuracao da loja.", false);
        }
    }

    private void carregarDados() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            GestaoLojaBLL.GestaoLojaResumo resumo = gestaoLojaBLL.obterResumo(utilizadorLogado.getId());

            lblNomeLoja.setText(resumo.nomeLoja());
            lblLocalizacao.setText(resumo.localizacao());
            lblCargoGestor.setText(resumo.cargoGestor());
            txtHoraAbertura.setText(resumo.horaAbertura());
            txtHoraFecho.setText(resumo.horaFecho());

            preencherRegras(resumo.regras());
        } catch (IllegalArgumentException e) {
            lblNomeLoja.setText("-");
            lblLocalizacao.setText("-");
            lblCargoGestor.setText("-");
            txtHoraAbertura.clear();
            txtHoraFecho.clear();
            preencherRegras(List.of());
            mostrarMensagem(e.getMessage(), false);
        }
    }

    private void preencherRegras(List<GestaoLojaBLL.RegraLojaResumo> regras) {
        regrasContainer.getChildren().clear();
        camposValor.clear();
        camposObservacoes.clear();

        if (regras == null || regras.isEmpty()) {
            Label semRegras = new Label("Nao existem regras base configuradas para apresentar nesta loja.");
            semRegras.getStyleClass().add("subtitulo");
            regrasContainer.getChildren().add(semRegras);
            return;
        }

        for (GestaoLojaBLL.RegraLojaResumo regra : regras) {
            regrasContainer.getChildren().add(criarCardRegra(regra));
        }
    }

    private VBox criarCardRegra(GestaoLojaBLL.RegraLojaResumo regra) {
        VBox card = new VBox(10);
        card.getStyleClass().add("regra-card");
        card.setPadding(new Insets(16));

        Label lblTitulo = new Label(regra.descricao());
        lblTitulo.getStyleClass().add("card-titulo");
        lblTitulo.setWrapText(true);

        String valorPadrao = regra.valorPadrao() != null ? String.valueOf(regra.valorPadrao()) : "sem valor base";
        Label lblDetalhe = new Label("Tipo: " + formatarTipo(regra.tipo()) + " | Valor base: " + valorPadrao);
        lblDetalhe.getStyleClass().add("subtitulo");
        lblDetalhe.setWrapText(true);

        Label lblValor = new Label("Valor especifico da loja");
        lblValor.getStyleClass().add("campo-titulo");

        TextField txtValor = new TextField();
        txtValor.getStyleClass().add("campo-input");
        txtValor.setPromptText("Usar valor base");
        if (regra.valorEspecifico() != null) {
            txtValor.setText(String.valueOf(regra.valorEspecifico()));
        }

        Label lblObs = new Label("Observacoes");
        lblObs.getStyleClass().add("campo-titulo");

        TextArea txtObs = new TextArea();
        txtObs.getStyleClass().add("campo-textarea");
        txtObs.setPromptText("Notas internas sobre esta regra");
        txtObs.setWrapText(true);
        txtObs.setPrefRowCount(2);
        if (regra.observacoes() != null) {
            txtObs.setText(regra.observacoes());
        }

        HBox linhaValor = new HBox(12, lblValor, txtValor);
        linhaValor.setFillHeight(true);
        HBox.setHgrow(txtValor, Priority.ALWAYS);

        card.getChildren().addAll(lblTitulo, lblDetalhe, linhaValor, lblObs, txtObs);

        camposValor.put(regra.idRegra(), txtValor);
        camposObservacoes.put(regra.idRegra(), txtObs);

        return card;
    }

    private LocalTime parseHora(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Indica a hora de " + campo + " no formato HH:mm.");
        }

        try {
            return LocalTime.parse(texto.trim(), HORA_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("A hora de " + campo + " deve seguir o formato HH:mm.");
        }
    }

    private Integer parseInteiroOpcional(String texto, Integer idRegra) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O valor especifico da regra " + idRegra + " deve ser um numero inteiro.");
        }
    }

    private String formatarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "geral";
        }

        return Character.toUpperCase(tipo.charAt(0)) + tipo.substring(1).toLowerCase();
    }

    private void esconderMensagem() {
        lblMensagem.setManaged(false);
        lblMensagem.setVisible(false);
    }

    private void mostrarMensagem(String mensagem, boolean sucesso) {
        lblMensagem.setText(mensagem);
        lblMensagem.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblMensagem.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblMensagem.setManaged(true);
        lblMensagem.setVisible(true);
    }
}
