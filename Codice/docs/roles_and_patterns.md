Configura Partita - Mappatura ruoli e pattern

Breve mappatura delle classi coinvolte nel processo di configurazione iniziale della partita.

Classi e responsabilità

- PartitaFactory
  - Pattern: Factory (GOF), Creator (GRASP)
  - Responsabilità: orchestrare la creazione di `Partita` e degli oggetti necessari (Giocatore, Mazzo, PilaDegliScarti).
  - Accetta un `RegoleDiGioco` esplicito per configurare le strategie usate dalla partita.
  - Step reali del codice: invoca `StandardMazzoFactory.getInstance()` per creare un mazzo già mescolato, distribuisce 5 carte a testa tramite `prelevaCarta()`, inizializza gli scarti con la prima carta disponibile e crea `Partita` con `RegoleStandard`.

- MazzoFactory
  - Pattern: Abstract Factory (GOF)
  - Responsabilità: creare un `Mazzo` completo della famiglia corrente tramite `creaMazzo()`.

- StandardMazzoFactory
  - Pattern: Concrete Factory, Singleton
  - Responsabilità: generare le carte standard `0-9` per ogni colore e costruire il mazzo standard.
  - Implementazione: `StandardMazzoFactory` è un singleton (campo `instance` e metodo `getInstance()`); `creaMazzo()` costruisce e mescola il `Mazzo`.


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
  - Responsabilità: configurare le strategie delle regole standard; `StrategiaVittoriaStandard` dichiara vincitore chi rimane senza carte.

- StrategiaVittoriaStandard
  - Pattern: Strategy (GOF)
  - Responsabilità: valutare la condizione di vittoria in base alle regole configurate, senza coinvolgere la UI.

- StrategiaPenalitaMancatoUno
  - Pattern: Strategy (GOF)
  - Responsabilità: applicare la penalità configurata quando una giocata valida lascia due carte in mano ridotte a una senza dichiarazione UNO.

- UnoLegendsGame
  - Pattern: Facade / Controller (GRASP)
  - Responsabilità: esporre API semplici alla UI, tradurre stato e richieste in DTO del controller, tradurre gli eventi della partita in messaggi e coordinare i comandi di gioco con la `Partita`.
  - Mantiene un riferimento persistente a `Partita` (associazione 1); le API UI-facing non espongono tipi del model.

- StatoPartitaDto (record con i tipi interni Fase, Richiesta e Giocatore: dallo Step 3 è l'unico DTO)
  - Responsabilità: trasferire alla UI snapshot (con la fase del turno) e richieste interattive usando solo dati immutabili indipendenti dal model.

- FaseTurno, AzioneInAttesa, EventoPartita / TipoEvento (Step 2)
  - FaseTurno: un solo campo descrive la fase del turno; ogni comando di `Partita` dichiara le fasi in cui è permesso.
  - AzioneInAttesa (Command): azione sospesa in attesa di input, completata da `Partita.rispondiInput`.
  - EventoPartita (Pure Fabrication): registro dei fatti accaduti, letto dalla UI tramite il controller.

- UnoLegendsCli
  - Pattern: View / UI
  - Responsabilità: raccogliere input e presentare lo stato usando esclusivamente l'API e i DTO esposti dal controller; non importa classi del model.

- Legend (Step 6) — dettagli in `step6_sesta_iter.md`
  - Legend (enum di soli dati): collega passiva, abilità attiva e cooldown di ogni personaggio.
  - IPassiva + Passiva* (Strategy, Polymorphism, Protected Variations): ogni passiva ridefinisce un solo hook; le interrogano RegoleDiGioco e pochi punti di Partita, senza sapere quale passiva hanno davanti.
  - AbilitaAttiva: un Ieffetto più la ricarica del singolo giocatore (le abilità riusano il meccanismo degli input delle carte).
  - EffettoConBersaglio (Template Method): scelta del bersaglio fra `Partita.getBersagliSelezionabili()`; sottoclassi EffettoScambiaMano, EffettoPescaBersaglio, EffettoRegalaCarte, EffettoCongela.
  - Giocatore (Information Expert): conosce passive, abilità e congelamento; risponde a `isBersagliabile()` e `partecipaAiTurni()`.

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

- L'interfaccia CLI (`UnoLegendsCli`) espone un semplice menù indicizzato. L'attore `Utente` seleziona un indice; la CLI inoltra le operazioni esclusivamente a `UnoLegendsGame`, mantenendo la factory e il model fuori dalla view.
