package unolegends.domain.rules.impl;

import unolegends.domain.rules.IStrategiaFinePartita;
import unolegends.model.Partita;

/**
 * Opzione Endless: chi finisce le carte prende posto in classifica e la partita
 * continua fra gli altri; finisce quando resta un solo giocatore in gioco, che
 * prende l'ultimo posto fra i non eliminati.
 */
public final class StrategiaFinePartitaEndless implements IStrategiaFinePartita {
    @Override
    public boolean isPartitaTerminata(Partita partita) {
        return partita.getNumeroGiocatoriInGioco() <= 1;
    }
}
