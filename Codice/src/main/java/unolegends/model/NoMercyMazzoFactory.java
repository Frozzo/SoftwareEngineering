package unolegends.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete Factory (Abstract Factory) + Singleton del mazzo No Mercy: 168 carte.
 *
 * <p>La composizione è scritta qui e solo qui (decisione presa: non cambierà, quindi
 * non serve renderla configurabile). Le quantità per colore sono costanti con un
 * nome, così si leggono e si verificano in un colpo d'occhio:</p>
 *
 * <table>
 *   <caption>Composizione del mazzo No Mercy</caption>
 *   <tr><th>Gruppo</th><th>Carte</th><th>Totale</th></tr>
 *   <tr><td>Numeriche</td><td>2 copie di ogni numero 0–9 per colore (lo 0 passa le mani, il 7 le scambia)</td><td>80</td></tr>
 *   <tr><td>Azioni colorate</td><td>per colore: 3 blocca turno, 3 cambia giro, 3 +2, 2 +4, 3 scarta tutto, 2 salta tutti</td><td>64</td></tr>
 *   <tr><td>Jolly</td><td>8 +4 cambio giro, 8 roulette colore, 4 +6, 4 +10</td><td>24</td></tr>
 * </table>
 *
 * <p>Ogni carta è un'unica classe {@link Carta}: il comportamento lo dà l'effetto
 * passato al costruttore, quindi qui non servono classi di carta nuove.</p>
 */
public class NoMercyMazzoFactory implements MazzoFactory {
    private static final NoMercyMazzoFactory INSTANCE = new NoMercyMazzoFactory();

    /** Totale atteso, controllato a ogni creazione del mazzo. */
    public static final int NUMERO_CARTE = 168;

    private static final int COPIE_PER_NUMERO = 2;
    private static final int BLOCCA_TURNO_PER_COLORE = 3;
    private static final int CAMBIA_GIRO_PER_COLORE = 3;
    private static final int PIU_DUE_PER_COLORE = 3;
    private static final int PIU_QUATTRO_PER_COLORE = 2;
    private static final int SCARTA_TUTTO_PER_COLORE = 3;
    private static final int SALTA_TUTTI_PER_COLORE = 2;

    private static final int JOLLY_CAMBIO_GIRO_PIU_QUATTRO = 8;
    private static final int JOLLY_ROULETTE = 8;
    private static final int JOLLY_PIU_SEI = 4;
    private static final int JOLLY_PIU_DIECI = 4;

    private NoMercyMazzoFactory() {
    }

    public static NoMercyMazzoFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public Mazzo creaMazzo() {
        List<Carta> carte = new ArrayList<>();

        for (Colore colore : List.of(Colore.ROSSO, Colore.GIALLO, Colore.VERDE, Colore.BLU)) {
            for (int numero = 0; numero <= 9; numero++) {
                for (int copia = 0; copia < COPIE_PER_NUMERO; copia++) {
                    carte.add(creaCartaNumero(colore, numero));
                }
            }
            aggiungi(carte, BLOCCA_TURNO_PER_COLORE,
                    () -> new Carta(colore, ValoreCarta.BLOCCA_TURNO, new Effetto_Blocca_Turno()));
            aggiungi(carte, CAMBIA_GIRO_PER_COLORE,
                    () -> new Carta(colore, ValoreCarta.CAMBIA_GIRO, new Effetto_cambio_giro()));
            aggiungi(carte, PIU_DUE_PER_COLORE,
                    () -> new Carta(colore, ValoreCarta.PIU_DUE, new Effetto_pescata_carte(2, OriginePesca.PIU_DUE)));
            aggiungi(carte, PIU_QUATTRO_PER_COLORE,
                    () -> new Carta(colore, ValoreCarta.PIU_QUATTRO, new Effetto_pescata_carte(4, OriginePesca.PIU_QUATTRO)));
            aggiungi(carte, SCARTA_TUTTO_PER_COLORE,
                    () -> new Carta(colore, ValoreCarta.SCARTA_TUTTO, new EffettoScartaColore()));
            aggiungi(carte, SALTA_TUTTI_PER_COLORE,
                    () -> new Carta(colore, ValoreCarta.SALTA_TUTTI, new EffettoTurnoExtra()));
        }

        // +4 cambio giro: prima il colore, poi il cambio di senso, poi pesca il
        // successivo nel NUOVO senso. Con 2 giocatori il cambio giro vale come
        // blocco, quindi il turno torna a chi ha giocato.
        aggiungi(carte, JOLLY_CAMBIO_GIRO_PIU_QUATTRO,
                () -> new Carta(Colore.NERO, ValoreCarta.CAMBIO_GIRO_PIU_QUATTRO, new EffettoComposito(List.of(
                        new Effetto_Cambia_colore(),
                        new Effetto_cambio_giro(),
                        new Effetto_pescata_carte(4, OriginePesca.PIU_QUATTRO)))));
        aggiungi(carte, JOLLY_ROULETTE,
                () -> new Carta(Colore.NERO, ValoreCarta.ROULETTE, new EffettoRoulette()));
        aggiungi(carte, JOLLY_PIU_SEI,
                () -> new Carta(Colore.NERO, ValoreCarta.PIU_SEI, new EffettoComposito(List.of(
                        new Effetto_Cambia_colore(),
                        new Effetto_pescata_carte(6, OriginePesca.PIU_SEI)))));
        aggiungi(carte, JOLLY_PIU_DIECI,
                () -> new Carta(Colore.NERO, ValoreCarta.PIU_DIECI, new EffettoComposito(List.of(
                        new Effetto_Cambia_colore(),
                        new Effetto_pescata_carte(10, OriginePesca.PIU_DIECI)))));

        if (carte.size() != NUMERO_CARTE) {
            throw new IllegalStateException("Il mazzo No Mercy deve avere " + NUMERO_CARTE
                    + " carte, ne ha " + carte.size());
        }
        Mazzo mazzo = new Mazzo(carte);
        mazzo.mischia();
        return mazzo;
    }

    /** Lo 0 e il 7 hanno un effetto in No Mercy; gli altri numeri no (Null Object). */
    private static Carta creaCartaNumero(Colore colore, int numero) {
        ValoreCarta valore = ValoreCarta.fromInt(numero);
        return switch (valore) {
            case ZERO -> new Carta(colore, valore, new EffettoPassaMani());
            case SETTE -> new Carta(colore, valore, new EffettoScambiaMano());
            default -> new Carta(colore, valore);
        };
    }

    private static void aggiungi(List<Carta> carte, int copie, java.util.function.Supplier<Carta> crea) {
        for (int i = 0; i < copie; i++) {
            carte.add(crea.get());
        }
    }
}
