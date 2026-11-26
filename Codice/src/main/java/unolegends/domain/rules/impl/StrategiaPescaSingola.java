package unolegends.domain.rules.impl;

import java.util.List;
import unolegends.domain.rules.IStrategiaPesca;
import unolegends.model.Carta;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Regola standard della pesca volontaria: il giocatore pesca una sola carta.
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

    /** Con la pesca di una sola carta vale la passiva Doppia pesca (Step 6). */
    @Override
    public boolean ammettePescaConScelta() {
        return true;
    }
}
