package com.example.projeto2.DESKTOP.support;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Classifica uma regra da loja para a página "Loja e Regras": distingue as regras
 * <b>fixas por lei</b> (que o gestor não pode reduzir) das <b>personalizáveis</b>, e
 * dentro destas marca as que são <b>liga/desliga</b> (booleanas).
 *
 * <p>Espelha a deteção por texto do {@code RegraGeracaoResolver} (que é quem realmente
 * aplica as regras na geração e já força os mínimos legais). Aqui a classificação é só
 * para apresentação/edição — o motor continua a impor o mínimo legal independentemente
 * do que ficar gravado, pelo que uma classificação imperfeita nunca gera um horário ilegal.
 */
public final class ClassificadorRegra {

    public enum Categoria {
        /** Mínimo legal — apresentada bloqueada, não pode ser reduzida. */
        FIXA_LEGAL,
        /** Regra opcional liga/desliga (ex.: chefia obrigatória ao sábado). */
        BOOLEANA,
        /** Regra numérica personalizável pela loja. */
        EDITAVEL
    }

    /** Descanso diário mínimo entre jornadas — Código do Trabalho, art. 214.º. */
    public static final int DESCANSO_DIARIO_LEGAL_HORAS = 11;

    private ClassificadorRegra() {
    }

    public static Categoria categoria(String descricao, String tipo) {
        String t = normalizar(descricao) + " " + normalizar(tipo);
        if (ehDescansoDiario(t)) return Categoria.FIXA_LEGAL;
        if (ehChefiaSabado(t)) return Categoria.BOOLEANA;
        return Categoria.EDITAVEL;
    }

    public static boolean ehFixaLegal(String descricao, String tipo) {
        return categoria(descricao, tipo) == Categoria.FIXA_LEGAL;
    }

    /** Mínimo legal a impor/mostrar para uma regra fixa, ou {@code null} se não aplicável. */
    public static Integer valorLegalMinimo(String descricao, String tipo) {
        String t = normalizar(descricao) + " " + normalizar(tipo);
        return ehDescansoDiario(t) ? DESCANSO_DIARIO_LEGAL_HORAS : null;
    }

    public static String notaLegal(String descricao, String tipo) {
        String t = normalizar(descricao) + " " + normalizar(tipo);
        if (ehDescansoDiario(t)) {
            return "Fixo por lei — Código do Trabalho art. 214.º (mínimo 11h de descanso entre turnos).";
        }
        return "Mínimo legal — não pode ser reduzido.";
    }

    private static boolean ehDescansoDiario(String t) {
        if (t.contains("seman")) return false; // descanso SEMANAL é outra regra (editável)
        return (t.contains("descanso") && (t.contains("hora") || t.contains("interval")))
                || (t.contains("entre") && t.contains("turno"));
    }

    private static boolean ehChefiaSabado(String t) {
        return t.contains("sabado")
                && (t.contains("gerente") || t.contains("subgerente")
                || t.contains("chefia") || t.contains("gestao"));
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT);
    }
}
