# ▌Funcionament en combat

[← Tornar a l'índex](../INDEX.md)

---

## ▌Flux d'activació

Quan el jugador selecciona una ulti al menú, no es resol un dany immediat.

El flux és:

1. es comprova si la ulti es pot activar
2. es paga el cost addicional
3. es consumeix la càrrega d'atac
4. s'afegeix un `UltimateChargeBoost`
5. es marca que ja s'ha usat una ulti en el combat
6. es registra l'esdeveniment d'assoliment corresponent
7. el menú retorna `Action.ATTACK`
8. el sistema de combat resol l'atac normalment
9. el boost modifica el cop i expira

Aquest flux evita duplicar lògica d'atac dins del menú.

---

## ▌Efecte intern de menú

Cada ulti té un efecte intern associat que controla:

- cooldown inicial
- disponibilitat contextual
- ús únic per combat
- bloqueig després d'una altra ulti
- requisits específics del tipus d'arma

Aquest efecte no representa un estat visible del personatge. És una peça tècnica per afegir o retirar opcions del menú de manera dinàmica.

---

## ▌Boost de càrrega

`UltimateChargeBoost` és l'efecte que modifica el següent cop.

S'activa en seleccionar la ulti i s'aplica quan el pipeline de combat resol l'atac. Després s'ha d'expirar perquè no afecti més d'un cop.

El boost pot modificar:

- dany final del cop
- probabilitat crítica
- efectes secundaris del cop
- missatges de combat associats

---

## ▌Sobrecàrrega arcana

Requisits de combat:

- arma `MAGICAL`
- atac carregat
- mana actual igual o superior al 75%
- cooldown finalitzat
- cap ulti usada en el combat

Efecte:

- multiplica el cop per `x2.05`
- consumeix un percentatge del mana màxim
- prepara un boost d'un sol atac

Aquesta ulti és la més explosiva, però també exigeix conservar molt mana abans d'activar-la.

---

## ▌Trencament colossal

Requisits de combat:

- arma `PHYSICAL`
- atac carregat
- cooldown finalitzat
- cap ulti usada en el combat

Efecte:

- multiplica el cop per `x1.95`
- consumeix estamina màxima
- consumeix resistència màxima
- pot aplicar `stagger` si el cop impacta

No requereix estamina o resistència mínima per aparèixer. Aquestes barres són recursos ocults i només funcionen com a cost posterior.

---

## ▌Tret èlfic d'obertura

Requisits de combat:

- raça `ELF`
- destresa efectiva mínima de 25
- arma `RANGE`
- atac carregat
- cooldown finalitzat
- cap ulti usada en el combat

Efecte:

- multiplica el cop per `x1.75`
- afegeix `+15%` de probabilitat crítica al cop
- consumeix estamina màxima

És la ulti més restrictiva, perquè depèn de raça, estadística i tipus d'arma.

---

## ▌Missatges

Els missatges d'activació es defineixen juntament amb la resta de missatges d'accions especials.

Els missatges de l'impacte formen part del sistema d'efectes i poden usar colors i símbols de combat. Això separa:

- missatge de menú: la decisió d'alliberar la ulti
- missatge de combat: l'efecte real sobre el cop

Aquesta separació manté coherència amb `Pacte de sang`, `Crida espiritual` i la resta d'efectes de combat.

---
