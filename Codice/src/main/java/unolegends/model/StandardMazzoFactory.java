package unolegends.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete singleton factory per il mazzo standard.
 */
public class StandardMazzoFactory implements MazzoFactory {
    private static final StandardMazzoFactory INSTANCE = new StandardMazzoFactory();

    private StandardMazzoFactory() {
    }

    public static StandardMazzoFactory getInstance() {
        return INSTANCE;
    }

    
    public Mazzo creaMazzo() {
        List<Carta> carte = new ArrayList<>();

        for (Colore colore : List.of(Colore.ROSSO, Colore.GIALLO, Colore.VERDE, Colore.BLU)) {
            for (int numero = 0; numero <= 9; numero++) {
                ValoreCarta numeroEnum = ValoreCarta.fromInt(numero);
                carte.add(new Carta(colore, numeroEnum));
                if (numero != 0) {
                    carte.add(new Carta(colore, numeroEnum));
                }
            }

            for (int copia = 0; copia < 2; copia++) {
                carte.add(new Carta(
                        colore,
                        ValoreCarta.PIU_DUE,
                        new Effetto_pescata_carte(2, OriginePesca.PIU_DUE)));
                carte.add(new Carta(
                        colore,
                        ValoreCarta.BLOCCA_TURNO,
                        new Effetto_Blocca_Turno()));
                carte.add(new Carta(
                        colore,
                        ValoreCarta.CAMBIA_GIRO,
                        new Effetto_cambio_giro()));
            }
        }

        for (int i = 0; i < 4; i++) {
            carte.add(new Carta(
                    Colore.NERO,
                    ValoreCarta.CAMBIA_COLORE,
                    new Effetto_Cambia_colore()));
            carte.add(new Carta(
                    Colore.NERO,
                    ValoreCarta.PIU_QUATTRO,
                    new EffettoComposito(List.of(
                        new Effetto_Cambia_colore(),
                        new Effetto_pescata_carte(4, OriginePesca.PIU_QUATTRO)))));
        }

        Mazzo mazzo = new Mazzo(carte);
        mazzo.mischia();
        return mazzo;
    }
}