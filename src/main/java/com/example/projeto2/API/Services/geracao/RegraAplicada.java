package com.example.projeto2.API.Services.geracao;

import static com.example.projeto2.API.Services.geracao.HorarioFormatters.normalizarTexto;

public record RegraAplicada(
        String descricao,
        String tipo,
        Integer valor
) {
    public String textoNormalizado() {
        return normalizarTexto(descricao + " " + tipo);
    }
}
