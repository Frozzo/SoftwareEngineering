package unolegends.domain.rules.impl;

import unolegends.domain.rules.IStrategiaAccumuloPenalita;
import unolegends.model.Accumulo;
import unolegends.model.Carta;

/**
 * Partita senza stacking: ogni carta di pesca si applica subito, il giocatore
 * successivo pesca e salta il turno. È il comportamento che il gioco aveva fino
 * allo Step 4.
 *
 * <p>{@link #puoRispondere} restituisce sempre false: senza accumulo non c'è
 * niente a cui rispondere, quindi il metodo non viene mai chiamato davvero.</p>
 */
public class StrategiaNessunAccumulo implements IStrategiaAccumuloPenalita {

    @Override
    public boolean isAccumuloAttivo() {
        return false;
    }

    @Override
    public boolean puoRispondere(Carta candidata, Accumulo accumulo) {
        return false;
    }
}
