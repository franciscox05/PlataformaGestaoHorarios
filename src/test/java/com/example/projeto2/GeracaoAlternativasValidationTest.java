package com.example.projeto2;

import com.example.projeto2.API.Services.geracao.dto.*;
import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.RegrasLoja;
import com.example.projeto2.API.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class GeracaoAlternativasValidationTest extends FluxosCriticosTestSupport {

    @Test
    void gerentePodeGerarVariasAlternativasECompararAntesDaValidacao() {
        GeracaoFixture fixture = criarContextoGeracao("alternativas-comparacao");

        List<PropostaResultado> alternativas = geracaoHorariosBLL.gerarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue(),
                3
        );

        assertEquals(3, alternativas.size());
        assertEquals(3, alternativas.stream().map(PropostaResultado::idProposta).collect(Collectors.toSet()).size());
        assertTrue(alternativas.stream().allMatch(proposta -> "rascunho".equalsIgnoreCase(proposta.estado())));
        assertTrue(alternativas.stream().allMatch(proposta -> proposta.metricas().pontuacao() >= 0));
        long blocosCobertura = contarBlocosCobertura(fixture.turnos());
        assertTrue(alternativas.stream().allMatch(proposta ->
                proposta.resumo().turnos() == fixture.referencia().lengthOfMonth() * blocosCobertura));

        List<PropostaResumo> resumos = geracaoHorariosBLL.listarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertTrue(resumos.size() >= 3);
        assertTrue(resumos.stream().map(PropostaResumo::politicaOtimizacao).distinct().count() >= 2);

        List<PropostaResumo> resumosSupervisorAntesDoEnvio = geracaoHorariosBLL.listarPropostas(
                fixture.lojaFixture().supervisor().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );
        assertTrue(resumosSupervisorAntesDoEnvio.isEmpty());

        ComparacaoPropostas comparacao = geracaoHorariosBLL.compararPropostas(
                fixture.lojaFixture().gerente().getId(),
                alternativas.get(0).idProposta(),
                alternativas.get(1).idProposta()
        );

        assertNotNull(comparacao);
        assertFalse(comparacao.diferencas().isEmpty());
        assertEquals(alternativas.get(0).idProposta(), comparacao.idPropostaBase());
        assertEquals(alternativas.get(1).idProposta(), comparacao.idPropostaComparada());

        List<PropostaResultado> enviadas = geracaoHorariosBLL.enviarPropostasParaValidacao(
                fixture.lojaFixture().gerente().getId(),
                List.of(alternativas.get(0).idProposta(), alternativas.get(1).idProposta())
        );
        assertEquals(2, enviadas.size());
        assertTrue(enviadas.stream().allMatch(proposta -> "pendente".equalsIgnoreCase(proposta.estado())));

        List<PropostaResumo> resumosSupervisorDepoisDoEnvio = geracaoHorariosBLL.listarPropostas(
                fixture.lojaFixture().supervisor().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );
        assertEquals(2, resumosSupervisorDepoisDoEnvio.size());
        assertTrue(resumosSupervisorDepoisDoEnvio.stream().allMatch(proposta -> "pendente".equalsIgnoreCase(proposta.estado())));
    }

    @Test
    void aprovarUmaAlternativaPublicaEssaERejeitaPendentesConcorrentes() {
        GeracaoFixture fixture = criarContextoGeracao("alternativas-aprovacao");

        List<PropostaResultado> alternativas = geracaoHorariosBLL.gerarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue(),
                2
        );
        geracaoHorariosBLL.enviarPropostasParaValidacao(
                fixture.lojaFixture().gerente().getId(),
                alternativas.stream().map(PropostaResultado::idProposta).toList()
        );

        PropostaResultado aprovada = geracaoHorariosBLL.aprovarProposta(
                fixture.lojaFixture().supervisor().getId(),
                alternativas.get(1).idProposta(),
                "Alternativa escolhida apos comparacao."
        );

        assertTrue("aprovado".equalsIgnoreCase(aprovada.estado()));

        List<PropostaResumo> resumos = geracaoHorariosBLL.listarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );
        assertEquals(1, resumos.stream().filter(proposta -> "aprovado".equalsIgnoreCase(proposta.estado())).count());
        assertEquals(1, resumos.stream().filter(proposta -> "rejeitado".equalsIgnoreCase(proposta.estado())).count());

        List<Horario> horariosAprovados = horarioRepository.findByIdPropostaHorarioId(alternativas.get(1).idProposta());
        List<Horario> horariosRejeitados = horarioRepository.findByIdPropostaHorarioId(alternativas.get(0).idProposta());

        assertFalse(horariosAprovados.isEmpty());
        assertFalse(horariosRejeitados.isEmpty());
        assertTrue(horariosAprovados.stream().allMatch(horario -> EstadoHorario.aprovado == horario.getEstado()));
        assertTrue(horariosRejeitados.stream().allMatch(horario -> EstadoHorario.rejeitado == horario.getEstado()));

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> geracaoHorariosBLL.gerarProposta(
                        fixture.lojaFixture().gerente().getId(),
                        fixture.referencia().getYear(),
                        fixture.referencia().getMonthValue()
                )
        );
        assertTrue(erro.getMessage().contains("proposta aprovada") || erro.getMessage().contains("horarios publicados"));
    }

    @Test
    void verPlaneamentoRecuperaAAlternativaMaisRecenteGeradaParaOMes() {
        GeracaoFixture fixture = criarContextoGeracao("alternativas-planeamento");

        List<PropostaResultado> alternativas = geracaoHorariosBLL.gerarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue(),
                3
        );

        List<PropostaResumo> resumos = geracaoHorariosBLL.listarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        PropostaResultado planeamento = geracaoHorariosBLL.obterPlaneamento(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertEquals(3, alternativas.size());
        assertFalse(resumos.isEmpty());
        assertNotNull(planeamento);
        assertEquals(resumos.getFirst().idProposta(), planeamento.idProposta());
        assertFalse(planeamento.linhas().isEmpty());
        assertTrue(planeamento.resumo().turnos() > 0);
    }

    @Test
    void geracaoAgrupaVariantesDoMesmoTipoESuportaMinimoGenericoMaisElevado() {
        GeracaoFixture fixture = criarContextoGeracao("alternativas-variantes-turno");
        Cargo cargoFullTime = obterOuCriarCargo("fulltime", "Assistente de Vendas FT");
        Utilizador colaboradorExtra = criarUtilizadorHashado(
                "Colaborador Extra Variantes",
                "colaborador.extra.variantes",
                "Colaborador123"
        );
        criarLigacaoAtiva(colaboradorExtra, fixture.lojaFixture().loja(), cargoFullTime);

        RegrasLoja regraMinimos = regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(
                        fixture.lojaFixture().loja().getId())
                .stream()
                .filter(regraLoja -> regraLoja.getIdRegra() != null)
                .filter(regraLoja -> regraLoja.getIdRegra().getDescricao() != null)
                .filter(regraLoja -> regraLoja.getIdRegra().getDescricao().toLowerCase().contains("minimo"))
                .filter(regraLoja -> regraLoja.getIdRegra().getDescricao().toLowerCase().contains("colaborador"))
                .findFirst()
                .orElseThrow();
        regraMinimos.setValorEspecifico(2);
        regrasLojaRepository.save(regraMinimos);
        flushAndClear();

        List<PropostaResultado> propostas = geracaoHorariosBLL.gerarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue(),
                1
        );

        assertEquals(1, propostas.size());
        PropostaResultado proposta = propostas.getFirst();
        assertEquals(fixture.referencia().lengthOfMonth() * contarBlocosCobertura(fixture.turnos()) * 2, proposta.resumo().turnos());

        List<Horario> horarios = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta());
        assertFalse(horarios.isEmpty());
        assertTrue(horarios.stream().anyMatch(horario -> duracaoEmMinutos(horario) < 300));
    }

    private long duracaoEmMinutos(Horario horario) {
        LocalTime horaInicio = horario.getIdTurno().getHoraInicio();
        LocalTime horaFim = horario.getIdTurno().getHoraFim();
        if (horaInicio == null || horaFim == null) {
            return 0;
        }
        if (!horaFim.isBefore(horaInicio)) {
            return Duration.between(horaInicio, horaFim).toMinutes();
        }
        return Duration.between(horaInicio, LocalTime.MAX).plusMinutes(1).toMinutes()
                + Duration.between(LocalTime.MIN, horaFim).toMinutes();
    }
}
