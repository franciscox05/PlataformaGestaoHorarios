package com.example.projeto2.API.Services.geracao;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Resultado do {@link PlaneadorFinsDeSemana}: a designação global de quem trabalha cada
 * fim de semana do mês, computada <em>antes</em> do greedy diário.
 *
 * <p>É um artefacto puramente consultivo — usado pelo {@link AvaliadorAtribuicao} como
 * <em>steering</em> de pontuação (rotação planeada em vez de reativa). Não cria nenhuma
 * restrição dura, pelo que nunca pode tornar a geração inviável; na pior das hipóteses
 * o plano é ignorado e o motor cai no comportamento reativo anterior.
 *
 * @param finsDeSemanaPorColaborador id-colaborador → sábados (chave do FDS) que lhe foram designados
 * @param finsDeSemanaComoChefia     id-colaborador → sábados em que é a chefia designada do FDS
 * @param ativo                      {@code false} quando o período não contém fins de semana
 */
public record PlanoFinsDeSemana(
        Map<Integer, Set<LocalDate>> finsDeSemanaPorColaborador,
        Map<Integer, Set<LocalDate>> finsDeSemanaComoChefia,
        boolean ativo
) {

    public static PlanoFinsDeSemana vazio() {
        return new PlanoFinsDeSemana(Map.of(), Map.of(), false);
    }

    public Set<LocalDate> designados(Integer idColaborador) {
        return finsDeSemanaPorColaborador.getOrDefault(idColaborador, Set.of());
    }

    public Set<LocalDate> chefiaEm(Integer idColaborador) {
        return finsDeSemanaComoChefia.getOrDefault(idColaborador, Set.of());
    }
}
