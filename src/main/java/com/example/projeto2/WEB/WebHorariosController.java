package com.example.projeto2.WEB;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
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
import java.util.List;

@Controller
@RequestMapping("/web/horarios")
public class WebHorariosController {

    private final GeracaoHorariosBLL geracaoHorariosBLL;
    private final WebAppService webAppService;

    public WebHorariosController(GeracaoHorariosBLL geracaoHorariosBLL,
                                 WebAppService webAppService) {
        this.geracaoHorariosBLL = geracaoHorariosBLL;
        this.webAppService = webAppService;
    }

    @GetMapping
    public String horarios(@RequestParam(value = "ano", required = false) Integer ano,
                           @RequestParam(value = "mes", required = false) Integer mes,
                           HttpSession session,
                           Model model) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "horarios");

        LocalDate hoje = LocalDate.now();
        int anoConsulta = ano != null ? ano : hoje.getYear();
        int mesConsulta = mes != null ? mes : hoje.getMonthValue();

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
            model.addAttribute("podeValidar", contexto.podeValidar());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
            model.addAttribute("podeGerar", false);
            model.addAttribute("podeValidar", false);
        }

        return "web/horarios";
    }

    @PostMapping("/gerar")
    public String gerar(@RequestParam("ano") Integer ano,
                        @RequestParam("mes") Integer mes,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);

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

    @PostMapping("/{idProposta}/aprovar")
    public String aprovar(@PathVariable("idProposta") Integer idProposta,
                          @RequestParam("ano") Integer ano,
                          @RequestParam("mes") Integer mes,
                          @RequestParam(value = "observacoes", required = false) String observacoes,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);

        try {
            geracaoHorariosBLL.aprovarProposta(utilizadorId, idProposta, observacoes);
            redirectAttributes.addFlashAttribute("sucesso", "Proposta aprovada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erro", "Nao foi possivel aprovar a proposta selecionada.");
        }

        return "redirect:/web/horarios?ano=" + ano + "&mes=" + mes;
    }

    @PostMapping("/{idProposta}/rejeitar")
    public String rejeitar(@PathVariable("idProposta") Integer idProposta,
                           @RequestParam("ano") Integer ano,
                           @RequestParam("mes") Integer mes,
                           @RequestParam(value = "observacoes", required = false) String observacoes,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);

        try {
            geracaoHorariosBLL.rejeitarProposta(utilizadorId, idProposta, observacoes);
            redirectAttributes.addFlashAttribute("sucesso", "Proposta rejeitada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erro", "Nao foi possivel rejeitar a proposta selecionada.");
        }

        return "redirect:/web/horarios?ano=" + ano + "&mes=" + mes;
    }
}
