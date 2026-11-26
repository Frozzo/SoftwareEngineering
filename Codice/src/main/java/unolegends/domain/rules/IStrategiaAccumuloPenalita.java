package unolegends.domain.rules;

import unolegends.model.Accumulo;
import unolegends.model.Carta;

/**
 * Strategia dello stacking (Step 5): stabilisce se le carte di pesca si
 * accumulano e con quali carte si può rispondere a un accumulo.
 *
 * <p>È il punto di variazione che permette agli effetti di pesca di non sapere se
 * lo stacking è attivo (Protected Variations): {@code Partita} chiede a
 * {@code RegoleDiGioco}, che delega a questa strategia.</p>
 */
public interface IStrategiaAccumuloPenalita {

    /**
     * true se una carta di pesca va aggiunta all'accumulo invece di far pescare
     * subito il giocatore successivo.
     */
    boolean isAccumuloAttivo();

    /**
     * true se la carta candidata può rispondere all'accumulo in corso. Non riguarda
     * il colore: conta solo il valore di pesca (decisione presa).
     */
    boolean puoRispondere(Carta candidata, Accumulo accumulo);
}
