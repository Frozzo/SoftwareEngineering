package unolegends;

import java.util.List;
import java.util.Objects;

/**
 * GRASP Coordinator / oggetto radice del dominio: coordina il turno delegando agli expert.
 */
public class Partita {
    private final List<Giocatore> giocatori;
    private final Mazzo mazzo;
    private final PilaDegliScarti pilaDegliScarti;
    private final RegoleDiGioco regole;
    private int indiceGiocatoreAttivo;
    private Carta cartaAppenaPescata;
    private boolean cartaPescataDaGiocare;
    public Partita(List<Giocatore> giocatori, Mazzo mazzo, PilaDegliScarti pilaDegliScarti, int indiceGiocatoreAttivo) {
        this(giocatori, mazzo, pilaDegliScarti, indiceGiocatoreAttivo, new RegoleStandard());
    }

    public Partita(List<Giocatore> giocatori, Mazzo mazzo, PilaDegliScarti pilaDegliScarti, int indiceGiocatoreAttivo, RegoleDiGioco regole) {
        this.giocatori = Objects.requireNonNull(giocatori, "giocatori non puo essere null");
        this.mazzo = Objects.requireNonNull(mazzo, "mazzo non puo essere null");
        this.pilaDegliScarti = Objects.requireNonNull(pilaDegliScarti, "pilaDegliScarti non puo essere null");
        this.regole = Objects.requireNonNull(regole, "regole non puo essere null");
        if (giocatori.isEmpty()) {
            throw new IllegalArgumentException("La lista giocatori non puo essere vuota");
        }
        if (indiceGiocatoreAttivo < 0 || indiceGiocatoreAttivo >= giocatori.size()) {
            throw new IllegalArgumentException("indiceGiocatoreAttivo non valido");
        }
        this.indiceGiocatoreAttivo = indiceGiocatoreAttivo;
    }

    /**
     * GRASP Coordinator: aggrega stato turno interrogando gli Information Expert.
     */
    public StatoTurno getStatoTurno() {
        Giocatore attivo = getGiocatoreAttivo();
        return new StatoTurno(attivo.getNome(), attivo.getMano(), pilaDegliScarti.getCartaInCima(), cartaPescataDaGiocare, cartaAppenaPescata);
    }

    /**
     * GRASP Coordinator: valida la mossa e orchestra estrazione carta + inserimento negli scarti.
     */
    public boolean giocaCarta(int indiceCarta) {
        Giocatore giocatoreAttivo = getGiocatoreAttivo();
        Carta cartaSelezionata = giocatoreAttivo.getCartaInPosizione(indiceCarta);
        if (cartaSelezionata == null) {
            return false;
        }

        if (cartaPescataDaGiocare && cartaSelezionata != cartaAppenaPescata) {
            return false;
        }

        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        if (cartaInCima != null && !regole.isGiocabile(cartaSelezionata, cartaInCima, this)) {
            return false;
        }

        Carta cartaDaGiocare = giocatoreAttivo.estraiCarta(indiceCarta);
        if (cartaDaGiocare == null) {
            return false;
        }

        pilaDegliScarti.aggiungiCarta(cartaDaGiocare);
        cartaAppenaPescata = null;
        cartaPescataDaGiocare = false;
        aggiornaGiocatoreAttivo();
        return true;
    }

    /**
     * GRASP Coordinator: richiede al Mazzo una carta e delega al Giocatore l'aggiornamento della mano.
     */
    public boolean pescaCarta() {
        if (cartaPescataDaGiocare) {
            return false;
        }

        Carta cartaPescata = mazzo.prelevaCarta();
        if (cartaPescata == null) {
            return false;
        }

        getGiocatoreAttivo().aggiungiCarta(cartaPescata);
        cartaAppenaPescata = cartaPescata;
        cartaPescataDaGiocare = true;
        return true;
    }

    /**
     * GRASP Coordinator: gestisce il cambio turno tramite self-message dedicato.
     */
    public boolean passaTurno() {
        if (!cartaPescataDaGiocare) {
            return false;
        }

        cartaAppenaPescata = null;
        cartaPescataDaGiocare = false;
        aggiornaGiocatoreAttivo();
        return true;
    }

    /**
     * GRASP Coordinator: indica se il giocatore attivo deve ancora gestire la carta appena pescata.
     */
    public boolean deveGiocareCartaPescata() {
        return cartaPescataDaGiocare;
    }

    private Giocatore getGiocatoreAttivo() {
        return giocatori.get(indiceGiocatoreAttivo);
    }

    private void aggiornaGiocatoreAttivo() {
        indiceGiocatoreAttivo = (indiceGiocatoreAttivo + 1) % giocatori.size();
    }
}
