package com.example.projeto2;

import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashboardController {

    // Ligações ao ficheiro FXML
    @FXML private Label lblBemVindo;
    @FXML private TableView<Horario> tabelaTurnos;
    @FXML private TableColumn<Horario, String> colData;
    @FXML private TableColumn<Horario, String> colHorario;
    @FXML private TableColumn<Horario, String> colLoja;
    @FXML private TableColumn<Horario, String> colEstado;

    private final HorarioBLL horarioBll;
    private Utilizador utilizadorLogado;

    public DashboardController(HorarioBLL horarioBll) {
        this.horarioBll = horarioBll;
    }

    // Método chamado pelo LoginController
    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;

        // 1. Muda o título da janela para ter o nome da pessoa
        lblBemVindo.setText("Bem-vindo(a), " + utilizador.getNome() + "!");

        // 2. Prepara e carrega a tabela com os turnos desta pessoa
        configurarTabela();
        carregarTurnos();
    }

    private void configurarTabela() {
        // Ensinar a Coluna DATA
        colData.setCellValueFactory(cellData -> {
            Horario h = cellData.getValue();
            if (h.getDataTurno() != null) {
                return new SimpleStringProperty(String.valueOf(h.getDataTurno()));
            }
            return new SimpleStringProperty("Sem Data");
        });

        // Ensinar a Coluna HORÁRIO
        colHorario.setCellValueFactory(cellData -> {
            Horario h = cellData.getValue();
            if (h.getIdTurno() != null && h.getIdTurno().getHoraInicio() != null && h.getIdTurno().getHoraFim() != null) {
                String inicio = String.valueOf(h.getIdTurno().getHoraInicio());
                String fim = String.valueOf(h.getIdTurno().getHoraFim());
                return new SimpleStringProperty(inicio + " - " + fim);
            }
            return new SimpleStringProperty("Sem Turno");
        });

        // Ensinar a Coluna LOJA
        colLoja.setCellValueFactory(cellData -> {
            Horario h = cellData.getValue();
            if (h.getIdLojautilizador() != null && h.getIdLojautilizador().getIdLoja() != null && h.getIdLojautilizador().getIdLoja().getNome() != null) {
                return new SimpleStringProperty(String.valueOf(h.getIdLojautilizador().getIdLoja().getNome()));
            }
            return new SimpleStringProperty("Loja Desconhecida");
        });

        // Ensinar a Coluna ESTADO
        colEstado.setCellValueFactory(cellData -> {
            Horario h = cellData.getValue();
            if (h.getEstado() != null) {
                return new SimpleStringProperty(String.valueOf(h.getEstado()));
            }
            return new SimpleStringProperty("Desconhecido");
        });
    }

    private void carregarTurnos() {
        // 1. Vai ao PostgreSQL
        List<Horario> turnos = horarioBll.listarProximosTurnos(utilizadorLogado.getId());

        // --- A NOSSA LINHA DETETIVE ---
        System.out.println("🕵️ Turnos encontrados na BD para o Tiago: " + turnos.size());
        // ------------------------------

        // 2. Transforma a Lista normal numa "Lista Visual"
        ObservableList<Horario> turnosFX = FXCollections.observableArrayList(turnos);

        // 3. Injeta na tabela
        tabelaTurnos.setItems(turnosFX);
    }
}