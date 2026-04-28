# ▌Missions i progrés

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `perks/mission/MissionDefinition.java`
- `perks/mission/MissionLoader.java`
- `perks/mission/MissionRegistry.java`
- `perks/mission/MissionProgress.java`
- `perks/mission/MissionUpdate.java`
- `perks/mission/MissionEvent.java`
- `perks/mission/ObjectiveType.java`
- `perks/config/MissionConfig.java`

---

## ▌Què és una missió?

Una missió és un objectiu temporal assignat a un jugador durant el combat.

Quan el jugador compleix l'objectiu, desbloqueja una elecció de perk.

---

## ▌Model de missió

`MissionDefinition` conté la informació ja carregada i validada.

Inclou:

- identificador
- nom
- descripció
- pes de selecció
- tipus d'objectiu
- esdeveniment principal
- esdeveniment d'èxit
- esdeveniment de reinici
- seqüència d'accions
- objectiu numèric
- valor mínim
- torns disponibles

---

## ▌Tipus d'objectiu

`ObjectiveType` defineix com es calcula el progrés.

Tipus disponibles:

- `COUNT_EVENT`: compta vegades que passa un esdeveniment
- `SUM_VALUE`: suma un valor, com el dany fet
- `CONSECUTIVE_EVENT`: exigeix èxits consecutius i pot reiniciar-se
- `AVOID_EVENT_FOR_TURNS`: premia evitar un esdeveniment durant torns
- `ACTION_SEQUENCE`: exigeix una seqüència d'accions
- `STATE_REACHED`: completa quan s'assoleix un estat
- `STATE_MAINTAINED`: exigeix mantenir un estat durant torns
- `REACT_TO_EVENT`: exigeix reaccionar a un esdeveniment concret
- `RISK_REWARD`: combina una condició de risc amb un resultat mínim

---

## ▌Esdeveniments observables

`MissionEvent` resumeix allò que una missió pot observar d'un torn.

Exemples:

- `ACTION_ATTACK`
- `ACTION_DEFEND`
- `ACTION_DODGE`
- `ACTION_CHARGE`
- `HIT`
- `DAMAGE_DEALT`
- `CRIT`
- `CHARGED_HIT`
- `SELF_HIT`
- `LOW_HEALTH`
- `HIGH_MOMENTUM`
- `LOW_STAMINA`
- `SURVIVE_TURN`

---

## ▌Actualització del progrés

`MissionUpdate.from(...)` transforma el resultat d'un torn en un conjunt d'esdeveniments.

```java
if (ownerAction == Action.ATTACK) events.add(MissionEvent.ACTION_ATTACK);
if (ownerAction == Action.DEFEND) events.add(MissionEvent.ACTION_DEFEND);
if (ownerAction == Action.DODGE) events.add(MissionEvent.ACTION_DODGE);
if (ownerAction == Action.CHARGE) events.add(MissionEvent.ACTION_CHARGE);
```

Després, `MissionProgress.update(...)` interpreta aquests esdeveniments segons el tipus d'objectiu.

---

## ▌Finalització

Quan el progrés arriba al `target`, la missió queda completada.

```java
if (progress >= definition.target()) complete();
```

La recompensa no es marca com a reclamada fins que el jugador resol l'elecció de perk.

---

## ▌Text de progrés

`progressText()` dona una representació curta per a la interfície.

Si l'objectiu és binari, mostra `Pendent` o `Completada`.

Si és numèric, mostra el format `actual/objectiu`.
