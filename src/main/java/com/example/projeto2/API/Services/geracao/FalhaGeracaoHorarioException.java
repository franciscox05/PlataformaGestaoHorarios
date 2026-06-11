package com.example.projeto2.API.Services.geracao;

import java.util.List;

/**
 * Falha de cobertura na geração de horários: além da mensagem, transporta o
 * diagnóstico estruturado (turno e data críticos, número de colaboradores
 * considerados, motivos agregados e sugestões acionáveis) para que a interface
 * possa explicar ao gestor <em>porquê</em> a geração não foi possível.
 */
public class FalhaGeracaoHorarioException extends IllegalArgumentException {

    private final String turno;
    private final String data;
    private final int colaboradoresConsiderados;
    private final String motivoPrincipal;
    private final List<MotivoFalhaGeracao> motivos;
    private final List<SugestaoFalhaGeracao> sugestoes;

    public FalhaGeracaoHorarioException(String mensagem,
                                        String turno,
                                        String data,
                                        int colaboradoresConsiderados,
                                        String motivoPrincipal,
                                        List<MotivoFalhaGeracao> motivos,
                                        List<SugestaoFalhaGeracao> sugestoes) {
        super(mensagem);
        this.turno = turno;
        this.data = data;
        this.colaboradoresConsiderados = colaboradoresConsiderados;
        this.motivoPrincipal = motivoPrincipal;
        this.motivos = motivos != null ? List.copyOf(motivos) : List.of();
        this.sugestoes = sugestoes != null ? List.copyOf(sugestoes) : List.of();
    }

    public String turno() { return turno; }

    public String data() { return data; }

    public int colaboradoresConsiderados() { return colaboradoresConsiderados; }

    public String motivoPrincipal() { return motivoPrincipal; }

    public List<MotivoFalhaGeracao> motivos() { return motivos; }

    public List<SugestaoFalhaGeracao> sugestoes() { return sugestoes; }
}
