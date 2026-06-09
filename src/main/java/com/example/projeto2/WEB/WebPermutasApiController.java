package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.API.Services.PermutaService;
import com.example.projeto2.API.Enums.EstadoPermuta;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Permuta;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * REST API de suporte ao Portal Web para o fluxo de Permutas.
 *
 * GET  /api/permutas/meus-turnos          → lista turnos aprovados do utilizador logado
 * GET  /api/permutas/turnos-elegiveis     → lista turnos elegíveis para permuta com o turno origem
 * POST /api/permutas/submeter             → submete um pedido de permuta, valida estado_permuta_enum
 */
@RestController
@RequestMapping("/api/permutas")
public class WebPermutasApiController {

    private static final DateTimeFormatter FMT_DATA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA  = DateTimeFormatter.ofPattern("HH:mm");

    private final WebAppService webAppService;
    private final HorarioService horarioBLL;
    private final PermutaService permutaBLL;

    public WebPermutasApiController(WebAppService webAppService,
                                    HorarioService horarioBLL,
                                    PermutaService permutaBLL) {
        this.webAppService = webAppService;
        this.horarioBLL    = horarioBLL;
        this.permutaBLL    = permutaBLL;
    }

    // ── GET /api/permutas/meus-turnos ────────────────────────────────────────
    @GetMapping("/meus-turnos")
    public ResponseEntity<?> meusTurnos(HttpSession session) {
        try {
            Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
            List<Horario> turnos = horarioBLL.listarMeusTurnosDisponiveisParaPermuta(utilizadorId);
            List<Map<String, Object>> resposta = turnos.stream()
                    .map(h -> Map.<String, Object>of(
                            "id",        h.getId(),
                            "data",      h.getDataTurno() != null ? h.getDataTurno().format(FMT_DATA) : "-",
                            "horaInicio", h.getIdTurno().getHoraInicio() != null ? h.getIdTurno().getHoraInicio().format(FMT_HORA) : "-",
                            "horaFim",   h.getIdTurno().getHoraFim()    != null ? h.getIdTurno().getHoraFim().format(FMT_HORA)    : "-",
                            "label",     formatarTurno(h)
                    ))
                    .toList();
            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Nao foi possivel carregar os turnos."));
        }
    }

    // ── GET /api/permutas/turnos-elegiveis?idHorarioOrigem=X ────────────────
    @GetMapping("/turnos-elegiveis")
    public ResponseEntity<?> turnosElegiveis(@RequestParam("idHorarioOrigem") Integer idHorarioOrigem,
                                             HttpSession session) {
        try {
            Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
            if (idHorarioOrigem == null) {
                return ResponseEntity.badRequest().body(Map.of("erro", "O turno de origem e obrigatorio."));
            }
            List<Horario> turnos = horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, idHorarioOrigem);
            List<Map<String, Object>> resposta = turnos.stream()
                    .map(h -> Map.<String, Object>of(
                            "id",        h.getId(),
                            "colega",    h.getIdLojautilizador().getIdUtilizador().getNome(),
                            "data",      h.getDataTurno() != null ? h.getDataTurno().format(FMT_DATA) : "-",
                            "horaInicio", h.getIdTurno().getHoraInicio() != null ? h.getIdTurno().getHoraInicio().format(FMT_HORA) : "-",
                            "horaFim",   h.getIdTurno().getHoraFim()    != null ? h.getIdTurno().getHoraFim().format(FMT_HORA)    : "-",
                            "label",     h.getIdLojautilizador().getIdUtilizador().getNome()
                                         + " — " + formatarTurno(h)
                    ))
                    .toList();
            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Nao foi possivel carregar os turnos elegiveis."));
        }
    }

    // ── POST /api/permutas/submeter ──────────────────────────────────────────
    /**
     * Submete um pedido de permuta e valida contra o estado_permuta_enum.
     * Retorna 200 com {estado: "pendente", mensagem: "..."} em caso de sucesso.
     * Retorna 422 com {erro: "..."} em caso de violação de regra de negócio.
     */
    @PostMapping("/submeter")
    public ResponseEntity<?> submeterPermuta(@RequestParam("idHorarioOrigem")  Integer idHorarioOrigem,
                                             @RequestParam("idHorarioDestino") Integer idHorarioDestino,
                                             HttpSession session) {
        try {
            Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);

            // Validar inputs
            if (idHorarioOrigem == null || idHorarioDestino == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Seleciona o teu turno e o turno do colega."));
            }

            // Obter entidades validadas
            List<Horario> meusTurnos = horarioBLL.listarMeusTurnosDisponiveisParaPermuta(utilizadorId);
            Horario turnoOrigem = meusTurnos.stream()
                    .filter(h -> idHorarioOrigem.equals(h.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "O turno de origem selecionado nao e valido ou nao te pertence."));

            List<Horario> elegiveis = horarioBLL.listarTurnosElegiveisParaPermuta(utilizadorId, idHorarioOrigem);
            Horario turnoDestino = elegiveis.stream()
                    .filter(h -> idHorarioDestino.equals(h.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "O turno de destino selecionado nao e elegivel para permuta."));

            // Registar — BLL valida todas as regras de negócio e estado_permuta_enum
            Permuta permuta = permutaBLL.registarPedidoTroca(utilizadorId, turnoOrigem, turnoDestino);

            // Garantir que o estado retornado e sempre um valor válido do enum
            EstadoPermuta estado = permuta.getEstado() != null ? permuta.getEstado() : EstadoPermuta.pendente;

            return ResponseEntity.ok(Map.of(
                    "id",      permuta.getId(),
                    "estado",  estado.name(),
                    "mensagem","Pedido de permuta submetido com sucesso. Aguarda a aprovacao do supervisor."
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", "Nao foi possivel submeter o pedido de permuta. Tenta novamente."));
        }
    }

    // ── Utilitário ───────────────────────────────────────────────────────────
    private String formatarTurno(Horario h) {
        if (h == null || h.getDataTurno() == null || h.getIdTurno() == null) {
            return "-";
        }
        String inicio = h.getIdTurno().getHoraInicio() != null ? h.getIdTurno().getHoraInicio().format(FMT_HORA) : "--:--";
        String fim    = h.getIdTurno().getHoraFim()    != null ? h.getIdTurno().getHoraFim().format(FMT_HORA)    : "--:--";
        return h.getDataTurno().format(FMT_DATA) + " · " + inicio + " – " + fim;
    }
}
