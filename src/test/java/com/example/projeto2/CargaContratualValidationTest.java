package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.Modules.Cargo;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class CargaContratualValidationTest extends FluxosCriticosTestSupport {

    private static final Map<String, Long> LIMITES_MINUTOS_POR_CARGO = Map.of(
            "gerente", 176L * 60L,
            "subgerente", 176L * 60L,
            "supervisor", 176L * 60L,
            "fulltime", 176L * 60L,
            "parttime", 96L * 60L,
            "reforco_parttime", 64L * 60L
    );

    @Test
    void geracaoMensalRespeitaCargaContratualConfiguradaPorPerfil() {
        GeracaoFixture fixture = criarContextoGeracao("carga-contratual");

        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertNotNull(proposta);
        assertEquals(fixture.referencia().lengthOfMonth() * contarBlocosCobertura(fixture.turnos()), proposta.resumo().turnos());
        assertFalse(proposta.resumoColaboradores().isEmpty());

        Map<Integer, String> tiposCargoPorUtilizador = mapearTiposCargoDaLoja(fixture.lojaFixture().loja().getId());
        Set<String> tiposComCargaAtribuida = proposta.resumoColaboradores().stream()
                .map(resumo -> tiposCargoPorUtilizador.get(resumo.idColaborador()))
                .collect(Collectors.toSet());

        assertTrue(tiposComCargaAtribuida.contains("fulltime"));
        assertTrue(tiposComCargaAtribuida.contains("parttime"));
        assertTrue(tiposComCargaAtribuida.contains("reforco_parttime"));

        for (GeracaoHorariosBLL.ResumoColaborador resumo : proposta.resumoColaboradores()) {
            String tipoCargo = tiposCargoPorUtilizador.get(resumo.idColaborador());
            Long limiteMinutos = LIMITES_MINUTOS_POR_CARGO.get(tipoCargo);

            assertNotNull(tipoCargo, "Todos os colaboradores resumidos devem ter um cargo ativo associado.");
            assertNotNull(limiteMinutos, "Faltou mapear um limite contratual para o cargo " + tipoCargo + ".");
            assertTrue(
                    resumo.minutos() <= limiteMinutos,
                    "O colaborador " + resumo.nome() + " ultrapassou o limite contratual do cargo " + tipoCargo
                            + ": " + resumo.minutos() + " > " + limiteMinutos
            );
        }
    }

    @Test
    void reforcoDeFimDeSemanaSoRecebeTurnosAoSabadoOuDomingo() {
        GeracaoFixture fixture = criarContextoGeracao("carga-reforco");

        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        List<Horario> horariosReforco = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta()).stream()
                .filter(horario -> horario.getIdLojautilizador() != null)
                .filter(horario -> horario.getIdLojautilizador().getIdCargo() != null)
                .filter(horario -> "reforco_parttime".equalsIgnoreCase(horario.getIdLojautilizador().getIdCargo().getTipo()))
                .toList();

        assertFalse(horariosReforco.isEmpty());
        assertTrue(horariosReforco.stream()
                .map(Horario::getDataTurno)
                .allMatch(this::ehFimDeSemana));
    }

    @Test
    void geracaoMensalExcluiInativosESemVinculoValidoNoPeriodo() {
        GeracaoFixture fixture = criarContextoGeracao("carga-elegibilidade");
        LocalDate inicioPeriodo = fixture.referencia().withDayOfMonth(1);
        LocalDate fimPeriodo = fixture.referencia().withDayOfMonth(fixture.referencia().lengthOfMonth());

        Cargo cargoFullTime = obterOuCriarCargo("fulltime", "Assistente de Vendas FT");

        Utilizador inativo = criarUtilizadorComPasswordEmTexto(
                "Colaborador Inativo " + inicioPeriodo,
                "inativo." + inicioPeriodo,
                "Colaborador123",
                "inativo"
        );
        criarLigacao(inativo, fixture.lojaFixture().loja(), cargoFullTime, inicioPeriodo.minusDays(20), null);

        Utilizador semVinculoNoPeriodo = criarUtilizadorHashado(
                "Colaborador Futuro " + inicioPeriodo,
                "futuro." + inicioPeriodo,
                "Colaborador123"
        );
        criarLigacao(semVinculoNoPeriodo, fixture.lojaFixture().loja(), cargoFullTime, fimPeriodo.plusDays(2), null);

        flushAndClear();

        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertFalse(contemColaborador(proposta, inativo.getNome()));
        assertFalse(contemColaborador(proposta, semVinculoNoPeriodo.getNome()));
        assertEquals(fixture.referencia().lengthOfMonth() * contarBlocosCobertura(fixture.turnos()), proposta.resumo().turnos());
    }

    private Map<Integer, String> mapearTiposCargoDaLoja(Integer idLoja) {
        return lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja).stream()
                .filter(ligacao -> ligacao.getIdUtilizador() != null)
                .filter(ligacao -> ligacao.getIdUtilizador().getId() != null)
                .filter(ligacao -> ligacao.getIdCargo() != null)
                .filter(ligacao -> ligacao.getIdCargo().getTipo() != null)
                .filter(ligacao -> ligacao.getDataFim() == null)
                .collect(Collectors.toMap(
                        ligacao -> ligacao.getIdUtilizador().getId(),
                        ligacao -> ligacao.getIdCargo().getTipo().toLowerCase(),
                        (atual, substituto) -> atual
                ));
    }

    private boolean contemColaborador(GeracaoHorariosBLL.PropostaResultado proposta, String nome) {
        Predicate<GeracaoHorariosBLL.HorarioLinha> noHorario = linha -> nome.equals(linha.colaborador());
        Predicate<GeracaoHorariosBLL.ResumoColaborador> noResumo = resumo -> nome.equals(resumo.nome());

        return proposta.linhas().stream().anyMatch(noHorario)
                || proposta.resumoColaboradores().stream().anyMatch(noResumo);
    }

    private boolean ehFimDeSemana(LocalDate data) {
        return data != null
                && (data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY);
    }
}
