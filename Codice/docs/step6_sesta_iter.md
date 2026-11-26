# Step 6 (sesta iterazione) — Legend: cosa è stato fatto e perché

**Branch:** `ci-aiuta-claudio` · **Roadmap:** [ROADMAP.md](ROADMAP.md), Step 6 · **Diagramma:** [domain_class_diagram.puml](domain_class_diagram.puml) · **Step precedente:** [step5_quinta_iter.md](step5_quinta_iter.md)

Lo Step 6 aggiunge i **Legend**. Ogni giocatore sceglie un personaggio, con:

- un'**abilità passiva**, sempre attiva e senza cooldown;
- un'**abilità attiva**, da usare a inizio turno, che poi va ricaricata per un certo numero di turni.

I Legend sono l'opzione **Legend** del menu e si possono usare in entrambe le modalità (Standard e No Mercy). Se l'opzione è attiva, ogni giocatore sceglie un Legend diverso.

---

## 1. I Legend

| Legend | Passiva (sempre attiva) | Attiva | Cooldown |
|---|---|---|---|
| **La Roccia** | Immune al **+2 singolo**: non pesca e non salta il turno. Un accumulo dello stacking lo subisce come tutti | **Il Castigatore**: un giocatore a scelta pesca 4 carte | 4 |
| **Il Mercante** | **Salesman**: una carta in meno a ogni penalità di pesca (+2/+4/+6/+10, accumulo, Castigatore, mancato UNO). Non vale con la roulette | **Gifted card**: dai 2 carte a caso della tua mano a un giocatore a scelta, senza attivarne l'effetto. Serve avere **più di 3 carte** | 3 |
| **Il Fantasma** | **Ghost**: non può essere scelto come bersaglio dagli effetti in cui si sceglie un giocatore | **Scambia mazzo**: scambi la mano con un giocatore a scelta che ha **più di una carta** | 5 |
| **Il Daltonico** | Può giocare carte **rosse sul verde** e **verdi sul rosso** | **Arcobaleno**: per il resto del turno puoi giocare qualsiasi carta, senza controllare la compatibilità (non durante lo stacking) | 4 |
| **Il Freezer** | **Pelle di ghiaccio**: la carta **Blocca** non ha effetto su di lui | **Congela**: un giocatore a scelta salta il suo prossimo turno e, finché è congelato, è "come se non ci fosse" | 5 |
| **L'Acrobata** | **Doppia pesca**: quando pesca volontariamente pesca 2 carte, ne tiene una e rimette l'altra **in fondo al mazzo**. Solo nella partita Standard | **Double card**: dopo questo turno ne giochi subito un altro (come Salta tutti) | 4 |

I cooldown sono un primo valore. Le abilità più forti (Scambia mazzo, Congela) hanno il cooldown più lungo. Vanno bilanciati giocando, nello Step 7.

---

## 2. Le scelte prese con il team

Prima di scrivere codice abbiamo discusso ogni punto aperto. Per ognuno c'era una proposta, che il team ha confermato o corretto.

| # | Domanda | Decisione | Perché |
|---|---|---|---|
| 1 | Quali Legend, con quali abilità | **I 6 Legend della tabella sopra**, con passiva e attiva **fisse** | Sei Legend, uno per ogni posto (2–6 giocatori), così non serve inventarne altri. Coppie fisse: più semplici da bilanciare e da spiegare. Ogni abilità dell'elenco originale ha trovato posto. Il Daltonico ha ricevuto un'attiva (Arcobaleno) su richiesta del team |
| 2 | Le passive hanno un cooldown? | **No**, sono sempre attive. Il cooldown ce l'hanno solo le attive | È la definizione di passiva |
| 3 | Un'abilità attiva consuma il turno? | **No, nessuna** | All'inizio si era pensato di far perdere il turno a Congela e Scambia mazzo. Poi il team le ha indebolite in modo diverso, e la regola è tornata la stessa per tutte (vedi 4 e 5). Così il codice non ha bisogno di un `boolean consumaTurno` |
| 4 | Quanti turni blocca Congela? | **1 turno** | Con 2 turni l'abilità era troppo forte, soprattutto con 2 giocatori. Con 1 turno non serve far perdere il turno al Freezer |
| 5 | Scambia mazzo può prendere chiunque? | **No: il bersaglio deve avere più di una carta** | Così non si "ruba" una mano quasi vincente. Per questo non serve far perdere il turno a chi la usa |
| 6 | Che cosa vuol dire "congelato" | **È come se non ci fosse.** Non gioca, non è bersagliabile, non risponde allo stacking, la carta 0 non gli passa la mano. Con 2 giocatori, se l'avversario è congelato e tu giochi un +2, lo peschi tu | Decisione del team: un'unica regola semplice da cui seguono tutti i casi |
| 7 | Il Freezer può congelare il Fantasma? | **No** | Congela è un effetto in cui si sceglie il bersaglio, e Ghost non è bersagliabile |
| 8 | Il congelato si può scegliere come bersaglio? | **No** | Segue dalla decisione 6 |
| 9 | Contro cosa protegge Pelle di ghiaccio? | **Solo contro la carta Blocca** | Non protegge dagli altri salti (cambio giro con 2 giocatori, salto dopo un +2/+4 senza stacking, roulette) |
| 10 | Quando vale Arcobaleno? | **Solo nella giocata normale**, non durante lo stacking | Durante lo stacking conta il valore di pesca, non la compatibilità |
| 11 | Gifted card con poche carte | **Si usa solo con più di 3 carte**, cioè almeno 4 | Chi la usa resta con almeno 2 carte: non vince regalando le ultime carte e non c'è il caso "resto con una carta, devo dichiarare UNO?" |
| 12 | Il Legend è obbligatorio? | **No: è un'opzione della partita.** Se la si attiva, ognuno sceglie il suo Legend | Scelta del team |
| 13 | Due giocatori possono avere lo stesso Legend? | **No** | Scelta del team |
| 14 | Quando si usa un'abilità attiva? | **Solo nella fase `NORMALE`**: prima di pescare o giocare, una volta per turno | Non durante lo stacking, non dopo aver pescato, non durante la dichiarazione UNO |
| 15 | Come si conta il cooldown? | **Si contano solo i turni del giocatore stesso.** Con cooldown 4, dopo l'uso l'abilità torna pronta al quarto turno successivo | Scelta del team |
| 16 | Cooldown all'inizio della partita | **Nessuno**: l'abilità è pronta dal primo turno | Scelta del team |
| 17 | Dove va la carta non tenuta con la Doppia pesca | **In fondo al mazzo** | Così non si ripesca subito |
| 18 | Come si implementano le passive | **Interfaccia `IPassiva` con metodi `default`** (hook) | Vedi sezione 4.1, con le alternative |
| 19 | Pelle di ghiaccio è una `IPassiva` come le altre? | **Sì** | Vedi sezione 3, passo 6 |
| 20 | Le abilità attive riusano `Ieffetto`? | **Sì**: un'abilità è un `Ieffetto` più un cooldown | Vedi sezione 4.2 |

### Regole che ne derivano

- **Il congelato e il +2 con 2 giocatori:** se A congela B e poi gioca un +2, il "prossimo" è A stesso. A pesca le 2 carte e perde il turno; intanto il turno congelato di B passa. Quindi tocca a B, che non è più congelato. È l'esempio fatto dal team: "dovrei pescare io".
- **Il congelato e il tempo:** un turno di congelamento si consuma ogni volta che il turno "passa sopra" al congelato, anche se nel frattempo qualcun altro salta un turno. Il congelato non perde altri turni oltre al suo.
- **Il Freezer con 2 giocatori:** usa Congela, gioca, e subito dopo gioca di nuovo, perché il turno dell'avversario è saltato. Poi tocca all'avversario.
- **La Roccia e lo stacking:** un accumulo fatto da un solo +2 viene accettato con 0 carte. Un accumulo di due o più carte lo subisce tutto, come gli altri giocatori. Può comunque rispondere a un accumulo con un +2 o superiore.
- **Salesman e roulette:** la roulette non ha lo sconto, come deciso allo Step 4. Nel codice la roulette non passa da `calcolaCartePescaForzata`.
- **Ghost e gli effetti automatici:** la carta 0, il +2 al successivo e la roulette colpiscono anche il Fantasma.

---

## 3. Cosa è stato fatto, passo per passo

### Passo 1 — I dati: `Legend`, `AbilitaAttiva`, configurazione

| Classe | Modifica | Perché |
|---|---|---|
| `Legend` (**nuovo** enum) | Sei valori. Ognuno contiene la passiva (`IPassiva`), l'effetto dell'attiva (`Ieffetto`) e il cooldown | Come `ModalitaDiGioco`, è un enum di **soli dati**, una tabella con una riga per Legend. Il comportamento sta nelle classi che la tabella collega. Nomi e descrizioni li scrive il controller |
| `AbilitaAttiva` (**nuova**) | `Ieffetto` + `cooldown` + `turniDiRicarica`; `isPronta()`, `avviaRicarica()`, `scalaRicarica()` | Il Legend descrive l'abilità ed è uguale per tutti. La ricarica invece è **lo stato di un giocatore**, quindi ogni giocatore ne ha una propria, creata da `Legend.creaAbilitaAttiva()` |
| `OpzionePartita` | nuovo valore `LEGEND` | Un'opzione nuova = un valore nell'enum, come previsto dallo Step 3. Il menu la mostra da solo |
| `ConfigurazionePartita` | nuovo campo `List<Legend> legend`, con la sua validazione | È il primo campo aggiunto dallo Step 3. Le opzioni nuove finiscono nel `Set`, ma un Legend è un dato **per giocatore** e nel `Set` non ci sta. Il costruttore e il metodo `verifica` a 3 parametri restano, per le partite senza Legend |
| `ErroreConfigurazione` | `LEGEND_MANCANTI`, `LEGEND_SENZA_OPZIONE`, `LEGEND_DUPLICATI` | Gli errori previsti si restituiscono come valori (regola del progetto); la frase la scrive il controller |
| `PartitaFactory` | passa a ogni `Giocatore` il suo Legend | Resta l'unico punto che traduce la configurazione in oggetti |
| `Giocatore` | `legend`, `passive`, `passiveTemporanee`, `abilitaAttiva`, `turniCongelati` | Information Expert: il giocatore conosce le sue passive e la sua abilità, quindi risponde lui alle domande che dipendono da quelle (`isBersagliabile()`, `isImmuneAlBlocca()`, `partecipaAiTurni()`) |

**Il "giocatore senza Legend".** Le passive sono una lista, e senza Legend la lista è **vuota**. È un Null Object naturale: chi interroga le passive non deve mai controllare se il Legend esiste. L'abilità attiva invece è `null`. "Non ha un'abilità" non è un comportamento da neutralizzare ma un comando da rifiutare, e il rifiuto sta in `Partita.isAbilitaUsabile()`. La ROADMAP proponeva un Null Object anche per l'abilità. Avrebbe richiesto un effetto "nullo" e un cooldown finto solo per arrivare comunque a rifiutare il comando.

### Passo 2 — Le passive: l'interfaccia `IPassiva`

`IPassiva` ha cinque **hook**. Ognuno ha un `default` che lascia tutto com'è:

| Hook | Default | Chi lo ridefinisce | Chi lo chiama |
|---|---|---|---|
| `modificaPescaForzata(contesto, carte)` | `carte` | La Roccia (`PIU_DUE` → 0), Salesman (`carte - 1`) | `RegoleDiGioco.calcolaCartePescaForzata` |
| `consenteGiocata(candidata, inCima, coloreAttivo)` | `false` | Daltonico (rosso ↔ verde), `PassivaColoreLibero` (Arcobaleno: sempre) | `RegoleDiGioco.isGiocabile` |
| `isBersagliabile()` | `true` | Ghost | `Giocatore.isBersagliabile` → `Partita.getBersagliSelezionabili` |
| `isImmuneAlBlocca()` | `false` | Pelle di ghiaccio | `Giocatore.isImmuneAlBlocca` → `Partita.bloccaProssimo` |
| `getCarteTraCuiScegliereInPesca()` | `1` | Doppia pesca (2) | `RegoleDiGioco.getCarteTraCuiScegliereInPesca` |

Le sette classi (`PassivaRoccia`, `PassivaSalesman`, `PassivaGhost`, `PassivaDaltonico`, `PassivaPelleDiGhiaccio`, `PassivaDoppiaPesca`, `PassivaColoreLibero`) hanno ognuna **un solo metodo**.

In `RegoleDiGioco` la regola della partita e le passive si combinano così:

- **`isGiocabile`:** regola base **OR** qualche passiva lo consente. Una passiva può solo aggiungere giocate, mai toglierne. Con un accumulo in corso contano solo i valori di pesca, quindi le passive sulla compatibilità non valgono (decisione 10).
- **`calcolaCartePescaForzata`:** il numero richiesto passa, una passiva dopo l'altra, dal loro `modificaPescaForzata` (mai sotto 0). Era già il punto previsto dallo Step 1, e `ContestoPesca` contiene già il bersaglio e l'origine.
- **`getCarteTraCuiScegliereInPesca`:** vale più di 1 solo se la strategia di pesca lo ammette (`IStrategiaPesca.ammettePescaConScelta()`, `true` solo per `StrategiaPescaSingola`) **e** una passiva lo chiede. Così la Doppia pesca è spenta in No Mercy senza nessun `if` sulla modalità.

**La Roccia non perde il turno.** Se le passive portano una penalità a 0 carte, `Partita` non fa saltare il turno (`isPenalitaAnnullata`):

- senza stacking, dopo il +2 il Roccia gioca normalmente;
- con lo stacking, se accetta un +2 singolo pesca 0 carte e il suo turno continua in `NORMALE`.

Senza questo, "immune al +2" avrebbe voluto dire "non pesca ma salta comunque il turno". Vedi sezione 6, punto 1.

### Passo 3 — Bersagli: un solo punto, e un Template Method per gli effetti

- `Partita.getBersagliSelezionabili()` sostituisce `getAvversariInGioco()`. Restituisce gli avversari in gioco, non congelati e non protetti da una passiva (Ghost). È **l'unico punto** che decide chi si può scegliere.
- **`EffettoConBersaglio`** (nuova classe astratta, **Template Method**) contiene la parte comune a tutti gli effetti con scelta del giocatore: proporre i bersagli, validare la risposta, trovare il giocatore scelto. Le sottoclassi ridefiniscono solo `applicaAlBersaglio(...)` e, se serve, `ammetteBersaglio(...)`:

| Sottoclasse | Uso | Condizione in più sul bersaglio |
|---|---|---|
| `EffettoScambiaMano` | carta 7 e Scambia mazzo (`abilitaScambiaMazzo()`) | per l'abilità: più di una carta in mano |
| `EffettoPescaBersaglio` | Il Castigatore (4 carte, `OriginePesca.CASTIGATORE`) | — |
| `EffettoRegalaCarte` | Gifted card (2 carte a caso) | — (ma chi la usa deve avere almeno 4 carte: `isAttivabile`) |
| `EffettoCongela` | Congela (1 turno) | — |

**Perché.** Prima la carta 7 faceva tutto da sola. Con quattro abilità nuove lo stesso codice si sarebbe ripetuto cinque volte. Con il Template Method, quando dovesse cambiare la regola dei bersagli (per esempio un Legend nuovo non bersagliabile) si tocca un punto solo.

**Carta 7 senza bersagli.** Se nessuno è selezionabile (per esempio l'unico avversario è Ghost) la 7 si gioca senza scambio, invece di aprire una richiesta senza opzioni. Prima dello Step 6 il caso non esisteva.

### Passo 4 — Il comando `usaAbilita()`

- `Partita.isAbilitaUsabile()` è vero se:
  - la fase è `NORMALE`;
  - il giocatore ha un'abilità;
  - l'abilità è pronta;
  - `effetto.isAttivabile(partita)` è vero.
- `Ieffetto.isAttivabile` è un nuovo metodo `default` (vale `true`), usato solo per le abilità. L'effetto conosce i propri prerequisiti (Information Expert): `EffettoConBersaglio` chiede che ci sia almeno un bersaglio, `EffettoRegalaCarte` anche che il giocatore abbia almeno 4 carte.
- `usaAbilita()` segue la strada di `giocaCarta`: se l'effetto chiede un input si apre un'`AzioneInAttesa` (fase `ATTESA_INPUT`) e si risponde con `rispondiInput`; altrimenti l'abilità si completa subito. **Nessuna fase nuova** e **nessun metodo `rispondiXxx`**.
- `completaAbilita(...)` avvia la ricarica, registra `ABILITA_USATA`, applica l'effetto e poi controlla la Mercy rule. È un nuovo **punto sicuro**: il bersaglio del Castigatore o di Gifted card, o chi riceve una mano grande con Scambia mazzo, può superare le 25 carte. Poi la fase torna `NORMALE`, perché l'abilità non consuma il turno.
- **Ricarica:** `chiudiTurno()`, che è già l'unico punto in cui un turno finisce, chiama `terminaTurno()` sul giocatore che chiude e `iniziaTurno()` su quello che comincia. La ricarica scende solo nei turni giocati davvero: un turno saltato o congelato non conta. Il turno in più di Double card conta, perché il giocatore lo gioca.
- **Arcobaleno** dà al giocatore una `PassivaColoreLibero` **temporanea**, che `terminaTurno()` toglie. Riusa il meccanismo delle passive: in `Partita` non c'è un flag "colore libero".
- **Double card** riusa `EffettoTurnoExtra` (Salta tutti) senza modifiche.

### Passo 5 — Congela: il congelato "come se non ci fosse"

- `Giocatore.turniCongelati` è un contatore, con `congela(n)`, `consumaTurnoCongelato()`, `isCongelato()` e `partecipaAiTurni()` (in gioco **e** non congelato).
- Il metodo che calcola "chi viene dopo" resta **uno solo**, ma ora riceve un criterio: `trovaProssimoIndice(daIndice, criterio)`. Prima si chiamava `trovaProssimoIndiceInGioco` e il criterio era fisso.
  - **Avanzamento vero** (`avanzaGiocatore`): criterio `isInGioco`. Se il turno tocca a un congelato, si consuma un turno di congelamento, si registra `TURNO_CONGELATO` e si va avanti. È l'unico punto in cui il congelamento si consuma, perché è l'unico in cui un turno passa davvero.
  - **"Chi giocherà dopo"** (`getProssimoGiocatore`, usato da +2, roulette, Blocca, Mercy rule): criterio `partecipaAiTurni`. Il congelato viene saltato senza consumare nulla. Per questo il +2 e l'accumulo dello stacking vanno al prossimo non congelato e, con 2 giocatori, tornano a chi ha giocato.
- **Carta 0:** `getGiocatoriPartecipantiNelSensoDiGioco()` esclude i congelati, che tengono la loro mano (decisione del team).
- **Cambio giro:** vale come blocco quando i giocatori che **partecipano ai turni** sono 2. Un congelato non conta (vedi sezione 6, punto 2).
- **Bersagli:** `Giocatore.isBersagliabile()` è falso per un congelato.

Nessun effetto conosce il congelamento: `EffettoCongela` segna il giocatore e basta, il resto lo fanno i punti di `Partita` che c'erano già.

### Passo 6 — Pelle di ghiaccio: perché la carta Blocca chiama `bloccaProssimo()`

Pelle di ghiaccio è una `IPassiva` come le altre (decisione 19): ridefinisce l'hook `isImmuneAlBlocca()`. Ogni hook però ha bisogno di un punto del gioco che lo interroghi.

Il salto del turno passa da `segnalaSaltoTurno()`, che usano in tanti: Blocca, cambio giro con 2 giocatori, +2/+4 senza stacking, roulette. Quel metodo non sa **perché** si salta. Interrogando l'hook lì, il Freezer sarebbe stato immune a **tutti** i salti. Per questo `Effetto_Blocca_Turno` ora chiama un metodo dedicato, `Partita.bloccaProssimo()`. Il metodo chiede al prossimo giocatore se è immune al Blocca: se sì registra `BLOCCO_EVITATO`, altrimenti chiama `segnalaSaltoTurno()`. Gli altri salti restano come prima.

### Passo 7 — Doppia pesca

- `Partita.pescaCarta()` chiede alle regole fra quante carte si sceglie. Se sono più di 1, `pescaConScelta(...)` pesca le carte e apre una richiesta `SCELTA_CARTA` con il meccanismo unificato. Le opzioni sono le descrizioni delle carte pescate.
- `EffettoSceltaCartaPescata` è un `Ieffetto` usato solo per passare da `mettiInAttesa` / `rispondiInput`, come vuole la regola "niente metodi `rispondiXxx`". Tiene la carta scelta e rimette le altre in fondo al mazzo (`Mazzo.aggiungiInFondo`). Poi il turno va in `POST_PESCA` come dopo una pesca normale.
- Se mazzo e scarti danno meno di 2 carte non c'è niente da scegliere: si va direttamente in `POST_PESCA`.
- Vale solo nella partita normale, dove nessuno viene eliminato, quindi non serve controllare la Mercy rule.

### Passo 8 — Eventi, controller, DTO e CLI

| Dove | Modifica |
|---|---|
| `TipoEvento` / `EventoPartita` | Eventi nuovi: `ABILITA_USATA`, `PESCA_RIDOTTA`, `BLOCCO_EVITATO`, `GIOCATORE_CONGELATO`, `TURNO_CONGELATO`, `CARTE_REGALATE`, `CARTA_SCELTA`. Il record ha un campo nuovo, `legend`, per gli eventi delle abilità: il controller ne ricava il nome del Legend o della passiva |
| `OriginePesca` | `CASTIGATORE` |
| `TipoRichiesta` | `SCELTA_CARTA` |
| `UnoLegendsGame` | Setup: `richiedeLegend(opzioni)`, `getLegendDisponibili()`, `descriviLegend(nome)`, e gli overload di `verificaImpostazioni` e `avviaPartita` con la lista dei Legend. Partita: `usaAbilita()`. Tutti i testi di Legend, passive e abilità stanno qui, in `switch` esaustivi sull'enum `Legend` |
| `StatoPartitaDto` | `abilitaDisponibile` (nome dell'abilità usabile adesso, oppure `null`); `Giocatore` ha `legend` e `ricaricaAbilita`; lo stato "congelato". **Nessun DTO nuovo**, come vuole la regola del progetto |
| `UnoLegendsCli` | Se le opzioni richiedono i Legend, ogni giocatore sceglie fra quelli ancora liberi. A inizio turno la voce "3) usa l'abilità …" compare solo se `abilitaDisponibile` non è `null`. Accanto a ogni giocatore si vede il Legend e lo stato della ricarica |

La CLI continua a non importare il model e non sa quale opzione porta i Legend: lo chiede a `richiedeLegend`.

Un dettaglio corretto durante le prove: il messaggio di `PENALITA_ACCETTATA` diceva "accetta la penalità e perde il turno". Con La Roccia non è più sempre vero, quindi ora dice solo "accetta la penalità". Che il turno passi lo racconta lo stato successivo.

---

## 4. Perché queste scelte, e le alternative scartate

### 4.1 Come implementare le passive (decisione 18)

**Il problema.** Le passive cambiano regole che sono **globali**: quante carte si pescano, se una carta è giocabile, chi si può scegliere, se un Blocca ha effetto. Le cambiano però **per un solo giocatore**. Bisognava decidere dove mettere la domanda "questo giocatore ha una passiva che cambia questa regola?".

| Alternativa | Come sarebbe | Perché scartata / scelta |
|---|---|---|
| **A. Enum `Passiva` di soli dati + `if` nelle regole** | `if (bersaglio.haPassiva(SALESMAN)) carte--;` | È la più semplice da leggere. Però la logica di ogni passiva finirebbe sparsa fra regole e `Partita`, e ogni passiva nuova obbligherebbe a modificarle (viola Open/Closed). È proprio il "niente `if` sul tipo concreto" che il progetto vuole evitare. **Scartata** |
| **B. Enum `Passiva` con un corpo per costante** | `SALESMAN { int modificaPescaForzata(...) {...} }` | Polimorfica come l'interfaccia, tutta in un file. Però va contro la convenzione "enum di soli dati" del progetto, e ci starebbero male le passive temporanee (Arcobaleno). **Scartata**, era il ripiego se si volevano meno file |
| **C. Decorator sulle strategie di `RegoleDiGioco`** | Un `SalesmanDecorator` che "avvolge" la strategia di pesca | Le regole sono **una per partita**, le passive sono **di un giocatore**: servirebbe un insieme di regole per ogni giocatore. Molto più complicato. **Scartata** |
| **D. Observer / eventi** | Le passive "ascoltano" gli eventi | Il progetto usa già un registro eventi "pull". Inoltre una passiva deve **cambiare un valore prima** che succeda la cosa, non reagire dopo. **Scartata** |
| **E. Interfaccia `IPassiva` con metodi `default` (hook)** | Ogni passiva è una classe che ridefinisce un solo hook | **Scelta.** È lo **Strategy** applicato al giocatore e, in GRASP, **Polymorphism** + **Protected Variations**. Né le regole né `Partita` sanno quale passiva hanno davanti, e una passiva nuova è una classe nuova. Il limite è che una passiva che interviene in un punto nuovo aggiunge un hook; grazie al `default` le altre classi non si toccano |

### 4.2 Come rappresentare le abilità attive (decisione 20)

| Alternativa | Perché scartata / scelta |
|---|---|
| Nuova interfaccia `IAbilitaAttiva` | Un'abilità fa le stesse cose di una carta: chiede un input, lo valida, modifica la partita. Una seconda interfaccia avrebbe duplicato `Ieffetto` e il meccanismo di input. **Scartata** |
| **`Ieffetto` + cooldown in `AbilitaAttiva`** | **Scelta.** Riusa il meccanismo unificato dello Step 2 e gli effetti esistenti: Scambia mazzo è la carta 7, Double card è Salta tutti. L'unica aggiunta a `Ieffetto` è `isAttivabile` (default `true`) |
| `boolean consumaTurno` per abilità | Previsto quando Congela e Scambia mazzo dovevano consumare il turno. Dopo le decisioni 3–5 nessuna abilità lo consuma, quindi **non è stato aggiunto** |

### 4.3 Il congelato

| Alternativa | Perché scartata / scelta |
|---|---|
| Valore `CONGELATO` in `StatoGiocatore` | Il congelato **è ancora in gioco**: conta per la fine della partita, per Endless e per la classifica. Inoltre serve sapere per quanti turni, quindi servirebbe comunque un contatore. **Scartata** |
| Flag o lista di congelati in `Partita` | Il congelamento è un'informazione del giocatore (Information Expert). **Scartata** |
| Saltare il congelato con `segnalaSaltoTurno()` | Il salto è uno solo e riguarda il prossimo giocatore: non funziona se il congelato non è il prossimo, o se c'è già un altro salto. **Scartata** |
| **Contatore in `Giocatore` + criterio in `trovaProssimoIndice`** | **Scelta.** Il congelato esce dai turni nei due soli punti che decidono "chi viene dopo". Tutti i casi della decisione 6 (stacking, +2, carta 0, bersagli) seguono da soli |

### 4.4 Pelle di ghiaccio

| Alternativa | Perché scartata / scelta |
|---|---|
| Interrogare l'hook dentro `segnalaSaltoTurno()` | Il Freezer sarebbe immune a **tutti** i salti, contro la decisione 9. **Scartata** |
| `segnalaSaltoTurno(MotivoSalto)` con un enum nuovo | Funziona, ma cambia la firma usata da quattro effetti per servirne uno. **Scartata** |
| **Metodo dedicato `bloccaProssimo()` chiamato solo dal Blocca** | **Scelta.** Tocca un solo effetto, e la passiva resta una normale `IPassiva` |

### 4.5 Arcobaleno

| Alternativa | Perché scartata / scelta |
|---|---|
| Flag `coloreLiberoNelTurno` in `Partita` | Un flag in più nel Coordinator e un `if` in più nelle regole. **Scartata** |
| **Passiva temporanea** (`PassivaColoreLibero`) | **Scelta.** Riusa l'hook `consenteGiocata` e la lista di passive del giocatore; `chiudiTurno()` la toglie. Nessun codice nuovo nelle regole |

### 4.6 Altre scelte confermate

- **Catalogo dei Legend: enum di soli dati.** Una factory o un registro di classi darebbe più struttura (Open/Closed completo) per un elenco piccolo, chiuso e deciso a tavolino. **Scartato.**
- **Niente Builder per `RegoleDiGioco`** (da rivalutare allo Step 6, secondo la ROADMAP): le passive sono del giocatore, non della partita, quindi le strategie sono rimaste 7. Il Builder continua a non servire.
- **Niente pattern State** (da rivalutare allo Step 6): le abilità non hanno aggiunto fasi. Usano `NORMALE` → `ATTESA_INPUT` → `NORMALE`, e la Doppia pesca `NORMALE` → `ATTESA_INPUT` → `POST_PESCA`. I controlli delle fasi sono ancora pochi e leggibili.

### 4.7 Riepilogo GRASP / GoF

| Pattern | Dove |
|---|---|
| Strategy + Polymorphism + Protected Variations | `IPassiva` e le sue implementazioni |
| Template Method | `EffettoConBersaglio` |
| Information Expert | `Giocatore` (passive, bersagliabilità, congelamento), `Ieffetto.isAttivabile` (prerequisiti dell'abilità) |
| Null Object | lista di passive vuota per chi gioca senza Legend |
| Command | `AzioneInAttesa`, riusata per le abilità e per la Doppia pesca |
| Creator | `Legend.creaAbilitaAttiva()`, `PartitaFactory` |

---

## 5. Verifiche fatte

Come negli step precedenti (niente Maven né JUnit, scelta del team), le prove sono un **programma esterno** al progetto, compilato con `javac`.

- **Compilazione** con il comando del README (JDK 25): nessun errore. Resta solo l'avviso già presente su un commento Javadoc "orfano" in `Partita`.
- **16 scenari mirati**, con mani e mazzo preparati a mano: **68 controlli, tutti superati.** Tra questi:
  - La Roccia con e senza stacking;
  - Salesman con il +2 e con il Castigatore;
  - Daltonico; Arcobaleno che scade a fine turno;
  - la carta 7 con l'unico avversario Ghost;
  - Freezer con 2 giocatori: Congela e poi il +2 che torna a chi l'ha giocato;
  - Pelle di ghiaccio contro Blocca e contro cambio giro;
  - Gifted card con 3 e con 4 carte;
  - Scambia mazzo con un bersaglio da 1 carta;
  - il cooldown che torna pronto al 4° turno;
  - la Doppia pesca in Standard e in No Mercy; Double card;
  - la carta 0 con un congelato; la validazione dei Legend; Ghost e congelato esclusi da Congela.
- **100 partite simulate** (il limite indicato dal team), da 2 a 6 giocatori, tutte con i Legend assegnati a caso. Metà Standard e metà No Mercy, con Stacking ed Endless alternati. Le ha giocate un "bot" casuale che usa le abilità quando può. Dopo ogni comando il programma ha controllato che:
  - il numero totale di carte non cambiasse;
  - il giocatore di turno fosse in gioco e non congelato;
  - i bersagli proposti non comprendessero mai sé stessi, Ghost, i congelati o chi è fuori gioco;
  - la ricarica restasse tra 0 e il cooldown;
  - ogni richiesta di input avesse almeno un'opzione.

  Il mazzo si mescola a caso, quindi le partite cambiano a ogni esecuzione. Il programma è stato eseguito due volte, l'ultima dopo tutte le correzioni: **ogni volta tutte le 100 partite sono terminate e nessun controllo è fallito** (12.478 comandi e 90.604 controlli nella prima esecuzione, 13.959 e 106.724 nella seconda). Nella prima esecuzione: abilità usate 1.170 volte; passive di pesca attivate 333 volte; Blocca evitati 62; congelamenti 157 e turni congelati consumati 153; carte regalate 220 volte; Doppia pesca 108 volte.
- **CLI provata a mano** con un input da file: menu dei Legend, voce "usa l'abilità", scelta del bersaglio, Salesman sul Castigatore.

**Due errori trovati dalle prove e corretti:**

1. La scelta della carta della Doppia pesca non veniva mai accettata. La descrizione di una carta inizia con un codice colore ANSI (carattere ESC), e `trim()` lo toglieva dalla risposta. Ora il confronto usa `strip()`.
2. `ConfigurazionePartita.verifica` usava `List.contains(null)`, che su una lista immutabile lancia un'eccezione. Ora controlla i `null` con uno stream.

---

## 6. Cosa resta aperto

Scelte fatte durante l'implementazione, coerenti con le decisioni del team ma **da confermare**:

1. **La Roccia non perde il turno sul +2 singolo.** "Immune al +2" è stato interpretato come "il +2 non ha effetto su di lui": né pesca né salto. In stacking, accettando un +2 singolo pesca 0 carte e il turno continua. Se il team preferisce "non pesca ma salta il turno", basta togliere il controllo `isPenalitaAnnullata` in `Partita`.
2. **Cambio giro e congelati.** Il cambio giro vale come blocco quando i giocatori che **partecipano ai turni** sono 2 (il congelato non conta), coerente con "come se non ci fosse".
3. **Il turno extra di Double card conta per la ricarica**, perché è un turno giocato. I turni saltati o congelati no.
4. **Arcobaleno vale per tutto il turno**, quindi anche dopo aver pescato (`POST_PESCA`), non solo per la prima giocata.
5. **Scambia mazzo** non mette condizioni sulla mano di chi la usa: anche con 1 carta si può scambiare con chi ne ha di più.
6. **Un'abilità scelta non si annulla**: dopo "usa l'abilità" bisogna scegliere un bersaglio, come per il colore di un jolly.
7. **L'Acrobata si può scegliere anche in No Mercy**: lì la passiva non fa nulla e la descrizione nel menu lo dice.

Ancora da fare:

- **Bilanciare i cooldown** (Step 7), giocando.
- Aggiornare gli artefatti UP (Use Case, Glossario) con i Legend (Step 7).

---

## 7. File toccati

**Nuovi (model):**
- `IPassiva`, `PassivaRoccia`, `PassivaSalesman`, `PassivaGhost`, `PassivaDaltonico`, `PassivaPelleDiGhiaccio`, `PassivaDoppiaPesca`, `PassivaColoreLibero`;
- `Legend`, `AbilitaAttiva`;
- `EffettoConBersaglio`, `EffettoPescaBersaglio`, `EffettoRegalaCarte`, `EffettoCongela`, `EffettoArcobaleno`, `EffettoSceltaCartaPescata`.

**Modificati (model):**
- `Partita`, `Giocatore`, `RegoleDiGioco`, `Mazzo`, `Ieffetto`;
- `EffettoScambiaMano`, `EffettoPassaMani`, `Effetto_Blocca_Turno`;
- `ConfigurazionePartita`, `PartitaFactory`;
- `OpzionePartita`, `ErroreConfigurazione`, `OriginePesca`, `TipoRichiesta`, `TipoEvento`, `EventoPartita`.

**Modificati (regole):** `IStrategiaPesca`, `StrategiaPescaSingola`.

**Modificati (controller e UI):** `UnoLegendsGame`, `StatoPartitaDto`, `UnoLegendsCli`.

**Documentazione:** questo file, `ROADMAP.md`, `../README.md`, `domain_class_diagram.puml`, `roles_and_patterns.md`, `../../CLAUDE.md`.
