package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioValidatorService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Fase de <b>pré-planeamento de folgas preferidas</b>, executada <em>antes</em> do
 * greedy diário do {@link HorarioGeneratorEngine}.
 *
 * <p>As folgas preferidas (soft) eram tratadas só de duas formas: penalização no
 * scoring do {@link AvaliadorAtribuicao} e recuperação <em>reativa</em>, no fim, pelo
 * {@link RefinadorPlaneamento}. O problema dessa abordagem puramente reativa: quando a
 * cobertura aperta perto do fim do mês, o greedy já consumiu o colaborador no seu dia
 * de folga e o refinador pode não encontrar troca válida — a folga fica por honrar.
 *
 * <p>Esta fase é <em>proativa</em>: para cada dia, decide antecipadamente quais folgas
 * preferidas <b>cabem</b> sem comprometer a cobertura, e reserva-as como restrição dura
 * localizada no {@link EstadoColaborador} (via {@link EstadoColaborador#reservarFolgasPreferidas}).
 * O que é reservado fica garantido; o que não couber mantém-se soft (scoring + refinador),
 * pelo que o comportamento nunca é pior do que antes — apenas mais folgas honradas.
 *
 * <p><b>Critério de viabilidade (estático, pré-greedy):</b> num dado dia, dos
 * colaboradores que poderiam trabalhar (perfil compatível, sem bloqueio duro), só se
 * reservam folgas enquanto sobrarem pelo menos {@code cobertura mínima + margem} workers
 * disponíveis. A margem dá folga ao greedy para satisfazer descanso/consecutivos sem
 * falhar a geração.
 *
 * <p><b>Conservador ao fim de semana:</b> folgas que caem ao sábado/domingo <em>não</em>
 * são pré-reservadas — o fim de semana tem cobertura mais frágil (chefia obrigatória e
 * rotação) e será tratado holisticamente pelo lookahead de fins de semana. Ao fim de
 * semana, a folga preferida continua a valer como soft.
 *
 * <p><b>Justiça:</b> quando vários colaboradores disputam folga no mesmo dia escasso,
 * prioriza-se quem tem menos folgas já reservadas (desempate determinístico por id),
 * distribuindo as folgas honradas de forma equilibrada pela equipa.
 */
public final class PlaneadorFolgasPreferidas {

    /**
     * Workers que devem sobrar acima da cobertura mínima depois de reservadas as folgas
     * de um dia. Protege a viabilidade do greedy (descanso semanal, dias consecutivos,
     * backtracking) sem o qual reservar folgas poderia tornar um dia impossível de cobrir.
     */
    private static final int MARGEM_SEGURANCA = 2;

    private final HorarioValidatorService validator;

    public PlaneadorFolgasPreferidas(HorarioValidatorService validator) {
        this.validator = validator;
    }

    /**
     * Calcula, para cada colaborador, o conjunto de dias de folga preferida que podem ser
     * garantidos sem comprometer a cobertura. Devolve um mapa id-colaborador → dias
     * reservados (vazio se não há folgas preferidas ou nenhuma cabe).
     */
    public Map<Integer, Set<LocalDate>> reservar(PedidoGeracao pedido) {
        Map<Integer, Set<LocalDate>> pedidas = pedido.folgasPreferidasPorColaborador();
        if (pedidas == null || pedidas.isEmpty()) {
            return Map.of();
        }

        // Inverte o mapa: dia → colaboradores que pediram folga nesse dia. TreeMap para
        // processar os dias por ordem cronológica (a contabilidade de justiça acumula).
        Map<LocalDate, List<Integer>> requerentesPorDia = new TreeMap<>();
        for (Map.Entry<Integer, Set<LocalDate>> entrada : pedidas.entrySet()) {
            if (entrada.getKey() == null || entrada.getValue() == null) {
                continue;
            }
            for (LocalDate dia : entrada.getValue()) {
                requerentesPorDia.computeIfAbsent(dia, ignored -> new ArrayList<>()).add(entrada.getKey());
            }
        }

        Map<Integer, Set<LocalDate>> reservadas = new HashMap<>();
        Map<Integer, Integer> honradasPorColaborador = new HashMap<>();

        for (Map.Entry<LocalDate, List<Integer>> entrada : requerentesPorDia.entrySet()) {
            LocalDate dia = entrada.getKey();
            if (validator.ehFimDeSemana(dia)) {
                continue; // FDS tratado pelo lookahead; folga preferida fica soft
            }
            ConfiguracaoDia config = pedido.configuracoesPorData().get(dia);
            if (config != null && config.lojaEncerrada()) {
                continue; // loja encerrada: toda a gente folga, reserva irrelevante
            }

            int procuraveis = contarProcuraveis(pedido, dia);
            int cobertura = coberturaMinima(pedido, config);
            int capacidadeLivre = procuraveis - cobertura - MARGEM_SEGURANCA;
            if (capacidadeLivre <= 0) {
                continue; // sem margem: honrar qualquer folga arriscaria a cobertura
            }

            List<Integer> requerentes = new ArrayList<>(entrada.getValue());
            requerentes.sort(Comparator
                    .comparingInt((Integer id) -> honradasPorColaborador.getOrDefault(id, 0))
                    .thenComparingInt(id -> id));

            int aHonrar = Math.min(requerentes.size(), capacidadeLivre);
            for (int i = 0; i < aHonrar; i++) {
                Integer id = requerentes.get(i);
                reservadas.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(dia);
                honradasPorColaborador.merge(id, 1, Integer::sum);
            }
        }
        return reservadas;
    }

    /**
     * Número de colaboradores que poderiam trabalhar neste dia: perfil compatível
     * (exclui reforço de fim de semana, que não trabalha dias úteis), com carga
     * contratual e sem bloqueio duro (folga/férias aprovada) nesse dia.
     */
    private int contarProcuraveis(PedidoGeracao pedido, LocalDate dia) {
        int count = 0;
        for (Lojautilizador ligacao : pedido.colaboradores()) {
            Integer id = idDe(ligacao);
            if (id == null) {
                continue;
            }
            if (pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L) <= 0) {
                continue;
            }
            if ("reforco_parttime".equals(tipoCargo(ligacao))) {
                continue; // só trabalha ao fim de semana — irrelevante em dia útil
            }
            if (pedido.bloqueiosPorColaborador().getOrDefault(id, Set.of()).contains(dia)) {
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * Cobertura mínima do dia: soma, por tipo de turno distinto, do mínimo de
     * colaboradores exigido. Espelha a construção de slots do motor — uma exceção de
     * calendário com mínimo definido sobrepõe-se ao mínimo por turno configurado.
     */
    private int coberturaMinima(PedidoGeracao pedido, ConfiguracaoDia config) {
        List<Turno> turnos = (config != null && config.turnosCompativeis() != null
                && !config.turnosCompativeis().isEmpty())
                ? config.turnosCompativeis()
                : pedido.turnos();

        Map<String, Integer> minimoPorTipo = new LinkedHashMap<>();
        for (Turno turno : turnos) {
            String tipo = TurnoClassifier.tipoNormalizado(turno);
            int minimo = (config != null && config.minimoColaboradoresTurno() != null)
                    ? config.minimoColaboradoresTurno()
                    : pedido.minimosPorTurno().getOrDefault(turno.getId(), 0);
            minimoPorTipo.merge(tipo, minimo, Math::max);
        }
        return minimoPorTipo.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static String tipoCargo(Lojautilizador ligacao) {
        if (ligacao.getIdCargo() == null || ligacao.getIdCargo().getTipo() == null) {
            return "";
        }
        return ligacao.getIdCargo().getTipo().trim().toLowerCase(Locale.ROOT);
    }

    private static Integer idDe(Lojautilizador ligacao) {
        return ligacao != null && ligacao.getIdUtilizador() != null
                ? ligacao.getIdUtilizador().getId()
                : null;
    }
}
