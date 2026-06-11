package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.API.Services.PainelGerenteService;
import com.example.projeto2.API.Services.PermutaService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Gere os painéis de pedidos da home-view:
 * <ul>
 *   <li>Banner de pendentes (para gestores)</li>
 *   <li>Meus pedidos recentes (para colaboradores)</li>
 *   <li>Pedidos pendentes a decidir (para gestores)</li>
 * </ul>
 *
 * <p>Extraído de {@code HomeController} para isolar a lógica de construção de
 * UI dinâmica e as chamadas a serviços de pedidos.
 */
public final class HomePedidosHelper {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORARIO_COMPACTO = DateTimeFormatter.ofPattern("HH:mm");

    // Nós FXML do banner
    private final HBox bannerPendentes;
    private final Label lblBannerPendentes;

    // Nós FXML de meus pedidos
    private final VBox painelMeusPedidos;
    private final VBox listaMeusPedidos;

    // Nós FXML de pedidos pendentes (gestão)
    private final VBox painelPedidosPendentes;
    private final VBox listaPedidosPendentes;
    private final Label lblPedidosPendentesSub;

    // Serviços
    private final DayOffService dayOffBLL;
    private final PermutaService permutaBLL;
    private final PainelGerenteService painelGerenteBLL;
    private final GestaoLojaService gestaoLojaBLL;

    private final Supplier<Window> janelaSupplier;

    /** Utilizado no refresh interno após aprovar/rejeitar um pedido. */
    private Utilizador utilizadorAtual;

    public HomePedidosHelper(HBox bannerPendentes,
                              Label lblBannerPendentes,
                              VBox painelMeusPedidos,
                              VBox listaMeusPedidos,
                              VBox painelPedidosPendentes,
                              VBox listaPedidosPendentes,
                              Label lblPedidosPendentesSub,
                              DayOffService dayOffBLL,
                              PermutaService permutaBLL,
                              PainelGerenteService painelGerenteBLL,
                              GestaoLojaService gestaoLojaBLL,
                              Supplier<Window> janelaSupplier) {
        this.bannerPendentes = bannerPendentes;
        this.lblBannerPendentes = lblBannerPendentes;
        this.painelMeusPedidos = painelMeusPedidos;
        this.listaMeusPedidos = listaMeusPedidos;
        this.painelPedidosPendentes = painelPedidosPendentes;
        this.listaPedidosPendentes = listaPedidosPendentes;
        this.lblPedidosPendentesSub = lblPedidosPendentesSub;
        this.dayOffBLL = dayOffBLL;
        this.permutaBLL = permutaBLL;
        this.painelGerenteBLL = painelGerenteBLL;
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

    // ── Pedidos pendentes a decidir (gestores) ───────────────────────────────

    public void carregarPedidosPendentes(Utilizador utilizadorLogado) {
        if (painelPedidosPendentes == null || listaPedidosPendentes == null
                || utilizadorLogado == null || utilizadorLogado.getId() == null) return;

        this.utilizadorAtual = utilizadorLogado;
        try {
            PainelGerenteService.PainelGerenteSnapshot snapshot =
                    painelGerenteBLL.carregarPainel(utilizadorLogado.getId());
            listaPedidosPendentes.getChildren().clear();

            int total = snapshot.resumo().totalPendentes();
            if (lblPedidosPendentesSub != null) {
                lblPedidosPendentesSub.setText(total == 1 ? "1 por decidir" : total + " por decidir");
            }

            if (total == 0) {
                Label vazio = new Label("Sem pedidos pendentes. Está tudo em dia.");
                vazio.getStyleClass().add("home-card-subtitle");
                vazio.setWrapText(true);
                listaPedidosPendentes.getChildren().add(vazio);
            } else {
                adicionarLinhasFolgas(snapshot, utilizadorLogado);
                adicionarLinhasPermutas(snapshot, utilizadorLogado);
                adicionarLinhasPreferencias(snapshot, utilizadorLogado);
            }

            painelPedidosPendentes.setVisible(true);
            painelPedidosPendentes.setManaged(true);
        } catch (Exception e) {
            painelPedidosPendentes.setVisible(false);
            painelPedidosPendentes.setManaged(false);
        }
    }

    private void adicionarLinhasFolgas(PainelGerenteService.PainelGerenteSnapshot snapshot,
                                        Utilizador utilizadorLogado) {
        for (DayOff folga : snapshot.folgasPendentes()) {
            Integer idUtil = folga.getIdUtilizador() != null ? folga.getIdUtilizador().getId() : null;
            String nome = snapshot.nomesFolgasPendentes().getOrDefault(idUtil,
                    folga.getIdUtilizador() != null ? folga.getIdUtilizador().getNome() : "?");
            String detalhe = formatarTipoDayOff(folga.getTipo())
                    + (folga.getDataAusencia() != null
                            ? " · " + DATA_FORMATTER.format(folga.getDataAusencia()) : "");
            listaPedidosPendentes.getChildren().add(construirLinhaPedidoPendente(
                    "Folga", "folga", nome, "pediu folga", detalhe, folga.getMotivo(),
                    () -> painelGerenteBLL.aprovarFolga(folga.getIdDayoff(), utilizadorLogado.getId()),
                    () -> painelGerenteBLL.rejeitarFolga(folga.getIdDayoff(), utilizadorLogado.getId())));
        }
    }

    private void adicionarLinhasPermutas(PainelGerenteService.PainelGerenteSnapshot snapshot,
                                          Utilizador utilizadorLogado) {
        for (Permuta permuta : snapshot.permutasPendentes()) {
            String origemNome = nomeColaboradorDe(permuta.getIdHorarioOrigem());
            String detalhe = descricaoPermuta(permuta);
            listaPedidosPendentes.getChildren().add(construirLinhaPedidoPendente(
                    "Permuta", "permuta", origemNome, "quer trocar turno", detalhe, null,
                    () -> painelGerenteBLL.aprovarPermuta(permuta.getId(), utilizadorLogado.getId()),
                    () -> painelGerenteBLL.rejeitarPermuta(permuta.getId(), utilizadorLogado.getId())));
        }
    }

    private void adicionarLinhasPreferencias(PainelGerenteService.PainelGerenteSnapshot snapshot,
                                              Utilizador utilizadorLogado) {
        for (Preferencia pref : snapshot.preferenciasPendentes()) {
            String nome = pref.getIdUtilizador() != null ? pref.getIdUtilizador().getNome() : "?";
            String detalhe = pref.getDescricao() != null && !pref.getDescricao().isBlank()
                    ? pref.getDescricao() : capitalizar(pref.getTipo());
            listaPedidosPendentes.getChildren().add(construirLinhaPedidoPendente(
                    "Preferência", "pref", nome, "atualizou preferências", detalhe, null,
                    () -> painelGerenteBLL.aprovarPreferencia(pref.getId(), utilizadorLogado.getId(), ""),
                    () -> painelGerenteBLL.rejeitarPreferencia(pref.getId(), utilizadorLogado.getId(), "")));
        }
    }

    private HBox construirLinhaPedidoPendente(String tag, String chaveTag, String nome, String acao,
                                               String detalhe, String motivo,
                                               Runnable aprovar, Runnable rejeitar) {
        HBox linha = new HBox(10);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.getStyleClass().add("pedido-resumo-linha");
        linha.setPadding(new Insets(10, 12, 10, 12));

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("grelha-avatar");
        avatar.setStyle("-fx-background-color: " + corPorTag(chaveTag) + ";");
        Label lblIniciais = new Label(iniciaisDe(nome));
        lblIniciais.getStyleClass().add("grelha-avatar-iniciais");
        avatar.getChildren().add(lblIniciais);

        VBox corpo = new VBox(2);
        HBox.setHgrow(corpo, Priority.ALWAYS);
        corpo.setMaxWidth(Double.MAX_VALUE);

        Label lblTag = new Label(tag.toUpperCase(Locale.ROOT));
        lblTag.getStyleClass().add("pedido-resumo-tipo");
        Label lblTexto = new Label(nome + " " + acao);
        lblTexto.getStyleClass().add("pedido-resumo-tipo");
        lblTexto.setWrapText(true);
        corpo.getChildren().addAll(lblTag, lblTexto);

        if (detalhe != null && !detalhe.isBlank()) {
            Label lblDetalhe = new Label(detalhe);
            lblDetalhe.getStyleClass().add("pedido-resumo-data");
            lblDetalhe.setWrapText(true);
            corpo.getChildren().add(lblDetalhe);
        }
        if (motivo != null && !motivo.isBlank()) {
            Label lblMotivo = new Label(motivo);
            lblMotivo.getStyleClass().add("home-card-subtitle");
            lblMotivo.setWrapText(true);
            corpo.getChildren().add(lblMotivo);
        }

        Button btnOk = new Button("✓");
        btnOk.getStyleClass().add("home-pedido-aprovar");
        btnOk.setStyle("-fx-background-color:#dcfce7; -fx-text-fill:#166534; -fx-font-weight:800; "
                + "-fx-background-radius:8; -fx-min-width:34; -fx-min-height:34; -fx-cursor:hand;");
        btnOk.setOnAction(e -> decidirPedido(tag, nome, true, aprovar));

        Button btnNo = new Button("✕");
        btnNo.getStyleClass().add("home-pedido-rejeitar");
        btnNo.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#991b1b; -fx-font-weight:800; "
                + "-fx-background-radius:8; -fx-min-width:34; -fx-min-height:34; -fx-cursor:hand;");
        btnNo.setOnAction(e -> decidirPedido(tag, nome, false, rejeitar));

        HBox acoes = new HBox(6, btnOk, btnNo);
        acoes.setAlignment(Pos.CENTER_RIGHT);

        linha.getChildren().addAll(avatar, corpo, acoes);
        return linha;
    }

    private void decidirPedido(String tipoLabel, String nome, boolean aprovar, Runnable accao) {
        boolean confirmado = DialogosHelper.confirmarAcao(
                janelaSupplier.get(),
                (aprovar ? "Aprovar " : "Rejeitar ") + tipoLabel.toLowerCase(Locale.ROOT),
                (aprovar ? "Confirmas a aprovação" : "Confirmas a rejeição")
                        + " do pedido de " + nome + "?",
                "O estado do pedido será atualizado e guardado na base de dados.");
        if (!confirmado) return;
        try {
            accao.run();
            carregarPedidosPendentes(utilizadorAtual);
            atualizarBannerPendentes(utilizadorAtual);
        } catch (Exception e) {
            DialogosHelper.mostrarErro(janelaSupplier.get(),
                    "Não foi possível concluir",
                    "Ocorreu um erro ao processar o pedido.",
                    e.getMessage() != null ? e.getMessage() : "Tenta novamente dentro de momentos.");
        }
    }

    // ── Formatação e helpers de UI ───────────────────────────────────────────

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

    private static String nomeColaboradorDe(Horario horario) {
        if (horario == null || horario.getIdLojautilizador() == null
                || horario.getIdLojautilizador().getIdUtilizador() == null) return "?";
        String nome = horario.getIdLojautilizador().getIdUtilizador().getNome();
        return nome != null ? nome : "-";
    }

    private static String descricaoPermuta(Permuta permuta) {
        if (permuta == null || permuta.getIdHorarioOrigem() == null) return "Troca de turno";
        String periodo = formatarPeriodoHorario(permuta.getIdHorarioOrigem());
        String data = permuta.getIdHorarioOrigem().getDataTurno() != null
                ? DATA_FORMATTER.format(permuta.getIdHorarioOrigem().getDataTurno()) : "";
        String destino = nomeColaboradorDe(permuta.getIdHorarioDestino());
        String origem = (periodo != null && !"-".equals(periodo) ? periodo + " " : "") + data;
        return (origem.isBlank() ? "Turno" : origem.trim()) + " ⇄ " + destino;
    }

    private static String formatarPeriodoHorario(Horario horario) {
        if (horario == null || horario.getIdTurno() == null) return "-";
        String inicio = horario.getIdTurno().getHoraInicio() != null
                ? horario.getIdTurno().getHoraInicio().format(HORARIO_COMPACTO) : "--:--";
        String fim = horario.getIdTurno().getHoraFim() != null
                ? horario.getIdTurno().getHoraFim().format(HORARIO_COMPACTO) : "--:--";
        return inicio + " - " + fim;
    }

    private static String corPorTag(String chave) {
        return switch (chave) {
            case "folga"   -> "#d97706";
            case "permuta" -> "#7c3aed";
            case "pref"    -> "#0891b2";
            default        -> "#6b7280";
        };
    }

    private static String iniciaisDe(String nome) {
        if (nome == null || nome.isBlank()) return "?";
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase(Locale.ROOT);
        }
        return (String.valueOf(partes[0].charAt(0))
                + partes[partes.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "-";
        String valor = texto.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }

    private record PedidoResumo(String tipo, String data, String estado, Instant dataOrdem) {}
}
