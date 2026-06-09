package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.GeracaoHorariosService;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Painel de seleção de colaboradores para geração de horários. Encapsula:
 * <ul>
 *   <li>O agrupamento dos colaboradores elegíveis por cargo, com checkbox de grupo</li>
 *   <li>A preservação da seleção entre carregamentos</li>
 *   <li>A atualização da label-resumo a cada clique</li>
 *   <li>A notificação ao controller (callback) quando a seleção muda</li>
 * </ul>
 */
public final class SelecaoColaboradoresPainel {

    private final VBox box;
    private final Label lblResumo;
    private final Runnable onSelecaoMudou;
    private final Map<Integer, CheckBox> checks = new LinkedHashMap<>();

    public SelecaoColaboradoresPainel(VBox box, Label lblResumo, Runnable onSelecaoMudou) {
        this.box = box;
        this.lblResumo = lblResumo;
        this.onSelecaoMudou = onSelecaoMudou;
    }

    /** Redenseña o painel com a equipa elegível, preservando seleção anterior se possível. */
    public void mostrar(List<GeracaoHorariosService.ColaboradorElegivel> colaboradores) {
        Set<Integer> selecionadosAnteriores = idsSelecionadosComoSet();
        boolean preservar = !checks.isEmpty();

        checks.clear();
        box.getChildren().clear();

        if (colaboradores == null || colaboradores.isEmpty()) {
            Label vazio = new Label("Sem colaboradores elegíveis para o período selecionado.");
            vazio.getStyleClass().add("texto-ajuda");
            box.getChildren().add(vazio);
            atualizarResumo();
            return;
        }

        for (Map.Entry<String, List<GeracaoHorariosService.ColaboradorElegivel>> grupo
                : agruparPorCargo(colaboradores).entrySet()) {
            box.getChildren().add(construirBlocoGrupo(grupo.getKey(), grupo.getValue(),
                    preservar, selecionadosAnteriores));
        }
        atualizarResumo();
    }

    /** Mostra uma mensagem de ajuda (sem colaboradores carregados). */
    public void mostrarMensagem(String mensagemResumo) {
        checks.clear();
        box.getChildren().clear();
        if (lblResumo != null) lblResumo.setText(mensagemResumo);
        if (onSelecaoMudou != null) onSelecaoMudou.run();
    }

    public void selecionarTodos() {
        checks.values().forEach(cb -> cb.setSelected(true));
        atualizarResumo();
    }

    public void limparSelecao() {
        checks.values().forEach(cb -> cb.setSelected(false));
        atualizarResumo();
    }

    public boolean isVazio() {
        return checks.isEmpty();
    }

    public List<Integer> idsSelecionados() {
        return new ArrayList<>(idsSelecionadosComoSet());
    }

    public Set<Integer> idsSelecionadosComoSet() {
        Set<Integer> ids = new LinkedHashSet<>();
        for (Map.Entry<Integer, CheckBox> entrada : checks.entrySet()) {
            if (entrada.getValue().isSelected()) ids.add(entrada.getKey());
        }
        return ids;
    }

    private VBox construirBlocoGrupo(String cargo,
                                      List<GeracaoHorariosService.ColaboradorElegivel> colaboradores,
                                      boolean preservar,
                                      Set<Integer> selecionadosAnteriores) {
        VBox boxGrupo = new VBox(6);
        boxGrupo.getStyleClass().add("grupo-colaboradores");

        CheckBox checkGrupo = new CheckBox(cargo + " (" + colaboradores.size() + ")");
        checkGrupo.getStyleClass().add("grupo-colaboradores-titulo");
        boxGrupo.getChildren().add(checkGrupo);

        VBox boxItens = new VBox(5);
        boxItens.getStyleClass().add("grupo-colaboradores-itens");
        List<CheckBox> checksGrupo = new ArrayList<>();
        for (GeracaoHorariosService.ColaboradorElegivel c : colaboradores) {
            CheckBox cb = criarCheckBox(c, preservar, selecionadosAnteriores);
            checksGrupo.add(cb);
            boxItens.getChildren().add(cb);
            checks.put(c.idColaborador(), cb);
        }

        checkGrupo.setOnAction(e -> checksGrupo.forEach(cb -> cb.setSelected(checkGrupo.isSelected())));
        checksGrupo.forEach(cb -> cb.selectedProperty().addListener(
                (obs, antigo, novo) -> atualizarEstadoCheckGrupo(checkGrupo, checksGrupo)));
        atualizarEstadoCheckGrupo(checkGrupo, checksGrupo);

        boxGrupo.getChildren().add(boxItens);
        return boxGrupo;
    }

    private CheckBox criarCheckBox(GeracaoHorariosService.ColaboradorElegivel c,
                                    boolean preservar,
                                    Set<Integer> selecionadosAnteriores) {
        CheckBox cb = new CheckBox(c.nome() + " | " + c.perfilContratual());
        cb.getStyleClass().add("colaborador-check");
        cb.setWrapText(true);
        cb.setSelected(preservar
                ? selecionadosAnteriores.contains(c.idColaborador())
                : c.selecionadoPorDefeito());
        cb.selectedProperty().addListener((obs, antigo, novo) -> atualizarResumo());
        return cb;
    }

    private static void atualizarEstadoCheckGrupo(CheckBox checkGrupo, List<CheckBox> checksGrupo) {
        long selecionados = checksGrupo.stream().filter(CheckBox::isSelected).count();
        checkGrupo.setIndeterminate(selecionados > 0 && selecionados < checksGrupo.size());
        checkGrupo.setSelected(selecionados == checksGrupo.size() && !checksGrupo.isEmpty());
    }

    private void atualizarResumo() {
        if (lblResumo != null) {
            int total = checks.size();
            int selecionados = idsSelecionadosComoSet().size();
            lblResumo.setText(total == 0
                    ? "Escolhe o período para carregar a equipa elegível."
                    : selecionados + " de " + total + " colaboradores selecionados para a geração.");
        }
        if (onSelecaoMudou != null) onSelecaoMudou.run();
    }

    private static Map<String, List<GeracaoHorariosService.ColaboradorElegivel>> agruparPorCargo(
            List<GeracaoHorariosService.ColaboradorElegivel> colaboradores) {
        Map<String, List<GeracaoHorariosService.ColaboradorElegivel>> grupos = new LinkedHashMap<>();
        colaboradores.stream()
                .sorted(Comparator
                        .comparing(GeracaoHorariosService.ColaboradorElegivel::cargo,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GeracaoHorariosService.ColaboradorElegivel::nome,
                                String.CASE_INSENSITIVE_ORDER))
                .forEach(c -> grupos.computeIfAbsent(c.cargo(), k -> new ArrayList<>()).add(c));
        return grupos;
    }
}
