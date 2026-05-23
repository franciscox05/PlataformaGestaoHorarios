# WEB

Interface Web da entrega, implementada com Spring MVC + Thymeleaf.

## Estado atual

- shell comum com navegacao modular por rotas distintas;
- guards de sessao e permissao por modulo no backend;
- painel Web como ponto de entrada apos autenticacao;
- modulo de horarios com consulta, geracao e decisao de propostas;
- modulo complementares com fluxos de folgas, preferencias e permutas;
- modulo de relatorios com filtros por periodo e colaborador;
- modulo de gestao de loja com horario base, regras e horarios especiais;
- modulo de perfil com consulta e edicao de dados.

## Paridade Desktop vs Web

- os dois canais usam a mesma base de dados e a mesma camada BLL;
- a informacao apresentada na Web e alimentada pelas mesmas regras de negocio da Desktop;
- o visual da Web foi alinhado com a identidade da Desktop, incluindo a imagem de login.

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
