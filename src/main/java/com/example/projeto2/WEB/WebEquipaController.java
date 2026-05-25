package com.example.projeto2.WEB;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.BLL.PreferenciaBLL;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.UtilizadorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/web/equipa")
public class WebEquipaController {

    private static final Set<String> CARGOS_GESTAO = Set.of("gerente", "subgerente", "supervisor");

    private final WebAppService webAppService;
    private final UtilizadorRepository utilizadorRepository;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final HorarioBLL horarioBLL;
    private final DayOffBLL dayOffBLL;
    private final PreferenciaBLL preferenciaBLL;
    private final PermutaBLL permutaBLL;

    public WebEquipaController(WebAppService webAppService,
                               UtilizadorRepository utilizadorRepository,
                               LojautilizadorRepository lojautilizadorRepository,
                               HorarioBLL horarioBLL,
                               DayOffBLL dayOffBLL,
                               PreferenciaBLL preferenciaBLL,
                               PermutaBLL permutaBLL) {
        this.webAppService = webAppService;
        this.utilizadorRepository = utilizadorRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.horarioBLL = horarioBLL;
        this.dayOffBLL = dayOffBLL;
        this.preferenciaBLL = preferenciaBLL;
        this.permutaBLL = permutaBLL;
    }

    @GetMapping
    public String equipa(@RequestParam(value = "q", required = false) String pesquisa,
                         HttpSession session,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Integer idGestor = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "equipa");

        if (!podeGerir(idGestor)) {
            return webAppService.redirecionarComErro(
                    redirectAttributes,
                    "Este modulo esta disponivel apenas para perfis de gestao."
            );
        }

        List<UtilizadorResumo> utilizadores = listarUtilizadoresComContextoDaLoja(idGestor, pesquisa);
        model.addAttribute("utilizadores", utilizadores);
        model.addAttribute("pesquisa", pesquisa == null ? "" : pesquisa.trim());
        model.addAttribute("totalUtilizadores", utilizadores.size());
        model.addAttribute("temSelecao", false);
        return "web/equipa";
    }

    @GetMapping("/{idUtilizador}")
    public String detalheUtilizador(@PathVariable("idUtilizador") Integer idUtilizador,
                                    @RequestParam(value = "q", required = false) String pesquisa,
                                    @RequestParam(value = "ano", required = false) Integer ano,
                                    @RequestParam(value = "mes", required = false) Integer mes,
                                    HttpSession session,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        Integer idGestor = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "equipa");

        if (!podeGerir(idGestor)) {
            return webAppService.redirecionarComErro(
                    redirectAttributes,
                    "Este modulo esta disponivel apenas para perfis de gestao."
            );
        }

        List<UtilizadorResumo> utilizadores = listarUtilizadoresComContextoDaLoja(idGestor, pesquisa);
        model.addAttribute("utilizadores", utilizadores);
        model.addAttribute("pesquisa", pesquisa == null ? "" : pesquisa.trim());
        model.addAttribute("totalUtilizadores", utilizadores.size());

        Utilizador utilizador = utilizadorRepository.findById(idUtilizador).orElse(null);
        if (utilizador == null) {
            model.addAttribute("erro", "Nao foi possivel encontrar o utilizador selecionado.");
            model.addAttribute("temSelecao", false);
            return "web/equipa";
        }

        LocalDate hoje = LocalDate.now();
        int anoConsulta = ano == null ? hoje.getYear() : ano;
        int mesConsulta = (mes == null || mes < 1 || mes > 12) ? hoje.getMonthValue() : mes;
        YearMonth periodo = YearMonth.of(anoConsulta, mesConsulta);
        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();

        boolean colaboradorDaLoja = utilizadores.stream().anyMatch(item -> item.idUtilizador().equals(idUtilizador) && item.daMinhaLoja());
        boolean podeAprovarItens = colaboradorDaLoja && !idUtilizador.equals(idGestor);

        List<Horario> horariosPublicados = horarioBLL.listarHorarioPublicadoDoUtilizador(idUtilizador, inicio, fim);
        List<DayOff> historicoFolgas = dayOffBLL.listarPedidosPorUtilizador(idUtilizador).stream()
                .sorted(Comparator.comparing(DayOff::getDataAusencia, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .toList();
        List<Preferencia> historicoPreferencias = preferenciaBLL.listarPreferenciasPorUtilizador(idUtilizador).stream()
                .sorted(Comparator
                        .comparing(Preferencia::getDataInicio, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Preferencia::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .toList();
        List<Permuta> historicoPermutas = permutaBLL.listarPedidosEnviados(idUtilizador).stream()
                .limit(12)
                .toList();

        List<DayOff> pendentesFolga = podeAprovarItens
                ? dayOffBLL.listarPedidosPendentesParaAprovacao(idGestor).stream()
                .filter(item -> idUtilizador.equals(item.getIdUtilizador()))
                .toList()
                : List.of();
        List<Preferencia> pendentesPreferencia = podeAprovarItens
                ? preferenciaBLL.listarPreferenciasPendentesParaAprovacao(idGestor).stream()
                .filter(item -> item.getIdUtilizador() != null && idUtilizador.equals(item.getIdUtilizador().getId()))
                .toList()
                : List.of();
        List<Permuta> pendentesPermuta = podeAprovarItens
                ? permutaBLL.listarPedidosPendentesParaAprovacao(idGestor).stream()
                .filter(item -> item.getIdHorarioOrigem() != null
                        && item.getIdHorarioOrigem().getIdLojautilizador() != null
                        && item.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador() != null
                        && idUtilizador.equals(item.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getId()))
                .toList()
                : List.of();

        Optional<Lojautilizador> ligacaoAtiva = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador);

        model.addAttribute("temSelecao", true);
        model.addAttribute("utilizadorSelecionado", utilizador);
        model.addAttribute("ligacaoAtiva", ligacaoAtiva.orElse(null));
        model.addAttribute("colaboradorDaLoja", colaboradorDaLoja);
        model.addAttribute("podeAprovarItens", podeAprovarItens);
        model.addAttribute("ano", anoConsulta);
        model.addAttribute("mes", mesConsulta);
        model.addAttribute("meses", MesWebOption.todos());
        model.addAttribute("anos", MesWebOption.anosProximos(hoje.getYear(), 2));
        model.addAttribute("horariosPublicados", horariosPublicados);
        model.addAttribute("historicoFolgas", historicoFolgas);
        model.addAttribute("historicoPreferencias", historicoPreferencias);
        model.addAttribute("historicoPermutas", historicoPermutas);
        model.addAttribute("pendentesFolga", pendentesFolga);
        model.addAttribute("pendentesPreferencia", pendentesPreferencia);
        model.addAttribute("pendentesPermuta", pendentesPermuta);
        return "web/equipa";
    }

    @PostMapping("/{idUtilizador}/folgas/{idDayOff}/aprovar")
    public String aprovarFolga(@PathVariable("idUtilizador") Integer idUtilizador,
                               @PathVariable("idDayOff") Integer idDayOff,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Integer idGestor = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            dayOffBLL.aprovarPedidoFolga(idDayOff, idGestor);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido de folga aprovado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/equipa/" + idUtilizador;
    }

    @PostMapping("/{idUtilizador}/folgas/{idDayOff}/rejeitar")
    public String rejeitarFolga(@PathVariable("idUtilizador") Integer idUtilizador,
                                @PathVariable("idDayOff") Integer idDayOff,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Integer idGestor = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            dayOffBLL.rejeitarPedidoFolga(idDayOff, idGestor);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido de folga rejeitado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/equipa/" + idUtilizador;
    }

    @PostMapping("/{idUtilizador}/preferencias/{idPreferencia}/aprovar")
    public String aprovarPreferencia(@PathVariable("idUtilizador") Integer idUtilizador,
                                     @PathVariable("idPreferencia") Integer idPreferencia,
                                     @RequestParam(value = "decisao", required = false) String decisao,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Integer idGestor = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            preferenciaBLL.aprovarPreferencia(idPreferencia, idGestor, decisao);
            redirectAttributes.addFlashAttribute("sucesso", "Preferencia aprovada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/equipa/" + idUtilizador;
    }

    @PostMapping("/{idUtilizador}/preferencias/{idPreferencia}/rejeitar")
    public String rejeitarPreferencia(@PathVariable("idUtilizador") Integer idUtilizador,
                                      @PathVariable("idPreferencia") Integer idPreferencia,
                                      @RequestParam(value = "decisao", required = false) String decisao,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Integer idGestor = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            preferenciaBLL.rejeitarPreferencia(idPreferencia, idGestor, decisao);
            redirectAttributes.addFlashAttribute("sucesso", "Preferencia rejeitada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/equipa/" + idUtilizador;
    }

    @PostMapping("/{idUtilizador}/permutas/{idPermuta}/aprovar")
    public String aprovarPermuta(@PathVariable("idUtilizador") Integer idUtilizador,
                                 @PathVariable("idPermuta") Integer idPermuta,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer idGestor = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            permutaBLL.aprovarPedidoPermuta(idPermuta, idGestor);
            redirectAttributes.addFlashAttribute("sucesso", "Permuta aprovada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/equipa/" + idUtilizador;
    }

    @PostMapping("/{idUtilizador}/permutas/{idPermuta}/rejeitar")
    public String rejeitarPermuta(@PathVariable("idUtilizador") Integer idUtilizador,
                                  @PathVariable("idPermuta") Integer idPermuta,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Integer idGestor = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            permutaBLL.rejeitarPedidoPermuta(idPermuta, idGestor);
            redirectAttributes.addFlashAttribute("sucesso", "Permuta rejeitada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/equipa/" + idUtilizador;
    }

    private boolean podeGerir(Integer idUtilizador) {
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(item -> item.getIdCargo() != null ? item.getIdCargo().getTipo() : null)
                .map(tipo -> tipo != null && CARGOS_GESTAO.contains(tipo.toLowerCase(Locale.ROOT)))
                .orElse(false);
    }

    private List<UtilizadorResumo> listarUtilizadoresComContextoDaLoja(Integer idGestor, String pesquisa) {
        Integer idLojaGestor = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idGestor)
                .map(item -> item.getIdLoja() != null ? item.getIdLoja().getId() : null)
                .orElse(null);

        String filtro = pesquisa == null ? "" : pesquisa.trim().toLowerCase(Locale.ROOT);

        return utilizadorRepository.findAll().stream()
                .filter(item -> correspondePesquisa(item, filtro))
                .map(item -> {
                    Optional<Lojautilizador> ligacao = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(item.getId());
                    boolean daMinhaLoja = ligacao
                            .map(value -> value.getIdLoja() != null && value.getIdLoja().getId() != null
                                    && value.getIdLoja().getId().equals(idLojaGestor))
                            .orElse(false);
                    String cargoAtual = ligacao
                            .map(value -> value.getIdCargo() != null ? value.getIdCargo().getNome() : null)
                            .orElse(null);

                    return new UtilizadorResumo(
                            item.getId(),
                            item.getNome(),
                            item.getEmail(),
                            item.getEstado(),
                            cargoAtual,
                            daMinhaLoja
                    );
                })
                .sorted(Comparator.comparing((UtilizadorResumo item) -> item.nome() == null ? "" : item.nome().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private boolean correspondePesquisa(Utilizador utilizador, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return true;
        }
        String nome = utilizador.getNome() == null ? "" : utilizador.getNome().toLowerCase(Locale.ROOT);
        String email = utilizador.getEmail() == null ? "" : utilizador.getEmail().toLowerCase(Locale.ROOT);
        String telemovel = utilizador.getTelemovel() == null ? "" : utilizador.getTelemovel().toLowerCase(Locale.ROOT);
        return nome.contains(filtro) || email.contains(filtro) || telemovel.contains(filtro);
    }

    public record UtilizadorResumo(
            Integer idUtilizador,
            String nome,
            String email,
            String estado,
            String cargoAtual,
            boolean daMinhaLoja
    ) {
    }
}
