package com.example.projeto2.API.Services.geracao.dto;

/**
 * Resumo de uma proposta para listagem e comparação. {@code recomendada} marca a
 * alternativa que o algoritmo identificou como a mais ótima do período.
 */
public record PropostaResumo(
        Integer idProposta,
        String rotulo,
        String estado,
        String dataGeracao,
        String geradoPor,
        String politicaOtimizacao,
        int pontuacao,
        String qualidade,
        String desvioMedioHoras,
        String amplitudeHoras,
        int colaboradores,
        int turnos,
        int diasCobertos,
        boolean recomendada
) {
    /** Cópia desta proposta com a marca de recomendação alterada. */
    public PropostaResumo comRecomendacao(boolean recomendar) {
        return new PropostaResumo(idProposta, rotulo, estado, dataGeracao, geradoPor,
                politicaOtimizacao, pontuacao, qualidade, desvioMedioHoras, amplitudeHoras,
                colaboradores, turnos, diasCobertos, recomendar);
    }

    @Override
    public String toString() {
        return rotulo + " · score " + pontuacao + (recomendada ? " · ★ recomendada" : "");
    }
}
