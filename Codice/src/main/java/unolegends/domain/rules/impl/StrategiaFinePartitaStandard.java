package unolegends.domain.rules.impl;

import unolegends.domain.rules.IStrategiaFinePartita;
import unolegends.model.Partita;

/**
 * Regola normale: la partita finisce appena un giocatore esaurisce le carte,
 * oppure quando resta un solo giocatore in gioco (gli altri sono eliminati).
 */
public final class StrategiaFinePartitaStandard implements IStrategiaFinePartita {
    @Override
    public boolean isPartitaTerminata(Partita partita) {
        return partita.getNumeroGiocatoriTerminati() >= 1 || partita.getNumeroGiocatoriInGioco() <= 1;
    }
}
