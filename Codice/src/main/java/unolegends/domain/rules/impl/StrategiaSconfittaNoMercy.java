package unolegends.domain.rules.impl;

import unolegends.domain.rules.IStrategiaCondizioneSconfitta;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Mercy rule della No Mercy: un giocatore che arriva ad avere almeno
 * {@code sogliaCarte} carte in mano (25 nella regola ufficiale) è eliminato.
 *
 * <p>La soglia è un parametro del costruttore e non una costante, così si può
 * provare il gioco con valori diversi senza toccare la classe.</p>
 */
public final class StrategiaSconfittaNoMercy implements IStrategiaCondizioneSconfitta {
    private final int sogliaCarte;

    public StrategiaSconfittaNoMercy(int sogliaCarte) {
        if (sogliaCarte <= 0) {
            throw new IllegalArgumentException("sogliaCarte deve essere positiva");
        }
        this.sogliaCarte = sogliaCarte;
    }

    @Override
    public boolean isEliminato(Giocatore giocatore, Partita partita) {
        return giocatore != null && giocatore.getMano().size() >= sogliaCarte;
    }
}
