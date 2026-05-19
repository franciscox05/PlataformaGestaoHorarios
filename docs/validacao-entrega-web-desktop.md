# Validacao da Entrega Web + Regressao Desktop

Data de validacao: 2026-05-19  
Responsavel: `taigueis` (execucao assistida por Codex)

## 1) Testes automatizados

Comando executado:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -Dtest=WebAccessIntegrationTest test
```

Resultado final:

- `BUILD SUCCESS`
- `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`

Cobertura funcional do teste:

- acesso anonimo redireciona para login em paginas protegidas;
- login valido redireciona para horarios;
- login invalido devolve para login;
- logout termina sessao;
- acesso a complementares com sessao autenticada;
- verificacao de comportamento de permissao em horarios.

## 2) Checklist manual Desktop

Estado: sem regressao critica identificada durante arranques e fluxos de validacao da entrega.

- [x] Aplicacao desktop arranca com perfil `Desktop`
- [x] Arranque desktop nao deve abrir interface web automaticamente
- [x] Fluxos desktop base mantidos (login, navegacao principal)

## 3) Checklist manual Web

Estado: funcional com correcoes de estabilidade e layout aplicadas.

- [x] `GET /web/login` funcional
- [x] autenticacao e logout funcionais
- [x] `GET /web/horarios` funcional com sessao
- [x] `GET /web/complementares` funcional com sessao
- [x] submissao de preferencia
- [x] submissao de folga
- [x] submissao de permuta
- [x] historicos de preferencias/folgas/permutas visiveis
- [x] mensagens de erro/sucesso apresentadas ao utilizador

## 4) Bugs encontrados e corrigidos

1. Erro 500 em `/web/complementares`  
   Causa: formatação de `dataPedido` com tipo incorreto (`LocalDateTime` em vez de `Instant`).  
   Correcao: conversao segura `Instant -> LocalDateTime` no `WebComplementaresController`.

2. Sobreposicao de elementos no card de selecao de periodo (`/web/horarios`)  
   Causa: grid do formulario com colunas inadequadas para largura do card.  
   Correcao: ajuste de CSS para botao em linha propria e largura total do container.

## 5) Estado para submissao

Estado atual: **apto para submissao**, sem bug critico aberto nos fluxos de demonstracao web validados.
