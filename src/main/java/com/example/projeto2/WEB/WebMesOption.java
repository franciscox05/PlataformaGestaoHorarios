package com.example.projeto2.WEB;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record WebMesOption(int valor, String nome) {

    public static List<WebMesOption> todos() {
        List<WebMesOption> meses = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            String nomeMes = Month.of(mes).getDisplayName(TextStyle.FULL, Locale.of("pt", "PT"));
            meses.add(new WebMesOption(mes, capitalizar(nomeMes)));
        }
        return meses;
    }

    public static List<Integer> anosProximos(int anoAtual, int raio) {
        List<Integer> anos = new ArrayList<>();
        for (int ano = anoAtual - raio; ano <= anoAtual + raio; ano++) {
            anos.add(ano);
        }
        return anos;
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }
}
