package unolegends.domain.rules;

import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Strategia che applica la penalita per mancata dichiarazione di UNO.
 */
@FunctionalInterface
public interface IStrategiaPenalitaUno {
    void applica(Giocatore giocatore, Partita partita);
}
