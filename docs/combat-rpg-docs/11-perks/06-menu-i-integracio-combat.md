# ▌Menú i integració amb combat

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `perks/CombatPerkSystem.java`
- `perks/PlayerPerkState.java`
- `perks/PerkChoiceMenu.java`
- `perks/PerkRegistry.java`
- `perks/effect/PerkEffectFactory.java`

---

## ▌Estat per jugador

`PlayerPerkState` guarda:

- la missió assignada
- si hi ha una elecció de perk pendent

Quan la missió està completada i la recompensa encara no s'ha reclamat, `updatePendingChoice()` activa l'elecció pendent.

```java
public void updatePendingChoice() {
    if (mission != null && mission.completed() && !mission.rewardClaimed()) {
        pendingChoice = true;
    }
}
```

---

## ▌Sistema coordinador

`CombatPerkSystem` és el punt d'integració amb el combat.

Fa servir un `IdentityHashMap<Character, PlayerPerkState>` per associar l'estat a cada instància concreta de personatge.

En construir-se, assigna una missió inicial a cada jugador.

---

## ▌Després de cada torn

`afterTurn(...)` actualitza la missió del personatge actiu.

```java
state.mission().update(
    MissionUpdate.from(actor, opponent, actorAction, opponentAction, result, roundNumber)
);
state.updatePendingChoice();
```

Si el jugador no té estat, no té missió o ja ha reclamat la recompensa, el mètode surt sense fer res.

---

## ▌Resum de missió

`missionSummary(...)` retorna un text amb:

- nom de la missió
- descripció
- progrés actual

El progrés pot mostrar:

- `Recompensa reclamada`
- `Completada`
- el text generat per `progressText()`

---

## ▌Resolució d'eleccions pendents

`resolvePendingChoices(...)` només actua si el jugador té una elecció pendent.

El flux és:

1. comprovar si el jugador està afectat per caos intern
2. generar tres opcions amb `PerkRegistry`
3. mostrar el menú amb `PerkChoiceMenu`
4. crear l'efecte de la perk triada
5. afegir l'efecte al jugador
6. netejar l'elecció pendent i marcar la recompensa com reclamada

---

## ▌Mode corrupte

Si el jugador té l'efecte intern de caos, el sistema demana opcions només corruptes.

```java
boolean corruptedOnly = player.hasEffect(Chaos.INTERNAL_EFFECT_KEY);
List<PerkDefinition> options = PerkRegistry.rollOptions(corruptedOnly, 3, rng);
```

Aquesta regla permet que altres mecàniques del combat modifiquin el tipus de recompensa disponible.

---

## ▌Menú visual

`PerkChoiceMenu` mostra les opcions com targetes i permet moure el cursor amb el teclat.

Responsabilitats del menú:

- ordenar les opcions
- renderitzar el títol i les targetes
- gestionar navegació
- retornar la perk seleccionada

La lògica de selecció queda separada del sistema de combat, de manera que el combat només rep el resultat final.
