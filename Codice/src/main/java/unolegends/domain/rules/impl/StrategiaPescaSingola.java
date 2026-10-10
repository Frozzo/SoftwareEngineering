package unolegends.domain.rules.impl;

import java.util.List;
import java.util.Objects;
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
        Objects.requireNonNull(giocatore, "giocatore non puo essere null");
        Objects.requireNonNull(partita, "partita non puo essere null");

        Carta carta = partita.getMazzoDaPesca().prelevaCarta();
        if (carta == null) {
            partita.getMazzoDaPesca().rimescolaDaScarti(partita.getPilaDegliScarti());
            carta = partita.getMazzoDaPesca().prelevaCarta();
        }
        if (carta == null) {
            return List.of();
        }
        giocatore.aggiungiCarta(carta);
        return List.of(carta);
    }
}
