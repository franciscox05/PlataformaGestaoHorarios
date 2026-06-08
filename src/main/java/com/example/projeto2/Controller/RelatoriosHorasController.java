package com.example.projeto2.Controller;

import com.example.projeto2.BLL.ExportacaoPdfBLL;
import com.example.projeto2.BLL.RelatorioHorasBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Month;
import java.util.List;

@Component
@Scope("prototype")
public class RelatoriosHorasController {

    @FXML
    private Label lblLojaRelatorio;

    @FXML
    private ComboBox<MesOption> cbMes;

    @FXML
    private Spinner<Integer> spAno;

    @FXML
    private ComboBox<RelatorioHorasBLL.FiltroColaborador> cbColaborador;

    @FXML
    private Button btnGerarRelatorio;

    @FXML
    private Button btnExportarCsv;

    @FXML
    private Button btnExportarPdf;

    @FXML
    private Label lblFeedback;

    @FXML
    private Label lblTotalColaboradores;

    @FXML
    private Label lblTotalTurnos;

    @FXML
    private Label lblTotalHoras;

    @FXML
    private Label lblTotalFolgas;

    @FXML
    private TableView<RelatorioHorasBLL.RelatorioLinha> tabelaRelatorio;

    @FXML
    private TableColumn<RelatorioHorasBLL.RelatorioLinha, String> colColaborador;

    @FXML
    private TableColumn<RelatorioHorasBLL.RelatorioLinha, String> colCargo;

    @FXML
    private TableColumn<RelatorioHorasBLL.RelatorioLinha, String> colTurnos;

    @FXML
    private TableColumn<RelatorioHorasBLL.RelatorioLinha, String> colFolgas;

    @FXML
    private TableColumn<RelatorioHorasBLL.RelatorioLinha, String> colHoras;

    private final RelatorioHorasBLL relatorioHorasBLL;
    private final ExportacaoPdfBLL exportacaoPdfBLL;

    private Utilizador utilizadorLogado;
    private RelatorioHorasBLL.RelatorioResultado ultimoResultado;

    public RelatoriosHorasController(RelatorioHorasBLL relatorioHorasBLL,
                                     ExportacaoPdfBLL exportacaoPdfBLL) {
        this.relatorioHorasBLL = relatorioHorasBLL;
        this.exportacaoPdfBLL = exportacaoPdfBLL;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarFiltrosBase();
        tabelaRelatorio.setPlaceholder(new Label("Seleciona os filtros e clica em \"Gerar Relatório\" para ver os dados."));
        btnExportarCsv.setDisable(true);
        if (btnExportarCsv.getTooltip() == null) {
            javafx.scene.control.Tooltip ttCsv = new javafx.scene.control.Tooltip("Gera o relatório primeiro para exportar");
            javafx.scene.control.Tooltip.install(btnExportarCsv, ttCsv);
        }
        if (btnExportarPdf != null) {
            btnExportarPdf.setDisable(true);
            if (btnExportarPdf.getTooltip() == null) {
                javafx.scene.control.Tooltip ttPdf = new javafx.scene.control.Tooltip("Gera o relatório primeiro para exportar");
                javafx.scene.control.Tooltip.install(btnExportarPdf, ttPdf);
            }
        }
        esconderFeedback();
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;

        try {
            RelatorioHorasBLL.RelatorioContexto contexto = relatorioHorasBLL.obterContexto(utilizadorLogado.getId());
            preencherContexto(contexto);
            gerarRelatorio();
        } catch (IllegalArgumentException e) {
            mostrarFeedback(e.getMessage(), false);
            cbMes.setDisable(true);
            spAno.setDisable(true);
            cbColaborador.setDisable(true);
            btnGerarRelatorio.setDisable(true);
            btnExportarCsv.setDisable(true);
            if (btnExportarPdf != null) btnExportarPdf.setDisable(true);
        }
    }

    @FXML
    public void onGerarRelatorioClick() {
        gerarRelatorio();
    }

    @FXML
    public void onExportarPdfClick() {
        if (ultimoResultado == null || ultimoResultado.linhas().isEmpty()) {
            mostrarFeedback("Gera primeiro um relatório com dados para exportar.", false);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar relatório de horas para PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fileChooser.setInitialFileName(
                "relatorio-horas-" + ultimoResultado.ano()
                + "-" + ultimoResultado.nomeMes().toLowerCase() + ".pdf");

        java.io.File ficheiro = fileChooser.showSaveDialog(
                btnExportarPdf.getScene() != null ? btnExportarPdf.getScene().getWindow() : null);
        if (ficheiro == null) return;

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(ficheiro)) {
            exportacaoPdfBLL.exportarRelatorioPdf(
                    fos,
                    ultimoResultado.nomeLoja(),
                    ultimoResultado.nomeMes() + " " + ultimoResultado.ano(),
                    ultimoResultado.ano(),
                    ultimoResultado.linhas(),
                    ultimoResultado.resumo()
            );
            mostrarFeedback("PDF exportado com sucesso.", true);
        } catch (IOException e) {
            mostrarFeedback("Não foi possível exportar o ficheiro PDF.", false);
        }
    }

    @FXML
    public void onExportarCsvClick() {
        if (ultimoResultado == null || ultimoResultado.linhas().isEmpty()) {
            mostrarFeedback("Gera primeiro um relatório com dados para exportar.", false);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar relatório mensal");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("relatorio-horas-" + ultimoResultado.ano() + "-" + ultimoResultado.nomeMes().toLowerCase() + ".csv");

        java.io.File ficheiro = fileChooser.showSaveDialog(btnExportarCsv.getScene().getWindow());
        if (ficheiro == null) {
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(ficheiro.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Loja;Mes;Ano");
            writer.newLine();
            writer.write(ultimoResultado.nomeLoja() + ";" + formatarNomeMesAtual() + ";" + ultimoResultado.ano());
            writer.newLine();
            writer.newLine();

            writer.write("Colaborador;Cargo;Turnos;Folgas aprovadas;Horas");
            writer.newLine();

            for (RelatorioHorasBLL.RelatorioLinha linha : ultimoResultado.linhas()) {
                writer.write(sanitizarCsv(linha.nomeColaborador()) + ";"
                        + sanitizarCsv(linha.cargo()) + ";"
                        + linha.turnos() + ";"
                        + linha.folgasAprovadas() + ";"
                        + sanitizarCsv(linha.horasFormatadas()));
                writer.newLine();
            }

            writer.newLine();
            writer.write("Resumo;;;;");
            writer.newLine();
            writer.write("Colaboradores;" + ultimoResultado.resumo().colaboradores());
            writer.newLine();
            writer.write("Turnos;" + ultimoResultado.resumo().turnos());
            writer.newLine();
            writer.write("Folgas aprovadas;" + ultimoResultado.resumo().folgasAprovadas());
            writer.newLine();
            writer.write("Horas totais;" + sanitizarCsv(ultimoResultado.resumo().horasFormatadas()));

            mostrarFeedback("Relatório exportado com sucesso.", true);
        } catch (IOException e) {
            mostrarFeedback("Não foi possível exportar o ficheiro CSV.", false);
        }
    }

    private void configurarTabela() {
        colColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().nomeColaborador()));

        colCargo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().cargo()));

        colTurnos.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().turnos())));

        colFolgas.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().folgasAprovadas())));

        colHoras.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().horasFormatadas()));
    }

    private void configurarFiltrosBase() {
        cbMes.setItems(FXCollections.observableArrayList(
                new MesOption(1, "Janeiro"),
                new MesOption(2, "Fevereiro"),
                new MesOption(3, "Março"),
                new MesOption(4, "Abril"),
                new MesOption(5, "Maio"),
                new MesOption(6, "Junho"),
                new MesOption(7, "Julho"),
                new MesOption(8, "Agosto"),
                new MesOption(9, "Setembro"),
                new MesOption(10, "Outubro"),
                new MesOption(11, "Novembro"),
                new MesOption(12, "Dezembro")
        ));

        spAno.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2024, 2100, java.time.LocalDate.now().getYear()));
        spAno.setEditable(true);

        cbMes.valueProperty().addListener((observavel, antigo, valor) -> invalidarRelatorioAtual());
        cbColaborador.valueProperty().addListener((observavel, antigo, valor) -> invalidarRelatorioAtual());
        spAno.valueProperty().addListener((observavel, antigo, valor) -> invalidarRelatorioAtual());
    }

    private void preencherContexto(RelatorioHorasBLL.RelatorioContexto contexto) {
        lblLojaRelatorio.setText(contexto.nomeLoja());
        spAno.getValueFactory().setValue(contexto.anoAtual());
        cbMes.getItems().stream()
                .filter(item -> item.numero() == contexto.mesAtual())
                .findFirst()
                .ifPresent(cbMes::setValue);

        List<RelatorioHorasBLL.FiltroColaborador> colaboradores = new java.util.ArrayList<>();
        colaboradores.add(new RelatorioHorasBLL.FiltroColaborador(null, "Todos os colaboradores", "-"));
        colaboradores.addAll(contexto.colaboradores());
        cbColaborador.setItems(FXCollections.observableArrayList(colaboradores));
        cbColaborador.getSelectionModel().selectFirst();
    }

    private void gerarRelatorio() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            MesOption mesSelecionado = cbMes.getValue();
            if (mesSelecionado == null) {
                throw new IllegalArgumentException("Seleciona um mês para gerar o relatório.");
            }

            RelatorioHorasBLL.FiltroColaborador colaboradorSelecionado = cbColaborador.getValue();
            Integer idColaborador = colaboradorSelecionado != null ? colaboradorSelecionado.id() : null;

            ultimoResultado = relatorioHorasBLL.gerarRelatorio(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    mesSelecionado.numero(),
                    idColaborador
            );

            tabelaRelatorio.setItems(FXCollections.observableArrayList(ultimoResultado.linhas()));
            atualizarResumo(ultimoResultado);
            boolean temDados = !ultimoResultado.linhas().isEmpty();
            btnExportarCsv.setDisable(!temDados);
            if (btnExportarCsv.getTooltip() != null) {
                btnExportarCsv.getTooltip().setText(temDados ? "Exportar tabela em formato CSV" : "Sem dados para exportar");
            }
            if (btnExportarPdf != null) {
                btnExportarPdf.setDisable(!temDados);
                if (btnExportarPdf.getTooltip() != null) {
                    btnExportarPdf.getTooltip().setText(temDados ? "Exportar relatório em PDF" : "Sem dados para exportar");
                }
            }
            mostrarFeedback("Relatório gerado com sucesso.", true);
        } catch (IllegalArgumentException e) {
            tabelaRelatorio.setItems(FXCollections.observableArrayList());
            limparResumo();
            btnExportarCsv.setDisable(true);
        if (btnExportarPdf != null) btnExportarPdf.setDisable(true);
            mostrarFeedback(e.getMessage(), false);
        } catch (Exception e) {
            tabelaRelatorio.setItems(FXCollections.observableArrayList());
            limparResumo();
            btnExportarCsv.setDisable(true);
        if (btnExportarPdf != null) btnExportarPdf.setDisable(true);
            mostrarFeedback("Não foi possível gerar o relatório.", false);
        }
    }

    private void atualizarResumo(RelatorioHorasBLL.RelatorioResultado resultado) {
        lblTotalColaboradores.setText(String.valueOf(resultado.resumo().colaboradores()));
        lblTotalTurnos.setText(String.valueOf(resultado.resumo().turnos()));
        lblTotalHoras.setText(resultado.resumo().horasFormatadas());
        lblTotalFolgas.setText(String.valueOf(resultado.resumo().folgasAprovadas()));
    }

    private void limparResumo() {
        lblTotalColaboradores.setText("0");
        lblTotalTurnos.setText("0");
        lblTotalHoras.setText("0h 0m");
        lblTotalFolgas.setText("0");
    }

    private void invalidarRelatorioAtual() {
        if (utilizadorLogado == null) {
            return;
        }

        ultimoResultado = null;
        tabelaRelatorio.setItems(FXCollections.observableArrayList());
        limparResumo();
        btnExportarCsv.setDisable(true);
        if (btnExportarPdf != null) btnExportarPdf.setDisable(true);
        esconderFeedback();
    }

    private void mostrarFeedback(String mensagem, boolean sucesso) {
        lblFeedback.setText(mensagem);
        lblFeedback.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblFeedback.getStyleClass().add("mensagem-feedback");
        lblFeedback.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblFeedback.setVisible(true);
        lblFeedback.setManaged(true);
    }

    private void esconderFeedback() {
        lblFeedback.setVisible(false);
        lblFeedback.setManaged(false);
        lblFeedback.setText("");
    }

    private String formatarNomeMesAtual() {
        MesOption mesSelecionado = cbMes.getValue();
        return mesSelecionado != null ? mesSelecionado.nome() : ultimoResultado.nomeMes();
    }

    private String sanitizarCsv(String valor) {
        if (valor == null) {
            return "";
        }

        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private record MesOption(int numero, String nome) {
        @Override
        public String toString() {
            return nome;
        }
    }
}
