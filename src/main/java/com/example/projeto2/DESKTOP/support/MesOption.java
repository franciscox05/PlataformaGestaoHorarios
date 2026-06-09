package com.example.projeto2.DESKTOP.support;

import java.util.List;

public record MesOption(int numero, String nome) {

    @Override
    public String toString() {
        return nome;
    }

    public static List<MesOption> todos() {
        return List.of(
                new MesOption(1, "Janeiro"),
                new MesOption(2, "Fevereiro"),
                new MesOption(3, "Março"),
                new MesOption(4, "Abril"),
                new MesOption(5, "Maio"),
                new MesOption(6, "Junho"),
                new MesOption(7, "Julho"),
                new MesOption(8, "Agosto"),
                new MesOption(9, "Setembro"),
                new MesOption(10, "Outubro"),
                new MesOption(11, "Novembro"),
                new MesOption(12, "Dezembro")
        );
    }
}
