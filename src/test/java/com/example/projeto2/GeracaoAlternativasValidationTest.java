package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.Modules.Horario;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class GeracaoAlternativasValidationTest extends FluxosCriticosTestSupport {

    @Test
    void gerentePodeGerarVariasAlternativasECompararAntesDaValidacao() {
        GeracaoFixture fixture = criarContextoGeracao("alternativas-comparacao");

        List<GeracaoHorariosBLL.PropostaResultado> alternativas = geracaoHorariosBLL.gerarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue(),
                3
        );

        assertEquals(3, alternativas.size());
        assertEquals(3, alternativas.stream().map(GeracaoHorariosBLL.PropostaResultado::idProposta).collect(Collectors.toSet()).size());
        assertTrue(alternativas.stream().allMatch(proposta -> "pendente".equalsIgnoreCase(proposta.estado())));
        assertTrue(alternativas.stream().allMatch(proposta -> proposta.metricas().pontuacao() >= 0));
        assertTrue(alternativas.stream().allMatch(proposta ->
                proposta.resumo().turnos() == fixture.referencia().lengthOfMonth() * fixture.turnos().size()));

        List<GeracaoHorariosBLL.PropostaResumo> resumos = geracaoHorariosBLL.listarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertTrue(resumos.size() >= 3);
        assertTrue(resumos.stream().map(GeracaoHorariosBLL.PropostaResumo::politicaOtimizacao).distinct().count() >= 2);

        GeracaoHorariosBLL.ComparacaoPropostas comparacao = geracaoHorariosBLL.compararPropostas(
                fixture.lojaFixture().gerente().getId(),
                alternativas.get(0).idProposta(),
                alternativas.get(1).idProposta()
        );

        assertNotNull(comparacao);
        assertFalse(comparacao.diferencas().isEmpty());
        assertEquals(alternativas.get(0).idProposta(), comparacao.idPropostaBase());
        assertEquals(alternativas.get(1).idProposta(), comparacao.idPropostaComparada());
    }

    @Test
    void aprovarUmaAlternativaPublicaEssaERejeitaPendentesConcorrentes() {
        GeracaoFixture fixture = criarContextoGeracao("alternativas-aprovacao");

        List<GeracaoHorariosBLL.PropostaResultado> alternativas = geracaoHorariosBLL.gerarPropostas(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue(),
                2
        );

        GeracaoHorariosBLL.PropostaResultado aprovada = geracaoHorariosBLL.aprovarProposta(
                fixture.lojaFixture().supervisor().getId(),
                alternativas.get(1).idProposta(),
                "Alternativa escolhida apos comparacao."
        );

        assertTrue("aprovado".equalsIgnoreCase(aprovada.estado()));

        List<GeracaoHorariosBLL.PropostaResumo> resumos = geracaoHorariosBLL.listarPropostas(
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
        assertTrue(horariosAprovados.stream().allMatch(horario -> "aprovado".equalsIgnoreCase(horario.getEstado())));
        assertTrue(horariosRejeitados.stream().allMatch(horario -> "rejeitado".equalsIgnoreCase(horario.getEstado())));

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
}
