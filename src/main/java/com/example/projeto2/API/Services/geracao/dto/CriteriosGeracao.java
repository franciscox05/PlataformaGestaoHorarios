package com.example.projeto2.API.Services.geracao.dto;

import java.util.List;

/**
 * Resumo legível de tudo o que o motor de geração tem em conta para um período:
 * regras hard ativas, cargas contratuais, mínimos por turno e o volume de
 * preferências/ausências aprovadas que vão influenciar o resultado.
 *
 * <p>Mostrado ao gestor antes de gerar, para que perceba e confie no processo —
 * e saiba onde mexer quando a geração falha ou o resultado não é o esperado.
 *
 * @param descansoMinimoHoras        RFS06 — horas mínimas entre turnos consecutivos
 * @param maxDiasConsecutivos        RFS07 — teto de dias seguidos de trabalho
 * @param descansoSemanalMinimoDias  RFS04 — dias de folga por semana
 * @param janelaRotacaoFimDeSemana   RFS08 — semanas mínimas entre fins de semana trabalhados
 * @param exigirChefiaAoSabado       RFS03 — gerente/subgerente obrigatório ao sábado
 * @param minimosPorTurno            descrição legível por turno (ex.: "Manhã 08:00–16:00 — mínimo 2")
 * @param cargasPorPerfil            descrição legível por perfil (ex.: "Full-time — 160h/mês")
 * @param totalColaboradoresElegiveis colaboradores com vínculo válido no período
 * @param totalAusenciasAprovadas    folgas/férias aprovadas que bloqueiam dias (hard)
 * @param totalFolgasPreferidas      pedidos de folga preferida aprovados (soft — o motor tenta honrar)
 * @param totalPreferenciasTurno     preferências de turno aprovadas (soft — favorece no scoring)
 * @param totalPreferenciasColegas   preferências de colegas aprovadas (soft)
 * @param totalDiasEspeciais         dias com configuração especial (encerramento/horário reduzido)
 */
public record CriteriosGeracao(
        int descansoMinimoHoras,
        int maxDiasConsecutivos,
        int descansoSemanalMinimoDias,
        int janelaRotacaoFimDeSemana,
        boolean exigirChefiaAoSabado,
        List<String> minimosPorTurno,
        List<String> cargasPorPerfil,
        int totalColaboradoresElegiveis,
        int totalAusenciasAprovadas,
        int totalFolgasPreferidas,
        int totalPreferenciasTurno,
        int totalPreferenciasColegas,
        int totalDiasEspeciais
) {
}
