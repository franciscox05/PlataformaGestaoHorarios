package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Turno;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.DayOffRepository;
import com.example.projeto2.Repositories.HorarioRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class PerfilBLL {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LojautilizadorRepository lojautilizadorRepository;
    private final HorarioRepository horarioRepository;
    private final DayOffRepository dayOffRepository;

    public PerfilBLL(LojautilizadorRepository lojautilizadorRepository,
                     HorarioRepository horarioRepository,
                     DayOffRepository dayOffRepository) {
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.horarioRepository = horarioRepository;
        this.dayOffRepository = dayOffRepository;
    }

    @Transactional(readOnly = true)
    public PerfilResumo obterResumoPerfil(Utilizador utilizador) {
        if (utilizador == null || utilizador.getId() == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        Lojautilizador ligacaoAtiva = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(utilizador.getId())
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para este utilizador."));

        List<Horario> turnos = horarioRepository.findTurnosPorUtilizador(utilizador.getId());
        List<DayOff> pedidosFolga = dayOffRepository.findByIdUtilizador(utilizador.getId());

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
                capitalizar(utilizador.getEstado()),
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
