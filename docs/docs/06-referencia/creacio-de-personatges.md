```md
# Creació de personatges

## `CharacterCreator`

És la peça que construeix personatges nous per al joc real i per al mode debug.

## Modes disponibles

### `createNewCharacter()`
Flux interactiu complet.

### `createDebugCharacter()`
Genera noms `test1`, `test2`, etc., edat 12 i stats automàtiques.

## Generació automàtica

Fa servir:

```java
StatsBudget.generate(TOTAL_POINTS)
```

Retorna:

- `baseStats()`
- `breed()`

## Generació manual

Flux:

1. l'usuari tria raça
2. reparteix exactament 140 punts
3. cada estadística té mínims
4. constitució té un mínim especial una mica més alt
5. si la suma no és exacta, es torna a demanar

## Conversió final a subclasse concreta

`CharacterCreator` no sempre crea un `Character` base. Segons la raça, pot instanciar:

- `Orc`
- `Elf`
- `Dwarf`
- `Gnome`
- `Tiefling`
- `Halfling`

Això és clau perquè els efectes especials de raça depenen de la subclasse concreta, no només del `Breed`.

## Implicació per desenvolupament

Quan afegeixis una raça nova, has de revisar també `CharacterCreator`, perquè si no hi afegeixes el cas corresponent, la raça pot quedar sense la seva lògica específica.
```
