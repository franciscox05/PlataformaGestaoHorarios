package com.example.projeto2.API.Services.geracao.dto;

import com.example.projeto2.API.Services.geracao.MetricasPlaneamento;

import java.util.List;

/** Resultado completo de uma proposta (ou planeamento publicado): metadados, linhas, resumos e métricas. */
public record PropostaResultado(
        Integer idProposta,
        String nomeLoja,
        Integer ano,
        Integer mes,
        String nomeMes,
        String estado,
        String origemPlaneamento,
        String resumoGeracao,
        String geradoPor,
        String dataGeracao,
        String decididoPor,
        String dataDecisao,
        String observacoesSupervisor,
        boolean podeSerDecidida,
        List<HorarioLinha> linhas,
        List<ResumoColaborador> resumoColaboradores,
        MetricasPlaneamento metricas,
        ResumoGeral resumo
) {
}
