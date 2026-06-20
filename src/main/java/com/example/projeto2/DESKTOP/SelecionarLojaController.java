package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.SessaoService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ecrã intermédio pós-login para utilizadores com vínculo ativo a mais do
 * que uma loja — espelha exatamente o papel de
 * {@code WebLoginController#selecionarLojaPage}/{@code escolherLoja} no
 * portal Web. Ver Revisao.md, ponto 17 (Fase 2 &amp; 3).
 */
@Component
public class SelecionarLojaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelecionarLojaController.class);

    @FXML
    private ListView<Lojautilizador> listaLojas;

    @FXML
    private Button btnConfirmarLoja;

    @FXML
    private Label lblErroSelecao;

    private final SessaoService sessaoBLL;
    private final ApplicationContext applicationContext;

    private Utilizador utilizadorLogado;

    public SelecionarLojaController(SessaoService sessaoBLL, ApplicationContext applicationContext) {
        this.sessaoBLL = sessaoBLL;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        listaLojas.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Lojautilizador item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getIdLoja() == null) {
                    setText(null);
                } else {
                    setText(item.getIdLoja().getNome());
                }
            }
        });

        listaLojas.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> btnConfirmarLoja.setDisable(newValue == null));
    }

    /**
     * Chamado pelo {@code LoginController} imediatamente depois do
     * carregamento do FXML, com o utilizador autenticado e as suas ligações
     * ativas (já sabemos, nesse ponto, que são 2 ou mais).
     */
    public void inicializarComLigacoes(Utilizador utilizador, List<Lojautilizador> ligacoesAtivas) {
        this.utilizadorLogado = utilizador;
        listaLojas.getItems().setAll(ligacoesAtivas);
    }

    @FXML
    public void onConfirmarClick() {
        Lojautilizador escolhida = listaLojas.getSelectionModel().getSelectedItem();
        if (escolhida == null || escolhida.getIdLoja() == null) {
            mostrarErro("Seleciona uma loja antes de continuar.");
            return;
        }

        sessaoBLL.definirLojaAtiva(escolhida.getIdLoja().getId());
        abrirDashboard(utilizadorLogado);
    }

    @FXML
    public void onFecharAplicacaoClick() {
        Stage stage = (Stage) listaLojas.getScene().getWindow();
        if (stage != null) {
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }

    private void abrirDashboard(Utilizador logado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/dashboard/dashboard-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            DashboardController dashboardController = loader.getController();
            dashboardController.setUtilizadorLogado(logado);

            Stage stage = (Stage) listaLojas.getScene().getWindow();
            stage.setScene(new Scene(root, UIConstants.APP_WIDTH, UIConstants.APP_HEIGHT));
            stage.setTitle("Levi's Staff Portal - Painel (UI V2)");
            stage.setMinWidth(UIConstants.APP_MIN_WIDTH);
            stage.setMinHeight(UIConstants.APP_MIN_HEIGHT);
            stage.setResizable(false);
            stage.setFullScreen(true);
        } catch (Exception e) {
            LOGGER.error("Erro ao abrir o dashboard depois da seleção de loja.", e);
            mostrarErro("Não foi possível abrir o painel em segurança. Tenta novamente.");
        }
    }

    private void mostrarErro(String mensagem) {
        lblErroSelecao.setText(mensagem);
        lblErroSelecao.setVisible(true);
        lblErroSelecao.setManaged(true);
    }
}
