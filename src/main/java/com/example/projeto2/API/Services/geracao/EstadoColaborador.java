package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioValidatorService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Estado mutável de um colaborador ao longo da geração de um mês.
 *
 * <p>Encapsula a contabilidade necessária para decidir elegibilidade
 * (carga acumulada, dias consecutivos, fins de semana trabalhados, atribuições
 * conhecidas) e delega a aplicação de cada regra hard ao {@link HorarioValidatorService}.
 *
 * <p>Extraído do motor de geração como objeto de domínio standalone (Fase 2 do
 * refactor SRP), para que o motor fique focado na pesquisa e a pontuação possa
 * ser avaliada por um colaborador separado.
 */
public final class EstadoColaborador {

    private final Lojautilizador ligacao;
    private final long cargaMaximaMinutos;
    private final boolean chefiaAoSabado;
    private final boolean apenasFimDeSemana;
    private final boolean exigeTurnoMinimoOitoHoras;
    private final HorarioValidatorService validator;

    private LocalDate ultimaDataAtribuida;
    private int diasConsecutivos;
    private long minutosAtribuidos;
    private Set<LocalDate> folgasReservadas = Set.of();
    private boolean planoFinsDeSemanaAtivo;
    private Set<LocalDate> finsDeSemanaDesignados = Set.of();
    private Set<LocalDate> finsDeSemanaComoChefia = Set.of();
    private final Map<LocalDate, Turno> atribuicoesConhecidas = new HashMap<>();
    private final Map<LocalDate, Integer> diasTrabalhadosPorSemana = new HashMap<>();
    private final Map<LocalDate, LocalDate> ultimoFimDeSemana = new HashMap<>();
    private int totalFimDeSemanaTrabalhados;

    public EstadoColaborador(Lojautilizador ligacao,
                             long cargaMaximaMinutos,
                             Set<Integer> chefiasSabadoIds,
                             HorarioValidatorService validator) {
        this.ligacao = ligacao;
        this.cargaMaximaMinutos = cargaMaximaMinutos;
        this.validator = validator;
        this.chefiaAoSabado = ligacao.getIdUtilizador() != null
                && chefiasSabadoIds.contains(ligacao.getIdUtilizador().getId());
        String tipoCargo = ligacao.getIdCargo() != null ? normalizarCargo(ligacao.getIdCargo().getTipo()) : "";
        this.apenasFimDeSemana = "reforco_parttime".equals(tipoCargo);
        this.exigeTurnoMinimoOitoHoras = "fulltime".equals(tipoCargo)
                || "gerente".equals(tipoCargo)
                || "subgerente".equals(tipoCargo)
                || "supervisor".equals(tipoCargo);
    }

    public Integer idUtilizador() {
        return ligacao.getIdUtilizador() != null ? ligacao.getIdUtilizador().getId() : null;
    }

    public Lojautilizador ligacao() { return ligacao; }

    public boolean ehChefiaAoSabado() { return chefiaAoSabado; }

    public boolean ehApenasFimDeSemana() { return apenasFimDeSemana; }

    public double utilizacaoProjetada(long minutosTurno) {
        if (cargaMaximaMinutos <= 0) return Double.MAX_VALUE;
        return (minutosAtribuidos + minutosTurno) / (double) cargaMaximaMinutos;
    }

    /** Minutos contratuais ainda por consumir neste mês. */
    public long capacidadeRestanteMinutos() {
        return Math.max(0, cargaMaximaMinutos - minutosAtribuidos);
    }

    /**
     * Fixa os dias de folga preferida pré-reservados pelo {@link PlaneadorFolgasPreferidas}:
     * passam a ser tratados como restrição dura (o colaborador nunca é escalado nesses dias),
     * garantindo que a folga é honrada. Definido uma única vez, logo após a construção e
     * antes de qualquer atribuição. As folgas <em>não</em> reservadas mantêm-se soft.
     */
    public void reservarFolgasPreferidas(Set<LocalDate> dias) {
        this.folgasReservadas = (dias != null && !dias.isEmpty()) ? Set.copyOf(dias) : Set.of();
    }

    /**
     * Fixa a designação de fins de semana calculada pelo {@link PlaneadorFinsDeSemana}:
     * os sábados (chave do FDS) em que este colaborador está previsto para trabalhar e
     * aqueles em que é a chefia designada. Definido uma única vez, após a construção.
     *
     * <p>É <em>consultivo</em> (usado só pelo {@link AvaliadorAtribuicao} como bónus de
     * pontuação) — nunca bloqueia o colaborador, ao contrário das folgas reservadas. Marca
     * o plano como ativo mesmo com conjuntos vazios, para o avaliador saber distinguir
     * "não designado" de "sem plano" e cair no comportamento reativo quando não há plano.
     */
    public void designarFinsDeSemana(Set<LocalDate> designados, Set<LocalDate> comoChefia) {
        this.planoFinsDeSemanaAtivo = true;
        this.finsDeSemanaDesignados = (designados != null && !designados.isEmpty())
                ? Set.copyOf(designados) : Set.of();
        this.finsDeSemanaComoChefia = (comoChefia != null && !comoChefia.isEmpty())
                ? Set.copyOf(comoChefia) : Set.of();
    }

    /** Há um plano de fins de semana ativo (computado pelo lookahead) para este colaborador. */
    public boolean temPlanoFinsDeSemana() {
        return planoFinsDeSemanaAtivo;
    }

    /** O colaborador está designado para trabalhar o fim de semana a que {@code data} pertence. */
    public boolean designadoParaFimDeSemana(LocalDate data) {
        return finsDeSemanaDesignados.contains(validator.inicioFimDeSemana(data));
    }

    /** O colaborador é a chefia designada do fim de semana a que {@code data} pertence. */
    public boolean ehChefiaDesignadaNoFimDeSemana(LocalDate data) {
        return finsDeSemanaComoChefia.contains(validator.inicioFimDeSemana(data));
    }

    public int totalFimDeSemanaTrabalhados() { return totalFimDeSemanaTrabalhados; }

    /**
     * Indica se o colaborador está atrasado face ao ritmo contratual: ao fim de
     * {@code diasDecorridos} de {@code diasTotais}, espera-se que tenha
     * aproximadamente {@code carga × diasDecorridos / diasTotais} minutos atribuídos.
     * É o critério da fase de preenchimento de capacidade — garante que a carga
     * contratual é consumida de forma uniforme ao longo do mês, nunca acima do teto.
     */
    public boolean abaixoDoRitmoContratual(long diasDecorridos, long diasTotais) {
        if (cargaMaximaMinutos <= 0 || diasTotais <= 0) {
            return false;
        }
        return minutosAtribuidos * diasTotais < cargaMaximaMinutos * diasDecorridos;
    }

    /**
     * Excesso de utilização face ao ritmo esperado para o dia {@code diasDecorridos}
     * de {@code diasTotais}. Positivo = acima do ritmo contratual; negativo = abaixo.
     * Usado pelo avaliador para penalizar colaboradores que estão a acumular turnos
     * a um ritmo superior ao contratualmente esperado.
     */
    public double excessoRitmoNormalizado(long diasDecorridos, long diasTotais) {
        if (cargaMaximaMinutos <= 0 || diasTotais <= 0) return 0.0;
        double utilizacaoAtual = (double) minutosAtribuidos / cargaMaximaMinutos;
        double ritmoEsperado = (double) diasDecorridos / diasTotais;
        return utilizacaoAtual - ritmoEsperado;
    }

    /** Dias já trabalhados na semana civil (segunda a domingo) a que {@code data} pertence. */
    public int diasTrabalhadosNaSemana(LocalDate data) {
        return diasTrabalhadosPorSemana.getOrDefault(validator.inicioSemana(data), 0);
    }

    /**
     * Indica se o colaborador ainda pode vir a ser necessário no fim de semana da
     * semana de {@code data}: ou é chefia ao sábado, ou é perfil de fim de semana,
     * ou a rotação de fins de semana não o bloqueia nesse fim de semana. Usado pela
     * fase de preenchimento para reservar margem semanal antes de sábado/domingo.
     */
    public boolean podeVirASerPrecisoNoFimDeSemana(LocalDate data, PedidoGeracao pedido) {
        if (apenasFimDeSemana || chefiaAoSabado) {
            return true;
        }
        LocalDate sabado = validator.inicioFimDeSemana(data);
        return !validator.violaRotacaoDeFimDeSemana(sabado, totalFimDeSemanaTrabalhados,
                ultimoFimDeSemanaInicio(), pedido.janelaRotacaoFimDeSemana());
    }

    public boolean podeReceber(LocalDate data,
                               Turno turno,
                               long minutosTurno,
                               PedidoGeracao pedido,
                               boolean ignorarRotacaoFDS,
                               boolean ignorarDescansoSemanal) {

        Set<LocalDate> bloqueios = pedido.bloqueiosPorColaborador()
                .getOrDefault(idUtilizador(), Set.of());

        if ((apenasFimDeSemana && !validator.ehFimDeSemana(data))
                || (exigeTurnoMinimoOitoHoras && minutosTurno < 8 * 60L)
                || bloqueios.contains(data)
                || folgasReservadas.contains(data)
                || atribuicoesConhecidas.containsKey(data)
                || (minutosAtribuidos + minutosTurno) > cargaMaximaMinutos) {
            return false;
        }

        if (!ignorarDescansoSemanal) {
            int diasNaSemana = diasTrabalhadosPorSemana
                    .getOrDefault(validator.inicioSemana(data), 0);
            if (validator.excedeDiasTrabalhadosNaSemana(
                    data, diasNaSemana, pedido.descansoSemanalMinimoDias())) {
                return false;
            }
        }

        // A rotação de FDS protege o descanso de quem também trabalha à semana;
        // o reforço de fim de semana é contratado precisamente para os FDS, pelo
        // que aplicar-lhe a rotação tornaria a carga contratual dele inalcançável.
        if (!ignorarRotacaoFDS && !apenasFimDeSemana && validator.ehFimDeSemana(data)) {
            LocalDate ultimoFDS = ultimoFimDeSemanaInicio();
            if (validator.violaRotacaoDeFimDeSemana(data,
                    totalFimDeSemanaTrabalhados, ultimoFDS,
                    pedido.janelaRotacaoFimDeSemana())) {
                return false;
            }
        }

        if (validator.violaMaximoDiasConsecutivos(ultimaDataAtribuida,
                diasConsecutivos, data, pedido.maxDiasConsecutivos())) {
            return false;
        }

        if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
            Turno turnoAnterior = atribuicoesConhecidas.get(ultimaDataAtribuida);
            if (!validator.respeitaDescansoMinimo(ultimaDataAtribuida, turnoAnterior,
                    data, turno, pedido.descansoMinimoHoras())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Explica por que motivo o colaborador <em>não</em> pode receber este turno, ou
     * {@code null} se afinal pode. Espelha {@link #podeReceber}, mas devolve o código
     * da primeira regra que falha — para o diagnóstico de falhas de cobertura.
     * Avaliado sem relaxações (cenário mais comum de falha).
     */
    public String diagnosticarExclusao(LocalDate data, Turno turno, long minutosTurno, PedidoGeracao pedido) {
        Set<LocalDate> bloqueios = pedido.bloqueiosPorColaborador()
                .getOrDefault(idUtilizador(), Set.of());

        if (apenasFimDeSemana && !validator.ehFimDeSemana(data)) return "parttime_fim_semana";
        if (exigeTurnoMinimoOitoHoras && minutosTurno < 8 * 60L) return "turno_curto";
        if (bloqueios.contains(data)) return "bloqueado";
        if (folgasReservadas.contains(data)) return "folga_reservada";
        if (atribuicoesConhecidas.containsKey(data)) return "ja_escalado";
        if ((minutosAtribuidos + minutosTurno) > cargaMaximaMinutos) return "carga_esgotada";

        int diasNaSemana = diasTrabalhadosPorSemana.getOrDefault(validator.inicioSemana(data), 0);
        if (validator.excedeDiasTrabalhadosNaSemana(data, diasNaSemana, pedido.descansoSemanalMinimoDias())) {
            return "descanso_semanal";
        }
        if (!apenasFimDeSemana && validator.ehFimDeSemana(data)
                && validator.violaRotacaoDeFimDeSemana(data, totalFimDeSemanaTrabalhados,
                        ultimoFimDeSemanaInicio(), pedido.janelaRotacaoFimDeSemana())) {
            return "rotacao_fim_semana";
        }
        if (validator.violaMaximoDiasConsecutivos(ultimaDataAtribuida, diasConsecutivos, data,
                pedido.maxDiasConsecutivos())) {
            return "dias_consecutivos";
        }
        if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
            Turno turnoAnterior = atribuicoesConhecidas.get(ultimaDataAtribuida);
            if (!validator.respeitaDescansoMinimo(ultimaDataAtribuida, turnoAnterior, data, turno,
                    pedido.descansoMinimoHoras())) {
                return "descanso_minimo";
            }
        }
        return null;
    }

    /** Nome do colaborador, ou um rótulo genérico se ausente. */
    public String nome() {
        if (ligacao.getIdUtilizador() != null && ligacao.getIdUtilizador().getNome() != null
                && !ligacao.getIdUtilizador().getNome().isBlank()) {
            return ligacao.getIdUtilizador().getNome();
        }
        Integer id = idUtilizador();
        return "Colaborador #" + (id != null ? id : "?");
    }

    public void registarAtribuicao(LocalDate data, Turno turno, long minutosTurno) {
        if (data == null || turno == null || atribuicoesConhecidas.containsKey(data)) return;

        if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
            diasConsecutivos++;
        } else {
            diasConsecutivos = 1;
        }
        ultimaDataAtribuida = data;
        atribuicoesConhecidas.put(data, turno);
        diasTrabalhadosPorSemana.merge(validator.inicioSemana(data), 1, Integer::sum);

        if (validator.ehFimDeSemana(data)) {
            totalFimDeSemanaTrabalhados++;
            ultimoFimDeSemana.put(validator.inicioFimDeSemana(data),
                    validator.inicioFimDeSemana(data));
        }
        minutosAtribuidos += minutosTurno;
    }

    public void inicializarComHistorico(List<Horario> historico) {
        for (Horario h : historico) {
            if (h.getDataTurno() == null || h.getIdTurno() == null) continue;
            registarAtribuicao(h.getDataTurno(), h.getIdTurno(), 0);
        }
    }

    /**
     * Dias de inatividade consecutivos antes de {@code data}: distância em dias desde
     * o último turno atribuído (incluindo histórico do mês anterior). Devolve 0 se nunca
     * houve atribuição — nesse caso o equilíbrio de carga já trata de priorizar este
     * colaborador. Usado pelo avaliador para penalizar longas ausências.
     */
    public int diasDesdeUltimoTurno(LocalDate data) {
        if (ultimaDataAtribuida == null) return 0;
        return (int) Math.max(0, ChronoUnit.DAYS.between(ultimaDataAtribuida, data));
    }

    /** Tipo do turno já atribuído na véspera, ou {@code null} se não trabalhou ontem. */
    public Turno turnoNaVespera(LocalDate data) {
        if (data == null) return null;
        return atribuicoesConhecidas.get(data.minusDays(1));
    }

    public boolean trabalhouFimDeSemanaAnterior(LocalDate data) {
        if (data == null || !validator.ehFimDeSemana(data)) {
            return false;
        }
        LocalDate fimDeSemanaAtual = validator.inicioFimDeSemana(data);
        return ultimoFimDeSemana.containsKey(fimDeSemanaAtual.minusWeeks(1));
    }

    private LocalDate ultimoFimDeSemanaInicio() {
        return ultimoFimDeSemana.values().stream()
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private String normalizarCargo(String tipoCargo) {
        if (tipoCargo == null) {
            return "";
        }
        return tipoCargo.trim().toLowerCase(Locale.ROOT);
    }
}
