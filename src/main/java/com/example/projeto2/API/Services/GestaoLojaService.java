package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.HorarioEspecialLoja;
import com.example.projeto2.API.Modules.Loja;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Regra;
import com.example.projeto2.API.Modules.RegrasLoja;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Repositories.HorarioEspecialLojaRepository;
import com.example.projeto2.API.Repositories.LojaRepository;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.valorOuTraco;
import com.example.projeto2.API.Repositories.RegraRepository;
import com.example.projeto2.API.Repositories.RegrasLojaRepository;
import com.example.projeto2.API.Repositories.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class GestaoLojaService {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LojautilizadorHelper lojautilizadorHelper;
    private final LojaRepository lojaRepository;
    private final RegraRepository regraRepository;
    private final RegrasLojaRepository regrasLojaRepository;
    private final HorarioEspecialLojaRepository horarioEspecialLojaRepository;
    private final TurnoRepository turnoRepository;

    public GestaoLojaService(LojautilizadorHelper lojautilizadorHelper,
                         LojaRepository lojaRepository,
                         RegraRepository regraRepository,
                         RegrasLojaRepository regrasLojaRepository,
                         HorarioEspecialLojaRepository horarioEspecialLojaRepository,
                         TurnoRepository turnoRepository) {
        this.lojautilizadorHelper = lojautilizadorHelper;
        this.lojaRepository = lojaRepository;
        this.regraRepository = regraRepository;
        this.regrasLojaRepository = regrasLojaRepository;
        this.horarioEspecialLojaRepository = horarioEspecialLojaRepository;
        this.turnoRepository = turnoRepository;
    }

    @Transactional(readOnly = true)
    public String obterNomeCargo(Integer idUtilizador) {
        if (idUtilizador == null) return null;
        return lojautilizadorHelper.findLigacaoAtiva(idUtilizador)
                .map(Lojautilizador::getIdCargo)
                .map(cargo -> cargo.getNome() != null ? cargo.getNome() : cargo.getTipo())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeGerirLoja(Integer idUtilizador) {
        if (idUtilizador == null) {
            return false;
        }
        return lojautilizadorHelper.temCargo(idUtilizador, LojautilizadorHelper.GESTAO);
    }

    @Transactional(readOnly = true)
    public GestaoLojaResumo obterResumo(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Loja loja = ligacaoAtiva.getIdLoja();
        List<Turno> turnosBase = turnoRepository.findAllByOrderByHoraInicioAsc();

        Map<Integer, RegrasLoja> regrasEspecificas = new LinkedHashMap<>();
        for (RegrasLoja regraLoja : regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(loja.getId())) {
            if (regraLoja.getIdRegra() != null && regraLoja.getIdRegra().getId() != null) {
                regrasEspecificas.put(regraLoja.getIdRegra().getId(), regraLoja);
            }
        }

        List<RegraLojaResumo> regras = regraRepository.findAllByOrderByDescricaoAsc().stream()
                .sorted(Comparator.comparing(
                        regra -> regra.getDescricao() != null ? regra.getDescricao() : "",
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(regra -> criarResumoRegra(regra, regrasEspecificas.get(regra.getId())))
                .toList();

        List<HorarioEspecialResumo> horariosEspeciais = horarioEspecialLojaRepository.findByIdLojaOrderByPeriodo(loja.getId()).stream()
                .map(horarioEspecial -> criarResumoHorarioEspecial(horarioEspecial, loja, turnosBase))
                .toList();

        return new GestaoLojaResumo(
                loja.getId(),
                valorOuTraco(loja.getNome()),
                valorOuTraco(loja.getLocalizacao()),
                formatarHora(loja.getHoraAbertura()),
                formatarHora(loja.getHoraFecho()),
                ligacaoAtiva.getIdCargo() != null ? valorOuTraco(ligacaoAtiva.getIdCargo().getNome()) : "-",
                regras,
                horariosEspeciais
        );
    }

    @Transactional
    public void guardarConfiguracao(Integer idUtilizador, ConfiguracaoLojaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A configuracao da loja e obrigatoria.");
        }

        if (request.horaAbertura() == null || request.horaFecho() == null) {
            throw new IllegalArgumentException("As horas de abertura e fecho sao obrigatorias.");
        }

        if (request.horaAbertura().equals(request.horaFecho())) {
            throw new IllegalArgumentException("A hora de abertura e a hora de fecho nao podem ser iguais.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Loja loja = ligacaoAtiva.getIdLoja();

        loja.setHoraAbertura(request.horaAbertura());
        loja.setHoraFecho(request.horaFecho());
        lojaRepository.save(loja);

        if (request.regras() == null) {
            return;
        }

        for (ConfiguracaoRegraRequest regraRequest : request.regras()) {
            if (regraRequest == null || regraRequest.idRegra() == null) {
                continue;
            }

            Regra regra = regraRepository.findById(regraRequest.idRegra())
                    .orElseThrow(() -> new IllegalArgumentException("Foi encontrada uma regra invalida no formulario."));

            Integer valorEspecifico = regraRequest.valorEspecifico();
            if (valorEspecifico != null && valorEspecifico < 0) {
                throw new IllegalArgumentException("Os valores especificos das regras nao podem ser negativos.");
            }

            String observacoes = limparTexto(regraRequest.observacoes());
            Optional<RegrasLoja> regraExistente = regrasLojaRepository.findByIdLojaIdAndIdRegraId(loja.getId(), regra.getId());

            if (valorEspecifico == null && observacoes == null) {
                regraExistente.ifPresent(regrasLojaRepository::delete);
                continue;
            }

            RegrasLoja regraLoja = regraExistente.orElseGet(RegrasLoja::new);
            regraLoja.setIdLoja(loja);
            regraLoja.setIdRegra(regra);
            regraLoja.setValorEspecifico(valorEspecifico);
            regraLoja.setObservacoes(observacoes);
            regrasLojaRepository.save(regraLoja);
        }
    }

    @Transactional
    public void guardarHorarioEspecial(Integer idUtilizador, ConfiguracaoHorarioEspecialRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A configuracao do horario especial e obrigatoria.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Loja loja = ligacaoAtiva.getIdLoja();

        String descricao = limparTexto(request.descricao());
        if (descricao == null) {
            throw new IllegalArgumentException("Indica uma descricao curta para identificar a excecao.");
        }

        LocalDate dataInicio = request.dataInicio();
        LocalDate dataFim = request.dataFim();
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Seleciona a data inicial e a data final da excecao.");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final nao pode ser anterior a data inicial.");
        }

        boolean lojaEncerrada = request.lojaEncerrada();
        LocalTime horaAbertura = request.horaAbertura();
        LocalTime horaFecho = request.horaFecho();
        Integer minimoColaboradoresTurno = request.minimoColaboradoresTurno();
        if (minimoColaboradoresTurno != null && minimoColaboradoresTurno <= 0) {
            throw new IllegalArgumentException("O minimo especial por turno deve ser superior a zero.");
        }

        if (lojaEncerrada) {
            horaAbertura = null;
            horaFecho = null;
            minimoColaboradoresTurno = null;
        } else {
            if ((horaAbertura == null) != (horaFecho == null)) {
                throw new IllegalArgumentException("Preenche as duas horas do horario especial ou deixa ambas em branco.");
            }
            if (horaAbertura != null && horaAbertura.equals(horaFecho)) {
                throw new IllegalArgumentException("A hora de abertura especial e a hora de fecho especial nao podem ser iguais.");
            }
            if (horaAbertura == null && minimoColaboradoresTurno == null) {
                throw new IllegalArgumentException("Define um horario especial, um minimo por turno ou marca a loja como encerrada.");
            }

            LocalTime aberturaEfetiva = horaAbertura != null ? horaAbertura : loja.getHoraAbertura();
            LocalTime fechoEfetivo = horaFecho != null ? horaFecho : loja.getHoraFecho();
            if (aberturaEfetiva == null || fechoEfetivo == null || aberturaEfetiva.equals(fechoEfetivo)) {
                throw new IllegalArgumentException("O horario base da loja nao permite aplicar esta excecao.");
            }

            List<Turno> turnosCompativeis = filtrarTurnosCompativeis(turnoRepository.findAllByOrderByHoraInicioAsc(), aberturaEfetiva, fechoEfetivo);
            if (turnosCompativeis.isEmpty()) {
                throw new IllegalArgumentException("Nao existe nenhum turno base compativel com o horario especial indicado.");
            }
        }

        if (horarioEspecialLojaRepository.existsConflitoDePeriodo(
                loja.getId(),
                dataInicio,
                dataFim,
                request.idHorarioEspecial()
        )) {
            throw new IllegalArgumentException("Ja existe uma excecao configurada para parte do periodo selecionado nesta loja.");
        }

        HorarioEspecialLoja horarioEspecial = request.idHorarioEspecial() != null
                ? horarioEspecialLojaRepository.findByIdAndIdLojaId(request.idHorarioEspecial(), loja.getId())
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada a excecao selecionada para editar."))
                : new HorarioEspecialLoja();

        horarioEspecial.setIdLoja(loja);
        horarioEspecial.setDescricao(descricao);
        horarioEspecial.setDataInicio(dataInicio);
        horarioEspecial.setDataFim(dataFim);
        horarioEspecial.setHoraAbertura(horaAbertura);
        horarioEspecial.setHoraFecho(horaFecho);
        horarioEspecial.setMinimoColaboradoresTurno(minimoColaboradoresTurno);
        horarioEspecial.setLojaEncerrada(lojaEncerrada);
        horarioEspecial.setObservacoes(limparTexto(request.observacoes()));
        horarioEspecialLojaRepository.save(horarioEspecial);
    }

    @Transactional
    public void removerHorarioEspecial(Integer idUtilizador, Integer idHorarioEspecial) {
        if (idHorarioEspecial == null) {
            throw new IllegalArgumentException("Seleciona uma excecao antes de a remover.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        HorarioEspecialLoja horarioEspecial = horarioEspecialLojaRepository.findByIdAndIdLojaId(
                        idHorarioEspecial,
                        ligacaoAtiva.getIdLoja().getId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada a excecao selecionada para remover."));

        horarioEspecialLojaRepository.delete(horarioEspecial);
    }

    private Lojautilizador obterLigacaoAtivaComPermissao(Integer idUtilizador) {
        return lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizador, LojautilizadorHelper.GESTAO,
                "Nao tens permissao para gerir a configuracao da loja.");
    }

    private RegraLojaResumo criarResumoRegra(Regra regra, RegrasLoja regraLoja) {
        return new RegraLojaResumo(
                regra.getId(),
                valorOuTraco(regra.getDescricao()),
                regra.getTipo(),
                regra.getValorPadrao(),
                regraLoja != null ? regraLoja.getValorEspecifico() : null,
                regraLoja != null ? regraLoja.getObservacoes() : null
        );
    }

    private HorarioEspecialResumo criarResumoHorarioEspecial(HorarioEspecialLoja horarioEspecial, Loja loja, List<Turno> turnosBase) {
        LocalTime aberturaEfetiva = horarioEspecial.getHoraAbertura() != null ? horarioEspecial.getHoraAbertura() : loja.getHoraAbertura();
        LocalTime fechoEfetivo = horarioEspecial.getHoraFecho() != null ? horarioEspecial.getHoraFecho() : loja.getHoraFecho();

        String tipoOperacao;
        if (Boolean.TRUE.equals(horarioEspecial.getLojaEncerrada())) {
            tipoOperacao = "Loja encerrada";
        } else if (horarioEspecial.getHoraAbertura() != null || horarioEspecial.getHoraFecho() != null) {
            tipoOperacao = horarioEspecial.getMinimoColaboradoresTurno() != null
                    ? "Horario especial com minimo reforcado"
                    : "Horario especial";
        } else {
            tipoOperacao = "Minimo especial por turno";
        }

        String turnosCompativeis = Boolean.TRUE.equals(horarioEspecial.getLojaEncerrada())
                ? "Sem geracao neste periodo"
                : filtrarTurnosCompativeis(turnosBase, aberturaEfetiva, fechoEfetivo).stream()
                .map(this::formatarTurno)
                .distinct()
                .reduce((primeiro, segundo) -> primeiro + ", " + segundo)
                .orElse("Sem turnos compativeis");

        return new HorarioEspecialResumo(
                horarioEspecial.getId(),
                horarioEspecial.getDescricao(),
                horarioEspecial.getDataInicio(),
                horarioEspecial.getDataFim(),
                Boolean.TRUE.equals(horarioEspecial.getLojaEncerrada()),
                horarioEspecial.getHoraAbertura(),
                horarioEspecial.getHoraFecho(),
                horarioEspecial.getMinimoColaboradoresTurno(),
                horarioEspecial.getObservacoes(),
                formatarPeriodoDatas(horarioEspecial.getDataInicio(), horarioEspecial.getDataFim()),
                tipoOperacao,
                Boolean.TRUE.equals(horarioEspecial.getLojaEncerrada())
                        ? "Encerrada"
                        : formatarHorarioAplicado(
                        aberturaEfetiva,
                        fechoEfetivo,
                        horarioEspecial.getHoraAbertura() == null && horarioEspecial.getHoraFecho() == null
                ),
                turnosCompativeis
        );
    }

    private List<Turno> filtrarTurnosCompativeis(List<Turno> turnosBase, LocalTime abertura, LocalTime fecho) {
        List<Turno> turnosComCorrespondenciaExata = turnosBase.stream()
                .filter(turno -> turno != null)
                .filter(turno -> abertura != null && fecho != null)
                .filter(turno -> intervaloCorrespondeExatamente(turno, abertura, fecho))
                .sorted(Comparator.comparing(Turno::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (!turnosComCorrespondenciaExata.isEmpty()) {
            return turnosComCorrespondenciaExata;
        }

        return turnosBase.stream()
                .filter(turno -> turnoCabeNoHorario(turno, abertura, fecho))
                .sorted(Comparator.comparing(Turno::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private boolean turnoCabeNoHorario(Turno turno, LocalTime abertura, LocalTime fecho) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null || abertura == null || fecho == null) {
            return false;
        }
        int aberturaMinutos = abertura.toSecondOfDay() / 60;
        int fechoMinutos = fecho.toSecondOfDay() / 60;
        if (fechoMinutos <= aberturaMinutos) {
            fechoMinutos += 24 * 60;
        }

        int inicioTurno = turno.getHoraInicio().toSecondOfDay() / 60;
        if (inicioTurno < aberturaMinutos) {
            inicioTurno += 24 * 60;
        }

        int fimTurno = turno.getHoraFim().toSecondOfDay() / 60;
        if (fimTurno <= inicioTurno) {
            fimTurno += 24 * 60;
        }

        return inicioTurno >= aberturaMinutos && fimTurno <= fechoMinutos;
    }

    private boolean intervaloCorrespondeExatamente(Turno turno, LocalTime abertura, LocalTime fecho) {
        return turno != null
                && turno.getHoraInicio() != null
                && turno.getHoraFim() != null
                && turno.getHoraInicio().equals(abertura)
                && turno.getHoraFim().equals(fecho);
    }

    private String formatarTurno(Turno turno) {
        if (turno == null || turno.getTipo() == null) {
            return "-";
        }
        String tipo = String.valueOf(turno.getTipo());
        return Character.toUpperCase(tipo.charAt(0)) + tipo.substring(1).toLowerCase()
                + " (" + formatarHora(turno.getHoraInicio()) + " - " + formatarHora(turno.getHoraFim()) + ")";
    }

    private String formatarPeriodoDatas(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            return "-";
        }
        if (dataInicio.equals(dataFim)) {
            return dataInicio.format(DATA_FORMATTER);
        }
        return dataInicio.format(DATA_FORMATTER) + " ate " + dataFim.format(DATA_FORMATTER);
    }

    private String formatarHorarioAplicado(LocalTime horaAbertura, LocalTime horaFecho, boolean usaHorarioBase) {
        if (horaAbertura == null || horaFecho == null) {
            return "-";
        }
        String prefixo = usaHorarioBase ? "Base da loja: " : "";
        String sufixo = horaFecho.isAfter(horaAbertura) ? "" : " (dia seguinte)";
        return prefixo + formatarHora(horaAbertura) + " - " + formatarHora(horaFecho) + sufixo;
    }

    private String limparTexto(String texto) {
        if (texto == null) {
            return null;
        }

        String textoLimpo = texto.trim();
        return textoLimpo.isEmpty() ? null : textoLimpo;
    }

    private String formatarHora(LocalTime hora) {
        if (hora == null) {
            return "";
        }

        return hora.format(HORA_FORMATTER);
    }


    public record GestaoLojaResumo(
            Integer idLoja,
            String nomeLoja,
            String localizacao,
            String horaAbertura,
            String horaFecho,
            String cargoGestor,
            List<RegraLojaResumo> regras,
            List<HorarioEspecialResumo> horariosEspeciais
    ) {
    }

    public record RegraLojaResumo(
            Integer idRegra,
            String descricao,
            String tipo,
            Integer valorPadrao,
            Integer valorEspecifico,
            String observacoes
    ) {
    }

    public record HorarioEspecialResumo(
            Integer idHorarioEspecial,
            String descricao,
            LocalDate dataInicio,
            LocalDate dataFim,
            boolean lojaEncerrada,
            LocalTime horaAbertura,
            LocalTime horaFecho,
            Integer minimoColaboradoresTurno,
            String observacoes,
            String periodo,
            String tipoOperacao,
            String horarioAplicado,
            String turnosCompativeis
    ) {
    }

    public record ConfiguracaoLojaRequest(
            LocalTime horaAbertura,
            LocalTime horaFecho,
            List<ConfiguracaoRegraRequest> regras
    ) {
    }

    public record ConfiguracaoRegraRequest(
            Integer idRegra,
            Integer valorEspecifico,
            String observacoes
    ) {
    }

    public record ConfiguracaoHorarioEspecialRequest(
            Integer idHorarioEspecial,
            String descricao,
            LocalDate dataInicio,
            LocalDate dataFim,
            boolean lojaEncerrada,
            LocalTime horaAbertura,
            LocalTime horaFecho,
            Integer minimoColaboradoresTurno,
            String observacoes
    ) {
    }
}
