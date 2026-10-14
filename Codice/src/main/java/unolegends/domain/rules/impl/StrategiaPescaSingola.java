package unolegends.domain.rules.impl;

import java.util.List;
import unolegends.domain.rules.IStrategiaPesca;
import unolegends.model.Carta;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Regola standard: il giocatore pesca una sola carta.
 */
public final class StrategiaPescaSingola implements IStrategiaPesca {
    @Override
    public List<Carta> pesca(Giocatore giocatore, Partita partita) {

        Carta carta = partita.pescaSingola(giocatore);
        if (carta == null) {
            return List.of();
        }
        return List.of(carta);
    }
}
