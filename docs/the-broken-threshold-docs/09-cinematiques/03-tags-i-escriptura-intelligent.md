# ▌Tags i escriptura intel·ligent

[← Tornar a l'índex](../INDEX.md)

---

## ▌Peces principals

- `MarkupParser`
- `MarkupToken`
- `TypingAnalyzer`
- `TypingMood`
- `TypingStyle`
- `TypingAction`

---

## ▌Objectiu

El sistema combina dues fonts de decisió:

- anàlisi automàtica del text
- tags manuals dins del bloc

Això fa que l'escriptura sigui més natural, però encara controlable quan una escena necessita direcció concreta.

---

## ▌Tags de rang

Els tags de rang modifiquen el text que contenen.

```text
<slow>...</slow>
<fast>...</fast>
<urgent>...</urgent>
<tense>...</tense>
<dramatic>...</dramatic>
<whisper>...</whisper>
```

També es poden usar colors:

```text
<red>No miris enrere.</red>
<gray>La nit era freda...</gray>
<cyan>Una veu va sonar.</cyan>
```

---

## ▌Tags instantanis

Els tags instantanis no imprimeixen text; generen una acció.

```text
<pause:500>
<br>
<reset>
<speed:fast>
<speed:slow>
<mood:tense>
<color:red>
```

---

## ▌Exemple

```java
TextBlock bloc = BlockBuilder
        .text("""
                <gray>La sala estava buida...</gray>
                <pause:300>
                <red><slow>O això semblava.</slow></red>
                """)
        .build();
```

---

## ▌Escriptura humana

`TypingAnalyzer` calcula retards segons:

- puntuació
- punts suspensius
- frases curtes
- majúscules
- interrogacions
- exclamacions
- paraules llargues
- revelacions narratives

---

## ▌Punts suspensius

Els punts suspensius no es tracten com tres punts normals.

El sistema pot fer que els primers punts flueixin ràpid i que l'últim deixi una pausa més marcada.

```text
La porta es va obrir...
```

Això evita una escriptura robòtica del tipus:

```text
. pausa . pausa . pausa
```

---

## ▌Prioritat

Ordre recomanat de decisió:

1. tags explícits
2. to actual (`TypingMood`)
3. patrons detectats automàticament
4. variació aleatòria humana

---

## ▌Quan tocar aquest sistema

- si vols afegir un tag nou
- si vols modificar el ritme natural
- si vols canviar com es detecten frases tenses o dramàtiques
- si vols millorar la interpretació del text narratiu
