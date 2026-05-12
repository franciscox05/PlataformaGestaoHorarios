package com.example.projeto2.WEB;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/web/horarios")
public class WebHorariosController {

    private final GeracaoHorariosBLL geracaoHorariosBLL;

    public WebHorariosController(GeracaoHorariosBLL geracaoHorariosBLL) {
        this.geracaoHorariosBLL = geracaoHorariosBLL;
    }

    @GetMapping
    public String horarios(@RequestParam(value = "ano", required = false) Integer ano,
                           @RequestParam(value = "mes", required = false) Integer mes,
                           HttpSession session,
                           Model model) {
        Integer utilizadorId = (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
        if (utilizadorId == null) {
            return "redirect:/web/login";
        }

        LocalDate hoje = LocalDate.now();
        int anoConsulta = ano != null ? ano : hoje.getYear();
        int mesConsulta = mes != null ? mes : hoje.getMonthValue();

        model.addAttribute("utilizadorNome", session.getAttribute(WebSession.UTILIZADOR_NOME));
        model.addAttribute("ano", anoConsulta);
        model.addAttribute("mes", mesConsulta);
        model.addAttribute("meses", MesWebOption.todos());
        model.addAttribute("anos", MesWebOption.anosProximos(hoje.getYear(), 2));

        try {
            GeracaoHorariosBLL.GeracaoContexto contexto = geracaoHorariosBLL.obterContexto(utilizadorId);
            GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.obterPlaneamento(utilizadorId, anoConsulta, mesConsulta);
            List<GeracaoHorariosBLL.PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(utilizadorId, anoConsulta, mesConsulta);

            model.addAttribute("contexto", contexto);
            model.addAttribute("proposta", proposta);
            model.addAttribute("propostas", propostas);
            model.addAttribute("podeGerar", contexto.podeGerar());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
            model.addAttribute("podeGerar", false);
        }

        return "web/horarios";
    }

    @PostMapping("/gerar")
    public String gerar(@RequestParam("ano") Integer ano,
                        @RequestParam("mes") Integer mes,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        Integer utilizadorId = (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
        if (utilizadorId == null) {
            return "redirect:/web/login";
        }

        try {
            geracaoHorariosBLL.gerarProposta(utilizadorId, ano, mes);
            redirectAttributes.addFlashAttribute("sucesso", "Proposta gerada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erro", "Nao foi possivel gerar a proposta para o periodo selecionado.");
        }

        return "redirect:/web/horarios?ano=" + ano + "&mes=" + mes;
    }
}
