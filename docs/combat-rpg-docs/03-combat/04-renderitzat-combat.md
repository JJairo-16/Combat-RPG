# ▌Renderitzat de combat

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe

`combat/ui/CombatRenderer.java`

---

## ▌Funció

Separa la presentació textual de la lògica del combat.

`CombatSystem` resol una ronda i després delega a `CombatRenderer` la seva visualització:

- capçalera de ronda
- resultat de cada torn
- resum de dany
- resum de regeneració

---

## ▌Per què és útil?

Aquesta separació fa que:

- la lògica no depengui directament del format de text
- sigui més fàcil canviar la UI de consola
- es puguin centralitzar millores visuals

---

## ▌Quan tocar aquesta classe

- si vols canviar el format del log
- si vols afegir informació al resum d'una ronda
- si vols mostrar estats/efectes de manera més clara
