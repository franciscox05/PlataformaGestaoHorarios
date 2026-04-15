package com.example.projeto2.Controller;

import com.example.projeto2.BLL.PerfilBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PerfilController {

    @FXML
    private Label lblNomePerfil;

    @FXML
    private Label lblEmailPerfil;

    @FXML
    private Label lblTelemovelPerfil;

    @FXML
    private Label lblEstadoPerfil;

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

    private final PerfilBLL perfilBLL;

    public PerfilController(PerfilBLL perfilBLL) {
        this.perfilBLL = perfilBLL;
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        if (utilizadorLogado == null) {
            preencherValoresEmFalta();
            return;
        }

        try {
            PerfilBLL.PerfilResumo resumo = perfilBLL.obterResumoPerfil(utilizadorLogado);

            lblNomePerfil.setText(resumo.nome());
            lblEmailPerfil.setText(resumo.email());
            lblTelemovelPerfil.setText(resumo.telemovel());
            lblEstadoPerfil.setText(resumo.estado());
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
            lblEstadoPerfil.setText("Dados indisponiveis");
            lblProximoTurno.setText(e.getMessage());
        }
    }

    private void preencherValoresEmFalta() {
        lblNomePerfil.setText("-");
        lblEmailPerfil.setText("-");
        lblTelemovelPerfil.setText("-");
        lblEstadoPerfil.setText("-");
        lblLojaAtual.setText("-");
        lblCargoAtual.setText("-");
        lblDataEntrada.setText("-");
        lblProximoTurno.setText("-");
        lblHorasMes.setText("0h 0m");
        lblFolgasPendentes.setText("0");
        lblFolgasAprovadas.setText("0");
        lblTurnosFuturos.setText("0");
    }
}
