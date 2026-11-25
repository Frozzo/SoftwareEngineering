package unolegends.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GRASP Creator: traduce una {@link ConfigurazionePartita} in una {@link Partita}
 * pronta da giocare.
 *
 * <p>Setup iniziale:</p>
 * <ol>
 *   <li>crea il mazzo con la {@link MazzoFactory} della modalità (Abstract Factory);</li>
 *   <li>crea le regole della modalità con le opzioni scelte;</li>
 *   <li>distribuisce 7 carte a ogni giocatore, con i nomi scelti nel menu;</li>
 *   <li>mette la prima carta del mazzo sugli scarti.</li>
 * </ol>
 *
 * <p>È l'unico punto in cui si sceglie in base alla modalità. Gli {@code if} e lo
 * {@code switch} sul tipo stanno qui e non in {@link Partita}: una factory esiste
 * proprio per scegliere le classi concrete, e lo fa una volta sola, alla
 * creazione. Da quel momento {@code Partita} usa solo le interfacce.</p>
 */
public class PartitaFactory {
    private static final int CARTE_PER_GIOCATORE = 7;

    private PartitaFactory() {
    }

    /** Crea la partita descritta dalla configurazione scelta nel menu. */
    public static Partita creaPartita(ConfigurazionePartita config) {
        Objects.requireNonNull(config, "config non puo essere null");
        return creaPartita(config.modalita().getMazzoFactory(), config.nomiGiocatori(), creaRegole(config));
    }

    /**
     * Crea una partita a partire dai singoli pezzi. È usato da
     * {@link #creaPartita(ConfigurazionePartita)} e resta pubblico per poter
     * provare il dominio con regole costruite a mano.
     */
    public static Partita creaPartita(MazzoFactory mazzoFactory, List<String> nomiGiocatori, RegoleDiGioco regole) {
        Objects.requireNonNull(mazzoFactory, "mazzoFactory non puo essere null");
        Objects.requireNonNull(nomiGiocatori, "nomiGiocatori non puo essere null");
        Objects.requireNonNull(regole, "regole non puo essere null");
        Mazzo mazzo = mazzoFactory.creaMazzo();
        List<Giocatore> giocatori = distribuisciCarteAiGiocatori(mazzo, nomiGiocatori);
        Carta primaCarta = mazzo.prelevaCarta();
        PilaDegliScarti pilaDegliScarti = new PilaDegliScarti(List.of(primaCarta));
        return new Partita(giocatori, mazzo, pilaDegliScarti, regole);
    }

    /**
     * Sceglie le regole della modalità passando le opzioni attive. Lo
     * {@code switch} è esaustivo: aggiungendo {@code NO_MERCY} allo Step 4 il
     * compilatore obbliga a scrivere qui {@code RegoleDiGioco.noMercy(...)}.
     */
    private static RegoleDiGioco creaRegole(ConfigurazionePartita config) {
        return switch (config.modalita()) {
            case NORMALE -> RegoleDiGioco.standard(config.opzioni());
        };
    }

    private static List<Giocatore> distribuisciCarteAiGiocatori(Mazzo mazzo, List<String> nomiGiocatori) {
        List<Giocatore> giocatori = new ArrayList<>();
        for (String nome : nomiGiocatori) {
            List<Carta> mano = new ArrayList<>();
            for (int j = 0; j < CARTE_PER_GIOCATORE; j++) {
                mano.add(mazzo.prelevaCarta());
            }
            giocatori.add(new Giocatore(nome, mano));
        }
        return giocatori;
    }
}
