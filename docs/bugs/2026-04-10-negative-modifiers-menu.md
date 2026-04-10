# Informe de proves i errors

## Informació general
- Projecte: RPG Combat
- Mòdul / Funcionalitat: MenuStatusModifier (Modificador de les opcions del menú segons efectes)
- Data: 10-04-2026
- Autor: Jairo Linares

---

## Casos de prova

### Cas de prova 1
- Objectiu: Comprova que el JSON es llegeix i es transforma correctament.
- Condicions / Entrada: Fitxer `"/status-modifiers-test.json"`
- Resultat esperat: JSON carregat correctament com a `Map<String, List<StatusMod>>` amb claus "BURN" i "FREEZE".
- Resultat obtingut: JSON carregat correctament amb les claus esperades i contingut vàlid.

---

### Cas de prova 2
- Objectiu: Comprova que el menú es modifica segons la configuració del JSON.
- Condicions / Entrada: Menú base + jugador amb efectes `BURN` i `FREEZE`.
- Resultat esperat: Increment del nombre d’opcions del menú segons els modificadors aplicables.
- Resultat obtingut: El menú incrementa correctament el nombre d’opcions segons els modificadors.

---

### Cas de prova 3
- Objectiu: Comprova que no es dupliquen opcions en reconstruir el menú.
- Condicions / Entrada: Aplicar `modifier.mod("base")` dues vegades amb els mateixos efectes actius.
- Resultat esperat: El nombre d’opcions es manté constant després de la segona execució.
- Resultat obtingut: No es dupliquen opcions; el recompte es manté estable.

---

### Cas de prova 4
- Objectiu: Comprova que s’ignoren els efectes expirats.
- Condicions / Entrada: Efectes amb `remainingTurns = 0` (expirats).
- Resultat esperat: No s’aplica cap modificador al menú.
- Resultat obtingut: El menú no canvia i es manté en estat base.

---

### Cas de prova 5
- Objectiu: Comprova que només s’apliquen els modificadors que compleixen els rangs definits al JSON.
- Condicions / Entrada: Efecte `BURN` amb valors dins d’un únic rang vàlid.
- Resultat esperat: Només s’aplica 1 modificador.
- Resultat obtingut: S’aplica exactament 1 modificador i el menú s’actualitza correctament.

---

### Cas de prova 6
- Objectiu: Comprova el cicle d’aparició i desaparició d'efectes.
- Condicions / Entrada: 
  1) Jugador sense efectes → aplicar `modifier.mod("base")`  
  2) Afegir efecte `BURN` actiu (remainingTurns > 0) → aplicar `modifier.mod("base")`  
  3) Eliminar efectes (`player.clearEffects()`) → aplicar `modifier.mod("base")`  
- Resultat esperat:
  1) El menú es manté amb el nombre base d’opcions  
  2) El menú augmenta amb els modificadors corresponents a l’efecte  
  3) El menú torna al nombre base d’opcions (sense modificadors)  
- Resultat obtingut:
  1) El menú es manté amb el nombre base d’opcions  
  2) El menú augmenta amb els modificadors corresponents a l’efecte  
  3) El menú no torna al nombre base d'opcions (manté el modificador)

---

## Resultats d'execució
| Cas | Estat (Correcte / Error) | Observacions                                                                                                     |
| --- | ------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| 1   | Correcte                 | JSON carregat sense errors                                                                                       |
| 2   | Correcte                 | Modificació del menú correcta                                                                                    |
| 3   | Correcte                 | Sense duplicació d’opcions                                                                                       |
| 4   | Correcte                 | Efectes expirats ignorats                                                                                        |
| 5   | Correcte                 | Aplicació correcta de rangs                                                                                      |
| 6   | Error                    | El sistema detecta quan s'ha d'afegir modificadors però no quan eliminar en cas de no haver efectes o modificadors |

---

## Incidències detectades

### Incidència 1
- Tipus: Comportament inesperat
- Descripció: El sistema detecta quan s'ha d'afegir modificadors però no quan eliminar en cas de no haver efectes o modificadors.
- Com s’ha detectat: S'ha detectat durant el disseny i execució de proves unitàries.
- Causa probable: Els early returns no permeten restaurar o actualitzar l'estat del menú.
- Solució aplicada: S'ha implementat una caché que guarda l'última quantitat de modificadors per aplicar una restauració en cas de haver modificadors prèviament.

---

### Incidència 2
- Tipus: Millora
- Descripció: El sistema reconstrueix el menú inclús quan no hi ha modificacions, la qual cosa pot causar micro-tirons.
- Com s’ha detectat: S'ha detectat mentre es mitigava la incidència anterior.
- Causa probable: ---
- Solució aplicada: Creació de hash i caché d'aquest per detectar eficientment canvis reals.

---

## Tècniques de depuració utilitzades

### Logs / traces / prints
- Descripció: L'utilització de Junit i provés unitaries.
- Exemple de sortida: `The test case did not report any output.`