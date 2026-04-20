# Testes automatizados dos fluxos criticos

Esta suite cobre quatro fluxos principais da aplicacao:

- autenticacao com migracao de passwords legadas
- atualizacao de password no perfil
- aprovacao de preferencias pela gerencia
- geracao e validacao de proposta mensal de horarios

## Como funciona

Os testes usam o contexto Spring Boot real e a base PostgreSQL configurada na aplicacao.

Cada teste cria a sua propria loja, equipa, regras e dados de apoio dentro de uma transacao com rollback automatico. Isto permite repetir a execucao sem deixar lixo funcional na base de dados local.

Para a geracao mensal, a fixture aplica overrides de regras na loja de teste. Assim, a suite continua deterministica mesmo quando a base local ja tem regras configuradas para outras lojas.

## Como correr

Com a JDK configurada no projeto:

```powershell
mvn test
```

Neste ambiente local, caso seja necessario forcar JDK 21:

```powershell
mvn -Djava.version=21 test
```

Para correr apenas a suite dos fluxos criticos:

```powershell
mvn -Djava.version=21 -Dtest=FluxosCriticosIntegrationTest test
```

## Notas

- a ligacao a PostgreSQL continua a vir de `src/main/resources/application.properties`
- o perfil de teste apenas reduz ruido de logs e desativa comportamento desnecessario para a suite
- se a base local estiver vazia de turnos, a fixture cria os turnos minimos para a geracao mensal
