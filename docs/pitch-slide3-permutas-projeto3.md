# Slide 3 do Pitch — reformulação (permutas inter-lojas como visão Projeto 3)

> **Porquê reformular:** a versão antiga do Slide 3 prometia "permutas cruzadas
> inter-lojas" como funcionalidade entregue. O código **bloqueia-as por desenho**
> (`PermutaService` exige a mesma loja entre origem e destino) — e isso é o
> **correto** a nível legal/operacional: um turno e o respetivo descanso pertencem
> à loja onde o colaborador está escalado, e trocar turnos entre lojas diferentes
> levanta questões de carga contratual, descanso e responsabilidade que não são
> triviais. Apresentar como "feito" seria uma promessa que a demo não cumpre.
> Solução: apresentar **o que está entregue** com confiança e **o cross-store como
> visão de evolução (Projeto 3)**.

---

## ✅ Texto sugerido para o Slide 3

**Título:** Permutas de turnos — flexibilidade com controlo

**O que entregamos (Projeto 2):**
- Permutas de turno entre colegas **da mesma loja**, com aprovação da gerência.
- **Permuta de folga** (um colega com folga assume o teu turno; tu assumes o dele
  noutro dia) — também intra-loja.
- Validações automáticas em cada permuta: **descanso mínimo de 11h**, antecedência
  de 24h, sem sobreposição de horários, e bloqueio de trocas redundantes.
- Deteção de conflitos: aprovar uma permuta rejeita automaticamente as pendentes
  que disputam o mesmo turno, com proteção de concorrência (optimistic locking).

**Visão de evolução (Projeto 3):**
- **Permutas inter-lojas** para colaboradores com vínculo a múltiplas lojas
  (ex.: gerentes/reforços regionais), com um modelo de conformidade que estende
  as validações de descanso e carga contratual **através das fronteiras de loja**.
- Requer: descanso e carga calculados globalmente por pessoa (já temos a base — a
  validação de descanso de 11h já é **global por colaborador**, mesmo entre lojas),
  + regras de elegibilidade e aprovação dupla (gerente de ambas as lojas).

---

## 🎤 Frase para dizer (se perguntarem "porque não cross-store já?")

> "As permutas inter-lojas não são um problema de UI — são um problema de
> conformidade laboral. Um turno tem descanso, carga contratual e responsabilidade
> associados à loja. Preferimos entregar permutas intra-loja **robustas e legais**
> agora, e ter o cross-store como uma evolução desenhada, já com a fundação pronta:
> a nossa validação de descanso mínimo de 11h **já funciona globalmente por
> pessoa**, mesmo entre lojas diferentes."

---

## Nota técnica (para o relatório, não para o slide)
- Onde o cross-store é bloqueado: `PermutaService.validarPedido`
  (`idLojaOrigem.equals(idLojaDestino)` obrigatório) e o equivalente em
  `PermutaFolgaService`.
- Fundação já existente: `HorarioService.validarDescansoMinimoGlobal` valida o
  descanso de 11h **sobre todas as lojas** do colaborador (Revisao.md, ponto 22.1).
