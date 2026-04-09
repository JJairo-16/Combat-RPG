```md
# Documentació tècnica del projecte `rpgcombat`

Aquesta documentació està generada a partir de l'anàlisi real del codi Java del projecte. L'objectiu és que una persona nova pugui entendre com està organitzat, com es resol un round de combat, com es calculen els atacs, com s'apliquen passives i efectes, i com ampliar el sistema sense haver vist el codi abans.

## Què cobreix

- arquitectura general del projecte
- flux complet d'execució des de `App` fins a la resolució d'un round
- model de dades de personatges, estadístiques, races, armes i efectes
- pipeline del combat per fases
- sistema de prioritat de torn
- defensa, esquiva, regeneració i càlcul de guanyador
- habilitats i passives actuals de l'arsenal
- guia pràctica per afegir noves armes, habilitats i efectes
- particularitats i detalls importants del codi actual

## Ordre de lectura recomanat

1. `00-introduccio/arquitectura-general.md`
2. `00-introduccio/flux-global.md`
3. `01-core-combat/combat-system.md`
4. `01-core-combat/turn-resolver.md`
5. `02-models/personatge-i-estadistiques.md`
6. `03-armes-i-habilitats/weapon.md`
7. `04-efectes-i-passives/effect-system.md`
8. `05-extensio/guia-per-extendre.md`
9. `06-referencia/taula-de-fitxers.md`

## Convencions d'aquesta documentació

- els noms de classes i mètodes es mantenen en l'idioma del codi
- les explicacions estan en català
- quan es mostren fragments de codi, es fa en Java o pseudocodi
- abans de cada bloc de codi no-Markdown hi ha una barra invertida, tal com has demanat

## Estructura de carpetes d'aquesta documentació

- `00-introduccio/`: visió global
- `01-core-combat/`: motor de combat
- `02-models/`: personatges, races i estadístiques
- `03-armes-i-habilitats/`: armes, tipus i habilitats
- `04-efectes-i-passives/`: efectes persistents i passives d'arma
- `05-extensio/`: com implementar noves mecàniques
- `06-referencia/`: inventari ràpid de fitxers i classes
- `07-exemples/`: exemples complets d'implementació
```
