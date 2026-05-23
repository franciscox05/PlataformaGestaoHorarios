package com.example.projeto2.API.Modules;

public final class UtilizadorApiModels {

    private UtilizadorApiModels() {
    }

    public record CriarUtilizadorRequest(
            String nome,
            String email,
            String telemovel,
            String password,
            Integer idCargo,
            String estado
    ) {
    }

    public record UtilizadorResponse(
            Integer id,
            String nome,
            String email,
            String telemovel,
            String estado
    ) {
    }

    public record ApiErrorResponse(
            String message
    ) {
    }
}
