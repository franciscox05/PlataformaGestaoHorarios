package com.example.projeto2;

import com.example.projeto2.BLL.UtilizadorBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    // A tua BLL que vai à base de dados!
    private final UtilizadorBLL userBll;

    // O Spring Boot vai injetar a BLL aqui magicamente
    public LoginController(UtilizadorBLL userBll) {
        this.userBll = userBll;
    }

    @FXML
    protected void onLoginClick() {
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        System.out.println("A tentar login na Base de Dados com: " + email);

        // Chamamos o TEU método da BLL!
        Utilizador logado = userBll.efetuarLogin(email, password);

        if (logado != null) {
            System.out.println("✅ SUCESSO! Bem-vindo(a), " + logado.getNome());
            // No futuro, aqui será o código para abrir a janela "Dashboard"
        } else {
            System.out.println("❌ ERRO! Email ou Password incorretos.");
            // No futuro, aqui será o código para mostrar uma mensagem de erro vermelha no ecrã
        }
    }
}