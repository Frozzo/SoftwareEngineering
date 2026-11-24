# CLAUDE.md — UnoLegends

Progetto universitario di Ingegneria del Software: **UnoLegends**, variante di UNO in Java
giocabile da CLI (hotseat, 2 giocatori). Il progetto segue il processo UP a iterazioni
e i principi GRASP/GoF: il design conta quanto il codice funzionante.

## Lingua e convenzioni

- Codice, commenti, documentazione e commit sono **in italiano**. Rispondere in italiano.
- Nomi di classi e metodi in italiano (`giocaCarta`, `pescaCarta`, `PilaDegliScarti`).
- I nomi delle classi effetto sono incoerenti (`Effetto_Blocca_Turno`, `Effetto_cambio_giro`,
  `Effetto_pescata_carte`, `EffettoComposito`): non rinominarli senza che venga chiesto.
- Ogni classe/metodo importante ha un Javadoc che spiega il **perché** e il ruolo GRASP/GoF.
  Mantenere questo stile quando si aggiunge codice.

## Struttura del repository

```
SoftwareEngineering/
├── README.md                       # come avviare la demo CLI
├── *.docx / *.doc                  # artefatti UP: Vision, Glossary, Use Case, Supplementary Spec
└── Codice/
    ├── README.md                   # documentazione tecnica dettagliata (lunga, ~65KB)
    ├── GiocaTurno_SequenceDiagram.puml
    ├── docs/
    │   ├── domain_class_diagram.puml
    │   ├── roles_and_patterns.md            # mappa classi → pattern (in parte datata)
    │   └── refactoring_e_update_soluzioni.md # roadmap/proposte di refactoring futuro
    └── src/main/java/unolegends/
        ├── ui/          UnoLegendsCli               (View)
        ├── controller/  UnoLegendsGame + *Dto       (Facade/Controller GRASP)
        ├── model/       Partita, carte, effetti, mazzo, factory
        └── domain/rules/ IStrategia* + impl/        (Strategy)
```

## Build ed esecuzione

Nessun build tool (né Maven né Gradle) e **nessun test** al momento. Da `Codice/` in PowerShell:

```powershell
javac -d out (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
java -cp out unolegends.ui.UnoLegendsCli
```

JDK installato: 25. Il codice usa switch expression (serve Java 14+).
Per verificare una modifica, ricompilare con il comando sopra; `out/` è un output di build.

## Architettura (MVC + GRASP)

**Regola fondamentale: la UI non importa mai classi di `unolegends.model`.**
Comunica solo con `UnoLegendsGame` e riceve DTO immutabili (`StatoPartitaDto`,
`EsitoGiocataDto`, `RichiestaInputDto`). Il controller converte model → DTO
(es. `formattaCarta`). Ogni nuovo dato da mostrare alla UI passa da un DTO.

Flusso: `UnoLegendsCli` → `UnoLegendsGame` (Facade) → `Partita` (Coordinator) → expert
(`Giocatore`, `Mazzo`, `PilaDegliScarti`, `RegoleDiGioco`, `Ieffetto`).

### Classi chiave

- **`Partita`** — radice del dominio e coordinatore del turno. Stato: giocatori, indice attivo,
  `coloreAttuale`, `sensoOrario`, `saltaProssimoTurno`, carta appena pescata, effetto iniziale
  in attesa, giocatore in attesa di dichiarazione UNO, vincitore.
  - `giocaCarta(indice, risposta)` valida **tutto prima** di modificare lo stato (partita finita,
    indice, compatibilità, input dell'effetto), poi sposta la carta, applica l'effetto, controlla
    vittoria e finestra UNO, e infine avanza il turno. Restituisce `EsitoGiocata`
    (`completata` / `inAttesa(richiesta)` / `richiedeDichiarazioneUno`).
  - L'avanzamento del turno è centralizzato in `aggiornaGiocatoreAttivo()` (gestisce salto e senso).
  - Metodi package-private (`applicaColoreScelto`, `segnalaSaltoTurno`) sono usati dagli effetti
    nello stesso package `model`.
- **`RegoleDiGioco`** — contenitore delle strategie (punto di variazione protetto). `standard()`
  crea il profilo di default. Strategie: vittoria, sconfitta, compatibilità, pesca, penalità UNO.
- **`PartitaFactory`** — crea la partita: mazzo da `MazzoFactory`, 7 carte a testa, prima carta
  sugli scarti. Se la prima carta richiede input inizia il giocatore 0.
- **`MazzoFactory` / `StandardMazzoFactory`** — Abstract Factory + Singleton. Mazzo UNO standard
  da 108 carte (numeri, +2, blocca, cambia giro, cambia colore, +4).
- **Carte** — `Carta` (astratta) → `CartaNumero`, `CartaSpeciale`. Ogni carta ha un `Ieffetto`
  (le numeriche usano `Effetto_Nullo`, Null Object). `Colore` include `NERO` per le jolly e
  gestisce la colorazione ANSI. `ValoreCarta` è l'enum dei valori.
- **Effetti (`Ieffetto`)** — Strategy/Command. Un effetto che richiede input espone
  `getRichiestaInput()` e `validaRispostaInput()`, e viene attivato con `attivaEffetto(partita, risposta)`.
  `EffettoComposito` (Composite) combina effetti: es. +4 = cambia colore + pesca 4.

### Meccaniche da ricordare

- Le carte nere non cambiano `coloreAttuale` da sole: lo imposta l'effetto con il colore scelto.
  La UI mostra la carta in cima con il colore scelto (`CartaSpeciale.getDescrizioneBreve(Colore)`).
- Interazione in due passi: prima `giocaCarta(i, null)`; se l'esito è "in attesa" la UI chiede
  l'input e richiama `giocaCarta(i, risposta)`.
- Dopo una pesca volontaria si può giocare solo la carta pescata o passare (`passaTurno`).
- Giocando la penultima carta si apre la finestra UNO: `completaDichiarazioneUno(bool)`;
  se non dichiarato si pescano 2 carte di penalità.
- Mazzo vuoto → rimescolamento dagli scarti (la carta in cima resta).
- **Tutte le pesche forzate** (+2, +4, penalità UNO, e in futuro abilità e stacking) passano da
  `Partita.applicaPescaForzata(bersaglio, n, OriginePesca)`, che chiede a
  `RegoleDiGioco.calcolaCartePescaForzata(ContestoPesca)` quante carte pescare davvero.
  Non scrivere cicli di `pescaSingola` negli effetti.
- La pesca **volontaria** passa da `RegoleDiGioco.pescaVolontaria` (`IStrategiaPesca`).
- Gli effetti ricevono `Partita` anche in `getRichiestaInput` e `validaRispostaInput`.

## Documentazione da tenere allineata

Quando si modifica il design, controllare se vanno aggiornati: `Codice/README.md`,
`Codice/docs/roles_and_patterns.md`, i diagrammi `.puml`. Nota: `roles_and_patterns.md` contiene
dettagli superati (parla di 5 carte a testa e di `RegoleStandard`, oggi sono 7 carte e `RegoleDiGioco`).

`Codice/docs/refactoring_e_update_soluzioni.md` contiene proposte di design per sviluppi futuri
(personaggi con passive, stacking, modalità Endless/No Mercy, Observer): leggerlo prima di
lavorare su quelle funzionalità.

## Git

- Branch: `main` (principale), `primaiter`, `secondaIterBETA` (seconda iterazione), `ci-aiuta-claudio` (lavori fatti con Claude, nati da `secondaIterBETA`).
- Committare solo su richiesta esplicita.

## Funzionalità future

Obiettivo: implementarle in modo **flessibile e manutenibile** (GRASP, GoF, SOLID,
Protected Variations, niente `instanceof`/`if` sul tipo concreto dentro `Partita`).
Le proposte di design già scritte sono in `Codice/docs/refactoring_e_update_soluzioni.md`
(roadmap in 6 step): partire da lì.

**Avanzamento roadmap:**
- ✅ Step 1 (fondamenta), sul branch `ci-aiuta-claudio`. Scelte approvate:
  - `Ieffetto` riceve `Partita`;
  - `OriginePesca` ha un valore per ogni causa;
  - `isGiocabile` riceve il `Giocatore` (non è stato creato un record `ContestoGiocata`).
- ⏭️ Prossimo: Step 2 (Observer/Listener e input unificati).

Legenda della difficoltà (classificazione dell'utente):

- 🟢 = implementabile già ora senza problemi
- 🔴 = "problema vero": richiede refactoring o nuovi meccanismi
- ❌ = idea scartata, **non implementare**

### 1. Legend (personaggi)

Ogni giocatore può scegliere un **Legend**, che ha un'abilità **passiva** e una **attiva**.

**Abilità attive**

- 🟢 **Scambia mazzo** — scambi la tua mano con quella di un giocatore a tua scelta (stesso effetto della carta 7 No Mercy).
- 🟢 **Double card** — puoi giocare due carte nello stesso turno; si implementa come l'effetto "salta tutti".
- 🟢 **Gifted card** — dai due carte della tua mano, scelte a caso, a un giocatore a tua scelta; il loro effetto non si attiva.
- 🟢 **Il castigatore** — un giocatore a tua scelta pesca 4 carte.
- ❌ **Jolly** — attiva un effetto a tua scelta (scartata).

**Abilità passive** (tutte 🔴)

- **La Roccia** — immune alle carte +2.
- **Daltonico** — può giocare carte rosse su carte verdi e viceversa.
- **Salesman** — quando subisce una penalità di pesca pesca una carta in meno (vedi "Decisioni prese").
- **Ghost** — non può essere bersaglio degli effetti che scelgono un giocatore a piacere.
- *(Solo partita normale)* Ogni volta che pesca volontariamente, pesca due carte e sceglie quale tenere.

### 2. Modalità No Mercy

Si gioca anche con i Legend. Va implementata con le sue carte e tutte le regole che ne derivano.

**Carte nuove** (🟢)

- **0** — tutti passano la propria mano al giocatore a destra.
- **7** — scambi la tua mano con un giocatore a scelta.
- **Scarta tutto** — scarti tutte le carte della tua mano di quel colore; i loro effetti NON si attivano.
- **Salta tutti** — tutti saltano il turno e giochi un'altra carta.
- **Jolly cambio giro +4**, **Jolly +6**, **Jolly +10**.
- **Jolly roulette colore** — chi gioca la carta sceglie un colore; il giocatore successivo pesca finché non trova una carta di quel colore e tiene in mano tutte le carte pescate.

**Regole speciali**

- 🟢 **Pesca volontaria** — peschi finché non trovi una carta giocabile (nuova `IStrategiaPesca`).
- 🟢 **Sconfitta a 25 carte** (Mercy rule) — `IStrategiaCondizioneSconfitta` esiste già, ma oggi `Partita` non la usa mai.
- 🔴 **Stacking** — puoi rispondere a un +2 con un +2 o una carta superiore, e così via: le carte si accumulano e le pesca tutte il giocatore che non risponde.

**Regole ufficiali trovate sul web** (fonti secondarie: unorules.com, officialgamerules.org)

- Mazzo da 168 carte, 2–6 giocatori, 7 carte a testa.
- Stacking: rispondi solo con una carta di pesca di **valore uguale o superiore** all'ultima giocata.
  - +2 ← +2/+4/+6/+10
  - +4 ← +4/+6/+10
  - +6 ← +6/+10
  - +10 ← solo +10
  - Il Jolly cambio giro +4 vale come +4.
  - Chi non risponde pesca tutto il totale e perde il turno.
- La roulette non si può accumulare e chi pesca salta il turno. Nella regola ufficiale il colore lo sceglie la **vittima**; l'utente ha scelto: lo sceglie **chi gioca la carta**.
- La carta 0 nella regola ufficiale passa la mano "al successivo nel senso di gioco".
- Giocatore eliminato:
  - le sue carte vengono messe da parte e tornano nel mazzo al prossimo rimescolamento;
  - se resta un solo giocatore, vince lui.
- Jolly cambio giro +4 con 2 giocatori: funziona come un blocco.

### 3. Selezione delle regole di partita (🔴)

Menu di setup in cui scegliere:

- la modalità (normale / No Mercy);
- il numero di giocatori;
- i Legend di ogni giocatore.

Il menu costruisce la configurazione che arriva a `PartitaFactory` (mazzo + `RegoleDiGioco`).

### Decisioni prese (vincolanti)

- **Abilità attiva:**
  - ogni abilità ha un **cooldown in turni**, cioè un semplice `int` configurabile per Legend;
  - usarla **non** consuma il turno: dopo puoi ancora giocare o pescare.
- **La Roccia:**
  - è immune solo a un **+2 singolo, senza accumulo**;
  - se c'è uno stacking in corso subisce l'accumulo come tutti, ma in No Mercy può comunque rispondere con un +2 o superiore.
- **Salesman** (variante scelta: "pesca una carta in meno"):
  - vale per ogni penalità di pesca: +2/+4/+6/+10, accumulo dello stacking, Castigatore;
  - vale anche per la penalità del mancato UNO, perché applicarla sempre rende l'implementazione più semplice.
- **Ghost:**
  - non è bersagliabile dagli effetti in cui l'utente sceglie il giocatore (carta 7, Scambia mazzo, Gifted card, Castigatore);
  - è bersagliabile dagli effetti automatici (carta 0, +2 al successivo, ecc.).
- **Giocatore eliminato:** le sue carte vengono **subito rimescolate nel mazzo**. Qui ci si discosta dalla regola ufficiale.
- **Roulette:** il colore lo sceglie chi gioca la carta. Anche qui ci si discosta dalla regola ufficiale.

### Ancora da decidere

- Il valore del cooldown di ogni abilità attiva.
- Salesman e Jolly roulette: la roulette conta come penalità di pesca? Se sì, "una carta in meno" non è ben definito.
