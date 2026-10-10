# GiocaTurno - Documentazione

Questo documento descrive il flusso dettagliato del caso d'uso *GiocaTurno* come continuazione dello step di configurazione partita (la `Partita` è già pronta e passata a `UnoLegendsGame`). Include la mappatura dei ruoli GRASP/GOF e gli step esatti delle interazioni tra UI, controller e domain.

## Panoramica

- Entry point UI: `UnoLegendsCli` (client)
- Controller / Facade: `UnoLegendsGame` — espone API di stato e comandi, coordina la scelta del colore e non restituisce oggetti del dominio alla UI
- Domain root / Coordinator: `Partita` — orchestration delle azioni del turno
- Information Experts: `Giocatore`, `Mazzo`, `PilaDegliScarti`
- DTO di dominio: `StatoTurno` — snapshot interno usato dal dominio e dal controller
- DTO per la UI: `StatoPartita` — snapshot immutabile con stringhe e flag, senza riferimenti a classi del model

Il diagramma di sequenza è in `docs/gioca_turno.puml`.

## Flusso dettagliato

### 1) `richiediStato()`
- `UnoLegendsCli` chiama `UnoLegendsGame.richiediStato()`
- `UnoLegendsGame` delega a `Partita.getStatoTurno()`
- `Partita` raccoglie informazioni dagli expert:
  - `Giocatore` (nome, mano)
  - `PilaDegliScarti` (carta in cima)
  - vari flag interni (`cartaPescataDaGiocare`, `cartaAppenaPescata`)
  - l'eventuale scelta del colore richiesta dalla prima carta degli scarti
- `Partita` costruisce `StatoTurno`; il controller trasforma le carte in stringhe e crea `StatoPartita`.
- La CLI riceve solo `StatoPartita`, senza importare o navigare classi del model.

### 2) `giocaCarta(indice)`
- `UnoLegendsCli` chiede al controller se la carta selezionata richiede un colore e raccoglie l'eventuale indice colore.
- `UnoLegendsCli` invoca `UnoLegendsGame.giocaCarta(indice, sceltaColore)`.
- `UnoLegendsGame` converte l'indice colore nell'enum di dominio e coordina scelta colore e giocata sulla `Partita`.
- `Partita`:
  - Interroga il `Giocatore` attivo per ottenere la carta in quella posizione (`getCartaInPosizione`) — potrebbe essere `null`.
  - Se è presente la regola "deve giocare la carta appena pescata" (`cartaPescataDaGiocare`) controlla che la carta selezionata sia appunto la `cartaAppenaPescata`.
  - Recupera la `cartaInCima` dagli scarti e verifica la compatibilità tramite `StrategiaCompatibilitaCarte`.
  - Se l'effetto richiede un colore, `Partita` verifica che il giocatore abbia effettuato la scelta prima di modificare lo stato della partita.
  - Se valida: ordina al `Giocatore` di estrarre la carta (`estraiCarta`) e la passa alla `PilaDegliScarti` (`aggiungiCarta`).
  - L'effetto della carta viene attivato dopo averla aggiunta agli scarti, quindi il colore scelto diventa il colore della carta in cima per il turno successivo.
  - Ripristina gli stati relativi alla pesca e invoca `aggiornaGiocatoreAttivo()`.
  - Ritorna `true` se la mossa è avvenuta, `false` altrimenti.

### Scelta del colore per la prima carta
- Se la prima carta degli scarti richiede un colore, la factory lascia la scelta in sospeso e rende attivo il primo giocatore.
- La CLI mostra la richiesta e passa l'indice colore a `UnoLegendsGame.scegliColore(indice)`.
- `Partita` delega all'effetto l'applicazione del colore sulla carta in cima; solo dopo la scelta permette di proseguire con il turno.

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
  - Crea la partita standard tramite la factory, converte lo stato di dominio in un DTO per la UI e coordina i comandi UI→dominio.
  - Non espone alla UI `Carta`, `Colore`, `StatoTurno` o altre classi del model.

- `Partita` — Coordinator (GRASP)
  - Ha la responsabilità di orchestrare il turno e delegare agli expert appropriati.

- `Giocatore`, `Mazzo`, `PilaDegliScarti` — Information Experts (GRASP)
  - Ognuno gestisce il proprio stato e le proprie operazioni.

- `PartitaFactory` — Factory / Creator (GOF / GRASP)
  - Creato separatamente (setup), non parte del flow di turno.

- `StatoTurno` — snapshot interno del dominio.
- `StatoPartita` — DTO / Snapshot del controller
  - Espone alla UI solo stringhe e flag immutabili, senza riferimenti agli oggetti del dominio.

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
