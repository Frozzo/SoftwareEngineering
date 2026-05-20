package unolegends;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract factory per creare le famiglie di oggetti legate al mazzo.
 */
public abstract class MazzoFactory {

    /**
     * Restituisce il catalogo completo delle carte della famiglia corrente.
     */
    public abstract List<Carta> getCarteNormali();

    /**
     * Crea un {@link Mazzo} prelevando {@code size} carte a partire da {@code startIndex}
     * dalla lista fornita.
     */
    public Mazzo creaMazzo(List<Carta> source, int startIndex, int size) {
        Objects.requireNonNull(source, "source non puo essere null");
        if (startIndex < 0 || startIndex > source.size()) {
            throw new IllegalArgumentException("startIndex non valido");
        }

        int endIndex = Math.min(startIndex + size, source.size());
        List<Carta> sub = new ArrayList<>(source.subList(startIndex, endIndex));
        return new Mazzo(sub);
    }
}
