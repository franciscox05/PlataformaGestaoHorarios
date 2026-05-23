# Validacao funcional final Web vs Desktop

Este ficheiro regista a validacao de paridade funcional base entre os dois canais da plataforma.

## Data de validacao

- 2026-05-23

## Premissas de paridade

- A versao Desktop e a versao Web usam a mesma base de dados PostgreSQL.
- Os modulos Web reutilizam a mesma camada BLL da versao Desktop.
- As regras de negocio sao executadas no backend comum, evitando divergencia funcional entre canais.

## Matriz de validacao por modulo

- Autenticacao e sessao:
  - Desktop: validado
  - Web: validado
  - Resultado: OK (mesmas credenciais, sessao obrigatoria, logout funcional)

- Painel:
  - Desktop: validado
  - Web: validado
  - Resultado: OK (ponto de entrada com indicadores e contexto)

- Horarios:
  - Desktop: validado
  - Web: validado
  - Resultado: OK (consulta de periodo, proposta atual, alternativas, geracao e decisao quando permitido)

- Complementares (folgas, preferencias, permutas):
  - Desktop: validado
  - Web: validado
  - Resultado: OK (submissao, historico e aprovacao por perfis de gestao)

- Relatorios:
  - Desktop: validado
  - Web: validado
  - Resultado: OK (filtros por periodo/colaborador e indicadores de resumo)

- Gestao Loja:
  - Desktop: validado
  - Web: validado
  - Resultado: OK (horario base, regras e horarios especiais)

- Perfil:
  - Desktop: validado
  - Web: validado
  - Resultado: OK (consulta e edicao com validacoes e feedback)

## Validacao de acessos por perfil

- Rotas web com guardas de sessao:
  - sem sessao: redireciona para `/web/login`
  - com sessao: acesso permitido conforme perfil

- Permissoes por modulo:
  - colaborador: sem acesso a modulos de gestao
  - gerente/supervisor: acesso aos modulos de gestao conforme BLL

## Evidencia automatizada

Testes executados:

- `WebAcessoPerfisIntegrationTest`
- `WebFluxosCriticosE2ETest`

Resultado:

- 5 testes executados
- 0 falhas
- 0 erros

## Conclusao

A paridade funcional base entre Desktop e Web foi validada para os modulos alvo da frente Web. A plataforma encontra-se pronta para fecho funcional da entrega, mantendo consistencia de dados e regras de negocio entre os dois canais.
