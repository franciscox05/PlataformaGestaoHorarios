package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.ExportacaoPdfService;
import com.example.projeto2.API.Services.geracao.dto.PropostaResultado;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Exportação PDF de um horário mensal: abre {@link FileChooser}, escreve o ficheiro
 * via {@link ExportacaoPdfService}. Análogo a {@link ExportadorHorarioCsv}.
 */
public final class ExportadorHorarioPdf {

    private static final String[] MESES_PT = {
            "janeiro", "fevereiro", "marco", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    private ExportadorHorarioPdf() {}

    public static void exportar(PropostaResultado proposta,
                                int mes,
                                int ano,
                                Window janela,
                                ExportacaoPdfService service,
                                Consumer<String> onSucesso,
                                Consumer<String> onErro) {
        String nomeMes = MESES_PT[mes - 1];
        String nomeMesCapitalizado = Character.toUpperCase(nomeMes.charAt(0)) + nomeMes.substring(1);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar horário mensal para PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fileChooser.setInitialFileName("horario-" + ano + "-" + nomeMes + ".pdf");

        java.io.File ficheiro = fileChooser.showSaveDialog(janela);
        if (ficheiro == null) return;

        try (FileOutputStream fos = new FileOutputStream(ficheiro)) {
            service.exportarHorarioPdf(
                    fos,
                    proposta.origemPlaneamento(),
                    nomeMesCapitalizado + " " + ano,
                    ano,
                    proposta.estado(),
                    proposta.geradoPor(),
                    proposta.linhas()
            );
            onSucesso.accept("PDF exportado com sucesso.");
        } catch (IOException e) {
            onErro.accept("Não foi possível exportar o ficheiro PDF.");
        }
    }
}
