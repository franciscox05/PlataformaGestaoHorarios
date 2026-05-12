# WEB

Interface Web da entrega, implementada com Spring MVC + Thymeleaf.

## Estado atual

- login/logout Web com sessao HTTP;
- pagina de consulta do planeamento mensal;
- acao de geracao de proposta mensal de horarios;
- tabela de alternativas geradas para o periodo.

## Arranque local

Executar a aplicacao Web com a classe principal `Projeto2WebApplication`.

Exemplo com Maven:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.main-class=com.example.projeto2.Projeto2WebApplication"
```

Depois abrir no browser:

```text
http://localhost:8080/web/login
```
