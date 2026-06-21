# Checklist Manual Final — só o que precisa dos teus olhos

> Tudo o resto (backend, Web E2E, carregamento de todos os ecrãs) já está provado
> por execução automática. Isto são **apenas** as interações de clique na UI
> JavaFX que não se conseguem automatizar. A lógica por trás de cada uma já foi
> verificada por código — aqui só confirmas o comportamento visual.
>
> **Arrancar a app:**
> ```powershell
> $env:JAVA_HOME = "C:\Users\franc\.jdks\openjdk-25"
> $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
> .\mvnw.cmd spring-boot:run "-Dspring-boot.run.mainClass=com.example.projeto2.AppLauncher"
> ```
> Conta gerente multi-loja: **`francisco.gomes@levis.com`** / `123456`
> Marca cada caixa: ✅ passou · ❌ falhou (anota o que viste)

---

## A. Painel do Gerente — seleção e botões (T2.2 + T2.3)

Login → escolher uma loja → sidebar **Pedidos** (Painel do Gerente).
Precisas de pelo menos 2 pedidos pendentes na tabela (folgas ou permutas) — se
não houver, submete-os antes via Web com um colaborador dessa loja.

- [ ] **A1.** Sem clicar em nenhuma linha: os botões **"Aprovar"** e **"Rejeitar"**
      estão **cinzentos/desativados**.
- [ ] **A2.** Clica na **1ª linha** → o painel de contexto à direita mostra os
      dados desse colaborador; os botões ficam **ativos**.
- [ ] **A3.** Clica na **2ª linha** (sem aprovar nada) → o contexto muda para o
      2º colaborador.
- [ ] **A4.** Com a 2ª linha selecionada, clica **"Aprovar"** → é aprovado o
      pedido da **2ª linha** (o que estava selecionado), não o da 1ª.
- [ ] **A5.** Depois de aprovar/rejeitar, os botões **voltam a ficar cinzentos**
      (a seleção foi limpa).
- [ ] **A6.** Repete A2–A4 no separador de **Permutas** e de **Preferências**.

---

## B. Gestão de Loja — validação de campos (T2.4)

Sidebar → **Gestão de Loja**.

- [ ] **B1.** Limpa a hora de abertura **ou** de fecho e clica **"Guardar"** →
      depois de confirmar o diálogo, aparece **erro inline** (não crash), algo
      como *"As horas de abertura e fecho são obrigatórias."*
- [ ] **B2.** Põe hora de abertura **igual** à de fecho (ex.: 09:00 e 09:00) e
      guarda → erro *"A hora de abertura e a hora de fecho não podem ser iguais."*
- [ ] **B3.** No diálogo de confirmação "Deseja guardar...", clica **Cancelar** →
      nada muda, nenhuma mensagem aparece.

---

## C. Multi-loja — reconfirmação visual (o coração da defesa)

- [ ] **C1.** Login `francisco.gomes` → aparece o **ecrã de seleção de loja** com
      Braga Parque **e** NorteShopping.
- [ ] **C2.** Escolhe **NorteShopping** → na **sidebar** aparece fixo
      "Levi's NorteShopping"; o **Perfil** abre sem erro e mostra NorteShopping.
- [ ] **C3.** No Painel do Gerente, a **bolinha de Pedidos** mostra **só** os
      pendentes da NorteShopping (não somados com Braga Parque).
- [ ] **C4.** Aprova um pedido → a bolinha desce corretamente e **não** aparece
      nada da outra loja.
- [ ] **C5.** Faz logout e re-login a escolher **Braga Parque** → agora vês
      **só** os pedidos/bolinhas de Braga Parque.
- [ ] **C6.** (Gestão de Loja / Funcionários / Relatórios) com NorteShopping
      ativa → confirma que mostram dados da **NorteShopping**, não de Braga Parque.

---

## D. Navegação geral (T2.1 — já provado por teste, confirmação rápida)

Abre cada ecrã da sidebar uma vez e confirma que **carrega sem ecrã em branco
nem diálogo de erro**: Dashboard · Pedidos · Horários · Gestão de Loja ·
Gestão de Funcionários · Relatórios · Perfil · Geração de Horários.

- [ ] **D1.** Todos abrem com conteúdo carregado.
- [ ] **D2.** Trocar de ecrã e voltar não duplica linhas/botões nas tabelas.

---

### Se algo falhar
Anota: que ecrã, que passo (ex.: A4), o que esperavas vs. o que viste, e
copia o texto de qualquer erro/stack trace da consola. Eu corrijo — é
continuação direta deste trabalho.

### Notas
- **Não** testar permutas entre lojas diferentes (bloqueio intencional; o
  Slide 3 do pitch deve apresentar isso como visão Projeto 3).
- Um gerente multi-loja a pedir a **sua própria** folga valida contra a 1ª loja
  (limitação documentada, exige mudança de schema — fora de scope).
