package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Loja;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.geracao.dto.ConfiguracaoDiaEspecial;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dados carregados de repositórios e prontos para alimentar o motor de geração.
 * Produzido por {@code GeracaoHorariosService.prepararDadosGeracao()} e consumido
 * por {@link PedidoGeracaoMontador}.
 */
public record DadosGeracao(
        Lojautilizador                         ligacaoAtiva,
        Loja                                   loja,
        int                                    ano,
        int                                    mes,
        LocalDate                              dataInicio,
        LocalDate                              dataFim,
        List<Lojautilizador>                   colaboradoresAtivos,
        List<Turno>                            turnos,
        ParametrosGeracao                      parametros,
        List<Horario>                          historicoHorarios,
        Map<Integer, Set<LocalDate>>           bloqueiosPorUtilizador,
        Map<Integer, Set<LocalDate>>           diasFolgaPreferidos,
        Map<Integer, List<Preferencia>>        preferenciasTurnos,
        Map<Integer, List<Preferencia>>        preferenciasColegas,
        Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData
) {
}
