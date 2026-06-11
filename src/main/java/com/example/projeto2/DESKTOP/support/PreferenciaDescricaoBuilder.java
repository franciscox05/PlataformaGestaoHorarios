package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Preferencia;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Encapsula a construção e leitura de descrições estruturadas de preferências
 * de turnos e colegas, e o preenchimento reverso dos controlos do formulário.
 */
public final class PreferenciaDescricaoBuilder {

    private final ComboBox<String> cbColegaPreferido;
    private final CheckBox chkTurnoManha;
    private final CheckBox chkTurnoIntermedio;
    private final CheckBox chkTurnoNoite;
    private final ComboBox<String> cbDuracaoPreferida;

    public PreferenciaDescricaoBuilder(
            ComboBox<String> cbColegaPreferido,
            CheckBox chkTurnoManha,
            CheckBox chkTurnoIntermedio,
            CheckBox chkTurnoNoite,
            ComboBox<String> cbDuracaoPreferida) {
        this.cbColegaPreferido  = cbColegaPreferido;
        this.chkTurnoManha      = chkTurnoManha;
        this.chkTurnoIntermedio = chkTurnoIntermedio;
        this.chkTurnoNoite      = chkTurnoNoite;
        this.cbDuracaoPreferida = cbDuracaoPreferida;
    }

    // ── Description building ────────────────────────────────────────────────────

    public String construirDescricaoFinal(String tipoNormalizado, String textoLivre) {
        if ("colegas".equals(tipoNormalizado)) {
            String colega = cbColegaPreferido.getValue();
            if (colega == null || colega.isBlank()) {
                throw new IllegalArgumentException("Seleciona o colega com quem queres trabalhar.");
            }
            return textoLivre == null
                    ? "Colega preferido: " + colega + "."
                    : "Colega preferido: " + colega + ". Nota adicional: " + textoLivre;
        }

        if ("turnos".equals(tipoNormalizado)) {
            return construirDescricaoEstruturadaTurnos(textoLivre);
        }

        if (textoLivre == null) {
            throw new IllegalArgumentException("Indica uma descrição para a preferência.");
        }
        return textoLivre;
    }

    private String construirDescricaoEstruturadaTurnos(String textoLivre) {
        Set<String> turnosPreferidos = new LinkedHashSet<>();
        if (chkTurnoManha.isSelected())      turnosPreferidos.add("manha/abertura");
        if (chkTurnoIntermedio.isSelected())  turnosPreferidos.add("intermedio/tarde");
        if (chkTurnoNoite.isSelected())       turnosPreferidos.add("noite/fecho");

        if (turnosPreferidos.isEmpty()) {
            turnosPreferidos.addAll(inferirTurnosAPartirDoContexto(textoLivre));
        }
        if (turnosPreferidos.isEmpty()) {
            throw new IllegalArgumentException("Seleciona pelo menos um bloco de turnos preferido.");
        }

        StringBuilder sb = new StringBuilder("Turnos preferidos: ");
        sb.append(String.join(", ", turnosPreferidos)).append(".");

        String duracao = resolverDuracaoPreferidaEstruturada();
        if (duracao == null) duracao = inferirDuracaoAPartirDoContexto(textoLivre);
        if (duracao != null) sb.append(" Duração preferida: ").append(duracao).append(".");
        if (textoLivre != null) sb.append(" Nota adicional: ").append(textoLivre);

        return sb.toString();
    }

    private Set<String> inferirTurnosAPartirDoContexto(String textoLivre) {
        Set<String> inferidos = new LinkedHashSet<>();
        String n = PreferenciaFormatters.normalizarTexto(textoLivre);
        if (n.isBlank()) return inferidos;

        if (n.contains("manha") || n.contains("abertura") || n.contains("cedo"))
            inferidos.add("manha/abertura");
        if (n.contains("intermedio") || n.contains("tarde") || n.contains("meio dia") || n.contains("meio-dia"))
            inferidos.add("intermedio/tarde");
        if (n.contains("noite") || n.contains("fecho") || n.contains("encerramento"))
            inferidos.add("noite/fecho");

        return inferidos;
    }

    private String resolverDuracaoPreferidaEstruturada() {
        String selecao = cbDuracaoPreferida.getValue();
        if (selecao == null || selecao.isBlank() || "Indiferente".equalsIgnoreCase(selecao)) return null;
        return switch (selecao) {
            case "Mais curto" -> "curto";
            case "Mais longo" -> "longo";
            default           -> null;
        };
    }

    private String inferirDuracaoAPartirDoContexto(String textoLivre) {
        if (textoLivre == null || textoLivre.isBlank()) return null;
        String n = PreferenciaFormatters.normalizarTexto(textoLivre);
        if (n.contains("curto") || n.contains("curtos") || n.contains("reduzido") || n.contains("mais curto"))
            return "curto";
        if (n.contains("longo") || n.contains("longos") || n.contains("mais longo") || n.contains("completo"))
            return "longo";
        return null;
    }

    // ── Form filling (reverse direction) ───────────────────────────────────────

    public void preencherFormularioColegas(Preferencia preferencia, List<String> colegas) {
        if (!"colegas".equalsIgnoreCase(preferencia.getTipo())) {
            cbColegaPreferido.setValue(null);
            return;
        }
        String descricao = preferencia.getDescricao();
        if (descricao == null || descricao.isBlank()) {
            cbColegaPreferido.setValue(null);
            return;
        }
        String descNorm = descricao.toLowerCase(Locale.ROOT);
        for (String colega : colegas) {
            if (descNorm.contains(colega.toLowerCase(Locale.ROOT))) {
                cbColegaPreferido.setValue(colega);
                return;
            }
        }
        cbColegaPreferido.setValue(null);
    }

    public void preencherFormularioTurnos(Preferencia preferencia) {
        if (preferencia == null || !"turnos".equalsIgnoreCase(preferencia.getTipo())) {
            resetarTurnos();
            return;
        }
        String descricao = PreferenciaFormatters.limparTexto(preferencia.getDescricao());
        if (descricao == null) {
            resetarTurnos();
            return;
        }
        String n = descricao.toLowerCase(Locale.ROOT);
        chkTurnoManha.setSelected(n.contains("manha") || n.contains("abertura"));
        chkTurnoIntermedio.setSelected(n.contains("intermedio") || n.contains("tarde"));
        chkTurnoNoite.setSelected(n.contains("noite") || n.contains("fecho"));

        if (n.contains("duracao preferida: curto") || n.contains(" turnos curtos")) {
            cbDuracaoPreferida.setValue("Mais curto");
        } else if (n.contains("duracao preferida: longo") || n.contains(" turnos longos")) {
            cbDuracaoPreferida.setValue("Mais longo");
        } else {
            cbDuracaoPreferida.setValue("Indiferente");
        }
    }

    private void resetarTurnos() {
        chkTurnoManha.setSelected(false);
        chkTurnoIntermedio.setSelected(false);
        chkTurnoNoite.setSelected(false);
        cbDuracaoPreferida.setValue("Indiferente");
    }

    public String obterNotaLivre(Preferencia preferencia) {
        if (preferencia == null || preferencia.getDescricao() == null) return "";
        String descricao = preferencia.getDescricao().trim();
        if (!"colegas".equalsIgnoreCase(preferencia.getTipo())
                && !"turnos".equalsIgnoreCase(preferencia.getTipo())) {
            return descricao;
        }
        int idx = descricao.indexOf("Nota adicional:");
        if (idx >= 0) return descricao.substring(idx + "Nota adicional:".length()).trim();
        return "";
    }
}
