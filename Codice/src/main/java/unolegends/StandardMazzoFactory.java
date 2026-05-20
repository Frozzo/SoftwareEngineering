package unolegends;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete singleton factory per il mazzo standard.
 */
public final class StandardMazzoFactory extends MazzoFactory {
    private static final StandardMazzoFactory INSTANCE = new StandardMazzoFactory();

    private StandardMazzoFactory() {
    }

    public static StandardMazzoFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public List<Carta> getCarteNormali() {
        List<Carta> carte = new ArrayList<>();
        int idCounter = 0;

        for (Colore colore : Colore.values()) {
            for (int numero = 0; numero <= 9; numero++) {
                String id = generaId(colore, numero, idCounter);
                carte.add(new CartaNumero(id, colore, numero));
                idCounter++;
            }
        }

        return carte;
    }

    private static String generaId(Colore colore, int numero, int contatore) {
        return String.format("N%d_%s_%d", contatore, colore.getNome().substring(0, 1), numero);
    }
}