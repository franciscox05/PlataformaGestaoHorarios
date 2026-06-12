package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioValidatorService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Função de pontuação heurística de uma atribuição candidata (colaborador + turno
 * num dia). <b>Menor pontuação = melhor candidato.</b>
 *
 * <p>Ao contrário da versão anterior — em que os pesos estavam fixos no código e as
 * cinco políticas de otimização eram meramente cosméticas — esta avaliação multiplica
 * cada componente pelo peso definido na {@link PoliticaOtimizacao} ativa. Assim, a
 * política "Preferências" maximiza genuinamente preferências aprovadas, a "Carga
 * contratual" equilibra horas, a "Fins de semana" reforça a rotação, etc., produzindo
 * alternativas com estratégias realmente distintas que o serviço depois compara para
 * identificar a mais ótima.
 *
 * <p>O peso de uma preferência <em>é</em> a concordância do gerente: só preferências
 * <b>aprovadas</b> chegam aqui, e a política escolhida amplifica a atenção que lhes é dada.
 *
 * <p>Componentes (cada um normalizado e ponderado):
 * <ol>
 *   <li>Chefia obrigatória ao sábado — nudge constante (não ponderado), promove quem cumpre</li>
 *   <li>Equilíbrio de carga contratual — {@code pesoEquilibrioCarga}</li>
 *   <li>Rotação de fins de semana — {@code pesoFinsDeSemana}</li>
 *   <li>Reserva operacional de part-time de fim de semana — {@code pesoReservaOperacional}</li>
 *   <li>Preferências de turno e de colegas aprovadas — {@code pesoPreferencias}</li>
 *   <li>Folga preferida no dia — base forte (soft) + reforço por {@code pesoPreferencias}</li>
 *   <li>Consistência de turno (mesmo período que na véspera) — {@code pesoTurnoRepetido}</li>
 *   <li>Idle streak (dias sem trabalhar ≥ 2) — bónus proporcional, cap 5 dias, via {@code pesoEquilibrioCarga}</li>
 *   <li>Diversificação (desempate determinístico) — {@code pesoDiversificacao} + base</li>
 * </ol>
 */
public final class AvaliadorAtribuicao {

    private static final long ENTROPIA = 0x9e3779b97f4a7c15L;

    // Escalas-base de cada componente (multiplicadas pelo peso da política)
    private static final double ESCALA_CARGA        = 100.0;
    private static final double ESCALA_FDS          = 55.0;
    private static final double ESCALA_RESERVA      = 50.0;
    private static final double ESCALA_PREFERENCIAS = 55.0;
    private static final double ESCALA_CONSISTENCIA = 22.0;
    private static final double ESCALA_IDLE         = 18.0;
    private static final double ESCALA_JITTER       = 8.0;

    // Penalização-base de folga preferida — independente da política (atenção garantida)
    private static final double BASE_FOLGA_PREFERIDA = 60.0;

    // Reduzido de 1000 → 500 para que o pace guard (componente 2b) e o equilíbrio
    // de carga consigam sobrepor-se quando a chefia já está acima do ritmo esperado.
    private static final double NUDGE_CHEFIA_SABADO = 500.0;

    // Nudge moderado para a chefia que NÃO é a designada deste fim de semana (lookahead
    // ativo): mantém-na preferível a um regular (a cobertura de chefia é garantida pela
    // restrição hard), mas sem a queimar — preserva-a para o fim de semana que lhe cabe.
    private static final double NUDGE_CHEFIA_SABADO_SECUNDARIA = 150.0;

    private final HorarioValidatorService validator;

    public AvaliadorAtribuicao(HorarioValidatorService validator) {
        this.validator = validator;
    }

    /**
     * Pré-computa, uma vez por resolução de candidatos, o conjunto de colaboradores
     * já escalados no histórico do mês — evitando varrer {@code horariosJaGerados} por
     * cada candidato (antes era O(H) por candidato; passa a O(1)).
     */
    public ContextoAvaliacao novoContexto(List<Horario> horariosJaGerados) {
        Set<Integer> noMes = new LinkedHashSet<>();
        for (Horario h : horariosJaGerados) {
            if (h.getIdLojautilizador() != null
                    && h.getIdLojautilizador().getIdUtilizador() != null
                    && h.getIdLojautilizador().getIdUtilizador().getId() != null) {
                noMes.add(h.getIdLojautilizador().getIdUtilizador().getId());
            }
        }
        return new ContextoAvaliacao(noMes);
    }

    /** Pontua uma atribuição candidata. Menor = melhor. */
    public double pontuar(EstadoColaborador estado,
                          Turno turno,
                          long minutos,
                          LocalDate data,
                          PedidoGeracao pedido,
                          ContextoAvaliacao contexto) {

        PoliticaOtimizacao politica = pedido.politica();
        double pontuacao = 0;
        boolean fimDeSemana = validator.ehFimDeSemana(data);

        // Ritmo contratual esperado — calculado uma vez e reutilizado nos componentes (2b) e (8)
        long diasTotais = ChronoUnit.DAYS.between(pedido.dataInicio(), pedido.dataFim()) + 1;
        long diasDecorridos = ChronoUnit.DAYS.between(pedido.dataInicio(), data) + 1;
        double excessoRitmo = estado.excessoRitmoNormalizado(diasDecorridos, diasTotais);

        // (1) Chefia obrigatória ao sábado — nudge plano-aware. Sem lookahead ativo, puxa
        // todas as chefias (comportamento anterior). Com lookahead, puxa forte a chefia
        // designada deste FDS e moderadamente as outras, sustentando a rotação de chefias.
        if (pedido.exigirChefiaAoSabado()
                && data.getDayOfWeek() == DayOfWeek.SATURDAY
                && estado.ehChefiaAoSabado()) {
            boolean designadaOuSemPlano = !estado.temPlanoFinsDeSemana()
                    || estado.ehChefiaDesignadaNoFimDeSemana(data);
            pontuacao -= designadaOuSemPlano ? NUDGE_CHEFIA_SABADO : NUDGE_CHEFIA_SABADO_SECUNDARIA;
        }

        // (2) Equilíbrio de carga contratual — preferir quem tem menos utilização projetada
        pontuacao += politica.pesoEquilibrioCarga() * estado.utilizacaoProjetada(minutos) * ESCALA_CARGA;

        // (2b) Pace guard — penalização extra para colaboradores acima do ritmo esperado.
        // Evita que preferências de turno ou o bónus de inatividade (componente 8) sejam
        // suficientes para escalar repetidamente colaboradores que já estão adiantados no
        // seu contrato, deixando-os sem capacidade antes do fim do mês.
        if (excessoRitmo > 0.05) {
            pontuacao += politica.pesoEquilibrioCarga() * excessoRitmo * ESCALA_CARGA * 5.0;
        }

        // (3) Rotação de fins de semana — penalizar quem já trabalhou mais / consecutivos.
        // O reforço de fim de semana está isento: trabalhar FDS seguidos é o propósito
        // do perfil, pelo que penalizá-lo só empurrava os FDS para a equipa regular.
        if (!estado.ehApenasFimDeSemana()) {
            double componenteFds = Math.min(estado.totalFimDeSemanaTrabalhados(), 5) / 5.0;
            if (fimDeSemana && estado.trabalhouFimDeSemanaAnterior(data)) {
                componenteFds += 1.0;
            }
            pontuacao += politica.pesoFinsDeSemana() * componenteFds * ESCALA_FDS;

            // Lookahead de FDS: puxar o colaborador para o fim de semana que lhe foi
            // designado no plano global (rotação planeada, não reativa). Apenas bónus —
            // nunca penaliza os não-designados, para não retirar candidatos a um fim de
            // semana que ainda possa precisar deles.
            if (fimDeSemana && estado.designadoParaFimDeSemana(data)) {
                pontuacao -= politica.pesoFinsDeSemana() * ESCALA_FDS;
            }
        }

        // (4) Reserva operacional — poupar part-time de fim de semana para o fim de semana
        if (fimDeSemana && estado.ehApenasFimDeSemana()) {
            pontuacao -= politica.pesoReservaOperacional() * 0.5 * ESCALA_RESERVA;
        }

        // (5) Preferências aprovadas (turno + colegas)
        double componentePref = 0;
        if (temPreferenciaTurnoFavoravel(estado.idUtilizador(), turno, data, pedido.preferenciasTurnos())) {
            componentePref -= 1.0;
        }
        Set<Integer> colegasPref = pedido.paresPreferisPorColaborador()
                .getOrDefault(estado.idUtilizador(), Set.of());
        if (!colegasPref.isEmpty() && !Collections.disjoint(colegasPref, contexto.colaboradoresNoMes())) {
            componentePref -= 0.45;
        }
        pontuacao += politica.pesoPreferencias() * componentePref * ESCALA_PREFERENCIAS;

        // (6) Folga preferida neste dia — soft, com muita atenção (base + reforço por política)
        Set<LocalDate> folgasPreferidas = pedido.folgasPreferidasPorColaborador()
                .getOrDefault(estado.idUtilizador(), Set.of());
        if (folgasPreferidas.contains(data)) {
            pontuacao += BASE_FOLGA_PREFERIDA
                    + politica.pesoPreferencias() * 1.2 * ESCALA_PREFERENCIAS;
        }

        // (7) Consistência de turno — recompensar repetir o período da véspera
        Turno turnoVespera = estado.turnoNaVespera(data);
        if (turnoVespera != null
                && TurnoClassifier.tipoNormalizado(turnoVespera)
                        .equals(TurnoClassifier.tipoNormalizado(turno))) {
            pontuacao -= politica.pesoTurnoRepetido() * ESCALA_CONSISTENCIA;
        }

        // (8) Idle streak — bónus para colaboradores que não trabalham há 2+ dias.
        // Só se aplica quando o colaborador NÃO está acima do ritmo esperado: se já está
        // adiantado no contrato, não precisa de incentivo extra — o pace guard (2b) trata
        // do reequilíbrio. Isto evita que o bónus de inatividade anule o pace guard e
        // volte a tornar colaboradores sobrecarregados preferidos após um dia de descanso.
        int diasIdle = estado.diasDesdeUltimoTurno(data);
        if (diasIdle >= 2 && excessoRitmo <= 0.05) {
            int diasIdleCapped = Math.min(diasIdle, 5);
            pontuacao -= politica.pesoEquilibrioCarga() * (diasIdleCapped - 1) * ESCALA_IDLE;
        }

        // (9) Diversificação — jitter determinístico de desempate (base garantida + reforço)
        double jitter = jitter(pedido.semente(), estado.idUtilizador());
        pontuacao += jitter * (1 + politica.pesoDiversificacao());

        return pontuacao;
    }

    /** Jitter determinístico em [0, ESCALA_JITTER), pequeno o suficiente para nunca anular preferências fortes. */
    private double jitter(long semente, Integer idColaborador) {
        long id = idColaborador != null ? idColaborador : 0;
        long mistura = (semente ^ (id * ENTROPIA));
        return (mistura >>> 50) / 16383.0 * ESCALA_JITTER;
    }

    /**
     * Indica se algum turno preferido aprovado do colaborador corresponde a este turno
     * nesta data (filtros de período e/ou duração presentes na descrição). Público para
     * permitir verificação direta em testes, sem reflexão sobre internos.
     */
    public boolean temPreferenciaTurnoFavoravel(Integer idColaborador,
                                                Turno turno,
                                                LocalDate data,
                                                Map<Integer, List<Preferencia>> preferenciasTurnos) {
        List<Preferencia> prefs = preferenciasTurnos.getOrDefault(idColaborador, List.of());
        if (prefs.isEmpty()) return false;
        long duracaoMinutos = validator.calcularDuracaoEmMinutos(turno);

        for (Preferencia p : prefs) {
            boolean dentroDoIntervalo = p.getDataInicio() != null
                    && !data.isBefore(p.getDataInicio())
                    && (p.getDataFim() == null || !data.isAfter(p.getDataFim()));
            if (!dentroDoIntervalo) continue;

            String desc = HorarioFormatters.normalizarTexto(p.getDescricao());
            boolean exigeCurto = desc.contains("curto");
            boolean exigeLongo = desc.contains("longo");
            boolean exigeManha = desc.contains("manha");
            boolean exigeTarde = desc.contains("tarde");
            boolean exigeIntermedio = desc.contains("intermedio");
            boolean exigeNoite = desc.contains("noite");

            boolean temFiltroDuracao = exigeCurto || exigeLongo;
            boolean temFiltroPeriodo = exigeManha || exigeTarde || exigeIntermedio || exigeNoite;

            boolean correspondeDuracao = !temFiltroDuracao
                    || (exigeCurto && duracaoMinutos < 300)
                    || (exigeLongo && duracaoMinutos >= 300);
            boolean correspondePeriodo = !temFiltroPeriodo
                    || TurnoClassifier.correspondePeriodo(turno, exigeManha, exigeTarde, exigeIntermedio, exigeNoite);

            if (correspondeDuracao && correspondePeriodo) {
                return true;
            }
        }
        return false;
    }

    /** Contexto pré-computado, partilhado por todos os candidatos de uma resolução. */
    public record ContextoAvaliacao(Set<Integer> colaboradoresNoMes) {
    }
}
