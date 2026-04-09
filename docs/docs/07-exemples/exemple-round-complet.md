```md
# Exemple complet d'un round

## Escenari

- P1: porta `EXECUTIONERS_EDGE`
- P2: porta `BALLISTA`
- P1 tria `ATTACK`
- P2 tria `DEFEND`

## 1. Prioritat

`DEFEND` té prioritat sobre `ATTACK`, així que P2 actua primer.

### Torn de P2
Com que la seva acció és `DEFEND` i no `ATTACK`, `TurnResolver.resolveNonAttackTurn(...)` retorna:

- dany 0
- missatge de defensa "bloqueja l'aire" o equivalent

### Torn de P1
Ara sí que és un atac real.

## 2. `attacker.attack()`

L'espasa `EXECUTIONERS_EDGE` fa `Skills::nothing`, que a la pràctica és un `basicAttackWithMessage`.

## 3. Crear `HitContext`

Es crea el context amb:

- atacant = P1
- defensor = P2
- arma = Sentència Final
- accions = `ATTACK` i `DEFEND`

## 4. Fases

### `START_TURN`
Només efectes de l'atacant.

### `BEFORE_ATTACK`
Efectes de tots dos i passives de l'arma.

### `ROLL_CRIT`
Es permet modificar el crític.

### `resolveCritical()`
Es decideix si el cop és crític.

### `MODIFY_DAMAGE`
Aquí pot entrar la passiva `executor(0.30, 0.25)` si P2 té la vida per sota del 30%.

### `BEFORE_DEFENSE`
Última oportunitat per alterar el cop abans que el defensor el rebi.

## 5. Defensa

Com que P2 ha triat `DEFEND`, `AttackResolver.resolveAttack(...)` crida:

```java
target.defend(damage);
```

El dany rebut final serà el 60% del dany que arribava a aquesta fase.

## 6. Bonus defensiu

`RoundRecoveryService.registerDefenseBonus(...)` detecta que `DEFEND` ha reduït el dany i acumula bonus de vida i mana per a P2.

## 7. `AFTER_DEFENSE` i `AFTER_HIT`

- `AFTER_DEFENSE` s'executa sempre
- `AFTER_HIT` només si hi ha hagut dany real

## 8. Final del round

`CombatSystem`:

- calcula dany rebut per cada jugador
- comprova si hi ha vencedor
- si no n'hi ha, aplica `regen()` a tots dos
- aplica bonus de regeneració a P2 per haver defensat bé
```
