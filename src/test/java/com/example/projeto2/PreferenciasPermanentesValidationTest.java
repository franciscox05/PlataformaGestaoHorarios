package com.example.projeto2;

import com.example.projeto2.API.Services.geracao.dto.*;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.AvaliadorAtribuicao;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
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

    @Autowired
    private HorarioValidatorService horarioValidatorService;

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

        PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
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
    void folgaPreferidaSoftNaoBloqueiaOColaboradorAoContrarioDaFolgaAprovada() {
        GeracaoFixture fixture = criarContextoGeracao("folga-preferida-soft");
        Utilizador gerente = fixture.lojaFixture().gerente();
        Utilizador comFolgaSoft = fixture.lojaFixture().colaboradores().get(0);
        LocalDate referencia = fixture.referencia();

        // Folga preferida recorrente (soft): o algoritmo deve dar-lhe muita atencao,
        // mas pode escalar o colaborador se for preciso para a cobertura.
        criarPreferenciaAprovada(
                comFolgaSoft,
                gerente,
                "folga_preferida",
                referencia,
                null,
                5,
                "Prefere folgar a este dia da semana."
        );

        PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                gerente.getId(),
                referencia.getYear(),
                referencia.getMonthValue()
        );

        assertNotNull(proposta);
        // Propriedade-chave do "soft": ao contrario de uma folga aprovada (hard block),
        // o colaborador NAO fica excluido do mes — continua a ser escalado.
        assertTrue(proposta.linhas().stream()
                        .anyMatch(linha -> comFolgaSoft.getNome().equals(linha.colaborador())),
                "Uma folga preferida (soft) nao deve bloquear o colaborador do mes inteiro.");
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
        Utilizador utilizador = new Utilizador();
        utilizador.setId(1);
        preferencia.setIdUtilizador(utilizador);
        AvaliadorAtribuicao avaliador = new AvaliadorAtribuicao(horarioValidatorService);
        return avaliador.temPreferenciaTurnoFavoravel(
                utilizador.getId(),
                turno,
                LocalDate.now(),
                java.util.Map.of(utilizador.getId(), List.of(preferencia)));
    }

    private Turno criarTurno(String tipo, int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        Turno turno = new Turno();
        turno.setTipo(tipo);
        turno.setHoraInicio(LocalTime.of(horaInicio, minutoInicio));
        turno.setHoraFim(LocalTime.of(horaFim, minutoFim));
        return turno;
    }
}
