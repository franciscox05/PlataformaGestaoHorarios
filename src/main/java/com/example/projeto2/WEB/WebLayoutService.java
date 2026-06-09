package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.GeracaoHorariosService;
import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.API.Services.RelatorioHorasService;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class WebLayoutService {

    private final GestaoLojaService gestaoLojaBLL;
    private final GeracaoHorariosService geracaoHorariosBLL;
    private final RelatorioHorasService relatorioHorasBLL;
    private final LojautilizadorRepository lojautilizadorRepository;

    public WebLayoutService(GestaoLojaService gestaoLojaBLL,
                            GeracaoHorariosService geracaoHorariosBLL,
                            RelatorioHorasService relatorioHorasBLL,
                            LojautilizadorRepository lojautilizadorRepository) {
        this.gestaoLojaBLL = gestaoLojaBLL;
        this.geracaoHorariosBLL = geracaoHorariosBLL;
        this.relatorioHorasBLL = relatorioHorasBLL;
        this.lojautilizadorRepository = lojautilizadorRepository;
    }

    public void aplicar(Model model, Integer idUtilizador, String paginaAtiva) {
        boolean podeGerirLoja = false;
        boolean podeValidarHorarios = false;
        boolean podeRelatorios = false;

        try {
            podeGerirLoja = gestaoLojaBLL.utilizadorPodeGerirLoja(idUtilizador);
        } catch (Exception ignored) {
            // Mantem comportamento resiliente para nao bloquear renderizacao web.
        }
        try {
            podeValidarHorarios = geracaoHorariosBLL.utilizadorPodeValidarHorarios(idUtilizador);
        } catch (Exception ignored) {
            // Mantem comportamento resiliente para nao bloquear renderizacao web.
        }
        try {
            podeRelatorios = relatorioHorasBLL.utilizadorPodeConsultarRelatorios(idUtilizador);
        } catch (Exception ignored) {
            // Mantem comportamento resiliente para nao bloquear renderizacao web.
        }

        boolean podeAcederHorarios = podeGerirLoja || podeValidarHorarios;

        String cargo = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(ligacao -> ligacao.getIdCargo())
                .map(c -> c != null ? c.getNome() : null)
                .orElse("-");

        model.addAttribute("webPaginaAtiva", paginaAtiva);
        model.addAttribute("webPodeGerirLoja", podeGerirLoja);
        model.addAttribute("webPodeAcederHorarios", podeAcederHorarios);
        model.addAttribute("webPodeRelatorios", podeRelatorios);
        model.addAttribute("webCargoAtual", cargo);
    }
}
