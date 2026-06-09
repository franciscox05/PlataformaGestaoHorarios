package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.GestaoLojaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Controller
@RequestMapping("/web/gestao-loja")
public class WebGestaoLojaController {

    private final WebAppService webAppService;
    private final GestaoLojaService gestaoLojaBLL;

    public WebGestaoLojaController(WebAppService webAppService,
                                   GestaoLojaService gestaoLojaBLL) {
        this.webAppService = webAppService;
        this.gestaoLojaBLL = gestaoLojaBLL;
    }

    @PostMapping("/regras")
    public String atualizarRegras(HttpServletRequest request,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            GestaoLojaService.GestaoLojaResumo resumo = gestaoLojaBLL.obterResumo(utilizadorId);
            LocalTime horaAberturaAtual = parseHoraObrigatoria(resumo.horaAbertura(), "abertura");
            LocalTime horaFechoAtual = parseHoraObrigatoria(resumo.horaFecho(), "fecho");
            List<GestaoLojaService.ConfiguracaoRegraRequest> regras = lerRegrasDoFormulario(request);

            gestaoLojaBLL.guardarConfiguracao(
                    utilizadorId,
                    new GestaoLojaService.ConfiguracaoLojaRequest(horaAberturaAtual, horaFechoAtual, regras)
            );
            redirectAttributes.addFlashAttribute("sucesso", "Regras da loja atualizadas com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/gestao-loja";
    }

    @GetMapping
    public String pagina(HttpSession session, Model model) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "gestao-loja");

        try {
            GestaoLojaService.GestaoLojaResumo resumo = gestaoLojaBLL.obterResumo(utilizadorId);
            model.addAttribute("resumo", resumo);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
        }

        return "web/gestao-loja";
    }

    @PostMapping("/horario")
    public String atualizarHorario(@RequestParam("horaAbertura") String horaAbertura,
                                   @RequestParam("horaFecho") String horaFecho,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            gestaoLojaBLL.guardarConfiguracao(
                    utilizadorId,
                    new GestaoLojaService.ConfiguracaoLojaRequest(
                            parseHoraObrigatoria(horaAbertura, "abertura"),
                            parseHoraObrigatoria(horaFecho, "fecho"),
                            null
                    )
            );
            redirectAttributes.addFlashAttribute("sucesso", "Horario base da loja atualizado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/gestao-loja";
    }

    @PostMapping("/excecao")
    public String guardarExcecao(@RequestParam(value = "idHorarioEspecial", required = false) Integer idHorarioEspecial,
                                 @RequestParam("descricao") String descricao,
                                 @RequestParam("dataInicio") String dataInicio,
                                 @RequestParam("dataFim") String dataFim,
                                 @RequestParam(value = "lojaEncerrada", required = false) boolean lojaEncerrada,
                                 @RequestParam(value = "horaAbertura", required = false) String horaAbertura,
                                 @RequestParam(value = "horaFecho", required = false) String horaFecho,
                                 @RequestParam(value = "minimoColaboradoresTurno", required = false) Integer minimoColaboradoresTurno,
                                 @RequestParam(value = "observacoes", required = false) String observacoes,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            gestaoLojaBLL.guardarHorarioEspecial(
                    utilizadorId,
                    new GestaoLojaService.ConfiguracaoHorarioEspecialRequest(
                            idHorarioEspecial,
                            descricao,
                            parseDataObrigatoria(dataInicio, "inicial"),
                            parseDataObrigatoria(dataFim, "final"),
                            lojaEncerrada,
                            lojaEncerrada ? null : parseHoraOpcional(horaAbertura, "abertura especial"),
                            lojaEncerrada ? null : parseHoraOpcional(horaFecho, "fecho especial"),
                            lojaEncerrada ? null : minimoColaboradoresTurno,
                            observacoes
                    )
            );
            redirectAttributes.addFlashAttribute("sucesso", "Excecao de horario guardada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/gestao-loja";
    }

    @PostMapping("/excecao/remover")
    public String removerExcecao(@RequestParam("idHorarioEspecial") Integer idHorarioEspecial,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            gestaoLojaBLL.removerHorarioEspecial(utilizadorId, idHorarioEspecial);
            redirectAttributes.addFlashAttribute("sucesso", "Excecao removida com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/gestao-loja";
    }

    private LocalTime parseHoraObrigatoria(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Indica a hora de " + campo + " no formato HH:mm.");
        }
        return parseHora(valor, campo);
    }

    private LocalTime parseHoraOpcional(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return parseHora(valor, campo);
    }

    private LocalTime parseHora(String valor, String campo) {
        try {
            return LocalTime.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("A hora de " + campo + " deve seguir o formato HH:mm.");
        }
    }

    private LocalDate parseDataObrigatoria(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Indica a data " + campo + " da excecao.");
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("A data " + campo + " da excecao e invalida.");
        }
    }

    private List<GestaoLojaService.ConfiguracaoRegraRequest> lerRegrasDoFormulario(HttpServletRequest request) {
        List<GestaoLojaService.ConfiguracaoRegraRequest> regras = new ArrayList<>();
        Enumeration<String> nomes = request.getParameterNames();
        while (nomes.hasMoreElements()) {
            String nome = nomes.nextElement();
            if (nome == null || !nome.startsWith("regra_valor_")) {
                continue;
            }

            Integer idRegra = parseIdRegra(nome.substring("regra_valor_".length()));
            Integer valor = parseInteiroOpcional(request.getParameter(nome));
            String observacoes = request.getParameter("regra_obs_" + idRegra);
            regras.add(new GestaoLojaService.ConfiguracaoRegraRequest(idRegra, valor, observacoes));
        }
        return regras;
    }

    private Integer parseIdRegra(String valor) {
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Foi encontrada uma regra invalida no formulario.");
        }
    }

    private Integer parseInteiroOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Os valores das regras devem ser numeros inteiros.");
        }
    }
}
