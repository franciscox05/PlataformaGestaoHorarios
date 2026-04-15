package com.example.projeto2.Controller;

import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Utilizador;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class PermutasController {

    @FXML
    private ComboBox<Horario> cbMeuTurno;
    @FXML
    private ComboBox<Horario> cbTurnoColega;
    @FXML
    private Label lblMensagem;

    private final HorarioBLL horarioBll;
    private final PermutaBLL permutaBll;
    private Utilizador utilizadorLogado;

    public PermutasController(HorarioBLL horarioBll, PermutaBLL permutaBll) {
        this.horarioBll = horarioBll;
        this.permutaBll = permutaBll;
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
        System.out.println("🔄 Ecrã de Permutas aberto para: " + utilizador.getNome());

        configurarFormatacaoCombo();
        carregarMeusTurnos();
    }

    private void configurarFormatacaoCombo() {
        // Conversor para a Caixa 1: Os MEUS turnos
        StringConverter<Horario> conversorMeus = new StringConverter<>() {
            @Override
            public String toString(Horario h) {
                if (h == null) return "";
                return h.getDataTurno() + " | " + h.getIdTurno().getHoraInicio() + " - " + h.getIdTurno().getHoraFim();
            }

            @Override
            public Horario fromString(String string) {
                return null;
            }
        };

        // Conversor para a Caixa 2: Turnos dos COLEGAS (Mostra o nome deles!)
        StringConverter<Horario> conversorColegas = new StringConverter<>() {
            @Override
            public String toString(Horario h) {
                if (h == null) return "";
                // Vai buscar o nome do colega através das relações da Base de Dados!
                String nomeColega = h.getIdLojautilizador().getIdUtilizador().getNome();
                return nomeColega + " | " + h.getDataTurno() + " | " + h.getIdTurno().getHoraInicio() + " - " + h.getIdTurno().getHoraFim();
            }

            @Override
            public Horario fromString(String string) {
                return null;
            }
        };

        cbMeuTurno.setConverter(conversorMeus);
        cbTurnoColega.setConverter(conversorColegas);

        // Quando escolho o meu turno, carrega os dos colegas
        cbMeuTurno.setOnAction(e -> carregarTurnosColegas());
    }

    private void carregarMeusTurnos() {
        if (utilizadorLogado != null) {
            // Vai à base de dados buscar os turnos futuros deste utilizador
            List<Horario> meusTurnos = horarioBll.listarProximosTurnos(utilizadorLogado.getId());
            cbMeuTurno.setItems(FXCollections.observableArrayList(meusTurnos));
        }
    }

    private void carregarTurnosColegas() {
        if (cbMeuTurno.getValue() == null) return;

        // Vai à BLL buscar toda a gente exceto eu
        List<Horario> turnosColegas = horarioBll.listarTurnosColegas(utilizadorLogado.getId());

        // Preenche a 2ª caixa!
        cbTurnoColega.setItems(FXCollections.observableArrayList(turnosColegas));
    }

    @FXML
    public void onSubmeterTrocaClick() {
        Horario meuTurno = cbMeuTurno.getValue();
        Horario turnoColega = cbTurnoColega.getValue();

        // 1. Validação
        if (meuTurno == null || turnoColega == null) {
            lblMensagem.setText("⚠️ Por favor, seleciona ambos os turnos para a troca.");
            lblMensagem.setStyle("-fx-text-fill: #c41230;");
            lblMensagem.setVisible(true);
            return;
        }

        try {
            // 2. Gravar na Base de Dados
            System.out.println("A gravar permuta na BD...");
            permutaBll.registarPedidoTroca(meuTurno, turnoColega);

            // 3. Feedback Visual de Sucesso
            lblMensagem.setText("✅ Pedido de troca submetido com sucesso!");
            lblMensagem.setStyle("-fx-text-fill: #2e7d32;");
            lblMensagem.setVisible(true);

            // 4. Limpar as caixas para nova operação
            cbMeuTurno.setValue(null);
            cbTurnoColega.setValue(null);

        } catch (Exception e) {
            System.out.println("❌ Erro ao gravar permuta: " + e.getMessage());
            lblMensagem.setText("❌ Ocorreu um erro ao gravar o pedido.");
            lblMensagem.setStyle("-fx-text-fill: #c41230;");
            lblMensagem.setVisible(true);
        }
    }
}