# Step 2 (seconda iterazione) — Cosa è stato fatto e perché

**Branch:** `ci-aiuta-claudio` · **Roadmap:** [ROADMAP.md](ROADMAP.md), Step 2 · **Diagramma:** [GiocaTurno_Step2_SequenceDiagram.puml](GiocaTurno_Step2_SequenceDiagram.puml)

Lo Step 2 non aggiunge regole di gioco: la partita si gioca esattamente come prima.
Cambia **come `Partita` sa in che punto del turno si trova**, **come chiede un input al giocatore**
e **come racconta alla UI cosa è successo**. Sono le tre basi che servono a No Mercy e ai Legend,
che porteranno molte nuove fasi, richieste e messaggi.

---

## 1. Il problema di partenza

| Aspetto               | Com'era                                                                                                                                                                                                | Perché era un problema                                                                                                                                                                                                                                       |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Fase del turno        | Dedotta da più variabili: `cartaPescataDaGiocare`, `effettoInizialeInAttesa`, `giocatoreInAttesaDichiarazioneUno`, `vincitore != null`                                                        | Ogni comando doveva elencare i casi da **rifiutare**. Dimenticarne uno era facile: `pescaCarta()` non controllava `effettoInizialeInAttesa`, quindi si poteva pescare prima di aver scelto il colore della carta iniziale. Lo impediva solo la CLI. |
| Richieste di input    | Tre meccanismi: `rispondiRichiestaInputIniziale` per la carta iniziale; doppia chiamata `giocaCarta(i, null)` e poi `giocaCarta(i, risposta)` per un jolly; `completaDichiarazioneUno` per UNO | Ogni richiesta nuova (bersaglio della carta 7, abilità attive, colore della roulette) avrebbe avuto il suo metodo e il suo ramo nella CLI. Con la doppia chiamata la CLI doveva ricordarsi l'indice e `Partita` rifaceva tutte le validazioni.             |
| Testo delle richieste | La frase "Scegli il colore…" era scritta nel model (`Effetto_Cambia_colore`)                                                                                                                        | Testo di interfaccia nel dominio: poco MVC.                                                                                                                                                                                                                   |
| Cosa è successo      | Solo il valore di ritorno (`EsitoGiocata` → `EsitoGiocataDto`); la CLI inventava i messaggi                                                                                                       | Nessuno diceva "G2 pesca 4 carte" o "G2 salta il turno". Ogni informazione nuova avrebbe aggiunto un campo a `EsitoGiocata`.                                                                                                                                |
| CLI                   | Catena di `if` in un ordine preciso (`isPartitaTerminata`, poi `getRichiestaInput() != null`, poi `isDeveGiocareCartaPescata`…)                                                               | La CLI ricostruiva la fase invece di leggerla.                                                                                                                                                                                                                |

---

## 2. Cosa è stato fatto

### 2.1 Fase del turno esplicita: `FaseTurno`

Nuovo enum [`FaseTurno`](../src/main/java/unolegends/model/FaseTurno.java), con un solo campo `fase` in `Partita`:

| Fase             | Comandi accettati                                           |
| ---------------- | ----------------------------------------------------------- |
| `NORMALE`      | `giocaCarta`, `pescaCarta`                              |
| `POST_PESCA`   | `giocaCarta` (qualsiasi carta della mano), `passaTurno` |
| `ATTESA_INPUT` | `rispondiInput`                                           |
| `ATTESA_UNO`   | `completaDichiarazioneUno`                                |
| `TERMINATA`    | nessuno                                                     |

Ogni comando comincia con il controllo delle fasi in cui è **permesso**:

```java
public boolean pescaCarta() { if (fase != FaseTurno.NORMALE) return false; ... }
public boolean passaTurno() { if (fase != FaseTurno.POST_PESCA) return false; ... }
```

**Perché:**

- **Gli stati assurdi non si possono rappresentare.** Con più flag erano possibili combinazioni senza senso (attesa UNO e carta iniziale da colorare insieme). Con un solo campo la partita è sempre in una fase sola.
- **È sicuro per default.** Una fase aggiunta in futuro (es. `RISPOSTA_STACKING` dello Step 5) viene rifiutata da tutti i comandi finché non la si abilita esplicitamente. Con i flag andava aggiunta a mano all'`if` di ogni comando.
- **Il difetto di `pescaCarta` è sparito da solo**, perché il controllo è passato da "cosa vieto" a "cosa permetto".
- **La UI legge la fase invece di dedurla.**
- **Corrisponde uno a uno a un diagramma degli stati UML**, utile nella relazione.

**Variabili eliminate:**

- `cartaPescataDaGiocare` → `POST_PESCA`;
- `effettoInizialeInAttesa` → `AzioneInAttesa`;
- `giocatoreInAttesaDichiarazioneUno` → in `ATTESA_UNO` il giocatore in attesa è sempre quello attivo, perché il turno non è ancora avanzato.

**Variabili rimaste, di proposito:**

- `saltaProssimoTurno` non è una fase ma un'informazione per il prossimo avanzamento;
- `vincitore` è un dato, la fase è `TERMINATA`;
- `giocoIniziato` riguarda il ciclo di vita, non il turno.

**Alternativa scartata, per ora: pattern State** (una classe per fase). È più pulito, ma richiede tante classi che devono modificare `Partita`. Progettarle adesso vorrebbe dire indovinare fasi che ancora non conosciamo bene. Si rivaluta allo Step 5, con lo stacking, se gli `if` sulla fase diventano troppi.

### 2.2 Input unificato: `AzioneInAttesa` + `rispondiInput`

- [`AzioneInAttesa`](../src/main/java/unolegends/model/AzioneInAttesa.java) è un record package-private `(Ieffetto effetto, Consumer<String> completamento)`. È il pattern **Command**:
  - `effetto` sa cosa chiedere (`getRichiestaInput`) e se la risposta è valida (`validaRispostaInput`): Information Expert, come prima;
  - `completamento` è ciò che `Partita` deve fare quando la risposta arriva.
- `Partita.mettiInAttesa(effetto, completamento)` è l'unico modo in cui si apre una richiesta; porta la fase in `ATTESA_INPUT`.
- `Partita.rispondiInput(String)` è l'unico modo in cui si risponde:
  1. valida con l'effetto;
  2. se la risposta è valida, riporta la fase a `NORMALE` ed esegue il completamento, che decide la fase successiva.
- `giocaCarta(int)` non riceve più la risposta. Se la carta richiede input:
  - la carta **resta in mano**;
  - il completamento è `risposta -> completaGiocata(carta, risposta)`;
  - si conserva il riferimento alla carta, non l'indice. Per questo `Giocatore` ha il nuovo metodo `rimuoviCarta(Carta)`.
- La **carta iniziale** nera usa lo stesso meccanismo, con il completamento `risposta -> { effetto.attivaEffetto(this, risposta); chiudiTurno(); }`. `rispondiRichiestaInputIniziale` è stato eliminato.
- `completaGiocata` (privato) raccoglie la parte finale comune a giocate con e senza input:
  - sposta la carta;
  - applica l'effetto;
  - decide tra vittoria, finestra UNO e `chiudiTurno()`.

**Perché il completamento è una funzione e non un `if`:** `rispondiInput` non deve sapere *che cosa* stava aspettando (giocata, carta iniziale, in futuro un'abilità usata a metà turno). Un'abilità attiva potrà salvare un completamento che, dopo l'effetto, riporta la fase a `NORMALE` o `POST_PESCA` senza consumare il turno, come richiesto dalle decisioni prese. Non servirà toccare `rispondiInput`.

**Perché la dichiarazione UNO resta separata:** ha il timer di 5 secondi ed è un'interazione diversa da "scegli fra delle opzioni". Ora però è una fase (`ATTESA_UNO`) come le altre.

### 2.3 Richieste tipizzate: `TipoRichiesta`

- `RichiestaInput` passa da `(String messaggio, List<String> opzioni)` a `(TipoRichiesta tipo, List<String> opzioni)`.
- [`TipoRichiesta`](../src/main/java/unolegends/model/TipoRichiesta.java) ha due valori:
  - `SCELTA_COLORE`;
  - `SCELTA_GIOCATORE`, già predisposto per la carta 7 e per Castigatore, Gifted card e Scambia mazzo.
- La frase la scrive il controller (`UnoLegendsGame.messaggioPer`).

**Perché il controller e non la CLI** (la roadmap diceva CLI): così tutti i testi, frasi delle richieste e messaggi degli eventi, stanno in un solo strato, quello che già traduce il model per la UI (per esempio `formattaCarta`). La CLI resta pura presentazione e `RichiestaInputDto` non è cambiato.

### 2.4 Eventi verso la UI: registro "pull" invece dell'Observer

- [`TipoEvento`](../src/main/java/unolegends/model/TipoEvento.java) ha questi valori: `PESCA_VOLONTARIA`, `PESCA_FORZATA`, `TURNO_SALTATO`, `SENSO_INVERTITO`, `COLORE_SCELTO`, `UNO_DICHIARATO`.
- [`EventoPartita`](../src/main/java/unolegends/model/EventoPartita.java) è un record `(tipo, nomeGiocatore, quantita, origine, colore)` con factory statiche (`pescaForzata(...)`, `turnoSaltato(...)`…).
- `Partita` ha:
  - una lista `eventi`;
  - `registraEvento(...)` (package-private);
  - `prelevaEventi()`, che restituisce gli eventi e svuota il registro.
- `UnoLegendsGame.prelevaMessaggi()` traduce gli eventi in frasi con `switch` esaustivi su `TipoEvento` e `OriginePesca`.
- La CLI, a ogni giro del ciclo, stampa `controller.prelevaMessaggi()`.

Dove si registrano gli eventi, sempre in un **punto unico**:

| Evento               | Dove                         | Perché lì                                                                                                                                                              |
| -------------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `PESCA_FORZATA`    | `applicaPescaForzata`      | È già l'unico ingresso di tutte le pesche forzate (Step 1): +2, +4, penalità UNO e in futuro Castigatore e stacking producono il messaggio senza toccare gli effetti. |
| `TURNO_SALTATO`    | `aggiornaGiocatoreAttivo`  | È l'unico punto in cui il salto viene consumato.                                                                                                                        |
| `SENSO_INVERTITO`  | `cambioGiro`               |                                                                                                                                                                          |
| `COLORE_SCELTO`    | `applicaColoreScelto`      |                                                                                                                                                                          |
| `PESCA_VOLONTARIA` | `pescaCarta`               | La quantità servirà a No Mercy ("pesca finché trovi una carta giocabile").                                                                                            |
| `UNO_DICHIARATO`   | `completaDichiarazioneUno` |                                                                                                                                                                          |

**Perché non l'Observer** (che la roadmap inizialmente consigliava):

- La CLI è **sincrona**: dà un comando e aspetta la risposta. Le basta leggere, dopo ogni comando, cosa è successo.
- L'Observer conviene quando chi ascolta deve reagire a eventi che non controlla (una GUI, la rete, un timer). Qui avrebbe richiesto due interfacce, le implementazioni e la regola "i callback non devono richiamare comandi", solo per stampare delle righe.
- Il registro costa un enum e un record, non ha callback e quindi nessun rischio di rientrare in `Partita` mentre sta eseguendo una giocata.

**Perché eventi tipizzati e non stringhe:** il model non produce testo (resta MVC) e gli `switch` esaustivi obbligano, a ogni evento nuovo, a scriverne il messaggio. Se un giorno servisse una GUI, gli eventi esistono già come oggetti e trasformare il registro in un Observer è un passo breve.

**Limite noto:** `origine` e `colore` sono `null` per i tipi che non li usano. Le factory statiche lo rendono esplicito. Se i tipi di evento diventassero molti e con dati molto diversi, si potrà passare a un record per evento (interfaccia `sealed`).

### 2.5 Eliminati `EsitoGiocata` ed `EsitoGiocataDto`

I comandi (`giocaCarta`, `rispondiInput`, `pescaCarta`, `passaTurno`, `completaDichiarazioneUno`) restituiscono un `boolean`: accettato o rifiutato. Ciò che prima trasportava `EsitoGiocata` ora ha un posto preciso:

- "serve un input" e "serve la dichiarazione UNO" → **fase**;
- "hai preso la penalità" → **evento**.

Cambia il significato di un valore di ritorno: `completaDichiarazioneUno` restituisce *comando accettato*, non più *penalità applicata*.

### 2.6 Controller e CLI

- [`FaseTurnoDto`](../src/main/java/unolegends/controller/FaseTurnoDto.java) è una copia dell'enum per la UI, che non può importare il model. La conversione `convertiFase` è uno `switch` esaustivo: una fase nuova nel model che non viene aggiunta anche qui fa fallire la compilazione.
- `StatoPartitaDto` ha `getFase()` al posto di `isDeveGiocareCartaPescata()` e `isPartitaTerminata()`.
- [`UnoLegendsCli`](../src/main/java/unolegends/ui/UnoLegendsCli.java), a ogni giro:

  1. stampa i messaggi;
  2. legge lo stato;
  3. fa un unico `switch` esaustivo sulla fase: `turnoNormale`, `turnoDopoPesca`, `gestisciRichiestaInput`, UNO, fine partita.

  `gestisciGiocata` (il ciclo della doppia chiamata) è stato eliminato.

---

## 3. Cosa NON è cambiato

- Regole di gioco, mazzo, strategie e `RegoleDiGioco` sono invariati.
- Gli effetti sono invariati. Solo `Effetto_Cambia_colore` crea la richiesta con `TipoRichiesta.SCELTA_COLORE` invece della frase.
- La convalida avviene sempre **prima** di modificare lo stato.
- Il giocatore iniziale è lo stesso di prima. Con una carta iniziale nera il colore lo sceglie `Giocatore1`, poi il turno avanza come prima.
- Dopo una pesca si può giocare **qualsiasi** carta della mano, come prevede la regola aggiornata del progetto.

---

## 4. File toccati

| File                                                                                                                  | Modifica                                                                                                                            |
| --------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `model/FaseTurno.java`                                                                                              | **nuovo**                                                                                                                     |
| `model/AzioneInAttesa.java`                                                                                         | **nuovo**                                                                                                                     |
| `model/TipoRichiesta.java`                                                                                          | **nuovo**                                                                                                                     |
| `model/TipoEvento.java`                                                                                             | **nuovo**                                                                                                                     |
| `model/EventoPartita.java`                                                                                          | **nuovo**                                                                                                                     |
| `controller/FaseTurnoDto.java`                                                                                      | **nuovo**                                                                                                                     |
| `model/Partita.java`                                                                                                | fase, azione in attesa, `rispondiInput`, eventi, `completaGiocata`/`chiudiTurno`; `aggiornaGiocatoreAttivo` diventa privato |
| `model/RichiestaInput.java`                                                                                         | `messaggio` → `tipo`                                                                                                           |
| `model/Effetto_Cambia_colore.java`                                                                                  | usa `TipoRichiesta.SCELTA_COLORE`                                                                                                 |
| `model/Giocatore.java`                                                                                              | `rimuoviCarta(Carta)`                                                                                                             |
| `controller/UnoLegendsGame.java`                                                                                    | `giocaCarta(int)`, `rispondiInput`, `prelevaMessaggi`, traduzioni                                                             |
| `controller/StatoPartitaDto.java`                                                                                   | `fase` al posto dei due flag                                                                                                      |
| `ui/UnoLegendsCli.java`                                                                                             | ciclo con `switch` sulla fase                                                                                                     |
| `model/EsitoGiocata.java`, `controller/EsitoGiocataDto.java`                                                      | **eliminati**                                                                                                                 |
| `README.md`, `docs/domain_class_diagram.puml`, `docs/roles_and_patterns.md`, `docs/ROADMAP.md`, `CLAUDE.md` | allineati                                                                                                                           |
| `docs/GiocaTurno_Step2_SequenceDiagram.puml`                                                                        | **nuovo** (quello dell'Iterazione 1 è stato lasciato com'è)                                                                 |

---

## 5. Come si estende (ricette per gli step successivi)

- **Una nuova fase** (es. `RISPOSTA_STACKING`):
  1. aggiungere il valore a `FaseTurno` e a `FaseTurnoDto`;
  2. abilitarla nei comandi che devono accettarla;
  3. aggiungere il `case` allo `switch` della CLI (il compilatore lo segnala).
- **Una nuova richiesta di input** (es. bersaglio della carta 7):
  1. l'effetto restituisce `RichiestaInput(SCELTA_GIOCATORE, nomi)` e valida la risposta;
  2. `Partita` non cambia: la giocata passa già da `mettiInAttesa` → `rispondiInput`.
- **Un'abilità attiva** a metà turno: un nuovo comando `usaAbilita()` chiama `mettiInAttesa(effetto, risposta -> { effetto.attivaEffetto(this, risposta); fase = faseDiPrima; })`.
- **Un nuovo evento** (es. `PASSIVA_ATTIVATA`, `GIOCATORE_ELIMINATO`):
  1. un valore in `TipoEvento`;
  2. una factory in `EventoPartita`;
  3. il `case` in `UnoLegendsGame.descriviEvento` (obbligato dal compilatore);
  4. `registraEvento(...)` nel punto in cui il fatto accade.
- **Una nuova `OriginePesca`** (+6, +10, `ACCUMULO`…): il compilatore chiede di aggiungerla a `descriviOrigine`.

---

## 6. Verifiche eseguite

- Compilazione completa con `javac -Xlint:all`: nessun errore né avviso.
- Uno script di prova (fuori dal repository) costruisce partite con carte scelte e verifica 35 condizioni, tutte superate:
  - **carta iniziale +4:** si entra in `ATTESA_INPUT`; pesca e giocata vengono rifiutate; un colore non valido viene rifiutato e la richiesta resta aperta; dopo la risposta compaiono 3 messaggi (colore, "G2 pesca 4 carte per un +4", "G2 salta il turno") e il turno torna a G1;
  - **jolly nel turno:** la carta resta in mano durante l'attesa; dopo la risposta va in cima con il colore scelto;
  - **finestra UNO:** in `ATTESA_UNO` ogni altro comando viene rifiutato; la penalità arriva come messaggio; una seconda dichiarazione viene rifiutata;
  - **pesca volontaria:** si entra in `POST_PESCA`; una seconda pesca viene rifiutata; si può giocare qualsiasi carta; `passaTurno` è accettato solo in `POST_PESCA`;
  - **vittoria:** si entra in `TERMINATA` e ogni comando viene rifiutato.
- Una partita CLI con input scritto in anticipo (pesca, passa, mossa non valida, uscita) funziona e mostra i messaggi degli eventi.
- Il progetto non ha ancora test automatici. I test JUnit sono previsti allo Step 7: lo script di prova è una buona traccia da cui partire.
