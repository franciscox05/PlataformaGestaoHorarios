package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.CriteriosGeracao;
import com.example.projeto2.API.Services.geracao.dto.HorarioLinha;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Analisa uma proposta de horário gerada e produz um <b>modelo de cumprimento</b> por
 * colaborador, organizado em duas partes: <b>carga contratual</b> e <b>preferências</b>
 * (folga preferida, turno, colegas), mais uma linha de <b>fins de semana</b>.
 *
 * <p>Cada categoria é resumida (ex.: "3/4 folgas cumpridas", "16/18 turnos no período
 * pedido", "2 turnos juntos") em vez de listar cada ocorrência — para o gestor ler de
 * relance o que foi e o que não foi cumprido.
 *
 * <p>É lógica pura (sem JavaFX, sem repositórios) para ser facilmente testável.
 */
public final class AnaliseCumprimentoHorario {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("d/MM");
    private static final DateTimeFormatter FMT_PARSE = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final double LIMITE_SUBUTILIZACAO = 0.75;
    private static final double TURNO_CUMPRIDO = 0.80;
    private static final double TURNO_PARCIAL = 0.40;

    private AnaliseCumprimentoHorario() {}

    public enum Estado { CUMPRIDO, PARCIAL, NAO_CUMPRIDO, INFORMATIVO }

    /** Uma linha resumida (carga, folga, turno, colega, ausência, fim de semana). */
    public record Item(String texto, Estado estado) {}

    /** Análise completa de um colaborador. */
    public record ColaboradorCumprimento(
            String nome,
            String cargo,
            Integer horasTrabalhadas,   // null se desconhecido
            Integer horasPrevistas,     // null se desconhecido
            Estado estadoCarga,
            Item folga,                 // resumo "x/y cumpridas" — null se sem preferência
            Item turno,                 // resumo "x/y turnos" — null se sem preferência
            List<Item> colegas,         // um por colega pedido
            Item ausencias,             // resumo "x/y respeitadas" — null se sem ausências
            Item finsDeSemana,          // resumo "trabalhou x de y" — null se sem FDS no período
            int honradas,
            int totalPreferencias
    ) {
        public boolean temProblema() {
            if (estadoCarga == Estado.NAO_CUMPRIDO || estadoCarga == Estado.PARCIAL) return true;
            return naoOk(folga) || naoOk(turno) || naoOk(ausencias) || naoOk(finsDeSemana)
                    || (colegas != null && colegas.stream().anyMatch(AnaliseCumprimentoHorario::naoOk));
        }

        public Estado estadoGeral() {
            if (contem(Estado.NAO_CUMPRIDO)) return Estado.NAO_CUMPRIDO;
            if (contem(Estado.PARCIAL)) return Estado.PARCIAL;
            return Estado.CUMPRIDO;
        }

        private boolean contem(Estado e) {
            if (estadoCarga == e || ehEstado(folga, e) || ehEstado(turno, e)
                    || ehEstado(ausencias, e) || ehEstado(finsDeSemana, e)) return true;
            return colegas != null && colegas.stream().anyMatch(i -> i.estado() == e);
        }

        public List<Item> preferencias() {
            List<Item> l = new ArrayList<>();
            if (folga != null) l.add(folga);
            if (turno != null) l.add(turno);
            if (colegas != null) l.addAll(colegas);
            return l;
        }

        public boolean temConteudo() {
            return horasPrevistas != null || folga != null || turno != null
                    || (colegas != null && !colegas.isEmpty())
                    || ausencias != null || finsDeSemana != null;
        }
    }

    public record Resumo(
            int totalColaboradores,
            int prefsHonradas, int prefsTotais,
            int cargaOk, int cargaTotais,
            int fdsComFolga, int fdsAvaliados,
            int ausenciasVioladas
    ) {}

    public record Resultado(Resumo resumo, List<ColaboradorCumprimento> colaboradores) {
        public boolean vazio() { return colaboradores.isEmpty(); }
    }

    // ── API ───────────────────────────────────────────────────────────────────

    public static Resultado analisar(List<HorarioLinha> linhas, CriteriosGeracao criterios, int ano) {
        if (linhas == null || linhas.isEmpty() || criterios == null) {
            return new Resultado(new Resumo(0, 0, 0, 0, 0, 0, 0, 0), List.of());
        }

        Map<Integer, List<HorarioLinha>> porId = new LinkedHashMap<>();
        Map<String, Integer> idPorNome = new LinkedHashMap<>();
        Map<Integer, String> nomePorId = new LinkedHashMap<>();
        for (HorarioLinha l : linhas) {
            if (l.data() == null || l.idColaborador() == null) continue;
            porId.computeIfAbsent(l.idColaborador(), k -> new ArrayList<>()).add(l);
            if (l.colaborador() != null) {
                idPorNome.putIfAbsent(l.colaborador().trim(), l.idColaborador());
                nomePorId.putIfAbsent(l.idColaborador(), l.colaborador().trim());
            }
        }

        Map<String, Integer> horasPrevistasPorNome = new LinkedHashMap<>();
        Map<String, String> cargoPorNome = new LinkedHashMap<>();
        if (criterios.detalheColaboradores() != null) {
            for (String d : criterios.detalheColaboradores()) {
                String[] partes = d.split(" — ", 2);
                if (partes.length < 2) continue;
                String nome = partes[0].trim();
                var m = java.util.regex.Pattern.compile("(\\d+)\\s*h\\s*/\\s*m").matcher(partes[1]);
                if (m.find()) horasPrevistasPorNome.put(nome, Integer.parseInt(m.group(1)));
                cargoPorNome.put(nome, partes[1].split(",", 2)[0].trim());
            }
        }

        Map<String, List<String>> turnosPref = agruparPorNome(criterios.detalhePreferenciasTurno());
        Map<String, List<String>> colegasPref = agruparPorNome(criterios.detalhePreferenciasColegas());
        Map<String, List<String>> ausenciasPref = agruparPorNome(criterios.detalheAusencias());

        Set<String> coocorrencia = new java.util.HashSet<>();
        for (HorarioLinha l : linhas) {
            if (l.data() != null && l.colaborador() != null) {
                coocorrencia.add(chaveCoocorrencia(l.colaborador().trim(), l.data(), l.turno()));
            }
        }

        // Apenas participantes (têm pelo menos um turno).
        Set<String> nomes = new TreeSet<>(nomePorId.values());

        YearMonth periodo = YearMonth.from(linhas.stream()
                .map(HorarioLinha::data).filter(java.util.Objects::nonNull)
                .findFirst().orElse(LocalDate.of(ano, 1, 1)));
        int totalSabados = contarSabados(periodo);

        int prefsHonradas = 0, prefsTotais = 0, cargaOk = 0, cargaTotais = 0;
        int ausenciasVioladas = 0, fdsComFolga = 0, fdsAvaliados = 0;
        List<ColaboradorCumprimento> colaboradores = new ArrayList<>();

        for (String nome : nomes) {
            Integer id = idPorNome.get(nome);
            List<HorarioLinha> turnosCol = id != null ? porId.getOrDefault(id, List.of()) : List.of();
            Set<LocalDate> diasTrabalhados = new LinkedHashSet<>();
            for (HorarioLinha l : turnosCol) diasTrabalhados.add(l.data());

            String cargo = cargoPorNome.getOrDefault(nome,
                    turnosCol.isEmpty() ? "" : valorOuVazio(turnosCol.get(0).cargo()));

            // ── Carga horária ──────────────────────────────────────────────
            Integer prevista = horasPrevistasPorNome.get(nome);
            Integer trabalhadas = null;
            Estado estadoCarga = Estado.INFORMATIVO;
            if (prevista != null) {
                int min = 0;
                for (HorarioLinha l : turnosCol) min += minutosTurno(l.periodo());
                trabalhadas = (int) Math.round(min / 60.0);
                estadoCarga = classificarCarga(trabalhadas, prevista);
                cargaTotais++;
                if (estadoCarga == Estado.CUMPRIDO) cargaOk++;
            }

            // ── Folga preferida (resumo x/y) ───────────────────────────────
            Item folga = null;
            Set<LocalDate> diasFolga = id != null
                    ? criterios.folgasPreferidasPorColaborador().getOrDefault(id, Set.of())
                    : Set.of();
            if (!diasFolga.isEmpty()) {
                List<LocalDate> violados = new ArrayList<>();
                for (LocalDate dia : new TreeSet<>(diasFolga)) {
                    if (diasTrabalhados.contains(dia)) violados.add(dia);
                }
                int total = diasFolga.size();
                int honradas = total - violados.size();
                Estado e = violados.isEmpty() ? Estado.CUMPRIDO
                        : (honradas == 0 ? Estado.NAO_CUMPRIDO : Estado.PARCIAL);
                String txt = honradas + "/" + total + " cumprida(s)";
                if (!violados.isEmpty()) txt += " — trabalhou em " + datas(violados);
                folga = new Item(txt, e);
                prefsTotais += total;
                prefsHonradas += honradas;
            }

            // ── Turno preferido (resumo x/y) ───────────────────────────────
            Item turno = null;
            List<String> turnosNome = turnosPref.getOrDefault(nome, List.of());
            if (!turnosNome.isEmpty()) {
                Set<String> tipos = new LinkedHashSet<>();
                for (String desc : turnosNome) tipos.addAll(detetarTurnos(extrairValor(desc)));
                if (tipos.isEmpty()) {
                    String desc = String.join("; ", turnosNome.stream()
                            .map(AnaliseCumprimentoHorario::extrairValor).toList());
                    turno = new Item("«" + desc + "» (registada, sem turno específico para verificar)",
                            Estado.INFORMATIVO);
                } else {
                    long corresp = turnosCol.stream()
                            .filter(l -> tipos.contains(GrelhaHorarioRenderer.turnoChave(l.turno())))
                            .count();
                    int total = turnosCol.size();
                    if (total == 0) {
                        turno = new Item(nomeTurnos(tipos) + ": sem turnos atribuídos", Estado.INFORMATIVO);
                    } else {
                        double r = corresp / (double) total;
                        Estado e = r >= TURNO_CUMPRIDO ? Estado.CUMPRIDO
                                : (r >= TURNO_PARCIAL ? Estado.PARCIAL : Estado.NAO_CUMPRIDO);
                        turno = new Item(nomeTurnos(tipos) + ": " + corresp + "/" + total
                                + " turnos no período pedido", e);
                        prefsTotais += 1;
                        if (e == Estado.CUMPRIDO) prefsHonradas += 1;
                    }
                }
            }

            // ── Colegas preferidos (nº turnos juntos) ──────────────────────
            List<Item> colegas = new ArrayList<>();
            for (String desc : colegasPref.getOrDefault(nome, List.of())) {
                for (String alvo : extrairNomesColegas(extrairValor(desc), nomes)) {
                    int n = contarCoincidencias(turnosCol, alvo, coocorrencia);
                    colegas.add(new Item(alvo + ": " + n + " turno(s) juntos",
                            n > 0 ? Estado.CUMPRIDO : Estado.NAO_CUMPRIDO));
                    prefsTotais += 1;
                    if (n > 0) prefsHonradas += 1;
                }
            }

            // ── Ausências aprovadas (resumo x/y) ───────────────────────────
            Item ausencias = null;
            List<String> ausNome = ausenciasPref.getOrDefault(nome, List.of());
            if (!ausNome.isEmpty()) {
                int total = 0;
                List<LocalDate> violadas = new ArrayList<>();
                for (String desc : ausNome) {
                    String valor = extrairValor(desc).split("\\s*\\(", 2)[0].trim();
                    Optional<LocalDate> data = parseData(valor, ano);
                    if (data.isPresent()) {
                        total++;
                        if (diasTrabalhados.contains(data.get())) violadas.add(data.get());
                    }
                }
                if (total > 0) {
                    String txt = (total - violadas.size()) + "/" + total + " respeitada(s)";
                    if (!violadas.isEmpty()) txt += " — escalado em " + datas(violadas);
                    ausencias = new Item(txt, violadas.isEmpty() ? Estado.CUMPRIDO : Estado.NAO_CUMPRIDO);
                    ausenciasVioladas += violadas.size();
                }
            }

            // ── Fins de semana (rotação configurada) ────────────────────────
            Item finsDeSemana = null;
            if (totalSabados > 0) {
                Set<LocalDate> fdsTrabalhados = new LinkedHashSet<>();
                for (HorarioLinha l : turnosCol) {
                    DayOfWeek dow = l.data().getDayOfWeek();
                    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                        fdsTrabalhados.add(l.data().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)));
                    }
                }
                int trab = fdsTrabalhados.size();
                int livres = totalSabados - trab;
                boolean isento = isentoRotacao(cargo);
                if (isento) {
                    finsDeSemana = new Item("trabalhou " + trab + " de " + totalSabados
                            + " (isento de rotação)", Estado.INFORMATIVO);
                } else {
                    fdsAvaliados++;
                    int janela = criterios.janelaRotacaoFimDeSemana();
                    // A regra exige FDS livre apenas quando o período tem >= janela FDS.
                    // Ex.: janela=7 e mês com 5 FDS → 5 < 7 → sem obrigação neste mês.
                    boolean obrigatorio = totalSabados >= janela;
                    boolean ok = livres >= 1 || !obrigatorio;
                    if (ok) fdsComFolga++;
                    Estado estadoFds = (obrigatorio && livres == 0) ? Estado.NAO_CUMPRIDO
                            : (livres >= 1 ? Estado.CUMPRIDO : Estado.INFORMATIVO);
                    String textoFds = (livres == 0 && !obrigatorio)
                            ? "trabalhou " + trab + " de " + totalSabados + " — ok (janela: " + janela + " sem.)"
                            : "trabalhou " + trab + " de " + totalSabados + " — " + livres + " livre(s)";
                    finsDeSemana = new Item(textoFds, estadoFds);
                }
            }

            int honr = 0, tot = 0;
            for (Item i : new Item[]{folga, turno}) {
                if (i == null || i.estado() == Estado.INFORMATIVO) continue;
                tot++; if (i.estado() == Estado.CUMPRIDO) honr++;
            }
            for (Item i : colegas) { tot++; if (i.estado() == Estado.CUMPRIDO) honr++; }

            ColaboradorCumprimento cc = new ColaboradorCumprimento(
                    nome, cargo, trabalhadas, prevista, estadoCarga,
                    folga, turno, colegas, ausencias, finsDeSemana, honr, tot);
            if (cc.temConteudo()) colaboradores.add(cc);
        }

        colaboradores.sort(Comparator
                .comparing((ColaboradorCumprimento c) -> c.temProblema() ? 0 : 1)
                .thenComparing(ColaboradorCumprimento::nome, String.CASE_INSENSITIVE_ORDER));

        Resumo resumo = new Resumo(colaboradores.size(),
                prefsHonradas, prefsTotais, cargaOk, cargaTotais,
                fdsComFolga, fdsAvaliados, ausenciasVioladas);
        return new Resultado(resumo, colaboradores);
    }

    // ── Auxiliares ─────────────────────────────────────────────────────────────

    private static boolean naoOk(Item i) {
        return i != null && (i.estado() == Estado.NAO_CUMPRIDO || i.estado() == Estado.PARCIAL);
    }

    private static boolean ehEstado(Item i, Estado e) {
        return i != null && i.estado() == e;
    }

    private static boolean isentoRotacao(String cargo) {
        String c = cargo == null ? "" : cargo.toLowerCase(Locale.ROOT);
        return c.contains("gerente") || c.contains("subgerente")
                || c.contains("reforco_parttime") || c.contains("reforço_parttime")
                || c.contains("reforço") || c.contains("reforco");
    }

    private static Estado classificarCarga(int trabalhadas, int previstas) {
        if (previstas <= 0) return Estado.INFORMATIVO;
        if (trabalhadas > previstas) return Estado.NAO_CUMPRIDO;
        if (trabalhadas < previstas * LIMITE_SUBUTILIZACAO) return Estado.PARCIAL;
        return Estado.CUMPRIDO;
    }

    private static int contarSabados(YearMonth ym) {
        int n = 0;
        for (LocalDate d = ym.atDay(1); !d.isAfter(ym.atEndOfMonth()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY) n++;
        }
        return n;
    }

    private static int contarCoincidencias(List<HorarioLinha> turnosCol, String colega, Set<String> coocorrencia) {
        int n = 0;
        for (HorarioLinha l : turnosCol) {
            if (coocorrencia.contains(chaveCoocorrencia(colega, l.data(), l.turno()))) n++;
        }
        return n;
    }

    private static String chaveCoocorrencia(String nome, LocalDate data, String turno) {
        return nome + "|" + data + "|" + GrelhaHorarioRenderer.turnoChave(turno);
    }

    private static Set<String> detetarTurnos(String descricao) {
        Set<String> tipos = new LinkedHashSet<>();
        String n = normalizar(descricao);
        if (n.contains("manha")) tipos.add("manha");
        if (n.contains("tarde") || n.contains("intermedio")) tipos.add("intermedio");
        if (n.contains("noite")) tipos.add("noite");
        return tipos;
    }

    private static String nomeTurnos(Set<String> tipos) {
        List<String> nomes = new ArrayList<>();
        for (String t : tipos) {
            nomes.add(switch (t) {
                case "manha" -> "Manhã";
                case "tarde" -> "Tarde";
                case "noite" -> "Noite";
                case "intermedio" -> "Intermédio";
                default -> t;
            });
        }
        return "Turno " + String.join("/", nomes);
    }

    private static List<String> extrairNomesColegas(String descricao, Set<String> nomesConhecidos) {
        List<String> resultado = new ArrayList<>();
        if (descricao == null || descricao.isBlank()) return resultado;
        for (String parte : descricao.split("[,;\n]+")) {
            String alvoNorm = normalizar(parte.trim());
            if (alvoNorm.isBlank()) continue;
            for (String conhecido : nomesConhecidos) {
                String cn = normalizar(conhecido);
                if (cn.equals(alvoNorm) || cn.contains(alvoNorm) || alvoNorm.contains(cn)) {
                    if (!resultado.contains(conhecido)) resultado.add(conhecido);
                    break;
                }
            }
        }
        return resultado;
    }

    private static Map<String, List<String>> agruparPorNome(List<String> detalhes) {
        Map<String, List<String>> mapa = new LinkedHashMap<>();
        if (detalhes == null) return mapa;
        for (String d : detalhes) {
            String[] partes = d.split(" — ", 2);
            if (partes.length < 2) continue;
            mapa.computeIfAbsent(partes[0].trim(), k -> new ArrayList<>()).add(partes[1].trim());
        }
        return mapa;
    }

    private static String extrairValor(String detalhe) {
        if (detalhe == null) return "";
        String[] partes = detalhe.split(" — ", 2);
        return partes.length > 1 ? partes[1].trim() : detalhe.trim();
    }

    private static Optional<LocalDate> parseData(String txt, int ano) {
        try {
            MonthDay md = MonthDay.parse(txt, FMT_PARSE);
            return Optional.of(LocalDate.of(ano, md.getMonth(), md.getDayOfMonth()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String datas(List<LocalDate> ds) {
        return ds.stream().sorted().map(FMT_DATA::format).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private static int minutosTurno(String periodo) {
        if (periodo == null || periodo.isBlank()) return 0;
        String[] partes = periodo.trim().split(" - | – ", 2);
        if (partes.length < 2) return 0;
        try {
            LocalTime inicio = LocalTime.parse(partes[0].trim(), HORA_FMT);
            LocalTime fim = LocalTime.parse(partes[1].trim(), HORA_FMT);
            int min = fim.toSecondOfDay() / 60 - inicio.toSecondOfDay() / 60;
            if (min < 0) min += 24 * 60;
            return min;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private static String valorOuVazio(String s) {
        return s == null ? "" : s;
    }
}
