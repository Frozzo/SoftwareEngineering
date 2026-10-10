package unolegends.domain.rules;

import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Strategia atomica che determina la condizione di vittoria.
 */
@FunctionalInterface
public interface IStrategiaCondizioneVittoria {
    boolean isVincitore(Giocatore giocatore, Partita partita);
}
