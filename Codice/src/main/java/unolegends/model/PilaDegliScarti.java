package unolegends.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GRASP Information Expert: gestisce la pila degli scarti e la carta in cima.
 */
public class PilaDegliScarti {
    private final List<Carta> scarti;

    public PilaDegliScarti(List<Carta> scartiIniziali) {
        this.scarti = new ArrayList<>(Objects.requireNonNull(scartiIniziali, "scartiIniziali non puo essere null"));
    }

    /**
     * GRASP Information Expert: fornisce la carta in cima alla pila.
     */
    public Carta getCartaInCima() {
        if (scarti.isEmpty()) {
            return null;
        }
        return scarti.get(scarti.size() - 1);
    }

    /**
     * GRASP Information Expert: inserisce una carta nella pila degli scarti.
     */
    public void aggiungiCarta(Carta carta) {
        scarti.add(Objects.requireNonNull(carta, "carta non puo essere null"));
    }

    /**
     * Rimuove gli scarti rimescolabili, mantenendo la carta in cima.
     */
    public List<Carta> estraiCartePerRimescolamento() {
        if (scarti.size() <= 1) {
            return new ArrayList<>();
        }
        List<Carta> carteDaRimescolare = new ArrayList<>(scarti.subList(0, scarti.size() - 1));
        scarti.subList(0, scarti.size() - 1).clear();
        return carteDaRimescolare;
    }
}
