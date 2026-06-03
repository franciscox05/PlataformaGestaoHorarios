package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Loja;
import com.example.projeto2.Modules.Regra;
import com.example.projeto2.Modules.RegrasLoja;
import com.example.projeto2.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class RegrasGestaoPublicacaoValidationTest extends FluxosCriticosTestSupport {

    @Test
    void geracaoMensalGarantePresencaDeChefiaNosSabadosComLojaAberta() {
        GeracaoFixture fixture = criarContextoGeracao("gestao-sabados");
        LocalDate primeiroSabadoAberto = fixture.referencia().with(TemporalAdjusters.firstInMonth(DayOfWeek.SATURDAY));
        List<LocalDate> sabadosFechados = new ArrayList<>();
        for (LocalDate sabado = primeiroSabadoAberto.plusWeeks(1);
             sabado.getMonth() == fixture.referencia().getMonth();
             sabado = sabado.plusWeeks(1)) {
            sabadosFechados.add(sabado);
            criarHorarioEspecial(
                    fixture.lojaFixture().gerente().getId(),
                    "Sabado encerrado para teste de chefia " + sabado,
                    sabado,
                    sabado,
                    true,
                    null,
                    null,
                    null,
                    "Este sabado nao deve exigir chefia."
            );
        }

        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                fixture.lojaFixture().gerente().getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        );

        assertNotNull(proposta);

        Map<LocalDate, Boolean> sabadosComChefia = new HashMap<>();
        List<Horario> horariosGerados = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta());
        for (Horario horario : horariosGerados) {
            if (horario.getDataTurno() == null || horario.getDataTurno().getDayOfWeek() != DayOfWeek.SATURDAY) {
                continue;
            }

            boolean chefia = horario.getIdLojautilizador() != null
                    && horario.getIdLojautilizador().getIdCargo() != null
                    && horario.getIdLojautilizador().getIdCargo().getTipo() != null
                    && List.of("gerente", "subgerente").contains(horario.getIdLojautilizador().getIdCargo().getTipo().toLowerCase());
            sabadosComChefia.merge(horario.getDataTurno(), chefia, Boolean::logicalOr);
        }

        assertTrue(sabadosComChefia.getOrDefault(primeiroSabadoAberto, false));
        assertEquals(1, sabadosComChefia.size());
        assertTrue(sabadosFechados.stream().noneMatch(sabadosComChefia::containsKey));
    }

    @Test
    void geracaoFalhaComMensagemClaraQuandoNaoHaChefiaDisponivelAoSabado() {
        GeracaoFixture fixture = criarContextoGeracao("gestao-falha");
        LocalDate primeiroSabado = fixture.referencia().with(TemporalAdjusters.firstInMonth(DayOfWeek.SATURDAY));

        criarDayOffAprovado(fixture.lojaFixture().gerente(), primeiroSabado, "Gerente ausente no sabado.");
        lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(fixture.lojaFixture().loja().getId()).stream()
                .filter(ligacao -> ligacao.getDataFim() == null)
                .filter(ligacao -> ligacao.getIdCargo() != null)
                .filter(ligacao -> ligacao.getIdCargo().getTipo() != null)
                .filter(ligacao -> "subgerente".equalsIgnoreCase(ligacao.getIdCargo().getTipo()))
                .findFirst()
                .ifPresent(ligacao -> criarDayOffAprovado(
                        ligacao.getIdUtilizador(),
                        primeiroSabado,
                        "Subgerente ausente no sabado."
                ));

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> geracaoHorariosBLL.gerarProposta(
                        fixture.lojaFixture().gerente().getId(),
                        fixture.referencia().getYear(),
                        fixture.referencia().getMonthValue()
                )
        );

        assertEquals(
                "Nao foi possivel garantir presenca de gerente/subgerente no sabado " + primeiroSabado.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".",
                erro.getMessage()
        );
    }

    @Test
    void geracaoFalhaQuandoDiaLimiteDeLancamentoJaFoiUltrapassado() {
        GeracaoFixture fixture = criarContextoGeracao("cutoff-publicacao");
        LocalDate referenciaProximoMes = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        aplicarOverrideRegra(fixture.lojaFixture().loja(), "dia limite de lancamento do horario mensal", 1);
        flushAndClear();

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> geracaoHorariosBLL.gerarProposta(
                        fixture.lojaFixture().gerente().getId(),
                        referenciaProximoMes.getYear(),
                        referenciaProximoMes.getMonthValue()
                )
        );

        assertTrue(erro.getMessage().contains("tinha de ser lancada ate"));
        assertTrue(erro.getMessage().contains(String.valueOf(referenciaProximoMes.getYear())));
    }

    private void aplicarOverrideRegra(Loja loja, String descricaoNormalizada, int valor) {
        Regra regra = regraRepository.findAllByOrderByDescricaoAsc().stream()
                .filter(item -> item.getDescricao() != null)
                .filter(item -> item.getDescricao().toLowerCase().contains(descricaoNormalizada))
                .findFirst()
                .orElseThrow();

        RegrasLoja regraLoja = regrasLojaRepository.findByIdLojaIdAndIdRegraId(loja.getId(), regra.getId())
                .orElseGet(RegrasLoja::new);
        regraLoja.setIdLoja(loja);
        regraLoja.setIdRegra(regra);
        regraLoja.setValorEspecifico(valor);
        regraLoja.setObservacoes("Override de teste para regras de gestao/publicacao.");
        regrasLojaRepository.save(regraLoja);
    }
}
