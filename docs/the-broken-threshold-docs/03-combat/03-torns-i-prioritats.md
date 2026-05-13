# ▌Torns i prioritats

[← Tornar a l'índex](../INDEX.md)

---

## ▌Ordre de torns

La decisió de qui actua primer depèn d'una política: `TurnPriorityPolicy`.

La implementació per defecte és `DefaultTurnPriorityPolicy`.

---

## ▌Resolució d'un torn

`TurnResolver` resol un sol torn complet d'un atacant contra un defensor.

Entre les seves responsabilitats hi ha:

- activar hooks d'inici de torn
- executar el ritme de combat
- crear i actualitzar `HitContext`
- aplicar fases del pipeline
- llançar l'atac de l'arma
- aplicar efectes post-impacte
- acumular missatges de combat

---

## ▌Dependències del torn

- `AttackResolver`
- `EffectPipeline`
- `RoundRecoveryService`
- `CombatRhythmService`

---

## ▌Idea clau

`TurnResolver` és la peça més important si vols entendre **què passa exactament quan un jugador actua**.

Si necessites afegir una mecànica nova que només passa:

- abans de colpejar
- quan hi ha crític
- després de defensar
- al final del torn

probablement el canvi passarà per `HitContext` + `EffectPipeline` + `TurnResolver`.
