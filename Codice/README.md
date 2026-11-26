# UnoLegends — Documentazione Tecnica Completa del Codice, Architettura e Flussi Dati

Questo documento descrive **nel dettaglio e in modo fedele al codice sorgente attuale** l'architettura, le classi, lo stato interno, i design pattern e tutti i flussi dati/controllo del progetto **UnoLegends** (situato in `SoftwareEngineering/Codice/src/main/java`).

---

## 1. Avvio e Compilazione

Dalla cartella `SoftwareEngineering/Codice` (PowerShell):

```powershell
javac -d out (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
java -cp out unolegends.ui.UnoLegendsCli
```

La CLI apre un **menu di setup** (modalità, opzioni come Endless, numero di giocatori da 2 a 6 e nomi) e poi avvia una partita hotseat con mazzo standard (108 carte). Dallo Step 3; dettagli e motivazioni in [`docs/step3_terza_iter.md`](docs/step3_terza_iter.md).

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
│  UnoLegendsGame, StatoPartitaDto (record con Fase, Richiesta, Giocatore)│
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
│  Carta, Colore, ValoreCarta,                                            │
│  StatoGiocatore, ModalitaDiGioco, OpzionePartita, ConfigurazionePartita,│
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
  - Contiene il menu iniziale (`0` nuova partita, `1` esce), il **menu di setup** (`configuraPartita`: modalità, una domanda sì/no per ogni opzione configurabile, numero e nomi dei giocatori; se il controller rifiuta le scelte mostra il motivo e ripete) e il **loop principale di gioco**.
  - `mostraStato` stampa anche la tabella dei giocatori (carte in mano, stato, chi è di turno) e il senso di gioco; a fine partita `mostraClassifica` stampa la classifica.
  - A ogni iterazione stampa i messaggi degli eventi (`controller.prelevaMessaggi()`), legge lo stato (`controller.richiediStato()`) e fa un unico `switch` esaustivo su `stato.getFase()`: `turnoNormale`, `turnoDopoPesca`, `gestisciRichiestaInput`, dichiarazione UNO, fine partita.
  - Gestisce i prompt interattivi (`richiediInput`), la finestra temporizzata di 5 secondi per la dichiarazione UNO (`attendiDichiarazioneUno`), il parsing degli indici (`parseIndice`) e la formattazione ANSI delle opzioni colore (`formattaOpzione`).

### 3.2 Package `unolegends.controller`

- **`UnoLegendsGame`** *(GRASP Controller / GoF Facade)*:
  - Mantiene un riferimento `private final Partita partita`.
  - **Menu di setup** (metodi statici, Step 3):
    - `getModalitaDisponibili() : List<String>`, `getOpzioniConfigurabili(String modalita) : List<String>` (le opzioni obbligatorie della modalità non compaiono) e `descriviOpzione(String) : String`;
    - `getMinimoGiocatori()` / `getMassimoGiocatori()`;
    - `verificaImpostazioni(String modalita, Set<String> opzioni, List<String> nomi) : String`: converte le stringhe in `ModalitaDiGioco` / `OpzionePartita`, chiama `ConfigurazionePartita.verifica(...)` e traduce l'eventuale `ErroreConfigurazione` in frase; restituisce `null` se le scelte sono valide.
    - `avviaPartita(String modalita, Set<String> opzioni, List<String> nomi) : UnoLegendsGame`: crea la `ConfigurazionePartita`, chiama `PartitaFactory.creaPartita(config)` e `iniziaGioco()`. Le scelte vanno verificate prima: se non sono valide lancia `IllegalArgumentException` (errore di programmazione della UI).
    - Nessun DTO e nessuna classe di eccezione per il setup: solo stringhe, e l'errore come valore restituito.
    - Sostituisce il vecchio `avviaPartitaStandard()`.
  - API esposte alla UI:
    - `richiediStato() : StatoPartitaDto` — costruisce uno snapshot testuale immutabile della partita; formatta la carta in cima con `coloreAttuale` se è una carta nera il cui colore è già stato scelto (basta controllare il colore: nessun `instanceof`).
    - `giocaCarta(int indiceCarta) : boolean` — inoltra a `partita.giocaCarta(...)`; `true` se la giocata è accettata (completata oppure in attesa di input).
    - `rispondiInput(String risposta) : boolean` — unico punto per rispondere a qualunque richiesta di input; inoltra a `partita.rispondiInput(...)`.
    - `completaDichiarazioneUno(boolean unoDichiarato) : boolean` — inoltra a `partita.completaDichiarazioneUno(...)`; `true` se il comando è accettato (la penalità arriva come evento).
    - `pescaCarta() : boolean` — inoltra a `partita.pescaCarta()`.
    - `passaTurno() : boolean` — inoltra a `partita.passaTurno()`.
    - `prelevaMessaggi() : List<String>` — preleva gli `EventoPartita` registrati da `Partita` e li traduce in frasi (`descriviEvento`, con `switch` esaustivi su `TipoEvento` e `OriginePesca`).
  - Conversioni private: `convertiFase` (`FaseTurno` → `StatoPartitaDto.Fase`), `convertiRichiesta` + `messaggioPer` (`TipoRichiesta` → frase), `formattaCarta`, `convertiGiocatori` + `descriviStato` (`Giocatore` → `StatoPartitaDto.Giocatore`), `nomeModalita` / `nomeOpzione` / `descrizioneOpzione`, `messaggioPer(ErroreConfigurazione)`.
- **`StatoPartitaDto`** *(DTO immutabile)*:
  - È un `record` ed è l'**unico DTO** del progetto. Componenti: `fase` (`Fase`), `nomeGiocatoreAttivo` (`String`), `mano` (`List<String>`, copia immutabile), `cartaInCima` (`String`), `cartaAppenaPescata` (`String`, solo in `POST_PESCA`), `richiestaInput` (`Richiesta`, solo in `ATTESA_INPUT`), `giocatori` (`List<Giocatore>`), `sensoOrario` (`boolean`), `classifica` (`List<String>`, solo in `TERMINATA`). Il metodo `nomeVincitore()` restituisce il primo della classifica.
  - Tipi interni (prima erano i file `FaseTurnoDto`, `RichiestaInputDto`, `GiocatoreDto`):
    - `StatoPartitaDto.Fase` *(enum)*: copia di `FaseTurno` per la UI, che non può importare il model;
    - `StatoPartitaDto.Richiesta` *(record)*: `messaggio` (scritto dal controller), `opzioni`;
    - `StatoPartitaDto.Giocatore` *(record)*: `nome`, `numeroCarte`, `stato` (già tradotto: "in gioco", "terminato al posto n. X", "eliminato"), `diTurno`.

### 3.3 Package `unolegends.model` — Entità Core

- **`Carta`** *(classe concreta e `final`)*:
  - Campi: `colore`, `valore`, `effetto`.
  - Costruttori: `Carta(Colore, ValoreCarta, Ieffetto)` e `Carta(Colore, ValoreCarta)`, che usa `Effetto_Nullo` (carte numeriche).
  - `toTestoBase()` produce `"<valore> <colore minuscolo>"` (es. `"7 rosso"`); `getDescrizioneBreve()` applica la formattazione ANSI di `Colore`.
  - L'overload `getDescrizioneBreve(Colore coloreVisualizzato)` è usato dal controller per mostrare una carta nera (`cambia colore` o `+4`) con il colore scelto dal giocatore.
  - **Perché una sola classe:** prima c'erano la classe astratta `Carta` e le sottoclassi `CartaNumero` e `CartaSpeciale`, identiche tranne per il fatto che `CartaNumero` usava sempre `Effetto_Nullo`. Il comportamento di una carta dipende solo dal suo `Ieffetto` (Strategy + Null Object), quindi l'ereditarietà non aggiungeva nulla. Con una sola classe le carte numeriche con effetto della No Mercy (0 e 7) non richiedono classi nuove, e sparisce l'unico `instanceof` del codice.
- **`Colore`** *(enum)*:
  - Valori: `ROSSO` (`\u001B[31m`), `VERDE` (`\u001B[32m`), `BLU` (`\u001B[34m`), `GIALLO` (`\u001B[33m`), `NERO` (`\u001B[30m`).
  - `formatta(String testo)` racchiude il testo tra il codice ANSI del colore e `ANSI_RESET` (`\u001B[0m`).
- **`ValoreCarta`** *(enum)*:
  - Numeriche: `ZERO("0")` .. `NOVE("9")`.
  - Azione: `PIU_DUE("+2")`, `BLOCCA_TURNO("blocca turno")`, `CAMBIA_GIRO("cambia giro")`.
  - Nere speciali: `CAMBIA_COLORE("cambia colore")`, `PIU_QUATTRO("+4")` (in No Mercy il +4 esiste anche colorato).
  - No Mercy (Step 4): azioni colorate `SCARTA_TUTTO`, `SALTA_TUTTI`; jolly `CAMBIO_GIRO_PIU_QUATTRO("+4 cambio giro")`, `PIU_SEI("+6")`, `PIU_DIECI("+10")`, `ROULETTE("roulette colore")`.
  - Utility statiche: `fromInt(int)` (0..9) e `fromString(String)` (case-insensitive).
- **`Giocatore`** *(GRASP Information Expert)*:
  - Campi: `nome` (`String`), `mano` (`List<Carta>`), `stato` (`StatoGiocatore`, iniziale `IN_GIOCO`).
  - Stato: `getStato()`, `isInGioco()`; `segnaComeTerminato()`, `segnaComeEliminato()` e `svuotaMano()` sono package-private, usati solo da `Partita`.
  - Metodi: `getNome()`, `getMano()` (restituisce `Collections.unmodifiableList(mano)`), `getCartaInPosizione(int)` (ritorna `null` se indice fuori range), `estraiCarta(int)` (rimuove e ritorna la carta, o `null` se fuori range), `rimuoviCarta(Carta)` (rimuove proprio quella carta, per identità; usato quando la giocata si completa dopo una risposta), `aggiungiCarta(Carta)`.
- **`Mazzo`** *(GRASP Information Expert)*:
  - Campo: `carte` (`Deque<Carta>` implementata con `ArrayDeque`).
  - `prelevaCarta()` esegue `carte.pollFirst()` (ritorna `null` se vuoto).
  - `isVuoto()`, `getNumeroCarte()`, `mischia()` (copia in `ArrayList`, `Collections.shuffle`, reinserisce nella `Deque`).
  - `rimescolaDaScarti(PilaDegliScarti)`: estrae tutte le carte dagli scarti tranne quella in cima, le aggiunge al mazzo e chiama `mischia()`.
  - `aggiungiCarteERimescola(Collection<Carta>)`: rimette in circolo le carte di un giocatore eliminato.
- **`StatoGiocatore`** *(enum, Step 3)*: `IN_GIOCO`, `TERMINATO` (ha finito le carte e ha un posto in classifica), `ELIMINATO`. Si chiama `IN_GIOCO` e non `ATTIVO` perché "giocatore attivo" indica già il giocatore di turno.
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
- **`Effetto_Nullo`** *(Null Object)*: `attivaEffetto` vuoto. Usato da tutte le carte numeriche (`new Carta(colore, valore)`).
- **`Effetto_Blocca_Turno`**: chiama `partita.segnalaSaltoTurno()`.
- **`Effetto_cambio_giro`**: chiama `partita.cambioGiro()`.
- **`Effetto_pescata_carte`**:
  - Campi: `numeroCarteDaPescare` (`int`: 2, 4, 6 o 10) e `origine` (`OriginePesca`).
  - `attivaEffetto(Partita)`: dallo Step 5 chiama solo `partita.applicaPenalitaPesca(numeroCarteDaPescare, origine)`. L'effetto **non sa se lo stacking è attivo**: senza stacking `Partita` fa pescare subito il successivo e gli fa saltare il turno (come prima), con lo stacking aggiunge all'accumulo.
  - `getValorePenalita()` restituisce `numeroCarteDaPescare` (Step 5).
- **`Effetto_Cambia_colore`**:
  - `attivaEffetto(Partita)` (senza risposta) lancia `IllegalStateException`.
  - `getRichiestaInput(Partita)` ritorna `RichiestaInput(TipoRichiesta.SCELTA_COLORE, ["Rosso", "Verde", "Blu", "Giallo"])`.
  - `validaRispostaInput(String, Partita)` controlla se la stringa (case-insensitive, trimmed) corrisponde a uno dei 4 colori (escluso `NERO`).
  - `attivaEffetto(Partita, String)` converte la stringa in `Colore` e chiama `partita.applicaColoreScelto(colore)`.
- **Effetti No Mercy (Step 4)** — tutto il lavoro sta nell'effetto, che riceve `Partita` (scelta 4.B):
  - **`EffettoScambiaMano`** (carta 7): richiesta `SCELTA_GIOCATORE` fra gli avversari in gioco, poi scambio delle mani con `svuotaMano`/`riceviCarte`. Se la 7 è l'ultima carta non chiede nulla e non scambia (ha vinto).
  - **`EffettoPassaMani`** (carta 0): ogni giocatore in gioco passa la mano al successivo nel senso di gioco (`Partita.getGiocatoriInGiocoNelSensoDiGioco()`). Se lo 0 è l'ultima carta non fa nulla.
  - **`EffettoScartaColore`** (Scarta tutto): toglie dalla mano di chi gioca tutte le carte del colore attivo e le mette negli scarti **sotto** la carta in cima (`Partita.scartaSottoLaCima`), senza attivarne gli effetti.
  - **`EffettoTurnoExtra`** (Salta tutti): `Partita.concediTurnoExtra()`; il giocatore rigioca un turno completo.
  - **`EffettoRoulette`** (jolly roulette): riusa `Effetto_Cambia_colore` per scegliere il colore, poi `Partita.applicaPescaFinoA(vittima, carta -> colore uguale, ROULETTE)` e `segnalaSaltoTurno()`. Un jolly nero non conta come carta del colore scelto.
  - `Ieffetto.getValorePenalita()` (default `0`, Step 5): valore di pesca usato dallo stacking. Vale il numero di carte per le carte di pesca, 0 per tutte le altre (roulette compresa, che non si accumula).
  - `Ieffetto.isAmmessoComeCartaIniziale()` (default `true`): i quattro effetti 7, 0, Scarta tutto e Salta tutti restituiscono `false`, così `PartitaFactory` non li scopre come prima carta.
- **`EffettoComposito`** *(GoF Composite)*:
  - Campo: `List<Ieffetto> effetti`.
  - Usato per la carta `+4`, che compone `[Effetto_Cambia_colore, Effetto_pescata_carte(4)]`, e per i jolly No Mercy: `+4 cambio giro` = `[Effetto_Cambia_colore, Effetto_cambio_giro, Effetto_pescata_carte(4)]`, `+6`/`+10` = `[Effetto_Cambia_colore, Effetto_pescata_carte(6/10)]`.
  - `isAmmessoComeCartaIniziale()` è vero solo se lo è per tutti i sotto-effetti.
  - `getValorePenalita()` è la somma dei valori dei sotto-effetti (Step 5): +4 cambio giro vale 4, +6 vale 6.
  - `getRichiestaInput(Partita)` restituisce la prima richiesta non nulla tra i sotto-effetti.
  - `validaRispostaInput(String, Partita)` delega la validazione al primo sotto-effetto con richiesta di input.
  - `attivaEffetto(Partita, String)` esegue tutti i sotto-effetti in ordine: passa `risposta` al primo sotto-effetto che richiede input, e chiama `attivaEffetto(partita)` senza argomenti su tutti gli altri.
- **`RichiestaInput`**: oggetto valore immutabile del dominio con `tipo` (`TipoRichiesta`) e `opzioni` (`List<String>`). Non contiene testo per l'utente.
- **`TipoRichiesta`** *(enum)*: `SCELTA_COLORE`, `SCELTA_GIOCATORE` (usato dalla carta 7 dallo Step 4, e allo Step 6 dalle abilità attive).
- **`AzioneInAttesa`** *(record package-private, GoF Command)*: `(Ieffetto effetto, Consumer<String> completamento)`. L'effetto sa cosa chiedere e validare; il completamento è ciò che `Partita` esegue quando arriva una risposta valida.
- **`FaseTurno`** *(enum)*: `NORMALE`, `POST_PESCA`, `ATTESA_INPUT`, `ATTESA_UNO`, `RISPOSTA_STACKING` (Step 5), `TERMINATA`. Vedi sezione 4.
- **`Accumulo`** *(record immutabile, Step 5)*: `(int totale, int valoreUltimaCarta, int numeroCarte, OriginePesca originePrimaCarta)`. Accumulo dello stacking; `Partita` lo sostituisce con `aggiungi(valore)` a ogni risposta. `getOriginePenalita()` restituisce l'origine della carta se l'accumulo è una carta sola, altrimenti `ACCUMULO` (servirà a La Roccia, immune solo al +2 singolo).
- **`EventoPartita`** *(record)* + **`TipoEvento`** *(enum)*: registro dei fatti accaduti (`PESCA_VOLONTARIA`, `PESCA_FORZATA`, `TURNO_SALTATO`, `SENSO_INVERTITO`, `COLORE_SCELTO`, `UNO_DICHIARATO`, `GIOCATORE_TERMINATO` con la posizione in `quantita`, `GIOCATORE_ELIMINATO`, dallo Step 4 `MANI_SCAMBIATE`, `MANI_PASSATE`, `CARTE_SCARTATE`, `TURNO_EXTRA`, e dallo Step 5 `ACCUMULO_AUMENTATO` con il totale in `quantita` e `PENALITA_ACCETTATA`). Campi: `tipo`, `nomeGiocatore`, `quantita`, `origine`, `colore`, `altroGiocatore` (solo per lo scambio di mani); si creano con factory statiche package-private.
- **`OriginePesca`** *(enum)*: causa di una pesca (`VOLONTARIA`, `PIU_DUE`, `PIU_QUATTRO`, `PENALITA_UNO`, dallo Step 4 `PIU_SEI`, `PIU_DIECI`, `ROULETTE`, e dallo Step 5 `ACCUMULO` per un accumulo di due o più carte). Permette alle regole (e in futuro alle passive dei Legend) di distinguere le pescate.
- **`ContestoPesca`** *(record immutabile)*: `(Giocatore bersaglio, int numeroCarte, OriginePesca origine)`, passato a `RegoleDiGioco.calcolaCartePescaForzata(...)`.

### 3.5 Package `unolegends.model` — Factory e Contenitore Regole

- **`MazzoFactory`** *(Abstract Factory)*: interfaccia con `Mazzo creaMazzo()`.
- **`StandardMazzoFactory`** *(Concrete Factory + Singleton)*:
  - Istanza statica eager `INSTANCE` esposta via `getInstance()`.
  - `creaMazzo()` costruisce **108 carte** e chiama `mazzo.mischia()`:
    - Per ciascuno dei 4 colori (`ROSSO`, `GIALLO`, `VERDE`, `BLU`):
      - 1 copia di `0` (`Carta` senza effetto)
      - 2 copie per ogni numero da `1` a `9` (`Carta` senza effetto) → 19 carte numeriche per colore
      - 2 copie di `PIU_DUE` (`Carta` con `Effetto_pescata_carte(2, PIU_DUE)`)
      - 2 copie di `BLOCCA_TURNO` (`Carta` con `Effetto_Blocca_Turno`)
      - 2 copie di `CAMBIA_GIRO` (`Carta` con `Effetto_cambio_giro`) → 6 carte azione per colore
    - Totale carte colorate: `4 × 25 = 100`.
    - Carte nere (`Colore.NERO`):
      - 4 copie di `CAMBIA_COLORE` (`Effetto_Cambia_colore`)
      - 4 copie di `PIU_QUATTRO` (`EffettoComposito([Effetto_Cambia_colore, Effetto_pescata_carte(4, PIU_QUATTRO)])`)
    - Totale complessivo: **108 carte**.
- **`NoMercyMazzoFactory`** *(Concrete Factory + Singleton, Step 4)*: **168 carte**, composizione scritta in un unico punto con costanti con nome; `creaMazzo()` controlla il totale.
    - Per ciascuno dei 4 colori: 2 copie di ogni numero 0–9 (lo 0 con `EffettoPassaMani`, il 7 con `EffettoScambiaMano`, gli altri senza effetto) → 80 numeriche;
      3 blocca turno, 3 cambia giro, 3 `+2`, 2 `+4` colorati, 3 scarta tutto, 2 salta tutti → 64 azioni colorate.
    - Jolly: 8 `+4 cambio giro`, 8 roulette colore, 4 `+6`, 4 `+10` → 24.
- **`ModalitaDiGioco`** *(enum di soli dati, Step 3)*: per ogni modalità la `MazzoFactory` da usare e le `OpzionePartita` obbligatorie (`getOpzioniObbligatorie()`, `isConfigurabile(opzione)`). Contiene `NORMALE` (`StandardMazzoFactory`) e `NO_MERCY` (`NoMercyMazzoFactory`, Step 4), che ha `STACKING` come opzione obbligatoria (Step 5). Sostituisce l'enum `TipoMazzo`, che non era usato.
- **`OpzionePartita`** *(enum di soli dati, Step 3)*: regole opzionali scelte dal menu. Oggi `ENDLESS` e `STACKING` (Step 5: configurabile nella partita normale, obbligatoria in No Mercy); `LEGEND` (Step 6) si aggiungerà quando funzionerà.
- **`ConfigurazionePartita`** *(record, Step 3)*: `(modalita, nomiGiocatori, opzioni)`. Il costruttore toglie gli spazi dai nomi, aggiunge le opzioni obbligatorie della modalità e valida (2–6 giocatori, nomi non vuoti e non duplicati, Endless con almeno 3 giocatori). La validazione è il metodo statico `verifica(modalita, nomi, opzioni) : ErroreConfigurazione` (`null` = valida; enum, nessun testo nel model); il costruttore lo richiama e, se i dati non sono validi, lancia una normale `IllegalArgumentException`.
- **`PartitaFactory`** *(GRASP Creator / GoF Factory)*:
  - Costante `CARTE_PER_GIOCATORE = 7`.
  - `creaPartita(ConfigurazionePartita)`: mazzo da `config.modalita().getMazzoFactory()`, regole da `creaRegole(config)` (uno `switch` esaustivo sulla modalità: `NORMALE -> RegoleDiGioco.standard(...)`, `NO_MERCY -> RegoleDiGioco.noMercy(...)`), giocatori con i nomi scelti.
  - `scopriPrimaCarta(mazzo)`: se l'effetto della prima carta non è ammesso a inizio partita (`isAmmessoComeCartaIniziale()`), la rimette nel mazzo, rimescola e ne scopre un'altra.
  - `creaPartita(MazzoFactory, List<String> nomiGiocatori, RegoleDiGioco)`: crea il mazzo, distribuisce 7 carte a ciascun giocatore, pesca la `primaCarta` per inizializzare `PilaDegliScarti` e istanzia `Partita` (il giocatore iniziale lo decide `Partita`).
- **`RegoleDiGioco`** *(Protected Variations / Contenitore di Strategy)*:
  - Contiene 7 strategie, tutte iniettate dal costruttore:
    1. `IStrategiaCondizioneVittoria`
    2. `IStrategiaCondizioneSconfitta`
    3. `IStrategiaCompatibilitaCarte`
    4. `IStrategiaPesca` (pesca **volontaria**)
    5. `IStrategiaPenalitaUno`
    6. `IStrategiaFinePartita` (Step 3)
    7. `IStrategiaAccumuloPenalita` (Step 5, stacking)
  - Metodi esposti:
    - `isGiocabile(Giocatore giocatore, Carta candidata, Carta inCima, Partita partita)` → se c'è un accumulo dello stacking delega a `strategiaAccumulo.puoRispondere(candidata, accumulo)`, altrimenti a `strategiaCompatibilitaCarte.puoGiocare(candidata, inCima, partita.getColoreAttuale())`. Il giocatore servirà alle passive che modificano la compatibilità.
    - `isAccumuloAttivo()` (Step 5) → se le carte di pesca si accumulano.
    - `isVincitore(Giocatore, Partita)` → delega a `strategiaVittoria` (ha finito le carte: in Endless vuol dire "prende il prossimo posto in classifica").
    - `isPartitaTerminata(Partita)` → delega a `strategiaFinePartita`.
    - `isEliminato(Giocatore, Partita)` → delega a `strategiaSconfitta`.
    - `pescaVolontaria(Giocatore, Partita)` → delega alla strategia di pesca volontaria.
    - `calcolaCartePescaForzata(ContestoPesca)` → numero di carte che il bersaglio deve pescare davvero; oggi restituisce `contesto.numeroCarte()`, in futuro vi si agganciano le passive.
    - `mancatoUno(Giocatore giocatore, boolean unoDichiarato, Partita partita)` → se `mano.size() == 1 && !unoDichiarato`, applica `strategiaPenalitaUno` e ritorna `true`; altrimenti `false`.
    - `standard(Set<OpzionePartita> opzioni)` → regole della partita normale; ogni opzione sostituisce una strategia (`ENDLESS` → `StrategiaFinePartitaEndless`). `standard()` equivale a `standard(Set.of())`.
    - `noMercy(Set<OpzionePartita> opzioni)` (Step 4) → come `standard` ma con `StrategiaPescaFinoAGiocabile` e `StrategiaSconfittaNoMercy(SOGLIA_ELIMINAZIONE_NO_MERCY = 25)`. Le opzioni Endless e Stacking valgono per entrambe (`finePartita(opzioni)`, `accumulo(opzioni)`).
    - Non c'è un Builder: `RegoleDiGioco.standard(opzioni)` è più leggibile; allo Step 5 (7 strategie) il team ha confermato di restarne senza, da rivalutare allo Step 6 con le passive.

### 3.6 Package `unolegends.domain.rules` e `unolegends.domain.rules.impl`

| Interfaccia | Implementazione Standard | Logica Implementata |
|---|---|---|
| `IStrategiaCompatibilitaCarte` | `StrategiaCompatibilitaCarte` | Ritorna `false` se `cartaGiocata == null`; `true` se `cartaInCima == null` o `cartaGiocata.getColore() == Colore.NERO`; altrimenti confronta `cartaGiocata.getColore() == coloreDaConfrontare` (dove `coloreDaConfrontare = coloreAttivo != null ? coloreAttivo : cartaInCima.getColore()`) oppure `cartaGiocata.getValoreCarta() == cartaInCima.getValoreCarta()`. |
| `IStrategiaCondizioneVittoria` | `StrategiaVittoriaStandard` | `giocatore != null && giocatore.getMano().isEmpty()`. |
| `IStrategiaCondizioneSconfitta` | `StrategiaSconfittaStandard` | Ritorna sempre `false` (nessuna eliminazione nelle regole standard). |
| `IStrategiaPesca` (pesca volontaria) | `StrategiaPescaSingola` | Chiama `partita.pescaSingola(giocatore)` e ritorna `List.of(carta)` (o `List.of()` se mazzo e scarti sono vuoti). |
| `IStrategiaPenalitaUno` | `StrategiaPenalitaMancatoUno` | Chiama `partita.applicaPescaForzata(giocatore, numeroCarte, OriginePesca.PENALITA_UNO)` (default `2` carte). |
| `IStrategiaFinePartita` | `StrategiaFinePartitaStandard` | `terminati >= 1 \|\| inGioco <= 1`: finisce appena uno finisce le carte. |
| `IStrategiaFinePartita` | `StrategiaFinePartitaEndless` | `inGioco <= 1`: si continua finché resta un solo giocatore. |
| `IStrategiaPesca` (No Mercy) | `StrategiaPescaFinoAGiocabile` | Pesca una carta alla volta finché `partita.isCartaGiocabile(...)`, o finché mazzo e scarti sono esauriti, o finché `partita.isDaEliminare(...)`. |
| `IStrategiaCondizioneSconfitta` (No Mercy) | `StrategiaSconfittaNoMercy(25)` | `mano.size() >= 25`: Mercy rule. |
| `IStrategiaAccumuloPenalita` | `StrategiaNessunAccumulo` | `isAccumuloAttivo()` = `false`: ogni carta di pesca si applica subito. |
| `IStrategiaAccumuloPenalita` (opzione Stacking) | `StrategiaAccumuloValoreCrescente` | `isAccumuloAttivo()` = `true`; `puoRispondere` = `valorePenalita > 0 && valorePenalita >= accumulo.valoreUltimaCarta()`. Il colore non conta. Una sola strategia per entrambe le modalità: con il mazzo normale dà "+2 ← +2/+4, +4 ← +4". |

---

## 4. Stato Interno di `Partita` e Macchina a Fasi del Turno

`Partita` (in `unolegends.model.Partita`) è il **GRASP Coordinator / radice del dominio**. Dallo Step 2 lo stato del turno è descritto da **un solo campo `fase`** (`FaseTurno`) invece che da più variabili sparse. Campi:

| Campo | Tipo | Significato e ciclo di vita |
|---|---|---|
| `giocatori` | `List<Giocatore>` | Lista ordinata dei giocatori partecipanti (copia difensiva), da 2 a 6. Restano tutti nella lista anche quando escono: lo dice il loro `StatoGiocatore`. |
| `mazzo` | `Mazzo` | Mazzo da cui pescare le carte. |
| `pilaDegliScarti` | `PilaDegliScarti` | Pila delle carte giocate; l'ultima è la carta in cima. |
| `regole` | `RegoleDiGioco` | Profilo di strategie di gioco attivo. |
| `eventi` | `List<EventoPartita>` | Registro degli eventi accaduti dall'ultima lettura; svuotato da `prelevaEventi()`. |
| `indiceGiocatoreAttivo` | `int` | Indice in `giocatori` del giocatore di turno (`0 .. n-1`). |
| `fase` | `FaseTurno` | Fase corrente del turno (vedi tabella sotto). |
| `azioneInAttesa` | `AzioneInAttesa` | Valorizzata solo in `ATTESA_INPUT`: effetto da completare e completamento da eseguire. |
| `cartaAppenaPescata` | `Carta` | Ultima carta pescata volontariamente; esposta solo in `POST_PESCA`. |
| `terminati` | `List<Giocatore>` | Chi ha finito le carte, nell'ordine in cui ha finito (Step 3). |
| `eliminati` | `List<Giocatore>` | Chi è stato eliminato, in ordine (Step 3; nessuno con le regole attuali). |
| `classifica` | `List<Giocatore>` | Classifica finale, valorizzata insieme a `fase = TERMINATA`: terminati, poi chi era in gioco (meno carte = meglio), poi eliminati dal più recente. Il vincitore è il primo. Sostituisce il campo `vincitore`. |
| `giocoIniziato` | `boolean` | Rende `iniziaGioco()` idempotente (evita la doppia applicazione dell'effetto iniziale). |
| `saltaProssimoTurno` | `boolean` | Non è una fase: è un'informazione per il prossimo avanzamento, impostata da `segnalaSaltoTurno()` e consumata da `aggiornaGiocatoreAttivo()`. |
| `accumulo` | `Accumulo` | Accumulo dello stacking in corso (Step 5), `null` se non c'è. Lo apre o lo fa crescere `applicaPenalitaPesca`, lo azzera `accettaPenalita`. |
| `turniExtra` | `int` | Turni extra del giocatore di turno (Salta tutti, Step 4): `aggiornaGiocatoreAttivo()` lo consuma lasciando il turno allo stesso giocatore, se è ancora in gioco. |
| `coloreAttuale` | `Colore` | Colore attivo per la compatibilità delle carte. Inizializzato al colore della prima carta; aggiornato sulle giocate colorate o tramite `applicaColoreScelto(Colore)` (package-private) sulle carte nere. |
| `sensoOrario` | `boolean` | Direzione di avanzamento turni (default `true`, invertito da `cambioGiro()`). |

Rimossi nello Step 2: `cartaPescataDaGiocare`, `effettoInizialeInAttesa`, `giocatoreInAttesaDichiarazioneUno` (il giocatore in attesa di UNO è sempre quello attivo, perché il turno non è ancora avanzato).

### Fasi del turno e comandi permessi

Ogni comando pubblico dichiara le fasi in cui è **permesso** (lista di ammissione): una fase nuova viene rifiutata da tutti i comandi finché non la si abilita.

| Fase | Comandi accettati | Si entra con | Si esce con |
|---|---|---|---|
| `NORMALE` | `giocaCarta`, `pescaCarta` | fine del turno precedente senza accumulo | giocata, pesca |
| `POST_PESCA` | `giocaCarta` (qualsiasi carta della mano), `passaTurno` | `pescaCarta` | giocata, `passaTurno` |
| `ATTESA_INPUT` | `rispondiInput` | carta iniziale o carta giocata che richiede input | risposta valida → il completamento decide la fase |
| `ATTESA_UNO` | `completaDichiarazioneUno` | giocata dopo la quale resta in mano una sola carta **che il giocatore aveva già** (`deveDichiarareUno`: vale anche per Scarta tutto, non dopo uno scambio o una rotazione di mani) | dichiarazione (con o senza penalità) |
| `RISPOSTA_STACKING` | `giocaCarta` (solo carte di pesca di valore ≥ ultima carta dell'accumulo, colore libero), `accettaPenalita` | `chiudiTurno()` con un accumulo in corso (Step 5) | risposta (l'accumulo passa al successivo) o accettazione (pesca il totale e il turno passa) |
| `TERMINATA` | nessuno | `IStrategiaFinePartita` dice che la partita è finita (uno finisce le carte, o in Endless ne resta uno solo) | — |

**Mercy rule (Step 4).** `verificaEliminazioni()` elimina chi ha raggiunto la soglia; è chiamata in punti sicuri, cioè dopo che effetto o pesca sono stati applicati per intero (`completaGiocata`, `pescaCarta`, `completaDichiarazioneUno`, `iniziaGioco`). Se l'eliminato è il giocatore che doveva saltare il turno, il salto si annulla; se è il giocatore di turno, il turno passa. `chiudiTurno()` non fa nulla a partita terminata. `accettaPenalita()` è un altro punto sicuro (Step 5).

**Stacking (Step 5).** `chiudiTurno()` è l'unico punto che decide la fase del nuovo turno: `RISPOSTA_STACKING` se c'è un accumulo, altrimenti `NORMALE`. Così l'accumulo passa al successivo qualunque sia il motivo per cui il turno si chiude. Anche `iniziaGioco()` chiude con `chiudiTurno()`, quindi una carta iniziale di pesca apre l'accumulo e il primo giocatore risponde. Con 2 giocatori il +4 cambio giro fa tornare l'accumulo a chi l'ha giocato, perché il salto lo chiede il cambio giro (decisione del team, diversa dalla regola ufficiale).

Quando un giocatore finisce le carte, `completaGiocata` chiama `registraTerminato` (stato `TERMINATO`, evento `GIOCATORE_TERMINATO`, `verificaFinePartita`). Se la partita non è finita (Endless) il turno passa al prossimo **in gioco** con `chiudiTurno()`.

---

## 5. Flussi Dati e Dinamiche Passo-Passo

### Flusso 1 — Avvio della Partita e Gestione della Prima Carta

```text
UnoLegendsCli.main
  └─> configuraPartita(reader)              // modalità, opzioni, numero e nomi
  └─> UnoLegendsGame.verificaImpostazioni(modalita, opzioni, nomi)   // null = valide
  └─> UnoLegendsGame.avviaPartita(modalita, opzioni, nomi)
        ├─> new ConfigurazionePartita(modalita, nomi, opzioni)
        ├─> PartitaFactory.creaPartita(config)
        │     ├─> config.modalita().getMazzoFactory().creaMazzo() -> 108 carte mescolate
        │     ├─> creaRegole(config) -> RegoleDiGioco.standard(config.opzioni())
        │     ├─> distribuisciCarteAiGiocatori(mazzo, nomi) -> 7 carte a ciascuno
        │     ├─> primaCarta = mazzo.prelevaCarta()
        │     ├─> pilaDegliScarti = new PilaDegliScarti([primaCarta])
        │     └─> new Partita(...) -> coloreAttuale = primaCarta.getColore();
        │                             indiceGiocatoreAttivo = giocatori.size() - 1
        └─> partita.iniziaGioco()
```

- **Caso A — La prima carta NON richiede input** (carta numerica, `+2`, `Blocca Turno`, `Cambia Giro`):
  1. `iniziaGioco()` applica `primaCarta.getEffetto().attivaEffetto(this)`.
  2. (Esempio con 2 giocatori.) Se la prima carta era un `+2`, `getProssimoGiocatore()` (partendo da `1` in senso orario) è `Giocatore1`: pesca 2 carte (evento `PESCA_FORZATA`) e viene impostato `saltaProssimoTurno = true`.
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
   - `TERMINATA` → `mostraClassifica`: stampa vincitore e classifica ed esce;
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
4. **Carte finite**: se `regole.isVincitore(...)` → `registraTerminato` (stato `TERMINATO`, evento, controllo `isPartitaTerminata`). Se la partita è finita la fase è `TERMINATA`; altrimenti (Endless) `chiudiTurno()`.
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
   - Unico punto d'ingresso per tutte le pesche forzate: carte di pesca (tramite `applicaPenalitaPesca`), accettazione dell'accumulo (`accettaPenalita`) e `StrategiaPenalitaMancatoUno`.
   - Costruisce un `ContestoPesca` e chiede a `regole.calcolaCartePescaForzata(contesto)` quante carte pescare davvero.
   - Chiama `pescaSingola(bersaglio)` quel numero di volte, fermandosi se mazzo e scarti sono esauriti.
   - Registra **un** evento `PESCA_FORZATA` con le carte effettivamente pescate e l'origine.
2. **Avanzamento turno**:
   - Unico helper privato `trovaProssimoIndiceInGioco(daIndice)`: avanza di ±1 (modulo `n`) nel senso di gioco **saltando i giocatori non `IN_GIOCO`**. Lo usano sia `getProssimoGiocatore()` (che non modifica `indiceGiocatoreAttivo`) sia `avanzaGiocatore()`. Prima ognuno usava `(i ± 1) % n`, che dava per scontato che nessuno uscisse.
   - `aggiornaGiocatoreAttivo()` (privato) chiama `avanzaGiocatore()`; se `saltaProssimoTurno`, azzera il flag, registra `TURNO_SALTATO` e avanza una **seconda volta**.
   - `cambioGiro()` inverte `sensoOrario` e registra `SENSO_INVERTITO`; con **2 giocatori in gioco** chiama anche `segnalaSaltoTurno()` (regola ufficiale, dallo Step 4 in entrambe le modalità).
   - Penalità delle carte di pesca (Step 5): `applicaPenalitaPesca(n, origine)` (package-private) chiede `regole.isAccumuloAttivo()`. Se no, `applicaPescaForzata(getProssimoGiocatore(), n, origine)` + `segnalaSaltoTurno()`; se sì, `accumulo = Accumulo.apri(...)` oppure `accumulo.aggiungi(n)` ed evento `ACCUMULO_AUMENTATO`, senza pescare né saltare.
   - Pesche forzate "a ripetizione": `applicaPescaFinoA(bersaglio, condizione, origine)` (roulette), unico punto come `applicaPescaForzata`; non passa da `calcolaCartePescaForzata` (niente sconti Salesman).
3. **Lettura degli eventi**: `Partita.prelevaEventi()` restituisce una copia del registro e lo svuota; `UnoLegendsGame.prelevaMessaggi()` traduce ogni evento in frase.

---

## 6. Tabella Riassuntiva dei Design Pattern (GoF & GRASP)

| Pattern | Tipo | Classi nel Codice | Ruolo Effettivo |
|---|---|---|---|
| **Facade / Controller** | GoF / GRASP | `UnoLegendsGame` | Espone API semplificate alla UI e disaccoppia `ui` da `model`. |
| **DTO (Data Transfer Object)** | Architetturale | `StatoPartitaDto` (con i tipi interni `Fase`, `Richiesta`, `Giocatore`, `Accumulo`) | Trasporta dati immutabili privi di dipendenze dal dominio verso la UI. |
| **Coordinator** | GRASP | `Partita` | Coordina turni, stato partita, delega a regole, effetti ed expert. |
| **Information Expert** | GRASP | `Giocatore`, `Mazzo`, `PilaDegliScarti`, `Effetto_Cambia_colore` | Ogni classe gestisce i dati di cui è esperta (mano, deck, scarti, validazione colore). |
| **Creator / Factory** | GRASP / GoF | `PartitaFactory`, `RegoleDiGioco.standard(opzioni)` | Centralizza la costruzione e il setup iniziale di `Partita`; è l'unico punto che sceglie le classi concrete in base a modalità e opzioni. |
| **Abstract Factory** | GoF | `MazzoFactory`, `StandardMazzoFactory`, `NoMercyMazzoFactory` | Astrae la creazione della famiglia di carte del mazzo. |
| **Singleton** | GoF | `StandardMazzoFactory`, `NoMercyMazzoFactory` | Garantisce un'unica istanza della factory del mazzo standard (`getInstance()`). |
| **Strategy / Protected Variations** | GoF / GRASP | `RegoleDiGioco`, `IStrategia*`, `Ieffetto` | Rende intercambiabili compatibilità carte, pesca, vittoria, sconfitta, penalità UNO, fine partita (normale/Endless), stacking (Step 5) ed effetti delle carte. |
| **Composite** | GoF | `EffettoComposito` | Compone più `Ieffetto` in sequenza trattandoli come un unico `Ieffetto` (`+4`, `+4 cambio giro`, `+6`, `+10`). |
| **Null Object** | Design Pattern | `Effetto_Nullo` | Evita controlli `if (effetto != null)` sulle carte numeriche e permette di avere una sola classe `Carta`. |
| **Command** | GoF | `AzioneInAttesa` | Incapsula un'azione sospesa (effetto + completamento) da eseguire quando arriva la risposta del giocatore. |
| **Pure Fabrication / Indirection** | GRASP | `EventoPartita`, `TipoEvento` | Registro eventi: il model registra cosa succede senza sapere chi lo leggerà; il controller lo traduce in testo. |

---

## 7. Peculiarità Note e Stato Reale di Implementazione (Utile per Analisi e Refactor)

Per comprendere con precisione ciò che il codice fa *effettivamente* oggi rispetto a eventuali documenti di analisi/diagrammi:

1. **Numero di carte iniziali**: `PartitaFactory.CARTE_PER_GIOCATORE` distribuisce **7 carte** per giocatore (mentre `docs/roles_and_patterns.md` cita ancora 5 carte da una revisione precedente).
2. **Disallineamento `domain_class_diagram.puml` vs codice reale**:
   - Nel codice reale `Partita.giocaCarta` ha firma `(int indiceCarta) : boolean`: la risposta agli input passa da `rispondiInput(String)` e la dichiarazione UNO da `completaDichiarazioneUno(boolean)`.
   - In `Mazzo`, il metodo si chiama `getNumeroCarte()` (non `getNumeroCarteRestanti()`).
   - `Partita` non espone `getMazzoDaPesca()`, `getNumeroCarteNelMazzo()` né `tuttiGliAltriGiocatoriHannoPiuDi(...)`.
3. **Eliminazione (Mercy rule)**: dallo Step 4 `Partita.verificaEliminazioni()` chiama `eliminaGiocatore(...)` per chi raggiunge le 25 carte in No Mercy; nella partita normale `StrategiaSconfittaStandard` non elimina mai.
4. **Dopo la pesca si gioca qualsiasi carta**: in `POST_PESCA` `Partita.giocaCarta` accetta ogni carta compatibile della mano, non solo `cartaAppenaPescata` (regola scelta per il progetto).
5. **Cambia Giro con 2 giocatori in gioco**: dallo Step 4 vale come un blocco (regola ufficiale, decisione del team), in entrambe le modalità e anche in Endless quando restano in due. Con 3 o più giocatori inverte il senso.
6. **Attese di input unificate (Step 2)**: carta iniziale nera e carta nera giocata nel turno passano entrambe da `AzioneInAttesa` + `rispondiInput(String)`. La dichiarazione UNO resta un comando separato (`completaDichiarazioneUno`) perché ha il timer di 5 secondi ed è un'interazione diversa da "scegli fra opzioni".
7. **Assenza di Legend / abilità passive**: non sono ancora presenti nel codice attuale (Step 6). Lo stacking c'è dallo Step 5: sempre attivo in No Mercy, opzione nella partita normale.
