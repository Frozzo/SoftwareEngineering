package unolegends.domain.rules.impl;

import unolegends.domain.rules.IStrategiaCondizioneSconfitta;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Nelle regole standard non sono previste eliminazioni per numero di carte.
 */
public final class StrategiaSconfittaStandard implements IStrategiaCondizioneSconfitta {
    @Override
    public boolean isEliminato(Giocatore giocatore, Partita partita) {
        return false;
    }
}
