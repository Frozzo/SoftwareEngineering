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
    private Ieffetto effettoInizialeInAttesa;
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
        if (cartaInCima != null && cartaInCima.getEffetto().getRichiestaInput() != null) {
            this.effettoInizialeInAttesa = cartaInCima.getEffetto();
        }
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

    public boolean isCartaPescataDaGiocare() {
        return cartaPescataDaGiocare;
    }

    public Carta getCartaAppenaPescata() {
        return cartaAppenaPescata;
    }

    public RichiestaInput getRichiestaInputIniziale() {
        return effettoInizialeInAttesa == null ? null : effettoInizialeInAttesa.getRichiestaInput();
    }

    public boolean isPartitaTerminata() {
        return vincitore != null;
    }

    public String getNomeVincitore() {
        return vincitore == null ? null : vincitore.getNome();
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

    public Mazzo getMazzoDaPesca() {
        return mazzo;
    }

    public PilaDegliScarti getPilaDegliScarti() {
        return pilaDegliScarti;
    }

    /**
     * GRASP Coordinator: valida la mossa e orchestra estrazione carta + inserimento negli scarti.
     */
    public EsitoGiocata giocaCarta(int indiceCarta, boolean unoDichiarato, String rispostaInput) {
        if (vincitore != null || effettoInizialeInAttesa != null) {
            return EsitoGiocata.completata(false);
        }

        Giocatore giocatoreAttivo = getGiocatoreAttivo();
        boolean penultimaCartaGiocata = giocatoreAttivo.getMano().size() == 2;
        Carta cartaSelezionata = giocatoreAttivo.getCartaInPosizione(indiceCarta);
        if (cartaSelezionata == null) {
            return EsitoGiocata.completata(false);
        }

        if (cartaPescataDaGiocare && cartaSelezionata != cartaAppenaPescata) {
            return EsitoGiocata.completata(false);
        }

        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        if (cartaInCima != null && !regole.isGiocabile(cartaSelezionata, cartaInCima)) {
            return EsitoGiocata.completata(false);
        }

        Ieffetto effetto = cartaSelezionata.getEffetto();
        RichiestaInput richiesta = effetto.getRichiestaInput();
        if (richiesta != null && rispostaInput == null) {
            return EsitoGiocata.inAttesa(richiesta);
        }
        if (!effetto.validaRispostaInput(rispostaInput)) {
            return richiesta == null
                    ? EsitoGiocata.completata(false)
                    : EsitoGiocata.inAttesa(richiesta);
        }

        Carta cartaDaGiocare = giocatoreAttivo.estraiCarta(indiceCarta);
        if (cartaDaGiocare == null) {
            return EsitoGiocata.completata(false);
        }

        pilaDegliScarti.aggiungiCarta(cartaDaGiocare);
        cartaAppenaPescata = null;
        cartaPescataDaGiocare = false;
        effetto.attivaeffetto(this, rispostaInput);
        if (penultimaCartaGiocata && !unoDichiarato) {
            regole.applicaPenalitaMancatoUno(giocatoreAttivo, this);
        }
        if (regole.isVincitore(giocatoreAttivo, this)) {
            vincitore = giocatoreAttivo;
            return EsitoGiocata.completata(true);
        }
        aggiornaGiocatoreAttivo();
        return EsitoGiocata.completata(true);
    }

    public boolean rispondiRichiestaInputIniziale(String rispostaInput) {
        if (vincitore != null || effettoInizialeInAttesa == null
                || !effettoInizialeInAttesa.validaRispostaInput(rispostaInput)) {
            return false;
        }

        Ieffetto effetto = effettoInizialeInAttesa;
        effettoInizialeInAttesa = null;
        effetto.attivaeffetto(this, rispostaInput);
        return true;
    }

    void applicaColoreScelto(Colore coloreScelto) {
        Objects.requireNonNull(coloreScelto, "coloreScelto non puo essere null");
        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        if (!(cartaInCima instanceof CartaSpeciale cartaSpeciale)) { 
            throw new IllegalStateException("La carta in cima agli scarti non supporta il cambio colore");
        }
        cartaSpeciale.setColore(coloreScelto);
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
