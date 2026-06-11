package com.example.projeto2.API.Services.geracao;

/**
 * Uma sugestão acionável para o gestor resolver uma falha de cobertura: o código
 * da causa, o texto da recomendação e, quando aplicável, o perfil contratual a
 * reforçar.
 */
public record SugestaoFalhaGeracao(
        String codigo,
        String texto,
        String perfilRecomendado
) {
}
