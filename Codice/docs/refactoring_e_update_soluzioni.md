# Piano di Refactoring Architetturale ed Espansione — UnoLegends

Documento di proposta tecnica per il team: analizza le limitazioni del codice attuale (`unolegends`), confronta le alternative architetturali, definisce le soluzioni progettuali per le nuove funzionalità (**Personaggi con Passive**, **Stacking +2/+4**, **Endless**, **No Mercy**, **Carte Uniche**, **Observer/Listener**) e propone una **scaletta incrementale a 6 step** per evitare refactor "big-bang".

---

## Indice
1. [Abilità Passive dei Personaggi (Strategy / Policy + Null Object)](#1-abilità-passive-dei-personaggi-strategy--policy--null-object)
2. [Contesti Transitori (`ContestoGiocata` e `ContestoPesca`)](#2-contesti-transitori-contestogiocata-e-contestopesca)
3. [Stacking (`+2` / `+4`) e Fasi del Turno (Strategy + Stato Esplicito / State)](#3-stacking-2--4-e-fasi-del-turno-strategy--stato-esplicito--state)
4. [Observer / Listener, Unificazione Input e Separazione Model–View](#4-observer--listener-unificazione-input-e-separazione-modelview)
5. [Modalità "Endless" e "No Mercy" (Anche Combinate)](#5-modalità-endless-e-no-mercy-anche-combinate)
6. [Carte Uniche dei Personaggi ed Effetti Speciali Avanzati](#6-carte-uniche-dei-personaggi-ed-effetti-speciali-avanzati)
7. [Scaletta Operativa Incrementale (Roadmap a Step)](#7-scaletta-operativa-incrementale-roadmap-a-step)

---

## 1. Abilità Passive dei Personaggi (Strategy / Policy + Null Object)

### Limitazione attuale
- `RegoleDiGioco` modella soltanto regole **globali** di partita (`IStrategia*`).
- `RegoleDiGioco.isGiocabile(Carta candidata, Carta inCima, Partita partita)` non riceve nemmeno il `Giocatore` che sta tentando la giocata (in `Partita`, `getGiocatoreAttivo()` è `private`).
- Non esiste un punto d'estensione per alterare le regole per singolo giocatore (es. *"può giocare carte Verdi su carte Rosse"*, *"immune al +2"*) senza inserire controlli `if` / `instanceof` dentro `Partita` o `RegoleDiGioco`.

### Alternative valutate
- **Observer nel motore regole (Scartata):** Observer è pensato per reagire *dopo* un evento, non per sovrascrivere un booleano (`isGiocabile`) o bloccare preventivamente un'azione (`pescaForzata`). Creerebbe dipendenze fragili sull'ordine dei listener e disperderebbe le regole fuori da `RegoleDiGioco`.
- **Decorator per ogni `IStrategia*` (Secondaria):** Elegante ma verbosa se i personaggi intervengono su punti eterogenei (compatibilità, pesca forzata, salto turno), moltiplicando il numero di classi wrapper.
- **Policy / Hook su `IAbilitaPassiva` + Null Object (Scelta consigliata):** Coerente con `Effetto_Nullo` già presente nel progetto, zero `instanceof`, zero `!= null`, mantiene `RegoleDiGioco` come unico punto di valutazione.

### Soluzione proposta
1. Associare `Giocatore -> Personaggio -> IAbilitaPassiva`, introducendo `PassivaNulla` (**Null Object**) per la modalità standard senza personaggi.
2. Definire metodi `default` neutri (identità) su `IAbilitaPassiva`, così ogni personaggio implementa solo l'hook che gli serve:

```java
public interface IAbilitaPassiva {
    default boolean modificaGiocabilita(boolean giocabileBase, ContestoGiocata ctx) {
        return giocabileBase;
    }

    default int modificaPescaForzata(int numeroCarteBase, ContestoPesca ctx) {
        return numeroCarteBase;
    }

    default boolean isImmuneAlSaltoTurno(OriginePesca origine) {
        return false;
    }
}
```

3. Aggiornare `RegoleDiGioco.isGiocabile(Giocatore giocatore, Carta candidata, Carta inCima, Partita partita)`: calcola prima la regola globale tramite `strategiaCompatibilitaCarte` e poi passa il risultato a `giocatore.getAbilitaPassiva().modificaGiocabilita(...)`.
4. `Partita` resta un puro **Coordinator** senza conoscere il tipo concreto di alcun personaggio.

---

## 2. Contesti Transitori (`ContestoGiocata` e `ContestoPesca`)

### Limitazione attuale
- `Partita` contiene lo **stato persistente** del gioco (`coloreAttuale`, `pilaDegliScarti`, `sensoOrario`), ma non i **metadati transitori** dell'azione in corso (es. la `cartaCandidata` prima che venga giocata, oppure la causa di una pescata).
- `Effetto_pescata_carte` esegue un ciclo `for` invocando `partita.pescaCartaForzata(prossimoGiocatore)` una carta alla volta.

### Perché è un problema
Né `Partita` né `RegoleDiGioco` conoscono il totale delle carte in arrivo né la causa della pesca (`+2`, `+4`, penalità UNO, carta unica). Una passiva come *"immune al +2, ma vulnerabile al +4 e alla penalità UNO"* non può distinguere le pescate.

### Soluzione proposta
1. Introdurre l'enum di dominio `OriginePesca`:
   `VOLONTARIA`, `PIU_DUE`, `PIU_QUATTRO`, `PENALITA_UNO`, `CARTA_UNICA`.
2. Introdurre due `record` immutabili leggeri:
   - `ContestoGiocata(Giocatore giocatore, Carta cartaCandidata, Carta cartaInCima, Colore coloreAttuale)`
   - `ContestoPesca(Giocatore bersaglio, OriginePesca origine)`
3. Rimuovere il ciclo `for` da `Effetto_pescata_carte`: l'effetto delega l'intera penalità in una sola chiamata:
   `partita.applicaPescaForzata(bersaglio, numeroCarte, origine)`.
4. In `Partita`, calcolare il numero effettivo di carte tramite `regole.calcolaCartePescaForzata(...)` (dove una passiva scrive un semplice `if (ctx.origine() == OriginePesca.PIU_DUE) return 0;`) ed eseguire il ciclo di `pescaSingola` solo sul totale risultante.

---

## 3. Stacking (`+2` / `+4`) e Fasi del Turno (Strategy + Stato Esplicito / State)

### Limitazione attuale
- Gli effetti `+2` e `+4` fanno pescare immediatamente il bersaglio e gli fanno saltare il turno.
- `Partita` gestisce già 5 fasi/attese tramite flag booleani/riferimenti sparsi (`effettoInizialeInAttesa`, `cartaPescataDaGiocare`, `giocatoreInAttesaDichiarazioneUno`, `saltaProssimoTurno`, `vincitore`) controllati all'inizio di ogni metodo.

### Alternative valutate
- **Observer per lo stacking (Scartata):** Lo stacking non è un evento reattivo, ma una sospensione della penalità che cambia le regole del turno successivo in attesa dell'input del giocatore bersaglio.
- **Stato esplicito leggero in `Partita` (Scelta snella):** Aggiungere `int cartePenalitaAccumulate` e `ValoreCarta tipoPenalitaInCorso`. Veloci da introdurre e subito integrabili in `StatoPartitaDto`.
- **GoF State Pattern per le fasi del turno (Alternativa strutturata consigliata a medio termine):** Sostituire i flag in `Partita` con stati espliciti (`TurnoNormale`, `PostPesca`, `RispostaPenalita`, `AttesaInputEffetto`, `AttesaDichiarazioneUno`, `PartitaTerminata`) per eliminare le catene di `if` in `giocaCarta`, `pescaCarta`, `passaTurno`.

### Soluzione proposta
1. **Regola in `RegoleDiGioco`:** Aggiungere `IStrategiaAccumuloPenalita` per configurare:
   - se lo stacking è attivo (standard UNO = disattivo; variante stacking = attivo);
   - quali carte possono rispondere a quale penalità (`+2` su `+2`, `+4` su `+2`, `+4` su `+4`).
2. **Comportamento del turno in `Partita`:**
   - Quando lo stacking è attivo, `+2`/`+4` **non** fanno pescare subito e **non** chiamano `segnalaSaltoTurno()`: incrementano `cartePenalitaAccumulate` e passano normalmente il turno al giocatore target.
   - Finché `cartePenalitaAccumulate > 0`:
     - `RegoleDiGioco.isGiocabile(...)` accetta **solo** carte di risposta ammesse dalla strategia di stacking;
     - se il target gioca una risposta valida, l'accumulo cresce (`+2` → `+4` → `+6`) e il turno passa al successivo;
     - se il target pesca / accetta la penalità (o non ha carte valide), subisce tutte le `cartePenalitaAccumulate` (filtrate dalla propria `IAbilitaPassiva`), l'accumulo torna a `0` e il turno avanza.

---

## 4. Observer / Listener, Unificazione Input e Separazione Model–View

### Limitazioni attuali
1. `Partita` gestisce le attese di input in **3 modi incoerenti**:
   - carta nera iniziale salvata in `effettoInizialeInAttesa` + `rispondiRichiestaInputIniziale`;
   - carta nera giocata nel turno restituita via `EsitoGiocata.inAttesa` + doppia chiamata a `giocaCarta(indice, risposta)`;
   - dichiarazione UNO salvata in `giocatoreInAttesaDichiarazioneUno` + `EsitoGiocata.richiedeDichiarazioneUno()` + `completaDichiarazioneUno`.
2. `EsitoGiocata` ed `EsitoGiocataDto` tendono a gonfiarsi ogni volta che serve comunicare nuovi dettagli alla UI (immunità attivate, eliminazioni, stacking).
3. Gli effetti del Model (es. `Effetto_Cambia_colore`) contengono stringhe testuali UI in italiano (`"Scegli il colore da assegnare alla carta:"`).

### Alternative valutate
- **Solo Pull su `StatoPartitaDto` (Alternativa semplice):** Unificare le attese in `Partita` ed esporre `List<String> ultimiEventi` nello snapshot, evitando Observer. Semplice, ma meno flessibile per future GUI e per log dettagliati in ordine cronologico.
- **Observer con unico `onEvento(EventoDto)` + `TipoEventoPartita` + `switch` nella View (Scartata):** Costringe a un `EventoDto` "coltellino svizzero" pieno di campi `null`/`0` e viola Open/Closed ad ogni nuovo evento.
- **Observer tipizzato solo come canale verso l'esterno (Scelta consigliata):** Zero `switch` sugli eventi, zero `EsitoGiocata`, zero stringhe UI nel Model.

### Soluzione proposta
1. **Ponte tra layer (per preservare l'isolamento `ui` ↔ `model`):**
   ```text
   Partita ──(notifica)──> IPartitaListener (model)
                                 ▲
                           implementato da
                           UnoLegendsGame (controller)
                                 │ (traduce in tipi primitivi/String)
                                 ▼
                           IGameListener (controller) ──> UnoLegendsCli (ui)
   ```
   `UnoLegendsCli` continua a non importare alcuna classe di `unolegends.model`.

2. **Unificazione delle attese in `Partita`:**
   - Salvare un piccolo stato di operazione sospesa (es. `AzioneInAttesa` che mantiene `Ieffetto effettoInAttesa` e l'eventuale `indiceCartaInAttesa` se la carta è ancora in mano e non è la carta iniziale già negli scarti).
   - Esporre un unico metodo `rispondiInput(String risposta)` (`void`): la UI spedisce la `String`, il Controller la inoltra grezza a `Partita` senza interpretarla, e `Partita` delega validazione/applicazione all'`Ieffetto` in attesa (**Information Expert**).

3. **Callback tipizzati su `IGameListener` (elimina `EsitoGiocata` ed `EsitoGiocataDto`):**
   - I comandi (`giocaCarta(int indice)`, `rispondiInput(String)`, `pescaCarta()`, `passaTurno()`) diventano `void`.
   - Callback informativi (stampabili subito dalla CLI in ordine cronologico):
     - `onCartaGiocata(String giocatore, String carta)`
     - `onMossaRifiutata(String codiceMotivo)` *(o enum `MotivoRifiuto`)*
     - `onPenalitaAccumulata(int totaleCarte)`
     - `onPassivaAttivata(String giocatore, String nomePassiva)`
     - `onEffettoApplicato(...)` *(o callback specifici per scambio mano / turni extra / pesca subita)*
     - `onGiocatoreCompletato(String giocatore, int posizionePodio)`
     - `onGiocatoreEliminato(String giocatore)`
     - `onPartitaTerminata(List<String> classifica, List<String> eliminati)`
   - Callback che richiedono un'azione dell'utente:
     - `onRichiestaDichiarazioneUno(String giocatore)` *(gestito col timer da 5s + `completaDichiarazioneUno(boolean)`)*
     - `onRichiestaInput(TipoRichiestaInput tipo, List<String> opzioni)` con `TipoRichiestaInput` ridotto ai **soli 2 casi** (`SCELTA_COLORE`, `SCELTA_GIOCATORE`) — oppure 2 metodi diretti (`onRichiestaColore(List<String>)`, `onRichiestaBersaglio(List<String>)`).
   - *Nota importante:* la risposta allo stacking `+2`/`+4` **non** è una `RichiestaInput`: è il turno normale del giocatore bersaglio che legge `cartePenalitaAccumulate > 0` in `StatoPartitaDto`.

4. **Regola ferrea (No Re-entrancy):**
   - **Mai** chiamare `reader.readLine()` o richiamare un comando del controller *dentro* un callback del listener.
   - I callback informativi possono stampare subito in console; i callback di richiesta input/UNO salvano solo un flag/oggetto pendente nella CLI, che leggerà l'input nel proprio ciclo `while (true)` solo dopo che `Partita` ha concluso il metodo.

### Riepilogo degli Enum: Cosa Tenere vs Eliminare

| Elemento | Verdetto | Motivazione |
|---|---|---|
| `StatoGiocatore` (`ATTIVO`, `COMPLETATO`, `ELIMINATO`) | **Tieni** | Stato di dominio essenziale per Endless + No Mercy; quasi ovunque basta `stato == StatoGiocatore.ATTIVO`. |
| `OriginePesca` (`VOLONTARIA`, `PIU_DUE`, `PIU_QUATTRO`, `PENALITA_UNO`, `CARTA_UNICA`) | **Tieni** | Indispensabile per le passive di immunità (`if (ctx.origine() == OriginePesca.PIU_DUE) return 0;`). |
| `TipoEventoPartita` + `EventoDto` | **Elimina** | Sostituiti da metodi tipizzati su `IGameListener`: nessun campo `null`, nessuno `switch` nella View. |
| `TipoRichiestaInput` | **Snellisci a 2 valori** (o 2 metodi sul listener) | Solo `SCELTA_COLORE` e `SCELTA_GIOCATORE` (zero frasi UI nel Model). Fuori `DICHIARAZIONE_UNO` (callback dedicato) e la penalità accumulata (gestita dal turno). |

---

## 5. Modalità "Endless" e "No Mercy" (Anche Combinate)

### Limitazione attuale
- `Partita` usa un singolo `Giocatore vincitore` e termina appena `vincitore != null`.
- `avanzaGiocatore()` e `getProssimoGiocatore()` calcolano `(indice ± 1) % giocatori.size()` assumendo che tutti i giocatori restino sempre in gioco.
- `IStrategiaCondizioneSconfitta` esiste già in `RegoleDiGioco`, ma non è mai invocata da `Partita`.

### Soluzione proposta
1. **Stato giocatore e classifica in `Partita`:**
   - Introdurre `StatoGiocatore` (`ATTIVO`, `COMPLETATO`, `ELIMINATO`), `List<Giocatore> classifica` e un flag `boolean partitaTerminata`.
2. **Eliminazione No Mercy (`IStrategiaCondizioneSconfitta`):**
   - Creare `StrategiaSconfittaNoMercy(25)` che verifica `giocatore.getMano().size() >= sogliaCarte`.
   - Invocare `verificaEliminazione(Giocatore g)` al termine di ogni operazione di pesca.
   - Quando un giocatore viene eliminato:
     - passa a `ELIMINATO`;
     - **le sue 25+ carte vengono rimesse in circolo** (nel mazzo o sotto la `PilaDegliScarti` preservando la carta in cima) per evitare l'esaurimento del deck da 108 carte;
     - se era il giocatore attivo, azzera lo stato post-pesca e avanza il turno al prossimo attivo;
     - notifica `onGiocatoreEliminato(...)` e verifica se la partita deve terminare (se resta 1 solo attivo in No Mercy, l'ultimo sopravvissuto vince).
3. **Fine partita (`IStrategiaFinePartita` in `RegoleDiGioco`):**
   - `StrategiaFinePartitaStandard`: termina se `classifica.size() >= 1 || giocatoriAttivi <= 1`.
   - `StrategiaFinePartitaEndless`: termina solo se `giocatoriAttivi <= 1`.
   - Copre automaticamente tutte e 4 le combinazioni (`Standard`, `Endless`, `Standard + No Mercy`, `Endless + No Mercy`).
   - *(Nota: la modalità Endless ha senso reale da 3+ giocatori; occorre quindi abilitare la scelta del numero di giocatori nel setup CLI → `UnoLegendsGame` → `PartitaFactory`.)*
4. **Scorrimento turni sicuro:**
   - Sostituire ogni `(indice ± 1) % n` in `avanzaGiocatore()` e `getProssimoGiocatore()` con un unico helper privato `trovaProssimoIndiceAttivo(int daIndice)` che salta tutti i giocatori non `ATTIVO`.

---

## 6. Carte Uniche dei Personaggi ed Effetti Speciali Avanzati

### 6.1. Input dinamico in `Ieffetto` (per *"giocatore X"* e *"Rubamazzo"*)
- **Limitazione attuale:** `getRichiestaInput()` e `validaRispostaInput(String)` non ricevono `Partita`, quindi non possono proporre né validare liste dinamiche (come gli avversari attualmente `ATTIVO`).
- **Soluzione:** Aggiornare le firme in `Ieffetto` preservando il comportamento corretto di default per gli effetti *senza* input:

```java
public interface Ieffetto {
    void attivaEffetto(Partita partita);

    default RichiestaInput getRichiestaInput(Partita partita) {
        return null;
    }

    default boolean validaRispostaInput(String risposta, Partita partita) {
        if (getRichiestaInput(partita) == null) {
            return risposta == null;
        }
        return risposta != null;
    }

    default void attivaEffetto(Partita partita, String risposta) {
        if (getRichiestaInput(partita) != null) {
            throw new IllegalArgumentException("L'effetto richiede un input");
        }
        attivaEffetto(partita);
    }
}
```

- `RichiestaInput` conterrà `(TipoRichiestaInput tipo, List<String> opzioni)` (zero frasi UI nel Model; `UnoLegendsCli` mappa `TipoRichiestaInput` nel prompt testuale).
- Esporre `partita.getNomiAvversariAttivi()` (filtra `StatoGiocatore.ATTIVO` escludendo `giocatoreAttivo`).

### 6.2. Modellazione di `CartaUnica` e trappola del rimescolamento
- **Limitazione attuale:** Aggiungere le carte uniche in `ValoreCarta` gonfierebbe l'enum ad ogni nuovo personaggio; inoltre, se finiscono in `PilaDegliScarti`, al rimescolamento verrebbero pescate da altri giocatori.
- **Soluzione:**
  - Creare `CartaUnica extends Carta` (con `nome`, `Colore`, `Ieffetto`), istanziata dal `Personaggio` tramite Factory Method (`personaggio.creaCartaUnica()`) e inserita nella mano iniziale in `PartitaFactory` (`StandardMazzoFactory` resta puro a 108 carte).
  - Aggiungere `public boolean isRimescolabile()` su `Carta` (`true` di default, `false` in `CartaUnica`) filtrandola in `PilaDegliScarti.estraiCartePerRimescolamento()` — oppure spostare le carte uniche giocate in una lista di carte rimosse/esiliate senza coprire la carta in cima agli scarti.

### 6.3. *"Salta il turno di tutti i giocatori per 2 volte"* → Contatore turni extra
- **Limitazione attuale:** `Partita` ha solo `boolean saltaProssimoTurno` (salta 1 giocatore). Moltiplicare `(numGiocatori - 1) * 2` si rompe se qualcuno esce (Endless) o viene eliminato (No Mercy).
- **Soluzione:** Modellare l'effetto come **turni extra consecutivi del giocatore attivo** (`private int turniExtraGiocatoreAttivo = 0;` + metodo package-private `aggiungiTurniExtra(int n)`):

```java
public void aggiornaGiocatoreAttivo() {
    if (turniExtraGiocatoreAttivo > 0 && isAttivo(getGiocatoreAttivo())) {
        turniExtraGiocatoreAttivo--;
        return; // Tocca di nuovo allo stesso giocatore
    }
    avanzaGiocatore();
    if (saltaProssimoTurno) {
        saltaProssimoTurno = false;
        avanzaGiocatore();
    }
}
```

### 6.4. *"Fai pescare 4 carte al giocatore X"* → Pesca forzata tipizzata
- L'effetto usa `getRichiestaInput(Partita)` (`TipoRichiestaInput.SCELTA_GIOCATORE`) per scegliere `X` tra gli avversari attivi e invoca `partita.applicaPescaForzata(bersaglio, 4, OriginePesca.CARTA_UNICA)`.
- In questo modo passano automaticamente sia eventuali **passive di immunità** di `X` sia il controllo **eliminazione No Mercy (25 carte)**.
- *(Nota: se dovesse anche saltare il prossimo turno dello specifico giocatore `X` non adiacente, introdurre un `Set<Giocatore> turnoDaSaltare` in `Partita` invece di riusare il booleano globale `saltaProssimoTurno`.)*

### 6.5. *"Rubamazzo"* (Scambio mano) → Due edge case critici in `Partita.giocaCarta`
1. **Giocata come ultima carta:** `estraiCarta` svuota la mano del giocatore attivo *prima* di `attivaEffetto`; se `attivaEffetto` scambia subito le mani, il giocatore regala la propria mano vuota all'avversario facendolo vincere.
   - *Soluzione:* decidere una regola esplicita — o **vietare** di giocare `Rubamazzo` come ultima carta (controllo in `isGiocabile` / effetto), oppure controllare `isVincitore` prima dello scambio, oppure verificare `isVincitore` su **entrambi** i giocatori dopo l'effetto.
2. **Dichiarazione UNO post-scambio:** oggi `Partita` controlla `avevaDueCarte && mano.size() == 1`. Se un giocatore con 5 carte ruba la mano da 1 carta di un avversario, `avevaDueCarte` era `false`.
   - *Soluzione:* sostituire con `boolean avevaPiuDiUnaCarta = giocatoreAttivo.getMano().size() > 1;` prima dell'effetto e verificare `mano.size() == 1` dopo l'effetto.
3. Esporre `scambiaMani(Giocatore g1, Giocatore g2)` (package-private) su `Partita`, delegando a `Giocatore` la sostituzione controllata della propria mano.

---

## 7. Scaletta Operativa Incrementale (Roadmap a Step)

Fare tutto in un unico refactor "one-shot" comporta alto rischio di regressioni e diff ingestibili per il team. La sequenza qui sotto è ordinata per **dipendenze strette**: dopo ogni singolo step il progetto **compila, gira in CLI ed è testabile** senza codice morto o workaround temporanei.

| Step | Obiettivo / Milestone | Modifiche Principali | Perché in questa posizione |
|---|---|---|---|
| **Step 1** | **Fondamenta del Dominio e Pulizia** | • `Ieffetto` riceve `Partita` in `getRichiestaInput`/`validaRispostaInput`<br>• `OriginePesca`, `ContestoGiocata`, `ContestoPesca`<br>• Rimozione del `for` da `Effetto_pescata_carte` → `Partita.applicaPescaForzata(...)`<br>• `RegoleDiGioco.isGiocabile` riceve `Giocatore`; pulizia firma `mancatoUno` | **Zero impatto su UI/CLI.** Prepara gli agganci puliti richiesti da tutti gli step successivi. |
| **Step 2** | **Observer / Listener e Unificazione Input** | • `IPartitaListener` (model) → `UnoLegendsGame` → `IGameListener` (controller/UI) non rientrante<br>• Unificazione attese input in `Partita` (`AzioneInAttesa` + `rispondiInput(String)`)<br>• `TipoRichiestaInput` (2 valori) e rimozione testo UI da `Effetto_Cambia_colore`<br>• Rimozione di `EsitoGiocata` ed `EsitoGiocataDto` (`giocaCarta` diventa `void`) | Da questo punto in poi ogni nuova feature potrà notificare eventi alla UI senza dover estendere (e poi buttare) `EsitoGiocata`. |
| **Step 3** | **N Giocatori + Endless + No Mercy** | • `StatoGiocatore` (`ATTIVO`, `COMPLETATO`, `ELIMINATO`), `classifica`, `trovaProssimoIndiceAttivo`<br>• `StrategiaSconfittaNoMercy(25)` + rientro carte dell'eliminato nel deck/scarti<br>• `IStrategiaFinePartita` (`Standard` vs `Endless`)<br>• Menu avvio CLI con scelta N giocatori + toggle `endless` e `noMercy` | Introduce `StatoGiocatore.ATTIVO` e `trovaProssimoIndiceAttivo`, prerequisiti diretti di `getNomiAvversariAttivi()` e dei turni extra delle carte uniche. |
| **Step 4** | **Stacking (`+2` / `+4`) (+ opz. State Pattern)** | • `IStrategiaAccumuloPenalita` in `RegoleDiGioco`<br>• `cartePenalitaAccumulate` e `tipoPenalitaInCorso` in `Partita` + `StatoPartitaDto`<br>• *(Opzionale ma ideale qui)*: refactor delle fasi del turno di `Partita` a **State Pattern** | Qui tutte le fasi del turno sono ormai note (normale, post-pesca, attesa input, attesa UNO, risposta penalità, terminata): è il momento giusto per consolidarle. |
| **Step 5** | **Personaggi e Abilità Passive** | • `Personaggio`, `IAbilitaPassiva` (metodi `default`), `PassivaNulla`<br>• Hook attivi in `RegoleDiGioco` (`modificaGiocabilita`, `modificaPescaForzata`, `isImmuneAlSaltoTurno`)<br>• Prime passive concrete (es. "Verde su Rosso", "Immune al +2") + selezione personaggio al setup | `ContestoGiocata`, `ContestoPesca`, `OriginePesca`, Stacking, No Mercy e `onPassivaAttivata` esistono già: le passive si innestano al primo colpo. |
| **Step 6** | **Carte Uniche ed Effetti Avanzati** | • `CartaUnica extends Carta` (`isRimescolabile() == false` o esilio) creata da `Personaggio` e distribuita in `PartitaFactory`<br>• `turniExtraGiocatoreAttivo`, `scambiaMani`, `getNomiAvversariAttivi()`, fix UNO (`avevaPiuDiUnaCarta`) ed edge case ultima carta<br>• Implementazione *"Salta tutti ×2"*, *"Fai pescare 4 a X"*, *"Rubamazzo"* | È la feature con il maggior numero di dipendenze (Personaggio, `Ieffetto(Partita)`, `StatoGiocatore.ATTIVO`, `applicaPescaForzata`, Listener, No Mercy/Endless): fatta per ultima non richiede alcun hack temporaneo. |
