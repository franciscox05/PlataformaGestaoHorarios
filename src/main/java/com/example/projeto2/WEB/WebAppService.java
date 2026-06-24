package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.GeracaoHorariosService;
import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.API.Services.NotificacaoService;
import com.example.projeto2.API.Services.PermutaService;
import com.example.projeto2.API.Services.PreferenciaService;
import com.example.projeto2.API.Services.RelatorioHorasService;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Repositories.UtilizadorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Service
public class WebAppService {

    private final UtilizadorRepository utilizadorRepository;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final GeracaoHorariosService geracaoHorariosBLL;
    private final GestaoLojaService gestaoLojaBLL;
    private final RelatorioHorasService relatorioHorasBLL;
    private final DayOffService dayOffBLL;
    private final PreferenciaService preferenciaBLL;
    private final PermutaService permutaBLL;
    private final NotificacaoService notificacaoBLL;

    public WebAppService(UtilizadorRepository utilizadorRepository,
                         LojautilizadorRepository lojautilizadorRepository,
                         GeracaoHorariosService geracaoHorariosBLL,
                         GestaoLojaService gestaoLojaBLL,
                         RelatorioHorasService relatorioHorasBLL,
                         DayOffService dayOffBLL,
                         PreferenciaService preferenciaBLL,
                         PermutaService permutaBLL,
                         NotificacaoService notificacaoBLL) {
        this.utilizadorRepository = utilizadorRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.geracaoHorariosBLL = geracaoHorariosBLL;
        this.gestaoLojaBLL = gestaoLojaBLL;
        this.relatorioHorasBLL = relatorioHorasBLL;
        this.dayOffBLL = dayOffBLL;
        this.preferenciaBLL = preferenciaBLL;
        this.permutaBLL = permutaBLL;
        this.notificacaoBLL = notificacaoBLL;
    }

    public Integer obterUtilizadorId(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
    }

    public Integer obterUtilizadorIdObrigatorio(HttpSession session) {
        Integer idUtilizador = obterUtilizadorId(session);
        if (idUtilizador == null) {
            throw new IllegalArgumentException("A sessão Web expirou. Inicia sessão novamente.");
        }
        return idUtilizador;
    }

    public Utilizador obterUtilizadorAutenticado(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
        }

        return utilizadorRepository.findById(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Não foi possível encontrar o utilizador autenticado."));
    }

    public Utilizador obterUtilizadorAutenticado(HttpSession session) {
        return obterUtilizadorAutenticado(obterUtilizadorIdObrigatorio(session));
    }

    public void sincronizarSessao(HttpSession session, Utilizador utilizador) {
        if (session == null || utilizador == null) {
            return;
        }
        session.setAttribute(WebSession.UTILIZADOR_ID, utilizador.getId());
        session.setAttribute(WebSession.UTILIZADOR_NOME, utilizador.getNome());
        session.setAttribute(WebSession.UTILIZADOR_EMAIL, utilizador.getEmail());
    }

    /** Devolve o ID da loja actualmente activa na sessão, ou {@code null} se não estiver definido. */
    public Integer obterLojaAtual(HttpSession session) {
        if (session == null) return null;
        return (Integer) session.getAttribute(WebSession.LOJA_ID);
    }

    /**
     * Devolve o ID da loja activa na sessão. Lança {@link IllegalStateException} se não estiver
     * definido — útil como guarda em controllers que exigem contexto de loja explícito.
     */
    public Integer obterLojaAtualObrigatoria(HttpSession session) {
        Integer idLoja = obterLojaAtual(session);
        if (idLoja == null) {
            throw new IllegalStateException("Contexto de loja não definido na sessão.");
        }
        return idLoja;
    }

    /** Store-scoped — eliminates NonUniqueResultException for multi-store users. */
    public WebPermissoes obterPermissoes(Integer idUtilizador, Integer idLoja) {
        if (idUtilizador == null) return WebPermissoes.semAcesso();
        return new WebPermissoes(
                true,
                gestaoLojaBLL.utilizadorPodeGerirLoja(idUtilizador, idLoja),
                relatorioHorasBLL.utilizadorPodeConsultarRelatorios(idUtilizador, idLoja),
                true,
                dayOffBLL.utilizadorPodeAprovarFolgas(idUtilizador, idLoja),
                preferenciaBLL.utilizadorPodeAprovarPreferencias(idUtilizador, idLoja),
                permutaBLL.utilizadorPodeAprovarPermutas(idUtilizador, idLoja),
                geracaoHorariosBLL.utilizadorPodeValidarHorarios(idUtilizador, idLoja),
                geracaoHorariosBLL.utilizadorPodeGerarHorarios(idUtilizador, idLoja)
        );
    }

    /** Backward-compatible 1-arg overload (Desktop / no-store-context callers). */
    public WebPermissoes obterPermissoes(Integer idUtilizador) {
        return obterPermissoes(idUtilizador, null);
    }

    public String obterCargoAtual(Integer idUtilizador) {
        Optional<Lojautilizador> ligacaoAtiva = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador);
        if (ligacaoAtiva.isEmpty() || ligacaoAtiva.get().getIdCargo() == null) {
            return "-";
        }

        String nomeCargo = ligacaoAtiva.get().getIdCargo().getNome();
        return nomeCargo == null || nomeCargo.isBlank() ? "-" : nomeCargo;
    }

    /** Store-scoped variant — resolves the role displayed in the sidebar for the active store. */
    public String obterCargoAtual(Integer idUtilizador, Integer idLoja) {
        if (idLoja == null) return obterCargoAtual(idUtilizador);
        Optional<Lojautilizador> ligacaoAtiva =
                lojautilizadorRepository.findLigacaoAtivaByIdUtilizadorAndIdLoja(idUtilizador, idLoja);
        if (ligacaoAtiva.isEmpty() || ligacaoAtiva.get().getIdCargo() == null) {
            return "-";
        }
        String nomeCargo = ligacaoAtiva.get().getIdCargo().getNome();
        return nomeCargo == null || nomeCargo.isBlank() ? "-" : nomeCargo;
    }

    /** Resolves the display name of the active store for the navbar indicator, or {@code null}. */
    public String obterNomeLojaAtual(Integer idUtilizador, Integer idLoja) {
        if (idLoja == null) return null;
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizadorAndIdLoja(idUtilizador, idLoja)
                .map(Lojautilizador::getIdLoja)
                .map(loja -> loja != null ? loja.getNome() : null)
                .filter(nome -> nome != null && !nome.isBlank())
                .orElse(null);
    }

    public void preencherModeloBase(Model model, HttpSession session, String moduloAtivo) {
        Integer idUtilizador = obterUtilizadorIdObrigatorio(session);
        Integer idLoja = obterLojaAtual(session);
        Utilizador utilizador = obterUtilizadorAutenticado(idUtilizador);
        WebPermissoes permissoes = obterPermissoes(idUtilizador, idLoja);

        long contagemLojas = lojautilizadorRepository.countByIdUtilizadorIdAndDataFimIsNull(idUtilizador);
        model.addAttribute("webUtilizador", utilizador);
        model.addAttribute("webCargoAtual", obterCargoAtual(idUtilizador, idLoja));
        model.addAttribute("webPermissoes", permissoes);
        model.addAttribute("webModuloAtivo", moduloAtivo);
        model.addAttribute("webLoja", obterNomeLojaAtual(idUtilizador, idLoja));
        model.addAttribute("podeAlternarLoja", contagemLojas >= 2);
        model.addAttribute("totalFolgasPendentes", 0);
        model.addAttribute("totalPreferenciasPendentes", 0);
        model.addAttribute("totalPermutasPendentes", 0);
        model.addAttribute("totalPendenciasComplementares", 0);
        model.addAttribute("totalNotificacoesPendentes",
                notificacaoBLL.contarNotificacoesPendentes(idUtilizador, idLoja));
    }

    public String redirecionarComErro(RedirectAttributes redirectAttributes, String mensagem) {
        if (redirectAttributes != null) {
            redirectAttributes.addFlashAttribute("erro", mensagem);
        }
        return "redirect:/web/painel";
    }

    public record WebPermissoes(
            boolean podeVerHorarios,
            boolean podeGerirLoja,
            boolean podeVerRelatorios,
            boolean podeVerComplementares,
            boolean podeAprovarFolgas,
            boolean podeAprovarPreferencias,
            boolean podeAprovarPermutas,
            boolean podeValidarHorarios,
            boolean podeGerarHorarios
    ) {
        static WebPermissoes semAcesso() {
            return new WebPermissoes(false, false, false, false, false, false, false, false, false);
        }

        public boolean podeAprovarAlgumComplementar() {
            return podeAprovarFolgas || podeAprovarPreferencias || podeAprovarPermutas;
        }
    }
}
