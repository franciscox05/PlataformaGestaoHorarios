package com.example.projeto2.DESKTOP.support;

/**
 * Funções utilitárias para transformar exceções da camada de serviço numa mensagem
 * apresentável ao utilizador final. Reutilizável em qualquer controller.
 */
public final class MensagemErroFormatter {

    private MensagemErroFormatter() {
        // utilitário
    }

    /**
     * Devolve uma mensagem apresentável. Se a causa-raiz for um {@link IllegalArgumentException}
     * com mensagem, sanitiza-a; caso contrário usa o {@code fallback}.
     */
    public static String resolver(Throwable erro, String fallback) {
        Throwable causaRaiz = causaRaiz(erro);
        if (causaRaiz instanceof IllegalArgumentException
                && causaRaiz.getMessage() != null
                && !causaRaiz.getMessage().isBlank()) {
            return tornarApresentavel(causaRaiz.getMessage(), fallback);
        }
        return fallback;
    }

    /** Encontra a causa mais profunda da cadeia de exceções (defendendo de ciclos). */
    public static Throwable causaRaiz(Throwable erro) {
        Throwable atual = erro;
        while (atual != null && atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return atual != null ? atual : erro;
    }

    private static String tornarApresentavel(String mensagem, String fallback) {
        String mensagemNormalizada = mensagem != null ? mensagem.trim() : "";
        if (mensagemNormalizada.isBlank()) {
            return fallback;
        }

        boolean pareceDiagnosticoTecnico = mensagemNormalizada.length() > 420
                || mensagemNormalizada.contains("={")
                || mensagemNormalizada.contains("=[")
                || mensagemNormalizada.contains("@10:")
                || mensagemNormalizada.contains("@14:")
                || mensagemNormalizada.contains("@18:");

        if (!pareceDiagnosticoTecnico) {
            return mensagemNormalizada;
        }

        String mensagemCurta = mensagemNormalizada
                .replaceAll("\\s*\\[[\\s\\S]*", "")
                .replaceAll("\\s*\\{[\\s\\S]*", "")
                .trim();

        if (mensagemCurta.isBlank() || mensagemCurta.length() > 260) {
            mensagemCurta = fallback;
        }

        return mensagemCurta
                + " Revê a equipa selecionada, folgas/preferências aprovadas, descanso entre turnos, limite de horas e mínimo exigido por turno.";
    }
}
