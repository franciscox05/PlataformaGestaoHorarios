package com.example.projeto2.API.Controllers;

import com.example.projeto2.API.Modules.UtilizadorApiModels.ApiErrorResponse;
import com.example.projeto2.API.Modules.UtilizadorApiModels.CriarUtilizadorRequest;
import com.example.projeto2.API.Modules.UtilizadorApiModels.UtilizadorResponse;
import com.example.projeto2.API.Services.UtilizadoresApiService;
import com.example.projeto2.WEB.WebAppService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UtilizadoresApiController {

    private final UtilizadoresApiService utilizadoresApiService;
    private final WebAppService webAppService;

    public UtilizadoresApiController(UtilizadoresApiService utilizadoresApiService,
                                     WebAppService webAppService) {
        this.utilizadoresApiService = utilizadoresApiService;
        this.webAppService = webAppService;
    }

    @GetMapping("/{idUtilizador}")
    public UtilizadorResponse obterUtilizador(@PathVariable Integer idUtilizador) {
        return utilizadoresApiService.obterUtilizador(idUtilizador);
    }

    @PostMapping
    public ResponseEntity<UtilizadorResponse> criarUtilizador(
            @RequestHeader(value = "X-Manager-Id", required = false) Integer idGestorHeader,
            HttpSession session,
            @RequestBody CriarUtilizadorRequest request) {
        Integer idGestor = idGestorHeader != null ? idGestorHeader
                                                   : webAppService.obterUtilizadorId(session);
        if (idGestor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UtilizadorResponse response = utilizadoresApiService.criarUtilizador(idGestor, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> tratarErrosDeValidacao(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage()));
    }
}
