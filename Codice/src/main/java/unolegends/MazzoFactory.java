package unolegends;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MazzoFactory {

    // Costruttore privato: è una classe Factory di utilità, non va istanziata con 'new'
    private MazzoFactory() {}

    /**
     * Metodo Factory principale: decide quale composizione generare
     */
    public static Mazzo creaMazzo(TipoVariante variante) {
        return switch (variante) {
            case CLASSICO -> creaMazzoStandard108();
            case SENZA_PIETA -> creaMazzoNoMercy();
        };
    }

    private static Mazzo creaMazzoStandard108() {
        List<Carta> carte = new ArrayList<>(108);
        Colore[] coloriStandard = {Colore.ROSSO, Colore.GIALLO, Colore.VERDE, Colore.BLU};
        int idProg = 1;

        for (Colore col : coloriStandard) {
            // 1 solo zero per colore
            carte.add(new Carta("std_" + (idProg++), col, Valore.ZERO));

            // Due carte per ogni numero da 1 a 9
            Valore[] numeri = {
                Valore.UNO, Valore.DUE, Valore.TRE, Valore.QUATTRO,
                Valore.CINQUE, Valore.SEI, Valore.SETTE, Valore.OTTO, Valore.NOVE
            };
            for (Valore val : numeri) {
                carte.add(new Carta("std_" + (idProg++), col, val));
                carte.add(new Carta("std_" + (idProg++), col, val));
            }

            // Due carte azione per colore: Salta, Inverti, +2
            Valore[] azioni = {Valore.SALTA, Valore.INVERTI, Valore.PIU_DUE};
            for (Valore az : azioni) {
                carte.add(new Carta("std_" + (idProg++), col, az));
                carte.add(new Carta("std_" + (idProg++), col, az));
            }
        }

        // 4 Jolly normali e 4 Jolly Pesca Quattro (Neri)
        for (int i = 0; i < 4; i++) {
            carte.add(new Carta("std_" + (idProg++), Colore.SPECIALE_NERO, Valore.JOLLY));
            carte.add(new Carta("std_" + (idProg++), Colore.SPECIALE_NERO, Valore.JOLLY_PIU_QUATTRO));
        }

        return new Mazzo(carte);
    }

    /**
     * Composizione per variante estrema (include carte aggiuntive)
     */
    private static Mazzo creaMazzoNoMercy() {
        // Parte dal mazzo standard e ci aggiunge penalità folli
        Mazzo mazzoBase = creaMazzoStandard108();
        List<Carta> carte = new ArrayList<>();
        
        // Svuotiamo il base nella lista per espanderlo
        while (mazzoBase.carteRimanenti() > 0) {
            carte.add(mazzoBase.pesca());
        }

        int idProg = 500;
        // Aggiungiamo 4 carte +6, 4 carte +10 e 4 Salta Tutti
        for (int i = 0; i < 4; i++) {
            carte.add(new Carta("nm_" + (idProg++), Colore.SPECIALE_NERO, Valore.PIU_SEI));
            carte.add(new Carta("nm_" + (idProg++), Colore.SPECIALE_NERO, Valore.PIU_DIECI));
            carte.add(new Carta("nm_" + (idProg++), Colore.SPECIALE_NERO, Valore.SALTA_TUTTI));
        }

        return new Mazzo(carte);
    }
}