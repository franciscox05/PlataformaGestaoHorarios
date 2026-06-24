package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.API.Services.PermutaService;
import com.example.projeto2.API.Services.PermutaFolgaService;
import com.example.projeto2.API.Services.PreferenciaService;
import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.PermutaFolga;
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
import java.util.Set;

@Controller
@RequestMapping("/web/complementares")
public class WebComplementaresController {

    private static final List<String> TIPOS_PREFERENCIA =
            List.of("folga_preferida", "colegas", "turnos");
    private static final Set<String> TIPOS_PREFERENCIA_VALIDOS = Set.copyOf(TIPOS_PREFERENCIA);

    private final WebAppService webAppService;
    private final DayOffService dayOffBLL;
    private final PreferenciaService preferenciaBLL;
    private final PermutaService permutaBLL;
    private final PermutaFolgaService permutaFolgaBLL;
    private final HorarioService horarioBLL;

    public WebComplementaresController(WebAppService webAppService,
                                       DayOffService dayOffBLL,
                                       PreferenciaService preferenciaBLL,
                                       PermutaService permutaBLL,
                                       PermutaFolgaService permutaFolgaBLL,
                                       HorarioService horarioBLL) {
        this.webAppService = webAppService;
        this.dayOffBLL = dayOffBLL;
        this.preferenciaBLL = preferenciaBLL;
        this.permutaBLL = permutaBLL;
        this.permutaFolgaBLL = permutaFolgaBLL;
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
        List<PermutaFolga> minhasPermutasFolga = permutaFolgaBLL.listarPedidosPorUtilizador(utilizadorId);

        List<Horario> meusTurnosPermutaveis = horarioBLL.listarMeusTurnosDisponiveisParaPermuta(utilizadorId);
        List<Horario> turnosElegiveis = idHorarioOrigem != null
                ? horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, idHorarioOrigem)
                : List.of();

        model.addAttribute("minhasFolgas", minhasFolgas);
        model.addAttribute("minhasPreferencias", minhasPreferencias);
        model.addAttribute("minhasPermutas", minhasPermutas);
        model.addAttribute("minhasPermutasFolga", minhasPermutasFolga);

        model.addAttribute("tiposPreferencia", TIPOS_PREFERENCIA);
        // Limite inferior para os date-pickers — impede a escolha de datas passadas no browser.
        model.addAttribute("hoje", LocalDate.now());
        Integer idLoja = webAppService.obterLojaAtual(session);
        try {
            model.addAttribute("colegasDaLoja", preferenciaBLL.listarColegasDaLoja(utilizadorId, idLoja));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("colegasDaLoja", List.of());
        }
        model.addAttribute("meusTurnosPermutaveis", meusTurnosPermutaveis);
        model.addAttribute("turnosElegiveis", turnosElegiveis);
        model.addAttribute("origemPermutaSelecionada", idHorarioOrigem);
        return "web/complementares";
    }

    @PostMapping("/folgas")
    public String registarFolga(@RequestParam(value = "dataAusencia", required = false) String dataAusencia,
                                @RequestParam(value = "dataFim", required = false) String dataFim,
                                @RequestParam(value = "tipo", required = false) String tipo,
                                @RequestParam(value = "motivo", required = false) String motivo,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);

        try {
            if (dataAusencia == null || dataAusencia.isBlank()) {
                throw new IllegalArgumentException("A data de ausência é obrigatória.");
            }
            if (motivo != null && motivo.strip().length() > 500) {
                throw new IllegalArgumentException("O motivo não pode ter mais de 500 caracteres.");
            }
            String tipoNorm = normalizarTipoFolga(tipo);
            Utilizador utilizadorProxy = new Utilizador();
            utilizadorProxy.setId(utilizadorId);
            String motivoLimpo = motivo != null && !motivo.isBlank() ? motivo.strip() : null;

            // Férias = intervalo de datas (paridade com o Desktop: registarPedidoFeriasIntervalo
            // cria uma ausência por cada dia do período). Folgas/Baixa = dia isolado.
            if ("ferias".equals(tipoNorm) && dataFim != null && !dataFim.isBlank()) {
                java.time.LocalDate inicio = parseData(dataAusencia, "inicio das ferias");
                java.time.LocalDate fim = parseData(dataFim, "fim das ferias");
                dayOffBLL.registarPedidoFeriasIntervalo(utilizadorProxy, inicio, fim, motivoLimpo);
                redirectAttributes.addFlashAttribute("sucesso", "Pedido de férias submetido com sucesso.");
                return "redirect:/web/complementares?tab=folgas";
            }

            DayOff pedido = new DayOff();
            pedido.setIdUtilizador(utilizadorProxy);
            pedido.setDataAusencia(parseData(dataAusencia, "ausencia"));
            pedido.setTipo(tipoNorm);
            pedido.setMotivo(motivoLimpo);
            dayOffBLL.registarPedidoFolga(pedido);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido de folga submetido com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(WebComplementaresController.class)
                    .error("Erro inesperado ao registar pedido de folga", ex);
            redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro inesperado. Tenta novamente.");
        }
        return "redirect:/web/complementares?tab=folgas";
    }

    @PostMapping("/preferencias")
    public String registarPreferencia(@RequestParam(value = "tipo", required = false) String tipo,
                                      @RequestParam(value = "dataInicio", required = false) String dataInicio,
                                      @RequestParam(value = "dataFim", required = false) String dataFim,
                                      @RequestParam(value = "diaSemana", required = false) String diaSemana,
                                      @RequestParam(value = "descricao", required = false) String descricao,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
            String tipoNorm = normalizarTipoPreferencia(tipo);
            java.time.LocalDate dataInicioDate;
            if ("folga_preferida".equals(tipoNorm)) {
                if (diaSemana == null || diaSemana.isBlank()) {
                    throw new IllegalArgumentException("Seleciona o dia da semana preferido para a folga.");
                }
                java.time.DayOfWeek dia;
                try {
                    dia = java.time.DayOfWeek.valueOf(diaSemana.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Dia da semana inválido.");
                }
                dataInicioDate = java.time.LocalDate.now()
                        .with(java.time.temporal.TemporalAdjusters.nextOrSame(dia));
            } else {
                dataInicioDate = parseDataOpcional(dataInicio);
            }
            Preferencia preferencia = new Preferencia();
            preferencia.setTipo(tipoNorm);
            preferencia.setDataInicio(dataInicioDate);
            preferencia.setDataFim(parseDataOpcional(dataFim));
            preferencia.setDescricao(descricao);
            preferenciaBLL.guardarPreferencia(utilizadorId, preferencia);
            redirectAttributes.addFlashAttribute("sucesso", "Preferência guardada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(WebComplementaresController.class)
                    .error("Erro inesperado ao guardar preferencia", ex);
            redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro inesperado. Tenta novamente.");
        }
        return "redirect:/web/complementares";
    }

    @PostMapping("/permutas")
    public String registarPermuta(@RequestParam(value = "idHorarioOrigem", required = false) Integer idHorarioOrigem,
                                  @RequestParam(value = "idHorarioDestino", required = false) Integer idHorarioDestino,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);

        try {
            if (idHorarioOrigem == null || idHorarioDestino == null) {
                throw new IllegalArgumentException("Seleciona os turnos de origem e destino para a permuta.");
            }
            List<Horario> meusTurnos = horarioBLL.listarMeusTurnosDisponiveisParaPermuta(utilizadorId);
            Horario turnoOrigem = meusTurnos.stream()
                    .filter(item -> item.getId() != null && item.getId().equals(idHorarioOrigem))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("O turno de origem selecionado não é válido."));

            List<Horario> turnosElegiveis = horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, idHorarioOrigem);
            Horario turnoDestino = turnosElegiveis.stream()
                    .filter(item -> item.getId() != null && item.getId().equals(idHorarioDestino))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("O turno de destino selecionado não é elegível para permuta."));

            permutaBLL.registarPedidoTroca(utilizadorId, turnoOrigem, turnoDestino);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido de permuta submetido com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares" + (idHorarioOrigem != null ? "?origemPermuta=" + idHorarioOrigem : "");
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

    @PostMapping("/preferencias/{idPreferencia}/remover")
    public String removerPreferencia(@PathVariable("idPreferencia") Integer idPreferencia,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            preferenciaBLL.removerPreferencia(utilizadorId, idPreferencia);
            redirectAttributes.addFlashAttribute("sucesso", "Preferência removida com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/complementares";
    }

    private LocalDate parseData(String valor, String nomeCampo) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("A data de " + nomeCampo + " é inválida.");
        }
    }

    private LocalDate parseDataOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Uma das datas indicadas para a preferência é inválida.");
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
            throw new IllegalArgumentException("Seleciona um tipo de preferência.");
        }

        String normalizado = tipo.trim().toLowerCase(Locale.ROOT);
        if (!TIPOS_PREFERENCIA_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("O tipo de preferência selecionado é inválido.");
        }
        return normalizado;
    }
}
