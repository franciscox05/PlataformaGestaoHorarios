package com.example.projeto2;

import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.UtilizadorBLL;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Utilizador;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Scanner; // Import necessário para ler o teclado

@SpringBootApplication
public class Projeto2Application {

    public static void main(String[] args) {
        SpringApplication.run(Projeto2Application.class, args);
    }

    @Bean
    @Transactional
    public CommandLineRunner menuInterativo(UtilizadorBLL userBll, HorarioBLL horarioBll) {
        return args -> {
            Scanner scanner = new Scanner(System.in);

            System.out.println("\n=================================");
            System.out.println("   SISTEMA DE GESTÃO LEVI'S");
            System.out.println("=================================");

            // 1. Pede os dados ao utilizador
            System.out.print("Email: ");
            String email = scanner.nextLine();

            System.out.print("Password: ");
            String pass = scanner.nextLine();

            // 2. Tenta fazer o login com o que foi escrito
            Utilizador logado = userBll.efetuarLogin(email, pass);

            if (logado != null) {
                System.out.println("\n✅ Login: OK! Bem-vindo(a), " + logado.getNome() + " (ID: " + logado.getId() + ")");

                boolean continuar = true;

                // 3. O Menu Principal entra num loop até o utilizador escolher Sair (Opção 2)
                while (continuar) {
                    System.out.println("\n--- MENU PRINCIPAL ---");
                    System.out.println("1. Ver os meus próximos turnos");
                    System.out.println("2. Sair");
                    System.out.print("Escolha uma opção: ");

                    String opcao = scanner.nextLine();

                    switch (opcao) {
                        case "1":
                            List<Horario> turnos = horarioBll.listarProximosTurnos(logado.getId());

                            if (turnos.isEmpty()) {
                                System.out.println("\nNão tens turnos agendados para breve.");
                            } else {
                                // Lógica para singular ou plural
                                if (turnos.size() == 1) {
                                    System.out.println("\nEncontrei 1 turno futuro:");
                                } else {
                                    System.out.println("\nEncontrei " + turnos.size() + " turnos futuros:");
                                }
                                for (Horario h : turnos) {
                                    String infoTurno = "Sem Turno";
                                    if (h.getIdTurno() != null) {
                                        infoTurno = h.getIdTurno().getTipo().toString() +
                                                " (" + h.getIdTurno().getHoraInicio() + " - " + h.getIdTurno().getHoraFim() + ")";
                                    }

                                    String nomeLoja = "Loja Desconhecida";
                                    if (h.getIdLojautilizador() != null && h.getIdLojautilizador().getIdLoja() != null) {
                                        nomeLoja = h.getIdLojautilizador().getIdLoja().getNome();
                                    }

                                    System.out.println("-> [" + h.getDataTurno() + "] Loja: " + nomeLoja + " | Horário: " + infoTurno + " | Estado: " + h.getEstado());
                                }
                            }
                            break;
                        case "2":
                            System.out.println("\nA encerrar sessão... Até à próxima!");
                            continuar = false;
                            break;
                        default:
                            System.out.println("\n❌ Opção inválida! Tenta novamente.");
                    }
                }
            } else {
                System.out.println("\n❌ Login falhou! Verifica as credenciais na base de dados (ou se estás inativo).");
            }

            // Fecha o scanner por boas práticas de programação
            scanner.close();
        };
    }
}