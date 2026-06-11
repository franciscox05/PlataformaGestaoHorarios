package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Turno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Input imutável e auto-contido do motor de geração de horários.
 *
 * <p>Contém tudo o que o algoritmo precisa — colaboradores, turnos, parâmetros
 * de regras, bloqueios, preferências e a política de otimização ativa — sem
 * qualquer dependência de repositórios. Construído pela camada de serviço a
 * partir dos dados já carregados, e consumido pelo {@code HorarioGeneratorEngine}.
 *
 * @param colaboradores               ligações loja-utilizador elegíveis
 * @param turnos                      turnos base configurados na loja
 * @param dataInicio                  primeiro dia do período (inclusivo)
 * @param dataFim                     último dia do período (inclusivo)
 * @param minimosPorTurno             id-turno → nº mínimo de colaboradores
 * @param maxDiasConsecutivos         RFS07 — teto de dias seguidos
 * @param descansoMinimoHoras         RFS06 — horas mínimas entre turnos
 * @param descansoSemanalMinimoDias   RFS04 — dias de folga por semana
 * @param janelaRotacaoFimDeSemana    RFS08 — semanas entre fins de semana trabalhados
 * @param exigirChefiaAoSabado        RFS03 — gerente/subgerente obrigatório ao sábado
 * @param chefiasSabadoIds            ids dos colaboradores que cumprem chefia ao sábado
 * @param cargaMaximaPorColaborador   RFS10 — id-colaborador → minutos contratuais/mês
 * @param bloqueiosPorColaborador     hard blocks (folgas/férias aprovadas)
 * @param preferenciasTurnos          preferências aprovadas de turno por colaborador
 * @param configuracoesPorData        exceções de calendário por data
 * @param historicoHorarios           turnos anteriores ao período (continuidade de regras)
 * @param prazoLimite                 instante-limite para a pesquisa (timeout)
 * @param politica                    política de otimização ativa (pesos do scoring)
 * @param folgasPreferidasPorColaborador  soft — id-colaborador → datas de folga preferida (1/semana)
 * @param paresPreferisPorColaborador soft — id-colaborador → ids de colegas preferidos
 * @param semente                     semente de diversificação (desempate determinístico)
 */
public record PedidoGeracao(
        List<Lojautilizador>            colaboradores,
        List<Turno>                     turnos,
        LocalDate                       dataInicio,
        LocalDate                       dataFim,
        Map<Integer, Integer>           minimosPorTurno,
        int                             maxDiasConsecutivos,
        int                             descansoMinimoHoras,
        int                             descansoSemanalMinimoDias,
        int                             janelaRotacaoFimDeSemana,
        boolean                         exigirChefiaAoSabado,
        Set<Integer>                    chefiasSabadoIds,
        Map<Integer, Long>              cargaMaximaPorColaborador,
        Map<Integer, Set<LocalDate>>    bloqueiosPorColaborador,
        Map<Integer, List<Preferencia>> preferenciasTurnos,
        Map<LocalDate, ConfiguracaoDia> configuracoesPorData,
        List<Horario>                   historicoHorarios,
        Instant                         prazoLimite,
        PoliticaOtimizacao              politica,
        Map<Integer, Set<LocalDate>>    folgasPreferidasPorColaborador,
        Map<Integer, Set<Integer>>      paresPreferisPorColaborador,
        long                            semente
) {
}
