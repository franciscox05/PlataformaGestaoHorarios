package com.example.projeto2.WEB;

import com.example.projeto2.BLL.RelatorioHorasBLL;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;

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

    @GetMapping(value = "/exportar.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(value = "ano") Integer ano,
                                              @RequestParam(value = "mes") Integer mes,
                                              @RequestParam(value = "colaborador", required = false) Integer idColaborador,
                                              HttpSession session) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        RelatorioHorasBLL.RelatorioResultado resultado = relatorioHorasBLL.gerarRelatorio(
                utilizadorId,
                ano,
                mes,
                idColaborador
        );

        StringBuilder csv = new StringBuilder();
        csv.append("Loja;Mes;Ano;Colaborador;Cargo;Turnos;FolgasAprovadas;Horas\n");
        for (RelatorioHorasBLL.RelatorioLinha linha : resultado.linhas()) {
            csv.append(csvCell(resultado.nomeLoja())).append(';')
                    .append(csvCell(resultado.nomeMes())).append(';')
                    .append(resultado.ano()).append(';')
                    .append(csvCell(linha.nomeColaborador())).append(';')
                    .append(csvCell(linha.cargo())).append(';')
                    .append(linha.turnos()).append(';')
                    .append(linha.folgasAprovadas()).append(';')
                    .append(csvCell(linha.horasFormatadas()))
                    .append('\n');
        }

        byte[] conteudo = csv.toString().getBytes(StandardCharsets.UTF_8);
        String nomeFicheiro = "relatorio-horas-" + ano + "-" + String.format("%02d", mes) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeFicheiro + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(conteudo);
    }

    private String csvCell(String valor) {
        String normalizado = valor == null ? "" : valor;
        return "\"" + normalizado.replace("\"", "\"\"") + "\"";
    }
}
