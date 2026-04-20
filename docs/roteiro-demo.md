# Roteiro de demonstracao

Este roteiro ajuda a fazer uma apresentacao rapida e estavel da aplicacao desktop.

## 1. Preparar a base de dados

Executar:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -d gestaohorarios -f .\sql\demo-entrega.sql
```

## 2. Contas recomendadas

Password comum:

```text
123456
```

Perfis:

- `francisco@levis.com`
  - subgerente da Levi's Braga Parque
  - ideal para mostrar dashboard, gestao da loja, gestao de funcionarios, aprovacao de preferencias, geracao de horarios, relatorios, folgas, permutas, perfil e preferencias

- `francisco.gomes@levis.com`
  - gerente da Levi's Braga Parque
  - alternativa para demonstrar fluxos de gestao

- `henrique.siano@levis.com`
  - colaborador full-time
  - ideal para mostrar o ponto de vista do funcionario

## 3. Sequencia sugerida

1. Fazer login como `francisco@levis.com`.
2. Mostrar o dashboard e os proximos turnos.
3. Mostrar a gestao da loja e das regras.
4. Mostrar a gestao de funcionarios.
5. Abrir Preferencias e mostrar a area de decisao da gerencia.
6. Abrir Horarios, selecionar o mes seguinte e gerar uma proposta mensal.
7. Abrir Relatorios Mensais de Horas.
8. Mostrar pedidos de folga e historico.
9. Mostrar permutas.
10. Mostrar o perfil e a edicao de dados.
11. Terminar sessao.
12. Fazer login como `henrique.siano@levis.com`.
13. Mostrar o ponto de vista do colaborador e a diferenca de permissoes.

## 4. Dados incluidos no script demo

O script carrega:

- lojas e regras de exemplo
- uma equipa principal na loja Braga Parque
- turnos passados, atuais e futuros
- folgas em varios estados
- preferencias em varios estados e com decisoes da gerencia
- permutas pendentes e aprovadas
- estrutura pronta para gerar propostas mensais de horario
- um colaborador inativo para teste da gestao de funcionarios

## 5. Cuidados antes da apresentacao

- confirmar que a base de dados foi recarregada no proprio dia
- arrancar a aplicacao depois de executar o script
- testar pelo menos um login de gestao e um login de colaborador
- evitar alterar os dados demo durante a apresentacao sem necessidade
