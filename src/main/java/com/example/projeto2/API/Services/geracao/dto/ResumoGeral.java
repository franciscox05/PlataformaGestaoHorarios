package com.example.projeto2.API.Services.geracao.dto;

/** Totais gerais de uma proposta: colaboradores, turnos e dias cobertos. */
public record ResumoGeral(
        int colaboradores,
        int turnos,
        int diasCobertos
) {
}
