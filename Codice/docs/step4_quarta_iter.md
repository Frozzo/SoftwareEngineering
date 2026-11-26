# Step 4 (quarta iterazione) — Modalità No Mercy: cosa è stato fatto e perché

**Branch:** `ci-aiuta-claudio` · **Roadmap:** [ROADMAP.md](ROADMAP.md), Step 4 · **Diagramma:** [domain_class_diagram.puml](domain_class_diagram.puml) · **Step precedente:** [step3_terza_iter.md](step3_terza_iter.md)

Lo Step 4 aggiunge la seconda modalità di gioco, **No Mercy**, che dal menu di setup si sceglie accanto a Standard e si combina con l'opzione Endless. Comprende:
- un mazzo da 168 carte con sei tipi di carta nuovi;
- cinque effetti nuovi;
- la pesca "finché trovi una carta giocabile";
- l'eliminazione a 25 carte (Mercy rule).

Lo **stacking** (rispondere a un +2 con un +2 o superiore) **non** fa parte di questo step: arriva allo Step 5. Per ora ogni carta di pesca si applica subito.

---

## 1. Le scelte prese con il team

Prima di implementare abbiamo chiarito tutti i punti dubbi delle carte e delle regole. Per ognuno c'era una proposta, che il team ha confermato o corretto.

### 1.1 Composizione del mazzo (scelta 4.A)

Le quantità ufficiali le ha fornite il team. Il totale fa **168 carte**:

| Gruppo | Carte | Totale |
|---|---|---|
| Numeriche | 2 copie di ogni numero da 0 a 9, per ognuno dei 4 colori | 80 |
| Azioni colorate | 16 per colore: salta turno, cambia giro, scarta tutto, salta tutti, +2, +4 | 64 |
| Jolly | 8 +4 cambio giro, 8 roulette colore, 4 +6, 4 +10 | 24 |

**Ripartizione delle 16 azioni per colore.** Sei tipi non dividono 16 in parti uguali. La proposta approvata è:

| Blocca turno | Cambia giro | +2 | +4 colorato | Scarta tutto | Salta tutti | Totale |
|---|---|---|---|---|---|---|
| 3 | 3 | 3 | 2 | 3 | 2 | 16 per colore (64 in tutto) |

**Dove scriverla:** direttamente nella `NoMercyMazzoFactory` (decisione del team). La composizione non cambierà mai, quindi non serve renderla configurabile. L'importante è che i numeri stiano **in un solo punto**: sono costanti con un nome, e `creaMazzo()` controlla che il totale sia 168.

### 1.2 Dove mettere lo scambio e la rotazione delle mani (scelta 4.B)

| Opzione | Esito |
|---|---|
| Metodi `scambiaMani` / `ruotaMani` in `Partita`, chiamati dagli effetti | Era la proposta iniziale della ROADMAP |
| ✅ **Tutto dentro gli effetti** | **Scelta del team** |

L'effetto riceve già `Partita` (Step 1), quindi ha tutto quello che gli serve. Il giocatore di turno e gli avversari glieli dà `Partita`; le mani le sposta l'effetto, con i metodi package-private di `Giocatore` (`svuotaMano`, `riceviCarte`).

La ROADMAP temeva un problema: "l'effetto deve ricordarsi i controlli UNO e vittoria". Non si presenta, perché `Partita` fa quei controlli **dopo ogni giocata, per qualunque effetto**:
- eliminazioni;
- finestra UNO;
- vittoria.

Gli effetti nuovi non devono ricordarsi niente, e `Partita` non conosce gli effetti nuovi.

### 1.3 Le carte, punto per punto

| # | Domanda | Decisione |
|---|---|---|
| 1 | Ripartizione delle 16 azioni per colore | 3 blocca, 3 cambia giro, 3 +2, 2 +4, 3 scarta tutto, 2 salta tutti |
| 2 | Lo 0 e il 7 hanno l'effetto in tutte le copie? | **Sì.** Lo 0 fa passare a tutti la mano al giocatore successivo **nel senso di gioco** (regola ufficiale); il 7 fa scegliere con chi scambiare la mano |
| 3 | Il +4 colorato | Il successivo pesca 4 carte e salta il turno; non si sceglie il colore |
| 4 | Il +4 cambio giro | Chi gioca sceglie il colore, poi si inverte il senso, poi il successivo **nel nuovo senso** pesca 4 e salta. Con 2 giocatori vale come un blocco: l'altro pesca 4 e il turno torna a chi ha giocato |
| 5 | Jolly +6 e +10 | Chi gioca sceglie il colore; il successivo pesca 6 o 10 e salta |
| 6 | Scarta tutto | Chi la gioca scarta tutte le carte della mano di quel colore, **senza** attivarne gli effetti. Le carte vanno **sotto** la Scarta tutto, che resta in cima. Se resta senza carte vince; **se resta con una carta deve dichiarare UNO** (correzione del team: la proposta era "no") |
| 7 | Salta tutti | Tutti gli altri saltano e chi l'ha giocata ha un **turno completo nuovo**, in cui può giocare o pescare. Se era l'ultima carta ha vinto |
| 8 | Roulette | Chi gioca sceglie il colore; il successivo pesca una carta alla volta finché esce quel colore, tiene tutto e salta. **Un jolly nero non conta** come carta del colore scelto. Se mazzo e scarti finiscono la pesca si ferma. Salesman non ha lo sconto |
| 9 | 7 o 0 giocati come **ultima carta** | Chi li gioca **ha vinto** e lo scambio o la rotazione **non avviene** |
| — | UNO dopo uno scambio o una rotazione di mani | **Nessuno** dichiara UNO, né chi ha giocato la carta né chi riceve la mano (indicazione del team) |

### 1.4 Le regole, punto per punto

| # | Domanda | Decisione |
|---|---|---|
| 10 | Stacking | Non c'è ancora: arriva allo Step 5. Per ora ogni carta di pesca si applica subito |
| 11 | Pesca volontaria No Mercy | Si pesca finché si trova una carta giocabile, poi si resta in `POST_PESCA` come nella partita normale: si può giocare una carta qualsiasi o passare, senza obbligo di giocare quella pescata |
| 12 | Mercy rule (25 carte) | Chi arriva a 25 carte è eliminato e le sue carte tornano subito nel mazzo. Se l'eliminato è il **giocatore di turno**, il turno passa al successivo. Se è chi **doveva saltare il turno** (per esempio la vittima di un +10), il salto si annulla e non passa a un innocente. Durante la roulette la pesca si ferma appena la vittima arriva a 25 |
| 13 | Cambio giro con 2 giocatori in gioco | Si adotta la **regola ufficiale**: vale come un blocco, **in entrambe le modalità** (anche nella partita normale e in Endless quando restano in due). Prima era una decisione aperta |

---

## 2. Cosa è stato fatto

### 2.1 Nuovi valori di carta, cause di pesca ed eventi

| Enum | Valori nuovi |
|---|---|
| [`ValoreCarta`](../src/main/java/unolegends/model/ValoreCarta.java) | `SCARTA_TUTTO`, `SALTA_TUTTI`, `CAMBIO_GIRO_PIU_QUATTRO` ("+4 cambio giro"), `PIU_SEI`, `PIU_DIECI`, `ROULETTE` ("roulette colore"). Il +4 colorato usa il `PIU_QUATTRO` esistente |
| [`OriginePesca`](../src/main/java/unolegends/model/OriginePesca.java) | `PIU_SEI`, `PIU_DIECI`, `ROULETTE` |
| [`TipoEvento`](../src/main/java/unolegends/model/TipoEvento.java) | `MANI_SCAMBIATE`, `MANI_PASSATE`, `CARTE_SCARTATE`, `TURNO_EXTRA` |

[`EventoPartita`](../src/main/java/unolegends/model/EventoPartita.java) ha un campo in più, `altroGiocatore`, per lo scambio di mani ("Anna scambia la mano con Luca"). Gli eventi si creano sempre con le factory statiche, quindi nessun chiamante è cambiato.

**Il compilatore ha fatto da guardia:** aggiunti i valori, la compilazione si è fermata sui tre `switch` esaustivi del controller (nome della modalità, frasi degli eventi, cause di pesca) finché non sono stati scritti i testi relativi. È proprio il motivo per cui quegli `switch` sono esaustivi.

### 2.2 Il mazzo: `NoMercyMazzoFactory`

[`NoMercyMazzoFactory`](../src/main/java/unolegends/model/NoMercyMazzoFactory.java) è una nuova Concrete Factory dell'Abstract Factory `MazzoFactory`, e un Singleton come `StandardMazzoFactory`.
- Le quantità sono costanti con nome: `BLOCCA_TURNO_PER_COLORE = 3`, `JOLLY_ROULETTE = 8`, ecc.
- `creaMazzo()` lancia un errore se il totale non è 168.
- Lo 0 e il 7 sono normali `Carta` con un effetto. È il vantaggio del refactoring della carta unica fatto nello Step 3 (§8 di [step3_terza_iter.md](step3_terza_iter.md)): non è servita nessuna classe di carta nuova né un costruttore speciale.
- I jolly sono `EffettoComposito`:

| Jolly | Composizione |
|---|---|
| +4 cambio giro | `[Effetto_Cambia_colore, Effetto_cambio_giro, Effetto_pescata_carte(4, PIU_QUATTRO)]`: l'ordine conta, perché si pesca dopo il cambio di senso |
| +6 / +10 | `[Effetto_Cambia_colore, Effetto_pescata_carte(6/10, PIU_SEI/PIU_DIECI)]` |

[`ModalitaDiGioco`](../src/main/java/unolegends/model/ModalitaDiGioco.java) ha il nuovo valore `NO_MERCY(NoMercyMazzoFactory.getInstance(), ...)`. Il menu lo mostra da solo, perché elenca le modalità chiedendole al controller.

### 2.3 Gli effetti nuovi

Tutti nel package `model`, tutti `Ieffetto`. Hanno nomi coerenti (`EffettoXxx`, come `EffettoComposito`) e quelli vecchi non sono stati toccati.

| Effetto | Carta | Cosa fa |
|---|---|---|
| [`EffettoScambiaMano`](../src/main/java/unolegends/model/EffettoScambiaMano.java) | 7 | Chiede `SCELTA_GIOCATORE` fra gli avversari in gioco, poi scambia le due mani. Se la 7 è l'unica carta in mano non chiede nulla e non scambia (decisione 9) |
| [`EffettoPassaMani`](../src/main/java/unolegends/model/EffettoPassaMani.java) | 0 | Ogni giocatore in gioco passa la mano al successivo nel senso di gioco. L'ordine glielo dà `Partita.getGiocatoriInGiocoNelSensoDiGioco()`. Chi è terminato o eliminato non partecipa |
| [`EffettoScartaColore`](../src/main/java/unolegends/model/EffettoScartaColore.java) | Scarta tutto | Toglie dalla mano tutte le carte del colore attivo e le mette negli scarti sotto la cima (`Partita.scartaSottoLaCima` → `PilaDegliScarti.inserisciSottoLaCima`) |
| [`EffettoTurnoExtra`](../src/main/java/unolegends/model/EffettoTurnoExtra.java) | Salta tutti | Chiede a `Partita` un turno extra (`concediTurnoExtra`) |
| [`EffettoRoulette`](../src/main/java/unolegends/model/EffettoRoulette.java) | Roulette | Riusa `Effetto_Cambia_colore` per scegliere e applicare il colore, poi fa pescare la vittima con `applicaPescaFinoA` e le fa saltare il turno |

Tre dettagli di design:

- **Scarta tutto usa il colore attivo** invece di ricevere il colore nel costruttore. `Partita` imposta il colore attivo al colore della carta giocata *prima* di attivarne l'effetto, quindi è già il colore giusto e basta un tipo di effetto per tutti i colori.
- **La roulette riusa `Effetto_Cambia_colore` per composizione.** Richiesta, validazione e applicazione del colore sono le stesse del cambia colore, quindi non c'è codice duplicato.
- **La 7 sa quando è l'ultima carta.** Se è l'unica carta in mano, `getRichiestaInput` restituisce `null`: non ha senso chiedere un bersaglio a chi ha già vinto.

### 2.4 Le regole: `RegoleDiGioco.noMercy(opzioni)`

[`RegoleDiGioco`](../src/main/java/unolegends/model/RegoleDiGioco.java) ha il nuovo metodo statico `noMercy(Set<OpzionePartita>)`, accanto a `standard(...)`, come deciso allo Step 3: niente Builder, un metodo per modalità. Rispetto alla partita normale cambiano due strategie:

| Strategia | Partita normale | No Mercy |
|---|---|---|
| Pesca volontaria (`IStrategiaPesca`) | `StrategiaPescaSingola` | [`StrategiaPescaFinoAGiocabile`](../src/main/java/unolegends/domain/rules/impl/StrategiaPescaFinoAGiocabile.java): pesca finché la carta è giocabile, o finché il mazzo è esaurito, o finché si arriva a 25 carte |
| Sconfitta (`IStrategiaCondizioneSconfitta`) | `StrategiaSconfittaStandard` (mai) | [`StrategiaSconfittaNoMercy(25)`](../src/main/java/unolegends/domain/rules/impl/StrategiaSconfittaNoMercy.java): la soglia è un parametro |

L'opzione Endless vale per entrambe le modalità: la scelta della strategia di fine partita è un metodo privato comune ai due metodi statici. `PartitaFactory.creaRegole` ha il caso `NO_MERCY -> RegoleDiGioco.noMercy(...)`.

Le strategie sono nuove implementazioni di interfacce che esistevano dallo Step 1: il punto di variazione era già pronto, e `Partita` non sa quale pesca o quale sconfitta è attiva.

### 2.5 Cosa è cambiato in `Partita`

[`Partita`](../src/main/java/unolegends/model/Partita.java) non contiene nessun riferimento ai nuovi effetti o alla No Mercy. Le modifiche sono **regole generali**, valide per qualunque effetto.

**a) Mercy rule: `verificaEliminazioni()`.** Elimina chi ha raggiunto la soglia di eliminazione. Si chiama **solo in punti sicuri**, dopo che un effetto o una pesca sono stati applicati per intero:
- dopo l'effetto in `completaGiocata`;
- dopo `pescaCarta`;
- dopo la penalità UNO;
- dopo la carta iniziale.

*Perché non subito, dentro `applicaPescaForzata`:* l'effetto del +10 prima fa pescare la vittima e poi chiede il salto del turno. Se la vittima venisse eliminata a metà, il salto chiesto subito dopo finirebbe al giocatore seguente, che è innocente. Eliminando alla fine:
- se l'eliminato è il "prossimo giocatore" il salto si annulla (decisione 12);
- se l'eliminato è il giocatore di turno, il turno passa;
- se l'eliminazione lascia un solo giocatore, la partita finisce.

`chiudiTurno()` ora non fa nulla a partita terminata, così nessun chiamante può "riaprire" una partita finita.

**b) Finestra UNO: `deveDichiarareUno(giocatore, manoPrima)`.** Prima la regola era "avevi 2 carte e ora ne hai 1". Con le carte No Mercy non basta più:
- con Scarta tutto si può passare da 5 carte a 1, e il team ha deciso che lì UNO va dichiarato;
- con la 7 si può ricevere una mano da 1 carta, e lì UNO non va dichiarato.

La regola nuova è: **si dichiara UNO se resta in mano una sola carta che il giocatore aveva già prima della giocata.** Il confronto è per identità, come in `Giocatore.rimuoviCarta`. Copre tutti i casi senza nessun `if` sul tipo di carta:

| Caso | Carta rimasta | UNO? |
|---|---|---|
| Giocata normale con 2 carte | era già sua | sì |
| Scarta tutto che lascia 1 carta | era già sua | sì |
| 7 o 0 che lasciano 1 carta | viene da un altro giocatore | no |

**c) Turno extra: campo `turniExtra`.** Salta tutti lo incrementa. `aggiornaGiocatoreAttivo()` lo consuma lasciando il turno allo stesso giocatore, e registra l'evento `TURNO_EXTRA`. È un contatore, non un booleano, perché funziona con qualunque numero di giocatori e servirà all'abilità Double card dello Step 6. Se chi aveva il turno extra è uscito (ha finito le carte o è eliminato), il contatore si azzera e il turno avanza normalmente.

**d) Cambio giro con 2 giocatori.** `cambioGiro()`, se i giocatori in gioco sono 2, chiede anche il salto del turno (decisione 13). La regola sta in `Partita` e non nell'effetto perché è `Partita` a sapere quanti giocatori sono in gioco (Information Expert). Così vale automaticamente anche per il +4 cambio giro.

**e) Pesca "a ripetizione": `applicaPescaFinoA(bersaglio, condizione, origine)`.** Per la roulette il numero di carte da pescare non è noto in anticipo. È un secondo punto unico di pesca forzata, accanto a `applicaPescaForzata`, così gli effetti continuano a non scrivere cicli di pesca (regola del progetto). Si ferma quando la condizione è vera, quando mazzo e scarti sono esauriti o quando il bersaglio arriva alla soglia di eliminazione. Non passa da `calcolaCartePescaForzata`, quindi Salesman non avrà lo sconto sulla roulette (decisione presa).

**f) Metodi di servizio per effetti e strategie:**
- `getGiocatoreAttivo()` ora è package-private;
- `getGiocatoriInGiocoNelSensoDiGioco()`, `concediTurnoExtra()`, `scartaSottoLaCima(...)` sono package-private;
- `isCartaGiocabile(...)` e `isDaEliminare(...)` sono pubblici, perché li usa la strategia di pesca, che sta in un altro package.

### 2.6 La carta iniziale

In No Mercy la prima carta scoperta potrebbe essere una 7, uno 0, una Scarta tutto o una Salta tutti. Sono effetti che agiscono su "chi ha giocato la carta", e a inizio partita nessuno l'ha giocata.

- Nuovo metodo `default` di `Ieffetto`: `isAmmessoComeCartaIniziale()`, che vale `true`. I quattro effetti sopra restituiscono `false`, e `EffettoComposito` è ammesso solo se lo sono tutti i suoi effetti.
- `PartitaFactory.scopriPrimaCarta` rimette nel mazzo una carta non ammessa, rimescola e ne scopre un'altra.

È un metodo polimorfico, non un controllo sul tipo della carta (Protected Variations): `PartitaFactory` non sa quali effetti esistono. Nella partita normale tutte le carte sono ammesse, quindi lì non cambia nulla.

### 2.7 Controller e CLI

- [`UnoLegendsGame`](../src/main/java/unolegends/controller/UnoLegendsGame.java):
  - nome della modalità "No Mercy";
  - frasi dei nuovi eventi, per esempio "Sara scambia la mano con Luca.", "Anna scarta 3 carte di colore verde.", "Tutti saltano il turno: Marco gioca ancora.";
  - cause di pesca "per un +6", "per un +10", "con la roulette".
- La CLI **non è cambiata**: la modalità nuova compare nel menu perché la elenca il controller, la scelta del bersaglio della 7 usa la stessa richiesta di input di tutte le altre (input unificato dello Step 2), e gli eventi nuovi sono frasi come le altre. È la conferma che le scelte degli step precedenti reggono.

---

## 3. Perché queste scelte (GRASP/GoF)

| Principio | Dove si vede in questo step |
|---|---|
| **Protected Variations** | Pesca e sconfitta No Mercy sono strategie nuove dietro interfacce dello Step 1. `isAmmessoComeCartaIniziale()` evita che la factory conosca gli effetti |
| **Strategy** | `Ieffetto` per le carte, `IStrategiaPesca` e `IStrategiaCondizioneSconfitta` per le regole |
| **Composite** | I jolly +4 cambio giro, +6 e +10 sono composizioni di effetti già esistenti |
| **Composizione / riuso** | La roulette contiene un `Effetto_Cambia_colore` invece di duplicarne il codice |
| **Abstract Factory + Singleton** | `NoMercyMazzoFactory` è la seconda famiglia di carte della stessa `MazzoFactory` |
| **Information Expert** | `Partita` decide quando eliminare, quando aprire UNO e cosa vuol dire "cambio giro con 2 giocatori", perché è lei a conoscere turno e giocatori |
| **Low Coupling** | `Partita` non importa nessuno dei nuovi effetti; la CLI non è stata toccata |
| **Nessun `instanceof`** | Nemmeno questa volta: le differenze passano tutte da metodi polimorfici o da regole generali |

---

## 4. Verifiche fatte

Le prove sono un programma esterno al progetto, non incluso, perché il progetto non ha ancora un framework di test (Step 7).

- **Compilazione** con il comando del README: nessun errore.
- **Composizione del mazzo:** 168 carte, con 12 blocca, 12 cambia giro, 12 +2, 8 +4 colorati, 12 scarta tutto, 8 salta tutti, 8 +4 cambio giro, 8 roulette, 4 +6 e 4 +10. Tutti gli 0 e i 7 hanno il loro effetto, gli altri numeri nessuno.
- **Scenari costruiti a mano**, uno per ogni decisione:
  - Scarta tutto scarta solo il colore giusto e la cima resta Scarta tutto; con una carta rimasta apre UNO; se svuota la mano è vittoria;
  - la 7 propone solo gli avversari, rifiuta sé stessi, scambia le mani e non apre UNO anche se resta una carta; come ultima carta fa vincere senza scambio;
  - lo 0 ruota le mani nel senso orario;
  - Salta tutti fa rigiocare lo stesso giocatore, poi il turno passa;
  - cambio giro con 2 giocatori vale come blocco (anche nella partita normale), con 3 inverte il senso;
  - +4 cambio giro con 3 giocatori: senso invertito, pesca il giocatore "precedente" e tocca al seguente; con 2 giocatori il turno torna a chi ha giocato;
  - roulette: la vittima pesca fino al colore scelto, il jolly nero non la ferma, il colore attivo diventa quello scelto e la vittima salta;
  - Mercy rule: un +10 su chi ha 20 carte lo elimina, il salto si annulla (tocca al giocatore dopo, nessun innocente salta) e le carte tornano nel mazzo; con 2 giocatori vince chi resta;
  - pesca No Mercy: si ferma alla prima carta giocabile; se si arriva a 25 carte pescando si è eliminati e il turno passa;
  - carta iniziale: in 2.000 partite non è mai una 7, uno 0, una Scarta tutto o una Salta tutti.
- **Simulazione:** 1.350 partite No Mercy complete, da 2 a 6 giocatori, con e senza Endless, giocate da un "bot", con 4.155 eliminazioni in totale. A ogni comando ha controllato che:
  - le carte totali (mazzo, scarti e mani) restino **sempre 168**;
  - il giocatore di turno sia in gioco;
  - nessun giocatore in gioco abbia 25 o più carte;
  - chi è uscito non abbia carte.

  Ogni partita termina con una classifica completa, e tutti gli eventi nuovi sono comparsi almeno una volta.
- **Regressione:** le prove dello Step 3 sulla partita normale (Standard ed Endless) passano ancora.
- **CLI:** il menu propone Standard e No Mercy e la partita No Mercy parte con le carte nuove. Le frasi dei nuovi eventi sono state lette in una partita vera dal controller.

---

## 5. Cosa resta aperto

| # | Questione | Note |
|---|---|---|
| 1 | **Stacking** | Step 5: `OpzionePartita.STACKING`, obbligatoria in `NO_MERCY`; nella partita normale +2 ← +2/+4, +4 ← +4 |
| 2 | **Bersagli "bersagliabili"** | La 7 propone gli avversari in gioco. Allo Step 6 diventeranno gli avversari bersagliabili, per escludere Ghost |
| 3 | **Carta iniziale nera** | Comportamento ereditato dagli step precedenti, non introdotto ora: se la prima carta è un jolly, il primo giocatore sceglie il colore e il turno passa al successivo. Si può rivedere allo Step 7 |
| 4 | **Test automatici** | Le prove di questo step diventerebbero test veri con Maven + JUnit (Step 7) |

---

## 6. File toccati

| File | Modifica |
|---|---|
| `model/NoMercyMazzoFactory.java` | **nuovo**: mazzo da 168 carte |
| `model/EffettoScambiaMano.java`, `model/EffettoPassaMani.java`, `model/EffettoScartaColore.java`, `model/EffettoTurnoExtra.java`, `model/EffettoRoulette.java` | **nuovi**: effetti della 7, dello 0, di Scarta tutto, di Salta tutti e della roulette |
| `domain/rules/impl/StrategiaPescaFinoAGiocabile.java`, `domain/rules/impl/StrategiaSconfittaNoMercy.java` | **nuovi**: pesca e Mercy rule della No Mercy |
| `model/ValoreCarta.java`, `model/OriginePesca.java`, `model/TipoEvento.java` | nuovi valori |
| `model/EventoPartita.java` | campo `altroGiocatore` e quattro factory nuove |
| `model/ModalitaDiGioco.java` | `NO_MERCY` |
| `model/RegoleDiGioco.java` | `noMercy(opzioni)`, soglia 25, fine partita comune alle due modalità |
| `model/PartitaFactory.java` | caso `NO_MERCY`, `scopriPrimaCarta` |
| `model/Partita.java` | `verificaEliminazioni`, `deveDichiarareUno`, `turniExtra`, cambio giro con 2 giocatori, `applicaPescaFinoA`, metodi di servizio |
| `model/Ieffetto.java`, `model/EffettoComposito.java` | `isAmmessoComeCartaIniziale()` |
| `model/Giocatore.java` | `riceviCarte(...)` |
| `model/PilaDegliScarti.java` | `inserisciSottoLaCima(...)` |
| `controller/UnoLegendsGame.java` | testi della modalità, degli eventi e delle cause di pesca |
| `README.md` (radice), `Codice/README.md`, `docs/ROADMAP.md`, `docs/domain_class_diagram.puml`, `CLAUDE.md` | documentazione allineata |
