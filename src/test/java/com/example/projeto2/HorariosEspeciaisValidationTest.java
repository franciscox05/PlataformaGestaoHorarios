package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class HorariosEspeciaisValidationTest extends FluxosCriticosTestSupport {

    @Test
    void excecoesDaLojaAfetamApenasOsDiasSelecionadosNaGeracaoMensal() {
        GeracaoFixture fixture = criarContextoGeracao("especiais");
        Utilizador gerente = fixture.lojaFixture().gerente();
        LocalDate referencia = fixture.referencia();
        LocalDate diaEncerrado = referencia.plusDays(2);
        LocalDate diaEspecial = referencia.plusDays(3);
        LocalDate diaNormal = referencia.plusDays(4);

        criarHorarioEspecial(
                gerente.getId(),
                "Feriado local",
                diaEncerrado,
                diaEncerrado,
                true,
                null,
                null,
                null,
                "Loja encerrada durante o feriado."
        );
        criarHorarioEspecial(
                gerente.getId(),
                "Campanha de tarde",
                diaEspecial,
                diaEspecial,
                false,
                LocalTime.of(12, 0),
                LocalTime.of(21, 0),
                2,
                "Horario reduzido com reforco de equipa."
        );

        var resumoLoja = gestaoLojaBLL.obterResumo(gerente.getId());
        assertEquals(2, resumoLoja.horariosEspeciais().size());
        assertTrue(resumoLoja.horariosEspeciais().stream()
                .anyMatch(horarioEspecial -> horarioEspecial.periodo().contains(diaEncerrado.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

        GeracaoHorariosBLL.PropostaResultado proposta = geracaoHorariosBLL.gerarProposta(
                gerente.getId(),
                referencia.getYear(),
                referencia.getMonthValue()
        );

        assertNotNull(proposta);
        assertFalse(proposta.linhas().stream()
                .anyMatch(linha -> diaEncerrado.equals(linha.data())));

        List<GeracaoHorariosBLL.HorarioLinha> linhasDiaEspecial = proposta.linhas().stream()
                .filter(linha -> diaEspecial.equals(linha.data()))
                .toList();
        assertEquals(2, linhasDiaEspecial.size());
        assertTrue(linhasDiaEspecial.stream()
                .allMatch(linha -> "Intermedio".equalsIgnoreCase(linha.turno())));
        assertTrue(linhasDiaEspecial.stream()
                .allMatch(linha -> "12:00 - 21:00".equals(linha.periodo())));

        long turnosDiaNormal = proposta.linhas().stream()
                .filter(linha -> diaNormal.equals(linha.data()))
                .count();
        assertEquals(contarBlocosCobertura(fixture.turnos()), turnosDiaNormal);
    }
}
