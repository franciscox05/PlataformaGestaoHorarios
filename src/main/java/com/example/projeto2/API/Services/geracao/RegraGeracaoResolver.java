package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Regra;
import com.example.projeto2.API.Modules.RegrasLoja;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Repositories.RegraRepository;
import com.example.projeto2.API.Repositories.RegrasLojaRepository;
import com.example.projeto2.API.Services.HorarioValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.example.projeto2.API.Services.geracao.HorarioFormatters.normalizarTexto;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.valorOuTraco;

@Component
public class RegraGeracaoResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegraGeracaoResolver.class);

    /**
     * Descanso mínimo legal entre jornadas de trabalho (Código do Trabalho, art. 214.º):
     * 11 horas seguidas entre o fim de um turno e o início do seguinte. Valores
     * configurados abaixo deste mínimo são corrigidos para 11 com aviso — sem isto,
     * um turno de noite (ex.: 16:00–00:00) seguido de manhã (08:00) passava com 8h de gap.
     */
    private static final int DESCANSO_MINIMO_LEGAL_HORAS = 11;

    private final RegrasLojaRepository regrasLojaRepository;
    private final RegraRepository regraRepository;
    private final HorarioValidatorService validator;

    public RegraGeracaoResolver(RegrasLojaRepository regrasLojaRepository,
                                RegraRepository regraRepository,
                                HorarioValidatorService validator) {
        this.regrasLojaRepository = regrasLojaRepository;
        this.regraRepository = regraRepository;
        this.validator = validator;
    }

    public List<RegraAplicada> obterRegrasAplicadas(Integer idLoja) {
        Map<Integer, RegrasLoja> overrides = new HashMap<>();
        for (RegrasLoja regraLoja : regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(idLoja)) {
            if (regraLoja.getIdRegra() != null && regraLoja.getIdRegra().getId() != null) {
                overrides.put(regraLoja.getIdRegra().getId(), regraLoja);
            }
        }

        List<RegraAplicada> regras = new ArrayList<>();
        for (Regra regra : regraRepository.findAllByOrderByDescricaoAsc()) {
            RegrasLoja override = overrides.get(regra.getId());
            Integer valor = override != null && override.getValorEspecifico() != null
                    ? override.getValorEspecifico()
                    : regra.getValorPadrao();
            regras.add(new RegraAplicada(
                    valorOuTraco(regra.getDescricao()),
                    valorOuTraco(regra.getTipo()),
                    valor
            ));
        }
        return regras;
    }

    public ParametrosGeracao resolverParametrosGeracao(List<RegraAplicada> regras, List<Turno> turnos) {
        Integer minimoGenerico = regras.stream()
                .filter(this::ehRegraDeMinimos)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor > 0)
                .findFirst()
                .orElse(null);

        Map<Integer, Integer> minimosPorTurno = new LinkedHashMap<>();
        for (Turno turno : turnos) {
            String tipoTurno = normalizarTexto(turno.getTipo() != null ? String.valueOf(turno.getTipo()) : "");
            Integer minimoEspecifico = regras.stream()
                    .filter(this::ehRegraDeMinimos)
                    .filter(regra -> regraMencionaTurno(regra, tipoTurno))
                    .map(RegraAplicada::valor)
                    .filter(Objects::nonNull)
                    .filter(valor -> valor > 0)
                    .findFirst()
                    .orElse(null);

            Integer minimo = minimoEspecifico != null ? minimoEspecifico : minimoGenerico;
            if (minimo == null || minimo <= 0) {
                throw new IllegalArgumentException("Nao existe uma regra minima valida para gerar o turno " + HorarioFormatters.formatarTurno(turno) + ".");
            }
            minimosPorTurno.put(turno.getId(), minimo);
        }

        int maxDiasConsecutivos = regras.stream()
                .filter(this::ehRegraDiasConsecutivos)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor > 0)
                .findFirst()
                .orElse(5);

        int descansoConfigurado = regras.stream()
                .filter(this::ehRegraDescanso)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor > 0)
                .findFirst()
                .orElse(DESCANSO_MINIMO_LEGAL_HORAS);
        if (descansoConfigurado < DESCANSO_MINIMO_LEGAL_HORAS) {
            LOGGER.warn(
                    "A regra de descanso minimo entre turnos ({}h) esta abaixo do minimo legal de {}h "
                    + "(CT art. 214.º). A geracao vai aplicar {}h; atualiza a regra RFS06 da loja.",
                    descansoConfigurado, DESCANSO_MINIMO_LEGAL_HORAS, DESCANSO_MINIMO_LEGAL_HORAS);
        }
        int descansoMinimoHoras = Math.max(descansoConfigurado, DESCANSO_MINIMO_LEGAL_HORAS);

        int descansoSemanalMinimoDias = regras.stream()
                .filter(this::ehRegraDescansoSemanal)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor >= 1 && valor <= 6)
                .findFirst()
                .orElse(2);

        int janelaRotacaoFinsDeSemanaSemanas = regras.stream()
                .filter(this::ehRegraRotacaoFinsDeSemana)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor >= 2)
                .findFirst()
                .orElse(2);

        int diaLimiteLancamento = regras.stream()
                .filter(this::ehRegraDiaLimiteLancamento)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor >= 1 && valor <= 31)
                .findFirst()
                .orElse(15);

        boolean exigirChefiaAoSabado = regras.stream()
                .filter(this::ehRegraChefiaAoSabado)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .findFirst()
                .map(valor -> valor > 0)
                .orElse(true);

        Map<PerfilContratual, Long> cargaMaximaMinutosPorPerfil = new EnumMap<>(PerfilContratual.class);
        for (PerfilContratual perfil : PerfilContratual.values()) {
            Integer horasMensais = regras.stream()
                    .filter(this::ehRegraCargaContratual)
                    .filter(regra -> regraMencionaPerfilContratual(regra, perfil))
                    .map(RegraAplicada::valor)
                    .filter(Objects::nonNull)
                    .filter(valor -> valor > 0)
                    .findFirst()
                    .orElse(perfil.cargaMensalHorasPadrao());

            if (horasMensais == null || horasMensais <= 0) {
                throw new IllegalArgumentException("Nao existe uma regra de carga contratual valida para o perfil " + perfil.descricaoCurta() + ".");
            }
            cargaMaximaMinutosPorPerfil.put(perfil, horasMensais * 60L);
        }

        if (!validator.janelaRotacaoRespeiraMinimo(janelaRotacaoFinsDeSemanaSemanas)) {
            LOGGER.warn(
                    "A janela de rotacao de fins de semana configurada ({} semanas) e inferior ao minimo legal recomendado de 7 semanas. "
                    + "Considera atualizar a regra RFS08 da loja.",
                    janelaRotacaoFinsDeSemanaSemanas);
        }

        return new ParametrosGeracao(
                minimosPorTurno,
                maxDiasConsecutivos,
                descansoMinimoHoras,
                descansoSemanalMinimoDias,
                janelaRotacaoFinsDeSemanaSemanas,
                diaLimiteLancamento,
                exigirChefiaAoSabado,
                cargaMaximaMinutosPorPerfil
        );
    }

    private boolean ehRegraDeMinimos(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        boolean mencionaMinimo = texto.contains("min") || texto.contains("minim");
        boolean mencionaCobertura = texto.contains("colaborador")
                || texto.contains("equipa")
                || texto.contains("pessoas")
                || texto.contains("funcionario")
                || (texto.contains("turno") && (texto.contains("por") || texto.contains("cobertura") || texto.contains("loja")));
        boolean pareceOutraRegra = texto.contains("descanso")
                || texto.contains("carga")
                || texto.contains("contrat")
                || texto.contains("lancamento")
                || texto.contains("publicacao");
        return mencionaMinimo && mencionaCobertura && !pareceOutraRegra;
    }

    private boolean ehRegraDiasConsecutivos(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return (texto.contains("dias") || texto.contains("dia"))
                && (texto.contains("consecut") || texto.contains("seguid"))
                && !texto.contains("hora");
    }

    private boolean ehRegraDescanso(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return (texto.contains("descanso") && (texto.contains("hora") || texto.contains("interval")))
                || (texto.contains("entre") && texto.contains("turno"));
    }

    private boolean ehRegraDescansoSemanal(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("descanso")
                && (texto.contains("seman") || texto.contains("folga"))
                && texto.contains("dia");
    }

    private boolean ehRegraRotacaoFinsDeSemana(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return (texto.contains("rotacao") || texto.contains("janela"))
                && (texto.contains("fim de semana") || texto.contains("weekend"));
    }

    private boolean ehRegraDiaLimiteLancamento(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("dia")
                && texto.contains("limite")
                && (texto.contains("lancamento") || texto.contains("publicacao") || texto.contains("publicar"));
    }

    private boolean ehRegraChefiaAoSabado(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("sabado")
                && (texto.contains("gerente") || texto.contains("subgerente") || texto.contains("chefia") || texto.contains("gestao"));
    }

    private boolean ehRegraCargaContratual(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("carga")
                && (texto.contains("contrat") || texto.contains("mensal"))
                && (texto.contains("hora") || texto.contains("horas"));
    }

    private boolean regraMencionaTurno(RegraAplicada regra, String tipoTurno) {
        for (String alias : aliasesTurno(tipoTurno)) {
            if (!alias.isBlank() && regra.textoNormalizado().contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private boolean regraMencionaPerfilContratual(RegraAplicada regra, PerfilContratual perfil) {
        return perfil.correspondeRegra(regra.textoNormalizado());
    }

    private List<String> aliasesTurno(String tipoTurno) {
        return switch (tipoTurno) {
            case "manha" -> List.of("manha", "abertura", "morning", "cedo");
            case "intermedio", "tarde" -> List.of(
                    "intermedio", "tarde", "afternoon", "meio dia", "meio-dia", "central"
            );
            case "noite" -> List.of("noite", "fecho", "encerramento", "night");
            default -> List.of(tipoTurno);
        };
    }
}
