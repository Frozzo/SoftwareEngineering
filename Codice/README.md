# UnoLegends — Documentazione Tecnica Completa del Codice, Architettura e Flussi Dati

Questo documento descrive **nel dettaglio e in modo fedele al codice sorgente attuale** l'architettura, le classi, lo stato interno, i design pattern e tutti i flussi dati/controllo del progetto **UnoLegends** (situato in `SoftwareEngineering/Codice/src/main/java`).

---

## 1. Avvio e Compilazione

Dalla cartella `SoftwareEngineering/Codice` (PowerShell):

```powershell
javac -d out (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
java -cp out unolegends.ui.UnoLegendsCli
```

La demo avvia una partita hotseat CLI a **2 giocatori** (`Giocatore1` e `Giocatore2`) con mazzo standard (108 carte) e regole standard.

---

## 2. Architettura a Strati (Layering) e Regole di Dipendenza

Il sistema è organizzato in **4 macro-strati / 5 package** con separazione netta tra presentazione, coordinamento casi d'uso, modello di dominio e strategie di regole:

```text
┌─────────────────────────────────────────────────────────────────────────┐
│  VIEW / UI  (package: unolegends.ui)                                    │
│  UnoLegendsCli                                                          │
│  - Dipende SOLO da unolegends.controller.*                              │
│  - NON importa né riceve MAI classi di unolegends.model o domain.rules  │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ comandi + DTO immutabili
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  CONTROLLER / FACADE  (package: unolegends.controller)                  │
│  UnoLegendsGame, StatoPartitaDto, RichiestaInputDto, FaseTurnoDto       │
│  - Unico punto d'ingresso della UI                                      │
│  - Avvia la partita tramite PartitaFactory                              │
│  - Converte oggetti del model in DTO immutabili (stringhe/enum)         │
│  - Traduce eventi e tipi di richiesta del model in frasi per la UI      │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ delega operazioni di dominio
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  MODEL / DOMINIO  (package: unolegends.model)                           │
│  Partita (Coordinator), Giocatore, Mazzo, PilaDegliScarti,              │
│  Carta / CartaNumero / CartaSpeciale, Colore, ValoreCarta, TipoMazzo,   │
│  Ieffetto + implementazioni, FaseTurno, AzioneInAttesa,                 │
│  RichiestaInput, TipoRichiesta, EventoPartita, TipoEvento,              │
│  PartitaFactory, MazzoFactory, StandardMazzoFactory, RegoleDiGioco      │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ Protected Variations (Strategy)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  DOMAIN RULES  (packages: unolegends.domain.rules / .impl)              │
│  Interfacce IStrategia* e implementazioni standard intercambiabili      │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Catalogo Completo delle Classi e Responsabilità

### 3.1 Package `unolegends.ui`

- **`UnoLegendsCli`** (`final class`, costruttore privato, entrypoint `main`):
  - Gestisce l'interazione testuale su `System.in` / `System.out` tramite `BufferedReader`.
  - Contiene il menu iniziale (`0` avvia partita demo standard, `1` esce) e il **loop principale di gioco**.
  - A ogni iterazione stampa i messaggi degli eventi (`controller.prelevaMessaggi()`), legge lo stato (`controller.richiediStato()`) e fa un unico `switch` esaustivo su `stato.getFase()`: `turnoNormale`, `turnoDopoPesca`, `gestisciRichiestaInput`, dichiarazione UNO, fine partita.
  - Gestisce i prompt interattivi (`richiediInput`), la finestra temporizzata di 5 secondi per la dichiarazione UNO (`attendiDichiarazioneUno`), il parsing degli indici (`parseIndice`) e la formattazione ANSI delle opzioni colore (`formattaOpzione`).

### 3.2 Package `unolegends.controller`

- **`UnoLegendsGame`** *(GRASP Controller / GoF Facade)*:
  - Mantiene un riferimento `private final Partita partita`.
  - Factory method statico `avviaPartitaStandard()`: crea una `Partita` per `2` giocatori tramite `PartitaFactory.creaPartita(StandardMazzoFactory.getInstance(), 2, RegoleDiGioco.standard())`, invoca `partita.iniziaGioco()` e restituisce il controller.
  - API esposte alla UI:
    - `richiediStato() : StatoPartitaDto` — costruisce uno snapshot testuale immutabile della partita; formatta la carta in cima con `coloreAttuale` se è una `CartaSpeciale` nera il cui colore è già stato scelto.
    - `giocaCarta(int indiceCarta) : boolean` — inoltra a `partita.giocaCarta(...)`; `true` se la giocata è accettata (completata oppure in attesa di input).
    - `rispondiInput(String risposta) : boolean` — unico punto per rispondere a qualunque richiesta di input; inoltra a `partita.rispondiInput(...)`.
    - `completaDichiarazioneUno(boolean unoDichiarato) : boolean` — inoltra a `partita.completaDichiarazioneUno(...)`; `true` se il comando è accettato (la penalità arriva come evento).
    - `pescaCarta() : boolean` — inoltra a `partita.pescaCarta()`.
    - `passaTurno() : boolean` — inoltra a `partita.passaTurno()`.
    - `prelevaMessaggi() : List<String>` — preleva gli `EventoPartita` registrati da `Partita` e li traduce in frasi (`descriviEvento`, con `switch` esaustivi su `TipoEvento` e `OriginePesca`).
  - Conversioni private: `convertiFase` (`FaseTurno` → `FaseTurnoDto`), `convertiRichiesta` + `messaggioPer` (`TipoRichiesta` → frase), `formattaCarta`.
- **`FaseTurnoDto`** *(enum)*: copia di `FaseTurno` per la UI, che non può importare il model.
- **`StatoPartitaDto`** *(DTO immutabile)*:
  - Campi: `fase` (`FaseTurnoDto`), `nomeGiocatoreAttivo` (`String`), `manoGiocatoreAttivo` (`List<String>`, copia immutabile), `cartaInCima` (`String`), `cartaAppenaPescata` (`String`, solo in `POST_PESCA`), `richiestaInput` (`RichiestaInputDto`, solo in `ATTESA_INPUT`), `nomeVincitore` (`String`, solo in `TERMINATA`).
- **`RichiestaInputDto`** *(DTO immutabile)*:
  - Campi: `messaggio` (`String`, scritto dal controller), `opzioni` (`List<String>`, copia immutabile).

### 3.3 Package `unolegends.model` — Entità Core

- **`Carta`** *(classe astratta)*:
  - Contratto: `getColore() : Colore`, `getValoreCarta() : ValoreCarta`, `getEffetto() : Ieffetto`, `getDescrizioneBreve() : String`.
- **`CartaNumero extends Carta`**:
  - Campi: `colore`, `valore`, `effetto` (sempre istanziato a `new Effetto_Nullo()`).
  - `toTestoBase()` produce `"<valore> <colore minuscolo>"` (es. `"7 rosso"`).
  - `getDescrizioneBreve()` applica la formattazione ANSI di `Colore`.
- **`CartaSpeciale extends Carta`**:
  - Campi: `colore`, `valoreCarta`, `effetto` (iniettato dal costruttore).
  - Oltre a `getDescrizioneBreve()`, offre l'overload `getDescrizioneBreve(Colore coloreVisualizzato)` usato dal controller per mostrare una carta nera (`cambia colore` o `+4`) con il colore attualmente attivo scelto dal giocatore.
- **`Colore`** *(enum)*:
  - Valori: `ROSSO` (`\u001B[31m`), `VERDE` (`\u001B[32m`), `BLU` (`\u001B[34m`), `GIALLO` (`\u001B[33m`), `NERO` (`\u001B[30m`).
  - `formatta(String testo)` racchiude il testo tra il codice ANSI del colore e `ANSI_RESET` (`\u001B[0m`).
- **`ValoreCarta`** *(enum)*:
  - Numeriche: `ZERO("0")` .. `NOVE("9")`.
  - Azione: `PIU_DUE("+2")`, `BLOCCA_TURNO("blocca turno")`, `CAMBIA_GIRO("cambia giro")`.
  - Nere speciali: `CAMBIA_COLORE("cambia colore")`, `PIU_QUATTRO("+4")`.
  - Utility statiche: `fromInt(int)` (0..9) e `fromString(String)` (case-insensitive).
- **`TipoMazzo`** *(enum)*:
  - Valore attuale: `STANDARD` (predisposto per configurazioni future).
- **`Giocatore`** *(GRASP Information Expert)*:
  - Campi: `nome` (`String`), `mano` (`List<Carta>`).
  - Metodi: `getNome()`, `getMano()` (restituisce `Collections.unmodifiableList(mano)`), `getCartaInPosizione(int)` (ritorna `null` se indice fuori range), `estraiCarta(int)` (rimuove e ritorna la carta, o `null` se fuori range), `rimuoviCarta(Carta)` (rimuove proprio quella carta, per identità; usato quando la giocata si completa dopo una risposta), `aggiungiCarta(Carta)`.
- **`Mazzo`** *(GRASP Information Expert)*:
  - Campo: `carte` (`Deque<Carta>` implementata con `ArrayDeque`).
  - `prelevaCarta()` esegue `carte.pollFirst()` (ritorna `null` se vuoto).
  - `isVuoto()`, `getNumeroCarte()`, `mischia()` (copia in `ArrayList`, `Collections.shuffle`, reinserisce nella `Deque`).
  - `rimescolaDaScarti(PilaDegliScarti)`: estrae tutte le carte dagli scarti tranne quella in cima, le aggiunge al mazzo e chiama `mischia()`.
- **`PilaDegliScarti`** *(GRASP Information Expert)*:
  - Campo: `scarti` (`List<Carta>`).
  - `getCartaInCima()` restituisce l'ultimo elemento della lista (o `null` se vuota).
  - `aggiungiCarta(Carta)` aggiunge in coda.
  - `estraiCartePerRimescolamento()`: se `scarti.size() <= 1` ritorna lista vuota; altrimenti rimuove la carta in cima, copia le restanti carte da rimescolare, svuota `scarti` e reinserisce la sola carta in cima.

### 3.4 Package `unolegends.model` — Effetti delle Carte e Interazione

- **`Ieffetto`** *(interfaccia Strategy / Command per gli effetti delle carte)*:
  - `void attivaEffetto(Partita partita)` (astratto).
  - Metodi `default` per supportare effetti con input opzionale (ricevono `Partita` perché alcune richieste, come la scelta di un giocatore bersaglio, dipendono dallo stato del gioco):
    - `getRichiestaInput(Partita partita)` → default `null`.
    - `validaRispostaInput(String risposta, Partita partita)` → default `risposta == null` se non richiede input, altrimenti `risposta != null`.
    - `attivaEffetto(Partita partita, String risposta)` → default verifica che non serva input e chiama `attivaEffetto(partita)`.
- **`Effetto_Nullo`** *(Null Object)*: `attivaEffetto` vuoto. Usato da tutte le `CartaNumero`.
- **`Effetto_Blocca_Turno`**: chiama `partita.segnalaSaltoTurno()`.
- **`Effetto_cambio_giro`**: chiama `partita.cambioGiro()`.
- **`Effetto_pescata_carte`**:
  - Campi: `numeroCarteDaPescare` (`int`, configurato a `2` o `4`) e `origine` (`OriginePesca`, `PIU_DUE` o `PIU_QUATTRO`).
  - `attivaEffetto(Partita)`: ottiene `prossimoGiocatore = partita.getProssimoGiocatore()`, invoca una sola volta `partita.applicaPescaForzata(prossimoGiocatore, numeroCarteDaPescare, origine)` e infine `partita.segnalaSaltoTurno()`.
- **`Effetto_Cambia_colore`**:
  - `attivaEffetto(Partita)` (senza risposta) lancia `IllegalStateException`.
  - `getRichiestaInput(Partita)` ritorna `RichiestaInput(TipoRichiesta.SCELTA_COLORE, ["Rosso", "Verde", "Blu", "Giallo"])`.
  - `validaRispostaInput(String, Partita)` controlla se la stringa (case-insensitive, trimmed) corrisponde a uno dei 4 colori (escluso `NERO`).
  - `attivaEffetto(Partita, String)` converte la stringa in `Colore` e chiama `partita.applicaColoreScelto(colore)`.
- **`EffettoComposito`** *(GoF Composite)*:
  - Campo: `List<Ieffetto> effetti`.
  - Usato per la carta `+4`, che compone `[Effetto_Cambia_colore, Effetto_pescata_carte(4)]`.
  - `getRichiestaInput(Partita)` restituisce la prima richiesta non nulla tra i sotto-effetti.
  - `validaRispostaInput(String, Partita)` delega la validazione al primo sotto-effetto con richiesta di input.
  - `attivaEffetto(Partita, String)` esegue tutti i sotto-effetti in ordine: passa `risposta` al primo sotto-effetto che richiede input, e chiama `attivaEffetto(partita)` senza argomenti su tutti gli altri.
- **`RichiestaInput`**: oggetto valore immutabile del dominio con `tipo` (`TipoRichiesta`) e `opzioni` (`List<String>`). Non contiene testo per l'utente.
- **`TipoRichiesta`** *(enum)*: `SCELTA_COLORE`, `SCELTA_GIOCATORE` (quest'ultimo predisposto per carta 7 e abilità attive).
- **`AzioneInAttesa`** *(record package-private, GoF Command)*: `(Ieffetto effetto, Consumer<String> completamento)`. L'effetto sa cosa chiedere e validare; il completamento è ciò che `Partita` esegue quando arriva una risposta valida.
- **`FaseTurno`** *(enum)*: `NORMALE`, `POST_PESCA`, `ATTESA_INPUT`, `ATTESA_UNO`, `TERMINATA`. Vedi sezione 4.
- **`EventoPartita`** *(record)* + **`TipoEvento`** *(enum)*: registro dei fatti accaduti (`PESCA_VOLONTARIA`, `PESCA_FORZATA`, `TURNO_SALTATO`, `SENSO_INVERTITO`, `COLORE_SCELTO`, `UNO_DICHIARATO`). Campi: `tipo`, `nomeGiocatore`, `quantita`, `origine`, `colore`; si creano con factory statiche package-private.
- **`OriginePesca`** *(enum)*: causa di una pesca (`VOLONTARIA`, `PIU_DUE`, `PIU_QUATTRO`, `PENALITA_UNO`). Permette alle regole (e in futuro alle passive dei Legend) di distinguere le pescate.
- **`ContestoPesca`** *(record immutabile)*: `(Giocatore bersaglio, int numeroCarte, OriginePesca origine)`, passato a `RegoleDiGioco.calcolaCartePescaForzata(...)`.

### 3.5 Package `unolegends.model` — Factory e Contenitore Regole

- **`MazzoFactory`** *(Abstract Factory)*: interfaccia con `Mazzo creaMazzo()`.
- **`StandardMazzoFactory`** *(Concrete Factory + Singleton)*:
  - Istanza statica eager `INSTANCE` esposta via `getInstance()`.
  - `creaMazzo()` costruisce **108 carte** e chiama `mazzo.mischia()`:
    - Per ciascuno dei 4 colori (`ROSSO`, `GIALLO`, `VERDE`, `BLU`):
      - 1 copia di `0` (`CartaNumero`)
      - 2 copie per ogni numero da `1` a `9` (`CartaNumero`) → 19 carte numeriche per colore
      - 2 copie di `PIU_DUE` (`CartaSpeciale` con `Effetto_pescata_carte(2, PIU_DUE)`)
      - 2 copie di `BLOCCA_TURNO` (`CartaSpeciale` con `Effetto_Blocca_Turno`)
      - 2 copie di `CAMBIA_GIRO` (`CartaSpeciale` con `Effetto_cambio_giro`) → 6 carte azione per colore
    - Totale carte colorate: `4 × 25 = 100`.
    - Carte nere (`Colore.NERO`):
      - 4 copie di `CAMBIA_COLORE` (`Effetto_Cambia_colore`)
      - 4 copie di `PIU_QUATTRO` (`EffettoComposito([Effetto_Cambia_colore, Effetto_pescata_carte(4, PIU_QUATTRO)])`)
    - Totale complessivo: **108 carte**.
- **`PartitaFactory`** *(GRASP Creator / GoF Factory)*:
  - Costante `CARTE_PER_GIOCATORE = 7`.
  - `creaRegoleStandard()` delega a `RegoleDiGioco.standard()`.
  - `creaPartita(MazzoFactory, int numeroDiGiocatori, RegoleDiGioco)`: crea il mazzo, distribuisce 7 carte a ciascun giocatore (nominati `"Giocatore1"`, `"Giocatore2"`, ...), pesca la `primaCarta` per inizializzare `PilaDegliScarti` e istanzia `Partita` (il giocatore iniziale lo decide `Partita`).
- **`RegoleDiGioco`** *(Protected Variations / Contenitore di Strategy)*:
  - Contiene 5 strategie, tutte iniettate dal costruttore:
    1. `IStrategiaCondizioneVittoria`
    2. `IStrategiaCondizioneSconfitta`
    3. `IStrategiaCompatibilitaCarte`
    4. `IStrategiaPesca` (pesca **volontaria**)
    5. `IStrategiaPenalitaUno`
  - Metodi esposti:
    - `isGiocabile(Giocatore giocatore, Carta candidata, Carta inCima, Partita partita)` → delega a `strategiaCompatibilitaCarte.puoGiocare(candidata, inCima, partita.getColoreAttuale())`. Il giocatore servirà alle passive che modificano la compatibilità.
    - `isVincitore(Giocatore, Partita)` → delega a `strategiaVittoria`.
    - `isEliminato(Giocatore, Partita)` → delega a `strategiaSconfitta`.
    - `pescaVolontaria(Giocatore, Partita)` → delega alla strategia di pesca volontaria.
    - `calcolaCartePescaForzata(ContestoPesca)` → numero di carte che il bersaglio deve pescare davvero; oggi restituisce `contesto.numeroCarte()`, in futuro vi si agganciano le passive.
    - `mancatoUno(Giocatore giocatore, boolean unoDichiarato, Partita partita)` → se `mano.size() == 1 && !unoDichiarato`, applica `strategiaPenalitaUno` e ritorna `true`; altrimenti `false`.
    - `standard()` → restituisce `RegoleDiGioco` con tutte le implementazioni standard.

### 3.6 Package `unolegends.domain.rules` e `unolegends.domain.rules.impl`

| Interfaccia | Implementazione Standard | Logica Implementata |
|---|---|---|
| `IStrategiaCompatibilitaCarte` | `StrategiaCompatibilitaCarte` | Ritorna `false` se `cartaGiocata == null`; `true` se `cartaInCima == null` o `cartaGiocata.getColore() == Colore.NERO`; altrimenti confronta `cartaGiocata.getColore() == coloreDaConfrontare` (dove `coloreDaConfrontare = coloreAttivo != null ? coloreAttivo : cartaInCima.getColore()`) oppure `cartaGiocata.getValoreCarta() == cartaInCima.getValoreCarta()`. |
| `IStrategiaCondizioneVittoria` | `StrategiaVittoriaStandard` | `giocatore != null && giocatore.getMano().isEmpty()`. |
| `IStrategiaCondizioneSconfitta` | `StrategiaSconfittaStandard` | Ritorna sempre `false` (nessuna eliminazione nelle regole standard). |
| `IStrategiaPesca` (pesca volontaria) | `StrategiaPescaSingola` | Chiama `partita.pescaSingola(giocatore)` e ritorna `List.of(carta)` (o `List.of()` se mazzo e scarti sono vuoti). |
| `IStrategiaPenalitaUno` | `StrategiaPenalitaMancatoUno` | Chiama `partita.applicaPescaForzata(giocatore, numeroCarte, OriginePesca.PENALITA_UNO)` (default `2` carte). |

---

## 4. Stato Interno di `Partita` e Macchina a Fasi del Turno

`Partita` (in `unolegends.model.Partita`) è il **GRASP Coordinator / radice del dominio**. Dallo Step 2 lo stato del turno è descritto da **un solo campo `fase`** (`FaseTurno`) invece che da più variabili sparse. Campi:

| Campo | Tipo | Significato e ciclo di vita |
|---|---|---|
| `giocatori` | `List<Giocatore>` | Lista ordinata dei giocatori partecipanti (copia difensiva). |
| `mazzo` | `Mazzo` | Mazzo da cui pescare le carte. |
| `pilaDegliScarti` | `PilaDegliScarti` | Pila delle carte giocate; l'ultima è la carta in cima. |
| `regole` | `RegoleDiGioco` | Profilo di strategie di gioco attivo. |
| `eventi` | `List<EventoPartita>` | Registro degli eventi accaduti dall'ultima lettura; svuotato da `prelevaEventi()`. |
| `indiceGiocatoreAttivo` | `int` | Indice in `giocatori` del giocatore di turno (`0 .. n-1`). |
| `fase` | `FaseTurno` | Fase corrente del turno (vedi tabella sotto). |
| `azioneInAttesa` | `AzioneInAttesa` | Valorizzata solo in `ATTESA_INPUT`: effetto da completare e completamento da eseguire. |
| `cartaAppenaPescata` | `Carta` | Ultima carta pescata volontariamente; esposta solo in `POST_PESCA`. |
| `vincitore` | `Giocatore` | Dato del vincitore, valorizzato insieme a `fase = TERMINATA`. |
| `giocoIniziato` | `boolean` | Rende `iniziaGioco()` idempotente (evita la doppia applicazione dell'effetto iniziale). |
| `saltaProssimoTurno` | `boolean` | Non è una fase: è un'informazione per il prossimo avanzamento, impostata da `segnalaSaltoTurno()` e consumata da `aggiornaGiocatoreAttivo()`. |
| `coloreAttuale` | `Colore` | Colore attivo per la compatibilità delle carte. Inizializzato al colore della prima carta; aggiornato sulle giocate colorate o tramite `applicaColoreScelto(Colore)` (package-private) sulle carte nere. |
| `sensoOrario` | `boolean` | Direzione di avanzamento turni (default `true`, invertito da `cambioGiro()`). |

Rimossi nello Step 2: `cartaPescataDaGiocare`, `effettoInizialeInAttesa`, `giocatoreInAttesaDichiarazioneUno` (il giocatore in attesa di UNO è sempre quello attivo, perché il turno non è ancora avanzato).

### Fasi del turno e comandi permessi

Ogni comando pubblico dichiara le fasi in cui è **permesso** (lista di ammissione): una fase nuova viene rifiutata da tutti i comandi finché non la si abilita.

| Fase | Comandi accettati | Si entra con | Si esce con |
|---|---|---|---|
| `NORMALE` | `giocaCarta`, `pescaCarta` | fine del turno precedente | giocata, pesca |
| `POST_PESCA` | `giocaCarta` (qualsiasi carta della mano), `passaTurno` | `pescaCarta` | giocata, `passaTurno` |
| `ATTESA_INPUT` | `rispondiInput` | carta iniziale o carta giocata che richiede input | risposta valida → il completamento decide la fase |
| `ATTESA_UNO` | `completaDichiarazioneUno` | giocata che lascia una sola carta | dichiarazione (con o senza penalità) |
| `TERMINATA` | nessuno | giocata che svuota la mano | — |

---

## 5. Flussi Dati e Dinamiche Passo-Passo

### Flusso 1 — Avvio della Partita e Gestione della Prima Carta

```text
UnoLegendsCli.main
  └─> UnoLegendsGame.avviaPartitaStandard()
        ├─> PartitaFactory.creaPartita(StandardMazzoFactory.getInstance(), 2, RegoleDiGioco.standard())
        │     ├─> StandardMazzoFactory.creaMazzo() -> 108 carte mescolate
        │     ├─> distribuisciCarteAiGiocatori(mazzo, 2) -> 7 carte a Giocatore1, 7 a Giocatore2
        │     ├─> primaCarta = mazzo.prelevaCarta()
        │     ├─> pilaDegliScarti = new PilaDegliScarti([primaCarta])
        │     └─> new Partita(...) -> coloreAttuale = primaCarta.getColore();
        │                             indiceGiocatoreAttivo = giocatori.size() - 1
        └─> partita.iniziaGioco()
```

- **Caso A — La prima carta NON richiede input** (carta numerica, `+2`, `Blocca Turno`, `Cambia Giro`):
  1. `iniziaGioco()` applica `primaCarta.getEffetto().attivaEffetto(this)`.
  2. Se la prima carta era un `+2`, `getProssimoGiocatore()` (partendo da `1` in senso orario) è `Giocatore1`: pesca 2 carte (evento `PESCA_FORZATA`) e viene impostato `saltaProssimoTurno = true`.
  3. Chiama `aggiornaGiocatoreAttivo()`:
     - per carta numerica: avanza da `1` a `0` → inizia `Giocatore1`;
     - per `+2` o `Blocca Turno`: avanza a `0`, registra `TURNO_SALTATO` e avanza a `1` → inizia `Giocatore2`;
     - per `Cambia Giro`: `sensoOrario` è già `false` (evento `SENSO_INVERTITO`) e avanza in senso antiorario.
- **Caso B — La prima carta richiede input** (`Cambia Colore` o `+4`):
  1. `iniziaGioco()` imposta `indiceGiocatoreAttivo = 0` e chiama `mettiInAttesa(effetto, completamento)`: la fase diventa `ATTESA_INPUT`.
  2. Il completamento è `risposta -> { effetto.attivaEffetto(this, risposta); chiudiTurno(); }`.
  3. Al primo giro la CLI vede `ATTESA_INPUT`, chiede il colore a `Giocatore1` e chiama `controller.rispondiInput(risposta)`: è lo **stesso** meccanismo delle carte giocate nel turno (Flusso 4).

---

### Flusso 2 — Ciclo Principale della CLI e Snapshot dello Stato

A ogni iterazione del ciclo in `UnoLegendsCli.main`:
1. Stampa i messaggi di `controller.prelevaMessaggi()` (eventi accaduti dall'ultimo comando).
2. Legge `StatoPartitaDto stato = controller.richiediStato()`.
3. Fa uno `switch` esaustivo su `stato.getFase()`:
   - `TERMINATA` → stampa il vincitore ed esce;
   - `ATTESA_INPUT` → `gestisciRichiestaInput` (stampa messaggio e opzioni, chiama `rispondiInput`);
   - `ATTESA_UNO` → `attendiDichiarazioneUno` e `completaDichiarazioneUno`;
   - `POST_PESCA` → `turnoDopoPesca`: mostra stato e carta pescata, accetta un indice oppure `'p'`;
   - `NORMALE` → `turnoNormale`: mostra stato e il menu `0) gioca carta`, `1) pesca carta`, `2) esci`.

La CLI non deduce più la fase combinando flag: la legge dal model.

---

### Flusso 3 — Giocare una Carta (`giocaCarta`)

`Partita.giocaCarta(int indiceCarta)`:
1. **Fase**: se non è `NORMALE` né `POST_PESCA` → `false`.
2. **Indice**: `cartaSelezionata = giocatoreAttivo.getCartaInPosizione(indiceCarta)`; se `null` → `false`.
3. **Compatibilità**: `regole.isGiocabile(giocatoreAttivo, cartaSelezionata, cartaInCima, this)`; se `false` → `false` (mano intatta).
4. **Input**: se `effetto.getRichiestaInput(this) != null` → `mettiInAttesa(effetto, risposta -> completaGiocata(carta, risposta))`, fase `ATTESA_INPUT`, ritorna `true`. **La carta resta in mano** fino alla risposta.
5. Altrimenti `completaGiocata(carta, null)` e ritorna `true`.

`completaGiocata(Carta, String risposta)` (privato, comune ai due casi):
1. Calcola `avevaDueCarte`, toglie la carta (`rimuoviCarta`) e la mette negli scarti.
2. Se la carta non è `NERO`, aggiorna `coloreAttuale`; azzera `cartaAppenaPescata`.
3. Applica `effetto.attivaEffetto(this, risposta)`.
4. **Vittoria**: se `regole.isVincitore(...)` → `vincitore`, fase `TERMINATA`.
5. **Finestra UNO**: se `avevaDueCarte` e ora ha una carta → fase `ATTESA_UNO` (il turno non avanza).
6. Altrimenti `chiudiTurno()`: fase `NORMALE` e `aggiornaGiocatoreAttivo()`.

---

### Flusso 4 — Input Unificato (`Cambia Colore`, `+4`, carta iniziale)

1. `giocaCarta` (o `iniziaGioco`) crea un'`AzioneInAttesa` e porta la fase in `ATTESA_INPUT`.
2. `richiediStato()` espone `partita.getRichiestaInputInAttesa()`: il controller converte `TipoRichiesta.SCELTA_COLORE` nella frase "Scegli il colore da assegnare alla carta:".
3. La CLI stampa le opzioni numerate e chiama `controller.rispondiInput(risposta)`.
4. `Partita.rispondiInput(risposta)`:
   - fuori da `ATTESA_INPUT` → `false`;
   - `azione.effetto().validaRispostaInput(risposta, this)` falsa → `false` (la richiesta resta aperta, la CLI richiede);
   - altrimenti azzera l'azione, riporta la fase a `NORMALE` ed esegue `azione.completamento().accept(risposta)`, che decide la fase successiva.
5. Per **`+4`**, `EffettoComposito.attivaEffetto(partita, risposta)` esegue:
   1. `Effetto_Cambia_colore` → `applicaColoreScelto(colore)` (evento `COLORE_SCELTO`);
   2. `Effetto_pescata_carte(4, PIU_QUATTRO)` → `applicaPescaForzata` (evento `PESCA_FORZATA`) e `segnalaSaltoTurno()`.

---

### Flusso 5 — Dichiarazione UNO e Penalità

1. Dopo la giocata la fase è `ATTESA_UNO`.
2. `UnoLegendsCli.attendiDichiarazioneUno(reader)` apre una finestra di **5 secondi** controllando `reader.ready()` ogni 50 ms: `"uno"` entro il tempo → `true`, altrimenti `false`.
3. `partita.completaDichiarazioneUno(unoDichiarato)`:
   - fuori da `ATTESA_UNO` → `false`;
   - se dichiarato registra `UNO_DICHIARATO`;
   - `regole.mancatoUno(giocatoreAttivo, unoDichiarato, this)`: se non dichiarato `StrategiaPenalitaMancatoUno` chiama `applicaPescaForzata(giocatore, 2, PENALITA_UNO)`, che registra `PESCA_FORZATA`;
   - `chiudiTurno()`.
4. La CLI non stampa più un messaggio di penalità proprio: arriva dagli eventi.

---

### Flusso 6 — Pesca Volontaria, Passa Turno e Rimescolamento Automatico

1. **Pesca volontaria (`partita.pescaCarta()`)**:
   - Permessa solo in `NORMALE`.
   - `regole.pescaVolontaria(giocatore, this)`; con le regole standard `StrategiaPescaSingola` chiama `pescaSingola(giocatore)`, che se il mazzo è vuoto rimescola gli scarti (la carta in cima resta) e riprova.
   - Registra `PESCA_VOLONTARIA` con il numero di carte pescate, imposta `cartaAppenaPescata` e porta la fase in `POST_PESCA`.
   - Ritorna `true` anche se mazzo e scarti erano vuoti (l'evento lo segnala con quantità 0).
2. **Passa turno (`partita.passaTurno()`)**: permesso solo in `POST_PESCA`; chiama `chiudiTurno()`.
3. In `POST_PESCA` si può giocare **qualsiasi** carta compatibile della mano, non solo quella pescata.

---

### Flusso 7 — Pesca Forzata, Eventi e Avanzamento / Salto / Inversione del Turno

1. **Pesca forzata (`partita.applicaPescaForzata(Giocatore, int, OriginePesca)`)**:
   - Unico punto d'ingresso per tutte le pesche forzate: `Effetto_pescata_carte` (`+2`, `+4`) e `StrategiaPenalitaMancatoUno`.
   - Costruisce un `ContestoPesca` e chiede a `regole.calcolaCartePescaForzata(contesto)` quante carte pescare davvero.
   - Chiama `pescaSingola(bersaglio)` quel numero di volte, fermandosi se mazzo e scarti sono esauriti.
   - Registra **un** evento `PESCA_FORZATA` con le carte effettivamente pescate e l'origine.
2. **Avanzamento turno**:
   - `getProssimoGiocatore()` calcola `(indice + 1) % n` se `sensoOrario`, oppure `(indice - 1 + n) % n`, **senza** modificare `indiceGiocatoreAttivo`.
   - `aggiornaGiocatoreAttivo()` (ora privato) chiama `avanzaGiocatore()`; se `saltaProssimoTurno`, azzera il flag, registra `TURNO_SALTATO` e avanza una **seconda volta**.
   - `cambioGiro()` inverte `sensoOrario` e registra `SENSO_INVERTITO`.
3. **Lettura degli eventi**: `Partita.prelevaEventi()` restituisce una copia del registro e lo svuota; `UnoLegendsGame.prelevaMessaggi()` traduce ogni evento in frase.

---

## 6. Tabella Riassuntiva dei Design Pattern (GoF & GRASP)

| Pattern | Tipo | Classi nel Codice | Ruolo Effettivo |
|---|---|---|---|
| **Facade / Controller** | GoF / GRASP | `UnoLegendsGame` | Espone API semplificate alla UI e disaccoppia `ui` da `model`. |
| **DTO (Data Transfer Object)** | Architetturale | `StatoPartitaDto`, `RichiestaInputDto`, `FaseTurnoDto` | Trasporta dati immutabili privi di dipendenze dal dominio verso la UI. |
| **Coordinator** | GRASP | `Partita` | Coordina turni, stato partita, delega a regole, effetti ed expert. |
| **Information Expert** | GRASP | `Giocatore`, `Mazzo`, `PilaDegliScarti`, `Effetto_Cambia_colore` | Ogni classe gestisce i dati di cui è esperta (mano, deck, scarti, validazione colore). |
| **Creator / Factory** | GRASP / GoF | `PartitaFactory` | Centralizza la costruzione e il setup iniziale di `Partita`. |
| **Abstract Factory** | GoF | `MazzoFactory`, `StandardMazzoFactory` | Astrae la creazione della famiglia di carte del mazzo. |
| **Singleton** | GoF | `StandardMazzoFactory` | Garantisce un'unica istanza della factory del mazzo standard (`getInstance()`). |
| **Strategy / Protected Variations** | GoF / GRASP | `RegoleDiGioco`, `IStrategia*`, `Ieffetto` | Rende intercambiabili compatibilità carte, pesca, vittoria, sconfitta, penalità UNO ed effetti delle carte. |
| **Composite** | GoF | `EffettoComposito` | Compone più `Ieffetto` in sequenza trattandoli come un unico `Ieffetto` (usato per `+4`). |
| **Null Object** | Design Pattern | `Effetto_Nullo` | Evita controlli `if (effetto != null)` sulle carte numeriche. |
| **Command** | GoF | `AzioneInAttesa` | Incapsula un'azione sospesa (effetto + completamento) da eseguire quando arriva la risposta del giocatore. |
| **Pure Fabrication / Indirection** | GRASP | `EventoPartita`, `TipoEvento` | Registro eventi: il model registra cosa succede senza sapere chi lo leggerà; il controller lo traduce in testo. |

---

## 7. Peculiarità Note e Stato Reale di Implementazione (Utile per Analisi e Refactor)

Per comprendere con precisione ciò che il codice fa *effettivamente* oggi rispetto a eventuali documenti di analisi/diagrammi:

1. **Numero di carte iniziali**: `PartitaFactory.CARTE_PER_GIOCATORE` distribuisce **7 carte** per giocatore (mentre `docs/roles_and_patterns.md` cita ancora 5 carte da una revisione precedente).
2. **Disallineamento `domain_class_diagram.puml` vs codice reale**:
   - Nel codice reale `Partita.giocaCarta` ha firma `(int indiceCarta) : boolean`: la risposta agli input passa da `rispondiInput(String)` e la dichiarazione UNO da `completaDichiarazioneUno(boolean)`.
   - Nel codice reale `CartaNumero` non ha il campo `id : int`.
   - In `Mazzo`, il metodo si chiama `getNumeroCarte()` (non `getNumeroCarteRestanti()`).
   - `Partita` non espone `getMazzoDaPesca()`, `getNumeroCarteNelMazzo()` né `tuttiGliAltriGiocatoriHannoPiuDi(...)`.
3. **Strategia di sconfitta predisposta ma non ancora agganciata**: `RegoleDiGioco` espone `isEliminato(Giocatore, Partita)` con `StrategiaSconfittaStandard` (che ritorna sempre `false`), ma `Partita` attualmente non invoca `regole.isEliminato(...)` durante il turno.
4. **Dopo la pesca si gioca qualsiasi carta**: in `POST_PESCA` `Partita.giocaCarta` accetta ogni carta compatibile della mano, non solo `cartaAppenaPescata` (regola scelta per il progetto).
5. **Cambia Giro con 2 giocatori**: `Effetto_cambio_giro` inverte `sensoOrario`, ma con `giocatori.size() == 2` avanzare di `+1` o `-1` modulo `2` seleziona comunque l'altro giocatore (non equivale a un salta turno in 2 giocatori, a differenza della variante UNO 2-player ufficiale).
6. **Attese di input unificate (Step 2)**: carta iniziale nera e carta nera giocata nel turno passano entrambe da `AzioneInAttesa` + `rispondiInput(String)`. La dichiarazione UNO resta un comando separato (`completaDichiarazioneUno`) perché ha il timer di 5 secondi ed è un'interazione diversa da "scegli fra opzioni".
7. **Assenza di `Personaggio` / abilità passive e stacking `+2`/`+4`**: non sono ancora presenti nel codice attuale; `Partita` e `RegoleDiGioco` implementano al momento il solo set di regole UNO standard senza personaggi né accumulo di penalità.
