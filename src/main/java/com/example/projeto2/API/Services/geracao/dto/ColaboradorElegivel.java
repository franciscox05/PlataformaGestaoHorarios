package com.example.projeto2.API.Services.geracao.dto;

/** Colaborador elegível para a geração, com o seu perfil contratual e vínculo. */
public record ColaboradorElegivel(
        Integer idColaborador,
        String nome,
        String cargo,
        String perfilContratual,
        String periodoVinculo,
        boolean selecionadoPorDefeito,
        boolean temTurnosNoutraLoja
) {
    /** Construtor de compatibilidade sem flag de conflito de loja. */
    public ColaboradorElegivel(Integer idColaborador, String nome, String cargo,
                               String perfilContratual, String periodoVinculo,
                               boolean selecionadoPorDefeito) {
        this(idColaborador, nome, cargo, perfilContratual, periodoVinculo, selecionadoPorDefeito, false);
    }

    @Override
    public String toString() {
        return nome + " - " + cargo;
    }
}
