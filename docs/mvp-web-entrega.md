# MVP da Entrega Web

Este documento fecha o ambito funcional minimo da entrega Web e evita desvio de escopo.

## Objetivo de entrega

Entregar uma aplicacao Web Java funcional, integrando com a API/camada de negocio existente, acompanhada de:

- ficheiro ZIP com projeto e dependencias;
- relatorio PDF atualizado com capitulo da Web e tecnologias usadas.

## MVP obrigatorio

1. Autenticacao Web:
- login com email/password;
- sessao ativa com controlo basico de acesso;
- logout funcional.

2. Modulo de horarios Web:
- consulta de planeamento por ano/mes;
- visualizacao de proposta/planeamento com resumo;
- acao de geracao de proposta para o periodo selecionado.

3. Pelo menos 1 fluxo complementar:
- preferencias, ou
- folgas, ou
- permutas.

4. Entregaveis de submissao:
- script/roteiro de geracao do ZIP;
- relatorio final PDF atualizado.

## Fora de escopo desta entrega

- paridade integral com todos os ecras desktop;
- refatoracoes profundas sem impacto direto na submissao;
- polimento UX extenso sem ganho funcional relevante para a avaliacao.

## Riscos e mitigacao

- Risco: divergencia entre desktop e Web nas regras de negocio.
  Mitigacao: reaproveitar BLL/repositorios e validar cenarios criticos.

- Risco: atraso na consolidacao dos entregaveis finais.
  Mitigacao: preparar ZIP/PDF em paralelo com desenvolvimento funcional.

- Risco: regressao no desktop.
  Mitigacao: executar checklist de regressao antes de fechar entrega.

## Checklist de fecho

- [ ] Escopo MVP validado pela equipa
- [ ] Fluxo de login/logout Web funcional
- [ ] Consulta e geracao de horarios Web funcional
- [ ] Fluxo complementar funcional (preferencias/folgas/permutas)
- [ ] ZIP final gerado e testado em ambiente limpo
- [ ] PDF final atualizado e revisto pela equipa
