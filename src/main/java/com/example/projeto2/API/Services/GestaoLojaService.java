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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class GestaoLojaService {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @PersistenceContext
    private EntityManager entityManager;

    private final LojautilizadorHelper lojautilizadorHelper;
    private final LojaRepository lojaRepository;
    private final RegraRepository regraRepository;
    private final RegrasLojaRepository regrasLojaRepository;
    private final HorarioEspecialLojaRepository horarioEspecialLojaRepository;
    private final TurnoRepository turnoRepository;
    private final SessaoService sessaoBLL;

    public GestaoLojaService(LojautilizadorHelper lojautilizadorHelper,
                         LojaRepository lojaRepository,
                         RegraRepository regraRepository,
                         RegrasLojaRepository regrasLojaRepository,
                         HorarioEspecialLojaRepository horarioEspecialLojaRepository,
                         TurnoRepository turnoRepository,
                         SessaoService sessaoBLL) {
        this.lojautilizadorHelper = lojautilizadorHelper;
        this.lojaRepository = lojaRepository;
        this.regraRepository = regraRepository;
        this.regrasLojaRepository = regrasLojaRepository;
        this.horarioEspecialLojaRepository = horarioEspecialLojaRepository;
        this.turnoRepository = turnoRepository;
        this.sessaoBLL = sessaoBLL;
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
        if (idUtilizador == null) return false;
        return lojautilizadorHelper.temCargo(idUtilizador, LojautilizadorHelper.GESTAO);
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeGerirLoja(Integer idUtilizador, Integer idLoja) {
        if (idUtilizador == null) return false;
        return lojautilizadorHelper.temCargo(idUtilizador, idLoja, LojautilizadorHelper.GESTAO);
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

        List<RegraLojaResumo> regras = regraRepository.findGlobaisEPrivadasPorLoja(loja.getId()).stream()
                .sorted(Comparator.comparing(
                        regra -> regra.getDescricao() != null ? regra.getDescricao() : "",
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(regra -> criarResumoRegra(regra, regrasEspecificas.get(regra.getId())))
                .toList();

        List<TurnoResumo> turnos = turnosBase.stream()
                .map(this::criarResumoTurno)
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
                horariosEspeciais,
                turnos
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

        boolean horasAlteraram = !request.horaAbertura().equals(loja.getHoraAbertura())
                || !request.horaFecho().equals(loja.getHoraFecho());

        if (horasAlteraram) {
            // Consistência operacional: a nova janela da loja tem de conter todos os
            // turnos ativos. Bloqueia se algum turno começa antes da abertura ou
            // termina depois do fecho — o gestor tem de o ajustar/desativar primeiro.
            List<Turno> turnosAtivos = turnoRepository.findAllAtivosOrderByHoraInicioAsc();
            List<Turno> foraDaJanela = turnosAtivos.stream()
                    .filter(t -> t.getHoraInicio() != null && t.getHoraFim() != null)
                    .filter(t -> t.getHoraInicio().isBefore(request.horaAbertura())
                            || t.getHoraFim().isAfter(request.horaFecho()))
                    .toList();
            if (!foraDaJanela.isEmpty()) {
                String listaTurnos = foraDaJanela.stream()
                        .map(t -> nomeDisplayTurno(t) + " (" + formatarHora(t.getHoraInicio())
                                + "-" + formatarHora(t.getHoraFim()) + ")")
                        .reduce((a, b) -> a + ", " + b).orElse("");
                throw new IllegalArgumentException(
                        "Não é possível aplicar este horário de funcionamento. Existem turnos ativos que "
                        + "ficam fora da nova janela da loja. Deves alterar ou desativar os seguintes turnos "
                        + "primeiro: " + listaTurnos + ".");
            }

            // Cobertura obrigatória de abertura e fecho: tem de existir pelo menos um turno
            // ativo com horaInicio == nova abertura e um com horaFim == novo fecho.
            // Sem esta guarda, alargar o horário criaria uma janela sem cobertura operacional.
            boolean coberturaAbertura = turnosAtivos.stream()
                    .anyMatch(t -> request.horaAbertura().equals(t.getHoraInicio()));
            boolean coberturaFecho = turnosAtivos.stream()
                    .anyMatch(t -> request.horaFecho().equals(t.getHoraFim()));
            if (!coberturaAbertura || !coberturaFecho) {
                String detalhe;
                if (!coberturaAbertura && !coberturaFecho) {
                    detalhe = "a abertura (" + formatarHora(request.horaAbertura())
                            + ") e o fecho (" + formatarHora(request.horaFecho()) + ")";
                } else if (!coberturaAbertura) {
                    detalhe = "a abertura (" + formatarHora(request.horaAbertura()) + ")";
                } else {
                    detalhe = "o fecho (" + formatarHora(request.horaFecho()) + ")";
                }
                throw new IllegalArgumentException(
                        "Inconsistência operacional: o novo horário da loja exige a existência de turnos "
                        + "ativos que cubram exatamente " + detalhe + ". "
                        + "Cria ou ajusta um turno para cobrir esse período antes de alterar o horário da loja.");
            }

            loja.setHoraAbertura(request.horaAbertura());
            loja.setHoraFecho(request.horaFecho());
            lojaRepository.save(loja);
        }

        if (request.regras() != null) {
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
                if (!Boolean.FALSE.equals(regraLoja.getAtivo())) {
                    regraLoja.setAtivo(true);
                }
                regrasLojaRepository.save(regraLoja);
            }
        }
    }

    @Transactional
    public void recriarTurnosPadrao(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        recriarTurnosELimparHorarios(ligacaoAtiva.getIdLoja());
    }

    private void recriarTurnosELimparHorarios(Loja loja) {
        LocalTime abertura = loja.getHoraAbertura();
        LocalTime fecho = loja.getHoraFecho();
        Integer idLoja = loja.getId();

        List<Integer> horarioIds = entityManager.createQuery(
                "SELECT h.id FROM Horario h WHERE h.idLojautilizador.idLoja.id = :idLoja", Integer.class)
                .setParameter("idLoja", idLoja).getResultList();

        if (!horarioIds.isEmpty()) {
            entityManager.createQuery(
                    "DELETE FROM HistoricoHorarioEstado hhe WHERE hhe.idHorario.id IN :ids")
                    .setParameter("ids", horarioIds).executeUpdate();

            entityManager.createQuery(
                    "DELETE FROM PermutaFolga pf WHERE pf.idHorarioD.id IN :ids OR pf.idHorarioY.id IN :ids")
                    .setParameter("ids", horarioIds).executeUpdate();

            entityManager.createQuery(
                    "DELETE FROM Permuta p WHERE p.idHorarioOrigem.id IN :ids OR p.idHorarioDestino.id IN :ids")
                    .setParameter("ids", horarioIds).executeUpdate();

            entityManager.createQuery(
                    "DELETE FROM Horario h WHERE h.id IN :ids")
                    .setParameter("ids", horarioIds).executeUpdate();
        }

        entityManager.createQuery("DELETE FROM Turno t").executeUpdate();
        entityManager.clear();

        criarTresTurnos(abertura, fecho);
    }

    private void criarTresTurnos(LocalTime abertura, LocalTime fecho) {
        long abMin = abertura.toSecondOfDay() / 60;
        long feMin = fecho.toSecondOfDay() / 60;
        if (feMin <= abMin) feMin += 24 * 60;
        long bloco = (feMin - abMin) / 3;

        String[] nomes = {"Manhã", "Tarde", "Noite"};
        String[] tipos = {"manha", "tarde", "noite"};

        for (int i = 0; i < 3; i++) {
            long iniMin = (abMin + (long) i * bloco) % (24 * 60);
            long fimMin = (abMin + (long) (i + 1) * bloco) % (24 * 60);

            Turno turno = new Turno();
            turno.setNome(nomes[i]);
            turno.setTipo(tipos[i]);
            turno.setHoraInicio(LocalTime.ofSecondOfDay(iniMin * 60));
            turno.setHoraFim(LocalTime.ofSecondOfDay(fimMin * 60));
            turnoRepository.save(turno);
        }
    }

    /**
     * Desativa a regra própria da loja: mantém o override mas marca ativo=false.
     * O motor de geração ignora overrides desativados e usa o valor base do sistema.
     */
    @Transactional
    public void desativarRegraDaLoja(Integer idUtilizador, Integer idRegra) {
        if (idRegra == null) {
            throw new IllegalArgumentException("Seleciona uma regra antes de a desativar.");
        }
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Loja loja = ligacaoAtiva.getIdLoja();
        Regra regra = regraRepository.findById(idRegra)
                .orElseThrow(() -> new IllegalArgumentException("Regra não encontrada."));
        RegrasLoja regraLoja = regrasLojaRepository
                .findByIdLojaIdAndIdRegraId(loja.getId(), idRegra)
                .orElseGet(RegrasLoja::new);
        if (regraLoja.getIdLoja() == null) {
            regraLoja.setIdLoja(loja);
            regraLoja.setIdRegra(regra);
        }
        regraLoja.setAtivo(false);
        regrasLojaRepository.save(regraLoja);
    }

    /**
     * Reativa a regra própria da loja: define ativo=true.
     * Se não havia override guardado (valorEspecifico=null), apaga a entrada
     * para que a regra volte ao estado "disponível" sem override.
     */
    @Transactional
    public void ativarRegraDaLoja(Integer idUtilizador, Integer idRegra) {
        if (idRegra == null) {
            throw new IllegalArgumentException("Seleciona uma regra antes de a ativar.");
        }
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        regrasLojaRepository
                .findByIdLojaIdAndIdRegraId(ligacaoAtiva.getIdLoja().getId(), idRegra)
                .ifPresent(regraLoja -> {
                    if (regraLoja.getValorEspecifico() == null && regraLoja.getObservacoes() == null) {
                        regrasLojaRepository.delete(regraLoja);
                    } else {
                        regraLoja.setAtivo(true);
                        regrasLojaRepository.save(regraLoja);
                    }
                });
    }

    /**
     * Cria uma regra livre (nota) associada exclusivamente a esta loja.
     * O motor de geração não a processa — serve de referência/lembrete para o gestor.
     */
    @Transactional
    public void criarRegraLivre(Integer idUtilizador, String descricao, String observacoes) {
        String desc = limparTexto(descricao);
        if (desc == null) {
            throw new IllegalArgumentException("Indica uma descrição para a regra.");
        }
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Loja loja = ligacaoAtiva.getIdLoja();

        Regra regra = new Regra();
        regra.setDescricao(desc);
        regra.setTipo("nota");
        regra.setIdLojaPrivada(loja);
        regraRepository.save(regra);

        RegrasLoja regraLoja = new RegrasLoja();
        regraLoja.setIdLoja(loja);
        regraLoja.setIdRegra(regra);
        regraLoja.setObservacoes(limparTexto(observacoes));
        regraLoja.setAtivo(true);
        regrasLojaRepository.save(regraLoja);
    }

    @Transactional
    public void removerRegraLivre(Integer idUtilizador, Integer idRegra) {
        if (idRegra == null) {
            throw new IllegalArgumentException("Seleciona uma regra antes de a remover.");
        }
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Regra regra = regraRepository.findById(idRegra)
                .orElseThrow(() -> new IllegalArgumentException("Regra não encontrada."));
        if (regra.getIdLojaPrivada() == null
                || !regra.getIdLojaPrivada().getId().equals(ligacaoAtiva.getIdLoja().getId())) {
            throw new IllegalArgumentException("Não tens permissão para remover esta regra.");
        }
        regrasLojaRepository.findByIdLojaIdAndIdRegraId(ligacaoAtiva.getIdLoja().getId(), idRegra)
                .ifPresent(regrasLojaRepository::delete);
        regraRepository.delete(regra);
    }

    /** Cria um novo turno. Valida sobreposição de horas com turnos existentes. */
    @Transactional
    public TurnoResumo criarTurno(Integer idUtilizador, String nome, LocalTime horaInicio, LocalTime horaFim) {
        obterLigacaoAtivaComPermissao(idUtilizador);
        String nomeLimpo = limparTexto(nome);
        if (nomeLimpo == null) {
            throw new IllegalArgumentException("Indica um nome para o turno.");
        }
        if (horaInicio == null || horaFim == null) {
            throw new IllegalArgumentException("Indica a hora de início e de fim do turno.");
        }
        if (!horaFim.isAfter(horaInicio)) {
            throw new IllegalArgumentException("A hora de fim deve ser posterior à hora de início.");
        }
        validarNomeTurnoUnico(nomeLimpo, null);
        validarIntervaloTurnoUnico(horaInicio, horaFim, null);
        Turno turno = new Turno();
        turno.setNome(nomeLimpo);
        turno.setHoraInicio(horaInicio);
        turno.setHoraFim(horaFim);
        turno.setTipo(derivarTipo(horaInicio));
        turno.setAtivo(true);
        return criarResumoTurno(turnoRepository.save(turno));
    }

    /** Edita um turno. Nome sempre editável. Horas só editáveis se não tiver horários atribuídos. */
    @Transactional
    public TurnoResumo editarTurno(Integer idUtilizador, Integer idTurno,
                                   String nome, LocalTime horaInicio, LocalTime horaFim) {
        if (idTurno == null) throw new IllegalArgumentException("Seleciona um turno antes de o editar.");
        obterLigacaoAtivaComPermissao(idUtilizador);
        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado."));
        String nomeLimpo = limparTexto(nome);
        if (nomeLimpo == null) throw new IllegalArgumentException("Indica um nome para o turno.");
        validarNomeTurnoUnico(nomeLimpo, idTurno);
        turno.setNome(nomeLimpo);

        boolean horasAlteraram = horaInicio != null && horaFim != null
                && (!horaInicio.equals(turno.getHoraInicio()) || !horaFim.equals(turno.getHoraFim()));
        if (horasAlteraram) {
            if (turnoRepository.existeEmHorarios(idTurno)) {
                throw new IllegalArgumentException(
                        "Este turno tem horários atribuídos — as horas não podem ser alteradas. "
                        + "Para mudar as horas, desativa este turno e cria um novo.");
            }
            if (!horaFim.isAfter(horaInicio)) {
                throw new IllegalArgumentException("A hora de fim deve ser posterior à hora de início.");
            }
            validarIntervaloTurnoUnico(horaInicio, horaFim, idTurno);
            turno.setHoraInicio(horaInicio);
            turno.setHoraFim(horaFim);
            turno.setTipo(derivarTipo(horaInicio));
        }
        return criarResumoTurno(turnoRepository.save(turno));
    }

    /** Desativa um turno — fica invisível para futuras gerações mas os registos históricos são preservados. */
    @Transactional
    public void desativarTurno(Integer idUtilizador, Integer idTurno) {
        if (idTurno == null) throw new IllegalArgumentException("Seleciona um turno antes de o desativar.");
        obterLigacaoAtivaComPermissao(idUtilizador);
        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado."));
        turno.setAtivo(false);
        turnoRepository.save(turno);
    }

    /** Reativa um turno desativado. */
    @Transactional
    public void ativarTurno(Integer idUtilizador, Integer idTurno) {
        if (idTurno == null) throw new IllegalArgumentException("Seleciona um turno antes de o ativar.");
        obterLigacaoAtivaComPermissao(idUtilizador);
        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado."));
        turno.setAtivo(true);
        turnoRepository.save(turno);
    }

    /** Remove um turno. Só permitido se não tiver nenhum horário atribuído. */
    @Transactional
    public void removerTurno(Integer idUtilizador, Integer idTurno) {
        if (idTurno == null) {
            throw new IllegalArgumentException("Seleciona um turno antes de o remover.");
        }
        obterLigacaoAtivaComPermissao(idUtilizador);
        if (!turnoRepository.existsById(idTurno)) {
            throw new IllegalArgumentException("Turno não encontrado.");
        }
        if (turnoRepository.existeEmHorarios(idTurno)) {
            throw new IllegalArgumentException(
                    "Não é possível eliminar um turno com horários atribuídos. Desativa-o em vez de eliminar.");
        }
        turnoRepository.deleteById(idTurno);
    }

    /** Política estrita: nenhum turno (ativo ou inativo) pode partilhar o mesmo nome — protege o histórico. */
    private void validarNomeTurnoUnico(String nomeLimpo, Integer idExcluir) {
        if (!turnoRepository.findTodosPorNome(nomeLimpo, idExcluir).isEmpty()) {
            throw new IllegalArgumentException(
                    "Erro: Já existe um turno registado com o nome \"" + nomeLimpo
                    + "\" (Ativo ou Inativo). Escolhe um nome exclusivo para proteger o histórico.");
        }
    }

    /** Política estrita: nenhum turno (ativo ou inativo) pode partilhar o intervalo de tempo EXATO. */
    private void validarIntervaloTurnoUnico(LocalTime horaInicio, LocalTime horaFim, Integer idExcluir) {
        if (!turnoRepository.findByIntervaloExato(horaInicio, horaFim, idExcluir).isEmpty()) {
            throw new IllegalArgumentException(
                    "Erro: Já existe um turno configurado exatamente para o intervalo "
                    + formatarHora(horaInicio) + " - " + formatarHora(horaFim)
                    + ". Altera o intervalo proposto.");
        }
    }

    /**
     * Dia de corte para o lançamento do horário mensal da loja (regra
     * "Dia limite de lancamento do horario mensal"): resolve o override
     * específico da loja, depois o valor padrão da regra, e por fim o
     * fallback 15. Usado pela UI para calcular em que mês uma alteração de
     * horário de funcionamento entra em vigor.
     */
    @Transactional(readOnly = true)
    public int obterDiaLimiteLancamento(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Loja loja = ligacaoAtiva.getIdLoja();
        final int fallback = 15;

        Regra regra = regraRepository.findAll().stream()
                .filter(r -> r.getDescricao() != null
                        && r.getDescricao().toLowerCase(Locale.ROOT).contains("dia limite de lancamento"))
                .findFirst()
                .orElse(null);
        if (regra == null) {
            return fallback;
        }

        Integer valor = regrasLojaRepository.findByIdLojaIdAndIdRegraId(loja.getId(), regra.getId())
                .map(RegrasLoja::getValorEspecifico)
                .orElse(null);
        if (valor == null) {
            valor = regra.getValorPadrao();
        }
        return (valor != null && valor >= 1 && valor <= 28) ? valor : fallback;
    }

    private String derivarTipo(LocalTime horaInicio) {
        if (horaInicio == null) return "outro";
        int h = horaInicio.getHour();
        if (h < 12) return "manha";
        if (h < 17) return "tarde";
        return "noite";
    }

    private String nomeDisplayTurno(Turno t) {
        if (t == null) return "-";
        return t.getNome() != null ? t.getNome() : (t.getTipo() != null ? t.getTipo() : "-");
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

            List<Turno> turnosCompativeis = filtrarTurnosCompativeis(turnoRepository.findAllAtivosOrderByHoraInicioAsc(), aberturaEfetiva, fechoEfetivo);
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

    /**
     * Resolve a ligação de gestão na loja activa trancada na sessão Desktop (escolhida
     * no ecrã de seleção pós-login). Todos os métodos de configuração da loja são
     * exclusivos do Desktop — a Web só usa {@code utilizadorPodeGerirLoja}, que não passa
     * por aqui. Se não houver loja válida na sessão (loja única, ou testes), cai na
     * primeira loja de gestão. A guarda {@code > 0} protege contra o Mockito devolver
     * {@code 0} para {@code Integer} não esboçado (ver Revisao.md, ponto 17).
     */
    private Lojautilizador obterLigacaoAtivaComPermissao(Integer idUtilizador) {
        Integer idLoja = sessaoBLL.obterLojaAtiva();
        Integer idLojaSegura = (idLoja != null && idLoja > 0) ? idLoja : null;
        return lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizador, idLojaSegura, LojautilizadorHelper.GESTAO,
                "Nao tens permissao para gerir a configuracao da loja.");
    }

    private RegraLojaResumo criarResumoRegra(Regra regra, RegrasLoja regraLoja) {
        boolean ativo = regraLoja == null || Boolean.TRUE.equals(regraLoja.getAtivo());
        boolean ehPrivada = regra.getIdLojaPrivada() != null;
        return new RegraLojaResumo(
                regra.getId(),
                valorOuTraco(regra.getDescricao()),
                regra.getTipo(),
                regra.getValorPadrao(),
                regraLoja != null ? regraLoja.getValorEspecifico() : null,
                regraLoja != null ? regraLoja.getObservacoes() : null,
                ativo,
                ehPrivada
        );
    }

    private TurnoResumo criarResumoTurno(Turno turno) {
        String nome = turno.getNome() != null ? turno.getNome() : capitalizar(turno.getTipo());
        return new TurnoResumo(
                turno.getId(),
                nome,
                turno.getTipo(),
                formatarHora(turno.getHoraInicio()),
                formatarHora(turno.getHoraFim()),
                Boolean.TRUE.equals(turno.getAtivo())
        );
    }

    private String capitalizar(String s) {
        if (s == null || s.isBlank()) return "-";
        String norm = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return Character.toUpperCase(norm.charAt(0)) + norm.substring(1).toLowerCase(Locale.ROOT);
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
            List<HorarioEspecialResumo> horariosEspeciais,
            List<TurnoResumo> turnos
    ) {
    }

    public record TurnoResumo(
            Integer idTurno,
            String nome,
            String tipo,
            String horaInicio,
            String horaFim,
            boolean ativo
    ) {
    }

    public record RegraLojaResumo(
            Integer idRegra,
            String descricao,
            String tipo,
            Integer valorPadrao,
            Integer valorEspecifico,
            String observacoes,
            boolean ativo,
            boolean privada
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
