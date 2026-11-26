package unolegends.model;

import java.util.Objects;

/**
 * Accumulo dello stacking: le carte di pesca giocate una sopra l'altra che
 * qualcuno dovrà pescare (Step 5, scelta 5.B).
 *
 * <p>Non basta un {@code int}: per le regole e per le passive dei Legend servono
 * tre informazioni diverse.</p>
 * <ul>
 *   <li>{@code totale}: quante carte pesca chi accetta;</li>
 *   <li>{@code valoreUltimaCarta}: la soglia per rispondere (si risponde con un
 *       valore uguale o superiore a quello dell'<b>ultima</b> carta, non del totale);</li>
 *   <li>{@code numeroCarte} e {@code originePrimaCarta}: distinguono "un +2 da solo"
 *       da un accumulo vero, come serve a La Roccia (Step 6), immune solo al +2
 *       singolo.</li>
 * </ul>
 *
 * <p>È un {@code record} immutabile: {@link Partita} lo sostituisce con
 * {@link #aggiungi(int)} a ogni risposta e lo azzera quando qualcuno accetta.
 * Così non esistono stati intermedi incoerenti.</p>
 */
public record Accumulo(int totale, int valoreUltimaCarta, int numeroCarte, OriginePesca originePrimaCarta) {

    public Accumulo {
        Objects.requireNonNull(originePrimaCarta, "originePrimaCarta non puo essere null");
        if (totale <= 0 || valoreUltimaCarta <= 0 || numeroCarte <= 0) {
            throw new IllegalArgumentException("un accumulo contiene almeno una carta di pesca");
        }
    }

    /** Accumulo aperto dalla prima carta di pesca. */
    static Accumulo apri(int valore, OriginePesca origine) {
        return new Accumulo(valore, valore, 1, origine);
    }

    /** Nuovo accumulo dopo una risposta di quel valore. */
    Accumulo aggiungi(int valore) {
        return new Accumulo(totale + valore, valore, numeroCarte + 1, originePrimaCarta);
    }

    /**
     * Causa della pesca per chi accetta: se l'accumulo è una sola carta resta
     * quella carta ({@code PIU_DUE}, {@code PIU_QUATTRO}...), altrimenti è
     * {@link OriginePesca#ACCUMULO}. Così La Roccia (Step 6) potrà essere immune al
     * +2 singolo ma non a un accumulo (decisione presa).
     */
    public OriginePesca getOriginePenalita() {
        return numeroCarte == 1 ? originePrimaCarta : OriginePesca.ACCUMULO;
    }
}
