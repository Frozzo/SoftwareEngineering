package unolegends;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogo delle carte normali: numero 0-9 per ciascun colore.
 * In futuro estenderai con altri cataloghi (carte speciali, carte legend, ecc.)
 * oppure creerai classi separate (CarteSpeciali, CarteLegend).
 */
public final class CarteCatalogo {
    private CarteCatalogo() {
    }

    /**
     * Restituisce il set completo di carte normali: 0-9 per ciascun colore.
     * Totale: 40 carte.
     */
    public static List<Carta> getCarteNormali() {
        List<Carta> carte = new ArrayList<>();
        int idCounter = 0;

        for (Colore colore : Colore.values()) {
            for (int numero = 0; numero <= 9; numero++) {
                String id = generaId(colore, numero, idCounter);
                carte.add(new Carta(id, colore, numero));
                idCounter++;
            }
        }

        return carte;
    }

    private static String generaId(Colore colore, int numero, int contatore) {
        return String.format("N%d_%s_%d", contatore, colore.getNome().substring(0, 1), numero);
    }
}
