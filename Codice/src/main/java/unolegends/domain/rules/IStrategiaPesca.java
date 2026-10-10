package unolegends.domain.rules;

import java.util.List;
import unolegends.model.Carta;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Strategia atomica che decide quante carte assegnare a un giocatore.
 */
@FunctionalInterface
public interface IStrategiaPesca {
    List<Carta> pesca(Giocatore giocatore, Partita partita);
}
