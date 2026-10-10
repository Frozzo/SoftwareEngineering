package unolegends.domain.rules.impl;

import unolegends.domain.rules.IStrategiaCondizioneVittoria;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Regola standard: vince chi rimane senza carte in mano.
 */
public final class StrategiaVittoriaStandard implements IStrategiaCondizioneVittoria {
    @Override
    public boolean isVincitore(Giocatore giocatore, Partita partita) {
        return giocatore != null && giocatore.getMano().isEmpty();
    }
}
