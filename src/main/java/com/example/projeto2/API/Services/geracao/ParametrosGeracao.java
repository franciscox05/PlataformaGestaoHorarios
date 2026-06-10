package com.example.projeto2.API.Services.geracao;

import java.util.Map;

public record ParametrosGeracao(
        Map<Integer, Integer> minimosPorTurno,
        int maxDiasConsecutivos,
        int descansoMinimoHoras,
        int descansoSemanalMinimoDias,
        int janelaRotacaoFinsDeSemanaSemanas,
        int diaLimiteLancamento,
        boolean exigirChefiaAoSabado,
        Map<PerfilContratual, Long> cargaMaximaMinutosPorPerfil
) {
}
