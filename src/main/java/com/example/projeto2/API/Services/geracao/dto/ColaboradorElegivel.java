package com.example.projeto2.API.Services.geracao.dto;

/** Colaborador elegível para a geração, com o seu perfil contratual e vínculo. */
public record ColaboradorElegivel(
        Integer idColaborador,
        String nome,
        String cargo,
        String perfilContratual,
        String periodoVinculo,
        boolean selecionadoPorDefeito
) {
    @Override
    public String toString() {
        return nome + " - " + cargo;
    }
}
