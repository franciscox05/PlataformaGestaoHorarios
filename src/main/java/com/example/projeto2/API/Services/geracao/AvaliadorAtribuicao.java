package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioValidatorService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
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
 *   <li>Preferências de turno e de colegas aprovadas — {@code pesoPreferencias} + base fixa de desempate ({@link #BASE_PREFERENCIA_TURNO})</li>
 *   <li>Folga preferida no dia — base forte (soft) + reforço por {@code pesoPreferencias}</li>
 *   <li>Consistência de turno (mesmo período que na véspera; rotação invertida penalizada) — {@code pesoTurnoRepetido}</li>
 *   <li>Idle streak (dias sem trabalhar ≥ 2) — bónus proporcional, cap 5 dias, via {@code pesoEquilibrioCarga}</li>
 *   <li>Diversificação (desempate determinístico) — {@code pesoDiversificacao} + base</li>
 *   <li>Proteção da chefia para o sábado — margem semanal e reserva de carga (soft, não ponderado)</li>
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

    // Desempate independente da política para preferência de turno aprovada: garante atenção
    // mínima mesmo nas políticas com pesoPreferencias=1 (EQUILIBRIO, FINS_DE_SEMANA).
    // 40 pt cobre deltas de ritmo de até ~8% com peso máximo, mas não sobrepõe diferenças
    // reais de pace ou idle streak — comporta-se como um tie-breaker, não como restrição.
    private static final double BASE_PREFERENCIA_TURNO = 40.0;

    // Penalização de rotação invertida (ex.: noite→manhã, tarde→manhã em dias seguidos):
    // mesmo quando o descanso legal de 11h é cumprido, recuar no período do dia comprime
    // o descanso real e desorganiza o ritmo circadiano. Independente da política — é uma
    // questão de ergonomia básica, não de otimização. Soft: nunca impede cobertura.
    private static final double PENALIZACAO_ROTACAO_INVERTIDA = 45.0;

    // Proteção da chefia para o sábado: penalização forte (mas soft) para escalar uma
    // chefia em dia útil quando isso a deixaria sem margem semanal ou sem carga para os
    // sábados que o plano lhe designou. Sem isto, o greedy gastava os 5 dias úteis e a
    // carga das chefias, e o sábado ficava sem gerente/subgerente possível.
    private static final double PROTECAO_CHEFIA_SABADO = 1500.0;

    // Penalização forte (independente da política) para trabalhar fins de semana
    // consecutivos. Quando o motor RELAXA a rotação para cobrir um FDS sem candidatos
    // suficientes, esta penalização garante que quem já trabalhou o FDS anterior só é
    // escolhido em último recurso — os colegas que descansaram têm prioridade. Não se
    // aplica ao reforço de fim de semana (trabalhar FDS seguidos é o propósito dele).
    private static final double PENALIZACAO_FDS_CONSECUTIVO = 250.0;

    // Penalização proporcional para trabalhar dentro da janela de rotação de FDS
    // (não consecutivos, mas ainda dentro da janela configurada). Decresce linearmente
    // de PENALIZACAO_JANELA_FDS (2 semanas de intervalo) até 0 (janela exata).
    // Só é activa quando a rotação é relaxada (fase 3 do backtracking) — em condições
    // normais o hard-check podeReceber já bloqueia estes candidatos.
    private static final double PENALIZACAO_JANELA_FDS = 180.0;

    private final HorarioValidatorService validator;

    public AvaliadorAtribuicao(HorarioValidatorService validator) {
        this.validator = validator;
    }

    /**
     * Pré-computa, uma vez por resolução de candidatos, o conjunto de colaboradores
     * já escalados no histórico do mês e um índice por (dia, tipo-de-turno) — evitando
     * varrer {@code horariosJaGerados} por cada candidato (O(H) → O(1) por avaliação).
     *
     * <p>O índice {@code colabsPorDiaTipo} é usado pela verificação de colegas preferidos
     * (componente 5): só atribui o bónus quando o colega preferido está já confirmado
     * no <em>mesmo dia e mesmo tipo de turno</em> que estamos a avaliar — não basta ter
     * trabalhado em qualquer dia do mês.
     */
    public ContextoAvaliacao novoContexto(List<Horario> horariosJaGerados) {
        Map<LocalDate, Map<String, Set<Integer>>> porDiaTipo = new HashMap<>();
        for (Horario h : horariosJaGerados) {
            if (h.getIdLojautilizador() == null
                    || h.getIdLojautilizador().getIdUtilizador() == null
                    || h.getIdLojautilizador().getIdUtilizador().getId() == null) {
                continue;
            }
            Integer id = h.getIdLojautilizador().getIdUtilizador().getId();
            if (h.getDataTurno() != null && h.getIdTurno() != null) {
                String tipo = TurnoClassifier.tipoNormalizado(h.getIdTurno());
                porDiaTipo.computeIfAbsent(h.getDataTurno(), k -> new HashMap<>())
                          .computeIfAbsent(tipo, k -> new LinkedHashSet<>())
                          .add(id);
            }
        }
        return new ContextoAvaliacao(porDiaTipo);
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
            if (fimDeSemana) {
                // Guarda "fim de semana livre": se esta atribuição deixar o colaborador
                // sem nenhum FDS livre no período, penalização quasi-hard (3 500 pts).
                // Só activo quando o período tem > 1 FDS (se só há 1, alguém tem de o cobrir).
                // Conta fins de semana DISTINTOS — quem já trabalhou este FDS (o outro dia)
                // não consome um FDS livre adicional ao receber o segundo dia.
                int totalFdsNoPeriodo = contarSabadosNoPeriodo(pedido.dataInicio(), pedido.dataFim());
                int fdsDistintosApos = estado.fimsDeSemanaDistintosTrabalhados()
                        + (estado.fimDeSemanaJaTrabalhado(data) ? 0 : 1);
                if (totalFdsNoPeriodo > 1 && fdsDistintosApos >= totalFdsNoPeriodo) {
                    pontuacao += 3500.0;
                }

                int semanasSinceFds = estado.semanasDesdeUltimoFimDeSemana(data);
                if (semanasSinceFds == 1) {
                    // FDS consecutivos: penalização máxima independente da política.
                    componenteFds += 1.0;
                    pontuacao += PENALIZACAO_FDS_CONSECUTIVO;
                } else if (semanasSinceFds > 1) {
                    int janela = pedido.janelaRotacaoFimDeSemana();
                    if (janela > 1 && semanasSinceFds < janela) {
                        // Dentro da janela mas não consecutivos: penalização proporcional
                        // ao quão próximo está do limite (máximo em 2 semanas, 0 na janela).
                        double frac = (double)(janela - semanasSinceFds) / (janela - 1);
                        pontuacao += PENALIZACAO_JANELA_FDS * frac;
                    }
                }
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
        // A verificação de colega usa o índice (dia, tipo-de-turno) do contexto para garantir
        // que o bónus só dispara quando o colega preferido já está confirmado no MESMO slot
        // (mesmo dia e mesmo tipo de turno). Verificar apenas "trabalhou este mês" (abordagem
        // anterior) tornava o bónus quase constante a partir do 3.º dia, sem sinal real.
        double componentePref = 0;
        boolean temPrefTurno = temPreferenciaTurnoFavoravel(
                estado.idUtilizador(), turno, data, pedido.preferenciasTurnos());
        if (temPrefTurno) {
            componentePref -= 1.0;
        }
        Set<Integer> colegasPref = pedido.paresPreferisPorColaborador()
                .getOrDefault(estado.idUtilizador(), Set.of());
        if (!colegasPref.isEmpty()) {
            String tipoTurno = TurnoClassifier.tipoNormalizado(turno);
            Set<Integer> colabsJaNesseSlot = contexto.colabsPorDiaTipo()
                    .getOrDefault(data, Map.of())
                    .getOrDefault(tipoTurno, Set.of());
            if (!Collections.disjoint(colegasPref, colabsJaNesseSlot)) {
                componentePref -= 0.45;
            }
        }
        pontuacao += politica.pesoPreferencias() * componentePref * ESCALA_PREFERENCIAS;
        // Tie-break: base mínima garantida independentemente da política ativa. O valor é
        // pequeno o suficiente para nunca sobrepor diferenças reais de ritmo ou idle streak,
        // mas suficiente para desempatar quando os outros factores são tecnicamente iguais.
        if (temPrefTurno) {
            pontuacao -= BASE_PREFERENCIA_TURNO;
        }

        // (6) Folga preferida neste dia — soft, com muita atenção (base + reforço por política)
        Set<LocalDate> folgasPreferidas = pedido.folgasPreferidasPorColaborador()
                .getOrDefault(estado.idUtilizador(), Set.of());
        if (folgasPreferidas.contains(data)) {
            pontuacao += BASE_FOLGA_PREFERIDA
                    + politica.pesoPreferencias() * 1.2 * ESCALA_PREFERENCIAS;
        }

        // (7) Consistência de turno — recompensar repetir o período da véspera e
        // penalizar rotações invertidas (recuar de noite/tarde para manhã comprime o
        // descanso real mesmo quando as 11h legais são cumpridas).
        Turno turnoVespera = estado.turnoNaVespera(data);
        if (turnoVespera != null) {
            if (TurnoClassifier.tipoNormalizado(turnoVespera)
                    .equals(TurnoClassifier.tipoNormalizado(turno))) {
                pontuacao -= politica.pesoTurnoRepetido() * ESCALA_CONSISTENCIA;
            }
            int ordemVespera = TurnoClassifier.ordemPeriodo(turnoVespera);
            int ordemHoje = TurnoClassifier.ordemPeriodo(turno);
            if (ordemVespera >= 0 && ordemHoje >= 0 && ordemHoje < ordemVespera) {
                pontuacao += PENALIZACAO_ROTACAO_INVERTIDA * (ordemVespera - ordemHoje);
            }
        }

        // (7b) Lookahead de turno: se o colaborador já tem turno atribuído AMANHÃ
        // (fase 1 já correu para esse dia), verificar se o turno de hoje cria uma
        // rotação invertida futura (ex.: noite hoje → manhã amanhã). Peso reduzido
        // a 40% vs a verificação retrospetiva: a restrição é ergonómica, não hard,
        // e o futuro imediato é menos certo do que o passado verificado.
        Map<String, Set<Integer>> tiposAmanha = contexto.colabsPorDiaTipo()
                .getOrDefault(data.plusDays(1), Map.of());
        for (Map.Entry<String, Set<Integer>> e : tiposAmanha.entrySet()) {
            if (!e.getValue().contains(estado.idUtilizador())) continue;
            int ordemHoje = TurnoClassifier.ordemPeriodo(turno);
            int ordemAmanha = TurnoClassifier.ordemPorTipoNormalizado(e.getKey());
            if (ordemHoje >= 0 && ordemAmanha >= 0 && ordemAmanha < ordemHoje) {
                pontuacao += PENALIZACAO_ROTACAO_INVERTIDA * 0.4 * (ordemHoje - ordemAmanha);
            }
            break; // um turno por dia
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

        // (10) Proteção da chefia para o sábado — em dia útil, uma chefia com sábado(s)
        // designado(s) pelo plano não deve ser escalada quando isso a deixaria sem margem
        // semanal (precisa de guardar um dia para o sábado) ou sem carga contratual para
        // os sábados que ainda lhe cabem. Soft mas forte: com alternativas disponíveis,
        // a chefia nunca é a escolhida; sem alternativas, a cobertura mínima prevalece.
        if (!fimDeSemana && pedido.exigirChefiaAoSabado() && estado.ehChefiaAoSabado()
                && estado.temPlanoFinsDeSemana()) {
            LocalDate sabadoDaSemana = validator.inicioFimDeSemana(data);
            boolean chefiaDesteSabado = estado.ehChefiaDesignadaNoFimDeSemana(sabadoDaSemana)
                    && !sabadoDaSemana.isAfter(pedido.dataFim());
            int maxDiasSemana = 7 - pedido.descansoSemanalMinimoDias();
            if (chefiaDesteSabado && estado.diasTrabalhadosNaSemana(data) >= maxDiasSemana - 1) {
                pontuacao += PROTECAO_CHEFIA_SABADO;
            }
            int sabadosReservados = estado.sabadosComoChefiaDesde(data);
            if (sabadosReservados > 0
                    && estado.capacidadeRestanteMinutos() - minutos < (long) sabadosReservados * minutos) {
                pontuacao += PROTECAO_CHEFIA_SABADO;
            }
        }

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

    /** Conta os sábados entre {@code inicio} e {@code fim} (inclusive). */
    private static int contarSabadosNoPeriodo(LocalDate inicio, LocalDate fim) {
        int count = 0;
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY) count++;
        }
        return count;
    }

    /**
     * Contexto pré-computado, partilhado por todos os candidatos de uma resolução.
     *
     * @param colabsPorDiaTipo  índice dia → (tipo-turno → ids já confirmados nesse slot);
     *                          usado pela verificação de co-presença com colegas preferidos
     *                          e pelo lookahead de rotação de turno (componente 7b)
     */
    public record ContextoAvaliacao(
            Map<LocalDate, Map<String, Set<Integer>>> colabsPorDiaTipo) {
    }
}
