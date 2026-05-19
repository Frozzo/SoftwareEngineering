package unolegends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * GRASP Information Expert: gestisce e conosce la propria mano.
 */
public class Giocatore {
    private final String nome;
    private final List<Carta> mano;

    public Giocatore(String nome, List<Carta> manoIniziale) {
        this.nome = Objects.requireNonNull(nome, "nome non puo essere null");
        this.mano = new ArrayList<>(Objects.requireNonNull(manoIniziale, "manoIniziale non puo essere null"));
    }

    public String getNome() {
        return nome;
    }

    /**
     * GRASP Information Expert: espone la mano del giocatore attivo alla radice del dominio.
     */
    public List<Carta> getMano() {
        return Collections.unmodifiableList(mano);
    }

    /**
     * GRASP Information Expert: restituisce la carta presente nella posizione richiesta della mano.
     */
    public Carta getCartaInPosizione(int indiceCarta) {
        if (indiceCarta < 0 || indiceCarta >= mano.size()) {
            return null;
        }
        return mano.get(indiceCarta);
    }

    /**
     * GRASP Information Expert: rimuove e restituisce la carta nella posizione richiesta.
     */
    public Carta estraiCarta(int indiceCarta) {
        if (indiceCarta < 0 || indiceCarta >= mano.size()) {
            return null;
        }
        return mano.remove(indiceCarta);
    }

    /**
     * GRASP Information Expert: aggiunge una carta alla mano del giocatore.
     */
    public void aggiungiCarta(Carta carta) {
        mano.add(Objects.requireNonNull(carta, "carta non puo essere null"));
    }
}
