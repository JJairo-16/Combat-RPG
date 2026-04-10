# Informe de proves i errors

## Informació general
- Projecte: RPG Combat
- Mòdul / Funcionalitat: SpiritualCallingFlag / MenuStatusModifier
- Data: 10-04-2026
- Autor: Jairo Linares

---

## Casos de prova

### Cas de prova 1
- Objectiu: Comprova que el menú es manté en l’estat base quan la vida està per sobre del llindar d’activació.
- Condicions / Entrada: Jugador amb `SpiritualCallingFlag` actiu i vida al `21%`.
- Resultat esperat: El menú manté 5 opcions.
- Resultat obtingut: El menú manté 5 opcions.

---

### Cas de prova 2
- Objectiu: Comprova que s’afegeix l’opció d’invocar esperits quan la vida baixa per sota del llindar.
- Condicions / Entrada: Jugador amb `SpiritualCallingFlag` actiu i vida al `15%`.
- Resultat esperat: El menú passa de 5 a 6 opcions.
- Resultat obtingut: El menú passa correctament de 5 a 6 opcions.

---

### Cas de prova 3
- Objectiu: Comprova que l’opció s’activa exactament al llindar permès.
- Condicions / Entrada: Jugador amb `SpiritualCallingFlag` actiu i vida al `20%`.
- Resultat esperat: El menú passa a tenir 6 opcions.
- Resultat obtingut: El menú mostra correctament 6 opcions.

---

### Cas de prova 4
- Objectiu: Comprova el cicle d’aparició i desaparició de l’opció segons la vida.
- Condicions / Entrada:
  1) Jugador amb `SpiritualCallingFlag` actiu i vida inicial normal  
  2) Reducció de vida al `15%`  
  3) Recuperació de vida al `100%`
- Resultat esperat:
  1) El menú comença amb 5 opcions  
  2) El menú passa a 6 opcions  
  3) El menú torna a 5 opcions
- Resultat obtingut:
  1) El menú comença amb 5 opcions  
  2) El menú passa a 6 opcions  
  3) El menú torna correctament a 5 opcions

---

### Cas de prova 5
- Objectiu: Comprova que sense l’efecte `SpiritualCallingFlag` no s’afegeix cap opció extra.
- Condicions / Entrada: Jugador sense efectes i vida al `10%`.
- Resultat esperat: El menú es manté amb 5 opcions.
- Resultat obtingut: El menú es manté correctament amb 5 opcions.

---

### Cas de prova 6
- Objectiu: Comprova que l’acció `spiritualCalling` aplica el cooldown, elimina temporalment l’opció i cura el personatge.
- Condicions / Entrada:
  1) Jugador amb `SpiritualCallingFlag` actiu i vida al `10%`
  2) Execució de `Actions.spiritualCalling(dummy)`
- Resultat esperat:
  1) Abans de l’acció, el menú té 6 opcions  
  2) Després de l’acció, el cooldown queda a `3`  
  3) El menú torna a 5 opcions  
  4) La vida no disminueix
- Resultat obtingut:
  1) El menú tenia 6 opcions abans de l’acció  
  2) El cooldown queda correctament a `3`  
  3) El menú torna a 5 opcions  
  4) La vida es manté o augmenta correctament

---

### Cas de prova 7
- Objectiu: Comprova que l’opció no es mostra mentre el cooldown està actiu, encara que la vida continuï baixa.
- Condicions / Entrada:
  1) Jugador amb `SpiritualCallingFlag` actiu i vida al `10%`
  2) Execució de `Actions.spiritualCalling(dummy)`
  3) Vida mantinguda al `10%`
- Resultat esperat: El menú es manté amb 5 opcions mentre el cooldown sigui major que 0.
- Resultat obtingut: El menú es manté correctament amb 5 opcions.

---

### Cas de prova 8
- Objectiu: Comprova que l’opció torna a aparèixer quan finalitza el cooldown i la vida continua baixa.
- Condicions / Entrada:
  1) Jugador amb `SpiritualCallingFlag` actiu i vida al `10%`
  2) Execució de `Actions.spiritualCalling(dummy)`
  3) Reducció del cooldown fins a `0`
  4) Vida mantinguda al `10%`
- Resultat esperat:
  1) El cooldown arriba a `0`  
  2) El menú torna a mostrar 6 opcions
- Resultat obtingut:
  1) El cooldown arriba a `0`  
  2) El menú no torna a mostrar 6 opcions

---

### Cas de prova 9
- Objectiu: Comprova que un tick de joc no altera el menú si les condicions de l’efecte es mantenen.
- Condicions / Entrada:
  1) Jugador amb `SpiritualCallingFlag` actiu i vida al `10%`
  2) El menú mostra l’opció d’invocar esperits (6 opcions)
  3) Execució d’un tick (`tickEffects()` o equivalent)
- Resultat esperat: El menú es manté amb 6 opcions, ja que l’efecte continua actiu i les condicions no han canviat.
- Resultat obtingut: El menú es manté amb 6 opcions després del tick.

---

## Resultats d'execució

| Cas | Estat (Correcte / Error) | Observacions                                                   |
| --- | ------------------------ | -------------------------------------------------------------- |
| 1   | Correcte                 | El menú no afegeix opcions per sobre del llindar               |
| 2   | Correcte                 | L’opció s’afegeix quan la vida baixa per sota del llindar      |
| 3   | Correcte                 | El llindar exacte activa correctament l’opció                  |
| 4   | Correcte                 | El cicle d’aparició i desaparició funciona correctament        |
| 5   | Correcte                 | Sense l’efecte, no apareix cap opció addicional                |
| 6   | Correcte                 | L’acció aplica cooldown, elimina temporalment l’opció i cura   |
| 7   | Correcte                 | Durant el cooldown, l’opció no torna a aparèixer               |
| 8   | Correcte                 | En acabar el cooldown, l’opció torna a estar disponible        |
| 9   | Correcte                 | L'opció no es veu afectada pel pas de temps al no se consumida |

---

## Incidències detectades

### Incidència 1
- Tipus: Comportament inesperat.
- Descripció: El sistema no invàlida la cache al eliminar la opció afegida manualment.
- Com s’ha detectat: S'ha detectat durant l'execució de proves unitàries.
- Causa probable: Interacció forçada amb el menú inhabilita la capacitat d'actualització correcte de la cache.
- Solució aplicada: Eliminar l'interacció forçada del menú.

---

## Tècniques de depuració utilitzades

### Proves unitàries
- Descripció: S’han executat proves unitàries amb JUnit per validar l’activació, desactivació i reaparició de l’opció dinàmica associada a `SpiritualCallingFlag`.
- Exemple de sortida: `Tests run finished after ... ms`