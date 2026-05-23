package com.example.projeto2.WEB;

import com.example.projeto2.BLL.RelatorioHorasBLL;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/web/relatorios")
public class WebRelatoriosController {

    private final WebAppService webAppService;
    private final RelatorioHorasBLL relatorioHorasBLL;

    public WebRelatoriosController(WebAppService webAppService,
                                   RelatorioHorasBLL relatorioHorasBLL) {
        this.webAppService = webAppService;
        this.relatorioHorasBLL = relatorioHorasBLL;
    }

    @GetMapping
    public String relatorios(@RequestParam(value = "ano", required = false) Integer ano,
                             @RequestParam(value = "mes", required = false) Integer mes,
                             @RequestParam(value = "colaborador", required = false) Integer idColaborador,
                             HttpSession session,
                             Model model) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "relatorios");

        try {
            RelatorioHorasBLL.RelatorioContexto contexto = relatorioHorasBLL.obterContexto(utilizadorId);

            int anoConsulta = ano != null ? ano : contexto.anoAtual();
            int mesConsulta = mes != null ? mes : contexto.mesAtual();
            RelatorioHorasBLL.RelatorioResultado resultado = relatorioHorasBLL.gerarRelatorio(
                    utilizadorId,
                    anoConsulta,
                    mesConsulta,
                    idColaborador
            );

            model.addAttribute("contexto", contexto);
            model.addAttribute("resultado", resultado);
            model.addAttribute("ano", anoConsulta);
            model.addAttribute("mes", mesConsulta);
            model.addAttribute("colaboradorSelecionado", idColaborador);
            model.addAttribute("meses", MesWebOption.todos());
            model.addAttribute("anos", MesWebOption.anosProximos(contexto.anoAtual(), 2));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
        }

        return "web/relatorios";
    }
}
