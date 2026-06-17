package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.API.Services.PermutaService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class HomePedidosHelper {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORARIO_COMPACTO = DateTimeFormatter.ofPattern("HH:mm");

    private final HBox bannerPendentes;
    private final Label lblBannerPendentes;
    private final VBox painelMeusPedidos;
    private final VBox listaMeusPedidos;

    private final DayOffService dayOffBLL;
    private final PermutaService permutaBLL;
    private final GestaoLojaService gestaoLojaBLL;

    private final Supplier<Window> janelaSupplier;

    public HomePedidosHelper(HBox bannerPendentes,
                              Label lblBannerPendentes,
                              VBox painelMeusPedidos,
                              VBox listaMeusPedidos,
                              DayOffService dayOffBLL,
                              PermutaService permutaBLL,
                              GestaoLojaService gestaoLojaBLL,
                              Supplier<Window> janelaSupplier) {
        this.bannerPendentes = bannerPendentes;
        this.lblBannerPendentes = lblBannerPendentes;
        this.painelMeusPedidos = painelMeusPedidos;
        this.listaMeusPedidos = listaMeusPedidos;
        this.dayOffBLL = dayOffBLL;
        this.permutaBLL = permutaBLL;
        this.gestaoLojaBLL = gestaoLojaBLL;
        this.janelaSupplier = janelaSupplier;
    }

    // ── Banner de pendentes ──────────────────────────────────────────────────

    public void atualizarBannerPendentes(Utilizador utilizadorLogado) {
        if (bannerPendentes == null || utilizadorLogado == null) return;
        try {
            int total = dayOffBLL.listarPedidosPendentesParaAprovacao(utilizadorLogado.getId()).size()
                    + permutaBLL.listarPedidosPendentesParaAprovacao(utilizadorLogado.getId()).size();
            if (total > 0) {
                String msg = total == 1
                        ? "Tens 1 pedido pendente a aguardar a tua aprovação."
                        : "Tens " + total + " pedidos pendentes a aguardar a tua aprovação.";
                if (lblBannerPendentes != null) lblBannerPendentes.setText(msg);
                bannerPendentes.setVisible(true);
                bannerPendentes.setManaged(true);
            } else {
                esconderBannerPendentes();
            }
        } catch (Exception e) {
            esconderBannerPendentes();
        }
    }

    public void esconderBannerPendentes() {
        if (bannerPendentes == null) return;
        bannerPendentes.setVisible(false);
        bannerPendentes.setManaged(false);
    }

    // ── Meus Pedidos (colaboradores) ─────────────────────────────────────────

    public void carregarMeusPedidos(Utilizador utilizadorLogado) {
        if (painelMeusPedidos == null || listaMeusPedidos == null) return;

        if (utilizadorLogado == null
                || gestaoLojaBLL.utilizadorPodeGerirLoja(utilizadorLogado.getId())) {
            painelMeusPedidos.setVisible(false);
            painelMeusPedidos.setManaged(false);
            return;
        }

        try {
            List<PedidoResumo> pedidos = new ArrayList<>();

            dayOffBLL.listarPedidosPorUtilizador(utilizadorLogado.getId()).stream()
                    .limit(10)
                    .forEach(dayOff -> pedidos.add(new PedidoResumo(
                            "Folga / " + formatarTipoDayOff(dayOff.getTipo()),
                            dayOff.getDataAusencia() != null
                                    ? DATA_FORMATTER.format(dayOff.getDataAusencia()) : "-",
                            dayOff.getEstado() != null ? dayOff.getEstado() : "pendente",
                            dayOff.getDataAusencia() != null
                                    ? dayOff.getDataAusencia().atStartOfDay()
                                            .toInstant(java.time.ZoneOffset.UTC)
                                    : Instant.MIN
                    )));

            permutaBLL.listarPedidosEnviados(utilizadorLogado.getId()).stream()
                    .limit(10)
                    .forEach(permuta -> {
                        String dataFormatada = permuta.getIdHorarioOrigem() != null
                                && permuta.getIdHorarioOrigem().getDataTurno() != null
                                ? DATA_FORMATTER.format(permuta.getIdHorarioOrigem().getDataTurno())
                                : "-";
                        String estado = permuta.getEstado() != null
                                ? permuta.getEstado().name() : "pendente";
                        Instant dataOrdem = permuta.getDataPedido() != null
                                ? permuta.getDataPedido() : Instant.MIN;
                        pedidos.add(new PedidoResumo("Troca de turno", dataFormatada, estado, dataOrdem));
                    });

            List<PedidoResumo> recentes = pedidos.stream()
                    .sorted(Comparator.comparing(PedidoResumo::dataOrdem, Comparator.reverseOrder()))
                    .limit(5)
                    .toList();

            listaMeusPedidos.getChildren().clear();
            if (recentes.isEmpty()) {
                Label lblVazio = new Label(
                        "Ainda não tens pedidos registados. Usa os atalhos acima para pedir folga ou trocar turno.");
                lblVazio.getStyleClass().add("home-card-subtitle");
                lblVazio.setWrapText(true);
                listaMeusPedidos.getChildren().add(lblVazio);
            } else {
                for (PedidoResumo pedido : recentes) {
                    listaMeusPedidos.getChildren().add(criarLinhaPedido(pedido));
                }
            }
            painelMeusPedidos.setVisible(true);
            painelMeusPedidos.setManaged(true);
        } catch (Exception e) {
            painelMeusPedidos.setVisible(false);
            painelMeusPedidos.setManaged(false);
        }
    }

    private HBox criarLinhaPedido(PedidoResumo pedido) {
        HBox linha = new HBox(12);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.getStyleClass().add("pedido-resumo-linha");
        linha.setPadding(new Insets(8, 12, 8, 12));

        Label lblTipo = new Label(pedido.tipo());
        lblTipo.getStyleClass().add("pedido-resumo-tipo");
        HBox.setHgrow(lblTipo, Priority.ALWAYS);
        lblTipo.setMaxWidth(Double.MAX_VALUE);

        Label lblData = new Label(pedido.data());
        lblData.getStyleClass().add("pedido-resumo-data");

        Label lblEstado = new Label(formatarEstadoPedido(pedido.estado()));
        lblEstado.getStyleClass().addAll("pedido-resumo-badge", resolverCssBadge(pedido.estado()));

        linha.getChildren().addAll(lblTipo, lblData, lblEstado);
        return linha;
    }

    // ── Formatação ───────────────────────────────────────────────────────────

    private static String formatarTipoDayOff(String tipo) {
        if (tipo == null) return "Ausência";
        return switch (tipo.toLowerCase(Locale.ROOT)) {
            case "ferias"  -> "Férias";
            case "folgas"  -> "Folgas";
            case "baixa"   -> "Baixa";
            case "urgente" -> "Urgente";
            default        -> capitalizar(tipo);
        };
    }

    private static String formatarEstadoPedido(String estado) {
        if (estado == null) return "Pendente";
        return switch (estado.toLowerCase(Locale.ROOT)) {
            case "pendente"          -> "Pendente";
            case "aprovado"          -> "Aprovado";
            case "rejeitado",
                 "recusado"          -> "Rejeitado";
            default                  -> capitalizar(estado);
        };
    }

    private static String resolverCssBadge(String estado) {
        if (estado == null) return "badge-pendente";
        return switch (estado.toLowerCase(Locale.ROOT)) {
            case "aprovado"          -> "badge-aprovado";
            case "rejeitado",
                 "recusado"          -> "badge-rejeitado";
            default                  -> "badge-pendente";
        };
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "-";
        String valor = texto.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }

    private record PedidoResumo(String tipo, String data, String estado, Instant dataOrdem) {}
}
