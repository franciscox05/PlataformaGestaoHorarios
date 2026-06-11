package com.example.projeto2.API.Services.geracao.dto;

/** Resumo de carga de um colaborador numa proposta: nº de turnos e horas. */
public record ResumoColaborador(
        Integer idColaborador,
        String nome,
        String cargo,
        int turnos,
        long minutos,
        String horasFormatadas
) {
}
