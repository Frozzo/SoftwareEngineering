package unolegends.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GRASP Factory (Creator pattern): responsabile della configurazione e creazione di una Partita.
 * 
 * Orchestrazione del setup iniziale:
 * 1. Ottiene il catalogo completo delle carte
 * 2. Mescola il mazzo
 * 3. Distribuisce equamente ai giocatori
 * 4. Inizializza gli scarti con la prima carta del mazzo
 * 5. Ritorna una Partita pronta per il gioco
 * 
 * Design principles:
 * - GRASP Creator: crea le Partite (oggetti complessi)
 * - GRASP Expert: delega ai rispettivi expert domain
 * - GRASP Facade: nasconde la complessità di setup
 * - GOF Factory: centralizza la logica di creazione
 * - SRP: unica responsabilità = configurare il setup iniziale
 */
public class PartitaFactory {
    private static final int CARTE_PER_GIOCATORE = 5;

    /**
     * Crea il compositore delle quattro strategie delle regole standard.
     */
    public static RegoleDiGioco creaRegoleStandard() {
        return RegoleDiGioco.standard();
    }

    /**
     * Crea una partita usando esplicitamente tutte le dipendenze di configurazione.
     *
     * @param mazzoFactory factory responsabile della creazione del mazzo
     * @param numeroDiGiocatori numero di giocatori (per ora solo 2 supportato)
     * @param regole profilo di regole della partita
     * @return una Partita completamente configurata e pronta per essere avviata
     */
    public static Partita creaPartita(MazzoFactory mazzoFactory, int numeroDiGiocatori, RegoleDiGioco regole) {
        Objects.requireNonNull(mazzoFactory, "mazzoFactory non puo essere null");
        Objects.requireNonNull(regole, "regole non puo essere null");
        if (numeroDiGiocatori != 2) {
            throw new IllegalArgumentException("Attualmente solo 2 giocatori sono supportati");
        }

        Mazzo mazzo = mazzoFactory.creaMazzo();
        List<Giocatore> giocatori = distribuisciCarteAiGiocatori(mazzo, numeroDiGiocatori);

        Carta primaCarta = mazzo.prelevaCarta();
        if (primaCarta != null) {
            PilaDegliScarti pilaDegliScarti = new PilaDegliScarti(List.of(primaCarta));
            boolean richiedeInput = primaCarta.getEffetto().getRichiestaInput() != null;
            int indiceGiocatoreIniziale = richiedeInput
                    ? 0
                    : (giocatori.size() - 1) % giocatori.size();
            return new Partita(giocatori, mazzo, pilaDegliScarti,
                    indiceGiocatoreIniziale, regole);
        }

        throw new IllegalStateException("Non ci sono carte sufficienti per inizializzare gli scarti");
    }

    /**
     * Distribuisce le carte equamente tra i giocatori.
     * GRASP Expert: sa come distribuire carte ai giocatori.
     * 
     * @param mazzo mazzo già mescolato da cui prelevare le carte
     * @param numeroDiGiocatori numero di giocatori
     * @return lista di Giocatori con le loro carte distribuite
     */
    private static List<Giocatore> distribuisciCarteAiGiocatori(Mazzo mazzo, int numeroDiGiocatori) {
        Objects.requireNonNull(mazzo, "mazzo non puo essere null");
        List<List<Carta>> mani = new ArrayList<>();
        for (int indiceGiocatore = 0; indiceGiocatore < numeroDiGiocatori; indiceGiocatore++) {
            mani.add(new ArrayList<>());
        }

        for (int indiceCarta = 0; indiceCarta < numeroDiGiocatori * CARTE_PER_GIOCATORE; indiceCarta++) {
            Carta carta = mazzo.prelevaCarta();
            if (carta == null) {
                throw new IllegalStateException("Non ci sono carte sufficienti per distribuire le mani");
            }
            mani.get(indiceCarta % numeroDiGiocatori).add(carta);
        }

        List<Giocatore> giocatori = new ArrayList<>();
        for (int indiceGiocatore = 0; indiceGiocatore < numeroDiGiocatori; indiceGiocatore++) {
            String nomePredefinito = "Giocatore" + (indiceGiocatore + 1);
            giocatori.add(new Giocatore(nomePredefinito, mani.get(indiceGiocatore)));
        }
        return giocatori;
    }
}
