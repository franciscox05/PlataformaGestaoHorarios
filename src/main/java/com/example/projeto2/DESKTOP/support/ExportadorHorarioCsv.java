package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Exportação CSV de um horário mensal: abre {@link FileChooser}, escreve cabeçalho com
 * loja/proposta/estado e uma linha por turno, ordenadas por data e colaborador.
 */
public final class ExportadorHorarioCsv {

    private static final String[] MESES_PT = {
            "janeiro", "fevereiro", "marco", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    private ExportadorHorarioCsv() {
        // utilitário
    }

    /**
     * @param proposta horário a exportar (não-nulo)
     * @param mes 1..12
     * @param ano ano do horário
     * @param janela owner do {@link FileChooser}
     * @param onSucesso callback ao escrever com sucesso (mensagem pronta para mostrar)
     * @param onErro callback se ocorrer {@link IOException} ao escrever
     */
    public static void exportar(PropostaResultado proposta,
                                int mes,
                                int ano,
                                Window janela,
                                Consumer<String> onSucesso,
                                Consumer<String> onErro) {
        String nomeMes = MESES_PT[mes - 1];

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar horário mensal");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("horario-" + ano + "-" + nomeMes + ".csv");

        File ficheiro = fileChooser.showSaveDialog(janela);
        if (ficheiro == null) return;

        try (BufferedWriter writer = Files.newBufferedWriter(ficheiro.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Loja;" + proposta.origemPlaneamento()
                    + ";Mês;" + nomeMes + ";Ano;" + ano);
            writer.newLine();
            writer.write("Proposta;" + sanitizar(proposta.geradoPor())
                    + ";Estado;" + sanitizar(proposta.estado()));
            writer.newLine();
            writer.newLine();
            writer.write("Colaborador;Cargo;Data;Dia Semana;Período;Turno;Estado");
            writer.newLine();

            List<HorarioLinha> linhasOrdenadas = proposta.linhas().stream()
                    .sorted(Comparator.comparing(HorarioLinha::data,
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(HorarioLinha::colaborador,
                                    Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();

            for (HorarioLinha linha : linhasOrdenadas) {
                writer.write(sanitizar(linha.colaborador()) + ";"
                        + sanitizar(linha.cargo()) + ";"
                        + (linha.data() != null ? linha.data().toString() : "") + ";"
                        + sanitizar(linha.diaSemana()) + ";"
                        + sanitizar(linha.periodo()) + ";"
                        + sanitizar(linha.turno()) + ";"
                        + sanitizar(linha.estado()));
                writer.newLine();
            }

            onSucesso.accept("Horário exportado com sucesso.");
        } catch (IOException e) {
            onErro.accept("Não foi possível exportar o ficheiro CSV.");
        }
    }

    private static String sanitizar(String valor) {
        if (valor == null) return "";
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}
