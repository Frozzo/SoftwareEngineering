# Step 5 (quinta iterazione) — Stacking: cosa è stato fatto e perché

**Branch:** `ci-aiuta-claudio` · **Roadmap:** [ROADMAP.md](ROADMAP.md), Step 5 · **Diagramma:** [domain_class_diagram.puml](domain_class_diagram.puml) · **Step precedente:** [step4_quarta_iter.md](step4_quarta_iter.md)

Lo Step 5 aggiunge lo **stacking**: chi riceve una carta di pesca può rispondere con una carta di pesca di valore uguale o superiore. Le carte si accumulano e chi non risponde le pesca tutte, perdendo il turno.

- In **No Mercy** lo stacking è sempre attivo: è un'opzione obbligatoria della modalità.
- Nella **partita normale** è l'opzione **Stacking**, da attivare o no nel menu. Con il mazzo normale la stessa regola dà "+2 ← +2/+4, +4 ← +4".

---

## 1. Le scelte prese con il team

Prima di scrivere codice abbiamo discusso ogni punto aperto. Per ognuno c'era una proposta, che il team ha confermato o corretto.

| # | Domanda                                                          | Decisione                                                                                                                                                                                          | Perché                                                                                                                                                                                                                                                                                                                                                   |
| - | ---------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1 | Come si conosce il "valore di pesca" di una carta (scelta 5.A)   | **Metodo `default int getValorePenalita()` in `Ieffetto`**, che vale 0. `Effetto_pescata_carte` restituisce il suo numero di carte, `EffettoComposito` somma quelli dei suoi effetti | La ROADMAP proponeva un attributo in `ValoreCarta`. Il numero di carte però è già scritto nell'effetto (`new Effetto_pescata_carte(4, …)`): ripeterlo nell'enum vorrebbe dire averlo in due punti, che potrebbero smettere di coincidere. Inoltre il comportamento di una carta dipende dal suo effetto, non dal suo valore (regola del progetto) |
| 2 | Il colore conta quando si risponde?                              | **No**: conta solo il valore, che deve essere uguale o superiore                                                                                                                             | È la regola ufficiale. Se la risposta è colorata il colore attivo diventa il suo; se è un jolly si sceglie il colore come sempre                                                                                                                                                                                                                       |
| 3 | Chi ha una risposta valida può comunque accettare la penalità? | **Sì**                                                                                                                                                                                      | Regola ufficiale. Rispondere è una scelta, non un obbligo                                                                                                                                                                                                                                                                                                |
| 4 | Con quale comando si accetta la penalità?                       | **Comando dedicato `accettaPenalita()`**                                                                                                                                                   | Il nome dice cosa succede. `pescaCarta()` non deve cambiare significato a seconda della fase. La CLI ha già uno `switch` sulla fase, quindi le costa solo un caso in più                                                                                                                                                                            |
| 5 | +4 cambio giro con 2 giocatori                                   | **"Arma a doppio taglio"**: il cambio giro fa saltare il turno all'avversario, quindi l'accumulo torna a chi ha giocato la carta. Lui può rispondere al proprio accumulo oppure accettarlo  | Scelta del team. **Ci si discosta dalla regola ufficiale**, dove l'avversario pesca e salta. Si ottiene senza codice speciale: il salto viene dal cambio giro, non dal +4 (vedi 2.4)                                                                                                                                                                |
| 6 | La carta iniziale di pesca apre l'accumulo?                      | **Sì**: la carta iniziale e una carta giocata a metà partita seguono la stessa strada                                                                                                      | Nessun caso speciale per l'inizio (vedi 2.5)                                                                                                                                                                                                                                                                                                              |
| 7 | Pattern State al posto dell'enum `FaseTurno`                   | **No, non ancora**                                                                                                                                                                           | Con una sola fase in più i controlli sono ancora pochi e leggibili. Si rivaluta allo Step 6 con le abilità attive                                                                                                                                                                                                                                       |
| 8 | Builder per `RegoleDiGioco`                                    | **No, non ancora**                                                                                                                                                                           | Scelta del team già presa allo Step 3: `RegoleDiGioco.standard(opzioni)` resta più leggibile di `builder()...build()`. Le strategie passano da 6 a 7; si rivaluta allo Step 6 con le passive                                                                                                                                                        |
| 9 | Maven + JUnit                                                    | **No**                                                                                                                                                                                       | Scelta del team. Si continua come negli step precedenti: le prove sono un programma esterno al progetto, compilato con `javac`, che gioca al massimo 100 partite                                                                                                                                                                                        |

### Regole che ne derivano

- Si risponde solo con una carta il cui valore di pesca è **≥ valore dell'ultima carta dell'accumulo** (non del totale). Esempio: su un +2 seguito da un +4 si risponde con +4, +6 o +10.
- Il Jolly cambio giro +4 vale 4. Il +4 colorato della No Mercy vale 4.
- La **roulette non si accumula**: non è una carta di pesca a valore fisso, quindi vale 0 e funziona come prima.
- Durante la risposta allo stacking **non si può pescare volontariamente**: o si risponde o si accetta.
- Chi accetta pesca il totale e il suo turno finisce lì: questo è il "perdere il turno".

---

## 2. Cosa è stato fatto, passo per passo

### Passo 1 — Il valore di pesca sta negli effetti

- `Ieffetto` ha un metodo nuovo `default int getValorePenalita()`, che restituisce 0.
- `Effetto_pescata_carte` lo ridefinisce e restituisce il suo `numeroCarteDaPescare`. Il costruttore ora rifiuta un numero di carte ≤ 0.
- `EffettoComposito` restituisce la **somma** dei valori dei suoi effetti. Il +4 cambio giro (`cambia colore + cambio giro + pesca 4`) vale 4, il +6 vale 6, il +10 vale 10.
- La roulette, la 7, lo 0 e tutte le altre carte valgono 0, quindi non possono mai rispondere a un accumulo.

**Perché.** È la scelta 1 (sezione 1). Il numero 4 di un +4 è scritto una volta sola, nell'effetto. Le regole chiedono "quanto vale?" con un metodo polimorfico, senza `instanceof` e senza un elenco di `ValoreCarta`. Il Composite risponde da solo, senza che nessuno guardi dentro la sua lista.

### Passo 2 — I dati nuovi

| Classe                                | Modifica                                                                                                                              | Perché                                                                                                                                                                                                                                                                                             |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Accumulo` (**nuovo** record) | `totale`, `valoreUltimaCarta`, `numeroCarte`, `originePrimaCarta`; `apri(...)`, `aggiungi(...)`, `getOriginePenalita()` | Scelta 5.B della ROADMAP. Il totale serve a chi accetta, il valore dell'ultima carta è la soglia per rispondere. Numero di carte e origine distinguono "un +2 da solo" da un accumulo vero, come servirà a La Roccia allo Step 6. È immutabile: `Partita` lo sostituisce invece di modificarlo |
| `OriginePesca`                      | nuovo valore `ACCUMULO`                                                                                                             | È la causa della pesca quando chi accetta subisce **due o più** carte. Con una carta sola l'origine resta quella della carta (`PIU_DUE`…)                                                                                                                                                |
| `OpzionePartita`                    | nuovo valore `STACKING`                                                                                                             | Un'opzione nuova = un valore qui, come previsto dallo Step 3                                                                                                                                                                                                                                        |
| `ModalitaDiGioco`                   | `NO_MERCY` ha `STACKING` tra le opzioni obbligatorie                                                                              | In No Mercy lo stacking è sempre attivo. `ConfigurazionePartita` lo aggiunge da solo e il menu non lo chiede, senza codice nuovo                                                                                                                                                                 |
| `FaseTurno`                         | nuovo valore `RISPOSTA_STACKING`                                                                                                    | Regola del progetto: la fase del turno è un valore dell'enum, non un flag booleano                                                                                                                                                                                                                 |
| `TipoEvento` / `EventoPartita`    | `ACCUMULO_AUMENTATO` (totale in `quantita`) e `PENALITA_ACCETTATA`                                                              | La UI racconta cosa succede. `ACCUMULO_AUMENTATO` non indica un giocatore, perché la carta iniziale non l'ha giocata nessuno                                                                                                                                                                     |

### Passo 3 — La regola: `IStrategiaAccumuloPenalita`

Nuova interfaccia in `domain.rules`, con due metodi:

- `isAccumuloAttivo()`: le carte di pesca si accumulano?
- `puoRispondere(candidata, accumulo)`: questa carta può rispondere?

Ha due implementazioni:

| Strategia                            | Quando                                           | Comportamento                                                              |
| ------------------------------------ | ------------------------------------------------ | -------------------------------------------------------------------------- |
| `StrategiaNessunAccumulo`          | senza l'opzione Stacking                         | Non accumula: ogni carta di pesca si applica subito, come fino allo Step 4 |
| `StrategiaAccumuloValoreCrescente` | con l'opzione Stacking, in entrambe le modalità | `valore > 0 && valore >= valoreUltimaCarta`. Il colore non conta         |

**Una sola strategia per le due modalità.** La ROADMAP prevedeva anche una `StrategiaAccumuloNoMercy`, ma non serve. Con il mazzo normale, che ha solo +2 e +4, la regola "valore uguale o superiore" dà già "+2 ← +2/+4, +4 ← +4", che è la decisione presa per la partita normale. Con il mazzo No Mercy dà la tabella ufficiale. Cambia il mazzo, non la regola.

In `RegoleDiGioco`:

- la settima strategia arriva dal costruttore;
- un metodo privato `accumulo(opzioni)`, costruito come il `finePartita(opzioni)` dell'opzione Endless, la sceglie per entrambe le modalità;
- `isAccumuloAttivo()` è nuovo;
- `isGiocabile(...)` è cambiato: se c'è un accumulo in corso chiede `puoRispondere`, altrimenti controlla la compatibilità come prima.

**Perché in `isGiocabile`.** Resta un'unica domanda "si può giocare questa carta?". Così `Partita.giocaCarta` non cambia logica e non deve sapere quale regola vale in quel momento. Allo Step 6 le passive che cambiano la giocabilità (Daltonico) si agganceranno nello stesso punto.

### Passo 4 — `Partita`: chi pesca, chi risponde, chi accetta

1. **`applicaPenalitaPesca(n, origine)`** (nuovo, package-private). È l'unico punto d'ingresso delle penalità delle carte di pesca. `Effetto_pescata_carte` ora chiama solo questo metodo.

   - **Senza stacking:** `applicaPescaForzata(prossimo, n, origine)` e poi `segnalaSaltoTurno()`. È esattamente quello che faceva prima l'effetto.
   - **Con lo stacking:** nessuno pesca e **nessun turno viene saltato**. L'accumulo si apre (`Accumulo.apri`) o cresce (`aggiungi`) e si registra `ACCUMULO_AUMENTATO`.
2. **`chiudiTurno()`** decide la fase del nuovo turno: `RISPOSTA_STACKING` se c'è un accumulo, altrimenti `NORMALE`. È l'unica modifica necessaria perché l'accumulo "passi al successivo", e vale qualunque sia il motivo per cui il turno si chiude: giocata normale, finestra UNO, vittoria in Endless, eliminazione, carta iniziale.
3. **`giocaCarta`** ammette anche la fase `RISPOSTA_STACKING`. Quali carte siano valide lo decide `isGiocabile` (passo 3).
4. **`accettaPenalita()`** (nuovo comando pubblico), permesso solo in `RISPOSTA_STACKING`, nell'ordine:

   1. registra `PENALITA_ACCETTATA`;
   2. azzera l'accumulo;
   3. fa pescare il totale con `applicaPescaForzata`, quindi le regole (e allo Step 6 Salesman) possono modificare il numero, e l'evento `PESCA_FORZATA` si registra da solo, con origine `getOriginePenalita()`;
   4. controlla la Mercy rule;
   5. chiude il turno.

   "Perdere il turno" vuol dire proprio che il turno di chi accetta finisce lì: non serve nessun flag di salto.
5. **`getAccumulo()`** per le regole e per il controller. **`concludiPartita()`** azzera anche l'accumulo.

`pescaCarta()` e `passaTurno()` non sono stati toccati. La loro lista di ammissione non contiene `RISPOSTA_STACKING`, quindi durante lo stacking vengono rifiutati da soli. È il vantaggio della "lista di ammissione" introdotta allo Step 2.

### 2.4 Il +4 cambio giro con 2 giocatori (decisione 5)

Non c'è codice dedicato: il comportamento viene dai pezzi che esistevano già.

1. L'effetto composito esegue in ordine: cambia colore, cambio giro, +4.
2. `cambioGiro()` vede 2 giocatori in gioco e chiama `segnalaSaltoTurno()`. Questo c'era già dallo Step 4.
3. Il +4 apre l'accumulo, ma con lo stacking non fa saltare nessuno.
4. `chiudiTurno()` passa il turno all'avversario, che deve saltare, quindi il turno torna a chi ha giocato la carta, nella fase `RISPOSTA_STACKING`.
5. Chi ha giocato la carta può rilanciare sul proprio accumulo (+4, +6, +10) oppure accettare e pescare.

### 2.5 La carta iniziale (decisione 6)

`iniziaGioco()` ora chiude con `chiudiTurno()` invece che con `aggiornaGiocatoreAttivo()`. La carta iniziale segue così la stessa strada di una carta giocata a metà partita. Ci sono tre casi.

| Carta iniziale            | Senza stacking (come prima)                                      | Con lo stacking                                                                                                                                          |
| ------------------------- | ---------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| +2                        | Il primo giocatore pesca 2 e salta; inizia il secondo            | Il **primo giocatore** deve rispondere a un accumulo di 2                                                                                          |
| +4 nero (mazzo normale)   | Il primo giocatore sceglie il colore, il secondo pesca 4 e salta | Il primo giocatore sceglie il colore, il **secondo** risponde a un accumulo di 4                                                                   |
| +4 cambio giro (No Mercy) | —                                                               | Il primo giocatore sceglie il colore, il senso si inverte e risponde l'**ultimo** giocatore. Con 2 giocatori l'accumulo torna al primo (punto 2.4) |

L'asimmetria tra +2 e +4 nero esisteva già. Con un jolly iniziale il primo giocatore deve scegliere il colore, quindi fa da "chi ha giocato la carta". Non è stata introdotta ora (vedi lo Step 4, sezione 5, punto 3).

> Nota: nella discussione iniziale si era detto che con un +2 iniziale avrebbe risposto "il giocatore 1". In realtà risponde il **primo giocatore** (`G0`), cioè quello a cui prima toccava pescare. È lo stesso schema "il primo di turno subisce la carta iniziale", e con lo stacking può rispondere.

### Passo 5 — Controller, DTO e CLI

- **`StatoPartitaDto`:**

  - nuovo valore `Fase.RISPOSTA_STACKING`;
  - nuovo tipo interno `Accumulo(carteDaPescare, valoreMinimoRisposta)`, `null` se non c'è uno stacking.

  Segue la regola del progetto: niente file DTO separati, un pezzo nuovo dello stato diventa un tipo interno.
- **`UnoLegendsGame`:**

  - `accettaPenalita()` inoltra il comando;
  - `convertiAccumulo(...)` converte i dati;
  - ci sono i testi dell'opzione Stacking, dei due eventi nuovi e dell'origine `ACCUMULO`.

  Gli `switch` esaustivi hanno obbligato a scrivere ogni testo: la compilazione falliva finché mancava un caso.
- **`UnoLegendsCli`:** nuovo caso `RISPOSTA_STACKING` nello `switch` sulla fase. Mostra quante carte sono accumulate e la soglia ("rispondi con una carta +4 o superiore, di qualsiasi colore"), poi offre tre scelte: rispondi, accetta, esci. La CLI non sa quali carte siano valide: se una carta non va bene, il model rifiuta la giocata.
- **Menu:** l'opzione Stacking compare solo nella partita Standard. In No Mercy non viene chiesta perché è obbligatoria.

Esempio dei messaggi durante uno stacking:

```text
Stacking: l'accumulo sale a 2 carte.
Stacking: l'accumulo sale a 4 carte.
G2 non risponde allo stacking: accetta la penalita e perde il turno.
G2 pesca 4 carte accumulate con lo stacking.
```

---

## 3. Perché queste scelte (GRASP/GoF)

| Principio                                         | Dove si vede in questo step                                                                                                                                                               |
| ------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Protected Variations**                    | `Effetto_pescata_carte` non sa se lo stacking è attivo: lo stesso effetto funziona con e senza. La differenza sta dietro `IStrategiaAccumuloPenalita`                                |
| **Strategy**                                | `StrategiaNessunAccumulo` / `StrategiaAccumuloValoreCrescente`, scelte dalle opzioni come Endless                                                                                     |
| **Polimorfismo al posto di `instanceof`** | `getValorePenalita()` risponde a "quanto vale questa carta?" senza controlli sul tipo                                                                                                   |
| **Composite**                               | `EffettoComposito.getValorePenalita()` somma i figli: i jolly +4 cambio giro, +6 e +10 non hanno codice nuovo                                                                           |
| **Information Expert**                      | L'effetto conosce il proprio valore; `Accumulo` sa qual è l'origine della penalità; `Partita` decide la fase, perché conosce turno e accumulo                                      |
| **Controller / Coordinator**                | `Partita.accettaPenalita()` coordina pesca, Mercy rule e cambio di turno; `UnoLegendsGame` si limita a inoltrare                                                                      |
| **Immutabilità**                           | `Accumulo` è un record: nessuno lo modifica "a metà", si sostituisce                                                                                                                  |
| **Low Coupling**                            | La CLI conosce solo il DTO; il menu legge le opzioni dal controller e non ha saputo nulla dello stacking                                                                                  |
| **Un solo punto di decisione**              | La fase del nuovo turno la decide solo `chiudiTurno()`. La penalità delle carte di pesca passa solo da `applicaPenalitaPesca`. La pesca vera passa sempre da `applicaPescaForzata` |

**Perché non il pattern State (decisione 7).** Le fasi sono 6. I controlli "in quale fase è permesso?" sono una riga per comando, e lo stacking ne ha toccate solo due (`giocaCarta` e il nuovo `accettaPenalita`). Il pattern State porterebbe 6 classi nuove senza togliere complessità vera. Si rivaluta allo Step 6, quando l'uso delle abilità attive attraverserà più fasi.

**Perché non il Builder (decisione 8).** Il costruttore lungo di `RegoleDiGioco` lo chiamano solo i due metodi `standard(opzioni)` e `noMercy(opzioni)`, ognuno con una riga per strategia. Per chi legge `RegoleDiGioco.noMercy(opzioni)` è ancora più chiaro di una catena `builder()...build()`.

---

## 4. Verifiche fatte

Come negli step precedenti, le prove sono un **programma esterno al progetto, non incluso** (decisione 9: niente Maven né JUnit). È compilato con `javac` insieme ai sorgenti.

- **Compilazione** con il comando del README: nessun errore.
- **Scenari costruiti a mano**, uno per ogni decisione presa:

  - S1: +2 su +2 di colore diverso; il colore diventa quello della risposta; un numero non può rispondere; durante lo stacking `pescaCarta` e `passaTurno` sono rifiutati; il terzo giocatore accetta e pesca 4 con origine `ACCUMULO`, poi tocca al primo in fase `NORMALE`; `accettaPenalita` senza accumulo viene rifiutato.
  - S2: chi ha un +2 può comunque accettare; con una carta sola l'origine resta `PIU_DUE`.
  - S3: partita normale con lo stacking: +4 nero su +2, poi un +2 non basta e un +4 sì; con i jolly si sceglie il colore e l'accumulo arriva a 10.
  - S4: **regressione** senza stacking: il +2 fa pescare subito e saltare il turno, senza accumulo.
  - S5: No Mercy con 2 giocatori. Dopo il +4 cambio giro l'avversario salta, l'accumulo torna a chi ha giocato, che rilancia con +6. Un +4 colorato non copre il +6 e l'avversario pesca 10. In S5b chi ha giocato accetta il proprio +4.
  - S6: roulette, Salta tutti e blocca non possono rispondere; un +4 blu risponde a un +2 rosso.
  - S7: un +2 iniziale con lo stacking fa rispondere il primo giocatore, senza salti. In S7b, senza stacking, funziona come prima. In S7c, con un +4 nero iniziale, il primo sceglie il colore e risponde il secondo.
  - S8: in Endless chi risponde con l'ultima carta esce e l'accumulo passa al successivo. In S8b, nella partita normale, chi risponde con l'ultima carta vince.
  - S9: chi risponde con la penultima carta apre la finestra UNO; la penalità UNO (2 carte) resta separata e l'accumulo passa dopo.
  - S10: Mercy rule: accettando un +6 si arriva oltre 25 carte, il giocatore è eliminato e il turno passa senza accumulo.
  - S11: `getValorePenalita()` di tutte le carte di pesca (+2, +4 colorato e nero, +4 cambio giro, +6, +10), della roulette e della 7.
  - S12: `STACKING` è obbligatoria in No Mercy e configurabile nella partita normale.
- **Simulazione: 100 partite in tutto** (limite indicato dal team), da 2 a 6 giocatori giocate da un "bot" casuale. Sono 20 per ciascuna configurazione: Normale, Normale + Stacking, Normale + Stacking + Endless, No Mercy, No Mercy + Endless. A ogni comando il programma ha controllato che:

  - le carte totali (mazzo, scarti e mani) restino sempre 108 o 168;
  - in `RISPOSTA_STACKING` ci sia sempre un accumulo, e in `NORMALE` e `POST_PESCA` mai;
  - le carte rifiutate durante lo stacking abbiano davvero un valore insufficiente, e quelle valide vengano accettate;
  - dopo `accettaPenalita` l'accumulo sia azzerato;
  - il giocatore di turno sia in gioco.

  Tutte le 100 partite sono terminate con una classifica completa. In totale ci sono state 337 risposte allo stacking e 1.013 accettazioni, con un accumulo massimo di 28 carte. 7 accumuli sono tornati a chi li aveva creati (il caso del punto 2.4) e 87 giocatori sono stati eliminati accettando un accumulo.
- **Esito:** 85.138 controlli, 0 falliti.
- **CLI:** il menu Standard chiede Endless e Stacking, quello No Mercy solo Endless. I messaggi dello stacking sono stati letti passando dal controller vero (esempio nel passo 5).

---

## 5. Cosa resta aperto

| # | Questione                               | Note                                                                                                                                                                                                              |
| - | --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1 | **La Roccia e Salesman** (Step 6) | Il terreno è pronto: con `getOriginePenalita()` un +2 singolo accettato ha origine `PIU_DUE`, un accumulo ha `ACCUMULO`. L'accettazione passa da `calcolaCartePescaForzata`, dove si aggancerà Salesman |
| 2 | **Pattern State**                 | Rimandato allo Step 6 (decisione 7)                                                                                                                                                                               |
| 3 | **Builder per `RegoleDiGioco`** | Rimandato allo Step 6 (decisione 8)                                                                                                                                                                               |
| 4 | **Carta iniziale nera**           | L'asimmetria "+2 iniziale colpisce il primo, +4 nero iniziale colpisce il secondo" è ereditata dagli step precedenti. Si può rivedere allo Step 7                                                               |

---

## 6. File toccati

| File                                                                                                                 | Modifica                                                                                                                                                                                                     |
| -------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `model/Accumulo.java`                                                                                              | **nuovo**: record dell'accumulo                                                                                                                                                                        |
| `domain/rules/IStrategiaAccumuloPenalita.java`                                                                     | **nuovo**: interfaccia della strategia di stacking                                                                                                                                                     |
| `domain/rules/impl/StrategiaNessunAccumulo.java`, `domain/rules/impl/StrategiaAccumuloValoreCrescente.java`      | **nuovi**: le due strategie                                                                                                                                                                            |
| `model/Ieffetto.java`, `model/EffettoComposito.java`, `model/Effetto_pescata_carte.java`                       | `getValorePenalita()`; l'effetto di pesca chiama `applicaPenalitaPesca`                                                                                                                                  |
| `model/Partita.java`                                                                                               | campo `accumulo`, `getAccumulo`, `applicaPenalitaPesca`, `accettaPenalita`, fase decisa in `chiudiTurno`, `iniziaGioco` chiude con `chiudiTurno`, `giocaCarta` ammette `RISPOSTA_STACKING` |
| `model/RegoleDiGioco.java`                                                                                         | settima strategia, `isAccumuloAttivo`, `isGiocabile` con l'accumulo, `accumulo(opzioni)`                                                                                                               |
| `model/FaseTurno.java`, `model/OriginePesca.java`, `model/OpzionePartita.java`, `model/TipoEvento.java`      | nuovi valori                                                                                                                                                                                                 |
| `model/EventoPartita.java`                                                                                         | factory `accumuloAumentato`, `penalitaAccettata`                                                                                                                                                         |
| `model/ModalitaDiGioco.java`                                                                                       | `STACKING` obbligatoria in `NO_MERCY`                                                                                                                                                                    |
| `controller/StatoPartitaDto.java`                                                                                  | `Fase.RISPOSTA_STACKING`, tipo interno `Accumulo`                                                                                                                                                        |
| `controller/UnoLegendsGame.java`                                                                                   | `accettaPenalita()`, conversione dell'accumulo, testi                                                                                                                                                      |
| `ui/UnoLegendsCli.java`                                                                                            | turno di risposta allo stacking                                                                                                                                                                              |
| `README.md` (radice), `Codice/README.md`, `docs/ROADMAP.md`, `docs/domain_class_diagram.puml`, `CLAUDE.md` | documentazione allineata. Nel diagramma sono stati tolti anche due metodi di `EffettoComposito` (`contaEffettiConInput`, `contieneEffetto`) che non esistono più nel codice                           |
