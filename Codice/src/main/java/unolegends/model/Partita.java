package unolegends.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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
                chiudiTurno();
            });
            return;
        }
        effetto.attivaEffetto(this);
        aggiornaGiocatoreAttivo();
    }

    private Giocatore getGiocatoreAttivo() {
        return giocatori.get(indiceGiocatoreAttivo);
    }

    public String getNomeGiocatoreAttivo() {
        return getGiocatoreAttivo().getNome();
    }

    public List<Carta> getManoGiocatoreAttivo() {
        return getGiocatoreAttivo().getMano();
    }

    /** Fase corrente del turno: è l'unica informazione che la UI deve leggere per sapere cosa chiedere. */
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
     * Avversari del giocatore attivo che sono ancora in gioco. Servirà agli
     * effetti con scelta del bersaglio (carta 7, abilità dei Legend).
     */
    public List<Giocatore> getAvversariInGioco() {
        Giocatore attivo = getGiocatoreAttivo();
        return giocatori.stream().filter(g -> g != attivo && g.isInGioco()).toList();
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
        if (fase != FaseTurno.NORMALE && fase != FaseTurno.POST_PESCA) {
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
        List<Carta> cartePescate = regole.pescaVolontaria(giocatore, this);
        registraEvento(EventoPartita.pescaVolontaria(giocatore, cartePescate.size()));
        cartaAppenaPescata = cartePescate.isEmpty() ? null : cartePescate.get(cartePescate.size() - 1);
        fase = FaseTurno.POST_PESCA;
        return true;
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
    }

    /**
     * Calcola il prossimo giocatore in gioco rispettando il senso corrente, senza
     * modificare l'indice del giocatore attivo. È usato dagli effetti che
     * agiscono sul giocatore successivo, come la pescata forzata.
     */
    public Giocatore getProssimoGiocatore() {
        return giocatori.get(trovaProssimoIndiceInGioco(indiceGiocatoreAttivo));
    }

    /**
     * Elimina un giocatore: le sue carte tornano subito nel mazzo, che viene
     * rimescolato (decisione del progetto), e il giocatore finisce in fondo alla
     * classifica. Se resta un solo giocatore in gioco la partita termina.
     *
     * <p>Package-private e preparato per lo Step 4: con le regole attuali
     * ({@code StrategiaSconfittaStandard}) nessuno viene eliminato. Lo Step 4
     * aggiungerà il controllo dopo ogni pesca e gestirà i casi in cui
     * l'eliminato è il giocatore di turno o quello che doveva saltare il turno.</p>
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
        boolean avevaDueCarte = giocatoreAttivo.getMano().size() == 2;
        giocatoreAttivo.rimuoviCarta(carta);
        pilaDegliScarti.aggiungiCarta(carta);

        // Le carte colorate aggiornano subito il colore attivo;
        // per una carta nera è l'effetto a impostare il colore scelto.
        if (carta.getColore() != Colore.NERO) {
            coloreAttuale = carta.getColore();
        }
        cartaAppenaPescata = null;
        carta.getEffetto().attivaEffetto(this, risposta);

        if (regole.isVincitore(giocatoreAttivo, this)) {
            registraTerminato(giocatoreAttivo);
            // In Endless la partita può continuare: il turno passa al prossimo in gioco.
            if (fase != FaseTurno.TERMINATA) {
                chiudiTurno();
            }
            return;
        }
        // Con una sola carta in mano, il turno resta al giocatore finché non dichiara UNO.
        if (avevaDueCarte && giocatoreAttivo.getMano().size() == 1) {
            fase = FaseTurno.ATTESA_UNO;
            return;
        }
        chiudiTurno();
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
        cartaAppenaPescata = null;
        fase = FaseTurno.TERMINATA;
    }

    /**
     * Chiude il turno del giocatore attivo: azzera i dati del turno, riporta la
     * fase a {@link FaseTurno#NORMALE} e passa al giocatore successivo.
     */
    private void chiudiTurno() {
        cartaAppenaPescata = null;
        fase = FaseTurno.NORMALE;
        aggiornaGiocatoreAttivo();
    }

    /**
     * Avanza al giocatore successivo. Se un effetto ha richiesto di saltare il
     * prossimo turno, consuma il flag, registra l'evento e avanza una seconda volta.
     * È privato: l'avanzamento passa sempre da un comando che ha già controllato
     * la fase.
     */
    private void aggiornaGiocatoreAttivo() {
        avanzaGiocatore();
        if (saltaProssimoTurno) {
            saltaProssimoTurno = false;
            registraEvento(EventoPartita.turnoSaltato(getGiocatoreAttivo()));
            avanzaGiocatore();
        }
    }

    /** Passa il turno al prossimo giocatore in gioco secondo il senso di gioco. */
    private void avanzaGiocatore() {
        indiceGiocatoreAttivo = trovaProssimoIndiceInGioco(indiceGiocatoreAttivo);
    }

    /**
     * Unico punto che calcola "chi viene dopo": parte da {@code daIndice} e
     * avanza nel senso di gioco saltando i giocatori terminati o eliminati. Prima
     * {@code avanzaGiocatore} e {@code getProssimoGiocatore} usavano ciascuno
     * {@code (i ± 1) % n}, che dava per scontato che nessuno uscisse dalla partita.
     *
     * <p>Se nessun altro è in gioco restituisce {@code daIndice}: succede solo a
     * partita finita.</p>
     */
    private int trovaProssimoIndiceInGioco(int daIndice) {
        int numeroGiocatori = giocatori.size();
        int passo = sensoOrario ? 1 : -1;
        int indice = daIndice;
        for (int tentativi = 0; tentativi < numeroGiocatori; tentativi++) {
            indice = Math.floorMod(indice + passo, numeroGiocatori);
            if (giocatori.get(indice).isInGioco()) {
                return indice;
            }
        }
        return daIndice;
    }
}
