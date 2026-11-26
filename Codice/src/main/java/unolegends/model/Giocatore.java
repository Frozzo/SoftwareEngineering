package unolegends.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * GRASP Information Expert: gestisce e conosce la propria mano e il proprio
 * {@link StatoGiocatore}.
 *
 * <p>Lo stato lo cambia solo {@link Partita} (metodi package-private), perché
 * l'uscita di un giocatore ha conseguenze sul turno e sulla classifica che solo
 * il Coordinator conosce.</p>
 */
public class Giocatore {
    private final String nome;
    private final List<Carta> mano;
    private StatoGiocatore stato = StatoGiocatore.IN_GIOCO;

    public Giocatore(String nome, List<Carta> manoIniziale) {
        this.nome = Objects.requireNonNull(nome, "nome non puo essere null");
        this.mano = new ArrayList<>(Objects.requireNonNull(manoIniziale, "manoIniziale non puo essere null"));
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
