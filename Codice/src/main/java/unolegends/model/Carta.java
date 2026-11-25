package unolegends.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Una carta del mazzo: colore, valore ed effetto.
 *
 * <p>Prima esistevano una classe astratta {@code Carta} e due sottoclassi,
 * {@code CartaNumero} e {@code CartaSpeciale}, che però erano identiche: stessi
 * campi, stessi metodi. L'unica differenza era che {@code CartaNumero} usava sempre
 * {@link Effetto_Nullo}. Il comportamento di una carta, infatti, non dipende dalla
 * sua classe ma dal suo {@link Ieffetto} (Strategy), e per le carte senza effetto
 * c'è già il Null Object. Per questo le tre classi sono state unificate in una sola
 * classe concreta: composizione al posto dell'ereditarietà.</p>
 *
 * <p>Vantaggi:</p>
 * <ul>
 *   <li>le carte numeriche con un effetto (lo 0 e il 7 della No Mercy, Step 4) non
 *       richiedono una classe nuova né un costruttore speciale;</li>
 *   <li>il controller non deve più usare {@code instanceof} per mostrare un jolly
 *       con il colore scelto;</li>
 *   <li>la distinzione "numero o speciale" resta disponibile in {@link ValoreCarta}.</li>
 * </ul>
 *
 * <p>La classe è {@code final}: una carta varia solo tramite l'effetto. Se un giorno
 * servisse un comportamento diverso oltre all'effetto, si potrà rivalutare una
 * sottoclasse.</p>
 */
public final class Carta {
    private final Colore colore;
    private final ValoreCarta valore;
    private final Ieffetto effetto;

    /** Carta con un effetto (+2, blocca, cambia giro, jolly, e in No Mercy anche 0 e 7). */
    public Carta(Colore colore, ValoreCarta valore, Ieffetto effetto) {
        this.colore = Objects.requireNonNull(colore, "colore non puo essere null");
        this.valore = Objects.requireNonNull(valore, "valore non puo essere null");
        this.effetto = Objects.requireNonNull(effetto, "effetto non puo essere null");
    }

    /** Carta senza effetto (le numeriche del mazzo standard): usa il Null Object {@link Effetto_Nullo}. */
    public Carta(Colore colore, ValoreCarta valore) {
        this(colore, valore, new Effetto_Nullo());
    }

    public Colore getColore() {
        return colore;
    }

    public ValoreCarta getValoreCarta() {
        return valore;
    }

    public Ieffetto getEffetto() {
        return effetto;
    }

    /** Testo della carta senza colori ANSI, per esempio {@code "7 rosso"} o {@code "+4 nero"}. */
    public String toTestoBase() {
        return testo(colore);
    }

    /** Testo della carta colorato con il suo colore. */
    public String getDescrizioneBreve() {
        return colore.formatta(toTestoBase());
    }

    /**
     * Testo della carta mostrata con un altro colore. Serve per la carta in cima
     * agli scarti: un jolly nero viene mostrato con il colore scelto da chi l'ha
     * giocato.
     */
    public String getDescrizioneBreve(Colore coloreVisualizzato) {
        Objects.requireNonNull(coloreVisualizzato, "coloreVisualizzato non puo essere null");
        return coloreVisualizzato.formatta(testo(coloreVisualizzato));
    }

    private String testo(Colore coloreMostrato) {
        return valore.getValore() + " " + coloreMostrato.getNome().toLowerCase(Locale.ROOT);
    }
}
