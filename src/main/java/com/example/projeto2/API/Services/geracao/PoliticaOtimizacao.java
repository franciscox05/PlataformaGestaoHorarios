package com.example.projeto2.API.Services.geracao;

public enum PoliticaOtimizacao {
    EQUILIBRIO("Equilibrio", "minimizar desvios de carga e evitar concentracao semanal", 4, 1, 2, 2, 2, 0, 0),
    PREFERENCIAS("Preferencias", "maximizar preferencias aprovadas sem violar restricoes legais", 2, 5, 2, 1, 2, 7, 1),
    FINS_DE_SEMANA("Fins de semana", "reforcar rotacao e uso adequado da equipa de fim de semana", 2, 1, 5, 2, 2, 13, 1),
    CARGA_CONTRATUAL("Carga contratual", "aproximar utilizacao de cada contrato ao perfil esperado", 5, 1, 2, 3, 1, 19, 1),
    DIVERSIFICADA("Diversificada", "explorar uma alternativa viavel com desempates diferentes", 3, 2, 3, 2, 4, 29, 2);

    private final String nome;
    private final String descricao;
    private final int pesoEquilibrioCarga;
    private final int pesoPreferencias;
    private final int pesoFinsDeSemana;
    private final int pesoReservaOperacional;
    private final int pesoTurnoRepetido;
    private final int sementeDiversificacao;
    private final int pesoDiversificacao;

    PoliticaOtimizacao(String nome,
                       String descricao,
                       int pesoEquilibrioCarga,
                       int pesoPreferencias,
                       int pesoFinsDeSemana,
                       int pesoReservaOperacional,
                       int pesoTurnoRepetido,
                       int sementeDiversificacao,
                       int pesoDiversificacao) {
        this.nome = nome;
        this.descricao = descricao;
        this.pesoEquilibrioCarga = pesoEquilibrioCarga;
        this.pesoPreferencias = pesoPreferencias;
        this.pesoFinsDeSemana = pesoFinsDeSemana;
        this.pesoReservaOperacional = pesoReservaOperacional;
        this.pesoTurnoRepetido = pesoTurnoRepetido;
        this.sementeDiversificacao = sementeDiversificacao;
        this.pesoDiversificacao = pesoDiversificacao;
    }

    public static PoliticaOtimizacao porIndice(int indice) {
        PoliticaOtimizacao[] valores = values();
        return valores[Math.floorMod(indice, valores.length)];
    }

    public String nome() { return nome; }
    public String descricao() { return descricao; }
    public int pesoEquilibrioCarga() { return pesoEquilibrioCarga; }
    public int pesoPreferencias() { return pesoPreferencias; }
    public int pesoFinsDeSemana() { return pesoFinsDeSemana; }
    public int pesoReservaOperacional() { return pesoReservaOperacional; }
    public int pesoTurnoRepetido() { return pesoTurnoRepetido; }
    public int sementeDiversificacao() { return sementeDiversificacao; }
    public int pesoDiversificacao() { return pesoDiversificacao; }
}
