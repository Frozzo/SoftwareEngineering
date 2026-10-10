package unolegends.domain.rules;

import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Strategia atomica che determina la condizione di eliminazione.
 */
@FunctionalInterface
public interface IStrategiaCondizioneSconfitta {
    boolean isEliminato(Giocatore giocatore, Partita partita);
}
