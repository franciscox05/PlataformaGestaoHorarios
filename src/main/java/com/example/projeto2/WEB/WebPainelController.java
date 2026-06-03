package com.example.projeto2.WEB;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.BLL.PreferenciaBLL;
import com.example.projeto2.Enums.EstadoPermuta;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Modules.Preferencia;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/web/painel")
public class WebPainelController {

    private final WebAppService webAppService;
    private final HorarioBLL horarioBLL;
    private final DayOffBLL dayOffBLL;
    private final PreferenciaBLL preferenciaBLL;
    private final PermutaBLL permutaBLL;

    public WebPainelController(WebAppService webAppService,
                               HorarioBLL horarioBLL,
                               DayOffBLL dayOffBLL,
                               PreferenciaBLL preferenciaBLL,
                               PermutaBLL permutaBLL) {
        this.webAppService = webAppService;
        this.horarioBLL = horarioBLL;
        this.dayOffBLL = dayOffBLL;
        this.preferenciaBLL = preferenciaBLL;
        this.permutaBLL = permutaBLL;
    }

    @GetMapping
    public String painel(HttpSession session,
                         @RequestParam(value = "acessoNegado", required = false) Boolean acessoNegado,
                         Model model) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "painel");

        List<Horario> proximosTurnos = horarioBLL.listarProximosTurnos(utilizadorId).stream()
                .limit(5)
                .toList();
        List<Horario> equipaHoje = horarioBLL.listarEquipaDeHoje(utilizadorId).stream()
                .limit(10)
                .toList();

        List<DayOff> folgas = dayOffBLL.listarPedidosPorUtilizador(utilizadorId);
        List<Preferencia> preferencias = preferenciaBLL.listarPreferenciasPorUtilizador(utilizadorId);
        List<Permuta> permutas = permutaBLL.listarPedidosEnviados(utilizadorId);

        long folgasPendentes = folgas.stream().filter(item -> estadoEquals(item.getEstado(), "pendente")).count();
        long preferenciasPendentes = preferencias.stream().filter(item -> estadoEquals(item.getEstado(), "pendente")).count();
        long permutasPendentes = permutas.stream().filter(item -> estadoEqualsEnum(item.getEstado(), "pendente")).count();

        WebAppService.WebPermissoes permissoes = webAppService.obterPermissoes(utilizadorId);
        long folgasParaAprovar = permissoes.podeAprovarFolgas()
                ? dayOffBLL.listarPedidosPendentesParaAprovacao(utilizadorId).size()
                : 0;
        long preferenciasParaAprovar = permissoes.podeAprovarPreferencias()
                ? preferenciaBLL.listarPreferenciasPendentesParaAprovacao(utilizadorId).size()
                : 0;
        long permutasParaAprovar = permissoes.podeAprovarPermutas()
                ? permutaBLL.listarPedidosPendentesParaAprovacao(utilizadorId).size()
                : 0;

        if (Boolean.TRUE.equals(acessoNegado)) {
            model.addAttribute("erro", "Nao tens permissao para abrir esse modulo com o perfil atual.");
        }

        model.addAttribute("proximosTurnos", proximosTurnos);
        model.addAttribute("equipaHoje", equipaHoje);
        model.addAttribute("totalEquipaHoje", equipaHoje.size());
        model.addAttribute("folgasPendentes", folgasPendentes);
        model.addAttribute("preferenciasPendentes", preferenciasPendentes);
        model.addAttribute("permutasPendentes", permutasPendentes);
        model.addAttribute("folgasParaAprovar", folgasParaAprovar);
        model.addAttribute("preferenciasParaAprovar", preferenciasParaAprovar);
        model.addAttribute("permutasParaAprovar", permutasParaAprovar);
        return "web/painel";
    }

    private boolean estadoEquals(String atual, String esperado) {
        if (atual == null || esperado == null) {
            return false;
        }
        return atual.trim().toLowerCase(Locale.ROOT).equals(esperado.trim().toLowerCase(Locale.ROOT));
    }

    private boolean estadoEqualsEnum(EstadoPermuta atual, String esperado) {
        if (atual == null || esperado == null) {
            return false;
        }
        return atual.name().toLowerCase(Locale.ROOT).equals(esperado.trim().toLowerCase(Locale.ROOT));
    }
}
