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
    private Giocatore giocatoreInAttesaDichiarazioneUno;
    private boolean giocoIniziato;
    private boolean saltaProssimoTurno;
    private Colore coloreAttuale;
    private boolean sensoOrario = true; // Variabile per tenere traccia del senso di gioco

    public Partita(List<Giocatore> giocatori, Mazzo mazzo, PilaDegliScarti pilaDegliScarti, int indiceGiocatoreAttivo, RegoleDiGioco regole) {
        this.giocatori = new ArrayList<>(giocatori);
        this.mazzo = mazzo;
        this.pilaDegliScarti = pilaDegliScarti;
        this.regole = regole;
        this.indiceGiocatoreAttivo = indiceGiocatoreAttivo;
        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        this.coloreAttuale = cartaInCima == null ? null : cartaInCima.getColore();
        if (cartaInCima != null && cartaInCima.getEffetto().getRichiestaInput() != null) {
            this.effettoInizialeInAttesa = cartaInCima.getEffetto();
        }
    }
    /**
     * Completa l'inizializzazione della partita applicando l'effetto della prima carta.
     * È idempotente per consentire al controller di invocarlo senza duplicare effetti.
     */
    public void iniziaGioco() {
        if (giocoIniziato) {
            return;
        }
        this.giocoIniziato = true;
        if (effettoInizialeInAttesa == null) {
            Carta cartaInCima = pilaDegliScarti.getCartaInCima();
            if (cartaInCima != null) {
                cartaInCima.getEffetto().attivaEffetto(this);
                aggiornaGiocatoreAttivo();
            }
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

    public PilaDegliScarti getPilaDegliScarti() {
        return pilaDegliScarti;
    }
    /**
     * GRASP Coordinator: valida la mossa e orchestra estrazione carta + inserimento negli scarti.
     */
    public EsitoGiocata giocaCarta(int indiceCarta, String rispostaInput) {
        if (vincitore != null || effettoInizialeInAttesa != null
                || giocatoreInAttesaDichiarazioneUno != null) {
            return EsitoGiocata.completata(false);
        }
        Giocatore giocatoreAttivo = getGiocatoreAttivo();
        boolean avevaDueCarte = giocatoreAttivo.getMano().size() == 2;
        Carta cartaSelezionata = giocatoreAttivo.getCartaInPosizione(indiceCarta);
        if (cartaSelezionata == null) {
            return EsitoGiocata.completata(false);
        }

        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        if (!regole.isGiocabile(cartaSelezionata, cartaInCima, this)) {
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
        pilaDegliScarti.aggiungiCarta(cartaDaGiocare);
        
        if (cartaDaGiocare.getColore() != Colore.NERO) {
            coloreAttuale = cartaDaGiocare.getColore();
        }
        cartaAppenaPescata = null;
        cartaPescataDaGiocare = false;
        effetto.attivaEffetto(this, rispostaInput);
        if (regole.isVincitore(giocatoreAttivo, this)) {
            vincitore = giocatoreAttivo;
            return EsitoGiocata.completata(true);
        }
        boolean richiedeDichiarazioneUno = avevaDueCarte
                && giocatoreAttivo.getMano().size() == 1;
        if (richiedeDichiarazioneUno) {
            giocatoreInAttesaDichiarazioneUno = giocatoreAttivo;
            return EsitoGiocata.richiedeDichiarazioneUno();
        }
        aggiornaGiocatoreAttivo();
        return EsitoGiocata.completata(true);
    }

    /**
     * Completa la finestra di dichiarazione UNO dopo una giocata valida.
     *
     * @return true se e stata applicata la penalita per mancata dichiarazione
     */
    public boolean completaDichiarazioneUno(boolean unoDichiarato) {
        if (giocatoreInAttesaDichiarazioneUno == null) {
            return false;
        }
        Giocatore giocatore = giocatoreInAttesaDichiarazioneUno;
        giocatoreInAttesaDichiarazioneUno = null;
        boolean mancatoUno = regole.mancatoUno(giocatore, unoDichiarato, this, regole);
        aggiornaGiocatoreAttivo();
        return mancatoUno;
    }

    public boolean rispondiRichiestaInputIniziale(String rispostaInput) {
        if (effettoInizialeInAttesa == null || !effettoInizialeInAttesa.validaRispostaInput(rispostaInput)) {
            return false;
        }

        Ieffetto effetto = effettoInizialeInAttesa;
        effettoInizialeInAttesa = null;
        effetto.attivaEffetto(this, rispostaInput);
        aggiornaGiocatoreAttivo();
        return true;
    }

    void applicaColoreScelto(Colore coloreScelto) {
        coloreAttuale = Objects.requireNonNull(coloreScelto, "coloreScelto non puo essere null");
    }

    public Colore getColoreAttuale() {
        return coloreAttuale;
    }

    void segnalaSaltoTurno() {
        saltaProssimoTurno = true;
    }

    public boolean pescaCarta() {
        if (vincitore != null || cartaPescataDaGiocare
                || giocatoreInAttesaDichiarazioneUno != null) {
            return false;
        }

        Carta cartaPescata = pescaSingola(getGiocatoreAttivo());
        cartaAppenaPescata = cartaPescata;
        cartaPescataDaGiocare = true;
        return true;
    }

    /**
     * Pesca una carta per il giocatore e gestisce il rimescolamento degli scarti.
     * Non modifica lo stato del turno o della carta appena pescata.
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

    public boolean pescaCartaForzata(Giocatore giocatore) {
        Objects.requireNonNull(giocatore, "giocatore non puo essere null");
        return !regole.pesca(giocatore, this).isEmpty();
    }

    /**
     * GRASP Coordinator: gestisce il cambio turno tramite self-message dedicato.
     */
    public boolean passaTurno() {
        if (vincitore != null || !cartaPescataDaGiocare
                || giocatoreInAttesaDichiarazioneUno != null) {
            return false;
        }

        cartaAppenaPescata = null;
        cartaPescataDaGiocare = false;
        aggiornaGiocatoreAttivo();
        return true;
    }

    public void aggiornaGiocatoreAttivo() {
        avanzaGiocatore();
        if (saltaProssimoTurno) {
            saltaProssimoTurno = false;
            avanzaGiocatore();
        }
    }

    private void avanzaGiocatore() {
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
