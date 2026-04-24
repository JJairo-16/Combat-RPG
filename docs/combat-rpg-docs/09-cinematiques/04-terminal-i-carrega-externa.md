# ▌Terminal i càrrega externa

[← Tornar a l'índex](../INDEX.md)

---

## ▌Peces principals

- `SharedTerminal`
- `TerminalSession`
- `TerminalController`
- `CinematicInput`
- `CinematicFileLoader` *(proposat)*

---

## ▌Terminal compartit

Les cinemàtiques no haurien de crear un terminal nou cada vegada.

El projecte pot compartir una sola instància de JLine entre:

- menús
- cinemàtiques
- selectors interactius

```java
TextCinematic.preloadTerminal();
```

---

## ▌Sessió temporal

Cada sistema interactiu hauria d'obrir una sessió sobre el terminal compartit.

La sessió s'encarrega de restaurar:

- mode raw
- pantalla alternativa
- keypad
- cursor
- atributs originals

```java
try (TerminalSession session = SharedTerminal.openSession()) {
    Terminal terminal = session.terminal();
    // ús interactiu
}
```

---

## ▌Entrada de teclat

`CinematicInput` evita que la cinemàtica depengui directament de caràcters crus del terminal.

La cinemàtica només necessita accions simples:

- avançar
- completar escena
- saltar seqüència

---

## ▌Càrrega des de fitxer

Una evolució natural és carregar cinemàtiques des d'un fitxer extern amb estructura similar a XML.

```java
TextCinematic intro = CinematicFileLoader.load("rpg/data/cinematics/intro.cinematic.xml");
intro.play();
```

---

## ▌Format proposat

```xml
<cinematic clearScreenOnEnd="true" arrowDelay="140">
    <scene clearBefore="true" waitAtEnd="true">
        <block color="gray" mood="dramatic" speed="32"><![CDATA[
            La nit queia sobre la ciutat...
            <pause:300>
            Els fanals parpellejaven.
        ]]></block>
    </scene>
</cinematic>
```

---

## ▌Per què CDATA?

Els tags narratius interns com `<pause:300>` o `<slow>...</slow>` no són XML pur en tots els casos.

Per això és millor que el fitxer faci servir:

- XML per a l'estructura externa
- CDATA per al text cinematogràfic intern

---

## ▌Lectura arquitectònica

La càrrega externa no hauria de substituir el sistema de builders.

Hauria de construir exactament les mateixes peces:

```text
fitxer extern
  ↓
CinematicFileLoader
  ↓
TextCinematic.builder()
  ↓
SceneBuilder
  ↓
BlockBuilder
```

---

## ▌Quan tocar aquest paquet

- si vols afegir cinemàtiques sense recompilar
- si vols moure narrativa a fitxers externs
- si vols validar estructura de cinemàtiques
- si vols connectar-les a rutes de configuració
