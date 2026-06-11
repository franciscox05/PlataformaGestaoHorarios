package com.example.projeto2.API.Services.geracao.dto;

/** Contexto inicial do ecrã de geração: loja, período corrente e permissões do utilizador. */
public record GeracaoContexto(
        Integer idLoja,
        String nomeLoja,
        String localizacao,
        Integer anoAtual,
        Integer mesAtual,
        boolean podeGerar,
        boolean podeValidar,
        boolean existePropostaAtual
) {
}
