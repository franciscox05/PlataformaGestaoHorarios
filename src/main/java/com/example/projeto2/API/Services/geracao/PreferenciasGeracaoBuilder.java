package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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
     * Soft constraints de folga (penalização sem hard block). Reservado para
     * preferências recorrentes por dia-da-semana sem data fixa. Por agora vazio —
     * as preferências aprovadas com datas já são hard blocks.
     */
    public static Map<Integer, Set<LocalDate>> construirDiasFolgaPreferidos(LocalDate dataInicio,
                                                                              LocalDate dataFim,
                                                                              List<Preferencia> preferenciasAprovadas) {
        return Map.of();
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

        Map<String, Integer> nomeParaId = new HashMap<>();
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
