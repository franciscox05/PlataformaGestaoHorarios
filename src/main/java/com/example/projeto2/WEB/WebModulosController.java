package com.example.projeto2.WEB;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Repositories.HorarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web")
public class WebModulosController {

    private final WebLayoutService webLayoutService;
    private final WebAppService webAppService;
    private final HorarioRepository horarioRepository;

    public WebModulosController(WebLayoutService webLayoutService,
                                WebAppService webAppService,
                                HorarioRepository horarioRepository) {
        this.webLayoutService = webLayoutService;
        this.webAppService = webAppService;
        this.horarioRepository = horarioRepository;
    }

    @GetMapping("/gestao-loja")
    public String gestaoLoja(HttpSession session, Model model) {
        return moduloPlaceholder(session, model, "gestao-loja", "Gestao de loja");
    }

    @GetMapping("/relatorios")
    public String relatorios(HttpSession session, Model model) {
        return moduloPlaceholder(session, model, "relatorios", "Relatorios");
    }

    @GetMapping("/relatorios/exportar.csv")
    @ResponseBody
    public ResponseEntity<String> exportarCsv(@RequestParam("ano") int ano,
                                               @RequestParam("mes") int mes,
                                               HttpSession session) {
        Integer utilizadorId = (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
        if (utilizadorId == null) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/web/login").build();
        }
        Integer idLoja = webAppService.obterLojaAtual(session);
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        List<Horario> horarios = horarioRepository.findHorariosPublicadosDaLojaEntreDatas(
                idLoja, inicio, fim, null);

        String nomeLoja = horarios.isEmpty() ? "" :
                horarios.get(0).getIdLojautilizador().getIdLoja().getNome();

        Map<Integer, List<Horario>> porColaborador = horarios.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId()));

        StringBuilder csv = new StringBuilder("Loja;Mes;Ano;Colaborador;Cargo;Turnos;FolgasAprovadas;Horas\n");
        for (Map.Entry<Integer, List<Horario>> entry : porColaborador.entrySet()) {
            List<Horario> turnos = entry.getValue();
            Horario primeiro = turnos.get(0);
            String nomeColab = primeiro.getIdLojautilizador().getIdUtilizador().getNome();
            String cargo = primeiro.getIdLojautilizador().getIdCargo().getNome();
            long totalTurnos = turnos.size();
            long totalMinutos = turnos.stream().mapToLong(h ->
                    Duration.between(h.getIdTurno().getHoraInicio(), h.getIdTurno().getHoraFim()).toMinutes()
            ).sum();
            double totalHoras = totalMinutos / 60.0;

            csv.append(nomeLoja).append(";")
               .append(mes).append(";")
               .append(ano).append(";")
               .append(nomeColab).append(";")
               .append(cargo).append(";")
               .append(totalTurnos).append(";")
               .append(0).append(";")
               .append(String.format("%.1f", totalHoras)).append("\n");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=relatorio-" + ano + "-" + mes + ".csv")
                .body(csv.toString());
    }

    private String moduloPlaceholder(HttpSession session, Model model, String paginaAtiva, String titulo) {
        Integer utilizadorId = (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
        if (utilizadorId == null) {
            return "redirect:/web/login";
        }
        model.addAttribute("utilizadorNome", session.getAttribute(WebSession.UTILIZADOR_NOME));
        webLayoutService.aplicar(model, utilizadorId, paginaAtiva);
        model.addAttribute("tituloModulo", titulo);
        return "web/modulo-placeholder";
    }
}
