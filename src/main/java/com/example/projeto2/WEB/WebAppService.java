package com.example.projeto2.WEB;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.BLL.GestaoLojaBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.BLL.PreferenciaBLL;
import com.example.projeto2.BLL.RelatorioHorasBLL;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.UtilizadorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Service
public class WebAppService {

    private final UtilizadorRepository utilizadorRepository;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final GeracaoHorariosBLL geracaoHorariosBLL;
    private final GestaoLojaBLL gestaoLojaBLL;
    private final RelatorioHorasBLL relatorioHorasBLL;
    private final DayOffBLL dayOffBLL;
    private final PreferenciaBLL preferenciaBLL;
    private final PermutaBLL permutaBLL;

    public WebAppService(UtilizadorRepository utilizadorRepository,
                         LojautilizadorRepository lojautilizadorRepository,
                         GeracaoHorariosBLL geracaoHorariosBLL,
                         GestaoLojaBLL gestaoLojaBLL,
                         RelatorioHorasBLL relatorioHorasBLL,
                         DayOffBLL dayOffBLL,
                         PreferenciaBLL preferenciaBLL,
                         PermutaBLL permutaBLL) {
        this.utilizadorRepository = utilizadorRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.geracaoHorariosBLL = geracaoHorariosBLL;
        this.gestaoLojaBLL = gestaoLojaBLL;
        this.relatorioHorasBLL = relatorioHorasBLL;
        this.dayOffBLL = dayOffBLL;
        this.preferenciaBLL = preferenciaBLL;
        this.permutaBLL = permutaBLL;
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
            throw new IllegalArgumentException("A sessao Web expirou. Inicia sessao novamente.");
        }
        return idUtilizador;
    }

    public Utilizador obterUtilizadorAutenticado(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
        }

        return utilizadorRepository.findById(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel encontrar o utilizador autenticado."));
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

    public WebPermissoes obterPermissoes(Integer idUtilizador) {
        if (idUtilizador == null) {
            return WebPermissoes.semAcesso();
        }

        return new WebPermissoes(
                true,
                gestaoLojaBLL.utilizadorPodeGerirLoja(idUtilizador),
                relatorioHorasBLL.utilizadorPodeConsultarRelatorios(idUtilizador),
                true,
                dayOffBLL.utilizadorPodeAprovarFolgas(idUtilizador),
                preferenciaBLL.utilizadorPodeAprovarPreferencias(idUtilizador),
                permutaBLL.utilizadorPodeAprovarPermutas(idUtilizador),
                geracaoHorariosBLL.utilizadorPodeValidarHorarios(idUtilizador)
        );
    }

    public String obterCargoAtual(Integer idUtilizador) {
        Optional<Lojautilizador> ligacaoAtiva = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador);
        if (ligacaoAtiva.isEmpty() || ligacaoAtiva.get().getIdCargo() == null) {
            return "-";
        }

        String nomeCargo = ligacaoAtiva.get().getIdCargo().getNome();
        return nomeCargo == null || nomeCargo.isBlank() ? "-" : nomeCargo;
    }

    public void preencherModeloBase(Model model, HttpSession session, String moduloAtivo) {
        Integer idUtilizador = obterUtilizadorIdObrigatorio(session);
        Utilizador utilizador = obterUtilizadorAutenticado(idUtilizador);
        WebPermissoes permissoes = obterPermissoes(idUtilizador);

        model.addAttribute("webUtilizador", utilizador);
        model.addAttribute("webCargoAtual", obterCargoAtual(idUtilizador));
        model.addAttribute("webPermissoes", permissoes);
        model.addAttribute("webModuloAtivo", moduloAtivo);
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
            boolean podeValidarHorarios
    ) {
        static WebPermissoes semAcesso() {
            return new WebPermissoes(false, false, false, false, false, false, false, false);
        }

        public boolean podeAprovarAlgumComplementar() {
            return podeAprovarFolgas || podeAprovarPreferencias || podeAprovarPermutas;
        }
    }
}
