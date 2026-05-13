# ▌Desbloqueig i aparició al menú

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

Les ultis no estan disponibles des del principi. Formen part del progrés global del jugador i només apareixen quan s'han descobert les peces necessàries del sistema.

El desbloqueig es divideix en dues capes:

- requisits de progrés, que indiquen si la ulti existeix per al jugador
- requisits de combat, que indiquen si pot aparèixer en aquell moment

---

## ▌Requisit comú

Totes les ultis requereixen haver descobert l'acció base de càrrega:

- `ACTIONS / CHARGE`

Això reforça que són una segona etapa de `Carregar atac` i no una acció independent.

---

## ▌Requisits de progrés per ulti

Cada ulti requereix un assoliment visible relacionat amb el seu tipus d'arma:

| Ulti | Descobriment | Assoliment requerit |
|---|---|---|
| `Sobrecàrrega arcana` | `ACTIONS / CHARGE` | `ARCANE_INITIATE` |
| `Trencament colossal` | `ACTIONS / CHARGE` | `PHYSICAL_INITIATE` |
| `Tret èlfic d'obertura` | `ACTIONS / CHARGE` | `EAGLE_EYE` |

Aquests assoliments són visibles perquè indiquen progrés d'arquetip, no revelen directament l'existència de la ulti.

---

## ▌Assoliments de progrés

Els assoliments de progrés representen experiència amb un tipus d'arma:

- `ARCANE_INITIATE`: utilitzar 4 vegades armes màgiques
- `PHYSICAL_INITIATE`: utilitzar 4 vegades armes físiques
- `EAGLE_EYE`: utilitzar 4 vegades armes de rang

Aquests assoliments desbloquegen la possibilitat de veure la ulti corresponent, sempre que també s'hagi descobert `CHARGE`.

---

## ▌Assoliments ocults d'ulti

Els assoliments per usar les ultis són ocults.

- `ARCANE_OVERLOAD_RELEASED`
- `COLOSSAL_BREAK_RELEASED`
- `ELVEN_OPENING_RELEASED`
- `THREE_HIDDEN_RUPTURES`

Aquests assoliments no guien el jugador abans de trobar la mecànica. Només registren que ha descobert i utilitzat una ruptura secreta.

---

## ▌Aparició al menú

Una ulti només apareix al menú si es compleixen totes aquestes condicions:

1. la ulti està desbloquejada pel progrés global
2. el personatge té l'efecte intern de la ulti actiu
3. el cooldown inicial ja ha acabat
4. el personatge té un atac carregat
5. el tipus d'arma i requisits específics encaixen
6. no s'ha usat cap ulti en el combat
7. no s'ha usat cap acció especial de menú durant el torn

Quan el personatge carrega l'atac, l'opció `Carregar atac` desapareix fins que la càrrega es consumeix. Si una ulti consumeix la càrrega, l'opció de càrrega torna a aparèixer.

---

## ▌Separació entre menú i progrés

El menú no decideix per si sol què està desbloquejat. Consulta una capa d'unlock que comprova assoliments i descobriments.

Això permet provar visualment o en tests la part de combat sense haver de completar tot el progrés, però en la partida normal el filtre depèn del progrés persistent del jugador.

---
