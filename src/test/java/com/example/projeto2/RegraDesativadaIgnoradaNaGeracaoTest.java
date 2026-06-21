package com.example.projeto2;

import com.example.projeto2.API.Modules.Regra;
import com.example.projeto2.API.Modules.RegrasLoja;
import com.example.projeto2.API.Repositories.RegraRepository;
import com.example.projeto2.API.Repositories.RegrasLojaRepository;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.RegraAplicada;
import com.example.projeto2.API.Services.geracao.RegraGeracaoResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * "Cenário Saldos / Black Friday" — uma {@code RegrasLoja} desativada
 * ({@code ativo = false}) tem de ser completamente ignorada na resolução de
 * parâmetros de geração, caindo no valor padrão global, sem NullPointerException.
 *
 * <p><b>Nota de fidelidade ao código atual (ver Revisao.md, secção 2):</b> não existe,
 * hoje, uma regra de catálogo "limite de turnos noturnos consecutivos" — o catálogo
 * de {@code Regra} cobre mínimos de cobertura, descanso, carga contratual, rotação de
 * fins de semana, chefia ao sábado e dia limite de publicação (ver
 * {@code RegraGeracaoResolver.ehRegraDe*}). Este teste usa a regra real
 * "Mínimo de colaboradores por turno" como o override de loja a desativar — é,
 * aliás, o exemplo de negócio mais fiel ao Black Friday: a loja relaxa
 * temporariamente o mínimo de cobertura por turno sem apagar a configuração.
 *
 * <p>Teste unitário puro: {@code RegraGeracaoResolver} é instanciado diretamente
 * com repositórios mockados (Mockito), sem contexto Spring — não toca a base de dados.
 */
@ExtendWith(MockitoExtension.class)
class RegraDesativadaIgnoradaNaGeracaoTest {

    @Mock
    private RegrasLojaRepository regrasLojaRepository;

    @Mock
    private RegraRepository regraRepository;

    private RegraGeracaoResolver resolver;

    private static final Integer ID_LOJA = 1;
    private static final Integer ID_REGRA_MINIMO = 10;

    @BeforeEach
    void preparar() {
        // HorarioValidatorService não tem dependências Spring — instanciação direta.
        resolver = new RegraGeracaoResolver(regrasLojaRepository, regraRepository, new HorarioValidatorService());
    }

    @Test
    void regraLojaDesativadaNaoEntraNosOverridesENaoLancaExcecao() {
        Regra regraMinimoGlobal = regra(ID_REGRA_MINIMO, "Mínimo de colaboradores por turno", "operacional", 1);

        // Override de loja para o Black Friday: minimo elevado para 4 — mas DESATIVADO
        // (a loja já reverteu a regra, mas não apagou o registo — soft toggle).
        RegrasLoja overrideDesativado = regraLoja(ID_LOJA, regraMinimoGlobal, 4, false);

        when(regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(ID_LOJA))
                .thenReturn(List.of(overrideDesativado));
        when(regraRepository.findAllByOrderByDescricaoAsc())
                .thenReturn(List.of(regraMinimoGlobal));

        List<RegraAplicada> regrasAplicadas = assertDoesNotThrow(
                () -> resolver.obterRegrasAplicadas(ID_LOJA),
                "Resolver uma RegrasLoja desativada nao pode lancar NullPointerException nem nenhuma excecao.");

        assertEquals(1, regrasAplicadas.size());
        RegraAplicada aplicada = regrasAplicadas.get(0);

        // O ponto central do teste: o valor aplicado tem de ser o PADRÃO global (1),
        // nunca o valor do override desativado (4) — prova que a flag ativo=false
        // é respeitada e o override é tratado como inexistente.
        assertEquals(1, aplicada.valor(),
                "Com a RegrasLoja desativada, o motor deve usar o valor padrao global (1) "
                        + "da regra, ignorando completamente o valor especifico (4) do override "
                        + "desativado de Black Friday.");
    }

    @Test
    void regraLojaAtivaContinuaASerAplicadaNormalmente() {
        // Caso de controlo: o mesmo override, mas ativo=true, TEM de prevalecer —
        // garante que o teste anterior está realmente a testar a flag, e não outra coisa.
        Regra regraMinimoGlobal = regra(ID_REGRA_MINIMO, "Mínimo de colaboradores por turno", "operacional", 1);
        RegrasLoja overrideAtivo = regraLoja(ID_LOJA, regraMinimoGlobal, 4, true);

        when(regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(ID_LOJA))
                .thenReturn(List.of(overrideAtivo));
        when(regraRepository.findAllByOrderByDescricaoAsc())
                .thenReturn(List.of(regraMinimoGlobal));

        List<RegraAplicada> regrasAplicadas = resolver.obterRegrasAplicadas(ID_LOJA);

        assertTrue(regrasAplicadas.stream().anyMatch(r -> r.valor() != null && r.valor() == 4),
                "Com o override ATIVO, o valor especifico de loja (4) deve prevalecer "
                        + "sobre o padrao global (1).");
    }

    @Test
    void regraLojaComAtivoNuloEhTratadaComoAtiva() {
        // Dados legados/migrados podem ter ativo == null (coluna recém-criada via
        // sql/migracao-junho2026.sql antes de qualquer UPDATE explícito). O padrão
        // Boolean.FALSE.equals(...) usado no resolver tem de tratar null como ativo.
        Regra regraMinimoGlobal = regra(ID_REGRA_MINIMO, "Mínimo de colaboradores por turno", "operacional", 1);
        RegrasLoja overrideSemFlag = regraLoja(ID_LOJA, regraMinimoGlobal, 4, null);

        when(regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(ID_LOJA))
                .thenReturn(List.of(overrideSemFlag));
        when(regraRepository.findAllByOrderByDescricaoAsc())
                .thenReturn(List.of(regraMinimoGlobal));

        List<RegraAplicada> regrasAplicadas = assertDoesNotThrow(
                () -> resolver.obterRegrasAplicadas(ID_LOJA),
                "ativo == null nao pode lancar NullPointerException.");

        assertTrue(regrasAplicadas.stream().anyMatch(r -> r.valor() != null && r.valor() == 4),
                "ativo == null deve ser tratado como ativo (default seguro) — o override (4) "
                        + "deve prevalecer, tal como na entidade RegrasLoja (private Boolean ativo = true).");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Regra regra(Integer id, String descricao, String tipo, Integer valorPadrao) {
        Regra r = new Regra();
        r.setId(id);
        r.setDescricao(descricao);
        r.setTipo(tipo);
        r.setValorPadrao(valorPadrao);
        return r;
    }

    private RegrasLoja regraLoja(Integer idLoja, Regra regra, Integer valorEspecifico, Boolean ativo) {
        RegrasLoja rl = new RegrasLoja();
        rl.setId(1);
        rl.setIdRegra(regra);
        rl.setValorEspecifico(valorEspecifico);
        rl.setAtivo(ativo);
        return rl;
    }
}
