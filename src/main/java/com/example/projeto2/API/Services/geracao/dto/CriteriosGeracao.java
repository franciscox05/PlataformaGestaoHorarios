package com.example.projeto2.API.Services.geracao.dto;

import java.util.List;

/**
 * Resumo legível de tudo o que o motor de geração tem em conta para um período:
 * regras hard ativas, cargas contratuais, mínimos por turno, balanço de capacidade
 * e o detalhe das preferências/ausências aprovadas que vão influenciar o resultado.
 *
 * <p>Mostrado ao gestor antes de gerar, para que perceba e confie no processo —
 * e saiba onde mexer quando a geração falha ou o resultado não é o esperado.
 *
 * @param descansoMinimoHoras        RFS06 — horas mínimas entre turnos consecutivos
 * @param maxDiasConsecutivos        RFS07 — teto de dias seguidos de trabalho
 * @param descansoSemanalMinimoDias  RFS04 — dias de folga por semana
 * @param janelaRotacaoFimDeSemana   RFS08 — semanas mínimas entre fins de semana trabalhados
 * @param exigirChefiaAoSabado       RFS03 — gerente/subgerente obrigatório ao sábado
 * @param minimosPorTurno            por tipo de turno (ex.: "Manhã (08:00–16:00) — mínimo 2")
 * @param cargasPorPerfil            por perfil (ex.: "Full-time — 176h/mês")
 * @param capacidadeEquipaHoras      soma das cargas contratuais da equipa elegível
 * @param necessidadeMinimaHoras     horas exigidas pela cobertura mínima de todos os dias
 * @param detalheColaboradores       um por colaborador: nome — cargo, carga mensal
 * @param detalheAusencias           uma por ausência aprovada: nome — data (tipo)
 * @param detalheFolgasPreferidas    uma por folga preferida: nome — data
 * @param detalhePreferenciasTurno   uma por preferência: nome — descrição (período)
 * @param detalhePreferenciasColegas uma por preferência: nome — descrição
 * @param detalheDiasEspeciais       um por dia: data — encerrado/horário especial
 */
public record CriteriosGeracao(
        int descansoMinimoHoras,
        int maxDiasConsecutivos,
        int descansoSemanalMinimoDias,
        int janelaRotacaoFimDeSemana,
        boolean exigirChefiaAoSabado,
        List<String> minimosPorTurno,
        List<String> cargasPorPerfil,
        long capacidadeEquipaHoras,
        long necessidadeMinimaHoras,
        List<String> detalheColaboradores,
        List<String> detalheAusencias,
        List<String> detalheFolgasPreferidas,
        List<String> detalhePreferenciasTurno,
        List<String> detalhePreferenciasColegas,
        List<String> detalheDiasEspeciais
) {

    /** A capacidade contratual da equipa chega para a cobertura mínima do período. */
    public boolean capacidadeSuficiente() {
        return capacidadeEquipaHoras >= necessidadeMinimaHoras;
    }
}
