package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.GeracaoHorariosService;
import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.API.Modules.Horario;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for the employee schedule day-detail view.
 *
 * GET /api/horarios/detalhe-dia?data=YYYY-MM-DD
 *   → { data, equipa: [{nome, cargo, tipoTurno, inicio, fim}], folgas: [{nome, tipo}] }
 *
 * Resolves the store from the active session. The 'motivo' field of approved day-offs
 * is intentionally excluded — it is private to managers only.
 */
@RestController
@RequestMapping("/api/horarios")
public class WebHorariosApiController {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final WebAppService webAppService;
    private final HorarioService horarioBLL;
    private final DayOffService dayOffBLL;
    private final GeracaoHorariosService geracaoHorariosBLL;

    public WebHorariosApiController(WebAppService webAppService,
                                    HorarioService horarioBLL,
                                    DayOffService dayOffBLL,
                                    GeracaoHorariosService geracaoHorariosBLL) {
        this.webAppService = webAppService;
        this.horarioBLL    = horarioBLL;
        this.dayOffBLL     = dayOffBLL;
        this.geracaoHorariosBLL = geracaoHorariosBLL;
    }

    /**
     * Dados de turnos de um mês, no mesmo formato de 'turnosCalendario' usado na
     * renderização SSR (WebHorariosController) — usado pela vista de calendário
     * para navegação client-side sem reload de página.
     */
    @GetMapping("/mes")
    public ResponseEntity<?> turnosDoMes(@RequestParam("ano") Integer ano,
                                         @RequestParam("mes") Integer mes,
                                         HttpSession session) {
        try {
            Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
            Integer idLoja = webAppService.obterLojaAtual(session);
            List<Horario> turnos = geracaoHorariosBLL.obterMeusHorarios(utilizadorId, idLoja, ano, mes);

            DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");
            List<Map<String, Object>> turnosCalendario = turnos.stream()
                    .filter(t -> t.getDataTurno() != null && t.getIdTurno() != null)
                    .map(t -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("dia", t.getDataTurno().getDayOfMonth());
                        m.put("tipo", t.getIdTurno().getTipo() != null ? t.getIdTurno().getTipo() : "");
                        m.put("inicio", t.getIdTurno().getHoraInicio() != null
                                ? t.getIdTurno().getHoraInicio().format(hhmm) : "");
                        m.put("fim", t.getIdTurno().getHoraFim() != null
                                ? t.getIdTurno().getHoraFim().format(hhmm) : "");
                        return m;
                    })
                    .toList();

            return ResponseEntity.ok(turnosCalendario);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", "Nao foi possivel carregar o horario do mes."));
        }
    }

    @GetMapping("/detalhe-dia")
    public ResponseEntity<?> detalheDia(@RequestParam("data") String dataParam,
                                        HttpSession session) {
        try {
            Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
            Integer idLoja = webAppService.obterLojaAtual(session); // null until Step 5 login wiring

            LocalDate data;
            try {
                data = LocalDate.parse(dataParam);
            } catch (DateTimeParseException ex) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Data invalida — usa o formato YYYY-MM-DD."));
            }

            List<HorarioService.ColaboradorNoDia> equipa =
                    horarioBLL.listarColegasNoDia(utilizadorId, data, idLoja);

            List<DayOffService.FolgaResumida> folgas =
                    idLoja != null
                            ? dayOffBLL.listarFuncionariosDeFolgaNoDia(idLoja, data)
                            : List.of();

            List<Map<String, Object>> equipaJson = equipa.stream()
                    .map(c -> {
                        Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("nome",      c.nome());
                        m.put("cargo",     c.cargo());
                        m.put("tipoTurno", c.tipoTurno());
                        m.put("inicio",    c.inicio());
                        m.put("fim",       c.fim());
                        m.put("meuTurno",  c.meuTurno());
                        if (c.horarioId() != null) m.put("horarioId", c.horarioId());
                        return m;
                    })
                    .toList();

            List<Map<String, String>> folgasJson = folgas.stream()
                    .map(f -> Map.of(
                            "nome", f.nome(),
                            "tipo", f.tipo()
                    ))
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "data",   data.format(FMT_DATA),
                    "equipa", equipaJson,
                    "folgas", folgasJson
            ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", "Nao foi possivel carregar os detalhes do dia."));
        }
    }
}
