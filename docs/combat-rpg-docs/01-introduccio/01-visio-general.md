# ▌Visió general

[← Tornar a l'índex](../INDEX.md)

---

## ▌Què és aquest projecte?

**Combat RPG** és un joc de combat per torns 1vs1 amb creació de personatges, estadístiques, armes, habilitats, efectes i sistemes de balance configurables.

El projecte no està pensat com un simple script lineal. Té una separació clara entre:

- arrencada i preload
- creació de personatges
- loop del joc
- resolució de combat
- configuració externa
- efectes i passives
- eines de debug i simulació

---

## ▌Objectiu d'aquesta documentació

Aquests apunts serveixen perquè un desenvolupador nou pugui:

- entendre el flux complet d'execució
- localitzar ràpidament on modificar cada comportament
- afegir armes, efectes, passives o configuracions noves
- programar sense haver de deduir tota l'arquitectura des de zero

---

## ▌Paquets principals

| Carpeta | Responsabilitat |
| --- | --- |
| `combat/` | Resolució de rondes, torns i renderitzat |
| `models/` | Personatges, estadístiques, races i efectes |
| `weapons/` | Armes, atacs, càrrega de definicions i passives |
| `creator/` | Creació interactiva de personatges |
| `config/` | Configuració d'aplicació, UI, debug i paths |
| `balance/` | Balance global i validació |
| `game/` | Loop principal i menús |
| `debug/` | Execució segura, informes i simulacions |
| `utils/` | Entrada, UI, RNG i eines auxiliars |

---

## ▌Idea clau

L'arquitectura gira al voltant d'una idea molt clara:

> **el combat es resol com una pipeline de fases** on interactuen personatge, arma, efectes i configuració global.

Això fa que el projecte sigui molt extensible, perquè moltes mecàniques noves es poden inserir sense reescriure el flux principal.
