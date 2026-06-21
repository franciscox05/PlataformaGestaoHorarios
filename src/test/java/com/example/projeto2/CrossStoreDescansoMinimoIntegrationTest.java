package com.example.projeto2;

import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Loja;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.PropostaHorarioMensal;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Repositories.PropostaHorarioMensalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Caso Braga Parque vs. Guimarães" — descanso mínimo de 11h para um colaborador
 * multi-loja, calculado GLOBALMENTE (por utilizador), não isolado por loja.
 *
 * <p><b>Nota de fidelidade ao código atual (ver Revisao.md, secção 1):</b>
 * {@code PermutaService.validarPedido()} já bloqueia qualquer permuta de turno
 * entre lojas diferentes ({@code idLojaOrigem.equals(idLojaDestino)} é obrigatório —
 * ver {@code PermutaService.java:295-296}). Por isso o cenário de risco real de
 * "ACT entre lojas" não passa por aprovar uma permuta cross-store (essa via está
 * fechada por desenho), mas por um colaborador com vínculo ativo em DUAS lojas
 * receber, por atribuição direta do gestor (não por permuta), um turno na Loja B
 * que viola o descanso face a um turno já aprovado na Loja A. É esse o caminho que
 * este teste exercita, via {@code HorarioService.adicionarTurno}.
 */
@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class CrossStoreDescansoMinimoIntegrationTest extends FluxosCriticosTestSupport {

    @Autowired
    private PropostaHorarioMensalRepository propostaRepository;

    /**
     * <b>BUG CORRIGIDO (ver Revisao.md, secções 1 e 22):</b> {@code HorarioService.adicionarTurno}
     * passou a validar o descanso mínimo de 11h GLOBALMENTE por colaborador (não só a
     * sobreposição literal no mesmo dia), via {@code validarDescansoMinimoGlobal}. Este
     * teste — que sempre asseverou o comportamento <i>desejado</i> — passa agora a guardar
     * a regressão: atribuir manualmente em Guimarães um turno que respeita menos de 11h face
     * a um turno já publicado em Braga Parque no dia anterior é rejeitado, mesmo sendo lojas
     * diferentes (o descanso é um direito da pessoa, não da loja — Código do Trabalho, art. 214.º).
     */
    @Test
    void atribuicaoEmGuimaraesFalhaQuandoViolaDescansoDeTurnoEmBragaParque() {
        LocalDate dia = LocalDate.now().plusDays(45);
        String uid = novoUuidLocal();

        // ── Arrange: Utilizador X com vínculo ativo em Braga Parque E em Guimarães ──
        Loja bragaParque = criarLojaComNome("Levi's Braga Parque " + uid);
        Loja guimaraes = criarLojaComNome("Levi's Guimarães " + uid);

        Utilizador utilizadorX = criarUtilizadorHashado("Utilizador X " + uid, "utilizadorx." + uid, "Pass123");
        Utilizador gerenteGuimaraes = criarUtilizadorHashado("Gerente Guimaraes " + uid, "gerente.guimaraes." + uid, "Pass123");

        Cargo cargoFullTime = obterOuCriarCargo("fulltime", "Assistente FT");
        Cargo cargoGerente = obterOuCriarCargo("gerente", "Gerente de Loja");

        Lojautilizador ligacaoBraga = criarLigacaoAtiva(utilizadorX, bragaParque, cargoFullTime);
        criarLigacaoAtiva(utilizadorX, guimaraes, cargoFullTime);
        criarLigacaoAtiva(gerenteGuimaraes, guimaraes, cargoGerente);

        // Turno de noite em Braga Parque: 16:00–23:30 (fecho)
        Turno turnoNoiteBraga = salvarTurnoLocal("noite", LocalTime.of(16, 0), LocalTime.of(23, 30));
        Horario horarioBraga = new Horario();
        horarioBraga.setIdLojautilizador(ligacaoBraga);
        horarioBraga.setIdTurno(turnoNoiteBraga);
        horarioBraga.setDataTurno(dia);
        horarioBraga.setEstado(EstadoHorario.aprovado);
        horarioRepository.save(horarioBraga);
        flushAndClear();

        // Turno de manhã em Guimarães no DIA SEGUINTE: 08:00–17:00.
        // Gap real: 23:30 (dia) → 08:00 (dia+1) = 8h30 — viola o mínimo legal de 11h.
        Turno turnoManhaGuimaraes = salvarTurnoLocal("manha", LocalTime.of(8, 0), LocalTime.of(17, 0));

        Lojautilizador ligacaoGuimaraes = lojautilizadorRepository
                .findLigacaoAtivaByIdUtilizadorAndIdLoja(utilizadorX.getId(), guimaraes.getId())
                .orElseThrow();

        PropostaHorarioMensal proposta = new PropostaHorarioMensal();
        proposta.setIdLoja(guimaraes);
        proposta.setIdUtilizadorGeracao(gerenteGuimaraes);
        proposta.setAno(dia.plusDays(1).getYear());
        proposta.setMes(dia.plusDays(1).getMonthValue());
        proposta.setEstado("rascunho");
        proposta.setDataGeracao(LocalDateTime.now());
        proposta = propostaRepository.save(proposta);
        flushAndClear();

        final Integer idProposta = proposta.getId();
        final Integer idLigacaoGuimaraes = ligacaoGuimaraes.getId();
        final Integer idTurnoGuimaraes = turnoManhaGuimaraes.getId();
        final Integer idGerente = gerenteGuimaraes.getId();
        final LocalDate diaSeguinte = dia.plusDays(1);

        // ── Act + Assert: a atribuição em Guimarães tem de ser rejeitada ──
        // TODO (Revisao.md, secção 4): atualmente isto é IllegalArgumentException
        // genérica. Se introduzirem uma ViolacaoDescansoMinimoException dedicada,
        // troquem o assertThrows abaixo para o tipo específico.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> horarioBLL.adicionarTurno(idProposta, idLigacaoGuimaraes, diaSeguinte, idTurnoGuimaraes, idGerente),
                "Atribuir um turno em Guimaraes que respeita menos de 11h de descanso face "
                        + "ao turno do Utilizador X em Braga Parque deve ser rejeitado, mesmo "
                        + "sendo lojas diferentes — o descanso é um direito da PESSOA, nao da loja.");

        assertTrue(erro.getMessage().toLowerCase().contains("descanso")
                        || erro.getMessage().toLowerCase().contains("sobrepo"),
                "A mensagem de erro deve identificar o problema de descanso/sobreposicao: " + erro.getMessage());

        // ── Assert de rollback: nenhum horario novo deve ter sido persistido em Guimaraes ──
        flushAndClear();
        long horariosGuimaraes = horarioRepository.findAll().stream()
                .filter(h -> h.getIdLojautilizador() != null
                        && h.getIdLojautilizador().getIdLoja() != null
                        && guimaraes.getId().equals(h.getIdLojautilizador().getIdLoja().getId()))
                .count();
        assertEquals(0L, horariosGuimaraes,
                "A tentativa rejeitada nao pode deixar nenhum Horario residual em Guimaraes.");
    }

    // ── helpers locais (evitam colidir com criarLoja/salvarTurno de outras suites) ──

    private Loja criarLojaComNome(String nome) {
        Loja loja = new Loja();
        loja.setNome(nome);
        loja.setLocalizacao("Ambiente de testes");
        loja.setHoraAbertura(LocalTime.of(9, 0));
        loja.setHoraFecho(LocalTime.of(23, 59));
        return lojaRepository.save(loja);
    }

    private Turno salvarTurnoLocal(String tipo, LocalTime inicio, LocalTime fim) {
        Turno t = new Turno();
        t.setTipo(tipo);
        t.setHoraInicio(inicio);
        t.setHoraFim(fim);
        return turnoRepository.save(t);
    }

    private String novoUuidLocal() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
