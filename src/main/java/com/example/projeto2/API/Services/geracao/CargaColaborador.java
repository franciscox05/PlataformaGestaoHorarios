package com.example.projeto2.API.Services.geracao;

public record CargaColaborador(
        Integer idColaborador,
        String nome,
        long minutos,
        long cargaMaximaMinutos,
        int finsDeSemanaTrabalhados
) {
}
