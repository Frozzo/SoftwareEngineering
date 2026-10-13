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
        this.carte = new ArrayDeque<>(carteIniziali);
    }

    public Carta prelevaCarta() {
        return carte.pollFirst();
    }

    public boolean isVuoto() {
        return carte.isEmpty();
    }

    public int getNumeroCarte() {
        return carte.size();
    }

    public void mischia() {
        List<Carta> listaTemporanea = new ArrayList<>(carte);
        Collections.shuffle(listaTemporanea);
        carte.clear();
        carte.addAll(listaTemporanea);
    }
    /**
     * Ricostituisce e mescola il mazzo usando gli scarti, preservando la carta in cima.
     */
    public void rimescolaDaScarti(PilaDegliScarti pilaDegliScarti) {
        Objects.requireNonNull(pilaDegliScarti, "pilaDegliScarti non puo essere null");
        List<Carta> carteDaRimescolare = pilaDegliScarti.estraiCartePerRimescolamento();
        carte.addAll(carteDaRimescolare);
        mischia();
    }
}
