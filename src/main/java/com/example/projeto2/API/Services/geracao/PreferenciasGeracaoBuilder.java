package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Constrói, a partir das preferências/folgas dos colaboradores, as estruturas de input
 * para o motor de geração:
 *
 * <ul>
 *   <li><b>bloqueios duros</b> (hard blocks) — datas em que o colaborador NÃO pode ser
 *       colocado em qualquer turno (folgas aprovadas + férias)</li>
 *   <li><b>folgas preferidas</b> (soft) — reservado para extensão futura</li>
 *   <li><b>pares de colegas preferidos</b> — para penalizar/recompensar coabitação no
 *       mesmo turno</li>
 *   <li>agrupamento de preferências por tipo (turnos, colegas, etc.)</li>
 * </ul>
 *
 * <p>Sem estado e sem dependências de repositórios — todas as funções são puras.
 */
public final class PreferenciasGeracaoBuilder {

    private PreferenciasGeracaoBuilder() {
        // utilitário
    }

    /**
     * Hard blocks: junta os {@code DayOff} aprovados e as {@link Preferencia preferências}
     * aprovadas de tipo "folgas"/"ferias" com data explícita, e devolve um mapa
     * idColaborador → datas em que NÃO pode ser colocado.
     */
    public static Map<Integer, Set<LocalDate>> construirBloqueiosPorUtilizador(LocalDate dataInicio,
                                                                                LocalDate dataFim,
                                                                                List<DayOff> dayOffsAprovados,
                                                                                List<Preferencia> preferenciasAprovadas) {
        Map<Integer, Set<LocalDate>> bloqueios = new HashMap<>();

        for (DayOff dayOff : dayOffsAprovados) {
            if (dayOff.getIdUtilizador() == null || dayOff.getDataAusencia() == null) {
                continue;
            }
            bloqueios.computeIfAbsent(dayOff.getIdUtilizador().getId(), k -> new LinkedHashSet<>())
                    .add(dayOff.getDataAusencia());
        }

        for (Preferencia preferencia : preferenciasAprovadas) {
            if (preferencia.getIdUtilizador() == null || preferencia.getIdUtilizador().getId() == null) {
                continue;
            }

            String tipo = HorarioFormatters.normalizarTexto(preferencia.getTipo());
            if (!"folgas".equals(tipo) && !"ferias".equals(tipo)) {
                continue;
            }

            LocalDate inicio = preferencia.getDataInicio() != null ? preferencia.getDataInicio() : dataInicio;
            LocalDate fim    = preferencia.getDataFim()    != null ? preferencia.getDataFim()    : dataFim;
            if (inicio == null) continue;

            LocalDate dataAtual  = inicio.isBefore(dataInicio) ? dataInicio : inicio;
            LocalDate ultimaData = fim.isAfter(dataFim)        ? dataFim    : fim;
            if (ultimaData.isBefore(dataAtual)) continue;

            Set<LocalDate> datas = bloqueios.computeIfAbsent(
                    preferencia.getIdUtilizador().getId(), k -> new LinkedHashSet<>());
            for (LocalDate data = dataAtual; !data.isAfter(ultimaData); data = data.plusDays(1)) {
                datas.add(data);
            }
        }

        return bloqueios;
    }

    /**
     * Soft constraints de folga (penalização, <b>sem</b> hard block): preferências aprovadas
     * do tipo {@code "folga_preferida"} — uma folga recorrente semanal que o algoritmo tenta
     * muito honrar mas pode quebrar se for preciso para a cobertura (ao contrário de "folgas"/
     * "ferias", que são ausências concedidas e bloqueiam).
     *
     * <p>O dia da semana preferido vem da {@code dataInicio}; a preferência aplica-se a esse
     * dia em cada semana do período (clamp a [dataInicio, dataFim] quando definido). Para
     * garantir <b>uma folga preferida por semana</b>, datas extra na mesma semana ISO são
     * descartadas (fica a mais cedo).
     */
    public static Map<Integer, Set<LocalDate>> construirDiasFolgaPreferidos(LocalDate dataInicio,
                                                                              LocalDate dataFim,
                                                                              List<Preferencia> preferenciasAprovadas) {
        Map<Integer, Set<LocalDate>> resultado = new HashMap<>();
        Map<Integer, Set<LocalDate>> semanasUsadas = new HashMap<>();

        for (Preferencia preferencia : preferenciasAprovadas) {
            if (preferencia.getIdUtilizador() == null || preferencia.getIdUtilizador().getId() == null) {
                continue;
            }
            if (!"folga_preferida".equals(HorarioFormatters.normalizarTexto(preferencia.getTipo()))) {
                continue;
            }

            LocalDate referenciaDia = preferencia.getDataInicio() != null
                    ? preferencia.getDataInicio() : dataInicio;
            if (referenciaDia == null) continue;
            DayOfWeek diaPreferido = referenciaDia.getDayOfWeek();

            LocalDate inicio = referenciaDia.isBefore(dataInicio) ? dataInicio : referenciaDia;
            LocalDate fim = (preferencia.getDataFim() != null && preferencia.getDataFim().isBefore(dataFim))
                    ? preferencia.getDataFim() : dataFim;
            if (inicio.isAfter(fim)) continue;

            Integer idColaborador = preferencia.getIdUtilizador().getId();
            Set<LocalDate> dias = resultado.computeIfAbsent(idColaborador, k -> new LinkedHashSet<>());
            Set<LocalDate> semanas = semanasUsadas.computeIfAbsent(idColaborador, k -> new LinkedHashSet<>());

            for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
                if (data.getDayOfWeek() != diaPreferido) continue;
                LocalDate inicioSemana = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                if (semanas.add(inicioSemana)) { // uma folga preferida por semana
                    dias.add(data);
                }
            }
        }
        return resultado;
    }

    /**
     * Constrói o mapa idColaborador → ids dos colegas preferidos. A descrição de cada
     * preferência do tipo "colegas" contém nomes separados por vírgula/ponto-e-vírgula;
     * cada nome é resolvido por contenção case-insensitive contra os colaboradores ativos.
     */
    public static Map<Integer, Set<Integer>> construirParesPreferidos(
            Map<Integer, List<Preferencia>> preferenciasColegas,
            List<Lojautilizador> colaboradoresAtivos) {

        if (preferenciasColegas == null || preferenciasColegas.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> nomeParaId = new LinkedHashMap<>();
        for (Lojautilizador lu : colaboradoresAtivos) {
            if (lu.getIdUtilizador() != null && lu.getIdUtilizador().getNome() != null) {
                nomeParaId.put(
                        HorarioFormatters.normalizarTexto(lu.getIdUtilizador().getNome()),
                        lu.getIdUtilizador().getId());
            }
        }

        Map<Integer, Set<Integer>> pares = new HashMap<>();
        for (Map.Entry<Integer, List<Preferencia>> entrada : preferenciasColegas.entrySet()) {
            Integer idRequerente = entrada.getKey();
            Set<Integer> idsPreferidos = new LinkedHashSet<>();

            for (Preferencia pref : entrada.getValue()) {
                String descricao = pref.getDescricao();
                if (descricao == null || descricao.isBlank()) continue;

                String[] partes = descricao.split("[,;\n]+");
                for (String parte : partes) {
                    String nomeNormalizado = HorarioFormatters.normalizarTexto(parte.trim());
                    if (nomeNormalizado.isBlank()) continue;

                    Integer idEncontrado = nomeParaId.get(nomeNormalizado);
                    if (idEncontrado == null) {
                        for (Map.Entry<String, Integer> e : nomeParaId.entrySet()) {
                            if (e.getKey().contains(nomeNormalizado)
                                    || nomeNormalizado.contains(e.getKey())) {
                                idEncontrado = e.getValue();
                                break;
                            }
                        }
                    }
                    if (idEncontrado != null && !idEncontrado.equals(idRequerente)) {
                        idsPreferidos.add(idEncontrado);
                    }
                }
            }

            if (!idsPreferidos.isEmpty()) {
                pares.put(idRequerente, idsPreferidos);
            }
        }

        return pares;
    }

    /**
     * Para cada folga preferida (soft) que o algoritmo acabou por quebrar — o colaborador
     * foi escalado num dia que preferia de folga, por necessidade de cobertura — gera uma
     * nota explicativa que é anexada ao resumo da proposta.
     */
    public static List<String> construirAvisosNaoHonrados(List<Lojautilizador> colaboradoresAtivos,
                                                          Map<Integer, Set<LocalDate>> diasFolgaPreferidos,
                                                          List<Horario> horarios) {
        if (diasFolgaPreferidos == null || diasFolgaPreferidos.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> nomePorColaborador = new HashMap<>();
        for (Lojautilizador ligacao : colaboradoresAtivos) {
            if (ligacao.getIdUtilizador() != null && ligacao.getIdUtilizador().getId() != null) {
                nomePorColaborador.put(ligacao.getIdUtilizador().getId(),
                        HorarioFormatters.valorOuTraco(ligacao.getIdUtilizador().getNome()));
            }
        }

        Map<Integer, Set<LocalDate>> diasTrabalhados = new HashMap<>();
        for (Horario h : horarios) {
            if (h.getIdLojautilizador() == null
                    || h.getIdLojautilizador().getIdUtilizador() == null
                    || h.getDataTurno() == null) {
                continue;
            }
            diasTrabalhados.computeIfAbsent(
                    h.getIdLojautilizador().getIdUtilizador().getId(),
                    k -> new LinkedHashSet<>()).add(h.getDataTurno());
        }

        List<String> avisos = new ArrayList<>();
        for (Map.Entry<Integer, Set<LocalDate>> entry : diasFolgaPreferidos.entrySet()) {
            Set<LocalDate> trabalhados = diasTrabalhados.getOrDefault(entry.getKey(), Set.of());
            for (LocalDate diaPreferido : entry.getValue()) {
                if (trabalhados.contains(diaPreferido)) {
                    avisos.add("Folga preferida de "
                            + nomePorColaborador.getOrDefault(entry.getKey(),
                                    "Colaborador #" + entry.getKey())
                            + " em " + HorarioFormatters.DATA_FORMATTER.format(diaPreferido)
                            + " nao foi honrada (cobertura necessaria nesse dia).");
                }
            }
        }
        return avisos;
    }

    /**
     * Agrupa preferências de um determinado tipo por id de colaborador. O tipo é
     * comparado já normalizado (sem acentos, lowercase).
     */
    public static Map<Integer, List<Preferencia>> agruparPorTipo(List<Preferencia> preferencias, String tipo) {
        Map<Integer, List<Preferencia>> agrupadas = new HashMap<>();
        String tipoNormalizado = HorarioFormatters.normalizarTexto(tipo);

        for (Preferencia preferencia : preferencias) {
            if (preferencia.getIdUtilizador() == null || preferencia.getIdUtilizador().getId() == null) {
                continue;
            }
            if (!tipoNormalizado.equals(HorarioFormatters.normalizarTexto(preferencia.getTipo()))) {
                continue;
            }
            agrupadas.computeIfAbsent(preferencia.getIdUtilizador().getId(), k -> new ArrayList<>())
                    .add(preferencia);
        }
        return agrupadas;
    }
}
