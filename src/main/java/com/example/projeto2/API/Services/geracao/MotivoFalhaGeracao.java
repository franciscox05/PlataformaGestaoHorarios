package com.example.projeto2.API.Services.geracao;

import java.util.List;

/**
 * Um motivo agregado pelo qual a geração não conseguiu cobrir um turno: o código
 * da causa, quantos colaboradores foram afetados, uma descrição legível e os nomes
 * envolvidos. Usado pelo painel de diagnóstico para explicar a falha ao gestor.
 */
public record MotivoFalhaGeracao(
        String codigo,
        int total,
        String descricao,
        List<String> nomes
) {
}
