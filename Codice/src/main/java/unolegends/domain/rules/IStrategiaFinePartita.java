package unolegends.domain.rules;

import unolegends.model.Partita;

/**
 * Strategia che decide quando la partita è finita.
 *
 * <p>È il punto di variazione fra partita normale (finisce appena qualcuno
 * esaurisce le carte) ed Endless (si continua finché resta un solo giocatore,
 * per stilare la classifica completa). {@code Partita} la interroga ogni volta
 * che un giocatore esce di scena, senza sapere quale opzione è attiva
 * (Protected Variations).</p>
 */
@FunctionalInterface
public interface IStrategiaFinePartita {
    boolean isPartitaTerminata(Partita partita);
}
