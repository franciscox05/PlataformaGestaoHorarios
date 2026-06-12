package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioValidatorService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fase de <b>lookahead de fins de semana</b>, executada <em>antes</em> do greedy diário
 * do {@link HorarioGeneratorEngine}.
 *
 * <p>O greedy processa os dias em sequência, pelo que é míope quanto aos fins de semana:
 * pode escalar as duas chefias no primeiro fim de semana — ficando ambas bloqueadas pela
 * rotação no fim de semana seguinte, o que força a relaxação da rotação (violando-a) para
 * cobrir a chefia. De forma geral, não pré-distribui quem trabalha cada fim de semana.
 *
 * <p>Este planeador computa uma designação <b>global</b>: quem está previsto para cada
 * fim de semana do mês, respeitando a janela de rotação. A designação é <em>consultiva</em>
 * — o {@link AvaliadorAtribuicao} usa-a apenas como bónus de pontuação (puxa o colaborador
 * para o seu fim de semana planeado) e para distinguir a chefia designada das restantes.
 * Como não cria restrições duras, o plano <b>nunca</b> torna a geração inviável: na pior
 * das hipóteses é ignorado e o motor recupera o comportamento reativo anterior.
 *
 * <p>Estratégia de designação, por ordem:
 * <ol>
 *   <li><b>Reforço de fim de semana</b> (perfil só-FDS, isento de rotação): designado para
 *       todos os fins de semana — é o propósito do contrato.</li>
 *   <li><b>Chefia</b>: uma por fim de semana, round-robin, respeitando a rotação e os
 *       bloqueios duros ao sábado. Sustenta a rotação de chefias (evita queimar duas no
 *       mesmo fim de semana).</li>
 *   <li><b>Regulares</b>: preenchem cada fim de semana até {@code cobertura mínima + margem},
 *       round-robin com espaçamento de rotação, equilibrando o total de fins de semana.</li>
 * </ol>
 */
public final class PlaneadorFinsDeSemana {

    /** Workers regulares a designar acima da cobertura mínima, por fim de semana. */
    private static final int MARGEM_REGULARES = 1;

    private final HorarioValidatorService validator;

    public PlaneadorFinsDeSemana(HorarioValidatorService validator) {
        this.validator = validator;
    }

    public PlanoFinsDeSemana planear(PedidoGeracao pedido) {
        List<LocalDate> fins = enumerarFinsDeSemana(pedido);
        if (fins.isEmpty()) {
            return PlanoFinsDeSemana.vazio();
        }

        int janela = Math.max(1, pedido.janelaRotacaoFimDeSemana());
        int cobertura = coberturaMinima(pedido);

        List<Lojautilizador> reforcos = new ArrayList<>();
        List<Lojautilizador> chefias = new ArrayList<>();
        List<Lojautilizador> regulares = new ArrayList<>();
        classificar(pedido, reforcos, chefias, regulares);

        Map<Integer, Set<LocalDate>> designados = new HashMap<>();
        Map<Integer, Set<LocalDate>> comoChefia = new HashMap<>();
        Map<Integer, LocalDate> ultimoDesignado = new HashMap<>();

        // (1) Reforço — todos os fins de semana (isento de rotação).
        for (Lojautilizador reforco : reforcos) {
            Integer id = idDe(reforco);
            if (id == null || pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L) <= 0) {
                continue;
            }
            for (LocalDate fds : fins) {
                designados.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(fds);
            }
        }

        // (2) Chefia — uma por fim de semana, respeitando rotação e bloqueio ao sábado.
        // Fallback: quando a janela de rotação não deixa nenhuma chefia elegível (ex.:
        // 2 chefias com rotação de 7 semanas num mês de 5 fins de semana), designa a
        // menos sobrecarregada MESMO violando o espaçamento. O motor vai precisar de
        // uma chefia nesse sábado de qualquer forma (via relaxação da rotação) — e só
        // com a designação feita é que o avaliador a protege durante a semana (margem
        // de dias e carga) para ela chegar ao sábado disponível.
        Map<Integer, LocalDate> ultimaChefia = new HashMap<>();
        for (LocalDate fds : fins) {
            Lojautilizador escolhido = escolher(chefias, fds, janela, ultimaChefia,
                    pedido, designados, true);
            if (escolhido == null) {
                escolhido = escolher(chefias, fds, 1, ultimaChefia, pedido, designados, true);
            }
            if (escolhido != null) {
                Integer id = idDe(escolhido);
                designados.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(fds);
                comoChefia.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(fds);
                ultimaChefia.put(id, fds);
                ultimoDesignado.put(id, fds);
            }
            // Sem chefia mesmo com fallback (ex.: folga aprovada no sábado): fica sem
            // designação — o nudge de fallback do avaliador e a relaxação tratam disso.
        }

        // (3) Regulares — preencher até cobertura + margem, com espaçamento de rotação.
        int alvo = cobertura + MARGEM_REGULARES;
        for (LocalDate fds : fins) {
            while (contarDesignados(designados, fds) < alvo) {
                Lojautilizador escolhido = escolher(regulares, fds, janela, ultimoDesignado,
                        pedido, designados, false);
                if (escolhido == null) {
                    break; // sem mais regulares elegíveis para este fim de semana
                }
                Integer id = idDe(escolhido);
                designados.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(fds);
                ultimoDesignado.put(id, fds);
            }
        }

        return new PlanoFinsDeSemana(designados, comoChefia, true);
    }

    /**
     * Escolhe, do {@code pool}, o colaborador com menos fins de semana já designados que
     * esteja elegível para {@code fds}: ainda não designado nesse FDS, com espaçamento de
     * rotação respeitado e sem bloqueio duro. {@code exigeSabadoLivre} aplica-se às chefias
     * (têm de cobrir o sábado); para regulares basta um dos dias do FDS estar livre.
     */
    private Lojautilizador escolher(List<Lojautilizador> pool,
                                    LocalDate fds,
                                    int janela,
                                    Map<Integer, LocalDate> ultimoDesignado,
                                    PedidoGeracao pedido,
                                    Map<Integer, Set<LocalDate>> designados,
                                    boolean exigeSabadoLivre) {
        Lojautilizador melhor = null;
        int melhorContagem = Integer.MAX_VALUE;

        for (Lojautilizador ligacao : pool) {
            Integer id = idDe(ligacao);
            if (id == null || pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L) <= 0) {
                continue;
            }
            Set<LocalDate> jaDesignados = designados.getOrDefault(id, Set.of());
            if (jaDesignados.contains(fds)) {
                continue;
            }
            LocalDate ultimo = ultimoDesignado.get(id);
            if (ultimo != null && fds.isBefore(ultimo.plusWeeks(janela))) {
                continue; // viola o espaçamento de rotação
            }
            if (indisponivel(pedido, id, fds, exigeSabadoLivre)) {
                continue;
            }
            int contagem = jaDesignados.size();
            if (contagem < melhorContagem) {
                melhorContagem = contagem;
                melhor = ligacao;
            }
        }
        return melhor;
    }

    private boolean indisponivel(PedidoGeracao pedido, Integer id, LocalDate sabado, boolean exigeSabadoLivre) {
        Set<LocalDate> bloqueios = pedido.bloqueiosPorColaborador().getOrDefault(id, Set.of());
        if (exigeSabadoLivre) {
            return bloqueios.contains(sabado);
        }
        return bloqueios.contains(sabado) && bloqueios.contains(sabado.plusDays(1));
    }

    private int contarDesignados(Map<Integer, Set<LocalDate>> designados, LocalDate fds) {
        int total = 0;
        for (Set<LocalDate> dias : designados.values()) {
            if (dias.contains(fds)) {
                total++;
            }
        }
        return total;
    }

    private void classificar(PedidoGeracao pedido,
                             List<Lojautilizador> reforcos,
                             List<Lojautilizador> chefias,
                             List<Lojautilizador> regulares) {
        Set<Integer> chefiasIds = pedido.chefiasSabadoIds();
        for (Lojautilizador ligacao : pedido.colaboradores()) {
            Integer id = idDe(ligacao);
            if (id == null) {
                continue;
            }
            if ("reforco_parttime".equals(tipoCargo(ligacao))) {
                reforcos.add(ligacao);
            } else if (chefiasIds.contains(id)) {
                chefias.add(ligacao);
            } else {
                regulares.add(ligacao);
            }
        }
        // Ordem determinística por id para reprodutibilidade do plano.
        Comparator<Lojautilizador> porId = Comparator.comparing(
                PlaneadorFinsDeSemana::idDe, Comparator.nullsLast(Comparator.naturalOrder()));
        reforcos.sort(porId);
        chefias.sort(porId);
        regulares.sort(porId);
    }

    /** Sábados distintos (chave do FDS) que têm pelo menos um dia dentro do período. */
    private List<LocalDate> enumerarFinsDeSemana(PedidoGeracao pedido) {
        Set<LocalDate> sabados = new TreeSet<>();
        for (LocalDate data = pedido.dataInicio();
             !data.isAfter(pedido.dataFim());
             data = data.plusDays(1)) {
            if (validator.ehFimDeSemana(data)) {
                sabados.add(validator.inicioFimDeSemana(data));
            }
        }
        return new ArrayList<>(sabados);
    }

    /**
     * Cobertura mínima de um dia de fim de semana: soma, por tipo de turno distinto, do
     * mínimo de colaboradores exigido (espelha a construção de slots do motor).
     */
    private int coberturaMinima(PedidoGeracao pedido) {
        Map<String, Integer> minimoPorTipo = new LinkedHashMap<>();
        for (Turno turno : pedido.turnos()) {
            String tipo = TurnoClassifier.tipoNormalizado(turno);
            int minimo = pedido.minimosPorTurno().getOrDefault(turno.getId(), 0);
            minimoPorTipo.merge(tipo, minimo, Math::max);
        }
        return minimoPorTipo.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static String tipoCargo(Lojautilizador ligacao) {
        if (ligacao.getIdCargo() == null || ligacao.getIdCargo().getTipo() == null) {
            return "";
        }
        return ligacao.getIdCargo().getTipo().trim().toLowerCase(Locale.ROOT);
    }

    private static Integer idDe(Lojautilizador ligacao) {
        return ligacao != null && ligacao.getIdUtilizador() != null
                ? ligacao.getIdUtilizador().getId()
                : null;
    }
}
