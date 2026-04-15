package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.DayOffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DayOffBLL {

    private final DayOffRepository dayOffRepository;

    @Autowired
    public DayOffBLL(DayOffRepository dayOffRepository) {
        this.dayOffRepository = dayOffRepository;
    }

    // Method que vai ser chamado quando o utilizador clicar em "Submeter" na janelinha
    public boolean registarPedidoFolga(Utilizador utilizador, LocalDate dataAusencia, String motivo) {

        // 1. Validação de Regra de Negócio: Não pode pedir folga no passado
        if (dataAusencia == null || dataAusencia.isBefore(LocalDate.now())) {
            System.out.println("❌ Erro: A data da folga não pode ser no passado.");
            return false;
        }

        // 2. Criar o objeto para gravar na Base de Dados
        DayOff novoPedido = new DayOff();
        novoPedido.setIdUtilizador(utilizador);
        novoPedido.setDataAusencia(dataAusencia);
        novoPedido.setMotivo(motivo);
        novoPedido.setTipo("folga"); // Enum da BD
        novoPedido.setEstado("pendente"); // O Gerente terá de aprovar depois

        // 3. Guardar no PostgreSQL via Hibernate
        try {
            dayOffRepository.save(novoPedido);
            System.out.println("✅ Sucesso: Pedido de folga registado para " + dataAusencia);
            return true;
        } catch (Exception e) {
            System.out.println("❌ Erro ao guardar na base de dados: " + e.getMessage());
            return false;
        }
    }
}