# Combat RPG - [Jairo Linares](https://github.com/JJairo-16)

![Java Version](https://img.shields.io/badge/Java-21%2B-blue)
![License](https://img.shields.io/badge/License-MIT-green)

---

## ▌Què és?

**Combat RPG** és un joc de rol per torns (1vs1) on dos jugadors creen els seus personatges i s'enfronten en combat estratègic.

Aquest projecte es pot considerar un **remake/rework** de la seva primera versió, amb millores en l’arquitectura, el sistema de combat i una major flexibilitat en la personalització dels personatges.

Cada jugador personalitza el seu personatge repartint punts d'estadístiques, escollint raça i equipant armes amb habilitats úniques.

---

## ▌Sistema de Personatges

Cada personatge té:

- Nom
- Edat
- Vida
- Manà
- Raça
- Arma equipada
- 7 estadístiques principals

### ▌Estadístiques (7)

Els jugadors disposen de **140 punts** per repartir entre:

| Estadística    | Funció principal                            |
| -------------- | ------------------------------------------- |
| Força          | Dany físic i combat cos a cos               |
| Destresa       | Esquiva, precisió i combat a distancia      |
| Constitució    | Vida i resistència                          |
| Intel·ligència | Dany màgic i manà                           |
| Saviesa        | Control i percepció                         |
| Carisma        | Influència i efectes socials                |
| Sort           | Probabilitat de crítics i efectes especials |

---

## ▌Races (7)

Hi ha 7 races disponibles.
Cada raça ofereix una **bonificació específica** a una estadística concreta, fomentant diferents estils de joc.

Exemple orientatiu:

- Humà → Bonificació a Carisma
- Elf → Bonificació a Destresa
- Orc → Bonificació a Força
- Nan → Bonificació a Constitució
- etc.

---

## ▌Armes

Hi ha **3 tipus d’armes** diferents.

Característiques:

- Rang de dany propi
- Poden ser físiques, màgiques o de rang
- Cada arma té una habilitat única

Les habilitats poden incloure:

- "Ruleta rusa"
- Multiples atacs per torn
- Major benefici per major risc
- Pensament ràpid

---

## ▌Sistema de Combat

El combat és per torns:

1. Canviar arma (opcional)
2. Escollir acció:
   - Atacar (i utilitzar l'habilitat)
   - Defensar-se
   - Esquivar
3. Aplicació de regeneració automàtica

La partida finalitza quan un personatge arriba a 0 de vida.

### Etapes del combat (per pasives)

1. Inici del torn
2. Abans de l'atac
3. Modificar dany (durant l'atac)
4. Després de la defensa (defensar-se o esquivar)
5. Després d'atacar (després d'un impacte)
6. Final de torn

---

## ▌Mecàniques Destacades

- Sistema d’esquiva basat en Destresa
- Sistema de crítics influenciat per la Sort
- Regeneració de vida i manà per torn
- Diferenciació entre armes físiques, màgiques i de rang
- Escalat de dany segons estadístiques
- Afinitat divina per carisma

### ▌Crida espiritual

Quan un personatge baixa al **20% de vida o menys**, pot activar una crida espiritual (amb cooldown).

Aquesta mecànica permet:

- Llançar un dau de 20 cares (1–20)
- Curar-se en proporció directa al resultat
- No consumir el torn

Això introdueix un element de **remuntada inesperada**, on un combat aparentment perdut pot capgirar-se.

Característiques clau:

- Escala amb la vida màxima del personatge
- Influenciada indirectament per la Sort (opcional segons implementació)
- Limitada per cooldown per evitar abusos

És una mecànica pensada per aportar **tensió, risc i moments clutch** dins del combat.

### ▌Afinitat Divina

Els déus tenen una preferència oculta respecte al **carisma**.

A cada partida es genera un perfil que defineix:

- Un rang de carisma "afavorit"
- Zones neutrals
- Zones desfavorables

Distribució conceptual:

cau malament - normal - cau bé - normal - cau malament

Això provoca que dos personatges amb el mateix nivell puguin tenir resultats diferents en la tirada del dau de 20 cares segons com encaixin amb aquesta preferència divina.

---

## ▌Dependències

Aquest projecte utilitza les següents llibreries:

- **JUnit (junit-jupiter-engine, junit-platform-runner)**
  Utilitzat per a la creació i execució de tests.

- **Gson**
  Llibreria de Google per gestionar JSON (lectura i escriptura de configuracions i dades).

- **dynamic-menu**
  Llibreria pròpia utilitzada per gestionar menús dinàmics dins del joc.
  Disponible a: https://github.com/JJairo-16/dynamic-menu

- **Jline**
  Llibreria utilitzada per gestionar l’entrada interactiva per consola, especialment per al menú d’armes i accions durant combat.

---

## ▌Instal·lació

Abans d’executar el projecte, cal assegurar-se que les dependències (JARs) estan instal·lades al repositori local de Maven.

Aquest projecte inclou un script de PowerShell i un script de Bash que automatitzen aquest procés a partir del directori `libs`.

### Instal·lació de dependències (Windows)

Executa el següent script des de l’arrel del projecte:

```powershell
.\install-jars.ps1
```

El script:

- Llegeix els fitxers `.jar` dins de `libs/`
- Utilitza la configuració definida a `libs/artifacts.json`
- Instal·la automàticament cada dependència amb Maven

### Instal·lació de dependències (Linux / Mac)

```bash
./install-jars.sh
```

El script:

- Llegeix els fitxers `.jar` dins de `libs/`
- Utilitza la configuració definida a `libs/artifacts.json`
- Instal·la automàticament cada dependència amb Maven

### Opcional

Si vols que el procés s’aturi en cas d’error:

```powershell
.\install-jars.ps1 -StopOnError
```

```bash
./install-jars.sh --stop-on-error
```

---

## ▌Execució

Compilar i executar la classe principal:

```java
App.java
```

---

## ▌Llicència

Aquest projecte està sota la llicència [MIT](LICENSE).

---

## ▌Autor

Jairo Linares
GitHub: https://github.com/JJairo-16