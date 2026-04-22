package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class FluxosCriticosIntegrationTest extends FluxosCriticosTestSupport {

    @Test
    void autenticacaoMigraPasswordsLegadasERegistaFalhas() {
        Utilizador utilizador = criarUtilizadorComPasswordEmTexto(
                "Autenticacao Legada",
                "auth.legada",
                "Segredo123",
                "ativo"
        );

        long falhasAntes = contarEventos("login", "falha", utilizador.getEmail());

        Utilizador autenticado = utilizadorBLL.efetuarLogin("  " + utilizador.getEmail().toUpperCase() + "  ", " Segredo123 ");
        assertNotNull(autenticado);

        flushAndClear();

        Utilizador recarregado = utilizadorRepository.findById(utilizador.getId()).orElseThrow();
        assertTrue(recarregado.getPasswordHash().startsWith("$2"));
        assertNotEquals("Segredo123", recarregado.getPasswordHash());

        Utilizador falha = utilizadorBLL.efetuarLogin(utilizador.getEmail(), "errada");
        assertEquals(null, falha);
        assertEquals(falhasAntes + 1, contarEventos("login", "falha", utilizador.getEmail()));
    }

    @Test
    void perfilAtualizaPasswordComHashENaoAceitaPasswordAtualIncorreta() {
        Utilizador utilizador = criarUtilizadorHashado(
                "Perfil Seguro",
                "perfil.seguro",
                "Atual123"
        );

        long alteracoesAntes = contarEventos("alteracao_password", "sucesso", utilizador.getEmail());

        sessaoBLL.iniciarSessao(utilizador);
        Utilizador atualizado = perfilBLL.atualizarPassword(
                utilizador.getId(),
                "Atual123",
                "Nova123",
                "Nova123"
        );

        assertTrue(segurancaBLL.passwordCorresponde("Nova123", atualizado.getPasswordHash()));
        assertFalse(segurancaBLL.precisaMigrarParaHash(atualizado.getPasswordHash()));
        assertEquals(alteracoesAntes + 1, contarEventos("alteracao_password", "sucesso", utilizador.getEmail()));

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> perfilBLL.atualizarPassword(utilizador.getId(), "Atual123", "Outra123", "Outra123")
        );
        assertEquals("A password atual esta incorreta.", erro.getMessage());
    }

    @Test
    void preferenciaAprovadaPelaGerenciaFicaNoHistoricoENaoPodeSerEditada() {
        LojaFixture fixture = criarLojaComEquipaCompleta("preferencias");
        Utilizador gerente = fixture.gerente();
        Utilizador colaborador = fixture.colaboradores().get(0);
        LocalDate dataInicial = LocalDate.now().plusDays(10);

        Preferencia novaPreferencia = new Preferencia();
        novaPreferencia.setTipo("folgas");
        novaPreferencia.setDataInicio(dataInicial);
        novaPreferencia.setDataFim(dataInicial.plusDays(1));
        novaPreferencia.setPrioridade(5);
        novaPreferencia.setDescricao("Pedido de folga para compromisso familiar.");

        Preferencia guardada = preferenciaBLL.guardarPreferencia(colaborador.getId(), novaPreferencia);
        assertEquals("pendente", guardada.getEstado());
        assertTrue(preferenciaBLL.listarPreferenciasPendentesParaAprovacao(gerente.getId()).stream()
                .anyMatch(preferencia -> guardada.getId().equals(preferencia.getId())));

        Preferencia aprovada = preferenciaBLL.aprovarPreferencia(guardada.getId(), gerente.getId(), "Cobertura validada.");
        assertEquals("aprovado", aprovada.getEstado());
        assertNotNull(aprovada.getDataDecisao());
        assertTrue(preferenciaBLL.listarHistoricoDecisoesDaLoja(gerente.getId()).stream()
                .anyMatch(preferencia -> guardada.getId().equals(preferencia.getId())));

        Preferencia tentativaEdicao = new Preferencia();
        tentativaEdicao.setId(aprovada.getId());
        tentativaEdicao.setTipo(aprovada.getTipo());
        tentativaEdicao.setDataInicio(aprovada.getDataInicio());
        tentativaEdicao.setDataFim(aprovada.getDataFim());
        tentativaEdicao.setPrioridade(aprovada.getPrioridade());
        tentativaEdicao.setDescricao("Descricao alterada apos aprovacao.");

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> preferenciaBLL.guardarPreferencia(colaborador.getId(), tentativaEdicao)
        );
        assertEquals("So podes editar preferencias pendentes.", erro.getMessage());
    }

    @Test
    void geracaoMensalRespeitaBloqueiosEAceitaValidacaoDoSupervisor() {
        GeracaoFixture fixture = criarContextoGeracao("horarios");
        LojaFixture lojaFixture = fixture.lojaFixture();
        Utilizador gerente = lojaFixture.gerente();
        Utilizador supervisor = lojaFixture.supervisor();
        Utilizador bloqueadoPorDayOff = lojaFixture.colaboradores().get(0);
        Utilizador bloqueadoPorPreferencia = lojaFixture.colaboradores().get(1);
        LocalDate referencia = fixture.referencia();

        assertTrue(geracaoHorariosBLL.utilizadorPodeGerarHorarios(gerente.getId()));
        assertTrue(geracaoHorariosBLL.utilizadorPodeValidarHorarios(supervisor.getId()));

        criarDayOffAprovado(bloqueadoPorDayOff.getId(), referencia, "Ausencia bloqueada pelo teste.");
        criarPreferenciaAprovada(
                bloqueadoPorPreferencia,
                gerente,
                "folgas",
                referencia.plusDays(1),
                referencia.plusDays(1),
                5,
                "Folga aprovada para o segundo dia do periodo."
        );

        GeracaoHorariosBLL.PropostaResultado proposta;
        try {
            proposta = geracaoHorariosBLL.gerarProposta(
                    gerente.getId(),
                    referencia.getYear(),
                    referencia.getMonthValue()
            );
        } catch (IllegalArgumentException erro) {
            throw new AssertionError(
                    erro.getMessage()
                            + " | ativos=" + contarLigacoesAtivas(lojaFixture.loja().getId())
                            + " | turnos=" + descreverTurnosBase()
                            + " | regras=" + descreverRegrasGeracaoDaLoja(lojaFixture.loja().getId()),
                    erro
            );
        }

        assertNotNull(proposta);
        assertTrue("pendente".equalsIgnoreCase(proposta.estado()));
        assertEquals(referencia.lengthOfMonth() * fixture.turnos().size(), proposta.resumo().turnos());
        assertFalse(proposta.linhas().isEmpty());
        assertFalse(proposta.linhas().stream()
                .anyMatch(linha -> referencia.equals(linha.data()) && bloqueadoPorDayOff.getNome().equals(linha.colaborador())));
        assertFalse(proposta.linhas().stream()
                .anyMatch(linha -> referencia.plusDays(1).equals(linha.data()) && bloqueadoPorPreferencia.getNome().equals(linha.colaborador())));

        GeracaoHorariosBLL.PropostaResultado aprovada = geracaoHorariosBLL.aprovarProposta(
                supervisor.getId(),
                proposta.idProposta(),
                "Validado em teste automatizado."
        );
        assertTrue("aprovado".equalsIgnoreCase(aprovada.estado()));

        flushAndClear();

        var horarios = horarioRepository.findByIdPropostaHorarioId(proposta.idProposta());
        assertFalse(horarios.isEmpty());
        assertTrue(horarios.stream().allMatch(horario -> "aprovado".equalsIgnoreCase(horario.getEstado())));

        Set<Integer> idsHorarios = horarios.stream()
                .map(Horario::getId)
                .collect(Collectors.toSet());

        long historicosAprovados = listarHistoricoPorHorarios(idsHorarios).stream()
                .filter(registo -> "aprovado".equalsIgnoreCase(registo.getEstadoNovo()))
                .count();
        assertEquals(horarios.size(), historicosAprovados);
    }

    @Test
    void painelDeHorariosConsegueCarregarHorariosPublicadosSemPropostaAssociada() {
        GeracaoFixture fixture = criarContextoGeracao("publicados-sem-proposta");
        LojaFixture lojaFixture = fixture.lojaFixture();
        Utilizador gerente = lojaFixture.gerente();
        LocalDate referencia = fixture.referencia();

        criarHorarioPublicadoSemProposta(lojaFixture.colaboradores().get(0), referencia, fixture.turnos().get(0));
        criarHorarioPublicadoSemProposta(lojaFixture.colaboradores().get(1), referencia.plusDays(1), fixture.turnos().get(1));
        flushAndClear();

        GeracaoHorariosBLL.PropostaResultado resultado = geracaoHorariosBLL.obterPlaneamento(
                gerente.getId(),
                referencia.getYear(),
                referencia.getMonthValue()
        );

        assertNotNull(resultado);
        assertEquals(null, resultado.idProposta());
        assertEquals("Publicado", resultado.estado());
        assertEquals("Horarios publicados", resultado.origemPlaneamento());
        assertEquals(2, resultado.linhas().size());
        assertTrue(resultado.linhas().stream().allMatch(linha -> "Aprovado".equalsIgnoreCase(linha.estado())));
    }
}
