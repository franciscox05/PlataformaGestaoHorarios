package com.example.projeto2.WEB;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.BLL.PreferenciaBLL;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Preferencia;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/web/complementares")
public class WebComplementaresController {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PreferenciaBLL preferenciaBLL;
    private final DayOffBLL dayOffBLL;
    private final PermutaBLL permutaBLL;
    private final HorarioBLL horarioBLL;

    public WebComplementaresController(PreferenciaBLL preferenciaBLL,
                                       DayOffBLL dayOffBLL,
                                       PermutaBLL permutaBLL,
                                       HorarioBLL horarioBLL) {
        this.preferenciaBLL = preferenciaBLL;
        this.dayOffBLL = dayOffBLL;
        this.permutaBLL = permutaBLL;
        this.horarioBLL = horarioBLL;
    }

    @GetMapping
    public String pagina(@RequestParam(value = "origemId", required = false) Integer origemId,
                         HttpSession session,
                         Model model) {
        Integer utilizadorId = (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
        if (utilizadorId == null) {
            return "redirect:/web/login";
        }

        model.addAttribute("utilizadorNome", session.getAttribute(WebSession.UTILIZADOR_NOME));
        model.addAttribute("preferencias", preferenciaBLL.listarPreferenciasPorUtilizador(utilizadorId));
        model.addAttribute("pedidosFolga", dayOffBLL.listarPedidosPorUtilizador(utilizadorId));
        model.addAttribute("pedidosPermuta", permutaBLL.listarPedidosEnviados(utilizadorId));

        List<Horario> meusTurnos = horarioBLL.listarMeusTurnosDisponiveisParaPermuta(utilizadorId);
        model.addAttribute("meusTurnos", meusTurnos);
        model.addAttribute("origemSelecionadaId", origemId);
        model.addAttribute("turnosElegiveis", listarTurnosElegiveis(utilizadorId, origemId));

        return "web/complementares";
    }

    @PostMapping("/preferencias")
    public String criarPreferencia(@RequestParam("tipo") String tipo,
                                   @RequestParam("descricao") String descricao,
                                   @RequestParam(value = "dataInicio", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                                   @RequestParam(value = "dataFim", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Integer utilizadorId = (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
        if (utilizadorId == null) {
            return "redirect:/web/login";
        }

        try {
            Preferencia preferencia = new Preferencia();
            preferencia.setTipo(tipo);
            preferencia.setDescricao(descricao);
            preferencia.setDataInicio(dataInicio);
            preferencia.setDataFim(dataFim);
            preferencia.setPrioridade(null);
            preferenciaBLL.guardarPreferencia(utilizadorId, preferencia);
            redirectAttributes.addFlashAttribute("sucessoComplementares", "Preferencia registada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erroComplementares", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erroComplementares", "Nao foi possivel registar a preferencia.");
        }

        return "redirect:/web/complementares";
    }

    @PostMapping("/folgas")
    public String criarFolga(@RequestParam("tipo") String tipo,
                             @RequestParam("dataAusencia")
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAusencia,
                             @RequestParam(value = "motivo", required = false) String motivo,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Integer utilizadorId = (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
        if (utilizadorId == null) {
            return "redirect:/web/login";
        }

        try {
            DayOff pedido = new DayOff();
            pedido.setIdUtilizador(utilizadorId);
            pedido.setTipo(tipo);
            pedido.setDataAusencia(dataAusencia);
            pedido.setMotivo(motivo);
            dayOffBLL.registarPedidoFolga(pedido);
            redirectAttributes.addFlashAttribute("sucessoComplementares", "Pedido de folga registado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erroComplementares", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erroComplementares", "Nao foi possivel registar o pedido de folga.");
        }

        return "redirect:/web/complementares";
    }

    @PostMapping("/permutas")
    public String criarPermuta(@RequestParam("origemId") Integer origemId,
                               @RequestParam("destinoId") Integer destinoId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Integer utilizadorId = (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
        if (utilizadorId == null) {
            return "redirect:/web/login";
        }

        try {
            Horario meuTurno = horarioBLL.listarMeusTurnosDisponiveisParaPermuta(utilizadorId).stream()
                    .filter(turno -> origemId.equals(turno.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Seleciona um turno de origem valido."));

            Horario turnoColega = horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, origemId).stream()
                    .filter(turno -> destinoId.equals(turno.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Seleciona um turno de destino valido."));

            permutaBLL.registarPedidoTroca(utilizadorId, meuTurno, turnoColega);
            redirectAttributes.addFlashAttribute("sucessoComplementares", "Pedido de permuta submetido com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erroComplementares", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erroComplementares", "Nao foi possivel submeter o pedido de permuta.");
        }

        return "redirect:/web/complementares?origemId=" + origemId;
    }

    public static String formatarPeriodo(Preferencia preferencia) {
        if (preferencia == null) {
            return "-";
        }
        if (preferencia.getDataInicio() == null && preferencia.getDataFim() == null) {
            return "Sem periodo";
        }
        if (preferencia.getDataInicio() != null && preferencia.getDataFim() == null) {
            return "Desde " + DATA_FORMATTER.format(preferencia.getDataInicio());
        }
        if (preferencia.getDataInicio() == null) {
            return DATA_FORMATTER.format(preferencia.getDataFim());
        }
        return DATA_FORMATTER.format(preferencia.getDataInicio()) + " a " + DATA_FORMATTER.format(preferencia.getDataFim());
    }

    public static String formatarTipoPreferencia(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "-";
        }
        return switch (tipo.toLowerCase(Locale.ROOT)) {
            case "ferias" -> "Ferias";
            case "folgas" -> "Folgas";
            case "colegas" -> "Colegas";
            case "turnos" -> "Turnos";
            default -> tipo;
        };
    }

    public static String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "-";
        }
        return Character.toUpperCase(estado.charAt(0)) + estado.substring(1).toLowerCase(Locale.ROOT);
    }

    public static String formatarTurno(Horario horario) {
        if (horario == null || horario.getDataTurno() == null || horario.getIdTurno() == null) {
            return "-";
        }
        return DATA_FORMATTER.format(horario.getDataTurno())
                + " | "
                + horario.getIdTurno().getHoraInicio()
                + "-"
                + horario.getIdTurno().getHoraFim();
    }

    public static String formatarDataHoraPedido(Instant dataHora) {
        if (dataHora == null) {
            return "-";
        }
        return DATA_HORA_FORMATTER.format(dataHora.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    private List<Horario> listarTurnosElegiveis(Integer utilizadorId, Integer origemId) {
        if (origemId == null) {
            return List.of();
        }
        List<Horario> turnos = horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, origemId);
        return new ArrayList<>(turnos);
    }
}
