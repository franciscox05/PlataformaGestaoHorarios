# WEB

Interface Web da entrega, implementada com Spring MVC + Thymeleaf.

## Estado atual

- login/logout Web com sessao HTTP;
- pagina de consulta do planeamento mensal;
- acao de geracao de proposta mensal de horarios;
- tabela de alternativas geradas para o periodo;
- fluxos complementares Web: preferencias, folgas e permutas (submissao + historico).
- execucao separada desktop/web com classes principais distintas (`Projeto2Application` e `Projeto2WebApplication`).

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

## Perfis de execucao (IntelliJ)

Criar duas configuracoes Spring Boot distintas:

- `Desktop` -> main class `com.example.projeto2.Projeto2Application`
- `Web` -> main class `com.example.projeto2.Projeto2WebApplication`

Isto evita abrir desktop e web ao mesmo tempo no mesmo arranque.

## Validacao da entrega

Evidencias e checklist em:

- `docs/validacao-entrega-web-desktop.md`
