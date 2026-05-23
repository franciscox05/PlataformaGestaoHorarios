package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Turno;
import com.example.projeto2.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class PreferenciasPermanentesValidationTest extends FluxosCriticosTestSupport {

    @Test
    void preferenciasSemDataFimAssumemDataInicialImplicitaParaTodosOsTipos() {
        LojaFixture fixture = criarLojaComEquipaCompleta("preferencias-permanentes");
        Utilizador colaborador = fixture.colaboradores().get(0);
        LocalDate hoje = LocalDate.now();

        List<String> tipos = List.of("folgas", "ferias", "colegas", "turnos");
        for (String tipo : tipos) {
            Preferencia preferencia = new Preferencia();
            preferencia.setTipo(tipo);
            preferencia.setDataInicio(null);
            preferencia.setDataFim(null);
            preferencia.setPrioridade(null);
            preferencia.setDescricao("Preferencia permanente de " + tipo + " para validacao.");

            Preferencia guardada = preferenciaBLL.guardarPreferencia(colaborador.getId(), preferencia);

            assertNotNull(guardada.getId(), "A preferencia " + tipo + " devia ter sido guardada.");
            assertEquals(hoje, guardada.getDataInicio(), "A preferencia " + tipo + " devia assumir hoje como inicio.");
            assertNull(guardada.getDataFim(), "A preferencia " + tipo + " devia manter-se sem data fim.");
            assertEquals("pendente", guardada.getEstado().toLowerCase(Locale.ROOT));
            assertNotNull(guardada.getPrioridade(), "A preferencia " + tipo + " devia ter prioridade preenchida.");
        }
    }

    @Test
    void geracaoMensalDistinguePreferenciasTemporariasEPermanentesSemDataFim() {
        GeracaoFixture fixture = criarContextoGeracao("motor-preferencias");
        Utilizador gerente = fixture.lojaFixture().gerente();
        Utilizador colaboradorPermanente = fixture.lojaFixture().colaboradores().get(0);
        Utilizador colaboradorTemporario = fixture.lojaFixture().colaboradores().get(1);
        LocalDate referencia = fixture.referencia();

        criarPreferenciaAprovada(
                colaboradorPermanente,
                gerente,
                "folgas",
                referencia,
                null,
                5,
                "Preferencia permanente sem data fim."
        );
        criarPreferenciaAprovada(
                colaboradorTemporario,
                gerente,
                "folgas",
                referencia.plusDays(1),
                referencia.plusDays(2),
                5,
                "Preferencia temporaria para o inicio do periodo."
        );

        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                gerente.getId(),
                referencia.getYear(),
                referencia.getMonthValue()
        );

        assertNotNull(proposta);
        assertFalse(proposta.linhas().isEmpty());

        assertFalse(proposta.linhas().stream()
                .anyMatch(linha -> colaboradorPermanente.getNome().equals(linha.colaborador())));

        assertFalse(proposta.linhas().stream()
                .anyMatch(linha -> referencia.plusDays(1).equals(linha.data())
                        && colaboradorTemporario.getNome().equals(linha.colaborador())));
        assertFalse(proposta.linhas().stream()
                .anyMatch(linha -> referencia.plusDays(2).equals(linha.data())
                        && colaboradorTemporario.getNome().equals(linha.colaborador())));

        assertTrue(proposta.linhas().stream()
                .anyMatch(linha -> linha.data().isAfter(referencia.plusDays(2))
                        && colaboradorTemporario.getNome().equals(linha.colaborador())));
    }

    @Test
    void preferenciaEstruturadaDeTurnosReconheceBlocoEDuracaoNoMotor() {
        Preferencia preferencia = new Preferencia();
        preferencia.setTipo("turnos");
        preferencia.setDataInicio(LocalDate.now());
        preferencia.setDataFim(null);
        preferencia.setDescricao("Turnos preferidos: intermedio/tarde. Duracao preferida: curto. Nota adicional: estudo de manha.");

        Turno turnoCurtoTarde = criarTurno("intermedio", 14, 0, 18, 30);
        Turno turnoLongoTarde = criarTurno("intermedio", 12, 0, 21, 0);
        Turno turnoManha = criarTurno("manha", 10, 0, 19, 0);

        assertTrue(preferenciaTurnoFavoravel(preferencia, turnoCurtoTarde));
        assertFalse(preferenciaTurnoFavoravel(preferencia, turnoLongoTarde));
        assertFalse(preferenciaTurnoFavoravel(preferencia, turnoManha));
    }

    private boolean preferenciaTurnoFavoravel(Preferencia preferencia, Turno turno) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                geracaoHorariosBLL,
                "temPreferenciaTurnoFavoravel",
                List.of(preferencia),
                LocalDate.now(),
                turno
        ));
    }

    private Turno criarTurno(String tipo, int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        Turno turno = new Turno();
        turno.setTipo(tipo);
        turno.setHoraInicio(LocalTime.of(horaInicio, minutoInicio));
        turno.setHoraFim(LocalTime.of(horaFim, minutoFim));
        return turno;
    }
}
