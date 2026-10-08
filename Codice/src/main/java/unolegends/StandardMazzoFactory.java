package unolegends;

import java.util.ArrayList;
import java.util.Collections;
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

        for (Colore colore : List.of(Colore.ROSSO, Colore.GIALLO, Colore.VERDE, Colore.BLU)) {
            for (int numero = 0; numero <= 9; numero++) {
                ValoreCarta numeroEnum = ValoreCarta.fromInt(numero);
                carte.add(new CartaNumero(idCounter++, colore, numeroEnum));
                if (numero != 0) {
                    carte.add(new CartaNumero(idCounter++, colore, numeroEnum));
                }
            }

            for (int copia = 0; copia < 2; copia++) {
                carte.add(new CartaSpeciale(
                        colore,
                        ValoreCarta.PIU_DUE,
                        new Effetto_pescata_carte(2)));
                carte.add(new CartaSpeciale(
                        colore,
                        ValoreCarta.BLOCCA_TURNO,
                        new Effetto_Blocca_Turno()));
                carte.add(new CartaSpeciale(
                        colore,
                        ValoreCarta.CAMBIA_GIRO,
                        new Effetto_cambio_giro()));
            }
        }

        for (int i = 0; i < 4; i++) {
            carte.add(new CartaSpeciale(
                    Colore.NERO,
                    ValoreCarta.CAMBIA_COLORE,
                    new Effetto_Cambia_colore()));
            carte.add(new CartaSpeciale(
                    Colore.NERO,
                    ValoreCarta.PIU_QUATTRO,
                    new EffettoComposito(List.of(
                        new Effetto_Cambia_colore(),
                        new Effetto_pescata_carte(4)))));
        }

        return Collections.unmodifiableList(carte);
    }
}