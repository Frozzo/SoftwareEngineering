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
 *   <li>distribuisce 7 carte a ogni giocatore, con i nomi e (dallo Step 6) i Legend scelti nel menu;</li>
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
        return creaPartita(config.modalita().getMazzoFactory(), config.nomiGiocatori(), config.legend(),
                creaRegole(config));
    }

    /**
     * Crea una partita a partire dai singoli pezzi. È usato da
     * {@link #creaPartita(ConfigurazionePartita)} e resta pubblico per poter
     * provare il dominio con regole costruite a mano.
     */
    public static Partita creaPartita(MazzoFactory mazzoFactory, List<String> nomiGiocatori, RegoleDiGioco regole) {
        return creaPartita(mazzoFactory, nomiGiocatori, List.of(), regole);
    }

    /**
     * Come {@link #creaPartita(MazzoFactory, List, RegoleDiGioco)}, con i Legend dei
     * giocatori nello stesso ordine dei nomi (lista vuota = partita senza Legend).
     */
    public static Partita creaPartita(MazzoFactory mazzoFactory, List<String> nomiGiocatori, List<Legend> legend,
                                      RegoleDiGioco regole) {
        Objects.requireNonNull(mazzoFactory, "mazzoFactory non puo essere null");
        Objects.requireNonNull(nomiGiocatori, "nomiGiocatori non puo essere null");
        Objects.requireNonNull(legend, "legend non puo essere null");
        Objects.requireNonNull(regole, "regole non puo essere null");
        if (!legend.isEmpty() && legend.size() != nomiGiocatori.size()) {
            throw new IllegalArgumentException("serve un Legend per ogni giocatore");
        }
        Mazzo mazzo = mazzoFactory.creaMazzo();
        List<Giocatore> giocatori = distribuisciCarteAiGiocatori(mazzo, nomiGiocatori, legend);
        Carta primaCarta = scopriPrimaCarta(mazzo);
        PilaDegliScarti pilaDegliScarti = new PilaDegliScarti(List.of(primaCarta));
        return new Partita(giocatori, mazzo, pilaDegliScarti, regole);
    }

    /**
     * Scopre la prima carta degli scarti. Le carte il cui effetto non ha senso a
     * inizio partita (scambio o rotazione delle mani, Scarta tutto, Salta tutti:
     * agiscono su "chi ha giocato", che qui non c'è) vengono rimesse nel mazzo,
     * che viene rimescolato, e se ne scopre un'altra.
     *
     * <p>Lo decide l'effetto ({@link Ieffetto#isAmmessoComeCartaIniziale()}), non
     * un controllo sul tipo della carta. Nella partita normale tutte le carte sono
     * ammesse, quindi il comportamento non cambia.</p>
     */
    private static Carta scopriPrimaCarta(Mazzo mazzo) {
        Carta carta = mazzo.prelevaCarta();
        while (carta != null && !carta.getEffetto().isAmmessoComeCartaIniziale()) {
            mazzo.aggiungiCarteERimescola(List.of(carta));
            carta = mazzo.prelevaCarta();
        }
        return carta;
    }

    /**
     * Sceglie le regole della modalità passando le opzioni attive. Lo
     * {@code switch} è esaustivo: una modalità nuova senza il suo caso qui non
     * compila.
     */
    private static RegoleDiGioco creaRegole(ConfigurazionePartita config) {
        return switch (config.modalita()) {
            case NORMALE -> RegoleDiGioco.standard(config.opzioni());
            case NO_MERCY -> RegoleDiGioco.noMercy(config.opzioni());
        };
    }

    private static List<Giocatore> distribuisciCarteAiGiocatori(Mazzo mazzo, List<String> nomiGiocatori,
                                                               List<Legend> legend) {
        List<Giocatore> giocatori = new ArrayList<>();
        for (int i = 0; i < nomiGiocatori.size(); i++) {
            List<Carta> mano = new ArrayList<>();
            for (int j = 0; j < CARTE_PER_GIOCATORE; j++) {
                mano.add(mazzo.prelevaCarta());
            }
            giocatori.add(new Giocatore(nomiGiocatori.get(i), mano, legend.isEmpty() ? null : legend.get(i)));
        }
        return giocatori;
    }
}
