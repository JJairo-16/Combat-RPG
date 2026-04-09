# `Arsenal` i `Skills`

## `Arsenal`

`Arsenal` ja no és un `enum`. Ara és un **catàleg dinàmic d'armes precarregat des de JSON**.

La classe manté dues estructures principals en memòria:

- `BY_ID`: mapa de definicions per `id`
- `SORTED`: llista ordenada per tipus i nom

També conserva:

- `namesList`: textos preparats per a menús
- `loaded`: indicador de precarrega

## Flux de càrrega

### `preload(Path jsonPath)`

Llig el fitxer JSON amb `WeaponLoader.loadDefinitions(...)` i delega en la sobrecàrrega que rep una col·lecció.

### `preload(Collection<WeaponDefinition> definitions)`

Fa la validació i la precarrega real:

1. comprova que hi haja definicions
2. valida que no hi haja `id` repetits
3. reconstrueix el mapa intern
4. ordena les armes per tipus i nom
5. genera les entrades de menú
6. marca el catàleg com a carregat

Si el catàleg no està carregat, els accessos fallen amb `IllegalStateException`.

## API pública d'`Arsenal`

### `values()`
Retorna una còpia immutable de les `WeaponDefinition` precarregades.

### `getDefinition(String id)`
Retorna la definició associada a un `id`.

### `create(String id)`
Crea una `Weapon` nova a partir d'una definició concreta.

### `getWeaponByIdx(int idx)`
Compatibilitat amb l'antic patró de menú per índex. No retorna la definició, sinó una arma nova ja construïda.

### `getNamesList()` i `getMenuEntries()`
Retornen textos preparats per mostrar al menú.

## Format de menú

`formatForMenu(...)` construïx una línia com aquesta:

```text
Nom - Descripció (Tipus: ... | Dany: ... | Crit: ... | Mult: ... | Mana: ...)
```

El camp de mana només s'afig si `manaPrice > 0`.

## Relació amb `WeaponLoader` i `WeaponDefinition`

La informació real de les armes viu en JSON i es transforma així:

1. `WeaponLoader` deserialitza `WeaponConfig[]`
2. cada `WeaponConfig` es transforma en `WeaponDefinition`
3. `WeaponDefinition.create()` resol la skill i construïx les passives
4. finalment crea una instància nova de `Weapon`

Això vol dir que el sistema actual és **data-driven**: afegir una arma nova implica principalment afegir o modificar configuració JSON, sempre que la skill i les passives existisquen al registre.

## Registre de skills

`AttackRegistry` resol el nom textual d'una skill a la seua implementació real.

Skills registrades actualment:

- `nothing`
- `explosiveShot`
- `arcaneDisruption`
- `luckyBallista`
- `grimoriCipher`
- `perforatingThrow`
- `chronoWeave`
- `crossCut`

Si arriba un identificador desconegut, es llança `IllegalArgumentException`.

## Skills actuals

### `nothing`
No aplica cap lògica especial. Retorna `weapon.basicAttackWithMessage(stats, rng)`.

### `explosiveShot`
Dispar explosiu amb risc d'autocolp.

Comportament:

- calcula el dany base amb `basicAttack(...)`
- la probabilitat d'autodispar depén de la sort
- aquesta probabilitat queda limitada entre `0.08` i `0.22`
- si impacta l'enemic, aplica multiplicador `1.10`
- si impacta a un mateix, aplica multiplicador `0.50` i canvia el `target` a `SELF`
- el missatge canvia també si hi havia crític

### `crossCut`
Executa **dos atacs bàsics complets** i suma el dany.

Detalls importants:

- cada iteració torna a cridar `weapon.basicAttack(...)`
- compta quants d'aquests atacs han sigut crítics
- el missatge final mostra el percentatge de crítics sobre 2 atacs
- no construïx dos `HitContext`; només torna un únic `AttackResult`

### `arcaneDisruption`
Skill màgica amb consum de mana i probabilitat de fallada.

Flux actual del codi:

1. comprova si hi ha prou mana amb `consumeMana(baseManaCost)`
2. immediatament després torna a cridar `consumeMana(baseManaCost)`
3. calcula una probabilitat de fallada depenent de la sort
4. si falla, retorna `0` de dany
5. si té èxit, fa `basicAttack(...)`
6. si és crític, consumeix un `50%` extra de mana

Particularitat rellevant:

- en el camí d'èxit hi ha **doble consum base de mana**
- si a més hi ha crític, hi ha consum addicional del `50%`

La probabilitat de fallada es limita entre `0.22` i `0.30`.

### `luckyBallista`
Genera entre 1 i 4 projectils consecutius.

Funcionament:

- probabilitat inicial de continuar: `0.50 + luck * 0.005`
- aquesta probabilitat queda limitada entre `0.40` i `0.75`
- a partir del segon tir, cada iteració pot tallar la cadena
- cada tret reduïx la probabilitat futura en `0.18`
- cada tret reduïx també el multiplicador de dany en `0.15`
- el multiplicador de dany no baixa de `0.40`
- un crític incrementa lleument la probabilitat de continuar en `0.05`

El resultat final:

- arrodonix el dany total a 2 decimals
- adapta el missatge segons si hi ha hagut 1, 2-3 o 4 projectils
- afig el nombre de crítics si n'hi ha hagut

### `grimoriCipher`
És una skill interactiva amb minijoc de mecanografia.

Flux real:

1. comprova mana i el consumeix una sola vegada
2. genera un codi amb `GrimoriCodeGenerator`
3. mostra una interfície textual a consola
4. espera l'entrada de l'usuari
5. mesura el temps de resposta amb `System.nanoTime()`
6. calcula un multiplicador segons temps i destresa
7. penalitza més si el text és incorrecte
8. aplica `basicAttack(...)`
9. multiplica el dany final i l'arrodonix

Detalls clau:

- la destresa dona un marge addicional anomenat `dexGrace`
- el multiplicador normal queda limitat entre `0.72` i `1.25`
- si l'usuari falla el codi, el multiplicador es reduïx i queda limitat entre `0.55` i `1.25`
- el missatge indica temps, multiplicador i si hi havia crític

És la skill més dependent de la UI de consola dins d'aquest paquet.

### `perforatingThrow`
Atac de penetració consistent.

Comportament:

- partix del resultat de `basicAttack(...)`
- aplica multiplicador base `1.18`
- la destresa suma fins a `0.10` extra
- un crític suma `0.10` addicional
- arrodonix el resultat final a 2 decimals

No ignora defensa de manera directa en aquest paquet; el concepte de penetració està modelat com un multiplicador de dany abans que el motor resolga la defensa.

### `chronoWeave`
Simula tres futurs possibles i en tria un.

Flux:

1. genera 3 atacs bàsics independents
2. guarda tant el dany com si cada simulació era crítica
3. ordena els resultats de menor a major dany
4. calcula la probabilitat d'agafar millor o pitjor resultat segons intel·ligència i sort
5. tria un índex final
6. retorna el dany i el missatge corresponent

Probabilitats:

- `bestChance = 0.20 + intelligence * 0.005 + luck * 0.003`, limitat entre `0.20` i `0.60`
- `worstChance = 0.30 - intelligence * 0.004`, limitat entre `0.10` i `0.30`

Particularitat important del codi actual:

- després d'ordenar de menor a major, l'índex `0` és el pitjor resultat i l'índex `2` és el millor
- els missatges del `switch` són coherents amb això
- però els comentaris interns `// ++`, `// ===`, `// --` poden confondre perquè no descriuen clarament la mateixa orientació

## Patró general per afegir una skill nova

En aquest codi, una skill nova acostuma a seguir aquest patró:

1. rebre `Weapon`, `Statistics` i `Random`
2. gastar recursos si cal
3. usar `weapon.basicAttack(...)` o una variació pròpia
4. transformar el dany o l'objectiu
5. retornar un `AttackResult`
6. registrar la skill textualment a `AttackRegistry`
7. referenciar-la des del JSON d'armes
