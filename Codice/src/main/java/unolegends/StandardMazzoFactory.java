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

        for (Colore colore : List.of(Colore.ROSSO, Colore.VERDE, Colore.BLU, Colore.GIALLO)) {
            for (int numero = 0; numero <= 9; numero++) {
                ValoreCarta numeroEnum = ValoreCarta.fromInt(numero);
                carte.add(new CartaNumero(idCounter++, colore, numeroEnum));
            }
        }

        for (int i = 0; i < 4; i++) {
            carte.add(new CartaSpeciale(
                    Colore.NERO,
                    ValoreCarta.CAMBIA_COLORE,
                    new Effetto_Cambia_colore()));
        }

        return carte;
    }
}