Configura Partita - Mappatura ruoli e pattern

Breve mappatura delle classi coinvolte nel processo di configurazione iniziale della partita.

Classi e responsabilità

- PartitaFactory
  - Pattern: Factory (GOF), Creator (GRASP)
  - Responsabilità: orchestrare la creazione di `Partita` e degli oggetti necessari (Giocatore, Mazzo, PilaDegliScarti).
  - Step reali del codice: invoca `StandardMazzoFactory.getInstance()` per ottenere il catalogo (`getCarteNormali()`), mescola, distribuisce 5 carte a testa, costruisce il mazzo residuo usando `creaMazzo(source, startIndex, size)`, inizializza gli scarti con la prima carta disponibile e crea `Partita` con `RegoleStandard`.

- MazzoFactory
  - Pattern: Abstract Factory (GOF)
  - Responsabilità: fornire il catalogo delle carte della famiglia corrente e costruire il `Mazzo` a partire da una lista già pronta.
  - Firma/contratto attuale nel codice: `getCarteNormali()` restituisce la lista catalogo; `creaMazzo(source, startIndex, size)` costruisce il `Mazzo` da una porzione della lista.

- StandardMazzoFactory
  - Pattern: Concrete Factory, Singleton
  - Responsabilità: generare le carte standard `0-9` per ogni colore e costruire il mazzo standard.
  - Implementazione: `StandardMazzoFactory` è un singleton (campo `instance` e metodo `getInstance()`), espone `getCarteNormali()` e `creaMazzo(...)` per la costruzione effettiva del `Mazzo`.

- CarteCatalogo
  - Stato attuale: non presente nel codice.
  - La sua responsabilità è stata assorbita da `StandardMazzoFactory`.

- Giocatore
  - Pattern: Information Expert
  - Responsabilità: gestire la propria mano e operazioni correlate (aggiungi/estrai carta).

- Mazzo
  - Pattern: Information Expert
  - Responsabilità: contenere il deck e prelevare carte (`prelevaCarta`).

- PilaDegliScarti
  - Pattern: Information Expert
  - Responsabilità: contenere gli scarti e fornire `getCartaInCima` e `aggiungiCarta`.

- Partita
  - Pattern: Coordinator / Controller interno (GRASP)
  - Responsabilità: orchestrare le azioni di gioco (`giocaCarta`, `pescaCarta`, `passaTurno`) delegando agli expert.

- RegoleStandard
  - Pattern: Strategy / Policy di default
  - Responsabilità: validare la giocata base in modo coerente con la `RegoleDiGioco`.

- UnoLegendsGame
  - Pattern: Facade / Controller (GRASP)
  - Responsabilità: esporre API semplici alla UI (`giocaCarta`, `pescaCarta`, `richiediStato`). Non esegue setup.
  - Responsabilità: esporre API semplici alla UI (`giocaCarta`, `pescaCarta`, `richiediStato`). Mantiene un riferimento persistente a `Partita` (associazione 1), quindi agisce come facciata delegando le chiamate direttamente all'istanza di `Partita`.

- UnoLegendsCli
  - Pattern: Client / UI
  - Responsabilità: entry point che richiede a `PartitaFactory` una `Partita` pronta e costruisce `UnoLegendsGame`.

Note architetturali

- Il singleton presente oggi è `StandardMazzoFactory`.
- Il design è pronto per estensioni:
  - Supportare N giocatori modificando `PartitaFactory`
  - House rules e regole di setup iniettando una `HouseRulesConfig` nella factory
  - Altre famiglie di mazzi con ulteriori concrete factory

Nota sulla terminologia — attore vs oggetto dominio

- `Utente` (attore): rappresenta la persona che usa l'interfaccia (CLI). Nei diagrammi di sequenza l'attore è esterno al sistema e invia comandi.
- `Giocatore` (oggetto dominio): è l'entità modellata nel dominio, creata dalla `PartitaFactory` e gestita internamente. L'attore esterno e l'oggetto dominio possono rappresentare la stessa persona nel mondo reale, ma sono ruoli distinti nella modellazione del sistema.

Nota sull'interazione UI

- L'interfaccia CLI (`UnoLegendsCli`) espone un semplice menù indicizzato. L'attore `Utente` seleziona un indice; il programma CLI interpreta l'indice e invoca la factory o il controller appropriato. Questo rende il flusso estendibile per future opzioni.

