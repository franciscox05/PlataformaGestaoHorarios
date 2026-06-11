package com.example.projeto2.WEB;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.PermutaFolga;
import com.example.projeto2.API.Services.PermutaFolgaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * REST API de suporte ao Portal Web para o fluxo de Permutas de Folga.
 *
 * GET  /api/permuta-folga/meus-turnos        → turnos que posso ceder (tenho turno, quero folga)
 * GET  /api/permuta-folga/compensacoes       → turnos de colegas elegíveis para compensação
 * POST /api/permuta-folga/submeter           → submete pedido
 * POST /api/permuta-folga/cancelar           → cancela pedido próprio pendente
 */
@RestController
@RequestMapping("/api/permuta-folga")
public class WebPermutaFolgaApiController {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final WebAppService webAppService;
    private final PermutaFolgaService permutaFolgaBLL;

    public WebPermutaFolgaApiController(WebAppService webAppService,
                                        PermutaFolgaService permutaFolgaBLL) {
        this.webAppService   = webAppService;
        this.permutaFolgaBLL = permutaFolgaBLL;
    }

    @GetMapping("/meus-turnos")
    public ResponseEntity<?> meusTurnos(HttpSession session) {
        try {
            Integer id = webAppService.obterUtilizadorIdObrigatorio(session);
            List<Map<String, Object>> resultado = permutaFolgaBLL.listarTurnosParaCederFolga(id)
                    .stream().map(this::mapHorario).toList();
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Nao foi possivel carregar os turnos."));
        }
    }

    @GetMapping("/compensacoes")
    public ResponseEntity<?> compensacoes(@RequestParam("idHorarioD") Integer idHorarioD,
                                          HttpSession session) {
        try {
            Integer id = webAppService.obterUtilizadorIdObrigatorio(session);
            List<Map<String, Object>> resultado = permutaFolgaBLL
                    .listarTurnosElegiveisCompensacao(id, idHorarioD)
                    .stream().map(h -> {
                        Map<String, Object> m = mapHorario(h);
                        m = new java.util.HashMap<>(m);
                        m.put("colega", h.getIdLojautilizador().getIdUtilizador().getNome());
                        return m;
                    }).toList();
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Nao foi possivel carregar as compensacoes."));
        }
    }

    @PostMapping("/submeter")
    public ResponseEntity<?> submeter(@RequestParam("idHorarioD") Integer idHorarioD,
                                      @RequestParam("idHorarioY") Integer idHorarioY,
                                      HttpSession session) {
        try {
            Integer id = webAppService.obterUtilizadorIdObrigatorio(session);

            List<Horario> meus = permutaFolgaBLL.listarTurnosParaCederFolga(id);
            Horario horarioD = meus.stream().filter(h -> idHorarioD.equals(h.getId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("O turno a ceder nao e valido ou nao te pertence."));

            List<Horario> compensacoes = permutaFolgaBLL.listarTurnosElegiveisCompensacao(id, idHorarioD);
            Horario horarioY = compensacoes.stream().filter(h -> idHorarioY.equals(h.getId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("O turno de compensacao selecionado nao e elegivel."));

            PermutaFolga pf = permutaFolgaBLL.registarPedido(id, horarioD, horarioY);
            return ResponseEntity.ok(Map.of(
                    "id",      pf.getId(),
                    "estado",  pf.getEstado(),
                    "mensagem","Pedido de permuta de folga submetido. Aguarda a aprovacao do supervisor."
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Nao foi possivel submeter o pedido."));
        }
    }

    @PostMapping("/cancelar")
    public ResponseEntity<?> cancelar(@RequestParam("idPermutaFolga") Integer idPermutaFolga,
                                      HttpSession session) {
        try {
            Integer id = webAppService.obterUtilizadorIdObrigatorio(session);
            permutaFolgaBLL.cancelar(idPermutaFolga, id);
            return ResponseEntity.ok(Map.of("mensagem", "Pedido cancelado com sucesso."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Nao foi possivel cancelar o pedido."));
        }
    }

    private Map<String, Object> mapHorario(Horario h) {
        return Map.of(
                "id",        h.getId(),
                "data",      h.getDataTurno() != null ? h.getDataTurno().format(FMT_DATA) : "-",
                "horaInicio", h.getIdTurno() != null && h.getIdTurno().getHoraInicio() != null
                        ? h.getIdTurno().getHoraInicio().format(FMT_HORA) : "-",
                "horaFim",   h.getIdTurno() != null && h.getIdTurno().getHoraFim() != null
                        ? h.getIdTurno().getHoraFim().format(FMT_HORA) : "-",
                "label",     formatarTurno(h)
        );
    }

    private String formatarTurno(Horario h) {
        if (h == null || h.getDataTurno() == null || h.getIdTurno() == null) return "-";
        String ini = h.getIdTurno().getHoraInicio() != null ? h.getIdTurno().getHoraInicio().format(FMT_HORA) : "--:--";
        String fim = h.getIdTurno().getHoraFim()    != null ? h.getIdTurno().getHoraFim().format(FMT_HORA)    : "--:--";
        return h.getDataTurno().format(FMT_DATA) + " · " + ini + " – " + fim;
    }
}
