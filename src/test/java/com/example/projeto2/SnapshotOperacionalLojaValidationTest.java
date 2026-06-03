package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.BLL.SnapshotOperacionalLojaBLL;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class SnapshotOperacionalLojaValidationTest extends FluxosCriticosTestSupport {

    @Test
    void snapshotDeUmDiaNormalAgrupaHorariosDaLojaPorColaborador() {
        GeracaoFixture fixture = criarContextoGeracao("snapshot-normal");
        Utilizador gerente = fixture.lojaFixture().gerente();
        Utilizador supervisor = fixture.lojaFixture().supervisor();
        LocalDate referencia = fixture.referencia();
        LocalDate diaAnalise = referencia.plusDays(2);

        gerarEAprovarHorarios(gerente, supervisor, referencia);

        SnapshotOperacionalLojaBLL.SnapshotOperacionalLoja snapshot = snapshotOperacionalLojaBLL.carregarSnapshot(
                gerente.getId(),
                diaAnalise
        );

        assertNotNull(snapshot);
        assertEquals(fixture.lojaFixture().loja().getId(), snapshot.contexto().idLoja());
        assertEquals(diaAnalise, snapshot.intervalo().dataInicio());
        assertEquals(diaAnalise, snapshot.intervalo().dataFim());
        assertTrue(snapshot.intervalo().unicoDia());
        assertFalse(snapshot.equipaEscalada().isEmpty());
        assertTrue(snapshot.resumo().turnosPlaneados() > 0);
        assertTrue(snapshot.equipaEscalada().stream()
                .allMatch(colaborador -> !colaborador.turnos().isEmpty()));
        assertTrue(snapshot.equipaEscalada().stream()
                .flatMap(colaborador -> colaborador.turnos().stream())
                .allMatch(turno -> diaAnalise.equals(turno.data())));

        long idsUnicos = snapshot.equipaEscalada().stream()
                .map(SnapshotOperacionalLojaBLL.ColaboradorEscala::idUtilizador)
                .distinct()
                .count();
        assertEquals(snapshot.equipaEscalada().size(), idsUnicos);
    }

    @Test
    void snapshotDeDiaComAusenciasIncluiFolgasAprovadasNoPeriodo() {
        GeracaoFixture fixture = criarContextoGeracao("snapshot-ausencias");
        Utilizador gerente = fixture.lojaFixture().gerente();
        Utilizador supervisor = fixture.lojaFixture().supervisor();
        Utilizador colaboradorAusente = fixture.lojaFixture().colaboradores().get(0);
        LocalDate referencia = fixture.referencia();
        LocalDate diaAnalise = referencia.plusDays(1);

        criarDayOffAprovado(colaboradorAusente, diaAnalise, "Consulta medica.");
        gerarEAprovarHorarios(gerente, supervisor, referencia);

        SnapshotOperacionalLojaBLL.SnapshotOperacionalLoja snapshot = snapshotOperacionalLojaBLL.carregarSnapshot(
                gerente.getId(),
                diaAnalise
        );

        assertEquals(1, snapshot.ausencias().size());
        assertEquals(colaboradorAusente.getNome(), snapshot.ausencias().get(0).colaborador());
        assertEquals("Consulta medica.", snapshot.ausencias().get(0).motivo());
        assertFalse(snapshot.equipaEscalada().stream()
                .anyMatch(colaborador -> colaboradorAusente.getId().equals(colaborador.idUtilizador())));
    }

    @Test
    void snapshotDoPeriodoListaPedidosPendentesEContextoDaPermuta() {
        GeracaoFixture fixture = criarContextoGeracao("snapshot-pendentes");
        Utilizador gerente = fixture.lojaFixture().gerente();
        Utilizador supervisor = fixture.lojaFixture().supervisor();
        LocalDate referencia = fixture.referencia();
        LocalDate diaAnalise = referencia.plusDays(4);

        GeracaoHorariosBLL.PropostaResultado proposta = gerarEAprovarHorarios(gerente, supervisor, referencia);
        List<Horario> horariosDia = horarioRepository.findHorariosDaLojaNoDia(fixture.lojaFixture().loja().getId(), diaAnalise);
        assertTrue(horariosDia.size() >= 2);

        Horario horarioOrigem = horariosDia.get(0);
        Horario horarioDestino = horariosDia.stream()
                .filter(horario -> !horario.getId().equals(horarioOrigem.getId()))
                .filter(horario -> !horario.getIdLojautilizador().getIdUtilizador().getId()
                        .equals(horarioOrigem.getIdLojautilizador().getIdUtilizador().getId()))
                .findFirst()
                .orElseThrow();

        Utilizador colaboradorFolga = fixture.lojaFixture().colaboradores().get(2);
        DayOff pedidoFolga = new DayOff();
        pedidoFolga.setIdUtilizador(colaboradorFolga);
        pedidoFolga.setDataAusencia(diaAnalise);
        pedidoFolga.setTipo("folgas");
        pedidoFolga.setMotivo("Compromisso familiar");
        DayOff folgaPendente = dayOffBLL.registarPedidoFolga(pedidoFolga);

        Utilizador colaboradorPreferencia = fixture.lojaFixture().colaboradores().get(3);
        Preferencia preferencia = new Preferencia();
        preferencia.setTipo("turnos");
        preferencia.setDataInicio(diaAnalise);
        preferencia.setDataFim(diaAnalise);
        preferencia.setPrioridade(4);
        preferencia.setDescricao("Prefere turno intermedio neste dia.");
        Preferencia preferenciaPendente = preferenciaBLL.guardarPreferencia(colaboradorPreferencia.getId(), preferencia);

        Permuta permutaPendente = permutaBLL.registarPedidoTroca(
                horarioOrigem.getIdLojautilizador().getIdUtilizador().getId(),
                horarioOrigem,
                horarioDestino
        );

        flushAndClear();

        SnapshotOperacionalLojaBLL.SnapshotOperacionalLoja snapshot = snapshotOperacionalLojaBLL.carregarSnapshot(
                gerente.getId(),
                diaAnalise,
                diaAnalise
        );

        assertEquals(3, snapshot.resumo().totalPedidosPendentes());
        assertEquals(1, snapshot.resumo().permutasPendentes());
        assertEquals(1, snapshot.resumo().folgasPendentes());
        assertEquals(1, snapshot.resumo().preferenciasPendentes());
        assertEquals(
                Set.of(
                        SnapshotOperacionalLojaBLL.TipoPedidoOperacional.FOLGA,
                        SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PERMUTA,
                        SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PREFERENCIA
                ),
                snapshot.pedidosPendentes().stream()
                        .map(SnapshotOperacionalLojaBLL.PedidoPendenteOperacional::tipo)
                        .collect(Collectors.toSet())
        );

        SnapshotOperacionalLojaBLL.ContextoPedidoOperacional contextoPermuta = snapshotOperacionalLojaBLL.carregarContextoPedido(
                gerente.getId(),
                SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PERMUTA,
                permutaPendente.getId()
        );

        assertEquals(SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PERMUTA, contextoPermuta.pedido().tipo());
        assertEquals(diaAnalise, contextoPermuta.snapshotRelacionada().intervalo().dataInicio());
        assertEquals(2, contextoPermuta.colaboradoresEnvolvidos().size());
        assertTrue(contextoPermuta.colaboradoresEnvolvidos().stream()
                .allMatch(colaborador -> !colaborador.turnosNoPeriodo().isEmpty()));
        assertTrue(contextoPermuta.colaboradoresEnvolvidos().stream()
                .map(SnapshotOperacionalLojaBLL.ColaboradorContexto::idUtilizador)
                .collect(Collectors.toSet())
                .containsAll(List.of(
                        horarioOrigem.getIdLojautilizador().getIdUtilizador().getId(),
                        horarioDestino.getIdLojautilizador().getIdUtilizador().getId()
                )));

        assertNotNull(proposta.idProposta());
        assertNotNull(folgaPendente.getIdDayoff());
        assertNotNull(preferenciaPendente.getId());
    }

    private GeracaoHorariosBLL.PropostaResultado gerarEAprovarHorarios(Utilizador gerente,
                                                                       Utilizador supervisor,
                                                                       LocalDate referencia) {
        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                gerente.getId(),
                referencia.getYear(),
                referencia.getMonthValue()
        );

        geracaoHorariosBLL.enviarPropostasParaValidacao(gerente.getId(), List.of(proposta.idProposta()));
        return geracaoHorariosBLL.aprovarProposta(
                supervisor.getId(),
                proposta.idProposta(),
                "Aprovado para validar o snapshot operacional."
        );
    }
}
