package unolegends.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * GRASP Information Expert: gestisce e conosce la propria mano, il proprio
 * {@link StatoGiocatore} e, dallo Step 6, il proprio {@link Legend}.
 *
 * <p>Lo stato lo cambia solo {@link Partita} (metodi package-private), perché
 * l'uscita di un giocatore ha conseguenze sul turno e sulla classifica che solo
 * il Coordinator conosce.</p>
 *
 * <p><b>Legend (Step 6).</b> Il giocatore conosce le sue passive e la sua abilità
 * attiva, quindi è lui a rispondere alle domande che dipendono da quelle
 * ({@link #getPassive()}, {@link #isBersagliabile()}). Chi gioca senza Legend ha
 * una lista di passive vuota, che fa da Null Object: chi interroga le passive non
 * deve controllare se il Legend c'è. L'abilità attiva invece è {@code null}: "non ha
 * un'abilità" non è un comportamento da neutralizzare ma un comando da rifiutare,
 * e lo fa {@link Partita#usaAbilita()}.</p>
 *
 * <p><b>Congelamento.</b> {@link #turniCongelati} conta i turni che il giocatore
 * deve ancora saltare (abilità Congela del Freezer). Non è un valore di
 * {@link StatoGiocatore}: il congelato è ancora in gioco (conta per la fine della
 * partita e per la classifica) ed esce dai turni solo per un po', quindi serve un
 * contatore e non uno stato.</p>
 */
public class Giocatore {
    private final String nome;
    private final List<Carta> mano;
    private StatoGiocatore stato = StatoGiocatore.IN_GIOCO;
    /** Legend scelto nel menu, oppure {@code null} se la partita è senza Legend. */
    private final Legend legend;
    /** Passiva del Legend (vuota senza Legend): non cambia per tutta la partita. */
    private final List<IPassiva> passive;
    /** Passive che valgono fino alla fine del turno (Arcobaleno). */
    private final List<IPassiva> passiveTemporanee = new ArrayList<>();
    /** Abilità attiva del Legend con la sua ricarica, oppure {@code null} senza Legend. */
    private final AbilitaAttiva abilitaAttiva;
    private int turniCongelati;

    public Giocatore(String nome, List<Carta> manoIniziale) {
        this(nome, manoIniziale, null);
    }

    /**
     * @param legend Legend scelto, oppure {@code null} per giocare senza
     */
    public Giocatore(String nome, List<Carta> manoIniziale, Legend legend) {
        this.nome = Objects.requireNonNull(nome, "nome non puo essere null");
        this.mano = new ArrayList<>(Objects.requireNonNull(manoIniziale, "manoIniziale non puo essere null"));
        this.legend = legend;
        this.passive = legend == null ? List.of() : List.of(legend.getPassiva());
        this.abilitaAttiva = legend == null ? null : legend.creaAbilitaAttiva();
    }

    /** Legend del giocatore, oppure {@code null} se la partita è senza Legend. */
    public Legend getLegend() {
        return legend;
    }

    /** Abilità attiva con la sua ricarica, oppure {@code null} se il giocatore non ha un Legend. */
    public AbilitaAttiva getAbilitaAttiva() {
        return abilitaAttiva;
    }

    /**
     * Tutte le passive che valgono adesso: quella del Legend più quelle temporanee.
     * Le regole le interrogano tutte, senza sapere quali sono.
     */
    List<IPassiva> getPassive() {
        if (passiveTemporanee.isEmpty()) {
            return passive;
        }
        List<IPassiva> tutte = new ArrayList<>(passive);
        tutte.addAll(passiveTemporanee);
        return tutte;
    }

    /** Aggiunge una passiva che vale fino alla fine del turno (Arcobaleno). */
    void aggiungiPassivaTemporanea(IPassiva passiva) {
        passiveTemporanee.add(Objects.requireNonNull(passiva, "passiva non puo essere null"));
    }

    /**
     * Si può scegliere come bersaglio degli effetti con scelta del giocatore? No se è
     * congelato (è "come se non ci fosse") o se una passiva lo protegge (Ghost).
     * Che sia in gioco e che non sia chi sceglie lo controlla Partita.
     */
    public boolean isBersagliabile() {
        return !isCongelato() && getPassive().stream().allMatch(IPassiva::isBersagliabile);
    }

    /** true se la carta Blocca giocata contro di lui non ha effetto (Pelle di ghiaccio). */
    boolean isImmuneAlBlocca() {
        return getPassive().stream().anyMatch(IPassiva::isImmuneAlBlocca);
    }

    /** true se deve ancora saltare almeno un turno per il congelamento. */
    public boolean isCongelato() {
        return turniCongelati > 0;
    }

    /** Partecipa ai turni: è in gioco e non è congelato. */
    boolean partecipaAiTurni() {
        return isInGioco() && !isCongelato();
    }

    /** Il giocatore salterà i suoi prossimi {@code turni} turni (Congela). */
    void congela(int turni) {
        turniCongelati += turni;
    }

    /** Il turno del congelato è passato: un turno di congelamento in meno. */
    void consumaTurnoCongelato() {
        if (turniCongelati > 0) {
            turniCongelati--;
        }
    }

    /** Inizio di un turno giocato davvero: la ricarica dell'abilità scende di 1. */
    void iniziaTurno() {
        if (abilitaAttiva != null) {
            abilitaAttiva.scalaRicarica();
        }
    }

    /** Fine del turno: le passive temporanee (Arcobaleno) smettono di valere. */
    void terminaTurno() {
        passiveTemporanee.clear();
    }

    public String getNome() {
        return nome;
    }

    public StatoGiocatore getStato() {
        return stato;
    }

    /** true se il giocatore partecipa ancora ai turni. */
    public boolean isInGioco() {
        return stato == StatoGiocatore.IN_GIOCO;
    }

    void segnaComeTerminato() {
        stato = StatoGiocatore.TERMINATO;
    }

    void segnaComeEliminato() {
        stato = StatoGiocatore.ELIMINATO;
    }

    /**
     * Toglie tutte le carte dalla mano e le restituisce. Serve all'eliminazione
     * (le carte dell'eliminato tornano subito nel mazzo) e agli scambi di mano.
     */
    List<Carta> svuotaMano() {
        List<Carta> carte = new ArrayList<>(mano);
        mano.clear();
        return carte;
    }

    /**
     * Aggiunge alla mano un gruppo di carte. Serve agli effetti che spostano mani
     * intere (carta 7 e carta 0 della No Mercy), insieme a {@link #svuotaMano()}.
     */
    void riceviCarte(List<Carta> carte) {
        mano.addAll(Objects.requireNonNull(carte, "carte non puo essere null"));
    }

    public List<Carta> getMano() {
        return Collections.unmodifiableList(mano);
    }

    public Carta getCartaInPosizione(int indiceCarta) {
        if (indiceCarta < 0 || indiceCarta >= mano.size()) {
            return null;
        }
        return mano.get(indiceCarta);
    }

    public Carta estraiCarta(int indiceCarta) {
        if (indiceCarta < 0 || indiceCarta >= mano.size()) {
            return null;
        }
        return mano.remove(indiceCarta);
    }

    /**
     * Toglie dalla mano proprio la carta indicata (confronto per identità, non per
     * valore: due "5 Rosso" sono carte diverse). Serve quando una giocata viene
     * completata in un secondo momento, dopo una risposta del giocatore: si
     * conserva il riferimento alla carta invece dell'indice.
     *
     * @return true se la carta era nella mano ed è stata tolta
     */
    public boolean rimuoviCarta(Carta carta) {
        for (int i = 0; i < mano.size(); i++) {
            if (mano.get(i) == carta) {
                mano.remove(i);
                return true;
            }
        }
        return false;
    }

    public void aggiungiCarta(Carta carta) {
        mano.add(Objects.requireNonNull(carta, "carta non puo essere null"));
    }
}
