package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Turno;
import com.example.projeto2.Repositories.DayOffRepository;
import com.example.projeto2.Repositories.HorarioRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RelatorioHorasBLL {

    private static final Set<String> CARGOS_COM_RELATORIO = Set.of("gerente", "subgerente");

    private final LojautilizadorRepository lojautilizadorRepository;
    private final HorarioRepository horarioRepository;
    private final DayOffRepository dayOffRepository;

    public RelatorioHorasBLL(LojautilizadorRepository lojautilizadorRepository,
                             HorarioRepository horarioRepository,
                             DayOffRepository dayOffRepository) {
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.horarioRepository = horarioRepository;
        this.dayOffRepository = dayOffRepository;
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeConsultarRelatorios(Integer idUtilizador) {
        return obterLigacaoAtiva(idUtilizador)
                .map(this::temPermissaoDeRelatorio)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public RelatorioContexto obterContexto(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);

        List<FiltroColaborador> colaboradores = lojautilizadorRepository.findAll().stream()
                .filter(lu -> lu.getDataFim() == null)
                .filter(lu -> lu.getIdLoja() != null && lu.getIdLoja().getId().equals(ligacaoAtiva.getIdLoja().getId()))
                .filter(lu -> lu.getIdUtilizador() != null)
                .sorted(Comparator.comparing(lu -> valorOuVazio(lu.getIdUtilizador().getNome()), String.CASE_INSENSITIVE_ORDER))
                .map(lu -> new FiltroColaborador(
                        lu.getIdUtilizador().getId(),
                        lu.getIdUtilizador().getNome(),
                        lu.getIdCargo() != null ? lu.getIdCargo().getNome() : "-"
                ))
                .toList();

        return new RelatorioContexto(
                ligacaoAtiva.getIdLoja().getId(),
                ligacaoAtiva.getIdLoja().getNome(),
                LocalDate.now().getYear(),
                LocalDate.now().getMonthValue(),
                colaboradores
        );
    }

    @Transactional(readOnly = true)
    public RelatorioResultado gerarRelatorio(Integer idUtilizador,
                                             Integer ano,
                                             Integer mes,
                                             Integer idColaborador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        int anoNormalizado = normalizarAno(ano);
        int mesNormalizado = normalizarMes(mes);

        LocalDate dataInicio = LocalDate.of(anoNormalizado, mesNormalizado, 1);
        LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());

        List<Horario> horarios = horarioRepository.findHorariosDaLojaEntreDatas(
                ligacaoAtiva.getIdLoja().getId(),
                dataInicio,
                dataFim
        );

        List<DayOff> folgasAprovadas = dayOffRepository.findPedidosAprovadosDaLojaEntreDatas(
                ligacaoAtiva.getIdLoja().getId(),
                dataInicio,
                dataFim
        );

        Map<Integer, FiltroColaborador> colaboradoresDaLoja = lojautilizadorRepository.findAll().stream()
                .filter(lu -> lu.getDataFim() == null)
                .filter(lu -> lu.getIdLoja() != null && lu.getIdLoja().getId().equals(ligacaoAtiva.getIdLoja().getId()))
                .filter(lu -> lu.getIdUtilizador() != null)
                .collect(java.util.stream.Collectors.toMap(
                        lu -> lu.getIdUtilizador().getId(),
                        lu -> new FiltroColaborador(
                                lu.getIdUtilizador().getId(),
                                lu.getIdUtilizador().getNome(),
                                lu.getIdCargo() != null ? lu.getIdCargo().getNome() : "-"
                        ),
                        (atual, ignorado) -> atual,
                        LinkedHashMap::new
                ));

        Map<Integer, LinhaAcumulada> acumuladoPorColaborador = new LinkedHashMap<>();
        for (Horario horario : horarios) {
            if (horario.getIdLojautilizador() == null || horario.getIdLojautilizador().getIdUtilizador() == null) {
                continue;
            }

            Integer idColaboradorAtual = horario.getIdLojautilizador().getIdUtilizador().getId();
            if (idColaborador != null && !idColaborador.equals(idColaboradorAtual)) {
                continue;
            }

            LinhaAcumulada linha = acumuladoPorColaborador.computeIfAbsent(
                    idColaboradorAtual,
                    ignored -> LinhaAcumulada.fromHorario(horario)
            );
            linha.turnos++;
            linha.minutos += calcularDuracaoEmMinutos(horario.getIdTurno());
        }

        for (DayOff folga : folgasAprovadas) {
            if (folga.getIdUtilizador() == null) {
                continue;
            }

            if (idColaborador != null && !idColaborador.equals(folga.getIdUtilizador())) {
                continue;
            }

            LinhaAcumulada linha = acumuladoPorColaborador.computeIfAbsent(
                    folga.getIdUtilizador(),
                    ignored -> LinhaAcumulada.fromDayOff(
                            folga,
                            colaboradoresDaLoja.get(folga.getIdUtilizador())
                    )
            );
            linha.folgasAprovadas++;
        }

        List<RelatorioLinha> linhas = new ArrayList<>();
        for (LinhaAcumulada linha : acumuladoPorColaborador.values()) {
            linhas.add(new RelatorioLinha(
                    linha.idColaborador,
                    linha.nomeColaborador,
                    linha.cargo,
                    linha.turnos,
                    linha.folgasAprovadas,
                    linha.minutos,
                    formatarDuracao(linha.minutos)
            ));
        }

        linhas.sort(Comparator.comparing(RelatorioLinha::nomeColaborador, String.CASE_INSENSITIVE_ORDER));

        int totalTurnos = linhas.stream().mapToInt(RelatorioLinha::turnos).sum();
        int totalFolgasAprovadas = linhas.stream().mapToInt(RelatorioLinha::folgasAprovadas).sum();
        long totalMinutos = linhas.stream().mapToLong(RelatorioLinha::minutos).sum();

        return new RelatorioResultado(
                ligacaoAtiva.getIdLoja().getNome(),
                nomeMes(mesNormalizado),
                anoNormalizado,
                linhas,
                new RelatorioResumo(
                        linhas.size(),
                        totalTurnos,
                        totalFolgasAprovadas,
                        totalMinutos,
                        formatarDuracao(totalMinutos)
                )
        );
    }

    private java.util.Optional<Lojautilizador> obterLigacaoAtiva(Integer idUtilizador) {
        if (idUtilizador == null) {
            return java.util.Optional.empty();
        }

        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador);
    }

    private Lojautilizador obterLigacaoAtivaComPermissao(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para o utilizador autenticado."));

        if (!temPermissaoDeRelatorio(ligacaoAtiva)) {
            throw new IllegalArgumentException("Nao tens permissao para consultar relatorios mensais.");
        }

        return ligacaoAtiva;
    }

    private boolean temPermissaoDeRelatorio(Lojautilizador ligacaoAtiva) {
        String tipoCargo = ligacaoAtiva.getIdCargo() != null ? ligacaoAtiva.getIdCargo().getTipo() : null;
        return tipoCargo != null && CARGOS_COM_RELATORIO.contains(tipoCargo.toLowerCase());
    }

    private int normalizarAno(Integer ano) {
        if (ano == null || ano < 2020 || ano > 2100) {
            throw new IllegalArgumentException("O ano selecionado e invalido.");
        }
        return ano;
    }

    private int normalizarMes(Integer mes) {
        if (mes == null || mes < 1 || mes > 12) {
            throw new IllegalArgumentException("O mes selecionado e invalido.");
        }
        return mes;
    }

    private long calcularDuracaoEmMinutos(Turno turno) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null) {
            return 0;
        }

        LocalTime horaInicio = turno.getHoraInicio();
        LocalTime horaFim = turno.getHoraFim();

        if (!horaFim.isBefore(horaInicio)) {
            return Duration.between(horaInicio, horaFim).toMinutes();
        }

        return Duration.between(horaInicio, LocalTime.MAX).plusMinutes(1).toMinutes()
                + Duration.between(LocalTime.MIN, horaFim).toMinutes();
    }

    private String formatarDuracao(long minutosTotais) {
        long horas = minutosTotais / 60;
        long minutos = minutosTotais % 60;
        return horas + "h " + minutos + "m";
    }

    private String nomeMes(int mes) {
        return switch (Month.of(mes)) {
            case JANUARY -> "Janeiro";
            case FEBRUARY -> "Fevereiro";
            case MARCH -> "Marco";
            case APRIL -> "Abril";
            case MAY -> "Maio";
            case JUNE -> "Junho";
            case JULY -> "Julho";
            case AUGUST -> "Agosto";
            case SEPTEMBER -> "Setembro";
            case OCTOBER -> "Outubro";
            case NOVEMBER -> "Novembro";
            case DECEMBER -> "Dezembro";
        };
    }

    private String valorOuVazio(String valor) {
        return valor == null ? "" : valor;
    }

    public record RelatorioContexto(
            Integer idLoja,
            String nomeLoja,
            Integer anoAtual,
            Integer mesAtual,
            List<FiltroColaborador> colaboradores
    ) {
    }

    public record FiltroColaborador(
            Integer id,
            String nome,
            String cargo
    ) {
        @Override
        public String toString() {
            return nome;
        }
    }

    public record RelatorioResultado(
            String nomeLoja,
            String nomeMes,
            Integer ano,
            List<RelatorioLinha> linhas,
            RelatorioResumo resumo
    ) {
    }

    public record RelatorioLinha(
            Integer idColaborador,
            String nomeColaborador,
            String cargo,
            int turnos,
            int folgasAprovadas,
            long minutos,
            String horasFormatadas
    ) {
    }

    public record RelatorioResumo(
            int colaboradores,
            int turnos,
            int folgasAprovadas,
            long minutos,
            String horasFormatadas
    ) {
    }

    private static final class LinhaAcumulada {
        private final Integer idColaborador;
        private final String nomeColaborador;
        private final String cargo;
        private int turnos;
        private int folgasAprovadas;
        private long minutos;

        private LinhaAcumulada(Integer idColaborador, String nomeColaborador, String cargo) {
            this.idColaborador = idColaborador;
            this.nomeColaborador = nomeColaborador;
            this.cargo = cargo;
        }

        private static LinhaAcumulada fromHorario(Horario horario) {
            return new LinhaAcumulada(
                    horario.getIdLojautilizador().getIdUtilizador().getId(),
                    horario.getIdLojautilizador().getIdUtilizador().getNome(),
                    horario.getIdLojautilizador().getIdCargo() != null
                            ? horario.getIdLojautilizador().getIdCargo().getNome()
                            : "-"
            );
        }

        private static LinhaAcumulada fromDayOff(DayOff dayOff, FiltroColaborador colaborador) {
            return new LinhaAcumulada(
                    dayOff.getIdUtilizador(),
                    colaborador != null ? colaborador.nome() : "Utilizador #" + dayOff.getIdUtilizador(),
                    colaborador != null ? colaborador.cargo() : "-"
            );
        }
    }
}
