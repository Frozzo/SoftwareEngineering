Design Class Diagram & Class Design - Decision log

Scopo

Questo documento riassume le scelte di modellazione per il dominio delle carte e le scelte GRASP / GOF per classi astratte, factory, strategy e polimorfismo.

Sintesi delle scelte

- `Carta` (abstract)
  - Motivazione: `Carta` è la superclasse astratta che fornisce l'identificativo comune (`id`) e il contratto polimorfo per il comportamento delle carte.
  - Metodi chiave: `getId()`, `compatibileCon(Carta cartaInCima)`, `toCliString()`.

- `CartaNumero` (concrete)
  - Attributi: `colore`, `numero`.
  - Implementa `compatibileCon` con la logica standard (stesso colore o stesso numero) e `toCliString` per la rappresentazione CLI.

- `CartaSpeciale` (future)
  - Potrà estendere `Carta` per effetti speciali o regole passive.

- `RegoleDiGioco` (interface / Strategy)
  - Motivazione: la logica che decide se una carta è giocabile può cambiare con house rules e abilità passive. Incapsularla in una interfaccia permette di cambiare policy senza modificare `Partita`.
  - Implementazione attuale: `RegoleStandard`.

- `PartitaFactory` (Factory / Creator)
  - Centralizza la creazione della `Partita`, dei `Giocatore`, del `Mazzo` e della `PilaDegliScarti`.
  - Recupera le carte dalla concrete factory, mescola, distribuisce, costruisce il mazzo residuo, inizializza gli scarti e crea la `Partita` completa.

- `MazzoFactory` (abstract factory)
  - Definisce il contratto per ottenere il catalogo delle carte della famiglia corrente e costruire un `Mazzo` da una lista già pronta.
  - `StandardMazzoFactory` è la concrete factory singleton attuale.
 - `MazzoFactory` (abstract factory)
   - Definisce il contratto per ottenere il catalogo delle carte della famiglia corrente e costruire un `Mazzo` da una lista già pronta. Le firme principali usate nel codice sono `getCarteNormali()` e `creaMazzo(source, startIndex, size)`.
   - `StandardMazzoFactory` è la concrete factory singleton attuale; espone `getInstance()` per l'accesso globale.

- `CarteCatalogo`
  - Non esiste più come classe autonoma nel codice corrente.
  - La sua responsabilità è stata assorbita da `StandardMazzoFactory`.

- `Partita`
  - Coordinator / Information Expert: orchestrazione del turno; mantiene riferimento a `RegoleDiGioco` per valutare se una carta è giocabile.
  - Stato interno rilevante: `turnoCorrente` (indice del giocatore attivo). Lo stato di runtime visibile al client è rappresentato dalla classe `StatoTurno` che espone, tra gli altri, `nomeGiocatoreAttivo` e `deveGiocareCartaPescata`.

- `UnoLegendsGame`
  - Facade / Controller per la UI.
  - Facade / Controller per la UI. Nota: `UnoLegendsGame` mantiene un'associazione persistente a `Partita` (multiplicità 1) — non è solo una dipendenza temporanea.

Design evolution notes

- Se in futuro serviranno famiglie di mazzi diverse:
  - Introduci ulteriori concrete factory, per esempio `NoMercyMazzoFactory`, lasciando invariato il resto del setup.

- Se vuoi separare ulteriormente le responsabilità:
  - Introduci `DistribuzioneStrategy` (Strategy) per cambiare come distribuisci le carte ai giocatori.
  - Introduci `HouseRulesConfig` per iniettare regole in `PartitaFactory` e `Partita`.

Refactor applicato

- La costruzione del mazzo standard è stata spostata in `StandardMazzoFactory`.
- `PartitaFactory` usa `StandardMazzoFactory` per ottenere il catalogo standard e costruire il mazzo.
- Tutte le strutture (`Giocatore`, `Mazzo`, `PilaDegliScarti`, `Partita`, `StatoTurno`) continuano a usare `Carta` come tipo comune.

Prossimi passi consigliati

- Se servono altri mazzi, aggiungere una nuova concrete factory.
- Eventualmente estrarre `DistribuzioneStrategy` per rendere `PartitaFactory` configurabile.

File generati / aggiornati

- `docs/domain_class_diagram.puml` — diagramma UML di classi aggiornato.
- `docs/domain_model.md` — spiegazione aggiornata delle decisioni.
