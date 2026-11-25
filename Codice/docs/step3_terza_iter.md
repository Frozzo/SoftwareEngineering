# Step 3 (terza iterazione) — Cosa è stato fatto e perché

**Branch:** `ci-aiuta-claudio` · **Roadmap:** [ROADMAP.md](ROADMAP.md), Step 3 · **Diagramma:** [domain_class_diagram.puml](domain_class_diagram.puml)

Lo Step 3 porta la partita da **2 a N giocatori (2–6)** e aggiunge un **menu di setup**.
Dal menu si sceglie la modalità e la si personalizza con delle **opzioni**; la prima è **Endless**.
Introduce anche lo **stato del giocatore** (in gioco, terminato, eliminato), su cui si appoggeranno
la Mercy rule (Step 4), lo stacking (Step 5) e i Legend (Step 6).

---

## 1. Il problema di partenza

| Aspetto | Com'era | Perché era un problema |
|---|---|---|
| Numero di giocatori | Fisso a 2, scritto in `UnoLegendsGame.avviaPartitaStandard()` | Il gioco finale deve arrivare a 6 giocatori. |
| Scorrimento dei turni | `avanzaGiocatore()` e `getProssimoGiocatore()` calcolavano ciascuno `(i ± 1) % n` | Dava per scontato che nessuno uscisse dalla partita. Con Endless o con un eliminato il turno sarebbe arrivato a chi non gioca più. |
| Fine partita | Campo `vincitore`: appena qualcuno finiva le carte, `TERMINATA` | Non permetteva di continuare (Endless) né di fare una classifica. |
| Modalità | Enum `TipoMazzo { STANDARD }`, non usato da nessuno | Nessun punto in cui scegliere modalità e regole. |
| Nomi | `Giocatore1`, `Giocatore2` fissi | Con N giocatori in hotseat servono nomi veri. |
| Setup | Menu con una sola voce, "avvia partita demo" | Nessuna scelta di modalità, opzioni o giocatori. |
| Vista della partita | La CLI mostrava solo il giocatore di turno | Con N giocatori bisogna vedere quante carte hanno gli altri e chi è ancora in gioco. |

---

## 2. Le scelte fatte (discussione con il team)

### 2.1 Il menu: "modalità + opzioni"

L'idea del team: prima si sceglie la modalità, poi la si personalizza con delle regole opzionali, una domanda sì/no ciascuna.

| Modalità | Opzioni chieste | Opzioni sempre attive |
|---|---|---|
| Standard | Stacking, Endless, Legend | — |
| No Mercy | Endless, Legend | Stacking |

Le modalità quindi diventano **combinabili** con le opzioni: Standard + Stacking + Endless, No Mercy + Endless, e così via.

**Quanto si allarga il progetto.** Le modalità sono due e **resteranno due per sempre**. Ciò che crescerà nel tempo sono le **regole/opzioni**. Questa informazione ha guidato tutte le scelte successive: la flessibilità va messa sulle regole, non sulle modalità.

### 2.2 Scelta 3.A — Come rappresentare modalità e opzioni

| Opzione | Esito |
|---|---|
| 3.A1 Booleani sparsi (`noMercy = true` letto ovunque) | ❌ sparge `if` in tutto il codice |
| 3.A2 Enum `ModalitaDiGioco` che **crea** mazzo e regole | Era la proposta iniziale. Con le opzioni combinabili servirebbe un valore per ogni combinazione |
| 3.A3 Abstract Factory "profilo di modalità" (una classe per modalità) | Troppe classi per due modalità che non cresceranno |
| Variante "base + opzioni" con logica negli enum (`applicaA(...)` su ogni opzione) | Scartata: un enum con metodi diversi per ogni valore è poco leggibile |
| ✅ **Enum di soli dati + regole scelte da `RegoleDiGioco`** | **Scelta approvata** |

Come funziona la scelta approvata:

- **`ModalitaDiGioco`** è una tabella di dati con una riga per modalità: quale `MazzoFactory` usare e quali opzioni sono obbligatorie. Contiene solo campi e getter.
- **`OpzionePartita`** è un semplice elenco di opzioni, senza logica.
- **Le regole le sceglie `RegoleDiGioco`**, con un metodo statico per modalità che riceve le opzioni attive: `RegoleDiGioco.standard(opzioni)` e, dallo Step 4, `RegoleDiGioco.noMercy(opzioni)`.
- `PartitaFactory.creaRegole(config)` fa solo uno `switch` sulla modalità per chiamare il metodo giusto.

**"Ma non dovevamo evitare gli `if`?"** La regola del progetto è: niente `if` sul tipo concreto **dentro `Partita`**, dove si spargerebbero in ogni metodo. Una factory, invece, esiste proprio per scegliere le classi concrete (Creator GRASP). Qui le scelte stanno in pochissimi punti (`RegoleDiGioco.standard`, `PartitaFactory.creaRegole`), si eseguono una sola volta alla creazione della partita, e da quel momento `Partita` usa solo le interfacce delle strategie.

### 2.3 Scelta 3.B — Builder sì o no

| Opzione | Esito |
|---|---|
| Builder su `ConfigurazionePartita` (proposta iniziale della ROADMAP) | Scartato: ha pochi campi che non crescono, perché le opzioni nuove finiscono in un `Set` |
| Builder su `RegoleDiGioco` (`RegoleDiGioco.builder().pescaVolontaria(...).build()`) | Valutato, perché `RegoleDiGioco` è l'oggetto che cresce a ogni regola nuova. **Rimandato**: vedi sotto |
| Due Builder | ❌ troppo per il progetto attuale |
| ✅ **Nessun Builder: `record` per la configurazione + `RegoleDiGioco.standard(opzioni)`** | **Scelta approvata** |

> **Precisazione sulle regole, da rivedere più avanti.**
> Il team ha trovato `builder()...build()` poco leggibile rispetto a `RegoleDiGioco.standard(opzioni)`. Con le strategie attuali (6) il costruttore lungo lo chiamano solo i metodi statici di `RegoleDiGioco`, quindi il problema del "costruttore telescopico" per ora non si pone. **Quando le strategie aumenteranno** (accumulo dello stacking allo Step 5, passive dei Legend allo Step 6) andrà rivalutato se introdurre il Builder su `RegoleDiGioco`. Si può farlo senza toccare `Partita`, perché cambierebbe solo come si costruisce `RegoleDiGioco`. È annotato tra le decisioni aperte della ROADMAP (n. 9).

### 2.4 Endless

Endless è stato reinserito tra gli obiettivi (prima era escluso). Definizione approvata:

- chi finisce le carte **esce dai turni** e prende il prossimo posto in classifica: il primo che finisce è primo;
- si continua finché resta **un solo giocatore** in gioco, che prende l'ultimo posto fra i non eliminati;
- alla fine la CLI stampa la **classifica**;
- richiede **almeno 3 giocatori**: con 2, quando il primo finisce la partita è già conclusa;
- è un'opzione, quindi si combina con entrambe le modalità.

### 2.5 Altre decisioni prese

- **Stacking nella partita normale** (si implementa allo Step 5): un +2 si copre con un +2 o un +4, un +4 solo con un +4. È la stessa regola "valore uguale o superiore" della No Mercy.
- **Il menu mostra solo ciò che funziona già.** Oggi ci sono la modalità Standard e l'opzione Endless. No Mercy arriva allo Step 4, Stacking allo Step 5, Legend allo Step 6. Per farli comparire nel menu basterà aggiungere il valore all'enum: la CLI elenca ciò che le passa il controller.
- **Ordine delle domande nel menu:** modalità → opzioni → numero e nomi dei giocatori.

---

## 3. Cosa è stato fatto

### 3.1 Stato del giocatore: `StatoGiocatore`

Nuovo enum [`StatoGiocatore`](../src/main/java/unolegends/model/StatoGiocatore.java), con un campo `stato` in `Giocatore`:

| Stato | Significato |
|---|---|
| `IN_GIOCO` | partecipa ai turni |
| `TERMINATO` | ha finito le carte e ha un posto in classifica (nella prima versione si chiamava `ARRIVATO`, vedi §7) |
| `ELIMINATO` | eliminato (Mercy rule, Step 4): va in fondo alla classifica |

- Il valore si chiama `IN_GIOCO` e non `ATTIVO`, come proponeva la ROADMAP, perché nel progetto "giocatore attivo" vuol già dire "giocatore di turno". Usare la stessa parola per due concetti avrebbe creato confusione.
- Lo stato lo cambia solo `Partita`: `segnaComeTerminato()`, `segnaComeEliminato()` e `svuotaMano()` sono package-private. L'uscita di un giocatore ha conseguenze sul turno e sulla classifica che conosce solo il Coordinator.
- Chi esce **resta nella lista** `giocatori`. Così gli indici non cambiano e la UI può continuare a mostrarlo, con il suo stato.

### 3.2 Turni con N giocatori: `trovaProssimoIndiceInGioco`

Un unico helper privato in [`Partita`](../src/main/java/unolegends/model/Partita.java) calcola "chi viene dopo": avanza di ±1 (con `Math.floorMod`) nel senso di gioco e **salta chi non è `IN_GIOCO`**.

- Lo usano sia `avanzaGiocatore()` sia `getProssimoGiocatore()`, che prima ripetevano la stessa aritmetica.
- **Perché un solo punto:** se la regola su chi viene dopo cambia (eliminati, turni extra di "Salta tutti"), si modifica un metodo solo.
- `getAvversariInGioco()` restituisce gli avversari ancora in gioco. Servirà agli effetti con scelta del bersaglio (carta 7, abilità).
- `getGiocatori()`, `getNumeroGiocatoriInGioco()`, `getGiocatoriTerminati()` e `isSensoOrario()` servono al controller e alle strategie.

### 3.3 Fine partita e classifica: `IStrategiaFinePartita`

Nuova strategia in `domain.rules` con due implementazioni:

| Strategia | La partita finisce quando… |
|---|---|
| [`StrategiaFinePartitaStandard`](../src/main/java/unolegends/domain/rules/impl/StrategiaFinePartitaStandard.java) | almeno uno ha finito le carte, oppure resta un solo giocatore in gioco |
| [`StrategiaFinePartitaEndless`](../src/main/java/unolegends/domain/rules/impl/StrategiaFinePartitaEndless.java) | resta un solo giocatore in gioco |

Il flusso in `Partita`:

```text
completaGiocata(carta)
  └─ regole.isVincitore(giocatore)?            // ha finito le carte
       └─ registraTerminato(giocatore)         // stato TERMINATO, evento GIOCATORE_TERMINATO(posizione)
            └─ verificaFinePartita()
                 └─ regole.isPartitaTerminata(this)?  →  concludiPartita()  // classifica + TERMINATA
       └─ se non è finita (Endless) → chiudiTurno()   // il turno passa al prossimo IN_GIOCO
```

**La classifica** (`concludiPartita`):
1. chi ha finito le carte, in ordine di arrivo;
2. chi era ancora in gioco, ordinato per numero di carte (meno carte = posizione migliore). In una partita normale questo dà comunque un ordine sensato ai non vincitori;
3. gli eliminati, dall'ultimo eliminato al primo.

Il vincitore è il primo della classifica. Il campo `vincitore` è stato eliminato e sostituito da `classifica`.

**Perché una strategia e non un `if (endless)` in `Partita`:** è lo stesso punto di variazione protetto delle altre regole. `Partita` chiede soltanto "è finita?" e non sa quale opzione è attiva.

### 3.4 Eliminazione, preparata per lo Step 4

`Partita.eliminaGiocatore(Giocatore)` (package-private) fa quattro cose:
- mette lo stato `ELIMINATO`;
- rimette le carte del giocatore nel mazzo con `Mazzo.aggiungiCarteERimescola` (decisione del progetto: subito, non al prossimo rimescolamento);
- registra l'evento `GIOCATORE_ELIMINATO`;
- controlla se la partita è finita.

Con le regole attuali **nessuno lo chiama**, perché `StrategiaSconfittaStandard` non elimina mai. Allo Step 4 si aggiungerà il controllo della Mercy rule dopo ogni pesca. In quel momento vanno gestiti due casi limite, già annotati nel Javadoc:
- l'eliminato è il **giocatore di turno**;
- l'eliminato è il giocatore che **doveva saltare il turno** dopo un +2/+4. In questo caso il salto non deve passare a un innocente.

### 3.5 Configurazione: `ModalitaDiGioco`, `OpzionePartita`, `ConfigurazionePartita`

| Classe | Cosa contiene |
|---|---|
| [`ModalitaDiGioco`](../src/main/java/unolegends/model/ModalitaDiGioco.java) | `NORMALE(StandardMazzoFactory, nessuna opzione obbligatoria)`; `getMazzoFactory()`, `getOpzioniObbligatorie()`, `isConfigurabile(opzione)` |
| [`OpzionePartita`](../src/main/java/unolegends/model/OpzionePartita.java) | `ENDLESS` |
| [`ConfigurazionePartita`](../src/main/java/unolegends/model/ConfigurazionePartita.java) | `record (modalita, nomiGiocatori, opzioni)` |
| [`ErroreConfigurazione`](../src/main/java/unolegends/model/ErroreConfigurazione.java) | motivo per cui le scelte non sono valide |

Il costruttore del record:
- toglie gli spazi esterni dai nomi;
- **aggiunge da solo le opzioni obbligatorie** della modalità, così chi legge la configurazione non deve gestire casi speciali come "No Mercy ha sempre lo stacking";
- **valida**, richiamando il metodo statico `verifica(modalita, nomi, opzioni)`: 2–6 giocatori, nomi non vuoti, nomi non duplicati (senza distinguere maiuscole e minuscole), Endless con almeno 3 giocatori.

`verifica` restituisce l'`ErroreConfigurazione` oppure `null`, e il controller lo usa **prima** di creare la configurazione. Il costruttore ripete il controllo come rete di sicurezza e, con dati non validi, lancia una normale `IllegalArgumentException`: è un errore di programmazione, non un caso da mostrare all'utente. Una `ConfigurazionePartita` che esiste è quindi sempre valida.

**Perché un enum di errori e non un messaggio:** il model non produce testo, come per gli eventi e le richieste di input. La frase la scrive il controller (`messaggioPer(ErroreConfigurazione)`).

`TipoMazzo` è stato eliminato: non era usato e lo sostituisce `ModalitaDiGioco`.

### 3.6 Regole: `RegoleDiGioco.standard(opzioni)`

```java
public static RegoleDiGioco standard(Set<OpzionePartita> opzioni) {
    IStrategiaFinePartita finePartita = opzioni.contains(OpzionePartita.ENDLESS)
            ? new StrategiaFinePartitaEndless()
            : new StrategiaFinePartitaStandard();
    return new RegoleDiGioco(/* strategie standard */ ..., finePartita);
}
```

- Ogni opzione sostituisce **una** strategia, e le altre restano standard.
- Un'opzione nuova costa: un valore in `OpzionePartita`, una riga qui e i testi nel controller.
- `standard()` senza argomenti resta disponibile e equivale a `standard(Set.of())`.

### 3.7 `PartitaFactory`

- `creaPartita(ConfigurazionePartita)`: mazzo da `config.modalita().getMazzoFactory()`, regole da `creaRegole(config)`, giocatori con i nomi scelti.
- `creaRegole` è uno `switch` **esaustivo** sulla modalità. Quando allo Step 4 si aggiungerà `NO_MERCY`, il compilatore obbligherà a scrivere il caso `RegoleDiGioco.noMercy(...)`.
- `creaPartita(MazzoFactory, List<String>, RegoleDiGioco)` resta pubblico, per provare il dominio con regole costruite a mano.
- `creaRegoleStandard()` è stato eliminato.

### 3.8 Controller e DTO

[`UnoLegendsGame`](../src/main/java/unolegends/controller/UnoLegendsGame.java) espone il menu di setup con metodi statici che usano **solo stringhe**:

| Metodo | A cosa serve |
|---|---|
| `getModalitaDisponibili()` | nomi delle modalità, per il menu |
| `getOpzioniConfigurabili(modalita)` | nomi delle opzioni da chiedere. **Quelle obbligatorie non compaiono**: la CLI non sa che No Mercy ha lo stacking, non lo chiede e basta |
| `descriviOpzione(opzione)` | la frase che spiega l'opzione |
| `getMinimoGiocatori()` / `getMassimoGiocatori()` | limiti da mostrare nella domanda |
| `verificaImpostazioni(modalita, opzioni, nomi)` | converte le stringhe negli enum del model e chiama `ConfigurazionePartita.verifica`. Restituisce la frase d'errore, oppure `null` se le scelte sono valide |
| `avviaPartita(modalita, opzioni, nomi)` | crea la `ConfigurazionePartita`, chiama la factory e `iniziaGioco()`. Va chiamato dopo la verifica: con scelte non valide lancia `IllegalArgumentException` |

Sostituisce `avviaPartitaStandard()`.

[`StatoPartitaDto`](../src/main/java/unolegends/controller/StatoPartitaDto.java) è un `record` ed è l'**unico DTO**. Oltre ai campi di prima contiene `giocatori`, `sensoOrario` (con 3 o più giocatori cambia davvero chi viene dopo) e `classifica`. I pezzi dello stato sono tipi interni:
- `StatoPartitaDto.Fase` (prima `FaseTurnoDto`);
- `StatoPartitaDto.Richiesta(messaggio, opzioni)` (prima `RichiestaInputDto`);
- `StatoPartitaDto.Giocatore(nome, numeroCarte, stato, diTurno)`, con lo stato già tradotto ("in gioco", "terminato al posto n. 2", "eliminato").

Nuovi eventi, con le relative frasi in `descriviEvento`:
- `GIOCATORE_TERMINATO`, con la posizione in `quantita`;
- `GIOCATORE_ELIMINATO`.

### 3.9 CLI

[`UnoLegendsCli`](../src/main/java/unolegends/ui/UnoLegendsCli.java):

- **`configuraPartita`** pone le domande del setup:
  - la modalità (se ce n'è una sola la sceglie e la mostra);
  - una domanda `s/n` per ogni opzione configurabile;
  - il numero di giocatori e i nomi (invio = nome predefinito).

  Poi chiama `verificaImpostazioni`: se restituisce una frase la stampa e ripete il setup, altrimenti chiama `avviaPartita`.
- **`mostraStato`** stampa la tabella dei giocatori (carte in mano, stato, `>` per chi è di turno) e il senso di gioco.
- **`mostraClassifica`** a fine partita stampa vincitore e classifica.
- `leggiRiga` sostituisce `reader.readLine().trim()`: se l'input finisce, la CLI si chiude in modo pulito invece di lanciare una `NullPointerException`.

---

## 4. Verifiche fatte

- **Compilazione** con il comando del README: nessun errore.
- **Simulazione automatica** (programma di prova esterno al progetto, non incluso): 1.800 partite complete giocate da un "bot", 200 per ogni numero di giocatori da 2 a 6, in Standard e in Endless. A ogni passo ha controllato che:
  - il giocatore di turno e il "prossimo giocatore" siano sempre `IN_GIOCO`;
  - ogni partita termini;
  - la classifica contenga tutti i giocatori, senza duplicati;
  - in Standard il vincitore abbia 0 carte e ci sia un solo terminato;
  - in Endless finiscano le carte `n − 1` giocatori, in classifica prima dell'ultimo rimasto, che ha ancora carte.
- **Eliminazione** (chiamata a mano): stato, mano svuotata, la partita continua con 3 in gioco e termina con 1; classifica `G1, G2, G4, G3` dopo l'eliminazione di G3, poi G4, poi G2; il turno salta un eliminato.
- **Validazione**: `verifica` restituisce l'errore giusto per Endless con 2 giocatori, 1 o 7 giocatori, nomi duplicati (`"Anna"` e `" anna "`) e nome vuoto, e `null` per una configurazione valida. Il costruttore con dati non validi lancia `IllegalArgumentException`. Dal controller, `verificaImpostazioni` restituisce la frase giusta anche per una modalità sconosciuta.
- **CLI con input preparato in anticipo**: il setup rifiuta Endless con 2 giocatori e i nomi duplicati, poi avvia una partita a 3 con la tabella dei giocatori.
- **Partita Endless a 4 giocatori dal controller**: frasi "ha finito le carte: si classifica al posto n. X", stati nella tabella, classifica finale corretta.

---

## 5. Cosa resta aperto

| # | Questione | Note |
|---|---|---|
| 1 | **Cambio giro con 2 giocatori in gioco** | Oggi inverte solo il senso e quindi non fa saltare il turno. La regola ufficiale per 2 giocatori lo tratta come un blocco. Il caso si presenta anche in Endless, quando restano in due. Non è stato cambiato perché modifica il comportamento attuale: va deciso |
| 2 | **Builder per `RegoleDiGioco`** | Da rivalutare agli Step 5–6 (vedi 2.3) |
| 3 | **Eliminazione del giocatore di turno o di chi doveva saltare** | Da gestire allo Step 4, quando la Mercy rule chiamerà `eliminaGiocatore` |
| 4 | **Test automatici** | La simulazione di questo step era un programma di prova esterno. Con Maven + JUnit (Step 7, o prima) diventerebbe un test vero da tenere nel progetto |

---

## 6. File toccati

| File | Modifica |
|---|---|
| `model/StatoGiocatore.java` | **nuovo** |
| `model/ModalitaDiGioco.java`, `model/OpzionePartita.java` | **nuovi** |
| `model/ConfigurazionePartita.java`, `model/ErroreConfigurazione.java` | **nuovi** |
| `domain/rules/IStrategiaFinePartita.java`, `domain/rules/impl/StrategiaFinePartitaStandard.java`, `domain/rules/impl/StrategiaFinePartitaEndless.java` | **nuovi** |
| `model/TipoMazzo.java` | **eliminato** |
| `controller/FaseTurnoDto.java`, `controller/RichiestaInputDto.java` | **eliminati**: diventano tipi interni di `StatoPartitaDto` (§7) |
| `model/CartaNumero.java`, `model/CartaSpeciale.java` | **eliminati**: unificati in `Carta` (§8) |
| `model/Carta.java` | da classe astratta a classe concreta `final` con colore, valore ed effetto (§8) |
| `model/StandardMazzoFactory.java` | crea `new Carta(...)` (§8) |
| `model/Partita.java` | turni con N giocatori, arrivi, classifica, `eliminaGiocatore`; tolto `vincitore` |
| `model/Giocatore.java` | campo `stato` e metodi relativi |
| `model/Mazzo.java` | `aggiungiCarteERimescola` |
| `model/RegoleDiGioco.java` | strategia di fine partita, `standard(opzioni)` |
| `model/PartitaFactory.java` | `creaPartita(config)`, `creaRegole`, nomi dei giocatori |
| `model/TipoEvento.java`, `model/EventoPartita.java` | `GIOCATORE_TERMINATO`, `GIOCATORE_ELIMINATO` |
| `controller/UnoLegendsGame.java` | API di setup a stringhe, `verificaImpostazioni`, conversione dei giocatori, nuovi eventi |
| `controller/StatoPartitaDto.java` | diventa un `record` con i tipi interni `Fase`, `Richiesta`, `Giocatore`; giocatori, senso di gioco, classifica |
| `ui/UnoLegendsCli.java` | menu di setup, tabella dei giocatori, classifica |
| `README.md` (radice), `Codice/README.md`, `docs/ROADMAP.md`, `docs/domain_class_diagram.puml`, `docs/roles_and_patterns.md`, `CLAUDE.md` | documentazione allineata |

---

## 7. Revisione dopo la prima versione: meno DTO, niente eccezioni, `TERMINATO`

Nella prima versione dello Step 3 il package `controller` era arrivato a **8 file**:
- `UnoLegendsGame`;
- sei DTO: `StatoPartitaDto`, `FaseTurnoDto`, `RichiestaInputDto`, `GiocatoreDto`, `OpzioneDto`, `ImpostazioniPartitaDto`;
- un'eccezione: `ImpostazioniNonValideException`.

Nel model c'era in più `ConfigurazioneNonValidaException`. Il team li ha trovati troppi e poco eleganti, e ha chiesto una soluzione più compatta.

| Cosa | Prima | Dopo | Perché |
|---|---|---|---|
| Stato verso la UI | `StatoPartitaDto` (classe con getter) + `FaseTurnoDto` + `RichiestaInputDto` + `GiocatoreDto` | **un solo `record` `StatoPartitaDto`** con i tipi interni `Fase`, `Richiesta`, `Giocatore` | Sono pezzi dello stato e vivono solo dentro lo stato. Il record elimina i getter scritti a mano. La UI continua a non importare il model |
| Opzioni nel menu | `List<OpzioneDto>` | `List<String>` + `descriviOpzione(String)` | Un nome e una frase non giustificano una classe |
| Scelte del setup | `ImpostazioniPartitaDto` | tre parametri: `(modalita, opzioni, nomi)` | Tre parametri si leggono bene anche senza un oggetto apposta |
| Errori di configurazione | due eccezioni nostre: `ConfigurazioneNonValidaException` (model) e `ImpostazioniNonValideException` (controller) | **nessuna**: `ConfigurazionePartita.verifica(...)` restituisce `ErroreConfigurazione` o `null`; `UnoLegendsGame.verificaImpostazioni(...)` restituisce la frase o `null` | Uno scenario previsto (l'utente sbaglia una scelta) si gestisce con un valore restituito, come fanno già i comandi di `Partita` (`boolean`, `null`). Per gli errori di programmazione bastano le eccezioni standard di Java |
| Nome dello stato | `ARRIVATO` | **`TERMINATO`** (anche `GIOCATORE_TERMINATO`, `registraTerminato`, `getGiocatoriTerminati`) | Parola più chiara per "ha finito le carte". Non si confonde con `FaseTurno.TERMINATA`, che descrive la partita e non un giocatore |

Risultato: nel `controller` restano **2 file** (`UnoLegendsGame` e `StatoPartitaDto`) e non ci sono classi di eccezione.

**Regola per il futuro:** un dato nuovo per la UI diventa un campo, o un tipo interno, di `StatoPartitaDto`, non un nuovo file DTO.

---

## 8. Refactoring fuori roadmap: una sola classe `Carta`

Fatto durante lo Step 3 ma indipendente dagli step: semplifica il modello delle carte prima della No Mercy.

### Il problema

Le carte erano rappresentate da una classe astratta `Carta` con due sottoclassi:

| Classe | Campi | Effetto |
|---|---|---|
| `CartaNumero extends Carta` | `colore`, `valore`, `effetto` | sempre `new Effetto_Nullo()`, creato nel costruttore |
| `CartaSpeciale extends Carta` | `colore`, `valoreCarta`, `effetto` | ricevuto dal costruttore |

Le due sottoclassi erano **identiche**: stessi campi, stessi metodi, stessa descrizione. L'unica differenza era chi sceglieva l'effetto. La classe astratta, a sua volta, non conteneva codice: dichiarava solo i metodi astratti. Ne seguivano tre problemi:

- **l'ereditarietà non esprimeva nessuna differenza di comportamento**, perché il comportamento di una carta sta tutto nel suo `Ieffetto` (Strategy) e per le carte senza effetto esiste già il Null Object `Effetto_Nullo`;
- **lo Step 4 avrebbe rotto la gerarchia:** lo 0 e il 7 della No Mercy sono carte numeriche **con un effetto**. La ROADMAP prevedeva di aggiungere a `CartaNumero` un costruttore con l'effetto, e a quel punto le due classi sarebbero diventate la stessa classe con due nomi;
- **l'unico `instanceof` del progetto** stava nel controller (`carta instanceof CartaSpeciale`), solo per poter chiamare `getDescrizioneBreve(Colore)` sui jolly.

### La soluzione

Un'unica classe concreta e `final`, [`Carta`](../src/main/java/unolegends/model/Carta.java):

```java
public final class Carta {
    public Carta(Colore colore, ValoreCarta valore, Ieffetto effetto)   // carte con effetto
    public Carta(Colore colore, ValoreCarta valore)                     // carte senza effetto: Effetto_Nullo

    public Colore getColore()
    public ValoreCarta getValoreCarta()
    public Ieffetto getEffetto()
    public String toTestoBase()
    public String getDescrizioneBreve()
    public String getDescrizioneBreve(Colore coloreVisualizzato)        // jolly con il colore scelto
}
```

- `CartaNumero` e `CartaSpeciale` sono state **eliminate**.
- `StandardMazzoFactory` crea `new Carta(colore, valore)` per le numeriche e `new Carta(colore, valore, effetto)` per le altre. Il mazzo ha sempre 108 carte.
- Nel controller, `formattaCarta` controlla solo se la carta è nera: **nessun `instanceof`** in tutto il codice.
- La distinzione "numero o carta azione" resta disponibile in `ValoreCarta`, se un giorno servisse.

### Perché è la scelta giusta (GRASP/GoF)

- **Composizione al posto dell'ereditarietà:** ciò che varia (l'effetto) è un oggetto che la carta contiene, non una sottoclasse. Per una carta nuova serve un effetto nuovo, non una classe di carta nuova.
- **Strategy + Null Object:** `Ieffetto` è il punto di variazione, `Effetto_Nullo` evita i controlli `effetto != null` e permette di trattare allo stesso modo tutte le carte.
- **Protected Variations:** le carte No Mercy (0, 7, Scarta tutto, +6, +10, roulette…) si creano tutte con lo stesso costruttore; `Carta` non cambierà.
- `final` rende esplicita la decisione: una carta varia solo tramite l'effetto. Se un giorno servisse un comportamento diverso oltre all'effetto, si rivaluterà una sottoclasse.

**Nota per il modello di dominio (UP):** nel modello di dominio i concetti "carta numerica" e "carta azione" possono restare distinti, perché descrivono il gioco. Nel modello di progetto diventano una sola classe con un effetto diverso: è una decisione di design motivata dai punti sopra.

### Verifiche

- Il progetto compila e non contiene più `CartaNumero`, `CartaSpeciale` né `instanceof`.
- Il mazzo standard ha ancora 108 carte.
- La simulazione delle 1.800 partite e la partita Endless dal controller danno gli stessi risultati di prima.
- La CLI mostra le carte come prima, compresi i jolly con il colore scelto.

