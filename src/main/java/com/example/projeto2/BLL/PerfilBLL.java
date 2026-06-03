package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Turno;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.DayOffRepository;
import com.example.projeto2.Repositories.HorarioRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PerfilBLL {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final LojautilizadorRepository lojautilizadorRepository;
    private final HorarioRepository horarioRepository;
    private final DayOffRepository dayOffRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final SegurancaBLL segurancaBLL;
    private final AuditoriaBLL auditoriaBLL;
    private final SessaoBLL sessaoBLL;

    public PerfilBLL(LojautilizadorRepository lojautilizadorRepository,
                     HorarioRepository horarioRepository,
                     DayOffRepository dayOffRepository,
                     UtilizadorRepository utilizadorRepository,
                     SegurancaBLL segurancaBLL,
                     AuditoriaBLL auditoriaBLL,
                     SessaoBLL sessaoBLL) {
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.horarioRepository = horarioRepository;
        this.dayOffRepository = dayOffRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.segurancaBLL = segurancaBLL;
        this.auditoriaBLL = auditoriaBLL;
        this.sessaoBLL = sessaoBLL;
    }

    @Transactional(readOnly = true)
    public PerfilResumo obterResumoPerfil(Utilizador utilizador) {
        if (utilizador == null || utilizador.getId() == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        Lojautilizador ligacaoAtiva = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(utilizador.getId())
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para este utilizador."));

        List<Horario> turnos = horarioRepository.findTurnosPorUtilizador(utilizador.getId());
        List<DayOff> pedidosFolga = dayOffRepository.findByIdUtilizadorId(utilizador.getId());

        Horario proximoTurno = turnos.stream()
                .filter(horario -> horario.getDataTurno() != null && !horario.getDataTurno().isBefore(LocalDate.now()))
                .min(Comparator
                        .comparing(Horario::getDataTurno)
                        .thenComparing(horario -> horario.getIdTurno().getHoraInicio()))
                .orElse(null);

        long pedidosPendentes = pedidosFolga.stream()
                .filter(dayOff -> "pendente".equalsIgnoreCase(dayOff.getEstado()))
                .count();

        long pedidosAprovados = pedidosFolga.stream()
                .filter(dayOff -> "aprovado".equalsIgnoreCase(dayOff.getEstado()))
                .count();

        long totalMinutosMes = turnos.stream()
                .filter(horario -> horario.getDataTurno() != null)
                .filter(horario -> horario.getDataTurno().getYear() == LocalDate.now().getYear())
                .filter(horario -> horario.getDataTurno().getMonth() == LocalDate.now().getMonth())
                .map(Horario::getIdTurno)
                .mapToLong(this::calcularDuracaoEmMinutos)
                .sum();

        long turnosFuturos = turnos.stream()
                .filter(horario -> horario.getDataTurno() != null && !horario.getDataTurno().isBefore(LocalDate.now()))
                .count();

        return new PerfilResumo(
                utilizador.getNome(),
                valorOuTraco(utilizador.getEmail()),
                valorOuTraco(utilizador.getTelemovel()),
                capitalizar(utilizador.getEstado() != null ? utilizador.getEstado().name() : null),
                ligacaoAtiva.getIdLoja() != null ? valorOuTraco(ligacaoAtiva.getIdLoja().getNome()) : "-",
                ligacaoAtiva.getIdCargo() != null ? valorOuTraco(ligacaoAtiva.getIdCargo().getNome()) : "-",
                ligacaoAtiva.getDataInicio() != null ? ligacaoAtiva.getDataInicio().format(DATA_FORMATTER) : "-",
                formatarProximoTurno(proximoTurno),
                formatarDuracao(totalMinutosMes),
                pedidosPendentes,
                pedidosAprovados,
                turnosFuturos
        );
    }

    @Transactional(readOnly = true)
    public Utilizador obterUtilizadorPorId(Integer idUtilizador) {
        return obterUtilizadorPersistido(idUtilizador);
    }

    @Transactional
    public Utilizador atualizarTelemovel(Integer idUtilizador, String novoTelemovel) {
        Utilizador utilizador = obterUtilizadorPersistido(idUtilizador);
        String telemovelNormalizado = normalizarTexto(novoTelemovel);

        if (telemovelNormalizado == null || !telemovelNormalizado.matches("\\d{9}")) {
            throw new IllegalArgumentException("O telemovel deve ter exatamente 9 digitos.");
        }

        if (telemovelNormalizado.equals(utilizador.getTelemovel())) {
            throw new IllegalArgumentException("O novo telemovel nao pode ser igual ao atual.");
        }

        if (utilizadorRepository.existsByTelemovelAndIdNot(telemovelNormalizado, utilizador.getId())) {
            throw new IllegalArgumentException("Este telemovel ja esta registado noutra conta.");
        }

        utilizador.setTelemovel(telemovelNormalizado);
        return utilizadorRepository.save(utilizador);
    }

    @Transactional
    public Utilizador atualizarEmail(Integer idUtilizador, String novoEmail) {
        Utilizador utilizador = obterUtilizadorPersistido(idUtilizador);
        String emailNormalizado = normalizarTexto(novoEmail);

        if (emailNormalizado == null) {
            throw new IllegalArgumentException("O email e obrigatorio.");
        }

        if (!EMAIL_PATTERN.matcher(emailNormalizado).matches()) {
            throw new IllegalArgumentException("Indica um email valido.");
        }

        if (utilizador.getEmail() != null && utilizador.getEmail().equalsIgnoreCase(emailNormalizado)) {
            throw new IllegalArgumentException("O novo email nao pode ser igual ao atual.");
        }

        if (utilizadorRepository.existsByEmailIgnoreCaseAndIdNot(emailNormalizado, utilizador.getId())) {
            throw new IllegalArgumentException("Este email ja esta registado noutra conta.");
        }

        utilizador.setEmail(emailNormalizado);
        return utilizadorRepository.save(utilizador);
    }

    @Transactional
    public Utilizador atualizarNome(Integer idUtilizador, String novoNome) {
        Utilizador utilizador = obterUtilizadorPersistido(idUtilizador);
        String nomeNormalizado = normalizarTexto(novoNome);

        if (nomeNormalizado == null) {
            throw new IllegalArgumentException("O nome e obrigatorio.");
        }

        if (nomeNormalizado.length() < 3) {
            throw new IllegalArgumentException("O nome deve ter pelo menos 3 caracteres.");
        }

        if (nomeNormalizado.length() > 100) {
            throw new IllegalArgumentException("O nome nao pode ter mais de 100 caracteres.");
        }

        if (nomeNormalizado.equalsIgnoreCase(utilizador.getNome())) {
            throw new IllegalArgumentException("O novo nome nao pode ser igual ao atual.");
        }

        utilizador.setNome(nomeNormalizado);
        return utilizadorRepository.save(utilizador);
    }

    @Transactional
    public Utilizador atualizarPassword(Integer idUtilizador, String passwordAtual, String novaPassword, String confirmarPassword) {
        Utilizador utilizador = obterUtilizadorPersistido(idUtilizador);

        String passwordAtualNormalizada = normalizarTexto(passwordAtual);
        String novaPasswordNormalizada = normalizarTexto(novaPassword);
        String confirmarPasswordNormalizada = normalizarTexto(confirmarPassword);

        if (passwordAtualNormalizada == null || novaPasswordNormalizada == null || confirmarPasswordNormalizada == null) {
            throw new IllegalArgumentException("Por favor, preenche todos os campos.");
        }

        if (!segurancaBLL.passwordCorresponde(passwordAtualNormalizada, utilizador.getPasswordHash())) {
            throw new IllegalArgumentException("A password atual esta incorreta.");
        }

        segurancaBLL.validarPasswordNova(novaPasswordNormalizada);

        if (!novaPasswordNormalizada.equals(confirmarPasswordNormalizada)) {
            throw new IllegalArgumentException("As novas passwords nao coincidem.");
        }

        if (segurancaBLL.passwordCorresponde(novaPasswordNormalizada, utilizador.getPasswordHash())) {
            throw new IllegalArgumentException("A nova password tem de ser diferente da atual.");
        }

        utilizador.setPasswordHash(segurancaBLL.gerarHash(novaPasswordNormalizada));
        Utilizador utilizadorAtualizado = utilizadorRepository.save(utilizador);
        auditoriaBLL.registarAlteracaoPassword(utilizadorAtualizado, sessaoBLL.obterIdentificadorSessao());
        return utilizadorAtualizado;
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

    private String formatarProximoTurno(Horario proximoTurno) {
        if (proximoTurno == null || proximoTurno.getDataTurno() == null || proximoTurno.getIdTurno() == null) {
            return "Sem turnos agendados";
        }

        return proximoTurno.getDataTurno().format(DATA_FORMATTER)
                + " | "
                + proximoTurno.getIdTurno().getHoraInicio()
                + " - "
                + proximoTurno.getIdTurno().getHoraFim();
    }

    private String formatarDuracao(long minutosTotais) {
        long horas = minutosTotais / 60;
        long minutos = minutosTotais % 60;
        return horas + "h " + minutos + "m";
    }

    private String capitalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }

        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1).toLowerCase();
    }

    private String valorOuTraco(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
    }

    private Utilizador obterUtilizadorPersistido(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        return utilizadorRepository.findById(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel encontrar o utilizador autenticado."));
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.isEmpty() ? null : valorNormalizado;
    }

    public record PerfilResumo(
            String nome,
            String email,
            String telemovel,
            String estado,
            String lojaAtual,
            String cargoAtual,
            String dataEntrada,
            String proximoTurno,
            String horasEsteMes,
            long pedidosPendentes,
            long pedidosAprovados,
            long turnosFuturos
    ) {
    }
}
