package unolegends;

import java.util.ArrayList;
import java.util.Collections;
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
    private static final int CARTE_IN_MAZZO_INIZIALE = 10;
    private static final MazzoFactory MAZZO_FACTORY = StandardMazzoFactory.getInstance();

    /**
     * Crea una Partita standard con 2 giocatori.
     * Configurazione di default per il prototipo demo.
     */
    public static Partita creaPartitaStandard() {
        return creaPartita(2);
    }

    /**
     * Crea una Partita con numero di giocatori specificato.
     * 
     * @param numeroDiGiocatori numero di giocatori (per ora solo 2 supportato)
     * @return una Partita completamente configurata e pronta al gioco
     * @throws IllegalArgumentException se numeroDiGiocatori != 2
     */
    public static Partita creaPartita(int numeroDiGiocatori) {
        if (numeroDiGiocatori != 2) {
            throw new IllegalArgumentException("Attualmente solo 2 giocatori sono supportati");
        }

        // 1. Obtain complete deck from the concrete MazzoFactory (Expert: knows which cards exist)
        List<Carta> carteTotali = MAZZO_FACTORY.getCarteNormali();

        // 2. Shuffle (randomize distribution)
        List<Carta> carteMescolate = new ArrayList<>(carteTotali);
        Collections.shuffle(carteMescolate);

        // 3. Distribute cards to players
        List<Giocatore> giocatori = distribuisciCarteAiGiocatori(carteMescolate, numeroDiGiocatori);

        // 4. Crea il Mazzo tramite MazzoFactory (estendibile per house rules in futuro)
        int indicePartenzaMazzo = numeroDiGiocatori * CARTE_PER_GIOCATORE;
        Mazzo mazzo = MAZZO_FACTORY.creaMazzo(carteMescolate, indicePartenzaMazzo, CARTE_IN_MAZZO_INIZIALE);

        // 5. Discard pile initialization (first card from remaining deck)
        int indicePartenzaScarti = Math.min(indicePartenzaMazzo + CARTE_IN_MAZZO_INIZIALE, carteMescolate.size());
        if (indicePartenzaScarti < carteMescolate.size()) {
            Carta primaCarta = carteMescolate.get(indicePartenzaScarti);
            PilaDegliScarti pilaDegliScarti = new PilaDegliScarti(List.of(primaCarta));
            // Default rules: RegoleStandard. In futuro PartitaFactory può accettare configurazioni.
            return new Partita(giocatori, mazzo, pilaDegliScarti, 0, new RegoleStandard());
        }

        throw new IllegalStateException("Non ci sono carte sufficienti per inizializzare gli scarti");
    }

    /**
     * Distribuisce le carte equamente tra i giocatori.
     * GRASP Expert: sa come distribuire carte ai giocatori.
     * 
     * @param carteMescolate lista mescolata di tutte le carte
     * @param numeroDiGiocatori numero di giocatori
     * @return lista di Giocatori con le loro carte distribuite
     */
    private static List<Giocatore> distribuisciCarteAiGiocatori(List<Carta> carteMescolate, int numeroDiGiocatori) {
        List<Giocatore> giocatori = new ArrayList<>();

        for (int indiceGiocatore = 0; indiceGiocatore < numeroDiGiocatori; indiceGiocatore++) {
            List<Carta> mano = new ArrayList<>();

            for (int i = 0; i < CARTE_PER_GIOCATORE; i++) {
                int indiceCartaGlobale = indiceGiocatore + (i * numeroDiGiocatori);
                mano.add(carteMescolate.get(indiceCartaGlobale));
            }

            String nomePredefinito = "Giocatore" + (indiceGiocatore + 1);
            giocatori.add(new Giocatore(nomePredefinito, mano));
        }

        return giocatori;
    }
}
