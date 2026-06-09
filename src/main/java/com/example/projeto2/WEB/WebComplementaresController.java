package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.API.Services.PermutaService;
import com.example.projeto2.API.Services.PreferenciaService;
import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.Preferencia;
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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web/complementares")
public class WebComplementaresController {

    private final WebAppService webAppService;
    private final DayOffService dayOffBLL;
    private final PreferenciaService preferenciaBLL;
    private final PermutaService permutaBLL;
    private final HorarioService horarioBLL;

    public WebComplementaresController(WebAppService webAppService,
                                       DayOffService dayOffBLL,
                                       PreferenciaService preferenciaBLL,
                                       PermutaService permutaBLL,
                                       HorarioService horarioBLL) {
        this.webAppService = webAppService;
        this.dayOffBLL = dayOffBLL;
        this.preferenciaBLL = preferenciaBLL;
        this.permutaBLL = permutaBLL;
        this.horarioBLL = horarioBLL;
    }

    @GetMapping
    public String pagina(@RequestParam(value = "origemPermuta", required = false) Integer idHorarioOrigem,
                         HttpSession session,
                         Model model) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "complementares");

        List<DayOff> minhasFolgas = dayOffBLL.listarPedidosPorUtilizador(utilizadorId);
        List<Preferencia> minhasPreferencias = preferenciaBLL.listarPreferenciasPorUtilizador(utilizadorId);
        List<Permuta> minhasPermutas = permutaBLL.listarPedidosEnviados(utilizadorId);

        WebAppService.WebPermissoes permissoes = webAppService.obterPermissoes(utilizadorId);
        List<DayOff> folgasPendentes = permissoes.podeAprovarFolgas()
                ? dayOffBLL.listarPedidosPendentesParaAprovacao(utilizadorId)
                : List.of();
        List<Preferencia> preferenciasPendentes = permissoes.podeAprovarPreferencias()
                ? preferenciaBLL.listarPreferenciasPendentesParaAprovacao(utilizadorId)
                : List.of();
        List<Permuta> permutasPendentes = permissoes.podeAprovarPermutas()
                ? permutaBLL.listarPedidosPendentesParaAprovacao(utilizadorId)
                : List.of();

        Map<Integer, String> nomesFolgasPendentes = dayOffBLL.listarNomesUtilizadores(
                folgasPendentes.stream().map(d -> d.getIdUtilizador().getId()).collect(Collectors.toSet())
        );

        Set<Integer> idsPreferencias = preferenciasPendentes.stream()
                .map(Preferencia::getIdUtilizador)
                .filter(item -> item != null && item.getId() != null)
                .map(item -> item.getId())
                .collect(Collectors.toSet());
        idsPreferencias.addAll(preferenciaBLL.listarHistoricoDecisoesDaLoja(utilizadorId).stream()
                .map(Preferencia::getIdUtilizador)
                .filter(item -> item != null && item.getId() != null)
                .map(item -> item.getId())
                .collect(Collectors.toSet()));
        Map<Integer, String> nomesPreferencias = dayOffBLL.listarNomesUtilizadores(idsPreferencias);

        List<Horario> meusTurnosPermutaveis = horarioBLL.listarMeusTurnosDisponiveisParaPermuta(utilizadorId);
        List<Horario> turnosElegiveis = idHorarioOrigem != null
                ? horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, idHorarioOrigem)
                : List.of();

        model.addAttribute("minhasFolgas", minhasFolgas);
        model.addAttribute("minhasPreferencias", minhasPreferencias);
        model.addAttribute("minhasPermutas", minhasPermutas);

        model.addAttribute("folgasPendentes", folgasPendentes);
        model.addAttribute("preferenciasPendentes", preferenciasPendentes);
        model.addAttribute("permutasPendentes", permutasPendentes);
        model.addAttribute("totalFolgasPendentes", folgasPendentes.size());
        model.addAttribute("totalPreferenciasPendentes", preferenciasPendentes.size());
        model.addAttribute("totalPermutasPendentes", permutasPendentes.size());
        model.addAttribute("totalPendenciasComplementares",
                folgasPendentes.size() + preferenciasPendentes.size() + permutasPendentes.size());
        model.addAttribute("nomesFolgasPendentes", nomesFolgasPendentes);
        model.addAttribute("nomesPreferencias", nomesPreferencias);

        model.addAttribute("tiposPreferencia", List.of("folgas", "ferias", "colegas", "turnos"));
        model.addAttribute("meusTurnosPermutaveis", meusTurnosPermutaveis);
        model.addAttribute("turnosElegiveis", turnosElegiveis);
        model.addAttribute("origemPermutaSelecionada", idHorarioOrigem);
        return "web/complementares";
    }

    @PostMapping("/folgas")
    public String registarFolga(@RequestParam("dataAusencia") String dataAusencia,
                                @RequestParam("tipo") String tipo,
                                @RequestParam(value = "motivo", required = false) String motivo,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);

        try {
            DayOff pedido = new DayOff();
            Utilizador utilizadorProxy = new Utilizador();
            utilizadorProxy.setId(utilizadorId);
            pedido.setIdUtilizador(utilizadorProxy);
            pedido.setDataAusencia(parseData(dataAusencia, "ausencia"));
            pedido.setTipo(normalizarTipoFolga(tipo));
            pedido.setMotivo(motivo);
            dayOffBLL.registarPedidoFolga(pedido);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido de folga submetido com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    @PostMapping("/folgas/{idDayOff}/aprovar")
    public String aprovarFolga(@PathVariable("idDayOff") Integer idDayOff,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            dayOffBLL.aprovarPedidoFolga(idDayOff, utilizadorId);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido de folga aprovado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    @PostMapping("/folgas/{idDayOff}/rejeitar")
    public String rejeitarFolga(@PathVariable("idDayOff") Integer idDayOff,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            dayOffBLL.rejeitarPedidoFolga(idDayOff, utilizadorId);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido de folga rejeitado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    @PostMapping("/preferencias")
    public String registarPreferencia(@RequestParam("tipo") String tipo,
                                      @RequestParam(value = "dataInicio", required = false) String dataInicio,
                                      @RequestParam(value = "dataFim", required = false) String dataFim,
                                      @RequestParam(value = "prioridade", required = false) Integer prioridade,
                                      @RequestParam("descricao") String descricao,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);

        try {
            Preferencia preferencia = new Preferencia();
            preferencia.setTipo(normalizarTipoPreferencia(tipo));
            preferencia.setDataInicio(parseDataOpcional(dataInicio));
            preferencia.setDataFim(parseDataOpcional(dataFim));
            preferencia.setPrioridade(prioridade);
            preferencia.setDescricao(descricao);
            preferenciaBLL.guardarPreferencia(utilizadorId, preferencia);
            redirectAttributes.addFlashAttribute("sucesso", "Preferencia guardada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    @PostMapping("/preferencias/{idPreferencia}/aprovar")
    public String aprovarPreferencia(@PathVariable("idPreferencia") Integer idPreferencia,
                                     @RequestParam(value = "decisao", required = false) String decisao,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            preferenciaBLL.aprovarPreferencia(idPreferencia, utilizadorId, decisao);
            redirectAttributes.addFlashAttribute("sucesso", "Preferencia aprovada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    @PostMapping("/preferencias/{idPreferencia}/rejeitar")
    public String rejeitarPreferencia(@PathVariable("idPreferencia") Integer idPreferencia,
                                      @RequestParam(value = "decisao", required = false) String decisao,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            preferenciaBLL.rejeitarPreferencia(idPreferencia, utilizadorId, decisao);
            redirectAttributes.addFlashAttribute("sucesso", "Preferencia rejeitada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    @PostMapping("/permutas")
    public String registarPermuta(@RequestParam("idHorarioOrigem") Integer idHorarioOrigem,
                                  @RequestParam("idHorarioDestino") Integer idHorarioDestino,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);

        List<Horario> meusTurnos = horarioBLL.listarMeusTurnosDisponiveisParaPermuta(utilizadorId);
        Horario turnoOrigem = meusTurnos.stream()
                .filter(item -> item.getId() != null && item.getId().equals(idHorarioOrigem))
                .findFirst()
                .orElse(null);

        List<Horario> turnosElegiveis = idHorarioOrigem == null
                ? List.of()
                : horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, idHorarioOrigem);
        Horario turnoDestino = turnosElegiveis.stream()
                .filter(item -> item.getId() != null && item.getId().equals(idHorarioDestino))
                .findFirst()
                .orElse(null);

        try {
            permutaBLL.registarPedidoTroca(utilizadorId, turnoOrigem, turnoDestino);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido de permuta submetido com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares?origemPermuta=" + idHorarioOrigem;
    }

    @PostMapping("/permutas/{idPermuta}/aprovar")
    public String aprovarPermuta(@PathVariable("idPermuta") Integer idPermuta,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            permutaBLL.aprovarPedidoPermuta(idPermuta, utilizadorId);
            redirectAttributes.addFlashAttribute("sucesso", "Permuta aprovada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    @PostMapping("/permutas/{idPermuta}/rejeitar")
    public String rejeitarPermuta(@PathVariable("idPermuta") Integer idPermuta,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            permutaBLL.rejeitarPedidoPermuta(idPermuta, utilizadorId);
            redirectAttributes.addFlashAttribute("sucesso", "Permuta rejeitada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    @PostMapping("/preferencias/{idPreferencia}/remover")
    public String removerPreferencia(@PathVariable("idPreferencia") Integer idPreferencia,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            preferenciaBLL.removerPreferencia(utilizadorId, idPreferencia);
            redirectAttributes.addFlashAttribute("sucesso", "Preferencia removida com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    private LocalDate parseData(String valor, String nomeCampo) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("A data de " + nomeCampo + " e invalida.");
        }
    }

    private LocalDate parseDataOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Uma das datas indicadas para a preferencia e invalida.");
        }
    }

    private String normalizarTipoFolga(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "folgas";
        }

        String normalizado = tipo.trim().toLowerCase(Locale.ROOT);
        return switch (normalizado) {
            case "ferias", "folgas", "baixa" -> normalizado;
            default -> "folgas";
        };
    }

    private String normalizarTipoPreferencia(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Seleciona um tipo de preferencia.");
        }

        String normalizado = tipo.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("folgas", "ferias", "colegas", "turnos").contains(normalizado)) {
            throw new IllegalArgumentException("O tipo de preferencia selecionado e invalido.");
        }
        return normalizado;
    }
}
