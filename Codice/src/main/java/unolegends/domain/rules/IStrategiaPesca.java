package unolegends.domain.rules;

import java.util.List;
import unolegends.model.Carta;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Strategia atomica della pesca volontaria: decide quante carte pesca il
 * giocatore quando sceglie di pescare. Restituisce le carte pescate in ordine;
 * l'ultima è quella che il giocatore può eventualmente giocare subito.
 */
@FunctionalInterface
public interface IStrategiaPesca {
    List<Carta> pesca(Giocatore giocatore, Partita partita);
}
