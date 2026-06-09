package com.example.projeto2.API.Services;

import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Enums.EstadoPermuta;
import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Repositories.DayOffRepository;
import com.example.projeto2.API.Repositories.HorarioRepository;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Repositories.PermutaRepository;
import com.example.projeto2.API.Repositories.PreferenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SnapshotOperacionalLojaService {

    private static final Set<String> CARGOS_COM_ACESSO = Set.of("gerente", "subgerente");
    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LojautilizadorRepository lojautilizadorRepository;
    private final HorarioRepository horarioRepository;
    private final DayOffRepository dayOffRepository;
    private final PermutaRepository permutaRepository;
    private final PreferenciaRepository preferenciaRepository;

    public SnapshotOperacionalLojaService(LojautilizadorRepository lojautilizadorRepository,
                                      HorarioRepository horarioRepository,
                                      DayOffRepository dayOffRepository,
                                      PermutaRepository permutaRepository,
                                      PreferenciaRepository preferenciaRepository) {
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.horarioRepository = horarioRepository;
        this.dayOffRepository = dayOffRepository;
        this.permutaRepository = permutaRepository;
        this.preferenciaRepository = preferenciaRepository;
    }

    @Transactional(readOnly = true)
    public SnapshotOperacionalLoja carregarSnapshot(Integer idUtilizadorGestor, LocalDate data) {
        if (data == null) {
            throw new IllegalArgumentException("A data do snapshot e obrigatoria.");
        }

        return carregarSnapshot(idUtilizadorGestor, data, data);
    }

    @Transactional(readOnly = true)
    public SnapshotOperacionalLoja carregarSnapshot(Integer idUtilizadorGestor,
                                                    LocalDate dataInicio,
                                                    LocalDate dataFim) {
        IntervaloOperacional intervalo = normalizarIntervalo(dataInicio, dataFim);
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizadorGestor);
        Integer idLoja = ligacaoAtiva.getIdLoja().getId();

        List<Horario> horarios = intervalo.unicoDia()
                ? horarioRepository.findHorariosDaLojaNoDia(idLoja, intervalo.dataInicio())
                : horarioRepository.findHorariosDaLojaEntreDatas(idLoja, intervalo.dataInicio(), intervalo.dataFim());

        List<DayOff> ausenciasAprovadas = dayOffRepository.findPedidosAprovadosDaLojaEntreDatas(
                idLoja,
                intervalo.dataInicio(),
                intervalo.dataFim()
        );
        List<DayOff> folgasPendentes = dayOffRepository.findPedidosPendentesDaLojaEntreDatas(
                idLoja,
                idUtilizadorGestor,
                intervalo.dataInicio(),
                intervalo.dataFim()
        );
        List<Permuta> permutasPendentes = permutaRepository.findPedidosPendentesDaLojaEntreDatas(
                idLoja,
                idUtilizadorGestor,
                intervalo.dataInicio(),
                intervalo.dataFim()
        );
        List<Preferencia> preferenciasPendentes = preferenciaRepository.findPreferenciasPendentesRelevantesDaLoja(
                idLoja,
                idUtilizadorGestor,
                intervalo.dataInicio(),
                intervalo.dataFim()
        );

        Map<Integer, ColaboradorBase> colaboradoresAtivos = carregarColaboradoresAtivosDaLoja(idLoja);

        List<ColaboradorEscala> equipaEscalada = agruparHorariosPorColaborador(horarios);
        List<AusenciaOperacional> ausencias = ausenciasAprovadas.stream()
                .map(dayOff -> mapAusencia(dayOff, colaboradoresAtivos))
                .toList();
        List<PermutaOperacional> permutas = permutasPendentes.stream()
                .map(this::mapPermuta)
                .toList();

        List<PedidoPendenteOperacional> pedidosPendentes = new ArrayList<>();
        folgasPendentes.forEach(dayOff -> pedidosPendentes.add(mapPedidoFolga(dayOff, colaboradoresAtivos)));
        permutasPendentes.forEach(permuta -> pedidosPendentes.add(mapPedidoPermuta(permuta)));
        preferenciasPendentes.forEach(preferencia -> pedidosPendentes.add(mapPedidoPreferencia(preferencia)));
        pedidosPendentes.sort(Comparator
                .comparing(PedidoPendenteOperacional::dataInicio, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PedidoPendenteOperacional::tipo)
                .thenComparing(PedidoPendenteOperacional::idPedido));

        ContextoLoja contexto = new ContextoLoja(
                idLoja,
                valorOuTraco(ligacaoAtiva.getIdLoja().getNome()),
                valorOuTraco(ligacaoAtiva.getIdLoja().getLocalizacao()),
                ligacaoAtiva.getIdCargo() != null ? valorOuTraco(ligacaoAtiva.getIdCargo().getNome()) : "-"
        );

        ResumoOperacional resumo = new ResumoOperacional(
                equipaEscalada.size(),
                horarios.size(),
                ausencias.size(),
                permutas.size(),
                folgasPendentes.size(),
                preferenciasPendentes.size(),
                pedidosPendentes.size()
        );

        return new SnapshotOperacionalLoja(
                contexto,
                intervalo,
                resumo,
                equipaEscalada,
                ausencias,
                permutas,
                pedidosPendentes
        );
    }

    @Transactional(readOnly = true)
    public ContextoPedidoOperacional carregarContextoPedido(Integer idUtilizadorGestor,
                                                            TipoPedidoOperacional tipoPedido,
                                                            Integer idPedido) {
        if (tipoPedido == null) {
            throw new IllegalArgumentException("O tipo de pedido e obrigatorio.");
        }

        if (idPedido == null) {
            throw new IllegalArgumentException("O pedido selecionado e obrigatorio.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizadorGestor);
        Integer idLoja = ligacaoAtiva.getIdLoja().getId();
        PedidoContexto pedidoContexto = carregarPedidoContexto(idLoja, tipoPedido, idPedido);

        SnapshotOperacionalLoja snapshotRelacionada = carregarSnapshot(
                idUtilizadorGestor,
                pedidoContexto.dataInicio(),
                pedidoContexto.dataFim()
        );

        List<ColaboradorContexto> colaboradores = construirContextoColaboradores(
                idLoja,
                pedidoContexto.idsUtilizadoresEnvolvidos(),
                snapshotRelacionada
        );

        return new ContextoPedidoOperacional(
                pedidoContexto.pedido(),
                colaboradores,
                snapshotRelacionada
        );
    }

    private PedidoContexto carregarPedidoContexto(Integer idLoja,
                                                  TipoPedidoOperacional tipoPedido,
                                                  Integer idPedido) {
        return switch (tipoPedido) {
            case FOLGA -> carregarContextoFolga(idLoja, idPedido);
            case PERMUTA -> carregarContextoPermuta(idLoja, idPedido);
            case PREFERENCIA -> carregarContextoPreferencia(idLoja, idPedido);
        };
    }

    private PedidoContexto carregarContextoFolga(Integer idLoja, Integer idPedido) {
        DayOff dayOff = dayOffRepository.findPedidoDaLojaById(idLoja, idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de folga nao encontrado."));

        if (!"pendente".equalsIgnoreCase(dayOff.getEstado())) {
            throw new IllegalArgumentException("Este pedido de folga ja nao esta pendente.");
        }

        Map<Integer, ColaboradorBase> colaboradoresAtivos = carregarColaboradoresAtivosDaLoja(idLoja);
        PedidoPendenteOperacional pedido = mapPedidoFolga(dayOff, colaboradoresAtivos);
        LocalDate dataReferencia = dayOff.getDataAusencia();

        return new PedidoContexto(
                pedido,
                dataReferencia,
                dataReferencia,
                List.of(dayOff.getIdUtilizador().getId())
        );
    }

    private PedidoContexto carregarContextoPermuta(Integer idLoja, Integer idPedido) {
        Permuta permuta = permutaRepository.findDetalhadaById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de permuta nao encontrado."));

        if (com.example.projeto2.API.Enums.EstadoPermuta.pendente != permuta.getEstado()) {
            throw new IllegalArgumentException("Este pedido de permuta ja nao esta pendente.");
        }

        Integer idLojaOrigem = permuta.getIdHorarioOrigem().getIdLojautilizador().getIdLoja().getId();
        Integer idLojaDestino = permuta.getIdHorarioDestino().getIdLojautilizador().getIdLoja().getId();

        if (!idLoja.equals(idLojaOrigem) || !idLoja.equals(idLojaDestino)) {
            throw new IllegalArgumentException("Nao tens permissao para consultar este pedido de permuta.");
        }

        PedidoPendenteOperacional pedido = mapPedidoPermuta(permuta);
        LocalDate dataReferencia = permuta.getIdHorarioOrigem().getDataTurno();
        Integer idSolicitante = permuta.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getId();
        Integer idColega = permuta.getIdHorarioDestino().getIdLojautilizador().getIdUtilizador().getId();

        return new PedidoContexto(
                pedido,
                dataReferencia,
                dataReferencia,
                List.of(idSolicitante, idColega)
        );
    }

    private PedidoContexto carregarContextoPreferencia(Integer idLoja, Integer idPedido) {
        Preferencia preferencia = preferenciaRepository.findPreferenciaDaLoja(idPedido, idLoja)
                .orElseThrow(() -> new IllegalArgumentException("Preferencia nao encontrada."));

        if (!"pendente".equalsIgnoreCase(preferencia.getEstado())) {
            throw new IllegalArgumentException("Esta preferencia ja nao esta pendente.");
        }

        IntervaloOperacional intervalo = resolverIntervaloDaPreferencia(preferencia);
        PedidoPendenteOperacional pedido = mapPedidoPreferencia(preferencia);
        Integer idUtilizador = preferencia.getIdUtilizador() != null ? preferencia.getIdUtilizador().getId() : null;

        return new PedidoContexto(
                pedido,
                intervalo.dataInicio(),
                intervalo.dataFim(),
                idUtilizador == null ? List.of() : List.of(idUtilizador)
        );
    }

    private List<ColaboradorContexto> construirContextoColaboradores(Integer idLoja,
                                                                     List<Integer> idsUtilizadores,
                                                                     SnapshotOperacionalLoja snapshot) {
        Map<Integer, ColaboradorEscala> equipaPorUtilizador = new LinkedHashMap<>();
        snapshot.equipaEscalada().forEach(colaborador -> equipaPorUtilizador.put(colaborador.idUtilizador(), colaborador));

        Map<Integer, List<AusenciaOperacional>> ausenciasPorUtilizador = new LinkedHashMap<>();
        for (AusenciaOperacional ausencia : snapshot.ausencias()) {
            ausenciasPorUtilizador.computeIfAbsent(ausencia.idUtilizador(), ignored -> new ArrayList<>()).add(ausencia);
        }

        Map<Integer, ColaboradorBase> colaboradoresAtivos = carregarColaboradoresAtivosDaLoja(idLoja);
        List<ColaboradorContexto> contexto = new ArrayList<>();
        LinkedHashSet<Integer> idsOrdenados = new LinkedHashSet<>(idsUtilizadores);

        for (Integer idUtilizador : idsOrdenados) {
            if (idUtilizador == null) {
                continue;
            }

            ColaboradorEscala colaboradorEscala = equipaPorUtilizador.get(idUtilizador);
            ColaboradorBase colaboradorBase = colaboradoresAtivos.get(idUtilizador);

            String nome = colaboradorEscala != null
                    ? colaboradorEscala.nome()
                    : colaboradorBase != null ? colaboradorBase.nome() : "Utilizador #" + idUtilizador;
            String cargo = colaboradorEscala != null
                    ? colaboradorEscala.cargo()
                    : colaboradorBase != null ? colaboradorBase.cargo() : "-";

            contexto.add(new ColaboradorContexto(
                    idUtilizador,
                    nome,
                    cargo,
                    colaboradorEscala != null ? colaboradorEscala.turnos() : List.of(),
                    ausenciasPorUtilizador.getOrDefault(idUtilizador, List.of())
            ));
        }

        return contexto;
    }

    private Map<Integer, ColaboradorBase> carregarColaboradoresAtivosDaLoja(Integer idLoja) {
        Map<Integer, ColaboradorBase> colaboradores = new LinkedHashMap<>();

        for (Lojautilizador ligacao : lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja)) {
            if (ligacao.getDataFim() != null
                    || ligacao.getIdUtilizador() == null
                    || ligacao.getIdUtilizador().getId() == null) {
                continue;
            }

            Integer idUtilizador = ligacao.getIdUtilizador().getId();
            colaboradores.putIfAbsent(
                    idUtilizador,
                    new ColaboradorBase(
                            idUtilizador,
                            valorOuFallback(ligacao.getIdUtilizador().getNome(), "Utilizador #" + idUtilizador),
                            ligacao.getIdCargo() != null ? valorOuTraco(ligacao.getIdCargo().getNome()) : "-"
                    )
            );
        }

        return colaboradores;
    }

    private List<ColaboradorEscala> agruparHorariosPorColaborador(List<Horario> horarios) {
        Map<Integer, AgrupadorColaborador> agrupados = new LinkedHashMap<>();

        for (Horario horario : horarios) {
            if (horario.getIdLojautilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador().getId() == null) {
                continue;
            }

            Integer idUtilizador = horario.getIdLojautilizador().getIdUtilizador().getId();
            AgrupadorColaborador agrupador = agrupados.computeIfAbsent(
                    idUtilizador,
                    ignored -> new AgrupadorColaborador(
                            idUtilizador,
                            valorOuFallback(horario.getIdLojautilizador().getIdUtilizador().getNome(), "Utilizador #" + idUtilizador),
                            horario.getIdLojautilizador().getIdCargo() != null
                                    ? valorOuTraco(horario.getIdLojautilizador().getIdCargo().getNome())
                                    : "-",
                            new ArrayList<>()
                    )
            );

            agrupador.turnos().add(new TurnoPlaneado(
                    horario.getId(),
                    horario.getDataTurno(),
                    capitalizar(textoOuTraco(horario.getIdTurno().getTipo())),
                    formatarPeriodo(horario),
                    valorOuTraco(horario.getEstado() != null ? horario.getEstado().name() : null)
            ));
        }

        return agrupados.values().stream()
                .map(agrupador -> new ColaboradorEscala(
                        agrupador.idUtilizador(),
                        agrupador.nome(),
                        agrupador.cargo(),
                        agrupador.turnos()
                ))
                .toList();
    }

    private AusenciaOperacional mapAusencia(DayOff dayOff, Map<Integer, ColaboradorBase> colaboradoresAtivos) {
        ColaboradorBase colaborador = colaboradoresAtivos.get(dayOff.getIdUtilizador().getId());
        return new AusenciaOperacional(
                dayOff.getIdDayoff(),
                dayOff.getIdUtilizador().getId(),
                colaborador != null ? colaborador.nome() : "Utilizador #" + dayOff.getIdUtilizador().getId(),
                dayOff.getDataAusencia(),
                capitalizar(valorOuTraco(dayOff.getTipo())),
                valorOuTraco(dayOff.getMotivo()),
                valorOuTraco(dayOff.getEstado())
        );
    }

    private PermutaOperacional mapPermuta(Permuta permuta) {
        Horario horarioOrigem = permuta.getIdHorarioOrigem();
        Horario horarioDestino = permuta.getIdHorarioDestino();

        String nomeSolicitante = horarioOrigem.getIdLojautilizador().getIdUtilizador().getNome();
        String nomeColega = horarioDestino.getIdLojautilizador().getIdUtilizador().getNome();

        return new PermutaOperacional(
                permuta.getId(),
                horarioOrigem.getDataTurno(),
                valorOuTraco(permuta.getEstado() != null ? permuta.getEstado().name() : null),
                valorOuFallback(nomeSolicitante, "Solicitante"),
                valorOuFallback(nomeColega, "Colega"),
                formatarPeriodo(horarioOrigem),
                formatarPeriodo(horarioDestino)
        );
    }

    private PedidoPendenteOperacional mapPedidoFolga(DayOff dayOff, Map<Integer, ColaboradorBase> colaboradoresAtivos) {
        ColaboradorBase colaborador = colaboradoresAtivos.get(dayOff.getIdUtilizador().getId());
        String nome = colaborador != null ? colaborador.nome() : "Utilizador #" + dayOff.getIdUtilizador().getId();

        return new PedidoPendenteOperacional(
                dayOff.getIdDayoff(),
                TipoPedidoOperacional.FOLGA,
                dayOff.getDataAusencia(),
                dayOff.getDataAusencia(),
                nome,
                "Pedido de " + valorOuFallback(dayOff.getTipo(), "folga")
                        + " para " + formatarData(dayOff.getDataAusencia()),
                List.of(dayOff.getIdUtilizador().getId())
        );
    }

    private PedidoPendenteOperacional mapPedidoPermuta(Permuta permuta) {
        Horario horarioOrigem = permuta.getIdHorarioOrigem();
        Horario horarioDestino = permuta.getIdHorarioDestino();
        Integer idSolicitante = horarioOrigem.getIdLojautilizador().getIdUtilizador().getId();
        Integer idColega = horarioDestino.getIdLojautilizador().getIdUtilizador().getId();

        return new PedidoPendenteOperacional(
                permuta.getId(),
                TipoPedidoOperacional.PERMUTA,
                horarioOrigem.getDataTurno(),
                horarioOrigem.getDataTurno(),
                valorOuFallback(horarioOrigem.getIdLojautilizador().getIdUtilizador().getNome(), "Solicitante"),
                "Permuta com " + valorOuFallback(horarioDestino.getIdLojautilizador().getIdUtilizador().getNome(), "colega")
                        + " para " + formatarData(horarioOrigem.getDataTurno()),
                List.of(idSolicitante, idColega)
        );
    }

    private PedidoPendenteOperacional mapPedidoPreferencia(Preferencia preferencia) {
        LocalDate dataInicio = preferencia.getDataInicio();
        LocalDate dataFim = preferencia.getDataFim();
        Integer idUtilizador = preferencia.getIdUtilizador() != null ? preferencia.getIdUtilizador().getId() : null;

        return new PedidoPendenteOperacional(
                preferencia.getId(),
                TipoPedidoOperacional.PREFERENCIA,
                dataInicio,
                dataFim,
                preferencia.getIdUtilizador() != null
                        ? valorOuFallback(preferencia.getIdUtilizador().getNome(), "Colaborador")
                        : "Colaborador",
                "Preferencia de " + capitalizar(valorOuTraco(preferencia.getTipo()))
                        + " (prioridade " + preferencia.getPrioridade() + ")",
                idUtilizador == null ? List.of() : List.of(idUtilizador)
        );
    }

    private IntervaloOperacional resolverIntervaloDaPreferencia(Preferencia preferencia) {
        LocalDate dataInicio = preferencia.getDataInicio();
        LocalDate dataFim = preferencia.getDataFim();

        if (dataInicio == null && dataFim == null) {
            LocalDate hoje = LocalDate.now();
            return new IntervaloOperacional(hoje, hoje, true);
        }

        LocalDate inicio = dataInicio != null ? dataInicio : dataFim;
        LocalDate fim = dataFim != null ? dataFim : dataInicio;
        return normalizarIntervalo(inicio, fim);
    }

    private IntervaloOperacional normalizarIntervalo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("As datas do snapshot sao obrigatorias.");
        }

        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final nao pode ser anterior a data inicial.");
        }

        return new IntervaloOperacional(dataInicio, dataFim, dataInicio.equals(dataFim));
    }

    private Lojautilizador obterLigacaoAtivaComPermissao(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        Lojautilizador ligacaoAtiva = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa a uma loja."));

        String tipoCargo = ligacaoAtiva.getIdCargo() != null && ligacaoAtiva.getIdCargo().getTipo() != null
                ? ligacaoAtiva.getIdCargo().getTipo().toLowerCase()
                : "";

        if (!CARGOS_COM_ACESSO.contains(tipoCargo)) {
            throw new IllegalArgumentException("Nao tens permissao para consultar o snapshot operacional da loja.");
        }

        return ligacaoAtiva;
    }

    private String formatarPeriodo(Horario horario) {
        if (horario == null || horario.getIdTurno() == null) {
            return "-";
        }

        return horario.getIdTurno().getHoraInicio() + " - " + horario.getIdTurno().getHoraFim();
    }

    private String formatarData(LocalDate data) {
        return data != null ? DATA_FORMATTER.format(data) : "-";
    }

    private String valorOuTraco(String valor) {
        return valorOuFallback(valor, "-");
    }

    private String textoOuTraco(Object valor) {
        if (valor == null) {
            return "-";
        }
        return valorOuFallback(String.valueOf(valor), "-");
    }

    private String valorOuFallback(String valor, String fallback) {
        if (valor == null || valor.isBlank()) {
            return fallback;
        }
        return valor.trim();
    }

    private String capitalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }

        String normalizado = valor.trim().toLowerCase();
        return Character.toUpperCase(normalizado.charAt(0)) + normalizado.substring(1);
    }

    public enum TipoPedidoOperacional {
        FOLGA,
        PERMUTA,
        PREFERENCIA
    }

    public record ContextoLoja(
            Integer idLoja,
            String nomeLoja,
            String localizacao,
            String cargoGestao
    ) {
    }

    public record IntervaloOperacional(
            LocalDate dataInicio,
            LocalDate dataFim,
            boolean unicoDia
    ) {
    }

    public record ResumoOperacional(
            int colaboradoresEscalados,
            int turnosPlaneados,
            int ausenciasAprovadas,
            int permutasPendentes,
            int folgasPendentes,
            int preferenciasPendentes,
            int totalPedidosPendentes
    ) {
    }

    public record TurnoPlaneado(
            Integer idHorario,
            LocalDate data,
            String turno,
            String periodo,
            String estado
    ) {
    }

    public record ColaboradorEscala(
            Integer idUtilizador,
            String nome,
            String cargo,
            List<TurnoPlaneado> turnos
    ) {
    }

    public record AusenciaOperacional(
            Integer idDayOff,
            Integer idUtilizador,
            String colaborador,
            LocalDate data,
            String tipo,
            String motivo,
            String estado
    ) {
    }

    public record PermutaOperacional(
            Integer idPermuta,
            LocalDate data,
            String estado,
            String solicitante,
            String colega,
            String periodoOrigem,
            String periodoDestino
    ) {
    }

    public record PedidoPendenteOperacional(
            Integer idPedido,
            TipoPedidoOperacional tipo,
            LocalDate dataInicio,
            LocalDate dataFim,
            String colaboradorPrincipal,
            String resumo,
            List<Integer> idsUtilizadoresEnvolvidos
    ) {
    }

    public record SnapshotOperacionalLoja(
            ContextoLoja contexto,
            IntervaloOperacional intervalo,
            ResumoOperacional resumo,
            List<ColaboradorEscala> equipaEscalada,
            List<AusenciaOperacional> ausencias,
            List<PermutaOperacional> permutasPendentes,
            List<PedidoPendenteOperacional> pedidosPendentes
    ) {
    }

    public record ColaboradorContexto(
            Integer idUtilizador,
            String nome,
            String cargo,
            List<TurnoPlaneado> turnosNoPeriodo,
            List<AusenciaOperacional> ausenciasNoPeriodo
    ) {
    }

    public record ContextoPedidoOperacional(
            PedidoPendenteOperacional pedido,
            List<ColaboradorContexto> colaboradoresEnvolvidos,
            SnapshotOperacionalLoja snapshotRelacionada
    ) {
    }

    private record ColaboradorBase(
            Integer idUtilizador,
            String nome,
            String cargo
    ) {
    }

    private record AgrupadorColaborador(
            Integer idUtilizador,
            String nome,
            String cargo,
            List<TurnoPlaneado> turnos
    ) {
    }

    private record PedidoContexto(
            PedidoPendenteOperacional pedido,
            LocalDate dataInicio,
            LocalDate dataFim,
            List<Integer> idsUtilizadoresEnvolvidos
    ) {
    }
}
