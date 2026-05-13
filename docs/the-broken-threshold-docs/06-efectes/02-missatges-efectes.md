# ▌Missatges d'efectes

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

Els missatges d'efectes no s'han de construir amb tags de text, codis ANSI ni símbols escrits manualment.

El sistema ha de guardar la informació del missatge de forma estructurada:

- símbol
- color
- text
- origen del missatge

Això evita errors visuals i fa que el renderitzat sigui responsabilitat d'una única capa.

---

## ▌Problema anterior

Abans alguns efectes generaven missatges així:

```java
return "[RED|!]" + owner.getName() + " pateix dany per verí.";
```

O així:

```java
return "[35m] La moneda caòtica cau de creu.";
```

Aquest enfocament és fràgil perquè:

- el color depèn d'escriure bé el tag
- el símbol queda barrejat amb el text
- cada classe pot formatar diferent
- és fàcil trencar l'alineació del combat
- el render ha de parsejar text que ja hauria d'estar classificat

---

## ▌Model nou

Els efectes han de retornar un `CombatMessage`.

```java
return EffectResult.styled(
        MessageColor.RED,
        MessageSymbol.WARNING,
        owner.getName() + " pateix dany per verí."
);
```

El missatge ja no diu com s'ha de pintar dins del text. Ho expressa amb dades.

---

## ▌Classes principals

Ubicació recomanada:

`combat/ui/messages/`

Classes:

- `CombatMessage`
- `CombatMessageBuffer`
- `CombatMessageFormatter`
- `MessageColor`
- `MessageSymbol`

---

## ▌CombatMessage

Representa una línia de missatge del combat.

Ha de contenir, com a mínim:

- `MessageSymbol symbol`
- `MessageColor color`
- `String text`

Exemple d'ús:

```java
CombatMessage.of(
        MessageSymbol.NEGATIVE,
        MessageColor.YELLOW,
        "La fatiga redueix el dany."
);
```

---

## ▌MessageSymbol

Defineix el significat visual del missatge.

Símbols recomanats:

| Símbol | Ús |
|---|---|
| `POSITIVE` | beneficis, curacions, millores |
| `NEGATIVE` | penalitzacions, pèrdues, debilitats |
| `WARNING` | avisos importants, efectes crítics |
| `CHAOS` | efectes caòtics o inestables |
| `INFO` | informació neutra |
| `HIT` | impactes confirmats |
| `WEAPON` | missatges narratius d'arma |

Exemple:

```java
MessageSymbol.NEGATIVE
```

No s'ha d'escriure `"-"` directament dins del text.

---

## ▌MessageColor

Defineix el color del missatge.

Colors recomanats:

| Color | Ús |
|---|---|
| `WHITE` | informació neutra |
| `GREEN` | efectes positius |
| `RED` | dany, errors, penalitzacions greus |
| `YELLOW` | avisos o penalitzacions lleus |
| `MAGENTA` | caos, efectes especials o inestables |
| `CYAN` | informació tècnica o màgica |

Exemple:

```java
MessageColor.MAGENTA
```

No s'han d'utilitzar codis com `[35m]`, `[RED|-]` o `Ansi.RED`.

---

## ▌EffectResult

Els efectes han de retornar missatges estructurats.

Exemple recomanat:

```java
return EffectResult.styled(
        MessageColor.RED,
        MessageSymbol.WARNING,
        owner.getName() + " pateix " + appliedDamage + " de dany per verí letal."
);
```

També es poden usar constructors semàntics:

```java
return EffectResult.negative(owner.getName() + " perd força.");
```

---

## ▌CombatMessageBuffer

Serveix per acumular missatges durant la resolució d'un torn.

Exemple:

```java
out.styled(
        MessageColor.MAGENTA,
        MessageSymbol.CHAOS,
        "Caos s'activa sobre " + owner.getName() + "."
);
```

El buffer evita haver de passar `List<String>` per tot el sistema.

---

## ▌Regla general

No s'han de crear missatges nous amb aquest format:

```java
"[RED|-] text"
"[35m] text"
Ansi.RED + text
"+ text"
"- text"
"! text"
"? text"
```

S'ha d'utilitzar sempre:

```java
CombatMessage.of(symbol, color, text)
```

o bé:

```java
out.styled(color, symbol, text)
```

---

## ▌Exemples de migració

### Dany constant

Abans:

```java
protected String buildMessage(double appliedDamage, Character owner) {
    return "[RED|-]" + owner.getName() + " rep dany per efecte.";
}
```

Després:

```java
protected CombatMessage buildMessage(double appliedDamage, Character owner) {
    return CombatMessage.of(
            MessageSymbol.NEGATIVE,
            MessageColor.RED,
            owner.getName() + " rep " + appliedDamage + " de dany per efecte."
    );
}
```

---

### Verí letal

Abans:

```java
return "[RED|!]" + owner.getName() + " pateix dany per verí letal.";
```

Després:

```java
return CombatMessage.of(
        MessageSymbol.WARNING,
        MessageColor.RED,
        owner.getName() + " pateix dany per verí letal."
);
```

---

### Fatiga

Abans:

```java
return "[YELLOW|-] La fatiga rebaixa una mica el dany.";
```

Després:

```java
return CombatMessage.of(
        MessageSymbol.NEGATIVE,
        MessageColor.YELLOW,
        "La fatiga rebaixa una mica el dany."
);
```

---

### Caos

Abans:

```java
return "[35m]? Caos s'activa.";
```

Després:

```java
return CombatMessage.of(
        MessageSymbol.CHAOS,
        MessageColor.MAGENTA,
        "Caos s'activa."
);
```

---

## ▌Missatges d'arma

Els missatges narratius d'arma també poden usar el mateix sistema.

Exemple:

```java
out.weapon("La fulla xiula tallant l'aire.");
```

Render esperat:

```text
» “La fulla xiula tallant l'aire.”
```

Aquests missatges no són efectes mecànics, però sí formen part del relat del combat.

---

## ▌Renderitzat

Només `CombatMessageFormatter` hauria de decidir:

- quin caràcter mostra cada símbol
- quin codi ANSI correspon a cada color
- com es reinicia el color
- com es calcula la longitud visible

Això evita que els efectes coneguin detalls del terminal.

---

## ▌Beneficis

Aquest sistema permet:

- eliminar parsing de tags
- evitar colors trencats
- mantenir alineació correcta
- unificar missatges d'efectes i passives
- separar lògica de combat i renderitzat
- facilitar nous estils visuals en el futur

---

## ▌Recomanació

Qualsevol classe que generi missatges de combat hauria de migrar progressivament a `CombatMessage`.

Prioritat recomanada:

1. efectes
2. triggers
3. passives d'arma
4. missatges base d'atac
5. renderitzat final

Quan la migració estigui completa, no hauria de quedar cap missatge nou amb tags manuals ni ANSI dins del model de combat.