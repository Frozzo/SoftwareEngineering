Design Class Diagram & Class Design - Decision log

Scopo

Questo documento spiega le scelte di modellazione per il dominio delle carte e le scelte GRASP / GOF per classi astratte, factory, strategy e polimorfismo.

Sintesi delle scelte

- `Carta` (abstract)
  - Motivazione: `Carta` è la superclasse astratta che fornisce l'identificativo comune (`id`) e il contratto polimorfo per il comportamento delle carte.
  - Metodi chiave: `getId()`, `compatibileCon(Carta cartaInCima)`, `toCliString()`.

- `CartaNumero` (concrete)
  - Attributi: `colore`, `numero`.
  - Implementa `compatibileCon` con la logica standard (stesso colore o stesso numero) e `toCliString` per la rappresentazione CLI.

- `CartaSpeciale` / `CartaUnica` (future)
  - Potranno estendere `Carta` e sovrascrivere `compatibileCon` e `onGiocata` per effetti speciali o regole passive.

- `RegoleDiGioco` (interface / Strategy)
  - Motivazione: la logica che decide se una carta è giocabile può dipendere da house rules e abilità passive. Incapsularla in una interfaccia permette di cambiare policy senza modificare `Partita`.
  - Esempio: `RegoleStandard`, `RegoleHouseX`.
  - Nota: `RegoleDiGioco` è un'estensione tipica dell'Iterazione 2. È documentata e implementabile, ma non strettamente necessaria per l'Iterazione 1 (gioco base senza regole speciali). In Java non ho usato il prefisso `I` per le interfacce (nomenclatura più idiomatica).

- `CarteCatalogo` (static factory)
  - Attualmente è una utility stateless che restituisce liste di carte (`getCarteNormali()`).
  - Non è singleton; se sarà necessario mantenere stato condiviso, si potrà convertire in singleton.

- `PartitaFactory` (Factory)
  - Centralizza la creazione della `Partita`, dei `Giocatore` e del `Mazzo`.
  - Rispettando GRASP Creator: la factory conosce gli ingredienti per costruire `Partita`.

- `MazzoFactory` (Factory)
  - Responsabile della creazione del `Mazzo` a partire da una lista di carte. Questo separa la responsabilita' di "costruire" il mazzo (politiche di selezione, opzioni di house rules in futuro) dalla composizione di alto livello effettuata da `PartitaFactory`.
  - `PartitaFactory` delega a `MazzoFactory` la costruzione del mazzo; questo migliora la coesione e facilita l'estensione.

- `CarteCatalogo` vs `MazzoFactory`
  - `CarteCatalogo` fornisce i prototipi/blueprint delle carte (es. `CartaNumero`, `CartaSpeciale`, `CartaUnica`). Rappresenta "cosa" esiste nel gioco.
  - `MazzoFactory` usa queste carte (o cloni/prototipi) per comporre un `Mazzo` concreto seguendo le regole di setup (distribuzione, filtri, house rules). Questa separazione rende semplice aggiungere nuovi tipi di carta senza cambiare la logica di costruzione del mazzo.

- `Partita`
  - Coordinator / Information Expert: orchestrazione del turno; mantiene riferimento a `RegoleDiGioco` per valutare `compatibileCon` in base alle regole.

- `UnoLegendsGame`
  - Facade / Controller per la UI.

Design evolution notes

- Se vuoi usare singleton per `CarteCatalogo`:
  - Usalo solo se è necessario mantenere stato condiviso (cache/locale/configurazioni). Le funzioni statiche sono più semplici e testabili.

- Se vuoi separare ulteriormente le responsabilit\u0000:
  - Introduci `DistribuzioneStrategy` (Strategy) per cambiare come distribuisci le carte ai giocatori.
  - Introduci `HouseRulesConfig` per iniettare regole in `PartitaFactory` e `Partita`.

Refactor applicato

- Ho applicato un refactor mirato per introdurre `Carta` (astratta) e `CartaNumero` (concreta). Le modifiche al codice sono minime e mantengono compatibilit\u0000 con l'API attuale:
  - `CarteCatalogo.getCarteNormali()` ora crea `CartaNumero`.
  - Tutte le strutture (`Giocatore`, `Mazzo`, `PilaDegliScarti`, `Partita`, `StatoTurno`) continuano a usare `Carta` come tipo comune.

Prossimi passi consigliati

- Aggiungere `RegoleDiGioco` e iniettarle nella `Partita` per supportare house rules e abilità passive.
- Implementare altre classi di carta (`CartaSpeciale`, `CartaUnica`) estendendo `Carta`.
- Eventualmente estrarre `DistribuzioneStrategy` per rendere `PartitaFactory` configurabile.

File generati / aggiornati

- `docs/domain_class_diagram.puml` — diagramma UML di classi aggiornato.
- `docs/domain_model.md` — spiegazione aggiornata delle decisioni.

Vuoi che proceda ora a introdurre `RegoleDiGioco` e un'implementazione `RegoleStandard` nel codice sorgente? Questo permette di separare la logica di validazione da `Partita` e rendere il design più estensibile.
