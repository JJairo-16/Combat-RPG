# ▌Ultis de segona etapa

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

Les ultis de segona etapa són accions especials avançades que converteixen una càrrega d'atac preparada en un cop immediat molt més potent.

No substitueixen el sistema normal d'atac carregat. El complementen com una capa secreta, limitada i condicionada.

La seva funció principal és crear moments excepcionals dins del combat sense convertir-se en una opció rutinària.

---

## ▌Regla principal

Una ulti de segona etapa només pot aparèixer quan el personatge ja té un atac carregat i compleix les condicions del seu arquetip.

En activar-se:

1. consumeix la càrrega d'atac
2. paga el cost addicional corresponent
3. prepara un boost intern d'un sol cop
4. marca que ja s'ha usat una ulti en aquell combat
5. retorna l'acció `ATTACK`
6. el combat resol l'atac pel flux normal

Això fa que la ulti sigui una barreja entre acció especial i atac: la lògica especial es prepara des del menú, però el dany es resol dins del pipeline normal de combat.

---

## ▌Ultis disponibles

El sistema defineix tres ultis, una per cada tipus principal d'arma:

- `Sobrecàrrega arcana`: associada a armes màgiques
- `Trencament colossal`: associada a armes físiques
- `Tret èlfic d'obertura`: associada a armes de rang

Cada una té requisits propis, però totes comparteixen la mateixa estructura interna.

---

## ▌Limitacions globals

Les ultis estan pensades com una mecànica gairebé secreta. Per això comparteixen diverses limitacions:

- només es poden usar una vegada per combat
- només es pot usar una ulti total per combat
- comencen amb cooldown inicial
- requereixen atac carregat
- consumeixen la càrrega en activar-se
- no apareixen si ja s'ha usat una acció especial de menú durant el torn
- no apareixen si no estan desbloquejades pel progrés global

Aquestes restriccions eviten que es converteixin en una rotació normal del combat.

---

## ▌Relació amb el combat

La ulti no aplica el dany directament.

El menú només activa un efecte intern que prepara el següent atac. Després retorna `ATTACK`, de manera que `TurnResolver` continua resolent el cop com qualsevol altre atac.

Això manté intactes les regles de:

- arma equipada
- crítics
- passives
- defensa
- esquiva
- efectes de fase
- assoliments d'atac
- missatges de combat

La ulti entra al sistema com un modificador temporal del cop, no com una ruta alternativa de dany.

---
