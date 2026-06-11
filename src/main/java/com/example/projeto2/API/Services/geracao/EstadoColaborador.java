package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioValidatorService;

import java.time.LocalDate;
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

    public int totalFimDeSemanaTrabalhados() { return totalFimDeSemanaTrabalhados; }

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

        if (!ignorarRotacaoFDS && validator.ehFimDeSemana(data)) {
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
        if (atribuicoesConhecidas.containsKey(data)) return "ja_escalado";
        if ((minutosAtribuidos + minutosTurno) > cargaMaximaMinutos) return "carga_esgotada";

        int diasNaSemana = diasTrabalhadosPorSemana.getOrDefault(validator.inicioSemana(data), 0);
        if (validator.excedeDiasTrabalhadosNaSemana(data, diasNaSemana, pedido.descansoSemanalMinimoDias())) {
            return "descanso_semanal";
        }
        if (validator.ehFimDeSemana(data)
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
