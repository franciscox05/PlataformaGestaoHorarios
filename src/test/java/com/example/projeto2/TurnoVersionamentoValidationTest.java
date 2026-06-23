package com.example.projeto2;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.GestaoLojaService.TurnoResumo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre os turnos por-loja + versionamento copy-on-write (junho 2026):
 *   1. criar turno → fica scoped à loja activa;
 *   2. editar horas SEM histórico → edita em-lugar (mesmo id);
 *   3. editar horas COM horário publicado → arquiva a versão antiga e cria uma nova
 *      vigente a partir da próxima geração permitida; a geração de cada mês passa a ver
 *      a versão correcta (simetria com a alteração do horário de funcionamento da loja).
 */
@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class TurnoVersionamentoValidationTest extends FluxosCriticosTestSupport {

    @Test
    void criarTurnoFicaScopedALojaActiva() {
        GeracaoFixture fixture = criarContextoGeracao("turno-scope");
        Integer idGerente = fixture.lojaFixture().gerente().getId();
        Integer idLoja = fixture.lojaFixture().loja().getId();

        TurnoResumo criado = gestaoLojaBLL.criarTurno(idGerente, "Turno Manhã Cedo",
                LocalTime.of(9, 0), LocalTime.of(13, 0));
        flushAndClear();

        Turno persistido = turnoRepository.findById(criado.idTurno()).orElseThrow();
        assertNotNull(persistido.getIdLoja(), "O turno criado deve ficar scoped à loja.");
        assertEquals(idLoja, persistido.getIdLoja().getId());

        // Visível na vista loja-scoped da própria loja.
        assertTrue(turnoRepository.findParaLojaOrderByHoraInicioAsc(idLoja).stream()
                .anyMatch(t -> t.getId().equals(criado.idTurno())));
    }

    @Test
    void editarHorasSemHistoricoAlteraEmLugar() {
        GeracaoFixture fixture = criarContextoGeracao("turno-inplace");
        Integer idGerente = fixture.lojaFixture().gerente().getId();

        TurnoResumo criado = gestaoLojaBLL.criarTurno(idGerente, "Turno Editável",
                LocalTime.of(9, 0), LocalTime.of(13, 0));
        flushAndClear();

        TurnoResumo editado = gestaoLojaBLL.editarTurno(idGerente, criado.idTurno(),
                "Turno Editável", LocalTime.of(9, 0), LocalTime.of(14, 0));
        flushAndClear();

        // Mesmo id (edição em-lugar), sem criar versão nova.
        assertEquals(criado.idTurno(), editado.idTurno());
        Turno persistido = turnoRepository.findById(criado.idTurno()).orElseThrow();
        assertEquals(LocalTime.of(14, 0), persistido.getHoraFim());
        assertNull(persistido.getDataInicioVigencia());
        assertNull(persistido.getDataFimVigencia());
    }

    @Test
    void editarHorasComHistoricoCriaVersaoNovaEArquivaAntiga() {
        GeracaoFixture fixture = criarContextoGeracao("turno-cow");
        Integer idGerente = fixture.lojaFixture().gerente().getId();
        Integer idLoja = fixture.lojaFixture().loja().getId();

        // Turno scoped à loja, com horário publicado a referenciá-lo.
        TurnoResumo criado = gestaoLojaBLL.criarTurno(idGerente, "Turno Com Histórico",
                LocalTime.of(9, 0), LocalTime.of(13, 0));
        flushAndClear();
        Turno turnoOriginal = turnoRepository.findById(criado.idTurno()).orElseThrow();
        criarHorarioPublicadoSemProposta(
                fixture.lojaFixture().colaboradores().get(0), LocalDate.now(), turnoOriginal);
        flushAndClear();

        // Editar as horas: já não bloqueia — aplica copy-on-write.
        TurnoResumo novaVersao = gestaoLojaBLL.editarTurno(idGerente, criado.idTurno(),
                "Turno Com Histórico", LocalTime.of(9, 0), LocalTime.of(14, 0));
        flushAndClear();

        // Versão nova distinta, com vigência futura anunciada.
        assertNotEquals(criado.idTurno(), novaVersao.idTurno());
        assertNotNull(novaVersao.vigencia());
        assertTrue(novaVersao.vigencia().startsWith("A partir de"));

        Turno antiga = turnoRepository.findById(criado.idTurno()).orElseThrow();
        Turno nova = turnoRepository.findById(novaVersao.idTurno()).orElseThrow();

        // A antiga fica arquivada (vigência fechada) e mantém as horas originais.
        assertNotNull(antiga.getDataFimVigencia(), "A versão antiga deve ficar arquivada.");
        assertEquals(LocalTime.of(13, 0), antiga.getHoraFim());
        // A nova arranca no primeiro dia da próxima geração permitida.
        LocalDate inicioNova = nova.getDataInicioVigencia();
        assertNotNull(inicioNova);
        assertEquals(inicioNova.minusDays(1), antiga.getDataFimVigencia());
        assertEquals(LocalTime.of(14, 0), nova.getHoraFim());
        assertEquals(idLoja, nova.getIdLoja().getId());

        // A geração de cada mês vê a versão correcta:
        //   - mês corrente → versão antiga (13:00), não a nova;
        //   - mês de arranque da nova → versão nova (14:00), não a antiga.
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMesCorrente = hoje.withDayOfMonth(1);
        LocalDate fimMesCorrente = inicioMesCorrente.withDayOfMonth(inicioMesCorrente.lengthOfMonth());
        List<Turno> vigentesAgora = turnoRepository.findAtivosVigentesParaLojaNoPeriodo(
                idLoja, inicioMesCorrente, fimMesCorrente);
        assertTrue(vigentesAgora.stream().anyMatch(t -> t.getId().equals(antiga.getId())));
        assertFalse(vigentesAgora.stream().anyMatch(t -> t.getId().equals(nova.getId())));

        LocalDate fimMesNova = inicioNova.withDayOfMonth(inicioNova.lengthOfMonth());
        List<Turno> vigentesFuturo = turnoRepository.findAtivosVigentesParaLojaNoPeriodo(
                idLoja, inicioNova, fimMesNova);
        assertTrue(vigentesFuturo.stream().anyMatch(t -> t.getId().equals(nova.getId())));
        assertFalse(vigentesFuturo.stream().anyMatch(t -> t.getId().equals(antiga.getId())));
    }
}
