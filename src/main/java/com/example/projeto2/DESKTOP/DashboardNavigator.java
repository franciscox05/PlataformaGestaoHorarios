package com.example.projeto2.DESKTOP;

public interface DashboardNavigator {

    void abrirDashboard();

    void abrirFolgas();

    void abrirPermutas();

    void abrirPerfil();

    void abrirPreferencias();

    void abrirPainelGerente();

    /** Abre o painel de Pedidos já com a aba do tipo indicado selecionada (folga/permuta/preferência). */
    void abrirPainelGerente(String abaInicial);

    void abrirHorarios();

    void abrirRelatorios();

    void atualizarBadges();
}
