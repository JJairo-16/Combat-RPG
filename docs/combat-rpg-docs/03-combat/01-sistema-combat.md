# ▌Sistema de combat

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe central

`combat/CombatSystem.java`

És l'orquestrador d'una ronda sencera.

---

## ▌Responsabilitats

- decidir l'ordre dels torns
- aplicar anti-stall si toca
- resoldre els dos torns
- calcular dany rebut
- determinar guanyador
- aplicar regeneració
- activar o eliminar efectes globals de ronda

---

## ▌Dependències internes

`CombatSystem` injecta o crea:

- `CombatRenderer`
- `AttackResolver`
- `EffectPipeline`
- `RoundRecoveryService`
- `TurnResolver`
- `TurnPriorityPolicy`

---

## ▌Model mental d'una ronda

```text
inici ronda
→ possibles efectes globals
→ decidir qui va primer
→ resoldre primer torn
→ resoldre segon torn
→ calcular danys
→ comprovar vencedor
→ aplicar regen
→ ajustar efectes post-ronda
```

---

## ▌Mostra de codi real

```java
boolean p1First = priorityPolicy.player1First(player1, a1, player2, a2, combatRng);

if (p1First) {
    firstTurn = turnResolver.resolveTurn(player1, player2, a1, a2, p2Bonus);
    secondTurn = turnResolver.resolveTurn(player2, player1, a2, a1, p1Bonus);
} else {
    firstTurn = turnResolver.resolveTurn(player2, player1, a2, a1, p1Bonus);
    secondTurn = turnResolver.resolveTurn(player1, player2, a1, a2, p2Bonus);
}
```

---

## ▌On modificar el comportament global

Si la regla afecta totes les rondes, aquest és un dels llocs més probables on tocar:

- ordre de torns
- condicions de finalització
- regen de final de ronda
- mecàniques globals de pressió temporal
