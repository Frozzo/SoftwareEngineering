# UnoLegends — Roadmap verso la versione finale

**Obiettivo finale:** una partita giocabile da CLI. Dal menu iniziale si sceglie:

- **la modalità:** partita normale oppure partita **No Mercy**;
- **le opzioni**, con una domanda sì/no ciascuna:
  - **Stacking** (solo in partita normale: in No Mercy è sempre attivo);
  - **Endless:** chi finisce le carte esce, si continua finché resta un solo giocatore e alla fine si stila la classifica;
  - **Legend:** ogni giocatore sceglie il proprio personaggio, che ha un'abilità passiva e un'abilità attiva;
- **il numero di giocatori** (2–6) e i loro nomi.

Questo documento riassume **cosa** implementare, in **che ordine**, **come**, quali **alternative** sono state considerate e quale è **consigliata**.
I dettagli di design più approfonditi sono in [`refactoring_e_update_soluzioni.md`](refactoring_e_update_soluzioni.md).
Le regole di gioco e le decisioni prese sono in [`CLAUDE.md`](../../CLAUDE.md), alla sezione "Funzionalità future".

---

## Regole generali del percorso

1. **Si procede per passi.** Alla fine di ogni step il progetto compila, la CLI è giocabile e c'è un commit dedicato. Niente refactoring "big-bang".
2. **La UI non importa mai il model.** Tutto quello che arriva alla CLI passa da `UnoLegendsGame` e dai DTO.
3. **`Partita` non conosce i tipi concreti.** Non contiene `instanceof`, né `if` sul nome di una carta, di una modalità o di un Legend. Ciò che può variare si trova in:
   - `RegoleDiGioco`, con le sue strategie;
   - `Ieffetto`, per gli effetti delle carte e delle abilità;
   - le abilità passive.
4. **Tutte le pesche forzate passano da un solo punto,** `Partita.applicaPescaForzata(...)`. Già fatto nello Step 1.
5. **A ogni step si aggiornano** il README tecnico, il diagramma delle classi e [`CLAUDE.md`](../../CLAUDE.md).

## Panoramica

| Step | Contenuto | Stato |
|---|---|---|
| 1 | Fondamenta del dominio: pesca forzata unica, `OriginePesca`, `Ieffetto` con `Partita` | ✅ fatto (branch `ci-aiuta-claudio`) |
| 2 | Fasi del turno, input unificato, comunicazione degli eventi alla UI | ✅ fatto (branch `ci-aiuta-claudio`) |
| 3 | N giocatori, giocatori eliminati, Endless, configurazione e menu di setup | ✅ fatto (branch `ci-aiuta-claudio`) |
| 4 | Modalità No Mercy: regole speciali e carte nuove | ✅ fatto (branch `ci-aiuta-claudio`) |
| 5 | Stacking delle carte di pesca | ✅ fatto (branch `ci-aiuta-claudio`) |
| 6 | Legend: abilità passive, abilità attive con cooldown, scelta nel menu | ✅ fatto (branch `ci-aiuta-claudio`) |
| 7 | Rifinitura: test, casi limite, bilanciamento, documentazione | ⏭️ prossimo |

Perché quest'ordine:
- I Legend vengono quasi per ultimi perché le loro abilità si appoggiano su tutto il resto: scelta del giocatore bersaglio, pesche forzate, stacking, eliminazioni.
- Il menu arriva presto, già nello Step 3, così ogni modalità nuova si può provare subito giocando.

---

## Step 1 — Fondamenta del dominio ✅

Fatto. In sintesi:
- `Ieffetto.getRichiestaInput(Partita)` e `validaRispostaInput(String, Partita)`;
- `OriginePesca` ha un valore per ogni causa di pesca;
- `ContestoPesca`;
- `Partita.applicaPescaForzata` come unico punto per le pesche forzate;
- `RegoleDiGioco.calcolaCartePescaForzata` (qui si agganceranno le passive) e `pescaVolontaria` (qui si aggancerà la pesca No Mercy);
- `isGiocabile` riceve il `Giocatore`.

Scelte approvate: **A1** (gli effetti ricevono `Partita`), **B1** (`OriginePesca` dettagliata), **C1** (nessun record `ContestoGiocata`).

---

## Step 2 — Fasi del turno, input unificato, eventi verso la UI ✅

Fatto. Resoconto completo (cosa è stato fatto e perché): [`step2_seconda_iter.md`](step2_seconda_iter.md).

Scelte approvate: **2.A2** (enum `FaseTurno`), **2.B4** (registro eventi tipizzato al posto dell'Observer). La frase delle richieste di input la scrive il controller (non la CLI), come per gli eventi.

### Problema
Oggi `Partita` sa "in che fase del turno siamo" tramite cinque variabili sparse:
- `effettoInizialeInAttesa`
- `cartaPescataDaGiocare`
- `giocatoreInAttesaDichiarazioneUno`
- `saltaProssimoTurno`
- `vincitore`

Le richieste di input sono gestite in **tre modi diversi**:
- il colore della carta iniziale;
- la doppia chiamata `giocaCarta(i, null)` e poi `giocaCarta(i, risposta)`;
- la dichiarazione UNO.

Con No Mercy e i Legend le richieste aumentano: scelta del giocatore, scelta del colore della roulette, abilità attive usate a metà turno. Se resta così diventa ingestibile.

C'era anche un difetto: `pescaCarta()` non controllava `effettoInizialeInAttesa`, quindi si poteva pescare mentre il colore della carta iniziale non era ancora stato scelto (lo impediva solo la CLI).

### Cosa fare
1. **Un unico meccanismo per le richieste di input.**
   - `Partita` conserva un'"azione in attesa": l'effetto da completare e, se serve, la carta ancora in mano.
   - Espone un unico metodo `rispondiInput(String)`. Validazione e applicazione restano all'effetto (Information Expert).
   - `RichiestaInput` diventa `(TipoRichiesta tipo, List<String> opzioni)`, con `TipoRichiesta` = `SCELTA_COLORE` o `SCELTA_GIOCATORE`. Così le frasi da mostrare all'utente escono dal model e le scrive la CLI.
2. **Fase del turno esplicita** al posto delle variabili sparse. Vedi le alternative sotto.
3. **Comunicazione degli eventi alla UI**, cioè cose come "X ha pescato 4 carte", "La Roccia è immune", "Y è eliminato". Vedi le alternative sotto.
4. **Correzione del difetto:** ogni comando controlla la fase in `Partita`, non nella CLI. Nota: dopo una pesca si può giocare **qualsiasi** carta della mano (regola scelta per il progetto), quindi non va aggiunto il vincolo "solo la carta pescata".

### Scelta 2.A — Come rappresentare la fase del turno

| Opzione | Descrizione | Pro | Contro |
|---|---|---|---|
| 2.A1 Variabili sparse (com'è oggi) | Si aggiungono altri booleani | Nessun lavoro | Ogni metodo controlla 5–8 condizioni; facile sbagliare |
| **2.A2 Enum `FaseTurno`** | `NORMALE`, `POST_PESCA`, `ATTESA_INPUT`, `ATTESA_UNO`, `TERMINATA` (più `RISPOSTA_STACKING` nello Step 5) | Semplice, leggibile, un solo campo da controllare | I comportamenti diversi per fase restano negli `if` dei metodi di `Partita` |
| 2.A3 Pattern State (GoF) | Una classe per fase, ognuna sa quali comandi accetta | Molto pulito, rispetta l'Open/Closed, ottimo da presentare all'esame | Tante classi; le fasi devono poter modificare `Partita` (più accoppiamento) |

**Opinione:** **2.A2 adesso.** Si può passare a **2.A3 nello Step 5**, quando con lo stacking le fasi saranno al completo, ma solo se gli `if` diventano davvero troppi. Partire subito dallo State pattern vuol dire progettare classi per fasi che ancora non conosciamo bene.

### Scelta 2.B — Come la UI viene a sapere cosa è successo

| Opzione | Descrizione | Pro | Contro |
|---|---|---|---|
| 2.B1 Tutto nel valore di ritorno (com'è oggi) | `EsitoGiocata` / `EsitoGiocataDto` con un campo in più per ogni nuovo caso | Nessun pattern nuovo | Questi oggetti crescono senza controllo; non dicono niente di ciò che succede durante il turno di un altro |
| 2.B2 Pull + registro eventi | `StatoPartitaDto` contiene `List<String> ultimiEventi`, che la CLI stampa | Semplice, nessun callback | Il model produce testo (poco MVC) oppure servono codici; diventa rigido se un giorno si fa una GUI |
| 2.B3 Observer tipizzato | `IPartitaListener` nel model → `UnoLegendsGame` lo implementa e traduce → `IGameListener` nel controller → la CLI lo implementa. Metodi specifici: `onCartaGiocata`, `onPescaSubita`, `onPassivaAttivata`, `onGiocatoreEliminato`… | Pattern GoF classico; il model non conosce la UI; ogni evento ha i propri parametri; estensibile | Più codice; bisogna evitare che un callback richiami un comando (regola: i callback **stampano soltanto**, non leggono input) |
| **2.B4 Registro eventi tipizzato** | `Partita` registra `EventoPartita(TipoEvento, ...)`; `UnoLegendsGame.prelevaMessaggi()` li traduce in frasi; la CLI le stampa dopo ogni comando | Poche righe (1 enum + 1 record); il model non produce testo; nessun callback né rientro in `Partita`; si può evolvere in Observer | I campi del record non usati da un tipo restano `null` (mitigato dalle factory statiche) |

**Opinione iniziale:** 2.B3. **Scelta approvata:** **2.B4.**
- La CLI è sincrona: dà un comando e aspetta. Le basta leggere, dopo ogni comando, cosa è successo. L'Observer serve quando chi ascolta deve reagire a eventi che non controlla (GUI, rete, timer): qui sarebbe sproporzionato.
- Gli eventi sono già oggetti tipizzati: se un giorno servisse una GUI, passare all'Observer è un passo breve.
- I **comandi** (`giocaCarta`, `pescaCarta`…) restituiscono un semplice `boolean` (accettato/rifiutato). `EsitoGiocata` ed `EsitoGiocataDto` sono stati eliminati: ciò che prima trasportavano ora lo dicono la fase e gli eventi.

---

## Step 3 — N giocatori, giocatori eliminati, configurazione e menu ✅

Fatto. Resoconto completo (cosa è stato fatto e perché): [`step3_terza_iter.md`](step3_terza_iter.md).

Scelte approvate:
- **3.A rivista: "modalità + opzioni".** Le modalità sono due e lo resteranno; ciò che crescerà sono le regole. Quindi:
  - `ModalitaDiGioco` è un enum **di soli dati** (mazzo e opzioni obbligatorie);
  - `OpzionePartita` è un enum di soli dati (oggi `ENDLESS`);
  - le regole le sceglie `RegoleDiGioco.standard(opzioni)`, e allo Step 4 `RegoleDiGioco.noMercy(opzioni)`.
- **3.B rivista: niente Builder, per ora.**
  - `ConfigurazionePartita` è un `record` che si valida nel costruttore.
  - Il Builder è stato valutato anche su `RegoleDiGioco` e scartato per ora: `builder()...build()` è meno leggibile di `RegoleDiGioco.standard(opzioni)` finché le strategie sono poche. Va rivalutato agli Step 5–6.
- **Endless è un obiettivo.** Chi finisce le carte esce (`StatoGiocatore.TERMINATO`) e si continua finché resta un solo giocatore; alla fine si stila la classifica. Il primo che ha finito le carte è il primo in classifica. Richiede almeno 3 giocatori.
- **Revisione dopo lo Step 3:** un solo DTO (`StatoPartitaDto`, record con i tipi interni `Fase`, `Richiesta`, `Giocatore`); il setup usa solo stringhe; nessuna classe di eccezione nostra (`ConfigurazionePartita.verifica` e `UnoLegendsGame.verificaImpostazioni` restituiscono l'errore oppure `null`); lo stato `TERMINATO` si chiama `TERMINATO`.
- Nel menu compaiono **solo le opzioni già implementate:** Stacking dallo Step 5, Legend dallo Step 6, No Mercy dallo Step 4.

Il piano originale, per confronto:

### Cosa fare (piano originale)
1. **Stato del giocatore:** enum `StatoGiocatore` (`ATTIVO`, `ELIMINATO`).
   - Scorrimento dei turni sicuro: un unico helper `trovaProssimoIndiceAttivo(...)` usato sia da `avanzaGiocatore()` sia da `getProssimoGiocatore()`. Oggi entrambi usano `(i ± 1) % n`, che dà per scontato che nessuno esca.
   - `Partita.getAvversariAttivi()` servirà agli effetti con scelta del giocatore.
2. **Fine partita:** vince chi finisce le carte, oppure l'ultimo rimasto se gli altri sono eliminati.
3. **Configurazione della partita.** Un oggetto `ConfigurazionePartita` con:
   - modalità;
   - numero di giocatori (2–6);
   - nomi dei giocatori;
   - più avanti, il Legend di ciascuno.

   `PartitaFactory.creaPartita(config)` lo traduce in mazzo, regole e giocatori.
4. **Menu di setup nella CLI.** Il controller espone le opzioni disponibili come stringhe/DTO (modalità, e più avanti i Legend). La CLI raccoglie le scelte e le passa in un DTO di configurazione. La CLI continua a non importare il model.

### Scelta 3.A — Come rappresentare la modalità di gioco

| Opzione | Descrizione | Pro | Contro |
|---|---|---|---|
| 3.A1 Booleani sparsi | `noMercy = true` letto in vari punti | Veloce | Gli `if (noMercy)` si spargono ovunque: proprio ciò che vogliamo evitare |
| 3.A2 Enum `ModalitaDiGioco` con metodi | `NORMALE`, `NO_MERCY`; ogni valore sa creare il proprio `MazzoFactory` e il proprio `RegoleDiGioco` | Compatto, un solo punto di scelta, facile da elencare nel menu | Aggiungere una modalità vuol dire modificare l'enum (accettabile: le modalità sono poche e note) |
| 3.A3 Abstract Factory "profilo di modalità" | Interfaccia `ProfiloModalita` (`creaMazzoFactory()`, `creaRegole()`) con una classe per modalità | Massima estensibilità (Open/Closed) | Più classi per lo stesso risultato |

**Opinione iniziale:** **3.A2.** Le modalità sono due e chiuse: l'enum è la soluzione più semplice e non sparge `if`. `TipoMazzo`, che oggi non usa nessuno, si può eliminare o assorbire nell'enum. Se un giorno le modalità diventassero combinabili (per esempio Endless + No Mercy), si passa a 3.A3 senza toccare `Partita`.

**Scelta approvata:** una variante della 3.A2, cioè **modalità + opzioni combinabili.** L'enum non crea niente: contiene solo dati. Le regole le sceglie `RegoleDiGioco.standard(opzioni)`. `TipoMazzo` è stato eliminato.

### Scelta 3.B — Come costruire la configurazione

| Opzione | Pro | Contro |
|---|---|---|
| Costruttore con tutti i parametri | Semplice | Illeggibile quando i parametri crescono (Legend per giocatore…) |
| **Builder (GoF)** | Leggibile; valida tutto in `build()` (numero di giocatori, Legend duplicati, ecc.) | Una classe in più |

**Opinione iniziale:** **Builder.** È il caso d'uso da manuale e tiene la validazione fuori dalla CLI.

**Scelta approvata:** **niente Builder.** `ConfigurazionePartita` è un `record` con la validazione nel costruttore: ha pochi campi, e le opzioni nuove finiscono in un `Set`. Anche `RegoleDiGioco` resta senza Builder, con un metodo statico per modalità che riceve le opzioni. Il Builder si rivaluta quando le strategie aumenteranno.

---

## Step 4 — Modalità No Mercy: regole e carte ✅

Fatto. Resoconto completo (cosa è stato fatto, perché, e tutte le scelte prese con il team): [`step4_quarta_iter.md`](step4_quarta_iter.md).

Scelte approvate, in sintesi:
- **4.A — mazzo:** composizione ufficiale scritta in `NoMercyMazzoFactory`, in un unico punto (non cambierà). Sono 168 carte:
  - 80 numeriche;
  - 64 azioni colorate; per colore: 3 blocca, 3 cambia giro, 3 +2, 2 +4, 3 scarta tutto, 2 salta tutti;
  - 24 jolly: 8 +4 cambio giro, 8 roulette, 4 +6, 4 +10.
- **4.B — scambio e rotazione delle mani: dentro gli effetti** (`EffettoScambiaMano`, `EffettoPassaMani`), non in `Partita`. Il parere iniziale era diverso: vedi sotto.
- **UNO:**
  - dopo uno scambio o una rotazione di mani **nessuno** dichiara UNO;
  - dopo Scarta tutto **sì**, se resta una carta.
- **Ultima carta:** la 7 o lo 0 giocati come ultima carta fanno vincere, e lo scambio non avviene.
- **Cambio giro con 2 giocatori in gioco:** vale come blocco (regola ufficiale), in entrambe le modalità. Con lo stacking (Step 5) il +4 cambio giro con 2 giocatori fa tornare l'accumulo a chi l'ha giocato.
- **Roulette:**
  - il colore lo sceglie chi gioca;
  - un jolly nero non conta come carta del colore scelto;
  - Salesman non ha lo sconto.
- **Mercy rule:**
  - l'eliminazione avviene dopo l'effetto o la pesca;
  - se l'eliminato doveva saltare il turno, il salto si annulla;
  - se è il giocatore di turno, il turno passa.
- **Stacking:** non c'era ancora, è arrivato allo Step 5.

Il piano originale, per confronto:


### Regole speciali (in `RegoleDiGioco.noMercy()`)
- **Pesca volontaria "finché trovi una carta giocabile":** `StrategiaPescaFinoAGiocabile implements IStrategiaPesca`. L'aggancio esiste già dallo Step 1. La carta giocabile è l'ultima pescata.
- **Eliminazione a 25 carte:** `StrategiaSconfittaNoMercy(25)`.
  - `Partita` la controlla dopo ogni pesca, in `applicaPescaForzata` e in `pescaCarta`.
  - L'eliminato passa a `ELIMINATO` e le sue carte **tornano subito nel mazzo, che viene rimescolato** (decisione presa).
  - Se l'eliminato era il giocatore di turno, il turno avanza.

### Carte nuove (in `NoMercyMazzoFactory`, nuova Concrete Factory)

| Carta | Implementazione |
|---|---|
| **0** — tutti passano la mano al giocatore successivo | `EffettoPassaMani`: `Partita.ruotaMani()` nel senso di gioco corrente |
| **7** — scambi la mano con un giocatore a scelta | `EffettoScambiaMano`: input `SCELTA_GIOCATORE` + `Partita.scambiaMani(a, b)` |
| **Scarta tutto** | `EffettoScartaColore`: scarta dalla mano tutte le carte del colore della carta, **senza** attivarne gli effetti |
| **Salta tutti** | `EffettoTurnoExtra`: contatore `turniExtra` in `Partita`, così il giocatore rigioca subito. Funziona con qualunque numero di giocatori e con gli eliminati |
| **Jolly cambio giro +4** | `EffettoComposito(cambia colore, cambio giro, pescata 4)` |
| **Jolly +6 / +10** | `EffettoComposito(cambia colore, pescata 6/10)` con `OriginePesca.PIU_SEI` / `PIU_DIECI` |
| **Jolly roulette colore** | `EffettoRoulette`: chi gioca sceglie il colore (decisione presa); il successivo pesca una carta alla volta finché esce quel colore, tiene tutto e salta il turno; `OriginePesca.ROULETTE`; il controllo dei 25 carte avviene **durante** la pesca |

Modifiche necessarie al modello delle carte:
- ~~`CartaNumero` oggi crea `Effetto_Nullo` da sola. Va aggiunto un costruttore che riceve l'effetto.~~ **Già risolto:** `CartaNumero` e `CartaSpeciale` sono state unificate in un'unica classe `Carta` (vedi [`step3_terza_iter.md`](step3_terza_iter.md), §8). La factory No Mercy crea lo 0 e il 7 con `new Carta(colore, valore, effetto)`.
- Nuovi valori in `ValoreCarta`: `SCARTA_TUTTO`, `SALTA_TUTTI`, `CAMBIO_GIRO_PIU_QUATTRO`, `PIU_SEI`, `PIU_DIECI`, `ROULETTE`.

### Casi limite da gestire
- **Controllo UNO dopo gli scambi di mano.** Oggi si verifica `avevaDueCarte && mano == 1`. Va cambiato in "la mano finale ha 1 carta e prima ne aveva più di una", perché con la 7 o la 0 si può ricevere una mano da 1 carta.
- **Vittoria dopo uno scambio.** Va controllata per **tutti** i giocatori coinvolti, non solo per chi ha giocato. Esempio: giocare la 7 come ultima carta regala la propria mano vuota all'avversario. Regola da decidere: vietare la 7 come ultima carta, oppure dichiarare vincitore chi resta senza carte.
- **Scarta tutto come penultima carta:** si può finire la mano. In quel caso vale come vittoria normale.

### Scelta 4.A — Composizione del mazzo No Mercy
Il mazzo ufficiale ha 168 carte, ma le quantità per tipo non le abbiamo trovate in fonti affidabili.

**Opinione:** definirle noi in una tabella dentro `NoMercyMazzoFactory`, e verificarle sulla scatola o sul PDF Mattel se disponibile. L'importante è che i numeri stiano in un solo punto.

**Scelta approvata:** le quantità le ha fornite il team; per le azioni colorate la ripartizione per colore è 3/3/3/2/3/2. Sono costanti con nome in `NoMercyMazzoFactory`, che controlla anche il totale di 168.

### Scelta 4.B — Dove mettere "scambia mani" e "ruota mani"

| Opzione | Pro | Contro |
|---|---|---|
| **Metodi package-private in `Partita`** (`scambiaMani`, `ruotaMani`) che delegano a `Giocatore.sostituisciMano(...)` | Coerente con `segnalaSaltoTurno` e `applicaColoreScelto`; `Partita` controlla UNO e vittoria dopo lo scambio | `Partita` cresce un po' |
| Effetto che manipola direttamente i `Giocatore` | `Partita` resta più piccola | L'effetto deve ricordarsi i controlli UNO e vittoria: responsabilità sparse |

**Opinione iniziale:** **metodi in `Partita`**, che è il Coordinator e deve garantire che il gioco resti coerente.

**Scelta approvata:** **dentro gli effetti.** L'effetto riceve `Partita` e sposta le mani con i metodi package-private di `Giocatore`. Il difetto previsto ("l'effetto deve ricordarsi i controlli UNO e vittoria") non si presenta, perché `Partita` fa comunque quei controlli dopo **ogni** giocata, per qualunque effetto:
- `verificaEliminazioni`;
- `deveDichiarareUno`;
- il controllo di vittoria.

---

## Step 5 — Stacking (+2 / +4 / +6 / +10) ✅

Fatto. Resoconto completo (cosa è stato fatto, perché, e tutte le scelte prese con il team): [`step5_quinta_iter.md`](step5_quinta_iter.md).

Scelte approvate, in sintesi:
- **5.A — valore di pesca:** metodo `default int getValorePenalita()` in `Ieffetto` (0 di default; `EffettoComposito` somma), **non** un attributo di `ValoreCarta` come proposto sotto: il numero di carte è già scritto nell'effetto, così non sta in due punti.
- **5.B — accumulo:** record `Accumulo` (totale, valore dell'ultima carta, numero di carte, origine della prima), come proposto.
- **Una sola strategia** `StrategiaAccumuloValoreCrescente` per entrambe le modalità (più `StrategiaNessunAccumulo`): con il mazzo normale la regola "valore uguale o superiore" dà già "+2 ← +2/+4, +4 ← +4".
- **Risposta:** conta solo il valore, non il colore; chi ha una risposta valida può comunque accettare; si accetta con il comando dedicato `accettaPenalita()` (non con "pesca").
- **+4 cambio giro con 2 giocatori:** "arma a doppio taglio". Il cambio giro fa saltare l'avversario, quindi l'accumulo torna a chi ha giocato la carta, che può rispondere o accettare. **Ci si discosta dalla regola ufficiale.**
- **Carta iniziale di pesca:** con lo stacking apre l'accumulo e il primo giocatore risponde.
- **Niente pattern State** e **niente Builder** per ora; **niente Maven/JUnit** (le prove restano un programma esterno).

Sotto resta il piano originale.

### Regola (No Mercy, e opzione Stacking della partita normale)
- In No Mercy lo stacking è **sempre attivo**: è un'opzione obbligatoria di `ModalitaDiGioco.NO_MERCY`. Nella partita normale è l'**opzione** `OpzionePartita.STACKING`, che si aggiunge all'enum in questo step.
- Nella partita normale (decisione presa): un +2 si copre con un +2 o un +4, un +4 solo con un +4. È la stessa regola "valore uguale o superiore".
- Si risponde a una carta di pesca con una carta di pesca di **valore uguale o superiore**. Il Jolly cambio giro +4 vale 4.
- L'accumulo passa al giocatore successivo.
- Chi non risponde pesca tutto il totale e salta il turno.
- La roulette non si accumula.

### Come implementarlo
- **Gli effetti di pesca non sanno se lo stacking è attivo.** Chiamano `partita.applicaPenalitaPesca(n, origine)` e decide `Partita`, tramite le regole:
  - modalità normale: pesca subito e salta il turno (come oggi);
  - No Mercy: aggiunge all'accumulo e passa il turno al bersaglio.

  È Protected Variations: lo stesso `Effetto_pescata_carte` funziona in entrambe le modalità.
- **Nuova `IStrategiaAccumuloPenalita`** in `RegoleDiGioco`:
  - `StrategiaNessunAccumulo` per la modalità normale;
  - `StrategiaAccumuloNoMercy` per No Mercy.

  La strategia stabilisce se una carta può rispondere a un accumulo.
- **Nuova fase `RISPOSTA_STACKING`:**
  - `isGiocabile` accetta solo carte di pesca di valore sufficiente;
  - "pesca" significa accettare la penalità: si pesca tutto il totale con `OriginePesca.ACCUMULO`.
- **`StatoPartitaDto`** espone il totale accumulato.
- **La Roccia** (decisione presa) è immune solo se l'accumulo è formato da **un solo +2**. In quel caso l'origine resta `PIU_DUE`; se le carte accumulate sono due o più, l'origine diventa `ACCUMULO`. Può comunque rispondere con un +2 o superiore.

### Scelta 5.A — Come conoscere il "valore di pesca" di una carta

| Opzione | Pro | Contro |
|---|---|---|
| **Attributo in `ValoreCarta`** (`PIU_DUE(2)`, `PIU_SEI(6)`…, 0 per le altre) | Semplice; il valore è una proprietà intrinseca della carta | Collega il valore all'enum delle carte (va bene: le carte di pesca sono note) |
| Metodo `default int getValorePenalita()` in `Ieffetto` | Più flessibile: un effetto composto lo calcola da solo | Bisogna risalire dentro `EffettoComposito`; più complesso |
| `instanceof Effetto_pescata_carte` | — | Viola le regole del progetto: **scartata** |

**Opinione:** **attributo in `ValoreCarta`.**

### Scelta 5.B — Cosa memorizzare dell'accumulo

| Opzione | Pro | Contro |
|---|---|---|
| Solo `int totale` + ultimo valore | Minimo | Non distingue "un +2 da solo" da "un accumulo": serve per La Roccia |
| **Piccolo oggetto `Accumulo`** (totale, valore dell'ultima carta, numero di carte) | Contiene tutto ciò che serve alle regole e alle passive | Una classe in più |

**Opinione:** **oggetto `Accumulo`.**

Se a questo punto gli `if` sulle fasi in `Partita` sono troppi, questo è il momento giusto per passare al **pattern State** (vedi 2.A3).

---

## Step 6 — Legend ✅

Fatto. Resoconto completo (cosa è stato fatto, perché, alternative scartate e tutte le scelte prese con il team): [`step6_sesta_iter.md`](step6_sesta_iter.md).

Scelte approvate, in sintesi:
- **Sei Legend con coppie fisse** passiva + attiva: La Roccia (immune al +2 singolo / Il Castigatore), Il Mercante (Salesman / Gifted card), Il Fantasma (Ghost / Scambia mazzo), Il Daltonico (rosso ↔ verde / Arcobaleno), Il Freezer (Pelle di ghiaccio / Congela), L'Acrobata (Doppia pesca / Double card).
- **Passive: interfaccia `IPassiva` con metodi `default`** (hook), come proposto sotto. Ogni passiva ridefinisce un solo hook; le interrogano `RegoleDiGioco` e pochi punti di `Partita`.
- **Attive: `Ieffetto` + cooldown** (`AbilitaAttiva`), comando `usaAbilita()` solo in fase `NORMALE`; **nessuna abilità consuma il turno**. Cooldown contato sui turni del possessore, nessun cooldown iniziale.
- **Congela blocca 1 turno**; il congelato è "come se non ci fosse" (non gioca, non è bersagliabile, non risponde allo stacking, la carta 0 lo salta).
- **Scambia mazzo:** solo verso chi ha più di una carta. **Gifted card:** solo con almeno 4 carte. **Arcobaleno:** solo nella giocata normale. **Pelle di ghiaccio:** solo contro la carta Blocca. **Doppia pesca:** la carta non tenuta va in fondo al mazzo.
- **Legend = opzione** `OpzionePartita.LEGEND`, in entrambe le modalità; due giocatori non possono avere lo stesso Legend.
- Rispetto al piano qui sotto: niente Null Object per l'abilità attiva (è `null` e il comando viene rifiutato; le passive sono una lista vuota); Gifted card che svuota la mano non è più un caso, perché servono almeno 4 carte; dopo Gifted e Scambia mazzo non si dichiara UNO.
- **Niente Builder** e **niente pattern State** neanche ora: le strategie restano 7 e non ci sono fasi nuove.

Sotto resta il piano originale.

### Modello
```
Giocatore ──> Legend (nome, IAbilitaPassiva, AbilitaAttiva)
AbilitaAttiva = Ieffetto + cooldown (int) + turni rimanenti
```
Chi gioca senza Legend usa un **Null Object**: `PassivaNulla`, e nessuna abilità attiva. Così `Partita` non controlla mai `legend != null`.

### Abilità passive — hook in `RegoleDiGioco`
Interfaccia `IAbilitaPassiva` con metodi `default` che non modificano nulla:

| Hook | Usato da | Dove viene chiamato |
|---|---|---|
| `modificaGiocabilita(boolean base, Carta candidata, Carta inCima, Colore attivo)` | **Daltonico** (rosso su verde e viceversa) | `RegoleDiGioco.isGiocabile` |
| `modificaCartePescaForzata(int base, ContestoPesca)` | **La Roccia** (`PIU_DUE` → 0), **Salesman** (`max(0, base - 1)` su ogni penalità, compresa UNO) | `RegoleDiGioco.calcolaCartePescaForzata` |
| `isBersagliabileDaScelta()` | **Ghost** (`false`) | `Partita.getAvversariBersagliabili()`, usato **solo** dagli effetti in cui l'utente sceglie il bersaglio (7, Scambia mazzo, Gifted, Castigatore). La carta 0 e il +2 al successivo lo colpiscono comunque |

Salesman e roulette: **deciso, nessuno sconto.** "Una carta in meno" non ha un significato chiaro quando si pesca finché esce un colore. Nel codice la roulette passa da `applicaPescaFinoA`, che non chiama `calcolaCartePescaForzata`.

### Abilità attive — sono `Ieffetto`

| Abilità | Effetto (riuso) |
|---|---|
| **Scambia mazzo** | lo stesso `EffettoScambiaMano` della carta 7 |
| **Double card** | lo stesso `EffettoTurnoExtra` di "Salta tutti" |
| **Gifted card** | nuovo `EffettoRegalaCarte(2)`: 2 carte casuali dalla propria mano a un avversario a scelta, senza attivarne gli effetti |
| **Il castigatore** | nuovo `EffettoPescaBersaglio(4)`: `applicaPescaForzata(scelto, 4, OriginePesca.ABILITA)`. Passa da La Roccia/Salesman e dal controllo dei 25 carte |

Uso dell'abilità attiva:
- Si usa con `Partita.usaAbilitaAttiva()`, disponibile solo nella fase `NORMALE`, quando il cooldown è a 0.
- **Non consuma il turno** (decisione presa): dopo si può ancora giocare o pescare.
- L'input (per esempio il giocatore bersaglio) passa dal meccanismo unificato dello Step 2.
- Il cooldown riparte e scende di 1 a ogni turno del possessore.
- Da decidere: il valore del cooldown di ogni abilità.

Casi limite da decidere:
- **Gifted card:** se lascia il giocatore con 0 carte, è vittoria? Proposta: sì, coerente con "vince chi finisce le carte".
- **UNO dopo Gifted o Scambia mazzo:** se il giocatore resta con 1 carta deve dichiarare UNO? Proposta: sì, con la stessa regola della carta 7.

### Abilità passiva "pesca 2, tienine 1" (solo partita normale)
Richiede input **durante** la pesca, quindi è la più complessa. Si implementa per ultima, con un hook sulla pesca volontaria che apre una richiesta `SCELTA_CARTA`. Con il meccanismo dello Step 2 è fattibile. Nell'elenco dei Legend va resa disponibile solo in modalità normale.

### Catalogo dei Legend

| Opzione | Pro | Contro |
|---|---|---|
| **Enum `TipoLegend`** che crea il `Legend` (nome, passiva, attiva, cooldown) | Elenco chiuso e semplice da mostrare nel menu; un unico punto di configurazione | Aggiungere un Legend vuol dire modificare l'enum |
| Factory / registro di classi | Open/Closed completo | Più struttura per un elenco piccolo |

**Opinione:** **enum.** I Legend sono un elenco deciso a tavolino.

Da decidere: la combinazione fra passive e attive è libera oppure ogni Legend ha una coppia fissa? Proposta: **coppie fisse**, più semplici da bilanciare e da presentare.

### Alternative valutate per le passive

| Opzione | Verdetto |
|---|---|
| **Hook + Null Object** (sopra) | ✅ consigliata: `Partita` resta ignara dei Legend; una passiva implementa solo l'hook che le serve |
| Decorator sulle strategie di `RegoleDiGioco` | Elegante, ma le passive agiscono su punti diversi e per singolo giocatore: troppe classi wrapper |
| Observer | ❌ L'Observer reagisce *dopo* un evento; le passive devono *modificare* una decisione prima che avvenga |

### Menu
Al setup:
1. si chiede "Giocare con i Legend? s/n". È l'opzione `OpzionePartita.LEGEND`, che si aggiunge all'enum in questo step: il menu la mostra da solo, perché elenca le opzioni configurabili;
2. se sì, ogni giocatore sceglie dall'elenco, che il controller espone come stringhe.

Durante la partita la CLI mostra il Legend di ogni giocatore e il cooldown, e aggiunge la voce di menu "usa abilità".

---

## Step 7 — Rifinitura

- **Test automatici.** Oggi non ce ne sono, e non c'è un build tool.

  | Opzione | Pro | Contro |
  |---|---|---|
  | **Maven/Gradle + JUnit 5** | Standard; test ripetibili sulle regole (stacking, eliminazione, passive) | Va aggiunta la struttura `pom.xml`/`build.gradle` |
  | JUnit da riga di comando con `javac` | Nessun build tool | Scomodo da far girare |

  **Decisione del team (Step 5): niente Maven né JUnit.** Le prove restano un programma esterno compilato con `javac` (al massimo 100 partite simulate). Il parere iniziale era **Maven + JUnit 5**, anche introdotto prima. Le regole No Mercy e le interazioni con i Legend hanno molti casi limite, e provarli a mano in CLI è lento.
- **Bilanciamento** dei cooldown.
- **Documentazione:**
  - diagrammi aggiornati (classi e sequenza);
  - [`roles_and_patterns.md`](roles_and_patterns.md) riallineato al codice;
  - artefatti UP (Use Case, Glossario) aggiornati con Legend e No Mercy.

---

## Riepilogo delle decisioni ancora aperte

| # | Decisione | Proposta |
|---|---|---|
| 1 | Valore del cooldown di ogni abilità attiva | primi valori allo Step 6 (3–5 turni); da bilanciare giocando (Step 7) |
| 2 | Salesman e roulette: si applica lo sconto? | ✅ deciso: no |
| 3 | Composizione esatta del mazzo No Mercy (168 carte) | ✅ decisa (Step 4) |
| 4 | Carta 7 / Scambia mazzo giocate come ultima carta | ✅ deciso: chi la gioca come ultima carta vince e lo scambio non avviene |
| 5 | Gifted card che svuota la mano | ✅ non succede: dallo Step 6 Gifted card richiede almeno 4 carte |
| 6 | Coppie passiva/attiva fisse o libere | ✅ fisse (Step 6) |
| 7 | Introdurre Maven + JUnit e quando | ✅ deciso allo Step 5: no, si continua con programmi di prova esterni (al massimo 100 partite simulate) |
| 8 | Cambio giro con 2 giocatori in gioco | ✅ deciso: vale come blocco (regola ufficiale), in entrambe le modalità; il +4 cambio giro con lo stacking rimanda l'accumulo a chi l'ha giocato (Step 5) |
| 9 | Builder per `RegoleDiGioco` quando le strategie cresceranno | ✅ no: allo Step 6 le strategie restano 7 (le passive sono del giocatore) |

## Pattern usati, a lavori finiti

| Pattern | Dove |
|---|---|
| Facade / Controller (GRASP) | `UnoLegendsGame` |
| DTO | comunicazione controller → UI |
| Command | `AzioneInAttesa`: azione sospesa in attesa di input (Step 2) |
| Pure Fabrication | registro eventi `EventoPartita` verso la UI (Step 2) |
| Strategy | `RegoleDiGioco` (vittoria, sconfitta, compatibilità, pesca, penalità UNO, fine partita normale/Endless, accumulo), `Ieffetto` |
| Composite | `EffettoComposito` (+4, Jolly +6/+10, cambio giro +4) |
| Null Object | `Effetto_Nullo`, `PassivaNulla` |
| Abstract Factory + Singleton | `MazzoFactory`, `StandardMazzoFactory`, `NoMercyMazzoFactory` |
| Builder | non usato per ora (vedi scelta 3.B, confermata allo Step 5); candidato `RegoleDiGioco` allo Step 6 |
| State (opzionale) | fasi del turno di `Partita`: allo Step 5 si è restati all'enum `FaseTurno`; da rivalutare allo Step 6 |
| Protected Variations / Information Expert / Coordinator (GRASP) | `RegoleDiGioco`, effetti, `Partita` |
