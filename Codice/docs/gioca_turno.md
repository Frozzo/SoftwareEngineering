# GiocaTurno - Documentazione

Questo documento descrive il flusso dettagliato del caso d'uso *GiocaTurno* come continuazione dello step di configurazione partita (la `Partita` è già pronta e passata a `UnoLegendsGame`). Include la mappatura dei ruoli GRASP/GOF e gli step esatti delle interazioni tra UI, controller e domain.

## Panoramica

- Entry point UI: `UnoLegendsCli` (client)
- Controller / Facade: `UnoLegendsGame` — espone API: `richiediStato()`, `giocaCarta(indice)`, `pescaCarta()`, `passaTurno()`
- Domain root / Coordinator: `Partita` — orchestration delle azioni del turno
- Information Experts: `Giocatore`, `Mazzo`, `PilaDegliScarti`
- DTO: `StatoTurno` — snapshot immutabile dello stato utile alla UI

Il diagramma di sequenza è in `docs/gioca_turno.puml`.

## Flusso dettagliato

### 1) `richiediStato()`
- `UnoLegendsCli` chiama `UnoLegendsGame.richiediStato()`
- `UnoLegendsGame` delega a `Partita.getStatoTurno()`
- `Partita` raccoglie informazioni dagli expert:
  - `Giocatore` (nome, mano)
  - `PilaDegliScarti` (carta in cima)
  - vari flag interni (`cartaPescataDaGiocare`, `cartaAppenaPescata`)
- `Partita` costruisce `StatoTurno` e lo ritorna alla UI tramite `UnoLegendsGame`.

### 2) `giocaCarta(indice)`
- `UnoLegendsCli` chiede a `UnoLegendsGame.giocaCarta(indice)`
- `UnoLegendsGame` chiama `Partita.giocaCarta(indice)`
- `Partita`:
  - Interroga il `Giocatore` attivo per ottenere la carta in quella posizione (`getCartaInPosizione`) — potrebbe essere `null`.
  - Se è presente la regola "deve giocare la carta appena pescata" (`cartaPescataDaGiocare`) controlla che la carta selezionata sia appunto la `cartaAppenaPescata`.
  - Recupera la `cartaInCima` dagli scarti e verifica la compatibilità (`compatibileCon`).
  - Se valida: ordina al `Giocatore` di estrarre la carta (`estraiCarta`) e la passa alla `PilaDegliScarti` (`aggiungiCarta`).
  - Ripristina gli stati relativi alla pesca e invoca `aggiornaGiocatoreAttivo()`.
  - Ritorna `true` se la mossa è avvenuta, `false` altrimenti.

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
  - Non crea `Partita`; espone solo API per l'interazione UI→dominio.

- `Partita` — Coordinator (GRASP)
  - Ha la responsabilità di orchestrare il turno e delegare agli expert appropriati.

- `Giocatore`, `Mazzo`, `PilaDegliScarti` — Information Experts (GRASP)
  - Ognuno gestisce il proprio stato e le proprie operazioni.

- `PartitaFactory` — Factory / Creator (GOF / GRASP)
  - Creato separatamente (setup), non parte del flow di turno.

- `StatoTurno` — DTO / Snapshot
  - Ha alta coesione: solo getter e dati immutabili per la UI.

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

L'interfaccia CLI (`UnoLegendsCli`) espone un menù indicizzato iniziale (es. `0` avvia partita). L'attore `Utente` seleziona l'indice; il programma CLI interpreta la scelta e inoltra il comando a `PartitaFactory` o a `UnoLegendsGame`. Questo rende facile estendere il menù con nuove opzioni in futuro.

