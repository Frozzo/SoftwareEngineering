package unolegends.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * GRASP Coordinator / oggetto radice del dominio: coordina il turno delegando agli expert.
 *
 * <p>Lo stato del turno è descritto da un solo campo, {@link FaseTurno}. Ogni comando
 * pubblico controlla per prima cosa di essere permesso nella fase corrente; le
 * richieste di input passano tutte da {@link AzioneInAttesa} e
 * {@link #rispondiInput(String)}; ciò che accade durante il turno viene registrato
 * come {@link EventoPartita} e letto dalla UI con {@link #prelevaEventi()}.</p>
 *
 * <p>Gestisce da 2 a 6 giocatori. Un giocatore esce dai turni quando finisce le
 * carte ({@link StatoGiocatore#TERMINATO}) o viene eliminato
 * ({@link StatoGiocatore#ELIMINATO}). Quando la partita finisce lo decidono le
 * regole ({@code IStrategiaFinePartita}): così la stessa Partita gestisce sia la
 * partita normale sia Endless senza sapere quale delle due è attiva.</p>
 *
 * <p>Dallo Step 5 gestisce lo stacking: le carte di pesca passano da
 * {@link #applicaPenalitaPesca}, che a seconda delle regole fa pescare subito o
 * aggiunge all'{@link Accumulo}; chi deve rispondere è in
 * {@link FaseTurno#RISPOSTA_STACKING} e risponde con {@link #giocaCarta(int)} o
 * {@link #accettaPenalita()}.</p>
 *
 * <p>Dallo Step 6 gestisce i Legend senza conoscerli: le passive le interrogano le
 * regole e i pochi punti di Partita che le riguardano ({@link #getBersagliSelezionabili()},
 * {@link #bloccaProssimo()}, la pesca volontaria), sempre attraverso gli hook di
 * {@link IPassiva}; l'abilità attiva si usa con {@link #usaAbilita()} e passa dal
 * meccanismo unificato degli input come una carta. Il giocatore congelato è "come se
 * non ci fosse": lo escludono i due soli punti che decidono chi viene dopo
 * ({@link #trovaProssimoIndice}) e chi si può scegliere.</p>
 */
public class Partita {
    private final List<Giocatore> giocatori;
    private final Mazzo mazzo;
    private final PilaDegliScarti pilaDegliScarti;
    private final RegoleDiGioco regole;
    private final List<EventoPartita> eventi = new ArrayList<>();
    private int indiceGiocatoreAttivo;
    private FaseTurno fase = FaseTurno.NORMALE;
    private AzioneInAttesa azioneInAttesa;
    private Carta cartaAppenaPescata;
    /** Giocatori che hanno finito le carte, nell'ordine in cui hanno finito. */
    private final List<Giocatore> terminati = new ArrayList<>();
    /** Giocatori eliminati, in ordine di eliminazione. */
    private final List<Giocatore> eliminati = new ArrayList<>();
    /** Classifica finale; vuota finché la partita non è {@link FaseTurno#TERMINATA}. */
    private List<Giocatore> classifica = List.of();
    private boolean giocoIniziato;
    /**
     * Non è una fase del turno ma un'informazione per il prossimo avanzamento:
     * un effetto (blocca, +2...) chiede di saltare il giocatore successivo e
     * {@link #aggiornaGiocatoreAttivo()} la consuma. Per questo resta separata da
     * {@link FaseTurno}.
     */
    private boolean saltaProssimoTurno;
    /**
     * Turni extra concessi al giocatore di turno (Salta tutti, e allo Step 6 l'abilità
     * Double card). Come {@link #saltaProssimoTurno} non è una fase ma
     * un'informazione per il prossimo avanzamento: invece di passare al successivo,
     * il turno resta al giocatore. È un contatore e non un booleano perché funziona
     * con qualunque numero di giocatori e di eliminati, e permette di sommare più
     * turni extra.
     */
    private int turniExtra;
    /**
     * Accumulo dello stacking in corso (Step 5), oppure {@code null} se non ce n'è
     * uno. Quando non è null il giocatore di turno è in
     * {@link FaseTurno#RISPOSTA_STACKING} (o sta completando la sua risposta: scelta
     * del colore, finestra UNO). Non è un flag sulla fase: contiene i dati che
     * servono alle regole ({@link Accumulo}), mentre la fase la decide
     * {@link #chiudiTurno()} guardando se l'accumulo esiste.
     */
    private Accumulo accumulo;
    private Colore coloreAttuale;
    private boolean sensoOrario = true; // Variabile per tenere traccia del senso di gioco

    /**
     * Costruisce lo stato iniziale della partita e ricava il colore attivo dalla
     * carta iniziale. L'effetto della carta iniziale viene applicato (o messo in
     * attesa di input) da {@link #iniziaGioco()}.
     *
     * <p>L'indice parte dall'ultimo giocatore, così l'avanzamento eseguito da
     * {@link #iniziaGioco()} porta il turno al primo.</p>
     */
    public Partita(List<Giocatore> giocatori, Mazzo mazzo, PilaDegliScarti pilaDegliScarti, RegoleDiGioco regole) {
        this.giocatori = new ArrayList<>(giocatori);
        this.mazzo = mazzo;
        this.pilaDegliScarti = pilaDegliScarti;
        this.regole = regole;
        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        this.coloreAttuale = cartaInCima == null ? null : cartaInCima.getColore();
        this.indiceGiocatoreAttivo = this.giocatori.size() - 1;
    }

    /**
     * Completa l'inizializzazione della partita applicando l'effetto della prima carta.
     * È idempotente per non applicare due volte lo stesso effetto.
     *
     * <p>Se la carta iniziale richiede input, risponde il primo giocatore: la
     * richiesta diventa un'{@link AzioneInAttesa} come tutte le altre e, quando
     * arriva la risposta, l'effetto viene applicato e il turno avanza. Prima esisteva
     * un meccanismo dedicato ({@code effettoInizialeInAttesa} +
     * {@code rispondiRichiestaInputIniziale}); ora è lo stesso delle carte giocate.</p>
     *
     * <p>Dallo Step 5, dopo l'effetto, il turno passa con {@link #chiudiTurno()} come
     * per qualunque carta giocata. Così una carta iniziale di pesca con lo stacking
     * attivo apre un accumulo e il primo giocatore può rispondere, senza un caso
     * speciale per l'inizio della partita.</p>
     */
    public void iniziaGioco() {
        if (giocoIniziato) {
            return;
        }
        this.giocoIniziato = true;
        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        if (cartaInCima == null) {
            return;
        }
        Ieffetto effetto = cartaInCima.getEffetto();
        if (effetto.getRichiestaInput(this) != null) {
            indiceGiocatoreAttivo = 0;
            mettiInAttesa(effetto, risposta -> {
                effetto.attivaEffetto(this, risposta);
                verificaEliminazioni();
                chiudiTurno();
            });
            return;
        }
        effetto.attivaEffetto(this);
        verificaEliminazioni();
        chiudiTurno();
    }

    /**
     * Giocatore di turno. Package-private: lo usano gli effetti che agiscono su
     * "chi ha giocato la carta" (scambio di mano, Scarta tutto).
     */
    Giocatore getGiocatoreAttivo() {
        return giocatori.get(indiceGiocatoreAttivo);
    }

    public String getNomeGiocatoreAttivo() {
        return getGiocatoreAttivo().getNome();
    }

    public List<Carta> getManoGiocatoreAttivo() {
        return getGiocatoreAttivo().getMano();
    }

    /** Fase corrente del turno: è l'unica informazione che la UI deve leggere per sapere cosa chiedere. */
    /**
     * Accumulo dello stacking in corso, oppure {@code null}. Lo leggono le regole
     * (per sapere se una carta può rispondere) e il controller (per mostrarlo).
     */
    public Accumulo getAccumulo() {
        return accumulo;
    }

    public FaseTurno getFase() {
        return fase;
    }

    /** Carta ottenuta con l'ultima pesca volontaria; significativa solo in {@link FaseTurno#POST_PESCA}. */
    public Carta getCartaAppenaPescata() {
        return fase == FaseTurno.POST_PESCA ? cartaAppenaPescata : null;
    }

    /**
     * Richiesta dell'azione in attesa, oppure null se non si è in
     * {@link FaseTurno#ATTESA_INPUT}. La calcola l'effetto (Information Expert),
     * perché può dipendere dallo stato attuale della partita.
     */
    public RichiestaInput getRichiestaInputInAttesa() {
        return fase == FaseTurno.ATTESA_INPUT ? azioneInAttesa.effetto().getRichiestaInput(this) : null;
    }

    /** Primo della classifica, oppure null se la partita non è finita. */
    public String getNomeVincitore() {
        return classifica.isEmpty() ? null : classifica.get(0).getNome();
    }

    /** Tutti i giocatori, in ordine di turno, qualunque sia il loro stato. */
    public List<Giocatore> getGiocatori() {
        return Collections.unmodifiableList(giocatori);
    }

    /**
     * Avversari che il giocatore attivo può scegliere come bersaglio (carta 7 e
     * abilità attive dei Legend): in gioco, non congelati e non protetti da una
     * passiva (Ghost). È l'unico punto che lo decide; gli effetti automatici (carta 0,
     * +2 al successivo, roulette) non passano di qui, quindi colpiscono anche Ghost.
     * Sostituisce {@code getAvversariInGioco()}.
     */
    public List<Giocatore> getBersagliSelezionabili() {
        Giocatore attivo = getGiocatoreAttivo();
        return giocatori.stream().filter(g -> g != attivo && g.isInGioco() && g.isBersagliabile()).toList();
    }

    public int getNumeroGiocatoriInGioco() {
        return (int) giocatori.stream().filter(Giocatore::isInGioco).count();
    }

    public int getNumeroGiocatoriTerminati() {
        return terminati.size();
    }

    /** Giocatori che hanno già finito le carte, nell'ordine in cui hanno finito (posizione = indice + 1). */
    public List<Giocatore> getGiocatoriTerminati() {
        return Collections.unmodifiableList(terminati);
    }

    /**
     * Classifica finale, dal primo all'ultimo; vuota finché la partita non è finita.
     * Ordine: chi ha finito le carte (nell'ordine in cui hanno finito), poi chi era ancora in
     * gioco (meno carte in mano = posizione migliore), poi gli eliminati (l'ultimo
     * eliminato davanti al primo).
     */
    public List<Giocatore> getClassifica() {
        return classifica;
    }

    public boolean isSensoOrario() {
        return sensoOrario;
    }

    public int getIndiceGiocatoreAttivo() {
        return indiceGiocatoreAttivo;
    }

    public PilaDegliScarti getPilaDegliScarti() {
        return pilaDegliScarti;
    }

    /**
     * Restituisce gli eventi registrati dall'ultima lettura e svuota il registro.
     * La UI lo chiama (tramite il controller) dopo ogni comando per raccontare
     * cosa è successo: è un modello "pull", più semplice di un Observer per una
     * CLI sincrona.
     */
    public List<EventoPartita> prelevaEventi() {
        List<EventoPartita> copia = List.copyOf(eventi);
        eventi.clear();
        return copia;
    }

    /**
     * Registra un fatto da mostrare alla UI. Package-private: lo usano Partita
     * stessa e, se servirà, gli effetti nello stesso package.
     */
    void registraEvento(EventoPartita evento) {
        eventi.add(Objects.requireNonNull(evento, "evento non puo essere null"));
    }

    /**
     * Gioca la carta in posizione {@code indiceCarta} del giocatore attivo.
     * Permesso in {@link FaseTurno#NORMALE} e {@link FaseTurno#POST_PESCA}
     * (dopo una pesca si può giocare qualsiasi carta della mano).
     *
     * <p>Valida tutto prima di modificare lo stato (indice, compatibilità). Se
     * l'effetto richiede un input, la carta resta in mano e la giocata diventa
     * un'{@link AzioneInAttesa}: la fase passa a {@link FaseTurno#ATTESA_INPUT} e
     * la giocata si completa in {@link #rispondiInput(String)}. Prima la UI doveva
     * richiamare {@code giocaCarta(indice, risposta)} ricordandosi l'indice, e
     * Partita rifaceva tutte le validazioni.</p>
     *
     * @return true se la giocata è stata accettata (completata o in attesa di input)
     */
    public boolean giocaCarta(int indiceCarta) {
        // In RISPOSTA_STACKING si gioca come sempre: è isGiocabile delle regole ad
        // accettare solo le carte di pesca di valore sufficiente.
        if (fase != FaseTurno.NORMALE && fase != FaseTurno.POST_PESCA
                && fase != FaseTurno.RISPOSTA_STACKING) {
            return false;
        }
        Giocatore giocatoreAttivo = getGiocatoreAttivo();
        Carta cartaSelezionata = giocatoreAttivo.getCartaInPosizione(indiceCarta);
        if (cartaSelezionata == null) {
            return false;
        }
        if (!regole.isGiocabile(giocatoreAttivo, cartaSelezionata, pilaDegliScarti.getCartaInCima(), this)) {
            return false;
        }

        Ieffetto effetto = cartaSelezionata.getEffetto();
        if (effetto.getRichiestaInput(this) != null) {
            mettiInAttesa(effetto, risposta -> completaGiocata(cartaSelezionata, risposta));
            return true;
        }
        completaGiocata(cartaSelezionata, null);
        return true;
    }

    /**
     * Unico punto d'ingresso per rispondere a qualunque richiesta di input
     * (colore della carta iniziale, colore di un jolly, in futuro bersagli e
     * abilità). Valida la risposta con l'effetto in attesa (Information Expert) e,
     * se è valida, esegue il completamento salvato nell'{@link AzioneInAttesa}.
     *
     * <p>Prima di eseguirlo la fase torna a {@link FaseTurno#NORMALE}: è il
     * completamento a decidere la fase successiva (attesa UNO, fine partita, turno
     * del prossimo giocatore...).</p>
     *
     * @return true se la risposta è stata accettata; false se non si è in attesa
     *         di input o se la risposta non è valida (la richiesta resta aperta)
     */
    public boolean rispondiInput(String risposta) {
        if (fase != FaseTurno.ATTESA_INPUT) {
            return false;
        }
        AzioneInAttesa azione = azioneInAttesa;
        if (!azione.effetto().validaRispostaInput(risposta, this)) {
            return false;
        }
        azioneInAttesa = null;
        fase = FaseTurno.NORMALE;
        azione.completamento().accept(risposta);
        return true;
    }

    /**
     * Il giocatore di turno non risponde allo stacking: pesca tutte le carte
     * accumulate e il suo turno finisce, cioè "perde il turno" (Step 5). Permesso
     * solo in {@link FaseTurno#RISPOSTA_STACKING}, anche se il giocatore avrebbe
     * una risposta valida (decisione presa).
     *
     * <p>È un comando dedicato e non {@link #pescaCarta()} (decisione presa): il
     * nome dice cosa succede, e la pesca volontaria non cambia significato a
     * seconda della fase.</p>
     *
     * <p>La pesca passa da {@link #applicaPescaForzata}, quindi le regole (e allo
     * Step 6 le passive) possono modificarla e l'evento di pesca si registra da
     * solo. L'origine è quella della carta se l'accumulo è una carta sola,
     * altrimenti {@link OriginePesca#ACCUMULO}. Dopo la pesca si controlla la Mercy
     * rule, come per tutte le pesche.</p>
     *
     * @return true se il comando è stato accettato
     */
    public boolean accettaPenalita() {
        if (fase != FaseTurno.RISPOSTA_STACKING) {
            return false;
        }
        Giocatore giocatore = getGiocatoreAttivo();
        Accumulo accettato = accumulo;
        accumulo = null;
        registraEvento(EventoPartita.penalitaAccettata(giocatore, accettato.totale()));
        boolean annullata = isPenalitaAnnullata(giocatore, accettato.totale(), accettato.getOriginePenalita());
        applicaPescaForzata(giocatore, accettato.totale(), accettato.getOriginePenalita());
        verificaEliminazioni();
        // Una passiva ha annullato la penalità (La Roccia su un +2 singolo): la carta non
        // ha effetto su di lui, quindi non perde il turno e lo gioca normalmente.
        if (annullata && fase != FaseTurno.TERMINATA && giocatore.isInGioco()) {
            fase = FaseTurno.NORMALE;
            return true;
        }
        chiudiTurno();
        return true;
    }

    /**
     * Unico punto d'ingresso delle penalità delle carte di pesca (+2, +4, +6, +10).
     * L'effetto non sa se lo stacking è attivo: lo chiede Partita alle regole
     * (Protected Variations).
     * <ul>
     *   <li><b>Senza stacking</b>: il giocatore successivo pesca subito e salta il
     *       turno, come fino allo Step 4.</li>
     *   <li><b>Con lo stacking</b>: nessuno pesca e nessun turno viene saltato. La
     *       penalità apre l'accumulo o lo fa crescere; quando il turno passa,
     *       {@link #chiudiTurno()} porta il giocatore successivo in
     *       {@link FaseTurno#RISPOSTA_STACKING}.</li>
     * </ul>
     *
     * <p>Chi deve rispondere è semplicemente il prossimo di turno. Per questo il +4
     * cambio giro con 2 giocatori torna a chi l'ha giocato: il salto lo chiede
     * {@link #cambioGiro()}, non il +4 (decisione presa: "arma a doppio taglio").</p>
     */
    void applicaPenalitaPesca(int numeroCarte, OriginePesca origine) {
        Objects.requireNonNull(origine, "origine non puo essere null");
        if (!regole.isAccumuloAttivo()) {
            Giocatore vittima = getProssimoGiocatore();
            boolean annullata = isPenalitaAnnullata(vittima, numeroCarte, origine);
            applicaPescaForzata(vittima, numeroCarte, origine);
            // Se una passiva annulla la penalità (La Roccia sul +2) la carta non ha
            // effetto: niente pesca e niente salto.
            if (!annullata) {
                segnalaSaltoTurno();
            }
            return;
        }
        accumulo = accumulo == null ? Accumulo.apri(numeroCarte, origine) : accumulo.aggiungi(numeroCarte);
        registraEvento(EventoPartita.accumuloAumentato(accumulo.totale()));
    }

    /**
     * Completa la finestra di dichiarazione UNO dopo una giocata valida.
     * Permesso solo in {@link FaseTurno#ATTESA_UNO}; il giocatore in attesa è
     * sempre quello attivo, perché il turno non è ancora avanzato. Se UNO non è
     * stato dichiarato, le regole applicano la penalità, che passa da
     * {@link #applicaPescaForzata} e quindi produce il proprio evento.
     *
     * @return true se il comando è stato accettato
     */
    public boolean completaDichiarazioneUno(boolean unoDichiarato) {
        if (fase != FaseTurno.ATTESA_UNO) {
            return false;
        }
        Giocatore giocatore = getGiocatoreAttivo();
        if (unoDichiarato) {
            registraEvento(EventoPartita.unoDichiarato(giocatore));
        }
        regole.mancatoUno(giocatore, unoDichiarato, this);
        verificaEliminazioni();
        chiudiTurno();
        return true;
    }

    /**
     * Registra il colore scelto per un effetto di cambio colore; rifiuta null
     * perché lascerebbe la partita senza un colore valido per le mosse successive.
     */
    void applicaColoreScelto(Colore coloreScelto) {
        coloreAttuale = Objects.requireNonNull(coloreScelto, "coloreScelto non puo essere null");
        registraEvento(EventoPartita.coloreScelto(getGiocatoreAttivo(), coloreScelto));
    }

    public Colore getColoreAttuale() {
        return coloreAttuale;
    }

    /**
     * Memorizza che il prossimo avanzamento deve saltare un giocatore.
     * L'avanzamento effettivo è centralizzato in aggiornaGiocatoreAttivo().
     */
    void segnalaSaltoTurno() {
        saltaProssimoTurno = true;
    }

    /**
     * Effetto della carta Blocca: il prossimo salta il turno, a meno che una sua
     * passiva lo renda immune (Pelle di ghiaccio del Freezer, Step 6).
     *
     * <p>È un metodo a parte e non un controllo dentro {@link #segnalaSaltoTurno()}
     * perché quello lo usano anche il cambio giro con 2 giocatori, il +2 senza
     * stacking e la roulette, e non sa <i>perché</i> si salta: interrogando l'hook lì il
     * Freezer sarebbe immune a tutti i salti, mentre la decisione presa è "solo al
     * Blocca".</p>
     */
    void bloccaProssimo() {
        Giocatore bersaglio = getProssimoGiocatore();
        if (bersaglio.isImmuneAlBlocca()) {
            registraEvento(EventoPartita.bloccoEvitato(bersaglio));
            return;
        }
        segnalaSaltoTurno();
    }

    /**
     * true se le passive del bersaglio annullano del tutto una penalità che avrebbe
     * fatto pescare delle carte. In quel caso la penalità non fa nemmeno perdere il
     * turno (La Roccia sul +2 singolo).
     */
    private boolean isPenalitaAnnullata(Giocatore bersaglio, int numeroCarte, OriginePesca origine) {
        return numeroCarte > 0
                && regole.calcolaCartePescaForzata(new ContestoPesca(bersaglio, numeroCarte, origine)) == 0;
    }

    // ------------------------------------------------------------------
    // Abilità attive dei Legend (Step 6)
    // ------------------------------------------------------------------

    /**
     * true se il giocatore di turno può usare ora la sua abilità attiva: siamo in
     * {@link FaseTurno#NORMALE} (decisione presa: prima di pescare o giocare, mai
     * durante lo stacking), ha un Legend, l'abilità è carica e i suoi prerequisiti
     * sono soddisfatti ({@link Ieffetto#isAttivabile}). Lo usa anche il controller per
     * mostrare la voce di menu solo quando serve.
     */
    public boolean isAbilitaUsabile() {
        if (fase != FaseTurno.NORMALE) {
            return false;
        }
        AbilitaAttiva abilita = getGiocatoreAttivo().getAbilitaAttiva();
        return abilita != null && abilita.isPronta() && abilita.getEffetto().isAttivabile(this);
    }

    /**
     * Usa l'abilità attiva del giocatore di turno. Permesso solo quando
     * {@link #isAbilitaUsabile()} è vero.
     *
     * <p>Segue la stessa strada di {@link #giocaCarta(int)}: se l'effetto chiede un
     * input (il bersaglio) si apre un'{@link AzioneInAttesa} e si risponde con
     * {@link #rispondiInput(String)}; altrimenti l'abilità si completa subito. Nessun
     * metodo {@code rispondiXxx} dedicato e nessuna fase nuova.</p>
     *
     * <p>Usare l'abilità <b>non consuma il turno</b> (decisione presa, vale per tutte):
     * dopo si torna in {@link FaseTurno#NORMALE} e si può giocare o pescare.</p>
     *
     * @return true se il comando è stato accettato
     */
    public boolean usaAbilita() {
        if (!isAbilitaUsabile()) {
            return false;
        }
        Giocatore giocatore = getGiocatoreAttivo();
        AbilitaAttiva abilita = giocatore.getAbilitaAttiva();
        Ieffetto effetto = abilita.getEffetto();
        if (effetto.getRichiestaInput(this) != null) {
            mettiInAttesa(effetto, risposta -> completaAbilita(giocatore, abilita, risposta));
            return true;
        }
        completaAbilita(giocatore, abilita, null);
        return true;
    }

    /**
     * Applica l'abilità, avvia la ricarica e controlla la Mercy rule (punto sicuro,
     * come dopo una carta): il bersaglio del Castigatore o di Gifted card, o chi
     * riceve una mano grande con Scambia mazzo, può superare il limite.
     */
    private void completaAbilita(Giocatore giocatore, AbilitaAttiva abilita, String risposta) {
        abilita.avviaRicarica();
        registraEvento(EventoPartita.abilitaUsata(giocatore));
        abilita.getEffetto().attivaEffetto(this, risposta);
        verificaEliminazioni();
        if (fase == FaseTurno.TERMINATA) {
            return;
        }
        if (!giocatore.isInGioco()) {
            chiudiTurno();
            return;
        }
        fase = FaseTurno.NORMALE;
    }

    /**
     * Gestisce la pesca volontaria del giocatore attivo. Permessa solo in
     * {@link FaseTurno#NORMALE}: dopo la pesca la fase diventa
     * {@link FaseTurno#POST_PESCA}, quindi non si può pescare due volte.
     * Il valore true indica che la richiesta è stata accettata, anche se il mazzo
     * e gli scarti non forniscono una carta.
     *
     * <p>Quante carte pescare lo decide la strategia di pesca volontaria delle
     * regole; la carta mostrata come "appena pescata" è l'ultima.</p>
     */
    public boolean pescaCarta() {
        if (fase != FaseTurno.NORMALE) {
            return false;
        }
        Giocatore giocatore = getGiocatoreAttivo();
        int carteTraCuiScegliere = regole.getCarteTraCuiScegliereInPesca(giocatore);
        if (carteTraCuiScegliere > 1) {
            pescaConScelta(giocatore, carteTraCuiScegliere);
            return true;
        }
        List<Carta> cartePescate = regole.pescaVolontaria(giocatore, this);
        registraEvento(EventoPartita.pescaVolontaria(giocatore, cartePescate.size()));
        verificaEliminazioni();
        // Mercy rule: se la pesca lo ha eliminato, il turno passa al successivo.
        if (!giocatore.isInGioco()) {
            chiudiTurno();
            return true;
        }
        cartaAppenaPescata = cartePescate.isEmpty() ? null : cartePescate.get(cartePescate.size() - 1);
        fase = FaseTurno.POST_PESCA;
        return true;
    }

    /**
     * Passiva Doppia pesca (Step 6): il giocatore pesca {@code numero} carte e sceglie
     * quale tenere; le altre tornano in fondo al mazzo. La scelta passa dal meccanismo
     * unificato degli input ({@link EffettoSceltaCartaPescata}) e, quando arriva la
     * risposta, il turno va in {@link FaseTurno#POST_PESCA} come dopo una pesca
     * normale. Se mazzo e scarti danno meno di 2 carte non c'è niente da scegliere.
     *
     * <p>Vale solo nella partita normale, dove nessuno viene eliminato: per questo non
     * serve controllare la Mercy rule.</p>
     */
    private void pescaConScelta(Giocatore giocatore, int numero) {
        List<Carta> cartePescate = new ArrayList<>();
        for (int i = 0; i < numero; i++) {
            Carta carta = pescaSingola(giocatore);
            if (carta == null) {
                break;
            }
            cartePescate.add(carta);
        }
        registraEvento(EventoPartita.pescaVolontaria(giocatore, cartePescate.size()));
        if (cartePescate.size() < 2) {
            cartaAppenaPescata = cartePescate.isEmpty() ? null : cartePescate.get(0);
            fase = FaseTurno.POST_PESCA;
            return;
        }
        EffettoSceltaCartaPescata scelta = new EffettoSceltaCartaPescata(cartePescate);
        mettiInAttesa(scelta, risposta -> {
            scelta.attivaEffetto(this, risposta);
            cartaAppenaPescata = scelta.getCartaTenuta();
            fase = FaseTurno.POST_PESCA;
        });
    }

    /** Rimette una carta in fondo al mazzo (Doppia pesca). */
    void rimettiInFondoAlMazzo(Carta carta) {
        mazzo.aggiungiInFondo(carta);
    }

    /**
     * true se il giocatore può giocare la carta sulla carta in cima. Serve alla
     * pesca No Mercy ("pesca finché trovi una carta giocabile"), che è una
     * strategia esterna e non vede le regole direttamente.
     */
    public boolean isCartaGiocabile(Giocatore giocatore, Carta carta) {
        return regole.isGiocabile(giocatore, carta, pilaDegliScarti.getCartaInCima(), this);
    }

    /**
     * true se il giocatore ha raggiunto la condizione di eliminazione (Mercy rule:
     * 25 carte). Le pesche "a ripetizione" (pesca No Mercy, roulette) lo usano per
     * fermarsi; l'eliminazione vera la esegue Partita in un punto sicuro del turno
     * ({@link #verificaEliminazioni()}).
     */
    public boolean isDaEliminare(Giocatore giocatore) {
        return regole.isEliminato(giocatore, this);
    }

    /**
     * Pesca una carta per il giocatore indicato. Se il mazzo è vuoto, prova a
     * ricostituirlo rimescolando gli scarti (la carta in cima resta agli scarti)
     * e riprova. Aggiunge la carta alla mano solo se ne ha trovata una.
     * Non modifica lo stato del turno né registra eventi, così può essere
     * riutilizzato da pesca volontaria, effetti e penalità.
     */
    public Carta pescaSingola(Giocatore giocatore) {
        Objects.requireNonNull(giocatore, "giocatore non puo essere null");

        Carta carta = mazzo.prelevaCarta();
        if (carta == null) {
            mazzo.rimescolaDaScarti(pilaDegliScarti);
            carta = mazzo.prelevaCarta();
        }
        if (carta != null) {
            giocatore.aggiungiCarta(carta);
        }
        return carta;
    }

    /**
     * Unico punto d'ingresso per tutte le pesche forzate (effetti delle carte,
     * penalità UNO e, in futuro, abilità e accumulo dello stacking). Chiede alle
     * regole quante carte vanno pescate davvero in base al contesto, poi le pesca
     * una alla volta fermandosi se mazzo e scarti sono esauriti.
     *
     * <p>Essendo l'unico punto, è anche l'unico in cui registrare l'evento
     * {@link TipoEvento#PESCA_FORZATA}: ogni causa di pesca presente e futura viene
     * comunicata alla UI senza toccare gli effetti.</p>
     *
     * @return le carte effettivamente pescate
     */
    public List<Carta> applicaPescaForzata(Giocatore bersaglio, int numeroCarte, OriginePesca origine) {
        ContestoPesca contesto = new ContestoPesca(bersaglio, numeroCarte, origine);
        int carteDaPescare = regole.calcolaCartePescaForzata(contesto);
        if (carteDaPescare < numeroCarte) {
            registraEvento(EventoPartita.pescaRidotta(bersaglio, numeroCarte - carteDaPescare));
        }

        List<Carta> cartePescate = new ArrayList<>();
        for (int i = 0; i < carteDaPescare; i++) {
            Carta carta = pescaSingola(bersaglio);
            if (carta == null) {
                break;
            }
            cartePescate.add(carta);
        }
        registraEvento(EventoPartita.pescaForzata(bersaglio, cartePescate.size(), origine));
        return cartePescate;
    }

    /**
     * Consente di passare solo in {@link FaseTurno#POST_PESCA}, cioè dopo una
     * pesca volontaria.
     */
    public boolean passaTurno() {
        if (fase != FaseTurno.POST_PESCA) {
            return false;
        }
        chiudiTurno();
        return true;
    }

    /**
     * Inverte il senso di avanzamento dei turni, come richiesto dall'effetto
     * della carta cambio giro.
     */
    public void cambioGiro() {
        this.sensoOrario = !sensoOrario; // Inverte il senso di gioco
        registraEvento(EventoPartita.sensoInvertito());
        // Regola ufficiale: con due giocatori in gioco invertire il senso equivale
        // a far saltare il turno all'altro. Vale in entrambe le modalità e anche in
        // Endless quando restano in due; la decide Partita perché è lei a sapere
        // quanti giocatori sono in gioco. Dallo Step 6 si contano quelli che partecipano
        // ai turni: un congelato è "come se non ci fosse".
        if (getNumeroPartecipantiAiTurni() == 2) {
            segnalaSaltoTurno();
        }
    }

    /**
     * Concede al giocatore di turno un turno in più: tutti gli altri saltano e lui
     * rigioca (Salta tutti). Il turno extra viene consumato da
     * {@link #aggiornaGiocatoreAttivo()}.
     */
    void concediTurnoExtra() {
        turniExtra++;
    }

    /**
     * Giocatori che partecipano ai turni a partire da quello di turno, nel senso di
     * gioco. Serve alla carta 0 della No Mercy, che fa passare ogni mano al
     * successivo. I congelati sono esclusi (decisione presa, Step 6): sono "come se non
     * ci fossero" e tengono la loro mano.
     */
    List<Giocatore> getGiocatoriPartecipantiNelSensoDiGioco() {
        List<Giocatore> ordine = new ArrayList<>();
        int indice = indiceGiocatoreAttivo;
        for (int i = 0; i < getNumeroPartecipantiAiTurni(); i++) {
            ordine.add(giocatori.get(indice));
            indice = trovaProssimoIndice(indice, Giocatore::partecipaAiTurni);
        }
        return ordine;
    }

    /** Giocatori in gioco e non congelati. */
    private int getNumeroPartecipantiAiTurni() {
        return (int) giocatori.stream().filter(Giocatore::partecipaAiTurni).count();
    }

    /**
     * Mette delle carte negli scarti sotto la carta in cima, senza attivarne gli
     * effetti (Scarta tutto).
     */
    void scartaSottoLaCima(List<Carta> carte) {
        pilaDegliScarti.inserisciSottoLaCima(carte);
    }

    /**
     * Pesca forzata "a ripetizione": il bersaglio pesca una carta alla volta
     * finché una carta soddisfa la condizione (roulette: finché esce il colore
     * scelto). Si ferma anche se mazzo e scarti sono esauriti o se il bersaglio
     * raggiunge la condizione di eliminazione (25 carte in No Mercy).
     *
     * <p>Come {@link #applicaPescaForzata} è un unico punto in Partita, così gli
     * effetti non scrivono cicli di {@link #pescaSingola}. Non passa da
     * {@code calcolaCartePescaForzata}, perché il numero di carte non è noto in
     * anticipo: per questo Salesman non ha lo sconto sulla roulette (decisione
     * presa).</p>
     *
     * @return le carte pescate, l'ultima delle quali è quella che ha fermato la pesca
     */
    public List<Carta> applicaPescaFinoA(Giocatore bersaglio, Predicate<Carta> condizioneDiArresto,
                                         OriginePesca origine) {
        Objects.requireNonNull(bersaglio, "bersaglio non puo essere null");
        Objects.requireNonNull(condizioneDiArresto, "condizioneDiArresto non puo essere null");
        List<Carta> cartePescate = new ArrayList<>();
        while (true) {
            Carta carta = pescaSingola(bersaglio);
            if (carta == null) {
                break;
            }
            cartePescate.add(carta);
            if (condizioneDiArresto.test(carta) || isDaEliminare(bersaglio)) {
                break;
            }
        }
        registraEvento(EventoPartita.pescaForzata(bersaglio, cartePescate.size(), origine));
        return cartePescate;
    }

    /**
     * Calcola il prossimo giocatore che giocherà davvero, rispettando il senso
     * corrente, senza modificare l'indice del giocatore attivo. È usato dagli effetti
     * che agiscono sul giocatore successivo, come la pescata forzata.
     *
     * <p>Salta i congelati (Step 6): per esempio con 2 giocatori, se l'avversario è
     * congelato, il prossimo è chi gioca, che quindi pesca il suo stesso +2
     * (decisione presa: il congelato "è come se non ci fosse").</p>
     */
    public Giocatore getProssimoGiocatore() {
        return giocatori.get(trovaProssimoIndice(indiceGiocatoreAttivo, Giocatore::partecipaAiTurni));
    }

    /**
     * Elimina un giocatore: le sue carte tornano subito nel mazzo, che viene
     * rimescolato (decisione del progetto), e il giocatore finisce in fondo alla
     * classifica. Se resta un solo giocatore in gioco la partita termina.
     *
     * <p>Lo chiama {@link #verificaEliminazioni()}, che gestisce anche i casi
     * limite del turno (eliminato di turno, eliminato che doveva saltare). Con le
     * regole standard nessuno viene eliminato.</p>
     */
    void eliminaGiocatore(Giocatore giocatore) {
        Objects.requireNonNull(giocatore, "giocatore non puo essere null");
        if (!giocatore.isInGioco()) {
            return;
        }
        mazzo.aggiungiCarteERimescola(giocatore.svuotaMano());
        giocatore.segnaComeEliminato();
        eliminati.add(giocatore);
        registraEvento(EventoPartita.giocatoreEliminato(giocatore));
        verificaFinePartita();
    }

    /**
     * Sospende un'azione in attesa di risposta e porta il turno in
     * {@link FaseTurno#ATTESA_INPUT}. È l'unico modo in cui Partita apre una
     * richiesta di input.
     */
    private void mettiInAttesa(Ieffetto effetto, Consumer<String> completamento) {
        azioneInAttesa = new AzioneInAttesa(effetto, completamento);
        fase = FaseTurno.ATTESA_INPUT;
    }

    /**
     * Parte finale di una giocata, comune al caso con e senza input: sposta la
     * carta negli scarti, applica l'effetto e decide la fase successiva
     * (vittoria, finestra UNO, oppure turno del prossimo giocatore).
     */
    private void completaGiocata(Carta carta, String risposta) {
        Giocatore giocatoreAttivo = getGiocatoreAttivo();
        List<Carta> manoPrima = new ArrayList<>(giocatoreAttivo.getMano());
        giocatoreAttivo.rimuoviCarta(carta);
        pilaDegliScarti.aggiungiCarta(carta);

        // Le carte colorate aggiornano subito il colore attivo;
        // per una carta nera è l'effetto a impostare il colore scelto.
        if (carta.getColore() != Colore.NERO) {
            coloreAttuale = carta.getColore();
        }
        cartaAppenaPescata = null;
        carta.getEffetto().attivaEffetto(this, risposta);

        // Mercy rule: dopo l'effetto qualcuno può aver superato il limite di carte
        // (vittima di un +10, o chi ha ricevuto una mano grande con la 7 o lo 0).
        verificaEliminazioni();
        if (fase == FaseTurno.TERMINATA) {
            return;
        }
        if (!giocatoreAttivo.isInGioco()) {
            chiudiTurno();
            return;
        }

        if (regole.isVincitore(giocatoreAttivo, this)) {
            registraTerminato(giocatoreAttivo);
            // In Endless la partita può continuare: il turno passa al prossimo in gioco.
            if (fase != FaseTurno.TERMINATA) {
                chiudiTurno();
            }
            return;
        }
        // Con una sola carta in mano, il turno resta al giocatore finché non dichiara UNO.
        if (deveDichiarareUno(giocatoreAttivo, manoPrima)) {
            fase = FaseTurno.ATTESA_UNO;
            return;
        }
        chiudiTurno();
    }

    /**
     * Si deve dichiarare UNO quando, dopo la giocata, resta in mano una sola carta
     * che il giocatore <b>aveva già</b>. Così:
     * <ul>
     *   <li>giocare una carta avendone due apre la finestra UNO (come prima);</li>
     *   <li>"Scarta tutto" che lascia una sola carta apre la finestra UNO (decisione presa);</li>
     *   <li>dopo uno scambio di mano (7) o una rotazione (0) la carta rimasta viene
     *       da un altro giocatore, quindi UNO non va dichiarato (decisione presa).</li>
     * </ul>
     * Il confronto è per identità, come in {@link Giocatore#rimuoviCarta(Carta)}.
     */
    private static boolean deveDichiarareUno(Giocatore giocatore, List<Carta> manoPrima) {
        List<Carta> mano = giocatore.getMano();
        if (mano.size() != 1 || manoPrima.size() < 2) {
            return false;
        }
        Carta rimasta = mano.get(0);
        return manoPrima.stream().anyMatch(carta -> carta == rimasta);
    }

    /**
     * Mercy rule: elimina chi ha raggiunto la condizione di eliminazione. Si chiama
     * in un punto sicuro, cioè dopo che l'effetto o la pesca sono stati applicati
     * per intero, e non durante: altrimenti un effetto come il +10, che prima fa
     * pescare e poi chiede il salto del turno, farebbe saltare il turno a un
     * innocente.
     *
     * <p>Casi limite (decisioni prese):</p>
     * <ul>
     *   <li>se l'eliminato è il giocatore che doveva saltare il turno, il salto si
     *       annulla e non passa al giocatore dopo di lui;</li>
     *   <li>se l'eliminato è il giocatore di turno, lo gestisce il chiamante
     *       facendo avanzare il turno.</li>
     * </ul>
     */
    private void verificaEliminazioni() {
        Giocatore prossimo = getProssimoGiocatore();
        for (Giocatore giocatore : List.copyOf(giocatori)) {
            if (fase == FaseTurno.TERMINATA) {
                return;
            }
            if (giocatore.isInGioco() && regole.isEliminato(giocatore, this)) {
                if (giocatore == prossimo) {
                    saltaProssimoTurno = false;
                }
                eliminaGiocatore(giocatore);
            }
        }
    }

    /**
     * Il giocatore ha finito le carte: esce dai turni, prende il prossimo posto
     * in classifica e si chiede alle regole se la partita è finita.
     */
    private void registraTerminato(Giocatore giocatore) {
        giocatore.segnaComeTerminato();
        terminati.add(giocatore);
        registraEvento(EventoPartita.giocatoreTerminato(giocatore, terminati.size()));
        verificaFinePartita();
    }

    /**
     * Chiede alle regole se la partita è finita e, in quel caso, la conclude.
     * È chiamato ogni volta che un giocatore esce di scena.
     */
    private void verificaFinePartita() {
        if (regole.isPartitaTerminata(this)) {
            concludiPartita();
        }
    }

    /** Stila la classifica (ordine descritto in {@link #getClassifica()}) e chiude la partita. */
    private void concludiPartita() {
        List<Giocatore> risultato = new ArrayList<>(terminati);
        giocatori.stream()
                .filter(Giocatore::isInGioco)
                .sorted(Comparator.comparingInt(g -> g.getMano().size()))
                .forEach(risultato::add);
        List<Giocatore> eliminatiDalPiuRecente = new ArrayList<>(eliminati);
        Collections.reverse(eliminatiDalPiuRecente);
        risultato.addAll(eliminatiDalPiuRecente);

        classifica = List.copyOf(risultato);
        azioneInAttesa = null;
        accumulo = null;
        cartaAppenaPescata = null;
        fase = FaseTurno.TERMINATA;
    }

    /**
     * Chiude il turno del giocatore attivo: azzera i dati del turno e passa al
     * giocatore successivo.
     *
     * <p>È l'unico punto che decide la fase del nuovo turno: se c'è un accumulo
     * dello stacking il nuovo giocatore deve rispondere
     * ({@link FaseTurno#RISPOSTA_STACKING}), altrimenti il turno è
     * {@link FaseTurno#NORMALE}. Così l'accumulo "passa al successivo" senza codice
     * dedicato, qualunque sia il motivo per cui il turno si chiude (giocata, UNO,
     * vittoria in Endless, eliminazione, carta iniziale).</p>
     */
    private void chiudiTurno() {
        if (fase == FaseTurno.TERMINATA) {
            return;
        }
        // Step 6: le passive temporanee (Arcobaleno) valgono solo nel turno in cui si
        // ottengono; la ricarica dell'abilità scende all'inizio di ogni turno giocato.
        getGiocatoreAttivo().terminaTurno();
        cartaAppenaPescata = null;
        fase = accumulo != null ? FaseTurno.RISPOSTA_STACKING : FaseTurno.NORMALE;
        aggiornaGiocatoreAttivo();
        getGiocatoreAttivo().iniziaTurno();
    }

    /**
     * Avanza al giocatore successivo. Se un effetto ha richiesto di saltare il
     * prossimo turno, consuma il flag, registra l'evento e avanza una seconda volta.
     * È privato: l'avanzamento passa sempre da un comando che ha già controllato
     * la fase.
     */
    private void aggiornaGiocatoreAttivo() {
        if (turniExtra > 0) {
            if (getGiocatoreAttivo().isInGioco()) {
                turniExtra--;
                registraEvento(EventoPartita.turnoExtra(getGiocatoreAttivo()));
                return;
            }
            // Chi aveva il turno extra è uscito (ha finito le carte o è eliminato).
            turniExtra = 0;
        }
        avanzaGiocatore();
        if (saltaProssimoTurno) {
            saltaProssimoTurno = false;
            registraEvento(EventoPartita.turnoSaltato(getGiocatoreAttivo()));
            avanzaGiocatore();
        }
    }

    /**
     * Passa il turno al prossimo giocatore in gioco secondo il senso di gioco. Se
     * tocca a un giocatore congelato (Step 6), il suo turno passa: si scala un turno di
     * congelamento, si registra l'evento e si va avanti. È l'unico punto in cui il
     * congelamento si consuma, perché è l'unico in cui un turno passa davvero.
     */
    private void avanzaGiocatore() {
        indiceGiocatoreAttivo = trovaProssimoIndice(indiceGiocatoreAttivo, Giocatore::isInGioco);
        while (getGiocatoreAttivo().isCongelato()) {
            Giocatore congelato = getGiocatoreAttivo();
            congelato.consumaTurnoCongelato();
            registraEvento(EventoPartita.turnoCongelato(congelato));
            indiceGiocatoreAttivo = trovaProssimoIndice(indiceGiocatoreAttivo, Giocatore::isInGioco);
        }
    }

    /**
     * Unico punto che calcola "chi viene dopo": parte da {@code daIndice} e avanza
     * nel senso di gioco fino al primo giocatore che soddisfa il criterio. Prima
     * {@code avanzaGiocatore} e {@code getProssimoGiocatore} usavano ciascuno
     * {@code (i ± 1) % n}, che dava per scontato che nessuno uscisse dalla partita.
     *
     * <p>Il criterio è {@link Giocatore#isInGioco()} per l'avanzamento vero (che poi
     * consuma i turni dei congelati) e {@link Giocatore#partecipaAiTurni()} per sapere
     * chi giocherà davvero dopo (Step 6: il congelato non risponde allo stacking e non
     * subisce il +2). Fino allo Step 5 si chiamava {@code trovaProssimoIndiceInGioco}
     * e il criterio era fisso.</p>
     *
     * <p>Il giro arriva fino a {@code daIndice} compreso: se nessun altro soddisfa il
     * criterio ma {@code daIndice} sì, il prossimo è lui stesso. Se nessuno lo soddisfa
     * restituisce {@code daIndice}: succede solo a partita finita.</p>
     */
    private int trovaProssimoIndice(int daIndice, Predicate<Giocatore> criterio) {
        int numeroGiocatori = giocatori.size();
        int passo = sensoOrario ? 1 : -1;
        int indice = daIndice;
        for (int tentativi = 0; tentativi < numeroGiocatori; tentativi++) {
            indice = Math.floorMod(indice + passo, numeroGiocatori);
            if (criterio.test(giocatori.get(indice))) {
                return indice;
            }
        }
        return daIndice;
    }
}
