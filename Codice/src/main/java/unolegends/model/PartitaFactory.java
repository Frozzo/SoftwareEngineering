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
    private static final int CARTE_PER_GIOCATORE = 4;

    public static RegoleDiGioco creaRegoleStandard() {
        return RegoleDiGioco.standard();
    }

    public static Partita creaPartita(MazzoFactory mazzoFactory, int numeroDiGiocatori, RegoleDiGioco regole) {
        Objects.requireNonNull(mazzoFactory, "mazzoFactory non puo essere null");
        Objects.requireNonNull(regole, "regole non puo essere null");
        Mazzo mazzo = mazzoFactory.creaMazzo();
        List<Giocatore> giocatori = distribuisciCarteAiGiocatori(mazzo, numeroDiGiocatori);
        Carta primaCarta = mazzo.prelevaCarta();
        PilaDegliScarti pilaDegliScarti = new PilaDegliScarti(List.of(primaCarta));
        boolean richiedeInput = primaCarta.getEffetto().getRichiestaInput() != null;
        int indiceGiocatoreIniziale;
        if (richiedeInput == true) {
            indiceGiocatoreIniziale = 0;
        } else {
            indiceGiocatoreIniziale = (giocatori.size() - 1) % giocatori.size();
        }
            return new Partita(giocatori, mazzo, pilaDegliScarti,indiceGiocatoreIniziale, regole);
    }

    private static List<Giocatore> distribuisciCarteAiGiocatori(Mazzo mazzo, int numeroDiGiocatori) {
        List<Giocatore> giocatori = new ArrayList<>();

        for (int indiceGiocatore = 0; indiceGiocatore < numeroDiGiocatori; indiceGiocatore++) {
            List<Carta> mano = new ArrayList<>();

            for (int j = 0; j < CARTE_PER_GIOCATORE; j++) {
                mano.add(mazzo.prelevaCarta());
            }

            String nomePredefinito = "Giocatore" + (indiceGiocatore + 1);
            giocatori.add(new Giocatore(nomePredefinito, mano));
        }

        return giocatori;
    }
}
