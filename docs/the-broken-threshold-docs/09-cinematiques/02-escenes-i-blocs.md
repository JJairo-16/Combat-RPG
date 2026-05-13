# ▌Escenes i blocs

[← Tornar a l'índex](../INDEX.md)

---

## ▌Peces principals

- `Scene`
- `SceneBuilder`
- `TextBlock`
- `BlockBuilder`

---

## ▌Escena

Una escena representa una unitat narrativa completa.

Pot decidir:

- si neteja la pantalla abans de començar
- si espera entrada de l'usuari al final
- quins blocs de text conté

```java
Scene escena = SceneBuilder.create()
        .clearBefore(true)
        .waitAtEnd(true)
        .blocks(bloc1, bloc2)
        .build();
```

---

## ▌Bloc de text

Un bloc representa una part concreta del text dins d'una escena.

Pot controlar:

- color inicial
- to inicial
- velocitat base
- variació humana
- wrap automàtic
- salt de línia al final

```java
TextBlock bloc = BlockBuilder
        .text("La porta es va obrir...")
        .color(CinematicColor.GRAY)
        .mood(TypingMood.DRAMATIC)
        .speed(28)
        .wrap(true)
        .build();
```

---

## ▌Construcció modular

Les escenes es poden declarar abans i carregar després a la cinemàtica.

```java
TextCinematic cinematic = TextCinematic.builder()
        .scenes(escenaIntro, escenaVeu, escenaFinal)
        .build();
```

---

## ▌Per què és important?

Això evita que una cinemàtica sigui una cadena de text gegant.

Cada escena pot tenir responsabilitat narrativa pròpia i cada bloc pot tenir estil, ritme o intenció diferent.

---

## ▌Relació amb el terminal

La neteja de pantalla hauria de dependre principalment de l'escena:

```java
.clearBefore(true)
```

Això és preferible a fer neteges dins del text, perquè manté la seqüència més clara i més fàcil de mantenir.
