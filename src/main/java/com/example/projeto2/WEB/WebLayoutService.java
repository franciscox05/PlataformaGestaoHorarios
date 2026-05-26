package com.example.projeto2.WEB;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.BLL.GestaoLojaBLL;
import com.example.projeto2.BLL.RelatorioHorasBLL;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class WebLayoutService {

    private final GestaoLojaBLL gestaoLojaBLL;
    private final GeracaoHorariosBLL geracaoHorariosBLL;
    private final RelatorioHorasBLL relatorioHorasBLL;
    private final LojautilizadorRepository lojautilizadorRepository;

    public WebLayoutService(GestaoLojaBLL gestaoLojaBLL,
                            GeracaoHorariosBLL geracaoHorariosBLL,
                            RelatorioHorasBLL relatorioHorasBLL,
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
