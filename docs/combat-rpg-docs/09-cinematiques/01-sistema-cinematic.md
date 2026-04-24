# ▌Sistema de cinemàtiques

[← Tornar a l'índex](../INDEX.md)

---

## ▌Paquet

`utils/cinematic/`

---

## ▌Què resol?

Aquest sistema permet reproduir **cinemàtiques de text per terminal** abans, durant o després de fragments importants del joc.

No és només una impressió de text; és una capa interactiva que controla:

- escenes
- blocs de text
- colors
- ritme d'escriptura
- entrada de teclat
- neteja de pantalla
- terminal compartit

---

## ▌Peces principals

- `TextCinematic`
- `Scene`
- `TextBlock`
- `TypingAnalyzer`
- `TypingEngine`
- `MarkupParser`
- `TerminalController`
- `CinematicInput`

---

## ▌Flux general

```text
TextCinematic
  ↓
Scene
  ↓
TextBlock
  ↓
MarkupParser
  ↓
TypingAnalyzer
  ↓
TypingEngine
  ↓
Terminal
```

---

## ▌Ús bàsic

```java
TextCinematic intro = TextCinematic.builder()
        .clearScreenOnEnd(true)
        .scene(escenaInicial)
        .build();

intro.play();
```

---

## ▌Lectura arquitectònica

La cinemàtica està separada en dues parts:

- **API externa**, per construir escenes i blocs
- **motor intern**, per interpretar tags, calcular ritme i escriure a terminal

Això permet que el codi del joc només decideixi **què es vol explicar**, mentre el motor decideix **com es mostra**.

---

## ▌Controls

- `Espai`: completa o avança l'escena actual
- `Enter`: salta tota la cinemàtica

---

## ▌Quan tocar aquest paquet

- si vols afegir una nova cinemàtica narrativa
- si vols canviar el ritme d'escriptura
- si vols ampliar els tags disponibles
- si vols carregar cinemàtiques des d'un fitxer extern
