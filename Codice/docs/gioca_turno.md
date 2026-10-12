# GiocaTurno - Documentazione

Questo documento descrive il flusso dettagliato del caso d'uso *GiocaTurno* come continuazione dello step di configurazione partita (la `Partita` è già pronta e passata a `UnoLegendsGame`). Include la mappatura dei ruoli GRASP/GOF e gli step esatti delle interazioni tra UI, controller e domain.

## Panoramica

- Entry point UI: `UnoLegendsCli` (client)
- Controller / Facade: `UnoLegendsGame` — espone API di stato e comandi e inoltra le richieste d'interazione degli effetti
- Domain root / Coordinator: `Partita` — orchestration delle azioni del turno
- Information Experts: `Giocatore`, `Mazzo`, `PilaDegliScarti`
- DTO per la UI: `StatoPartita` — snapshot immutabile con stringhe, flag ed eventuale richiesta d'interazione

Il diagramma di sequenza è in `docs/gioca_turno.puml`.

## Flusso dettagliato

### 1) `richiediStato()`
- `UnoLegendsCli` chiama `UnoLegendsGame.richiediStato()`
- `UnoLegendsGame` legge da `Partita` le informazioni necessarie allo snapshot:
  - `Giocatore` (nome, mano)
  - `PilaDegliScarti` (carta in cima)
  - vari flag interni (`cartaPescataDaGiocare`, `cartaAppenaPescata`)
  - l'eventuale richiesta d'input dell'effetto della prima carta degli scarti
- Il controller trasforma le carte in stringhe e crea `StatoPartita`.
- La CLI riceve `StatoPartita` e non accede direttamente alle entità del dominio.

### 2) `giocaCarta(indice)`
- `UnoLegendsCli` invoca `UnoLegendsGame.giocaCarta(indice, null, unoDichiarato)`.
- `Partita` verifica la carta e, se il suo effetto richiede un dato, restituisce `EsitoGiocata` con la `RichiestaInput` dichiarata dall'effetto.
- Il controller inoltra la richiesta alla UI. La CLI mostra messaggio e opzioni genericamente, raccoglie la risposta e ritenta la giocata passandola al controller.
- `Partita` delega all'effetto la validazione e l'applicazione della risposta; la CLI non identifica gli effetti né implementa regole specifiche per il colore.
- `Partita`:
  - Interroga il `Giocatore` attivo per ottenere la carta in quella posizione (`getCartaInPosizione`) — potrebbe essere `null`.
  - Se il giocatore ha due carte e gioca una carta, la chiamata include l'eventuale dichiarazione di UNO. Se non è stata dichiarata, `RegoleDiGioco` applica la strategia di penalità dopo la giocata valida.
  - La strategia standard pesca due carte per il giocatore che non ha dichiarato UNO; la pesca forzata riutilizza la strategia di pesca configurata, incluso il rimescolamento degli scarti.
  - Se è presente la regola "deve giocare la carta appena pescata" (`cartaPescataDaGiocare`) controlla che la carta selezionata sia appunto la `cartaAppenaPescata`.
  - Recupera la `cartaInCima` dagli scarti e verifica la compatibilità tramite `StrategiaCompatibilitaCarte`.
  - Se un effetto richiede un input, la giocata resta invariata finché la View non restituisce una risposta valida.
  - Se valida: ordina al `Giocatore` di estrarre la carta (`estraiCarta`) e la passa alla `PilaDegliScarti` (`aggiungiCarta`).
  - L'effetto della carta viene attivato dopo averla aggiunta agli scarti e riceve la risposta precedentemente richiesta; l'effetto cambia-colore applica così il colore scelto alla carta in cima.
  - Ripristina gli stati relativi alla pesca e chiede a `RegoleDiGioco` di verificare la vittoria del giocatore che ha appena giocato.
  - Se ha vinto, conserva il risultato senza avanzare il turno; altrimenti invoca `aggiornaGiocatoreAttivo()`.
  - Ritorna un `EsitoGiocata`: giocata conclusa oppure richiesta d'input da mostrare alla UI.

### Condizioni di vittoria
- `RegoleDiGioco.standard()` usa `StrategiaVittoriaStandard`: vince chi rimane senza carte in mano.
- `RegoleDiGioco.noMercy()` usa `StrategiaVittoriaNoMercy`: mantiene la vittoria per mano vuota e permette inoltre di vincere quando tutti gli avversari hanno più di 25 carte.
- La strategia No Mercy accetta anche una soglia esplicita con `RegoleDiGioco.noMercy(soglia)`.
- `PartitaFactory.creaPartita(tipoMazzo, numeroGiocatori, regole)` permette di associare alla partita il profilo scelto; la factory del mazzo `NO_MERCY` resta non implementata.
- L'esito è incluso in `StatoPartita` e la CLI annuncia il vincitore senza accedere al model.

### Input richiesto dalla carta iniziale
- Se l'effetto della prima carta richiede un input, la factory lascia l'effetto in sospeso.
- La richiesta viene esposta nello snapshot di partita; controller e CLI la inoltrano e la mostrano senza conoscere il tipo di effetto.
- La risposta viene inoltrata all'effetto, che la valida e la applica; solo dopo la partita prosegue.

### 3) `pescaCarta()`
- `UnoLegendsCli` -> `UnoLegendsGame.pescaCarta()` -> `Partita.pescaCarta()`
- `Partita` verifica che non ci sia già una carta pescata non gestita.
- `Partita` chiede a `Mazzo.prelevaCarta()` la prossima carta.
- Se presente, la passa a `Giocatore.aggiungiCarta(carta)`, registra `cartaAppenaPescata` e setta `cartaPescataDaGiocare=true`.
- Ritorna `true`/`false` in base al successo.

### 4) `passaTurno()`
- `UnoLegendsCli` -> `UnoLegendsGame.passaTurno()` -> `Partita.passaTurno()`
- `Partita` controlla che la regola di carta pescata sia attiva; se sì, resetta i flag e chiama `aggiornaGiocatoreAttivo()`.
- Ritorna `true` se il passaggio è avvenuto, `false` altrimenti.

## Ruoli GRASP e pattern GOF rilevanti

- `UnoLegendsGame` — Controller / Facade (GRASP)
  - Crea la partita standard tramite la factory, converte lo stato di gioco e inoltra i comandi e le risposte tra UI e dominio.
  - Non espone alla UI le entità `Carta`, `Colore` o `Partita`.

- `Partita` — Coordinator (GRASP)
  - Ha la responsabilità di orchestrare il turno, chiedere a `RegoleDiGioco` di valutare la vittoria e conservare l'esito della partita.

- `StrategiaVittoriaStandard` e `StrategiaVittoriaNoMercy` — strategie della condizione di vittoria
  - Sono selezionate attraverso `RegoleDiGioco`; il controller e la CLI non implementano le regole.

- `StrategiaPenalitaMancatoUno` — strategia della penalità di mancata dichiarazione
  - Regola standard: pesca due carte; è iniettabile e configurabile separatamente nelle `RegoleDiGioco`.

- `Giocatore`, `Mazzo`, `PilaDegliScarti` — Information Experts (GRASP)
  - Ognuno gestisce il proprio stato e le proprie operazioni.

- `PartitaFactory` — Factory / Creator (GOF / GRASP)
  - Creato separatamente (setup), non parte del flow di turno.

- `StatoPartita` — DTO / Snapshot del controller
  - Espone dati di presentazione immutabili e l'eventuale `RichiestaInput`; non espone entità di dominio.
- `Ieffetto` — può dichiarare una `RichiestaInput`, validare la risposta e attivarsi con essa; la UI presenta la richiesta in modo generico. Un effetto composto supporta al massimo un effetto che richiede input.

## Considerazioni di design e estendibilità

- `Partita` è il punto centrale per regole di validazione del turno — è corretto secondo GRASP (Expert + Controller)
- Se in futuro si aggiungono regole o strategie (house rules, effetti speciali), usare un oggetto `RegoleDiGioco` iniettato in `Partita` (Strategy/Policy)
- `UnoLegendsGame` rimane pulito: non assume responsabilità di creazione o persistenza

## Link utili
- Diagramma sequenza: `docs/gioca_turno.puml`
- Configurazione partita (factory): `docs/configura_partita.puml`
- Mappatura ruoli: `docs/roles_and_patterns.md`

Nota sulla terminologia

- `Utente` (attore): rappresenta la persona che usa il CLI e interagisce con il sistema. Nei diagrammi di sequenza è mostrato come attore esterno che invia comandi.
- `Giocatore` (oggetto dominio): è l'entità interna alla `Partita` che mantiene la mano e le operazioni correlate. Anche se nella vita reale l'utente è il giocatore, nel modello software sono ruoli distinti.

Interazione UI indicizzata

L'interfaccia CLI (`UnoLegendsCli`) espone un menù indicizzato iniziale (es. `0` avvia partita). L'attore `Utente` seleziona l'indice; la CLI inoltra le operazioni esclusivamente a `UnoLegendsGame`, mantenendo la factory e il model fuori dalla view.
