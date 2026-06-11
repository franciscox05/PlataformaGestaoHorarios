package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.dto.ConfiguracaoDiaEspecial;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Monta um {@link PedidoGeracao} imutável a partir dos dados já carregados em
 * {@link DadosGeracao}, isolando a lógica de transformação da orquestração de
 * repositórios ({@code GeracaoHorariosService}) e da pesquisa ({@code HorarioGeneratorEngine}).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Derivar a carga máxima em minutos por colaborador (perfil contratual × regra)</li>
 *   <li>Identificar as chefias com presença obrigatória ao sábado</li>
 *   <li>Converter {@link ConfiguracaoDiaEspecial} (DTO público) em {@link ConfiguracaoDia}
 *       (tipo interno do motor)</li>
 *   <li>Construir os pares de colegas preferidos a partir das preferências aprovadas</li>
 * </ul>
 */
@Component
public class PedidoGeracaoMontador {

    private final HorarioValidatorService validator;

    public PedidoGeracaoMontador(HorarioValidatorService validator) {
        this.validator = validator;
    }

    public PedidoGeracao montar(DadosGeracao dados,
                                PoliticaOtimizacao politica,
                                Instant prazoLimiteGeracao,
                                long sementeDiversificacao) {
        return new PedidoGeracao(
                dados.colaboradoresAtivos(),
                dados.turnos(),
                dados.dataInicio(),
                dados.dataFim(),
                dados.parametros().minimosPorTurno(),
                dados.parametros().maxDiasConsecutivos(),
                dados.parametros().descansoMinimoHoras(),
                dados.parametros().descansoSemanalMinimoDias(),
                dados.parametros().janelaRotacaoFinsDeSemanaSemanas(),
                dados.parametros().exigirChefiaAoSabado(),
                resolverChefiasSabado(dados),
                resolverCargasMaximas(dados),
                dados.bloqueiosPorUtilizador(),
                dados.preferenciasTurnos(),
                converterConfiguracoes(dados),
                dados.historicoHorarios(),
                prazoLimiteGeracao,
                politica,
                dados.diasFolgaPreferidos(),
                PreferenciasGeracaoBuilder.construirParesPreferidos(
                        dados.preferenciasColegas(), dados.colaboradoresAtivos()),
                sementeDiversificacao
        );
    }

    private Map<Integer, Long> resolverCargasMaximas(DadosGeracao dados) {
        Map<Integer, Long> cargas = new LinkedHashMap<>();
        for (Lojautilizador ligacao : dados.colaboradoresAtivos()) {
            PerfilContratual perfil = PerfilContratual.fromCargoTipo(
                            ligacao.getIdCargo() != null ? ligacao.getIdCargo().getTipo() : null)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Foi encontrado um colaborador sem perfil contratual valido para a geracao."));
            Long cargaMaximaMinutos = dados.parametros().cargaMaximaMinutosPorPerfil().get(perfil);
            if (cargaMaximaMinutos == null || cargaMaximaMinutos <= 0) {
                throw new IllegalArgumentException(
                        "Nao existe uma carga contratual mensal valida para o perfil "
                                + perfil.descricaoCurta() + ".");
            }
            cargas.put(ligacao.getIdUtilizador().getId(), cargaMaximaMinutos);
        }
        return cargas;
    }

    private Set<Integer> resolverChefiasSabado(DadosGeracao dados) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (Lojautilizador ligacao : dados.colaboradoresAtivos()) {
            if (ligacao.getIdCargo() != null
                    && validator.exigePresencaAoSabado(ligacao.getIdCargo().getNome())
                    && ligacao.getIdUtilizador() != null) {
                ids.add(ligacao.getIdUtilizador().getId());
            }
        }
        return ids;
    }

    private Map<LocalDate, ConfiguracaoDia> converterConfiguracoes(DadosGeracao dados) {
        Map<LocalDate, ConfiguracaoDia> configsEngine = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, ConfiguracaoDiaEspecial> entry : dados.configuracoesPorData().entrySet()) {
            ConfiguracaoDiaEspecial src = entry.getValue();
            configsEngine.put(entry.getKey(), new ConfiguracaoDia(
                    src.lojaEncerrada(), src.turnosCompativeis(),
                    src.minimoColaboradoresTurno(), src.descricao()));
        }
        return configsEngine;
    }
}
