package com.example.projeto2.DESKTOP.support;

import java.util.List;

/** Resultado da validação de uma proposta de horário face às regras configuradas. */
public record ValidacaoHorarioResultado(
        Estado estadoGeral,
        List<CategoriaValidacao> categorias
) {

    public enum Estado { OK, VIOLACAO }

    public record CategoriaValidacao(
            String nome,
            Estado estado,
            String resumo,
            List<String> violacoes
    ) {
        public boolean semViolacoes() { return violacoes == null || violacoes.isEmpty(); }
    }

    public static ValidacaoHorarioResultado vazio() {
        return new ValidacaoHorarioResultado(Estado.OK, List.of());
    }
}
