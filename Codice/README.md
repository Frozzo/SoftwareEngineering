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
│  UnoLegendsGame, StatoPartitaDto, EsitoGiocataDto, RichiestaInputDto    │
│  - Unico punto d'ingresso della UI                                      │
│  - Avvia la partita tramite PartitaFactory                              │
│  - Converte oggetti del model in DTO immutabili (stringhe/booleani)     │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ delega operazioni di dominio
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  MODEL / DOMINIO  (package: unolegends.model)                           │
│  Partita (Coordinator), Giocatore, Mazzo, PilaDegliScarti,              │
│  Carta / CartaNumero / CartaSpeciale, Colore, ValoreCarta, TipoMazzo,   │
│  Ieffetto + implementazioni, RichiestaInput, EsitoGiocata,              │
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
  - Contiene il menu iniziale (`0` avvia partita demo standard, `1` esce) e il **loop principale di gioco (`while (true)`)**.
  - A ogni iterazione interroga il controller tramite `controller.richiediStato()` ottenendo uno `StatoPartitaDto`.
  - Gestisce i prompt interattivi (`richiediInput`, `gestisciGiocata`), la finestra temporizzata di 5 secondi per la dichiarazione UNO (`attendiDichiarazioneUno`), il parsing degli indici (`parseIndice`) e la formattazione ANSI delle opzioni colore (`formattaOpzione`).

### 3.2 Package `unolegends.controller`

- **`UnoLegendsGame`** *(GRASP Controller / GoF Facade)*:
  - Mantiene un riferimento `private final Partita partita`.
  - Factory method statico `avviaPartitaStandard()`: crea una `Partita` per `2` giocatori tramite `PartitaFactory.creaPartita(StandardMazzoFactory.getInstance(), 2, RegoleDiGioco.standard())`, invoca `partita.iniziaGioco()` e restituisce il controller.
  - API esposte alla UI:
    - `richiediStato() : StatoPartitaDto` — costruisce uno snapshot testuale immutabile della partita; formatta la carta in cima con `coloreAttuale` se è una `CartaSpeciale` nera il cui colore è già stato scelto.
    - `giocaCarta(int indiceCarta, String rispostaInput) : EsitoGiocataDto` — inoltra a `partita.giocaCarta(...)` e converte `EsitoGiocata` in `EsitoGiocataDto`.
    - `completaDichiarazioneUno(boolean unoDichiarato) : boolean` — inoltra a `partita.completaDichiarazioneUno(...)`.
    - `rispondiRichiestaInputIniziale(String rispostaInput) : boolean` — inoltra a `partita.rispondiRichiestaInputIniziale(...)`.
    - `pescaCarta() : boolean` — inoltra a `partita.pescaCarta()`.
    - `passaTurno() : boolean` — inoltra a `partita.passaTurno()`.
- **`StatoPartitaDto`** *(DTO immutabile)*:
  - Campi: `nomeGiocatoreAttivo` (`String`), `manoGiocatoreAttivo` (`List<String>`, copia immutabile), `cartaInCima` (`String`), `deveGiocareCartaPescata` (`boolean`), `cartaAppenaPescata` (`String`), `richiestaInput` (`RichiestaInputDto`), `partitaTerminata` (`boolean`), `nomeVincitore` (`String`).
- **`EsitoGiocataDto`** *(DTO immutabile)*:
  - Campi: `giocata` (`boolean`), `richiestaInput` (`RichiestaInputDto`), `richiedeDichiarazioneUno` (`boolean`).
- **`RichiestaInputDto`** *(DTO immutabile)*:
  - Campi: `messaggio` (`String`), `opzioni` (`List<String>`, copia immutabile).

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
  - Metodi: `getNome()`, `getMano()` (restituisce `Collections.unmodifiableList(mano)`), `getCartaInPosizione(int)` (ritorna `null` se indice fuori range), `estraiCarta(int)` (rimuove e ritorna la carta, o `null` se fuori range), `aggiungiCarta(Carta)`.
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
  - `getRichiestaInput(Partita)` ritorna `RichiestaInput("Scegli il colore da assegnare alla carta:", ["Rosso", "Verde", "Blu", "Giallo"])`.
  - `validaRispostaInput(String, Partita)` controlla se la stringa (case-insensitive, trimmed) corrisponde a uno dei 4 colori (escluso `NERO`).
  - `attivaEffetto(Partita, String)` converte la stringa in `Colore` e chiama `partita.applicaColoreScelto(colore)`.
- **`EffettoComposito`** *(GoF Composite)*:
  - Campo: `List<Ieffetto> effetti`.
  - Usato per la carta `+4`, che compone `[Effetto_Cambia_colore, Effetto_pescata_carte(4)]`.
  - `getRichiestaInput(Partita)` restituisce la prima richiesta non nulla tra i sotto-effetti.
  - `validaRispostaInput(String, Partita)` delega la validazione al primo sotto-effetto con richiesta di input.
  - `attivaEffetto(Partita, String)` esegue tutti i sotto-effetti in ordine: passa `risposta` al primo sotto-effetto che richiede input, e chiama `attivaEffetto(partita)` senza argomenti su tutti gli altri.
- **`RichiestaInput`**: oggetto valore immutabile del dominio con `messaggio` (`String`) e `opzioni` (`List<String>`).
- **`EsitoGiocata`**: oggetto valore immutabile del dominio con 3 factory statiche:
  - `completata(boolean giocata)` → giocata completata (`true`) o rifiutata (`false`), senza input né attesa UNO.
  - `inAttesa(RichiestaInput richiesta)` → `giocata = false`, `richiestaInput != null`.
  - `richiedeDichiarazioneUno()` → `giocata = true`, `richiedeDichiarazioneUno = true`.
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

`Partita` (in `unolegends.model.Partita`) è il **GRASP Coordinator / radice del dominio**. Mantiene tutto lo stato persistente e di coordinamento del turno nei seguenti **14 campi**:

| Campo | Tipo | Significato e ciclo di vita |
|---|---|---|
| `giocatori` | `List<Giocatore>` | Lista ordinata dei giocatori partecipanti (copia difensiva). |
| `mazzo` | `Mazzo` | Mazzo da cui pescare le carte. |
| `pilaDegliScarti` | `PilaDegliScarti` | Pila delle carte giocate; l'ultima è la carta in cima. |
| `regole` | `RegoleDiGioco` | Profilo di strategie di gioco attivo. |
| `indiceGiocatoreAttivo` | `int` | Indice in `giocatori` del giocatore di turno (`0 .. n-1`). |
| `cartaAppenaPescata` | `Carta` | Riferimento all'ultima carta pescata volontariamente nel turno corrente (o `null`). |
| `cartaPescataDaGiocare` | `boolean` | `true` dopo `pescaCarta()` finché il giocatore non gioca una carta o chiama `passaTurno()`. |
| `effettoInizialeInAttesa` | `Ieffetto` | Se la prima carta scoperta negli scarti richiede input (`Cambia Colore` o `+4`), conserva qui l'effetto finché il primo giocatore non risponde. |
| `vincitore` | `Giocatore` | `null` finché la partita è in corso; valorizzato quando `regole.isVincitore(...)` diventa `true`. |
| `giocatoreInAttesaDichiarazioneUno` | `Giocatore` | Valorizzato quando un giocatore passa da 2 carte a 1 carta; blocca altre mosse finché non viene chiamato `completaDichiarazioneUno(...)`. |
| `giocoIniziato` | `boolean` | Rende `iniziaGioco()` idempotente (evita la doppia applicazione dell'effetto iniziale). |
| `saltaProssimoTurno` | `boolean` | Flag impostato da `segnalaSaltoTurno()` (package-private, chiamato da `Effetto_Blocca_Turno` ed `Effetto_pescata_carte`) e consumato da `aggiornaGiocatoreAttivo()`. |
| `coloreAttuale` | `Colore` | Colore attivo per la compatibilità delle carte. Inizializzato al colore della prima carta; aggiornato sulle giocate colorate o tramite `applicaColoreScelto(Colore)` (package-private) sulle carte nere. |
| `sensoOrario` | `boolean` | Direzione di avanzamento turni (default `true`, invertito da `cambioGiro()`). |

### Stati Operativi / Guardie di `Partita`

Ogni comando pubblico controlla prima le guardie di stato per rifiutare azioni non coerenti senza corrompere lo stato:

1. **Partita terminata** (`vincitore != null`): rifiuta `giocaCarta`, `pescaCarta`, `passaTurno`.
2. **In attesa di input iniziale** (`effettoInizialeInAttesa != null`): rifiuta `giocaCarta`; accetta `rispondiRichiestaInputIniziale(risposta)`.
3. **In attesa di dichiarazione UNO** (`giocatoreInAttesaDichiarazioneUno != null`): rifiuta `giocaCarta`, `pescaCarta`, `passaTurno`; accetta `completaDichiarazioneUno(boolean)`.
4. **Dopo pesca volontaria** (`cartaPescataDaGiocare == true`): rifiuta un'altra `pescaCarta()`; accetta `giocaCarta(...)` oppure `passaTurno()`.
5. **Turno normale** (tutti i flag di attesa a `false`/`null`): accetta `giocaCarta(...)` oppure `pescaCarta()`; rifiuta `passaTurno()`.

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
        │     └─> new Partita(...) -> imposta coloreAttuale = primaCarta.getColore();
        │         se primaCarta.getEffetto().getRichiestaInput(this) != null, salva effettoInizialeInAttesa;
        │         calcola indiceGiocatoreAttivo iniziale (vedi dettaglio sotto)
        └─> partita.iniziaGioco()
```

**Dettaglio fondamentale sul giocatore iniziale (calcolato nel costruttore di `Partita`):**
- **Caso A — La prima carta NON richiede input** (carta numerica, `+2`, `Blocca Turno`, `Cambia Giro`):
  - `Partita` imposta `indiceGiocatoreAttivo = giocatori.size() - 1` (con 2 giocatori vale `1`, cioè l'ultimo giocatore).
  - In `partita.iniziaGioco()`:
    1. Applica `primaCarta.getEffetto().attivaEffetto(this)`.
    2. Se la prima carta era un `+2`, `getProssimoGiocatore()` (partendo da `1` in senso orario) è `Giocatore1` (indice `0`): `Giocatore1` pesca 2 carte e viene impostato `saltaProssimoTurno = true`.
    3. Subito dopo chiama `aggiornaGiocatoreAttivo()`:
       - Per carta numerica / `Effetto_Nullo`: avanza da `1` a `0` → inizia `Giocatore1`.
       - Per `+2` o `Blocca Turno`: avanza da `1` a `0` e poi salta a `1` → `Giocatore1` salta il turno e inizia `Giocatore2`.
       - Per `Cambia Giro`: inverte `sensoOrario` a `false` e poi avanza in senso antiorario.
- **Caso B — La prima carta richiede input** (`Cambia Colore` o `+4`):
  - `Partita` imposta `indiceGiocatoreAttivo = 0` (`Giocatore1`).
  - Il costruttore di `Partita` valorizza `effettoInizialeInAttesa`.
  - `iniziaGioco()` vede `effettoInizialeInAttesa != null`, quindi **non** applica l'effetto né avanza il turno.
  - Al primo giro della CLI, `richiediStato()` espone `richiestaInput != null` (preso da `partita.getRichiestaInputIniziale()`).
  - La CLI chiede il colore a `Giocatore1` e chiama `controller.rispondiRichiestaInputIniziale(risposta)`:
    - Se valida: azzera `effettoInizialeInAttesa`, applica `effetto.attivaEffetto(this, risposta)` (che per `+4` cambia il colore e fa pescare 4 carte al `prossimoGiocatore`, cioè `Giocatore2`, segnalando salto turno), e chiama `aggiornaGiocatoreAttivo()`.

---

### Flusso 2 — Ciclo Principale della CLI e Snapshot dello Stato

A ogni iterazione del `while (true)` in `UnoLegendsCli.main`:
1. Chiama `StatoPartitaDto stato = controller.richiediStato()`.
2. **Se `stato.isPartitaTerminata()`**: stampa il vincitore (`stato.getNomeVincitore()`) e termina il programma.
3. **Se `stato.getRichiestaInput() != null`**: risolve la richiesta iniziale pendente tramite `richiediInput(...)` e `controller.rispondiRichiestaInputIniziale(risposta)`, poi `continue`.
4. Stampa lo stato del turno (`mostraStato(stato)`): nome giocatore attivo, carta in cima agli scarti (formattata con il colore attivo se carta nera risolta), mano numerata da `0` a `N-1`.
5. **Se `stato.isDeveGiocareCartaPescata()`**: mostra `cartaAppenaPescata` e consente solo di inserire un indice carta da giocare oppure `'p'` per chiamare `controller.passaTurno()`.
6. **Altrimenti (turno normale)**: mostra il menu `0) gioca carta`, `1) pesca carta`, `2) esci`.

---

### Flusso 3 — Giocare una Carta (`giocaCarta`) — Tutti gli 8 Passaggi

Quando l'utente seleziona una carta (indice `i`), la CLI invoca `gestisciGiocata(i, controller, reader)`:

1. **Prima chiamata**: `EsitoGiocataDto esito = controller.giocaCarta(indiceCarta, null)`.
2. **Dentro `Partita.giocaCarta(indiceCarta, rispostaInput)`** (ordine atomico per non alterare lo stato in caso di errore/attesa):
   - **Caso 1 (Guardie)**: se `vincitore != null || effettoInizialeInAttesa != null || giocatoreInAttesaDichiarazioneUno != null` → ritorna `EsitoGiocata.completata(false)`.
   - **Caso 2 (Indice valido)**: legge `cartaSelezionata = giocatoreAttivo.getCartaInPosizione(indiceCarta)` e registra `avevaDueCarte = (giocatoreAttivo.getMano().size() == 2)`. Se `cartaSelezionata == null` → `completata(false)`.
   - **Caso 3 (Compatibilità)**: chiama `regole.isGiocabile(giocatoreAttivo, cartaSelezionata, cartaInCima, this)`. Se `false` → `completata(false)` (mano intatta).
   - **Caso 4 (Richiesta input mancante)**: ottiene `effetto = cartaSelezionata.getEffetto()` e `richiesta = effetto.getRichiestaInput(this)`. Se `richiesta != null && rispostaInput == null` → ritorna `EsitoGiocata.inAttesa(richiesta)` **senza** rimuovere la carta dalla mano.
   - **Caso 5 (Validazione risposta)**: chiama `effetto.validaRispostaInput(rispostaInput, this)`. Se `false`: ritorna `inAttesa(richiesta)` se era previsto un input, altrimenti `completata(false)`.
   - **Mutazione dello stato (punto di non ritorno)**:
     - `giocatoreAttivo.estraiCarta(indiceCarta)`
     - `pilaDegliScarti.aggiungiCarta(cartaSelezionata)`
   - **Caso 6 (Aggiornamento colore)**: se `cartaSelezionata.getColore() != Colore.NERO`, imposta `coloreAttuale = cartaSelezionata.getColore()`. (Se è `NERO`, il colore verrà aggiornato dall'effetto `Effetto_Cambia_colore`).
   - **Reset stato post-pesca**: `cartaAppenaPescata = null; cartaPescataDaGiocare = false;`.
   - **Attivazione effetto**: `effetto.attivaEffetto(this, rispostaInput)`.
   - **Caso 7 (Vittoria)**: se `regole.isVincitore(giocatoreAttivo, this)` (mano vuota) → imposta `vincitore = giocatoreAttivo` e ritorna `EsitoGiocata.completata(true)` senza avanzare il turno.
   - **Caso 8 (Finestra UNO)**: se `avevaDueCarte && giocatoreAttivo.getMano().size() == 1` → salva `giocatoreInAttesaDichiarazioneUno = giocatoreAttivo` e ritorna `EsitoGiocata.richiedeDichiarazioneUno()` **senza** avanzare ancora il turno.
   - **Avanzamento turno standard**: chiama `aggiornaGiocatoreAttivo()` e ritorna `EsitoGiocata.completata(true)`.

---

### Flusso 4 — Carte con Input Interattivo (`Cambia Colore` e `+4`)

Quando la carta giocata ha `getRichiestaInput(partita) != null`:
1. La prima chiamata `controller.giocaCarta(indiceCarta, null)` ritorna `EsitoGiocataDto(giocata=false, richiestaInput!=null, richiedeDichiarazioneUno=false)`.
2. In `UnoLegendsCli.gestisciGiocata`, il ciclo `while (esito.getRichiestaInput() != null)`:
   - Stampa `messaggio` e `opzioni` numerate (`0) Rosso`, `1) Verde`, `2) Blu`, `3) Giallo`).
   - Legge l'indice digitato dall'utente e lo traduce nella stringa dell'opzione scelta (oppure `""` se indice non valido).
   - Richiama `controller.giocaCarta(indiceCarta, risposta)` passando lo **stesso `indiceCarta`** e la stringa scelta.
3. Se la stringa non è valida, `Partita` ritorna di nuovo `EsitoGiocata.inAttesa(richiesta)` e il `while` richiede l'input.
4. Se la stringa è valida:
   - Per **`Cambia Colore`**: `Effetto_Cambia_colore.attivaEffetto(partita, risposta)` chiama `partita.applicaColoreScelto(colore)`.
   - Per **`+4`**: `EffettoComposito.attivaEffetto(partita, risposta)` esegue nell'ordine:
     1. `Effetto_Cambia_colore.attivaEffetto(partita, risposta)` → cambia `coloreAttuale`.
     2. `Effetto_pescata_carte(4, PIU_QUATTRO).attivaEffetto(partita)` → fa pescare 4 carte al `prossimoGiocatore` tramite `applicaPescaForzata` e chiama `partita.segnalaSaltoTurno()`.

---

### Flusso 5 — Dichiarazione UNO e Penalità

1. Se `EsitoGiocataDto.isRichiedeDichiarazioneUno()` è `true`, il turno in `Partita` è sospeso (`giocatoreInAttesaDichiarazioneUno != null`).
2. `UnoLegendsCli.attendiDichiarazioneUno(reader)` apre una finestra di **5 secondi** controllando `reader.ready()` ogni 50 ms:
   - Se l'utente digita `"uno"` (case-insensitive) entro 5 secondi → `unoDichiarato = true`.
   - Altrimenti (input diverso o timeout) → `unoDichiarato = false`.
3. La CLI chiama `controller.completaDichiarazioneUno(unoDichiarato)` → `partita.completaDichiarazioneUno(unoDichiarato)`:
   - Recupera `giocatore = giocatoreInAttesaDichiarazioneUno` e azzera il campo.
   - Chiama `regole.mancatoUno(giocatore, unoDichiarato, this)`:
     - Se `giocatore.getMano().size() == 1 && !unoDichiarato`: invoca `StrategiaPenalitaMancatoUno.applica(giocatore, partita)` che chiama `partita.applicaPescaForzata(giocatore, 2, PENALITA_UNO)` e ritorna `true`.
     - Altrimenti ritorna `false`.
   - Chiama `aggiornaGiocatoreAttivo()` per passare finalmente il turno.
4. Se il valore ritornato alla CLI è `true`, la CLI stampa il messaggio di penalità.

---

### Flusso 6 — Pesca Volontaria, Passa Turno e Rimescolamento Automatico

1. **Pesca volontaria (`controller.pescaCarta()` → `partita.pescaCarta()`)**:
   - Rifiutata (`false`) se `vincitore != null || cartaPescataDaGiocare || giocatoreInAttesaDichiarazioneUno != null`.
   - Invoca `regole.pescaVolontaria(getGiocatoreAttivo(), this)`; con le regole standard `StrategiaPescaSingola` chiama `pescaSingola(giocatore)`:
     - Chiama `mazzo.prelevaCarta()`.
     - Se `null` (mazzo vuoto), chiama `mazzo.rimescolaDaScarti(pilaDegliScarti)` (che mantiene intatta la carta in cima agli scarti e rimescola le sottostanti) e riprova `mazzo.prelevaCarta()`.
     - Se trova una carta, la aggiunge alla mano del giocatore (`giocatore.aggiungiCarta(carta)`).
   - Imposta `cartaAppenaPescata` all'ultima carta pescata (o `null`) e `cartaPescataDaGiocare = true`.
   - Ritorna `true` (anche se il mazzo ed eventuali scarti erano completamente vuoti).
2. **Passa turno (`controller.passaTurno()` → `partita.passaTurno()`)**:
   - Accettato **solo** se `vincitore == null && cartaPescataDaGiocare && giocatoreInAttesaDichiarazioneUno == null`.
   - Azzera `cartaAppenaPescata = null`, `cartaPescataDaGiocare = false`, e invoca `aggiornaGiocatoreAttivo()`.

---

### Flusso 7 — Pesca Forzata e Avanzamento / Salto / Inversione del Turno

1. **Pesca forzata (`partita.applicaPescaForzata(Giocatore, int, OriginePesca)`)**:
   - Unico punto d'ingresso per tutte le pesche forzate: `Effetto_pescata_carte` (`+2`, `+4`) e `StrategiaPenalitaMancatoUno`.
   - Costruisce un `ContestoPesca` e chiede a `regole.calcolaCartePescaForzata(contesto)` quante carte pescare davvero.
   - Chiama `pescaSingola(bersaglio)` quel numero di volte, fermandosi se mazzo e scarti sono esauriti.
   - Ritorna la lista delle carte effettivamente pescate.
2. **Calcolo prossimo giocatore e avanzamento turno**:
   - `getProssimoGiocatore()` calcola `(indice + 1) % n` se `sensoOrario`, oppure `(indice - 1 + n) % n` se antiorario, **senza** modificare `indiceGiocatoreAttivo`.
   - `aggiornaGiocatoreAttivo()` chiama `avanzaGiocatore()`; se `saltaProssimoTurno == true`, azzera il flag e chiama `avanzaGiocatore()` una **seconda volta**.
   - `cambioGiro()` inverte `sensoOrario = !sensoOrario`.

---

## 6. Tabella Riassuntiva dei Design Pattern (GoF & GRASP)

| Pattern | Tipo | Classi nel Codice | Ruolo Effettivo |
|---|---|---|---|
| **Facade / Controller** | GoF / GRASP | `UnoLegendsGame` | Espone API semplificate alla UI e disaccoppia `ui` da `model`. |
| **DTO (Data Transfer Object)** | Architetturale | `StatoPartitaDto`, `EsitoGiocataDto`, `RichiestaInputDto` | Trasporta dati immutabili privi di dipendenze dal dominio verso la UI. |
| **Coordinator** | GRASP | `Partita` | Coordina turni, stato partita, delega a regole, effetti ed expert. |
| **Information Expert** | GRASP | `Giocatore`, `Mazzo`, `PilaDegliScarti`, `Effetto_Cambia_colore` | Ogni classe gestisce i dati di cui è esperta (mano, deck, scarti, validazione colore). |
| **Creator / Factory** | GRASP / GoF | `PartitaFactory` | Centralizza la costruzione e il setup iniziale di `Partita`. |
| **Abstract Factory** | GoF | `MazzoFactory`, `StandardMazzoFactory` | Astrae la creazione della famiglia di carte del mazzo. |
| **Singleton** | GoF | `StandardMazzoFactory` | Garantisce un'unica istanza della factory del mazzo standard (`getInstance()`). |
| **Strategy / Protected Variations** | GoF / GRASP | `RegoleDiGioco`, `IStrategia*`, `Ieffetto` | Rende intercambiabili compatibilità carte, pesca, vittoria, sconfitta, penalità UNO ed effetti delle carte. |
| **Composite** | GoF | `EffettoComposito` | Compone più `Ieffetto` in sequenza trattandoli come un unico `Ieffetto` (usato per `+4`). |
| **Null Object** | Design Pattern | `Effetto_Nullo` | Evita controlli `if (effetto != null)` sulle carte numeriche. |

---

## 7. Peculiarità Note e Stato Reale di Implementazione (Utile per Analisi e Refactor)

Per comprendere con precisione ciò che il codice fa *effettivamente* oggi rispetto a eventuali documenti di analisi/diagrammi:

1. **Numero di carte iniziali**: `PartitaFactory.CARTE_PER_GIOCATORE` distribuisce **7 carte** per giocatore (mentre `docs/roles_and_patterns.md` cita ancora 5 carte da una revisione precedente).
2. **Disallineamento `domain_class_diagram.puml` vs codice reale**:
   - Nel codice reale `Partita.giocaCarta` ha firma `(int indiceCarta, String rispostaInput)` (non ha più il parametro `boolean unoDichiarato`, ora gestito nella fase separata `completaDichiarazioneUno(boolean)`).
   - Nel codice reale `CartaNumero` non ha il campo `id : int`.
   - In `Mazzo`, il metodo si chiama `getNumeroCarte()` (non `getNumeroCarteRestanti()`).
   - `Partita` non espone `getMazzoDaPesca()`, `getNumeroCarteNelMazzo()` né `tuttiGliAltriGiocatoriHannoPiuDi(...)`.
3. **Strategia di sconfitta predisposta ma non ancora agganciata**: `RegoleDiGioco` espone `isEliminato(Giocatore, Partita)` con `StrategiaSconfittaStandard` (che ritorna sempre `false`), ma `Partita` attualmente non invoca `regole.isEliminato(...)` durante il turno.
4. **Vincolo "solo carta appena pescata"**: dopo `pescaCarta()`, la CLI guida l'utente a giocare la carta appena pescata o passare, ma `Partita.giocaCarta(indiceCarta, rispostaInput)` attualmente verifica soltanto `regole.isGiocabile(...)` senza controllare che la carta scelta coincida con `cartaAppenaPescata`.
5. **Cambia Giro con 2 giocatori**: `Effetto_cambio_giro` inverte `sensoOrario`, ma con `giocatori.size() == 2` avanzare di `+1` o `-1` modulo `2` seleziona comunque l'altro giocatore (non equivale a un salta turno in 2 giocatori, a differenza della variante UNO 2-player ufficiale).
6. **Gestione eterogenea delle attese di input in `Partita`**:
   - La carta iniziale nera usa stato salvato in `Partita` (`effettoInizialeInAttesa`) + `StatoPartitaDto` + `rispondiRichiestaInputIniziale`.
   - La carta nera giocata nel turno usa `EsitoGiocata.inAttesa` e una seconda chiamata a `giocaCarta(indiceCarta, risposta)` senza stato salvato in `Partita`.
   - La dichiarazione UNO usa stato salvato in `Partita` (`giocatoreInAttesaDichiarazioneUno`) + `EsitoGiocata.richiedeDichiarazioneUno()` + `completaDichiarazioneUno`.
7. **Assenza di `Personaggio` / abilità passive e stacking `+2`/`+4`**: non sono ancora presenti nel codice attuale; `Partita` e `RegoleDiGioco` implementano al momento il solo set di regole UNO standard senza personaggi né accumulo di penalità.
