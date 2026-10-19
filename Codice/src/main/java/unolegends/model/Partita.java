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
    private boolean giocoIniziato;
    private boolean saltaProssimoTurno;
    private Colore coloreAttuale;
    private boolean sensoOrario = true; // Variabile per tenere traccia del senso di gioco

    /**
     * Costruisce lo stato iniziale della partita e ricava il colore attivo dalla
     * carta iniziale. Se il suo effetto richiede input, lo conserva in attesa:
     * non può essere applicato correttamente finché il giocatore non risponde.
     */
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
     * È idempotente per non applicare due volte lo stesso effetto. Se la carta
     * iniziale richiede input, ne rimanda l'applicazione a
     * {@link #rispondiRichiestaInputIniziale(String)}; altrimenti applica l'effetto
     * e aggiorna il giocatore attivo.
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

    public boolean richiedeDichiarazioneUno() {
        return getGiocatoreAttivo().getMano().size() == 2;
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

    public int getNumeroCarteNelMazzo() {
        return mazzo.getNumeroCarte();
    }

    public Mazzo getMazzoDaPesca() {
        return mazzo;
    }

    public PilaDegliScarti getPilaDegliScarti() {
        return pilaDegliScarti;
    }
    /**
     * Coordina una giocata nell'ordine necessario a non alterare lo stato per mosse
     * rifiutate: controlla prima partita, indice, compatibilità e input dell'effetto;
     * solo dopo rimuove la carta dalla mano, la sposta negli scarti e applica l'effetto.
     * Infine controlla la penalità UNO e la vittoria, avanzando il turno solo se la
     * partita continua. Restituisce un esito distinto anche quando serve un input,
     * così la UI può chiedere una risposta senza considerare conclusa la giocata.
     */
    public EsitoGiocata giocaCarta(int indiceCarta, boolean unoDichiarato, String rispostaInput) { 
        // Caso 1: la partita è già conclusa oppure l'effetto della carta iniziale
        // aspetta una risposta; in entrambi i casi non si può iniziare una giocata.
        if (vincitore != null || effettoInizialeInAttesa != null) {
            return EsitoGiocata.completata(false);
        }
        Giocatore giocatoreAttivo = getGiocatoreAttivo();
        Carta cartaSelezionata = giocatoreAttivo.getCartaInPosizione(indiceCarta);
        // Caso 2: l'indice è fuori dalla mano; non esiste una carta da giocare.
        if (cartaSelezionata == null) {
            return EsitoGiocata.completata(false);
        }

        Carta cartaInCima = pilaDegliScarti.getCartaInCima();
        // Caso 3: la carta non rispetta le regole di compatibilità; la mano resta invariata.
        if (!regole.isGiocabile(cartaSelezionata, cartaInCima, this)) {
            return EsitoGiocata.completata(false);
        }

        Ieffetto effetto = cartaSelezionata.getEffetto();
        RichiestaInput richiesta = effetto.getRichiestaInput();
        // Caso 4: l'effetto ha bisogno di una risposta (per esempio un colore),
        // ma questa è la prima chiamata; restituisce la richiesta senza giocare la carta.
        if (richiesta != null && rispostaInput == null) {
            return EsitoGiocata.inAttesa(richiesta);
        }
        // Caso 5: la risposta non rispetta l'effetto. Se è previsto un input,
        // lascia la giocata in attesa; altrimenti rifiuta la mossa.
        if (!effetto.validaRispostaInput(rispostaInput)) {
            return richiesta == null
                    ? EsitoGiocata.completata(false)
                    : EsitoGiocata.inAttesa(richiesta);
        }

        giocatoreAttivo.estraiCarta(indiceCarta);
        pilaDegliScarti.aggiungiCarta(cartaSelezionata);
        
        // Caso 6: le carte colorate aggiornano subito il colore attivo.
        // Una carta nera delegando all'effetto di impostare il colore scelto.
        if (cartaSelezionata.getColore() != Colore.NERO) { 
            coloreAttuale = cartaSelezionata.getColore();
        }
        cartaAppenaPescata = null;
        cartaPescataDaGiocare = false;
        effetto.attivaEffetto(this, rispostaInput);
        boolean MancatoUno = regole.mancatoUno(giocatoreAttivo, unoDichiarato, this, regole); //controlla sei il giocatore ha dichiarato uno e se è necessario applicare la penalità
        // Caso 7: registra il vincitore e non sposta il turno a un altro giocatore.
        if (regole.isVincitore(giocatoreAttivo, this)) {
            vincitore = giocatoreAttivo;
            return EsitoGiocata.completata(true, MancatoUno);
        }
        aggiornaGiocatoreAttivo();
        return EsitoGiocata.completata(true, MancatoUno);
    }

    /**
     * Completa l'effetto della carta iniziale, che non passa da una giocata
     * ordinaria. Rifiuta la risposta se non c'è un effetto in attesa o se non è
     * valida; in caso positivo consuma l'attesa, applica l'effetto e aggiorna il turno.
     */
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

    /**
     * Registra il colore scelto per un effetto di cambio colore; rifiuta null
     * perché lascerebbe la partita senza un colore valido per le mosse successive.
     */
    void applicaColoreScelto(Colore coloreScelto) {
        coloreAttuale = Objects.requireNonNull(coloreScelto, "coloreScelto non puo essere null");
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
     * Gestisce la pesca volontaria del giocatore attivo. La rifiuta se la partita
     * è terminata o se esiste già una carta pescata da risolvere; altrimenti pesca
     * e registra la carta come unica giocabile prima del passaggio del turno.
     * Il valore true indica che la richiesta è stata accettata, anche se il mazzo
     * e gli scarti non forniscono una carta.
     */
    public boolean pescaCarta() {
        if (vincitore != null || cartaPescataDaGiocare) {
            return false;
        }

        Carta cartaPescata = pescaSingola(getGiocatoreAttivo());
        cartaAppenaPescata = cartaPescata;
        cartaPescataDaGiocare = true;
        return true;
    }

    /**
     * Pesca una carta per il giocatore indicato. Se il mazzo è vuoto, prova a
     * ricostituirlo rimescolando gli scarti (la carta in cima resta agli scarti)
     * e riprova. Aggiunge la carta alla mano solo se ne ha trovata una.
     * Non modifica lo stato del turno o della carta appena pescata, così può
     * essere riutilizzato anche per effetti e penalità.
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
     * Esegue una pesca richiesta da un effetto, delegando alle regole la strategia
     * e il numero di carte da pescare; restituisce true se è stata pescata almeno
     * una carta.
     */
    public boolean pescaCartaForzata(Giocatore giocatore) {
        Objects.requireNonNull(giocatore, "giocatore non puo essere null");
        return !regole.pesca(giocatore, this).isEmpty();
    }

    /**
     * GRASP Coordinator: gestisce il cambio turno tramite self-message dedicato.
     */
    /**
     * Consente di passare solo dopo una pesca e solo finché la partita è attiva.
     * Se il controllo passa, elimina lo stato della carta pescata e aggiorna il
     * giocatore attivo; altrimenti non modifica la partita.
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
     * Avanza al giocatore successivo. Se un effetto ha richiesto di saltare il
     * prossimo turno, consuma il flag e avanza una seconda volta.
     */
    public void aggiornaGiocatoreAttivo() {
        avanzaGiocatore();
        if (saltaProssimoTurno) {
            saltaProssimoTurno = false;
            avanzaGiocatore();
        }
    }

    /**
     * Aggiorna l'indice del giocatore attivo secondo il senso di gioco.
     * L'aritmetica circolare mantiene l'indice entro i limiti della lista anche
     * quando si supera il primo o l'ultimo giocatore.
     */
    private void avanzaGiocatore() {
        if (sensoOrario) {
            indiceGiocatoreAttivo = (indiceGiocatoreAttivo + 1) % giocatori.size();
        } else {
            indiceGiocatoreAttivo = (indiceGiocatoreAttivo - 1 + giocatori.size()) % giocatori.size();
        }
    }
    /**
     * Inverte il senso di avanzamento dei turni, come richiesto dall'effetto
     * della carta cambio giro.
     */
    public void cambioGiro(){
        this.sensoOrario = !sensoOrario; // Inverte il senso di gioco
    }
    /**
     * Calcola il prossimo giocatore rispettando il senso corrente, senza
     * modificare l'indice del giocatore attivo. È usato dagli effetti che
     * agiscono sul giocatore successivo, come la pescata forzata.
     */
    public Giocatore getProssimoGiocatore() {
        if (sensoOrario) {
            return giocatori.get((indiceGiocatoreAttivo + 1) % giocatori.size());
        } else {
            return giocatori.get((indiceGiocatoreAttivo - 1 + giocatori.size()) % giocatori.size());
        }
    }
}
