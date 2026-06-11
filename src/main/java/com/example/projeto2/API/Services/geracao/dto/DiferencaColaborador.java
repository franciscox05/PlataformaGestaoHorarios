package com.example.projeto2.API.Services.geracao.dto;

/** Diferença de carga de um colaborador entre duas propostas comparadas. */
public record DiferencaColaborador(
        Integer idColaborador,
        String colaborador,
        String cargo,
        int turnosBase,
        String horasBase,
        int turnosComparada,
        String horasComparada,
        int diferencaTurnos,
        String diferencaHoras
) {
}
