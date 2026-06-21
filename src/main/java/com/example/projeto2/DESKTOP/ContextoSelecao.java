package com.example.projeto2.DESKTOP;

/**
 * Origem da navegação para o ecrã de seleção de loja
 * ({@code selecionar-loja-view.fxml} / {@link SelecionarLojaController}).
 *
 * <p>Determina o comportamento do botão de recuo desse ecrã, eliminando os
 * dois "becos sem saída" do fluxo multi-loja:
 * <ul>
 *   <li>{@link #LOGIN} — o utilizador chegou aqui imediatamente após
 *       autenticar. O botão de recuo deve devolvê-lo ao ecrã de login (ainda
 *       não existe sessão trancada, logo é apenas uma troca de scene).</li>
 *   <li>{@link #DASHBOARD} — o utilizador já estava dentro do painel e pediu
 *       para alternar de loja. O botão de recuo deve cancelar a troca e
 *       devolvê-lo ao painel <b>sem</b> perder a sessão nem a loja já ativa.</li>
 * </ul>
 */
public enum ContextoSelecao {
    LOGIN,
    DASHBOARD
}
