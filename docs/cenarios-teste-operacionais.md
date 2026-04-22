# Cenarios de teste operacionais

Este guia complementa a demo principal e foi pensado para acelerar testes manuais da aplicacao desktop enquanto novas features continuam a ser desenvolvidas.

## Objetivo

O script [issue-56-dados-teste-operacionais.sql](C:/LEI/2_ano/Semestre_2/P%20II/Projeto2/PlataformaGestaoHorarios/sql/issue-56-dados-teste-operacionais.sql) acrescenta uma equipa operacional complementar na loja `Levi's Braga Parque` sem substituir os dados existentes da demo.

O intervalo de IDs `56001..56099` fica reservado para estes dados, por isso o script pode ser corrido novamente de forma repetivel.

## Ordem recomendada

1. Executar primeiro [demo-entrega.sql](C:/LEI/2_ano/Semestre_2/P%20II/Projeto2/PlataformaGestaoHorarios/sql/demo-entrega.sql) para preparar a base principal.
2. Executar depois [issue-56-dados-teste-operacionais.sql](C:/LEI/2_ano/Semestre_2/P%20II/Projeto2/PlataformaGestaoHorarios/sql/issue-56-dados-teste-operacionais.sql) para enriquecer os cenarios.

Se preferires, podes usar o helper:

```powershell
.\scripts\carregar-dados-teste-operacionais.ps1 -Password 'projeto2026'
```

## Contas adicionadas

Todas as contas abaixo usam a password `123456`.

- `gestor.operacional@levis.com`
  - gerente de teste da loja
  - ideal para testar dashboard de gestão, painel do gerente, contexto operacional e relatórios

- `supervisor.operacional@levis.com`
  - supervisor de teste
  - útil para validar cenarios operacionais sem permissões totais de gerência

- `sofia.almeida.operacional@levis.com`
  - full-time
  - tem preferência permanente por turnos da manhã

- `rui.matos.operacional@levis.com`
  - full-time
  - tem horários futuros e participa em permuta pendente

- `marta.cunha.operacional@levis.com`
  - part-time
  - tem preferência permanente de colega e uma ausência aprovada

- `diogo.lopes.operacional@levis.com`
  - reforço de fim de semana
  - tem férias aprovadas no próximo mês

- `joana.silva.operacional@levis.com`
  - full-time
  - tem pedido de folga pendente com impacto na operação

- `ines.rocha.operacional@levis.com`
  - part-time inativa
  - útil para validar exclusão de utilizadores inativos

## Cenarios cobertos

### 1. Dashboard e contexto diário da loja

- vários colaboradores escalados no dia atual
- vários turnos distribuídos pelos dias seguintes
- mistura de gerente, supervisor, full-time, part-time e reforço

### 2. Painel do gerente e decisões pendentes

- pedido de folga pendente da `Joana Silva`
- permuta pendente entre `Sofia Almeida` e `Rui Matos`
- preferências pendentes e aprovadas com datas que colidem com horários futuros

### 3. Geração mensal

- preferências permanentes com `data_fim = NULL`
- férias aprovadas para o próximo mês
- horários especiais da loja:
  - encerramento técnico
  - campanha de tarde com horário explícito
  - fim de semana com reforço de mínimos

### 4. Casos de fronteira para gestão

- colaboradora inativa associada historicamente à loja
- histórico mínimo de estados de horários
- eventos de auditoria adicionais para contexto de gestão

## Notas de uso

- Este seed não altera fluxos da aplicação; apenas acrescenta dados.
- Foi pensado para ser complementar à demo principal, não para a substituir.
- Se quiseres repetir o cenário, basta correr novamente o script, porque ele limpa apenas o intervalo reservado desta issue.
