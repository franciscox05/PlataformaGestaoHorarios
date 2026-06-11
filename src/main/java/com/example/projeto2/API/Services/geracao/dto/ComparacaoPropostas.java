package com.example.projeto2.API.Services.geracao.dto;

import java.util.List;

/** Resultado da comparação entre duas propostas do mesmo período. */
public record ComparacaoPropostas(
        Integer idPropostaBase,
        Integer idPropostaComparada,
        String rotuloBase,
        String rotuloComparada,
        String resumo,
        int turnosDiferentes,
        int diasAfetados,
        List<DiferencaColaborador> diferencas
) {
}
