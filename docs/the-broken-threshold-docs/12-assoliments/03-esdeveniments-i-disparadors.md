# ▌Esdeveniments i disparadors

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

Els assoliments avancen quan alguna part del joc envia un `AchievementUpdate` a `AchievementSystem`.

L'actualització pot contenir un o més `AchievementEvent` i un mapa de camps amb informació contextual.

---

## ▌Esdeveniments principals

`AchievementEvent` agrupa els esdeveniments observables del joc:

- accions: `ACTION_ATTACK`, `ACTION_DEFEND`, `ACTION_DODGE`, `ACTION_CHARGE`
- atac: `HIT`, `MISS`, `CRIT`, `DAMAGE_DEALT`, `DAMAGE_RECEIVED`
- resultat: `MATCH_STARTED`, `MATCH_FINISHED`, `MATCH_WON`, `MATCH_LOST`, `MATCH_TIED`
- armes: `WEAPON_EQUIPPED`, `WEAPON_USED`
- perks: `PERK_MISSION_PROGRESS`, `PERK_MISSION_COMPLETED`, `PERK_GAINED`, `PERK_ACTIVATED`
- especials: `BLOOD_PACT_USED`, `SPIRITUAL_CALLING_USED`, `CHAOS_MATCH_STARTED`

---

## ▌Punts d'entrada

`AchievementSystem` exposa mètodes explícits per als casos més importants:

- `onTurn(...)`
- `onMatchFinished(...)`
- `onWeaponEquipped(...)`
- `onPerkMissionProgress(...)`
- `onPerkMissionCompleted(...)`
- `onPerkGained(...)`
- `onDivinePerkAssigned(...)`
- `onSynergyActivated(...)`
- `onBloodPactUsed(...)`
- `onSpiritualCallingUsed(...)`
- `onChaosMatchStarted(...)`

Aquests mètodes converteixen fets del joc en actualitzacions genèriques.

---

## ▌Camps contextuals

Un esdeveniment pot incloure camps com:

- `weaponId`
- `winnerWeaponId`
- `damage`
- `roundNumber`
- `chaosMode`
- `ownerAction`
- `opponentAction`
- `perkId`
- `missionId`

Els camps són el que permet que un mateix esdeveniment serveixi per molts assoliments diferents.

---

## ▌Memòria temporal de combat

`AchievementSystem` conserva una memòria interna per actor durant el combat.

Aquesta memòria permet generar esdeveniments derivats, per exemple:

- guanyar sense haver fet crítics
- usar Pacte de Sang i Crida Espiritual en la mateixa partida
- resoldre el Grimori després d'haver pagat vida amb Pacte de Sang

Quan acaba el combat, aquesta memòria temporal es neteja.

---

## ▌Bones pràctiques

- afegir un esdeveniment nou només si no es pot expressar amb els existents
- reutilitzar camps genèrics sempre que sigui possible
- no fer que un assoliment depengui de missatges de text
- mantenir els identificadors estables perquè el progrés desat no es trenqui
