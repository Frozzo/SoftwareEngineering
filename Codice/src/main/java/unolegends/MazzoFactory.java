package unolegends;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Factory per creare istanze di {@link Mazzo} a partire da una lista di carte.
 * In futuro potrà ricevere opzioni/house rules per modificare la composizione.
 */
public final class MazzoFactory {
    private MazzoFactory() {
    }

    /**
     * Crea un {@link Mazzo} prelevando `size` carte a partire da `startIndex` dalla lista fornita.
     * Nota: il metodo restituisce solo il Mazzo. Se il chiamante necessita dell'indice
     * della prossima carta libera (per esempio per inizializzare la pila degli scarti),
     * può calcolarlo come `Math.min(startIndex + size, source.size())`.
     */
    public static Mazzo creaMazzo(List<Carta> source, int startIndex, int size) {
        Objects.requireNonNull(source, "source non puo essere null");
        if (startIndex < 0 || startIndex > source.size()) {
            throw new IllegalArgumentException("startIndex non valido");
        }

        int endIndex = Math.min(startIndex + size, source.size());
        List<Carta> sub = new ArrayList<>(source.subList(startIndex, endIndex));
        return new Mazzo(sub);
    }
}
