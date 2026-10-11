package unolegends.model;

import java.util.ArrayList;
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
    private boolean deveScegliereColoreIniziale;
    private Colore coloreScelto;
    private Giocatore vincitore;
    private boolean sensoOrario = true; // Variabile per tenere traccia del senso di gioco
    public Partita(List<Giocatore> giocatori, Mazzo mazzo, PilaDegliScarti pilaDegliScarti, int indiceGiocatoreAttivo) {
        this(giocatori, mazzo, pilaDegliScarti, indiceGiocatoreAttivo, RegoleDiGioco.standard());
    }

    public Partita(List<Giocatore> giocatori, Mazzo mazzo, PilaDegliScarti pilaDegliScarti, int indiceGiocatoreAttivo, RegoleDiGioco regole) {
        this.giocatori = new ArrayList<>(Objects.requireNonNull(giocatori, "giocatori non puo essere null"));
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
        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        this.deveScegliereColoreIniziale = cartaInCima != null
                && EffettoComposito.contieneEffetto(cartaInCima.getEffetto(), Effetto_Cambia_colore.class);
    }

    /**
     * GRASP Coordinator: aggrega stato turno interrogando gli Information Expert.
     */
    public StatoTurno getStatoTurno() {
        Giocatore attivo = getGiocatoreAttivo();
        return new StatoTurno(attivo.getNome(), attivo.getMano(), pilaDegliScarti.getCartaInCima(),
                cartaPescataDaGiocare, cartaAppenaPescata, deveScegliereColoreIniziale,
                vincitore != null, vincitore == null ? null : vincitore.getNome());
    }
    public int getIndiceGiocatoreAttivo() {
        return indiceGiocatoreAttivo;
    }
    public List<Carta> getMazzo() {
        return mazzo.getCarte();
    }

    public int getNumeroCarteNelMazzo() {
        return mazzo.getCarte().size();
    }

    public boolean cartaRichiedeSceltaColore(int indiceCarta) {
        if (vincitore != null) {
            return false;
        }
        Carta carta = getGiocatoreAttivo().getCartaInPosizione(indiceCarta);
        return carta != null && EffettoComposito.contieneEffetto(
                carta.getEffetto(), Effetto_Cambia_colore.class);
    }

    public Mazzo getMazzoDaPesca() {
        return mazzo;
    }

    public PilaDegliScarti getPilaDegliScarti() {
        return pilaDegliScarti;
    }

    /**
     * GRASP Coordinator: valida la mossa e orchestra estrazione carta + inserimento negli scarti.
     */
    public boolean giocaCarta(int indiceCarta, boolean unoDichiarato) {
        if (vincitore != null || deveScegliereColoreIniziale) {
            return false;
        }

        Giocatore giocatoreAttivo = getGiocatoreAttivo();
        boolean penultimaCartaGiocata = giocatoreAttivo.getMano().size() == 2;
        Carta cartaSelezionata = giocatoreAttivo.getCartaInPosizione(indiceCarta);
        if (cartaSelezionata == null) {
            coloreScelto = null;
            return false;
        }

        boolean richiedeSceltaColore = EffettoComposito.contieneEffetto(
                cartaSelezionata.getEffetto(), Effetto_Cambia_colore.class);
        if (richiedeSceltaColore && coloreScelto == null) {
            return false;
        }
        if (!richiedeSceltaColore) {
            coloreScelto = null;
        }

        if (cartaPescataDaGiocare && cartaSelezionata != cartaAppenaPescata) {
            coloreScelto = null;
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
        cartaDaGiocare.getEffetto().attivaeffetto(this);
        if (penultimaCartaGiocata && !unoDichiarato) {
            regole.applicaPenalitaMancatoUno(giocatoreAttivo, this);
        }
        if (regole.isVincitore(giocatoreAttivo, this)) {
            vincitore = giocatoreAttivo;
            return true;
        }
        aggiornaGiocatoreAttivo();
        return true;
    }

    public boolean scegliColore(Colore colore) {
        if (vincitore != null || colore == null || colore == Colore.NERO) {
            return false;
        }

        if (deveScegliereColoreIniziale) {
            coloreScelto = colore;
            pilaDegliScarti.getCartaInCima().getEffetto().attivaeffetto(this);
            deveScegliereColoreIniziale = false;
            return true;
        }

        coloreScelto = colore;
        return true;
    }

    void applicaColoreScelto() {
        if (coloreScelto == null) {
            throw new IllegalStateException("Il colore deve essere scelto prima di attivare l'effetto");
        }
        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        if (!(cartaInCima instanceof CartaSpeciale cartaSpeciale)) {
            throw new IllegalStateException("La carta in cima agli scarti non supporta il cambio colore");
        }
        cartaSpeciale.setColore(coloreScelto);
        coloreScelto = null;
    }

    /**
     * GRASP Coordinator: richiede al Mazzo una carta e delega al Giocatore l'aggiornamento della mano.
     */
    public boolean pescaCarta() {
        if (vincitore != null || cartaPescataDaGiocare) {
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
    public boolean pescaCartaForzata(Giocatore giocatore) {
        Objects.requireNonNull(giocatore, "giocatore non puo essere null");
        return !regole.pesca(giocatore, this).isEmpty();
    }

    /**
     * GRASP Coordinator: gestisce il cambio turno tramite self-message dedicato.
     */
    public boolean passaTurno() {
        if (vincitore != null || !cartaPescataDaGiocare) {
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

    public boolean tuttiGliAltriGiocatoriHannoPiuDi(Giocatore giocatore, int sogliaCarte) {
        if (giocatore == null || sogliaCarte < 0) {
            return false;
        }

        boolean trovatoAvversario = false;
        for (Giocatore altroGiocatore : giocatori) {
            if (altroGiocatore != giocatore) {
                trovatoAvversario = true;
                if (altroGiocatore.getMano().size() <= sogliaCarte) {
                    return false;
                }
            }
        }
        return trovatoAvversario;
    }

    private Giocatore getGiocatoreAttivo() {
        return giocatori.get(indiceGiocatoreAttivo);
    }

    public void aggiornaGiocatoreAttivo() {
        if (sensoOrario) {
            indiceGiocatoreAttivo = (indiceGiocatoreAttivo + 1) % giocatori.size();
        } else {
            indiceGiocatoreAttivo = (indiceGiocatoreAttivo - 1 + giocatori.size()) % giocatori.size();
        }
    }
    public void cambioGiro(){
        this.sensoOrario = !sensoOrario; // Inverte il senso di gioco
    }
    public Giocatore getProssimoGiocatore() {
        if (sensoOrario) {
            return giocatori.get((indiceGiocatoreAttivo + 1) % giocatori.size());
        } else {
            return giocatori.get((indiceGiocatoreAttivo - 1 + giocatori.size()) % giocatori.size());
        }
    }
}
