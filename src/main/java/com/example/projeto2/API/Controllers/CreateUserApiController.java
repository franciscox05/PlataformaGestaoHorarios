package com.example.projeto2.API.Controllers;

import com.example.projeto2.API.Modules.UtilizadorApiModels.ApiErrorResponse;
import com.example.projeto2.API.Modules.UtilizadorApiModels.CriarUtilizadorRequest;
import com.example.projeto2.API.Modules.UtilizadorApiModels.UtilizadorResponse;
import com.example.projeto2.API.Services.UtilizadoresApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CreateUserApiController {

    private final UtilizadoresApiService utilizadoresApiService;

    public CreateUserApiController(UtilizadoresApiService utilizadoresApiService) {
        this.utilizadoresApiService = utilizadoresApiService;
    }

    @PostMapping("/create-user")
    public ResponseEntity<UtilizadorResponse> criarUtilizador(
            @RequestHeader("X-Manager-Id") Integer idGestor,
            @RequestBody CriarUtilizadorRequest request) {
        UtilizadorResponse response = utilizadoresApiService.criarUtilizador(idGestor, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> tratarErrosDeValidacao(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage()));
    }
}
