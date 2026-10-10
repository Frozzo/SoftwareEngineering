package unolegends.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Collections;

/**
 * GRASP Information Expert: responsabile del prelievo carte dal mazzo.
 */
public class Mazzo {
    private final Deque<Carta> carte;

    public Mazzo(Collection<Carta> carteIniziali) {
        Objects.requireNonNull(carteIniziali, "carteIniziali non puo essere null");
        this.carte = new ArrayDeque<>(carteIniziali);
    }

    /**
     * GRASP Information Expert: preleva la prossima carta disponibile dal mazzo.
     */
    public Carta prelevaCarta() {
        return carte.pollFirst();
    }
    public List<Carta> getCarte() {
        return new ArrayList<>(carte);
    }

    /**
     * Ricostituisce il mazzo usando gli scarti, preservando la carta in cima.
     */
    public void rimescolaDaScarti(PilaDegliScarti pilaDegliScarti) {
        Objects.requireNonNull(pilaDegliScarti, "pilaDegliScarti non puo essere null");
        List<Carta> carteDaRimescolare = pilaDegliScarti.estraiCartePerRimescolamento();
        Collections.shuffle(carteDaRimescolare);
        carte.addAll(carteDaRimescolare);
    }
}
