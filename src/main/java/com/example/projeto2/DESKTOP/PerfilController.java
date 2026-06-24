package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.PerfilService;
import com.example.projeto2.API.Services.SessaoService;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class PerfilController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerfilController.class);

    @FXML
    private VBox raizPerfil;

    @FXML
    private Label lblNomePerfil;

    @FXML
    private Label lblEmailPerfil;

    @FXML
    private Label lblTelemovelPerfil;

    @FXML
    private Label lblLojaAtual;

    @FXML
    private Label lblCargoAtual;

    @FXML
    private Label lblDataEntrada;

    @FXML
    private Label lblProximoTurno;

    @FXML
    private Label lblHorasMes;

    @FXML
    private Label lblFolgasPendentes;

    @FXML
    private Label lblFolgasAprovadas;

    @FXML
    private Label lblTurnosFuturos;

    @FXML
    private StackPane stkPerfilAvatar;

    @FXML
    private Label lblPerfilAvatar;

    @FXML
    private javafx.scene.control.Button btnRemoverFoto;

    private Consumer<Utilizador> aoAtualizarFoto;
    private final PerfilService perfilBLL;
    private final SessaoService sessaoBLL;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final ApplicationContext applicationContext;
    private Utilizador utilizadorLogado;

    public PerfilController(PerfilService perfilBLL,
                            SessaoService sessaoBLL,
                            LojautilizadorRepository lojautilizadorRepository,
                            ApplicationContext applicationContext) {
        this.perfilBLL = perfilBLL;
        this.sessaoBLL = sessaoBLL;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.applicationContext = applicationContext;
    }

    public void setAoAtualizarFoto(Consumer<Utilizador> callback) {
        this.aoAtualizarFoto = callback;
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;

        if (utilizadorLogado == null) {
            preencherValoresEmFalta();
            return;
        }

        try {
            Integer idLojaAtiva = sessaoBLL.obterLojaAtiva();
            // idLoja é uma serial do Postgres (começa em 1) — null ou <= 0 nunca é válido.
            if (idLojaAtiva == null || idLojaAtiva <= 0) {
                // Rede de segurança: não deveria acontecer depois da Fase 2/3
                // (LoginController/SelecionarLojaController trancam sempre a
                // loja ativa antes do dashboard abrir), mas evita regressão
                // para qualquer caminho de entrada futuro que ainda não o faça.
                idLojaAtiva = resolverLojaAtivaFallback(utilizadorLogado.getId());
            }
            PerfilService.PerfilResumo resumo = perfilBLL.obterResumoPerfil(utilizadorLogado, idLojaAtiva);

            atualizarAvatarPerfil(utilizadorLogado.getFotoPerfil(), resumo.nome());
            if (stkPerfilAvatar != null) stkPerfilAvatar.setCursor(javafx.scene.Cursor.HAND);
            lblNomePerfil.setText(resumo.nome());
            lblEmailPerfil.setText(resumo.email());
            lblTelemovelPerfil.setText(resumo.telemovel());
            lblLojaAtual.setText(resumo.lojaAtual());
            lblCargoAtual.setText(resumo.cargoAtual());
            lblDataEntrada.setText(resumo.dataEntrada());
            lblProximoTurno.setText(resumo.proximoTurno());
            lblHorasMes.setText(resumo.horasEsteMes());
            lblFolgasPendentes.setText(String.valueOf(resumo.pedidosPendentes()));
            lblFolgasAprovadas.setText(String.valueOf(resumo.pedidosAprovados()));
            lblTurnosFuturos.setText(String.valueOf(resumo.turnosFuturos()));
        } catch (IllegalArgumentException e) {
            preencherValoresEmFalta();
            lblNomePerfil.setText(utilizadorLogado.getNome());
            lblProximoTurno.setText(e.getMessage());
        }
    }

    /**
     * Rede de segurança (ver Revisao.md, pontos 16 e 17): só é usada se,
     * excecionalmente, {@code sessaoBLL.obterLojaAtiva()} ainda não tiver
     * sido definida. Agarra a primeira ligação ativa do utilizador, em vez
     * de deixar o overload sem idLoja tentar adivinhar uma única linha onde
     * podem existir 2 ou mais.
     */
    private Integer resolverLojaAtivaFallback(Integer idUtilizador) {
        List<Lojautilizador> ligacoesAtivas = lojautilizadorRepository.findLigacoesAtivasByIdUtilizador(idUtilizador);
        return ligacoesAtivas.isEmpty() ? null : ligacoesAtivas.get(0).getIdLoja().getId();
    }

    @FXML
    public void onAlterarFotoClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar foto de perfil");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File ficheiro = fileChooser.showOpenDialog(obterJanelaAtual());
        if (ficheiro == null) return;
        try {
            byte[] bytes = Files.readAllBytes(ficheiro.toPath());
            if (bytes.length > 2 * 1024 * 1024) {
                mostrarErro("Imagem demasiado grande", "A foto deve ter menos de 2 MB.");
                return;
            }
            String base64 = Base64.getEncoder().encodeToString(bytes);
            Utilizador atualizado = perfilBLL.atualizarFotoPerfil(utilizadorLogado.getId(), base64);
            this.utilizadorLogado = atualizado;
            atualizarAvatarPerfil(atualizado.getFotoPerfil(), atualizado.getNome());
            if (aoAtualizarFoto != null) aoAtualizarFoto.accept(atualizado);
            DialogosHelper.mostrarInformacao(obterJanelaAtual(),
                    "Foto atualizada", "Foto de perfil atualizada",
                    "A tua nova foto foi guardada com sucesso.");
        } catch (java.io.IOException e) {
            LOGGER.error("Erro ao ler ficheiro de foto.", e);
            mostrarErro("Erro ao ler ficheiro", "Não foi possível ler o ficheiro selecionado.");
        } catch (Exception e) {
            LOGGER.error("Erro ao guardar foto de perfil.", e);
            mostrarErro("Erro ao guardar", "Não foi possível guardar a foto. Tenta novamente.");
        }
    }

    private void atualizarAvatarPerfil(String fotoPerfil, String nome) {
        if (stkPerfilAvatar == null) return;
        // Mantém a Circle de fundo (índice 0); substitui o conteúdo seguinte
        while (stkPerfilAvatar.getChildren().size() > 1) {
            stkPerfilAvatar.getChildren().remove(stkPerfilAvatar.getChildren().size() - 1);
        }
        boolean temFoto = false;
        if (fotoPerfil != null && !fotoPerfil.isBlank()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(fotoPerfil);
                javafx.scene.image.Image img = new javafx.scene.image.Image(new ByteArrayInputStream(bytes));
                ImageView iv = new ImageView(img);
                cropCentrado(iv, img, 96);
                iv.setClip(new Circle(48, 48, 48));
                stkPerfilAvatar.getChildren().add(iv);
                temFoto = true;
            } catch (Exception e) {
                LOGGER.warn("Falha ao renderizar foto de perfil.", e);
            }
        }
        if (!temFoto && lblPerfilAvatar != null) {
            lblPerfilAvatar.setText(nome != null && !nome.isBlank()
                    ? String.valueOf(nome.charAt(0)).toUpperCase() : "?");
            if (!stkPerfilAvatar.getChildren().contains(lblPerfilAvatar)) {
                stkPerfilAvatar.getChildren().add(lblPerfilAvatar);
            }
        }
        if (btnRemoverFoto != null) {
            btnRemoverFoto.setVisible(temFoto);
            btnRemoverFoto.setManaged(temFoto);
        }
    }

    private static void cropCentrado(ImageView iv, javafx.scene.image.Image img, double size) {
        double w = img.getWidth();
        double h = img.getHeight();
        double side = Math.min(w, h);
        iv.setViewport(new javafx.geometry.Rectangle2D((w - side) / 2, (h - side) / 2, side, side));
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(false);
    }

    @FXML
    public void onRemoverFotoClick() {
        boolean confirmado = DialogosHelper.confirmarAcao(
                obterJanelaAtual(),
                "Remover foto de perfil",
                "Tens a certeza que queres remover a foto?",
                "Passará a ser usada a letra inicial do teu nome.",
                "Remover"
        );
        if (confirmado) {
            try {
                Utilizador atualizado = perfilBLL.atualizarFotoPerfil(utilizadorLogado.getId(), null);
                this.utilizadorLogado = atualizado;
                atualizarAvatarPerfil(null, atualizado.getNome());
                if (aoAtualizarFoto != null) aoAtualizarFoto.accept(atualizado);
            } catch (Exception e) {
                LOGGER.error("Erro ao remover foto.", e);
                mostrarErro("Erro", "Não foi possível remover a foto.");
            }
        }
    }

    @FXML
    public void onEditarEmailClick() {
        abrirModalComBlur("/com/example/projeto2/dashboard/editar-email-view.fxml", "Editar Email");
    }

    @FXML
    public void onEditarNomeClick() {
        abrirModalComBlur("/com/example/projeto2/dashboard/editar-nome-view.fxml", "Editar Nome");
    }

    @FXML
    public void onEditarTelemovelClick() {
        abrirModalComBlur("/com/example/projeto2/dashboard/editar-telemovel-view.fxml", "Editar Telemóvel");
    }

    @FXML
    public void onAlterarPasswordClick() {
        abrirModalComBlur("/com/example/projeto2/dashboard/alterar-password-view.fxml", "Alterar Palavra-passe");
    }

    private void abrirModalComBlur(String caminhoFxml, String titulo) {
        try {
            raizPerfil.setEffect(new GaussianBlur(10));

            FXMLLoader loader = new FXMLLoader(getClass().getResource(caminhoFxml));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EditarNomeController nomeController) {
                nomeController.setUtilizadorLogado(this.utilizadorLogado);
            } else if (controller instanceof EditarEmailController emailController) {
                emailController.setUtilizadorLogado(this.utilizadorLogado);
            } else if (controller instanceof EditarTelemovelController telemovelController) {
                telemovelController.setUtilizadorLogado(this.utilizadorLogado);
            } else if (controller instanceof AlterarPasswordController passwordController) {
                passwordController.setUtilizadorLogado(this.utilizadorLogado);
            }

            Stage modalStage = new Stage();
            modalStage.setTitle(titulo);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);
            Window owner = obterJanelaAtual();
            if (owner != null) {
                modalStage.initOwner(owner);
                modalStage.initModality(Modality.WINDOW_MODAL);
            } else {
                modalStage.initModality(Modality.APPLICATION_MODAL);
            }
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.setResizable(false);
            modalStage.showAndWait();

            if (this.utilizadorLogado != null && this.utilizadorLogado.getId() != null) {
                this.utilizadorLogado = perfilBLL.obterUtilizadorPorId(this.utilizadorLogado.getId());
            }
            setUtilizadorLogado(this.utilizadorLogado);
        } catch (Exception e) {
            LOGGER.error("Erro ao abrir o modal {}.", caminhoFxml, e);
            mostrarErro(
                    "Não foi possível abrir esta janela.",
                    "Tenta novamente. Se o problema persistir, volta a abrir o perfil."
            );
        } finally {
            raizPerfil.setEffect(null);
        }
    }

    private void preencherValoresEmFalta() {
        lblNomePerfil.setText("-");
        lblEmailPerfil.setText("-");
        lblTelemovelPerfil.setText("-");
        lblLojaAtual.setText("-");
        lblCargoAtual.setText("-");
        lblDataEntrada.setText("-");
        lblProximoTurno.setText("-");
        lblHorasMes.setText("0h 0m");
        lblFolgasPendentes.setText("0");
        lblFolgasAprovadas.setText("0");
        lblTurnosFuturos.setText("0");
    }

    private void mostrarErro(String titulo, String mensagem) {
        DialogosHelper.mostrarErro(obterJanelaAtual(), "Erro", titulo, mensagem);
    }

    private Window obterJanelaAtual() {
        if (raizPerfil == null || raizPerfil.getScene() == null) {
            return null;
        }
        return raizPerfil.getScene().getWindow();
    }
}
