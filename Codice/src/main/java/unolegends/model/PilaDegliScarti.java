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
     * Inserisce delle carte subito sotto la carta in cima. Serve a "Scarta tutto"
     * (No Mercy): le carte scartate finiscono nella pila ma la carta in cima, e
     * quindi il colore attivo, resta la Scarta tutto appena giocata.
     */
    public void inserisciSottoLaCima(List<Carta> carte) {
        Objects.requireNonNull(carte, "carte non puo essere null");
        int posizione = Math.max(0, scarti.size() - 1);
        scarti.addAll(posizione, carte);
    }

    /**
     * Rimuove gli scarti rimescolabili, mantenendo la carta in cima.
     */
    public List<Carta> estraiCartePerRimescolamento() {
        if (scarti.size() <= 1) {
            return new ArrayList<>();
        }

        Carta cartaInCima = scarti.remove(scarti.size() - 1);
        List<Carta> carteDaRimescolare = new ArrayList<>(scarti);
        scarti.clear();
        scarti.add(cartaInCima);
        return carteDaRimescolare;
    }
}
