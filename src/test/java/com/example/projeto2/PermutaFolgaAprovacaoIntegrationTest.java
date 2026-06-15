package com.example.projeto2;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.PermutaFolga;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.PermutaFolgaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Revalidação na aprovação da permuta de folga: entre o pedido e a aprovação o
 * horário pode mudar (outra permuta aprovada, edição manual). Sem a revalidação,
 * a aprovação trocava os donos às cegas e podia deixar um colaborador com dois
 * turnos no mesmo dia ou sem o descanso mínimo de 11h.
 */
@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class PermutaFolgaAprovacaoIntegrationTest extends FluxosCriticosTestSupport {

    @Autowired
    private PermutaFolgaService permutaFolgaBLL;

    @Test
    void aprovacaoValidaTrocaOsDonosDosTurnos() {
        Cenario cenario = montarCenario("pf-aprova");

        PermutaFolga aprovada = permutaFolgaBLL.aprovar(cenario.pedido().getId(), cenario.gerente().getId());
        assertEquals("aprovado", aprovada.getEstado());

        flushAndClear();
        Horario horarioD = horarioRepository.findById(cenario.idHorarioD()).orElseThrow();
        Horario horarioY = horarioRepository.findById(cenario.idHorarioY()).orElseThrow();
        assertEquals(cenario.func2().getId(), horarioD.getIdLojautilizador().getIdUtilizador().getId(),
                "Após a aprovação, o turno do dia D passa para o Func2.");
        assertEquals(cenario.func1().getId(), horarioY.getIdLojautilizador().getIdUtilizador().getId(),
                "Após a aprovação, o turno do dia Y passa para o Func1.");
    }

    @Test
    void aprovacaoFalhaSeColegaPerdeuAFolgaEntretanto() {
        Cenario cenario = montarCenario("pf-sem-folga");

        // Entretanto o Func2 ganhou um turno no dia D — deixou de ter folga.
        criarHorarioPublicadoSemProposta(cenario.func2(), cenario.diaD(), cenario.turnoManha());
        flushAndClear();

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> permutaFolgaBLL.aprovar(cenario.pedido().getId(), cenario.gerente().getId()));
        assertTrue(erro.getMessage().contains("ja nao tem folga"),
                "A aprovação deve explicar que o horário mudou: " + erro.getMessage());

        flushAndClear();
        Horario horarioD = horarioRepository.findById(cenario.idHorarioD()).orElseThrow();
        assertEquals(cenario.func1().getId(), horarioD.getIdLojautilizador().getIdUtilizador().getId(),
                "Com a falha, a troca não pode ter acontecido.");
    }

    @Test
    void aprovacaoFalhaSeDescansoMinimoDeixouDeSeCumprir() {
        Cenario cenario = montarCenario("pf-descanso");

        // Entretanto o Func2 ganhou um turno tardio na véspera do dia D: 23:30 → 10:00
        // são 10h30 de gap, abaixo das 11h legais.
        Turno turnoTardio = criarTurnoTeste("noite", LocalTime.of(15, 0), LocalTime.of(23, 30));
        criarHorarioPublicadoSemProposta(cenario.func2(), cenario.diaD().minusDays(1), turnoTardio);
        flushAndClear();

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> permutaFolgaBLL.aprovar(cenario.pedido().getId(), cenario.gerente().getId()));
        assertTrue(erro.getMessage().contains("descanso"),
                "A aprovação deve apontar a violação do descanso mínimo: " + erro.getMessage());
    }

    // ── fixture ──────────────────────────────────────────────────────────────

    private Cenario montarCenario(String prefixo) {
        LojaFixture fixture = criarLojaComEquipaCompleta(prefixo);
        Utilizador func1 = fixture.colaboradores().get(0);
        Utilizador func2 = fixture.colaboradores().get(1);

        Turno turnoManha = criarTurnoTeste("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        LocalDate diaD = LocalDate.now().plusDays(10);
        LocalDate diaY = LocalDate.now().plusDays(12);

        Horario horarioD = criarHorarioPublicadoSemProposta(func1, diaD, turnoManha);
        Horario horarioY = criarHorarioPublicadoSemProposta(func2, diaY, turnoManha);
        flushAndClear();
        horarioD = horarioRepository.findById(horarioD.getId()).orElseThrow();
        horarioY = horarioRepository.findById(horarioY.getId()).orElseThrow();

        PermutaFolga pedido = permutaFolgaBLL.registarPedido(func1.getId(), horarioD, horarioY);
        flushAndClear();

        return new Cenario(fixture.gerente(), func1, func2, diaD, diaY,
                horarioD.getId(), horarioY.getId(), turnoManha, pedido);
    }

    private Turno criarTurnoTeste(String tipo, LocalTime horaInicio, LocalTime horaFim) {
        Turno turno = new Turno();
        turno.setTipo(tipo);
        turno.setHoraInicio(horaInicio);
        turno.setHoraFim(horaFim);
        return turnoRepository.save(turno);
    }

    private record Cenario(
            Utilizador gerente,
            Utilizador func1,
            Utilizador func2,
            LocalDate diaD,
            LocalDate diaY,
            Integer idHorarioD,
            Integer idHorarioY,
            Turno turnoManha,
            PermutaFolga pedido
    ) {
    }
}
