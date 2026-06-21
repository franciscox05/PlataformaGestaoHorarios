# Guião de Demonstração — Defesa 25/06

> Roteiro passo-a-passo testado contra os dados de demo reais. O fluxo Web foi
> verificado ao vivo (servidor real + PostgreSQL); o fluxo Desktop está coberto
> pelos testes de integração e pelos dados de demo preparados.
>
> **A história central:** uma plataforma única (Desktop + Web, lógica partilhada)
> que gere **múltiplas lojas** com isolamento estrito de dados, conformidade legal
> (descanso, ACT) e um motor de geração de horários — tudo blindado por **185 testes
> automáticos, 0 falhas, 0 desativados**.

---

## 0. Preparação (5 min antes, uma vez)

```powershell
# 1) Repor a base de dados no estado de demonstração (limpa lixo de testes)
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -d gestaohorarios -f .\sql\demo-entrega.sql

# 2) Ambiente Java (o JAVA_HOME por defeito está partido nesta máquina)
$env:JAVA_HOME = "C:\Users\franc\.jdks\openjdk-25"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 3) (Opcional) Confirmar a suite verde ao vivo
.\mvnw.cmd test    # 185 testes, 0 falhas, 0 skipped

# 4a) Desktop
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.mainClass=com.example.projeto2.AppLauncher"
# 4b) Web (noutro terminal)
java -jar target\Projeto2-0.0.1-SNAPSHOT-web.jar --server.port=8081
```

**Conta principal da demo:** `francisco.gomes@levis.com` / `123456`
→ é **gerente em DUAS lojas** (Braga Parque + NorteShopping) — a estrela do multi-loja.
Outras contas (password `123456`): `henrique.siano@levis.com` (fulltime, Braga),
`diogo.faria@levis.com` (fulltime, NorteShopping), `sofia.marques@levis.com` (supervisor, NorteShopping).

**Estado de demo garantido após reset:**
- Braga Parque: 18 turnos, equipa completa, pedidos pendentes.
- NorteShopping: 6 membros, 14 turnos publicados, **1 folga + 1 permuta + 1 preferência pendentes**.

---

## 1. DESKTOP — o multi-loja como espinha dorsal (≈6 min)

### 1.1 Login multi-loja → seleção de loja
1. Login com `francisco.gomes@levis.com`.
2. **Aparece o ecrã "Seleciona a loja"** com Braga Parque **e** NorteShopping.
   - *Falar:* "O mesmo gestor opera duas lojas. O sistema obriga-o a escolher
     em qual está a trabalhar — tal como o portal Web faz."
3. Escolher **NorteShopping** → entra no Dashboard com **"Levi's NorteShopping"
   fixo na sidebar**.

### 1.2 Perfil + isolamento de dados
4. Sidebar → **Perfil**: abre sem erros, mostra dados da **NorteShopping**.
   - *Falar:* "Antes, esta conta crashava aqui (`IncorrectResultSizeDataAccessException`)
     por ter duas lojas. Resolvido com o conceito de loja activa na sessão."
5. Sidebar → **Horários**: o **Horário da Equipa** mostra só a equipa da
   NorteShopping; o **horário individual** é cross-store (mostra os turnos do
   Francisco em ambas as lojas, com o nome da loja por turno).

### 1.3 Aprovações multi-loja (o fix do "bug 14.2")
6. Sidebar → **Pedidos / Painel do Gerente**: vê o pedido de folga do **Diogo**
   (colaborador da NorteShopping) e a permuta/preferência pendentes.
7. Selecionar a folga do Diogo → **Aprovar**.
   - *Falar:* "Este gerente é da Braga Parque E da NorteShopping. Antes, aprovar
     um pedido da loja 'secundária' era rejeitado com 'não tens permissão'. Agora,
     porque ele escolheu a NorteShopping no login, a aprovação funciona."
   - (Botões "Aprovar/Rejeitar" só ficam ativos com uma linha selecionada.)

### 1.4 Alterar loja sem logout
8. Sidebar → **Alterar loja** (só aparece a quem tem >1 loja) → confirma →
   escolhe **Braga Parque**. Agora os Pedidos/Equipa mostram a Braga Parque.
   - *Falar:* "Troca de contexto operacional sem terminar sessão — e cada loja
     vê estritamente os seus próprios dados e as suas próprias notificações."

### 1.5 Gestão de Loja + geração (se houver tempo)
9. **Gestão de Loja**: horário de funcionamento editável; tentar abertura=fecho
   → erro inline; o sistema bloqueia gravar se um turno ficar fora da janela.
10. **Geração de Horários**: gerar uma proposta para a NorteShopping (mostra o
    motor de backtracking a respeitar regras, cargas e preferências).

---

## 2. WEB — paridade e acessibilidade (≈4 min) — *verificado ao vivo*

`http://localhost:8081/web/login`

### 2.1 Login multi-loja (mesmo modelo do Desktop)
1. Login `francisco.gomes@levis.com` → **redireciona para a seleção de loja**
   com as 2 lojas → escolher **NorteShopping**.

### 2.2 Painel + complementares
2. **Painel** (`/web/painel`): carrega sem erros (antes rebentava com HTTP 500
   para multi-loja — corrigido), mostra a equipa de hoje da NorteShopping.
3. **Complementares**: 3 abas — Folgas, Preferências, Permutas.
   - **Folga "Férias"** → mostra **intervalo de datas** (início + fim), igual ao
     Desktop — cada dia vira um pedido.
   - **Preferência "Colegas"** → escolher **até 2 colegas preferidos** (sem a
     antiga coluna "Evitar", que estava partida); botão **"Submeter"**.

### 2.3 Aprovações na Web (store-correto)
4. **Equipa** (`/web/equipa`): vê os pedidos pendentes da **NorteShopping**
   (folga do Diogo, preferência da Marta, permuta). Aprovar a folga → sucesso.
   - *Falar:* "A Web sempre passou a loja activa da sessão nas aprovações — é o
     mesmo backend partilhado que o Desktop agora também respeita."

---

## 3. PONTOS TÉCNICOS PARA A DEFESA (se perguntarem "e a robustez?")

- **Concorrência (optimistic locking):** dois gestores a decidir o mesmo pedido
  em simultâneo já não causam *lost update* — `@Version` em `DayOff`/`Permuta`
  garante que exatamente uma decisão sobrevive. **Provado por testes de
  concorrência reais** (2 threads, transações JDBC distintas).
- **Conformidade legal (ACT):** descanso mínimo de 11h validado **globalmente**
  por colaborador, mesmo entre lojas diferentes (Código do Trabalho, art. 214.º).
- **Isolamento multi-loja:** loja activa na sessão (Desktop) / `HttpSession` (Web)
  propagada a todos os serviços de gestão — nenhum dado vaza entre lojas.
- **Qualidade:** 185 testes automáticos, 0 falhas, **0 desativados** (todos os
  bugs descobertos na auditoria foram corrigidos e têm teste de regressão).

---

## 4. NOTAS / ARMADILHAS

- **Não demonstrar permutas entre lojas diferentes** — é bloqueado por desenho
  (o descanso/turno é da loja). Se o Slide 3 do pitch promete "permutas
  inter-lojas", apresentá-lo como **visão de Projeto 3**.
- Se algum fluxo de folga/permuta na Web disser "não há horário publicado",
  confirmar que se está na loja certa (NorteShopping e Braga têm escala; Colombo
  e Vasco da Gama, não — são propositadamente vazias).
- Se a BD ficar "suja" a meio (ex.: aprovaram pedidos da demo), basta **repor**
  com o `demo-entrega.sql` (idempotente, limpa tudo e recarrega).
- Limitação conhecida (não bloqueia a demo): um gestor multi-loja a submeter a
  **sua própria** folga valida contra a primeira loja (o `DayOff` não tem `idLoja`)
  — documentado para Projeto 3.
