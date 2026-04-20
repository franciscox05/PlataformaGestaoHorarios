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
  - ideal para mostrar dashboard, gestao da loja, gestao de funcionarios, relatorios, folgas, permutas, perfil e preferencias

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
5. Abrir relatorios mensais de horas.
6. Mostrar pedidos de folga e historico.
7. Mostrar permutas.
8. Mostrar preferencias.
9. Mostrar perfil e edicao de dados.
10. Terminar sessao.
11. Fazer login como `henrique.siano@levis.com`.
12. Mostrar os modulos do colaborador e a diferenca de permissoes.

## 4. Dados incluidos no script demo

O script carrega:

- lojas e regras de exemplo
- uma equipa principal na loja Braga Parque
- turnos passados, atuais e futuros
- folgas em varios estados
- preferencias em varios estados
- permutas pendentes e aprovadas
- um colaborador inativo para teste da gestao de funcionarios

## 5. Cuidados antes da apresentacao

- confirmar que a base de dados foi recarregada no proprio dia
- arrancar a aplicacao depois de executar o script
- testar pelo menos um login de gestao e um login de colaborador
- evitar alterar os dados demo durante a apresentacao sem necessidade
