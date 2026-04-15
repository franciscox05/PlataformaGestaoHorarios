package com.example.projeto2.Controller;

import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class HomeController {

    @FXML private Label lblBemVindo;
    @FXML private TableView<Horario> tabelaTurnos;
    @FXML private TableColumn<Horario, String> colData;
    @FXML private TableColumn<Horario, String> colHorario;
    @FXML private TableColumn<Horario, String> colLoja;
    @FXML private TableColumn<Horario, String> colEstado;

    private final HorarioBLL horarioBll;
    private Utilizador utilizadorLogado;

    public HomeController(HorarioBLL horarioBll) {
        this.horarioBll = horarioBll;
    }

    // Este method será chamado pelo DashboardController logo após injetar este ecrã
    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
        lblBemVindo.setText("Bem-vindo(a), " + utilizador.getNome() + "!");

        configurarTabela();
        carregarTurnos();
    }

    private void configurarTabela() {
        colData.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getDataTurno())));

        colHorario.setCellValueFactory(cellData -> {
            Horario h = cellData.getValue();
            return new SimpleStringProperty(h.getIdTurno().getHoraInicio() + " - " + h.getIdTurno().getHoraFim());
        });

        colLoja.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIdLojautilizador().getIdLoja().getNome()));

        colEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getEstado())));
    }

    private void carregarTurnos() {
        if (utilizadorLogado != null) {
            List<Horario> turnos = horarioBll.listarProximosTurnos(utilizadorLogado.getId());
            tabelaTurnos.setItems(FXCollections.observableArrayList(turnos));
        }
    }
}