package com.example.projeto2.API.Services;

import com.example.projeto2.API.Services.geracao.dto.*;
import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Modules.HistoricoHorarioEstado;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.PropostaHorarioMensal;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.API.Repositories.HorarioRepository;
import com.example.projeto2.API.Repositories.PropostaHorarioMensalRepository;
import com.example.projeto2.API.Services.geracao.MetricasPlaneamento;
import com.example.projeto2.API.Services.geracao.PlaneamentoGerado;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.projeto2.API.Services.geracao.HorarioFormatters.limparTexto;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.normalizarTexto;

@Component
public class PropostaPersistenciaHelper {

    private static final String ESTADO_RASCUNHO  = "rascunho";
    private static final String ESTADO_PENDENTE  = "pendente";
    private static final String ESTADO_APROVADO  = "aprovado";
    private static final String ESTADO_REJEITADO = "rejeitado";

    private static final String[] MESES_PT = {
        "", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private final PropostaResultadoBuilder resultadoBuilder;
    private final HorarioRepository horarioRepository;
    private final PropostaHorarioMensalRepository propostaHorarioMensalRepository;
    private final HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;
    private final NotificacaoService notificacaoService;

    public PropostaPersistenciaHelper(PropostaResultadoBuilder resultadoBuilder,
                                      HorarioRepository horarioRepository,
                                      PropostaHorarioMensalRepository propostaHorarioMensalRepository,
                                      HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository,
                                      NotificacaoService notificacaoService) {
        this.resultadoBuilder = resultadoBuilder;
        this.horarioRepository = horarioRepository;
        this.propostaHorarioMensalRepository = propostaHorarioMensalRepository;
        this.historicoHorarioEstadoRepository = historicoHorarioEstadoRepository;
        this.notificacaoService = notificacaoService;
    }

    public PropostaHorarioMensal persistirProposta(Lojautilizador ligacaoAtiva,
                                                   int ano, int mes,
                                                   PlaneamentoGerado planeamento,
                                                   PoliticaOtimizacao politica,
                                                   MetricasPlaneamento metricas) {
        PropostaHorarioMensal proposta = new PropostaHorarioMensal();
        proposta.setIdLoja(ligacaoAtiva.getIdLoja());
        proposta.setIdUtilizadorGeracao(ligacaoAtiva.getIdUtilizador());
        proposta.setAno(ano);
        proposta.setMes(mes);
        proposta.setEstado(ESTADO_RASCUNHO);
        proposta.setResumoGeracao(resultadoBuilder.criarResumoGeracao(planeamento, politica, metricas));
        proposta.setDataGeracao(LocalDateTime.now());
        proposta = propostaHorarioMensalRepository.save(proposta);

        List<HistoricoHorarioEstado> historicos = new ArrayList<>();
        for (Horario horario : planeamento.horarios()) {
            horario.setIdPropostaHorario(proposta);
            horario.setEstado(EstadoHorario.pendente);

            if (horario.getIdLojautilizador() != null
                    && horario.getIdLojautilizador().getIdUtilizador() != null
                    && horario.getIdTurno() != null
                    && horario.getDataTurno() != null) {
                Integer idUtilizador = horario.getIdLojautilizador().getIdUtilizador().getId();
                Integer idLoja = ligacaoAtiva.getIdLoja().getId();
                List<com.example.projeto2.API.Modules.Horario> conflitos =
                        horarioRepository.findOverlappingShiftsInOtherStores(
                                idUtilizador,
                                idLoja,
                                horario.getDataTurno(),
                                horario.getIdTurno().getHoraInicio(),
                                horario.getIdTurno().getHoraFim());
                if (!conflitos.isEmpty()) {
                    com.example.projeto2.API.Modules.Horario conflito = conflitos.getFirst();
                    String nomeColaborador = horario.getIdLojautilizador().getIdUtilizador().getNome();
                    if (nomeColaborador == null || nomeColaborador.isBlank()) nomeColaborador = "Um colaborador";
                    String dataStr = horario.getDataTurno()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    String outraLoja = (conflito.getIdLojautilizador() != null
                            && conflito.getIdLojautilizador().getIdLoja() != null
                            && conflito.getIdLojautilizador().getIdLoja().getNome() != null)
                            ? conflito.getIdLojautilizador().getIdLoja().getNome()
                            : "outra loja";
                    String hInicioConflito = conflito.getIdTurno() != null
                            && conflito.getIdTurno().getHoraInicio() != null
                            ? conflito.getIdTurno().getHoraInicio()
                                    .format(DateTimeFormatter.ofPattern("HH:mm")) : "?";
                    String hFimConflito = conflito.getIdTurno() != null
                            && conflito.getIdTurno().getHoraFim() != null
                            ? conflito.getIdTurno().getHoraFim()
                                    .format(DateTimeFormatter.ofPattern("HH:mm")) : "?";
                    String hInicioNovo = horario.getIdTurno().getHoraInicio() != null
                            ? horario.getIdTurno().getHoraInicio()
                                    .format(DateTimeFormatter.ofPattern("HH:mm")) : "?";
                    String hFimNovo = horario.getIdTurno().getHoraFim() != null
                            ? horario.getIdTurno().getHoraFim()
                                    .format(DateTimeFormatter.ofPattern("HH:mm")) : "?";
                    throw new IllegalArgumentException(
                            nomeColaborador + " já tem turno em \"" + outraLoja + "\" no dia "
                            + dataStr + " das " + hInicioConflito + " às " + hFimConflito
                            + ", que se sobrepõe ao turno " + hInicioNovo + "–" + hFimNovo
                            + " gerado para esta loja. Remove o turno existente ou exclui "
                            + nomeColaborador + " da geração.");
                }
            }

            Horario guardado = horarioRepository.save(horario);

            HistoricoHorarioEstado historico = new HistoricoHorarioEstado();
            historico.setIdHorario(guardado);
            historico.setEstadoNovo(ESTADO_PENDENTE);
            historico.setDataRegisto(Instant.now());
            historico.setObservacoes("Gerado automaticamente na proposta mensal.");
            historicos.add(historico);
        }
        historicoHorarioEstadoRepository.saveAll(historicos);

        return proposta;
    }

    public PropostaResultado decidirProposta(Lojautilizador ligacaoAtiva,
                                                                    Integer idProposta,
                                                                    String novoEstado,
                                                                    String observacoesSupervisor) {
        if (idProposta == null) {
            throw new IllegalArgumentException("Seleciona uma proposta antes de tomar uma decisao.");
        }

        PropostaHorarioMensal proposta = propostaHorarioMensalRepository
                .findByIdAndIdLojaId(idProposta, ligacaoAtiva.getIdLoja().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nao foi encontrada nenhuma proposta para a tua loja com esse identificador."));

        if (ESTADO_RASCUNHO.equals(normalizarTexto(proposta.getEstado()))) {
            throw new IllegalArgumentException(
                    "Esta proposta ainda esta em rascunho. O gerente tem de a enviar ao supervisor antes da validacao.");
        }
        if (!ESTADO_PENDENTE.equals(normalizarTexto(proposta.getEstado()))) {
            throw new IllegalArgumentException(
                    "Esta proposta ja foi decidida e nao pode voltar a ser alterada.");
        }
        if (ESTADO_APROVADO.equals(normalizarTexto(novoEstado))) {
            validarAprovacaoSemConflitos(proposta);
        }

        proposta.setEstado(novoEstado);
        proposta.setIdUtilizadorDecisao(ligacaoAtiva.getIdUtilizador());
        proposta.setDataDecisao(LocalDateTime.now());
        proposta.setObservacoesSupervisor(limparTexto(observacoesSupervisor));
        proposta = propostaHorarioMensalRepository.save(proposta);

        List<Horario> horarios = horarioRepository.findByIdPropostaHorarioId(proposta.getId());
        List<HistoricoHorarioEstado> historicos = new ArrayList<>();
        for (Horario horario : horarios) {
            horario.setEstado(EstadoHorario.valueOf(novoEstado.toLowerCase()));

            HistoricoHorarioEstado historico = new HistoricoHorarioEstado();
            historico.setIdHorario(horario);
            historico.setEstadoNovo(novoEstado);
            historico.setDataRegisto(Instant.now());
            historico.setObservacoes(criarObservacaoHistoricoDecisao(proposta, novoEstado));
            historicos.add(historico);
        }

        horarioRepository.saveAll(horarios);
        historicoHorarioEstadoRepository.saveAll(historicos);
        if (ESTADO_APROVADO.equals(normalizarTexto(novoEstado))) {
            rejeitarPropostasPendentesConcorrentes(proposta, ligacaoAtiva.getIdUtilizador());
            notificarPublicacaoHorario(proposta, horarios);
        }

        return resultadoBuilder.construirResultado(proposta, horarios);
    }

    private void validarAprovacaoSemConflitos(PropostaHorarioMensal proposta) {
        List<PropostaHorarioMensal> propostasDoPeriodo = propostaHorarioMensalRepository
                .findByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(
                        proposta.getIdLoja().getId(), proposta.getAno(), proposta.getMes());

        boolean existeOutraAprovada = propostasDoPeriodo.stream()
                .filter(outra -> !Objects.equals(outra.getId(), proposta.getId()))
                .anyMatch(outra -> ESTADO_APROVADO.equals(normalizarTexto(outra.getEstado())));
        if (existeOutraAprovada) {
            throw new IllegalArgumentException("Ja existe uma proposta aprovada para este periodo.");
        }

        LocalDate dataInicio = LocalDate.of(proposta.getAno(), proposta.getMes(), 1);
        LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());
        long horariosVisiveis = horarioRepository.countHorariosVisiveisDaLojaEntreDatas(
                proposta.getIdLoja().getId(), dataInicio, dataFim);
        if (horariosVisiveis > 0) {
            throw new IllegalArgumentException(
                    "Ja existem horarios publicados neste periodo. Nao e seguro publicar outra alternativa.");
        }
    }

    private void rejeitarPropostasPendentesConcorrentes(PropostaHorarioMensal propostaAprovada,
                                                        Utilizador decisor) {
        List<PropostaHorarioMensal> propostasDoPeriodo = propostaHorarioMensalRepository
                .findByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(
                        propostaAprovada.getIdLoja().getId(),
                        propostaAprovada.getAno(),
                        propostaAprovada.getMes());

        List<PropostaHorarioMensal> paraRejeitar = propostasDoPeriodo.stream()
                .filter(p -> !Objects.equals(p.getId(), propostaAprovada.getId()))
                .filter(p -> ESTADO_PENDENTE.equals(normalizarTexto(p.getEstado())))
                .toList();
        if (paraRejeitar.isEmpty()) return;

        List<Horario> horariosParaAtualizar = new ArrayList<>();
        List<HistoricoHorarioEstado> historicos = new ArrayList<>();
        for (PropostaHorarioMensal proposta : paraRejeitar) {
            proposta.setEstado(ESTADO_REJEITADO);
            proposta.setIdUtilizadorDecisao(decisor);
            proposta.setDataDecisao(LocalDateTime.now());
            proposta.setObservacoesSupervisor(
                    "Rejeitada automaticamente porque a proposta #"
                            + propostaAprovada.getId()
                            + " foi aprovada para o mesmo periodo.");

            for (Horario horario : horarioRepository.findByIdPropostaHorarioId(proposta.getId())) {
                horario.setEstado(EstadoHorario.rejeitado);
                horariosParaAtualizar.add(horario);

                HistoricoHorarioEstado historico = new HistoricoHorarioEstado();
                historico.setIdHorario(horario);
                historico.setEstadoNovo(ESTADO_REJEITADO);
                historico.setDataRegisto(Instant.now());
                historico.setObservacoes("Rejeitado automaticamente apos aprovacao de uma alternativa concorrente.");
                historicos.add(historico);
            }
        }

        propostaHorarioMensalRepository.saveAll(paraRejeitar);
        horarioRepository.saveAll(horariosParaAtualizar);
        historicoHorarioEstadoRepository.saveAll(historicos);
    }

    private void notificarPublicacaoHorario(PropostaHorarioMensal proposta, List<Horario> horarios) {
        String nomeMes = proposta.getMes() != null && proposta.getMes() >= 1 && proposta.getMes() <= 12
                ? MESES_PT[proposta.getMes()] : "?";
        String mensagem = "O horário oficial para " + nomeMes + " de " + proposta.getAno()
                + " já se encontra publicado! Consulta o teu calendário.";

        Integer idLoja = horarios.stream()
                .filter(h -> h.getIdLojautilizador() != null && h.getIdLojautilizador().getIdLoja() != null)
                .map(h -> h.getIdLojautilizador().getIdLoja().getId())
                .findFirst()
                .orElse(null);

        horarios.stream()
                .filter(h -> h.getIdLojautilizador() != null
                        && h.getIdLojautilizador().getIdUtilizador() != null
                        && h.getIdLojautilizador().getIdUtilizador().getId() != null)
                .map(h -> h.getIdLojautilizador().getIdUtilizador().getId())
                .collect(Collectors.toSet())
                .forEach(idUtilizador -> notificacaoService.criarNotificacao(idUtilizador, mensagem, idLoja));
    }

    private String criarObservacaoHistoricoDecisao(PropostaHorarioMensal proposta, String novoEstado) {
        String acao = ESTADO_APROVADO.equals(normalizarTexto(novoEstado)) ? "aprovado" : "rejeitado";
        String observacoes = limparTexto(proposta.getObservacoesSupervisor());
        if (observacoes == null) {
            return "Horario " + acao + " pelo supervisor.";
        }
        return "Horario " + acao + " pelo supervisor. Observacoes: " + observacoes;
    }
}
