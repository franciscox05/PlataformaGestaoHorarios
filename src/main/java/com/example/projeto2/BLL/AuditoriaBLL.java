package com.example.projeto2.BLL;

import com.example.projeto2.Modules.EventoAuditoria;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.EventoAuditoriaRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditoriaBLL {

    private static final Set<String> CARGOS_COM_AUDITORIA = Set.of("gerente", "subgerente");
    private static final Set<String> TIPOS_AUTENTICACAO = Set.of("login", "logout", "sessao_expirada");
    private static final Set<String> ORIGENS_SENSIVEIS = Set.of("perfil", "gestao_funcionarios", "gestao_loja");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EventoAuditoriaRepository eventoAuditoriaRepository;
    private final LojautilizadorRepository lojautilizadorRepository;

    public AuditoriaBLL(EventoAuditoriaRepository eventoAuditoriaRepository,
                        LojautilizadorRepository lojautilizadorRepository) {
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
    }

    @Transactional
    public void registarFalhaLogin(String emailReferencia, String detalhes) {
        registarEvento("login", "falha", null, emailReferencia, null, "autenticacao", detalhes);
    }

    @Transactional
    public void registarLoginSucesso(Utilizador utilizador, String identificadorSessao) {
        registarEvento(
                "login",
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                "autenticacao",
                "Autenticacao concluida com sucesso."
        );
    }

    @Transactional
    public void registarLogout(Utilizador utilizador, String identificadorSessao) {
        registarEvento(
                "logout",
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                "sessao",
                "Sessao terminada manualmente."
        );
    }

    @Transactional
    public void registarSessaoExpirada(Utilizador utilizador, String identificadorSessao) {
        registarEvento(
                "sessao_expirada",
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                "sessao",
                "Sessao terminada por inatividade."
        );
    }

    @Transactional
    public void registarAlteracaoPassword(Utilizador utilizador, String identificadorSessao) {
        registarEvento(
                "alteracao_password",
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                "perfil",
                "Password atualizada com sucesso."
        );
    }

    @Transactional
    public void registarEventoSensivel(String tipoEvento,
                                       Utilizador utilizador,
                                       String identificadorSessao,
                                       String origem,
                                       String detalhes) {
        registarEvento(
                tipoEvento,
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                origem,
                detalhes
        );
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeConsultarAuditoria(Integer idUtilizador) {
        if (idUtilizador == null) {
            return false;
        }

        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(Lojautilizador::getIdCargo)
                .map(cargo -> cargo.getTipo() != null ? cargo.getTipo().toLowerCase(Locale.ROOT) : "")
                .filter(CARGOS_COM_AUDITORIA::contains)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public PainelAuditoriaSnapshot carregarPainel(Integer idUtilizadorGestor, FiltroAuditoria filtro) {
        FiltroAuditoria filtroNormalizado = normalizarFiltro(filtro);
        Lojautilizador ligacaoGestor = obterLigacaoAtivaComPermissao(idUtilizadorGestor);

        Map<Integer, FiltroUtilizador> colaboradoresPorId = carregarColaboradoresDaLoja(ligacaoGestor.getIdLoja().getId());
        Map<String, FiltroUtilizador> colaboradoresPorEmail = colaboradoresPorId.values().stream()
                .filter(colaborador -> colaborador.email() != null)
                .collect(Collectors.toMap(
                        colaborador -> colaborador.email().toLowerCase(Locale.ROOT),
                        colaborador -> colaborador,
                        (atual, ignorado) -> atual,
                        LinkedHashMap::new
                ));

        if (filtroNormalizado.idUtilizador() != null && !colaboradoresPorId.containsKey(filtroNormalizado.idUtilizador())) {
            throw new IllegalArgumentException("Seleciona um colaborador valido para consultar a auditoria.");
        }

        List<EventoAuditoria> eventosRelacionados = eventoAuditoriaRepository.findAllByOrderByDataEventoDesc(PageRequest.of(0, 500))
                .stream()
                .filter(evento -> pertenceALoja(evento, colaboradoresPorId.keySet(), colaboradoresPorEmail.keySet()))
                .toList();

        List<FiltroTipoEvento> tiposEvento = eventosRelacionados.stream()
                .map(EventoAuditoria::getTipoEvento)
                .map(this::normalizar)
                .filter(tipo -> tipo != null && !tipo.isBlank())
                .distinct()
                .sorted()
                .map(tipo -> new FiltroTipoEvento(tipo, formatarEtiqueta(tipo)))
                .toList();

        FiltroUtilizador colaboradorFiltrado = filtroNormalizado.idUtilizador() != null
                ? colaboradoresPorId.get(filtroNormalizado.idUtilizador())
                : null;

        List<EventoAuditoria> eventosFiltrados = eventosRelacionados.stream()
                .filter(evento -> cumpreFiltroTipo(evento, filtroNormalizado.tipoEvento()))
                .filter(evento -> cumpreFiltroColaborador(evento, colaboradorFiltrado))
                .filter(evento -> cumpreFiltroData(evento, filtroNormalizado.dataInicio(), filtroNormalizado.dataFim()))
                .toList();

        List<EventoLinha> linhas = eventosFiltrados.stream()
                .map(evento -> criarLinha(evento, colaboradoresPorId, colaboradoresPorEmail))
                .toList();

        AuditoriaResumo resumo = new AuditoriaResumo(
                linhas.size(),
                (int) eventosFiltrados.stream().filter(this::eventoFalhado).count(),
                (int) eventosFiltrados.stream().filter(this::eventoDeAutenticacao).count(),
                (int) eventosFiltrados.stream().filter(this::eventoSensivel).count()
        );

        AuditoriaContexto contexto = new AuditoriaContexto(
                ligacaoGestor.getIdLoja().getId(),
                valorOuTraco(ligacaoGestor.getIdLoja().getNome()),
                valorOuTraco(ligacaoGestor.getIdLoja().getLocalizacao()),
                ligacaoGestor.getIdCargo() != null ? valorOuTraco(ligacaoGestor.getIdCargo().getNome()) : "-",
                new ArrayList<>(colaboradoresPorId.values()),
                tiposEvento
        );

        return new PainelAuditoriaSnapshot(contexto, resumo, linhas);
    }

    private void registarEvento(String tipoEvento,
                                String resultado,
                                Utilizador utilizador,
                                String emailReferencia,
                                String identificadorSessao,
                                String origem,
                                String detalhes) {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setTipoEvento(normalizar(valorOuPadrao(tipoEvento, "evento")));
        evento.setResultado(normalizar(valorOuPadrao(resultado, "sucesso")));
        evento.setOrigem(normalizar(valorOuPadrao(origem, "sistema")));
        evento.setIdUtilizador(utilizador);
        evento.setEmailReferencia(normalizarEmail(emailReferencia));
        evento.setIdentificadorSessao(limparTexto(identificadorSessao));
        evento.setDataEvento(Instant.now());
        evento.setDetalhes(limparTexto(detalhes));
        eventoAuditoriaRepository.save(evento);
    }

    private Lojautilizador obterLigacaoAtivaComPermissao(Integer idUtilizadorGestor) {
        if (idUtilizadorGestor == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        Lojautilizador ligacaoGestor = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizadorGestor)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa a uma loja."));

        String tipoCargo = ligacaoGestor.getIdCargo() != null && ligacaoGestor.getIdCargo().getTipo() != null
                ? ligacaoGestor.getIdCargo().getTipo().toLowerCase(Locale.ROOT)
                : "";

        if (!CARGOS_COM_AUDITORIA.contains(tipoCargo)) {
            throw new IllegalArgumentException("Nao tens permissao para consultar o historico de auditoria.");
        }

        return ligacaoGestor;
    }

    private Map<Integer, FiltroUtilizador> carregarColaboradoresDaLoja(Integer idLoja) {
        Map<Integer, FiltroUtilizador> colaboradores = new LinkedHashMap<>();

        for (Lojautilizador ligacao : lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja)) {
            if (ligacao.getIdUtilizador() == null || ligacao.getIdUtilizador().getId() == null) {
                continue;
            }

            colaboradores.putIfAbsent(
                    ligacao.getIdUtilizador().getId(),
                    new FiltroUtilizador(
                            ligacao.getIdUtilizador().getId(),
                            valorOuTraco(ligacao.getIdUtilizador().getNome()),
                            normalizarEmail(ligacao.getIdUtilizador().getEmail())
                    )
            );
        }

        return colaboradores.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().nome(), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (atual, ignorado) -> atual,
                        LinkedHashMap::new
                ));
    }

    private FiltroAuditoria normalizarFiltro(FiltroAuditoria filtro) {
        if (filtro == null) {
            return new FiltroAuditoria(null, null, null, null);
        }

        LocalDate dataInicio = filtro.dataInicio();
        LocalDate dataFim = filtro.dataFim();
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data inicial nao pode ser posterior a data final.");
        }

        return new FiltroAuditoria(
                normalizar(filtro.tipoEvento()),
                filtro.idUtilizador(),
                dataInicio,
                dataFim
        );
    }

    private boolean pertenceALoja(EventoAuditoria evento,
                                  Set<Integer> idsColaboradores,
                                  Set<String> emailsColaboradores) {
        Integer idUtilizadorEvento = evento.getIdUtilizador() != null ? evento.getIdUtilizador().getId() : null;
        if (idUtilizadorEvento != null && idsColaboradores.contains(idUtilizadorEvento)) {
            return true;
        }

        String emailEvento = normalizarEmail(evento.getEmailReferencia());
        return emailEvento != null && emailsColaboradores.contains(emailEvento);
    }

    private boolean cumpreFiltroTipo(EventoAuditoria evento, String tipoFiltrado) {
        if (tipoFiltrado == null) {
            return true;
        }

        return tipoFiltrado.equals(normalizar(evento.getTipoEvento()));
    }

    private boolean cumpreFiltroColaborador(EventoAuditoria evento, FiltroUtilizador colaboradorFiltrado) {
        if (colaboradorFiltrado == null) {
            return true;
        }

        Integer idEvento = evento.getIdUtilizador() != null ? evento.getIdUtilizador().getId() : null;
        if (idEvento != null && idEvento.equals(colaboradorFiltrado.id())) {
            return true;
        }

        String emailEvento = normalizarEmail(evento.getEmailReferencia());
        return emailEvento != null && emailEvento.equals(normalizarEmail(colaboradorFiltrado.email()));
    }

    private boolean cumpreFiltroData(EventoAuditoria evento, LocalDate dataInicio, LocalDate dataFim) {
        if (evento.getDataEvento() == null) {
            return dataInicio == null && dataFim == null;
        }

        LocalDate dataEvento = evento.getDataEvento().atZone(ZoneId.systemDefault()).toLocalDate();
        if (dataInicio != null && dataEvento.isBefore(dataInicio)) {
            return false;
        }

        return dataFim == null || !dataEvento.isAfter(dataFim);
    }

    private EventoLinha criarLinha(EventoAuditoria evento,
                                   Map<Integer, FiltroUtilizador> colaboradoresPorId,
                                   Map<String, FiltroUtilizador> colaboradoresPorEmail) {
        FiltroUtilizador colaboradorAssociado = null;
        if (evento.getIdUtilizador() != null && evento.getIdUtilizador().getId() != null) {
            colaboradorAssociado = colaboradoresPorId.get(evento.getIdUtilizador().getId());
        }

        if (colaboradorAssociado == null) {
            colaboradorAssociado = colaboradoresPorEmail.get(normalizarEmail(evento.getEmailReferencia()));
        }

        String email = valorOuTraco(normalizarEmail(evento.getEmailReferencia()));
        String colaborador = colaboradorAssociado != null
                ? colaboradorAssociado.nome()
                : (evento.getIdUtilizador() != null ? valorOuTraco(evento.getIdUtilizador().getNome()) : email);

        return new EventoLinha(
                evento.getId(),
                evento.getDataEvento() != null
                        ? DATA_HORA_FORMATTER.format(evento.getDataEvento().atZone(ZoneId.systemDefault()))
                        : "-",
                formatarEtiqueta(evento.getTipoEvento()),
                formatarEtiqueta(evento.getResultado()),
                formatarEtiqueta(evento.getOrigem()),
                valorOuTraco(colaborador),
                email,
                valorOuTraco(evento.getIdentificadorSessao()),
                valorOuTraco(evento.getDetalhes())
        );
    }

    private boolean eventoFalhado(EventoAuditoria evento) {
        return "falha".equals(normalizar(evento.getResultado()));
    }

    private boolean eventoDeAutenticacao(EventoAuditoria evento) {
        String tipoEvento = normalizar(evento.getTipoEvento());
        String origem = normalizar(evento.getOrigem());
        return TIPOS_AUTENTICACAO.contains(tipoEvento)
                || "autenticacao".equals(origem)
                || "sessao".equals(origem);
    }

    private boolean eventoSensivel(EventoAuditoria evento) {
        String tipoEvento = normalizar(evento.getTipoEvento());
        String origem = normalizar(evento.getOrigem());

        return "alteracao_password".equals(tipoEvento)
                || (tipoEvento != null && tipoEvento.startsWith("colaborador_"))
                || ORIGENS_SENSIVEIS.contains(origem);
    }

    private String valorOuPadrao(String valor, String valorPadrao) {
        String valorLimpo = limparTexto(valor);
        return valorLimpo == null ? valorPadrao : valorLimpo;
    }

    private String normalizar(String valor) {
        if (valor == null) return null;
        return valor.toLowerCase(Locale.ROOT);
    }

    private String normalizarEmail(String emailReferencia) {
        String emailNormalizado = limparTexto(emailReferencia);
        return emailNormalizado == null ? null : emailNormalizado.toLowerCase(Locale.ROOT);
    }

    private static final java.util.Map<String, String> ETIQUETAS_PT = java.util.Map.ofEntries(
            java.util.Map.entry("login",                   "Autenticação"),
            java.util.Map.entry("logout",                  "Terminar sessão"),
            java.util.Map.entry("sessao_expirada",         "Sessão expirada"),
            java.util.Map.entry("alteracao_password",      "Alteração de password"),
            java.util.Map.entry("colaborador_criado",      "Colaborador criado"),
            java.util.Map.entry("colaborador_atualizado",  "Colaborador atualizado"),
            java.util.Map.entry("colaborador_desativado",  "Colaborador desativado"),
            java.util.Map.entry("horario_publicado",       "Horário publicado"),
            java.util.Map.entry("horario_gerado",          "Horário gerado"),
            java.util.Map.entry("proposta_enviada",        "Proposta enviada"),
            java.util.Map.entry("proposta_aprovada",       "Proposta aprovada"),
            java.util.Map.entry("proposta_rejeitada",      "Proposta rejeitada"),
            java.util.Map.entry("folga_aprovada",          "Folga aprovada"),
            java.util.Map.entry("folga_rejeitada",         "Folga rejeitada"),
            java.util.Map.entry("permuta_aprovada",        "Permuta aprovada"),
            java.util.Map.entry("permuta_rejeitada",       "Permuta rejeitada"),
            java.util.Map.entry("sucesso",                 "Sucesso"),
            java.util.Map.entry("falha",                   "Falha"),
            java.util.Map.entry("autenticacao",            "Autenticação")
    );

    private String formatarEtiqueta(String valor) {
        String valorNormalizado = normalizar(valor);
        if (valorNormalizado == null) {
            return "-";
        }

        // Usar etiqueta em português se conhecida
        String etiquetaConhecida = ETIQUETAS_PT.get(valorNormalizado);
        if (etiquetaConhecida != null) {
            return etiquetaConhecida;
        }

        // Fallback: split por underscore e capitalizar
        String[] partes = valorNormalizado.split("_");
        StringBuilder resultado = new StringBuilder();
        for (String parte : partes) {
            if (parte.isBlank()) {
                continue;
            }

            if (!resultado.isEmpty()) {
                resultado.append(' ');
            }

            resultado.append(Character.toUpperCase(parte.charAt(0)));
            if (parte.length() > 1) {
                resultado.append(parte.substring(1));
            }
        }

        return resultado.isEmpty() ? "-" : resultado.toString();
    }

    private String valorOuTraco(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
    }

    private String limparTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.isEmpty() ? null : valorNormalizado;
    }

    public record FiltroAuditoria(
            String tipoEvento,
            Integer idUtilizador,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
    }

    public record PainelAuditoriaSnapshot(
            AuditoriaContexto contexto,
            AuditoriaResumo resumo,
            List<EventoLinha> eventos
    ) {
    }

    public record AuditoriaContexto(
            Integer idLoja,
            String nomeLoja,
            String localizacao,
            String cargoGestao,
            List<FiltroUtilizador> utilizadores,
            List<FiltroTipoEvento> tiposEvento
    ) {
    }

    public record AuditoriaResumo(
            int totalEventos,
            int totalFalhas,
            int totalAutenticacoes,
            int totalAlteracoesSensiveis
    ) {
    }

    public record EventoLinha(
            Integer idEvento,
            String dataHora,
            String tipoEvento,
            String resultado,
            String origem,
            String colaborador,
            String email,
            String identificadorSessao,
            String detalhes
    ) {
    }

    public record FiltroUtilizador(
            Integer id,
            String nome,
            String email
    ) {
        @Override
        public String toString() {
            return nome;
        }
    }

    public record FiltroTipoEvento(
            String codigo,
            String etiqueta
    ) {
        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
