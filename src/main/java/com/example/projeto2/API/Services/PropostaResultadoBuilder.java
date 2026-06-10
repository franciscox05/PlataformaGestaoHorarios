package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Loja;
import com.example.projeto2.API.Modules.PropostaHorarioMensal;
import com.example.projeto2.API.Services.geracao.HorarioFormatters;
import com.example.projeto2.API.Services.geracao.MetricasPlaneamento;
import com.example.projeto2.API.Services.geracao.MetricasPlaneamentoCalculator;
import com.example.projeto2.API.Services.geracao.PlaneamentoGerado;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.example.projeto2.API.Services.geracao.HorarioFormatters.calcularDuracaoEmMinutos;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarDuracao;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarEstado;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarPeriodo;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarTurno;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.nomeDiaSemana;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.nomeMes;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.normalizarTexto;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.valorOuTraco;

@Component
public class PropostaResultadoBuilder {

    private static final String ESTADO_RASCUNHO  = "rascunho";
    private static final String ESTADO_PENDENTE  = "pendente";
    private static final String ESTADO_APROVADO  = "aprovado";
    private static final String ESTADO_REJEITADO = "rejeitado";

    private final MetricasPlaneamentoCalculator metricasCalculator;

    public PropostaResultadoBuilder(MetricasPlaneamentoCalculator metricasCalculator) {
        this.metricasCalculator = metricasCalculator;
    }

    public String criarResumoGeracao(PlaneamentoGerado planeamento,
                                     PoliticaOtimizacao politica,
                                     MetricasPlaneamento metricas) {
        int colaboradoresComTurnos = (int) planeamento.estados().stream()
                .filter(estado -> estado.turnosAtribuidos() > 0)
                .count();
        return "Modelo IO: satisfacao de restricoes com funcao objetivo ponderada ("
                + politica.nome()
                + "). Proposta gerada automaticamente com "
                + planeamento.horarios().size()
                + " turnos distribuidos por "
                + colaboradoresComTurnos
                + " colaboradores. Pontuacao: "
                + metricas.pontuacao()
                + " (menor e melhor). Carga: desvio medio "
                + metricas.desvioMedioHoras()
                + ", amplitude "
                + metricas.amplitudeHoras()
                + ". Politica: "
                + politica.descricao()
                + ".";
    }

    public GeracaoHorariosService.PropostaResultado construirResultado(
            PropostaHorarioMensal proposta, List<Horario> horarios) {
        return construirResultado(
                proposta.getId(),
                valorOuTraco(proposta.getIdLoja().getNome()),
                proposta.getAno(),
                proposta.getMes(),
                nomeMes(proposta.getMes()),
                formatarEstado(proposta.getEstado()),
                construirOrigemPlaneamento(proposta),
                proposta.getResumoGeracao(),
                valorOuTraco(proposta.getIdUtilizadorGeracao().getNome()),
                proposta.getDataGeracao() != null
                        ? HorarioFormatters.DATA_HORA_FORMATTER.format(proposta.getDataGeracao()) : "-",
                proposta.getIdUtilizadorDecisao() != null
                        ? valorOuTraco(proposta.getIdUtilizadorDecisao().getNome()) : "-",
                proposta.getDataDecisao() != null
                        ? HorarioFormatters.DATA_HORA_FORMATTER.format(proposta.getDataDecisao()) : "-",
                valorOuTraco(proposta.getObservacoesSupervisor()),
                ESTADO_PENDENTE.equals(normalizarTexto(proposta.getEstado())),
                horarios
        );
    }

    public GeracaoHorariosService.PropostaResultado construirResultadoHorariosPublicados(
            Loja loja, int ano, int mes, List<Horario> horarios) {
        String resumo = "Foram encontrados "
                + horarios.size()
                + " turnos ja publicados para "
                + nomeMes(mes).toLowerCase(Locale.ROOT)
                + " de "
                + ano
                + ". Podes consultar o planeamento atual, mesmo sem existir uma proposta mensal guardada.";
        return construirResultado(
                null, valorOuTraco(loja.getNome()), ano, mes, nomeMes(mes),
                "Publicado", "Horarios publicados", resumo,
                "-", "-", "-", "-",
                "Estes horarios ja estao publicados na loja e podem ser analisados diretamente neste ecra.",
                false, horarios
        );
    }

    public GeracaoHorariosService.PropostaResultado construirResultado(
            Integer idProposta, String nomeLoja, Integer ano, Integer mes,
            String nomeMes, String estado, String origemPlaneamento,
            String resumoGeracao, String geradoPor, String dataGeracao,
            String decididoPor, String dataDecisao, String observacoesSupervisor,
            boolean podeSerDecidida, List<Horario> horarios) {

        List<GeracaoHorariosService.HorarioLinha> linhas = horarios.stream()
                .sorted(Comparator
                        .comparing(Horario::getDataTurno, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(h -> h.getIdTurno() != null ? h.getIdTurno().getHoraInicio() : LocalTime.MIN)
                        .thenComparing(h -> h.getIdLojautilizador().getIdUtilizador().getNome(),
                                String.CASE_INSENSITIVE_ORDER))
                .map(this::mapearLinhaHorario)
                .toList();

        Map<Integer, ResumoAcumulado> acumulado = new LinkedHashMap<>();
        for (Horario horario : horarios) {
            if (horario.getIdLojautilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador() == null) {
                continue;
            }
            Integer idCol = horario.getIdLojautilizador().getIdUtilizador().getId();
            ResumoAcumulado r = acumulado.computeIfAbsent(
                    idCol,
                    ignored -> new ResumoAcumulado(
                            idCol,
                            valorOuTraco(horario.getIdLojautilizador().getIdUtilizador().getNome()),
                            horario.getIdLojautilizador().getIdCargo() != null
                                    ? valorOuTraco(horario.getIdLojautilizador().getIdCargo().getNome())
                                    : "-"
                    )
            );
            acumulado.put(idCol, r.comTurno(calcularDuracaoEmMinutos(horario.getIdTurno())));
        }

        List<GeracaoHorariosService.ResumoColaborador> resumoColaboradores =
                acumulado.values().stream()
                        .sorted(Comparator.comparing(ResumoAcumulado::nomeColaborador,
                                String.CASE_INSENSITIVE_ORDER))
                        .map(r -> new GeracaoHorariosService.ResumoColaborador(
                                r.idColaborador(), r.nomeColaborador(), r.cargo(),
                                r.turnos(), r.minutos(), formatarDuracao(r.minutos())))
                        .toList();

        Set<LocalDate> diasCobertos = new LinkedHashSet<>();
        for (Horario horario : horarios) {
            if (horario.getDataTurno() != null) {
                diasCobertos.add(horario.getDataTurno());
            }
        }

        return new GeracaoHorariosService.PropostaResultado(
                idProposta, nomeLoja, ano, mes, nomeMes, estado,
                origemPlaneamento, resumoGeracao, geradoPor, dataGeracao,
                decididoPor, dataDecisao, observacoesSupervisor, podeSerDecidida,
                linhas, resumoColaboradores,
                metricasCalculator.calcular(horarios,
                        metricasCalculator.extrairPolitica(resumoGeracao)),
                new GeracaoHorariosService.ResumoGeral(
                        resumoColaboradores.size(), horarios.size(), diasCobertos.size())
        );
    }

    public GeracaoHorariosService.PropostaResumo construirResumoProposta(
            PropostaHorarioMensal proposta,
            GeracaoHorariosService.PropostaResultado resultado) {
        return new GeracaoHorariosService.PropostaResumo(
                proposta.getId(),
                rotuloCurtoProposta(resultado),
                resultado.estado(),
                resultado.dataGeracao(),
                resultado.geradoPor(),
                resultado.metricas().politicaOtimizacao(),
                resultado.metricas().pontuacao(),
                resultado.metricas().qualidade(),
                resultado.metricas().desvioMedioHoras(),
                resultado.metricas().amplitudeHoras(),
                resultado.resumo().colaboradores(),
                resultado.resumo().turnos(),
                resultado.resumo().diasCobertos()
        );
    }

    public String rotuloCurtoProposta(GeracaoHorariosService.PropostaResultado proposta) {
        if (proposta == null || proposta.idProposta() == null) {
            return "Horarios publicados";
        }
        return proposta.nomeMes()
                + " " + proposta.ano()
                + " · #" + proposta.idProposta()
                + " · " + proposta.estado();
    }

    private GeracaoHorariosService.HorarioLinha mapearLinhaHorario(Horario horario) {
        Integer idColaborador = horario.getIdLojautilizador() != null
                && horario.getIdLojautilizador().getIdUtilizador() != null
                ? horario.getIdLojautilizador().getIdUtilizador().getId() : null;
        String colaborador = horario.getIdLojautilizador() != null
                && horario.getIdLojautilizador().getIdUtilizador() != null
                ? valorOuTraco(horario.getIdLojautilizador().getIdUtilizador().getNome()) : "-";
        String cargo = horario.getIdLojautilizador() != null
                && horario.getIdLojautilizador().getIdCargo() != null
                ? valorOuTraco(horario.getIdLojautilizador().getIdCargo().getNome()) : "-";
        return new GeracaoHorariosService.HorarioLinha(
                horario.getId(), idColaborador, horario.getDataTurno(),
                horario.getDataTurno() != null ? nomeDiaSemana(horario.getDataTurno()) : "-",
                formatarTurno(horario.getIdTurno()),
                formatarPeriodo(horario.getIdTurno()),
                colaborador, cargo,
                formatarEstado(horario.getEstado() != null ? horario.getEstado().name() : null)
        );
    }

    private String construirOrigemPlaneamento(PropostaHorarioMensal proposta) {
        return switch (normalizarTexto(proposta.getEstado())) {
            case ESTADO_APROVADO  -> "Horarios publicados a partir de proposta aprovada";
            case ESTADO_REJEITADO -> "Proposta mensal rejeitada";
            case ESTADO_RASCUNHO  -> "Rascunho do gerente";
            case ESTADO_PENDENTE  -> "Enviada ao supervisor";
            default               -> "Proposta mensal";
        };
    }

    private record ResumoAcumulado(Integer idColaborador, String nomeColaborador,
                                   String cargo, int turnos, long minutos) {
        ResumoAcumulado(Integer idColaborador, String nomeColaborador, String cargo) {
            this(idColaborador, nomeColaborador, cargo, 0, 0);
        }

        ResumoAcumulado comTurno(long minutosTurno) {
            return new ResumoAcumulado(idColaborador, nomeColaborador, cargo,
                    turnos + 1, minutos + minutosTurno);
        }
    }
}
