package com.example.projeto2.API.Services.geracao;

public record MetricasPlaneamento(
        int pontuacao,
        String qualidade,
        String politicaOtimizacao,
        String descricaoPolitica,
        String amplitudeHoras,
        String desvioMedioHoras,
        int amplitudeFinsDeSemana,
        String resumo
) {
}
