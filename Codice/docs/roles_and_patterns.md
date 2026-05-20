Configura Partita - Mappatura ruoli e pattern

Breve mappatura delle classi coinvolte nel processo di configurazione iniziale della partita.

Classi e responsabilità

- PartitaFactory
  - Pattern: Factory (GOF), Creator (GRASP)
  - Responsabilità: orchestrare la creazione di `Partita` e degli oggetti necessari (Giocatore, Mazzo, PilaDegliScarti)
  - Motivazione: centralizza il setup iniziale, mantiene alta coesione e basso accoppiamento con UI/Controller.

- MazzoFactory
  - Pattern: Factory (GOF)
  - Responsabilità: creare il `Mazzo` a partire da un set di carte. Separando questa responsabilità, `PartitaFactory` può concentrarsi sulla composizione della partita e delegare dettaglio di costruzione del deck a `MazzoFactory`.

- CarteCatalogo
  - Pattern: Utility / Expert
  - Responsabilità: conoscere il set completo delle carte disponibili (static factory)

- Giocatore
  - Pattern: Information Expert
  - Responsabilità: gestire la propria mano e operazioni correlate (aggiungi/estrai carta)

- Mazzo
  - Pattern: Information Expert
  - Responsabilità: contenere il deck e prelevare carte (prelevaCarta)

- PilaDegliScarti
  - Pattern: Information Expert
  - Responsabilità: contenere gli scarti e fornire `getCartaInCima` e `aggiungiCarta`

- Partita
  - Pattern: Coordinator / Controller interno (GRASP)
  - Responsabilità: orchestrare le azioni di gioco (giocaCarta, pescaCarta, passaTurno) delegando agli expert

- UnoLegendsGame
  - Pattern: Facade / Controller (GRASP)
  - Responsabilità: esporre API semplici alla UI (giocaCarta, pescaCarta, richiediStato). NON esegue setup.

- UnoLegendsCli
  - Pattern: Client / UI
  - Responsabilità: entry point che richiede a `PartitaFactory` una `Partita` pronta e costruisce `UnoLegendsGame`.

Note architetturali

- Al momento non esistono Singletons; se necessario, `CarteCatalogo` potrebbe diventare singleton/enum in futuro, ma ora è solo una classe con metodi statici.
- Il design è pronto per estensioni:
  - Supportare N giocatori (modificare `PartitaFactory`)
  - House rules e regole di setup (iniettare una `HouseRulesConfig` nella factory)
  - Strategie di distribuzione (Strategy pattern)

Nota sulla terminologia — attore vs oggetto dominio

- `Utente` (attore): rappresenta la persona che usa l'interfaccia (CLI). Nei diagrammi di sequenza l'attore è esterno al sistema e invia comandi (es. "crea partita", "gioca carta").
- `Giocatore` (oggetto dominio): è l'entità modellata nel dominio, creata dalla `PartitaFactory` e gestita internamente (ha una mano, può aggiungere/estrarre carte). L'attore esterno e l'oggetto dominio possono rappresentare la stessa persona nel mondo reale, ma sono ruoli distinti nella modellazione del sistema.


Nota sull'interazione UI

- L'interfaccia CLI (`UnoLegendsCli`) espone un semplice menù indicizzato (es. `0` avvia partita). L'attore `Utente` seleziona un indice; il programma CLI interpreta l'indice e invoca la factory o il controller appropriato. Questo rende il flusso facilmente estendibile per future opzioni (house rules, numero giocatori, selezione legends).

