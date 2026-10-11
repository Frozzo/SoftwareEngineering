package unolegends.domain.rules.impl;

import unolegends.domain.rules.IStrategiaCondizioneVittoria;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Vittoria No Mercy: mano vuota oppure tutti gli avversari sopra la soglia.
 */
public final class StrategiaVittoriaNoMercy implements IStrategiaCondizioneVittoria {
    public static final int SOGLIA_STANDARD = 25;

    private final int sogliaCarteAvversari;

    public StrategiaVittoriaNoMercy() {
        this(SOGLIA_STANDARD);
    }

    public StrategiaVittoriaNoMercy(int sogliaCarteAvversari) {
        if (sogliaCarteAvversari < 0) {
            throw new IllegalArgumentException("La soglia delle carte non puo essere negativa");
        }
        this.sogliaCarteAvversari = sogliaCarteAvversari;
    }

    @Override
    public boolean isVincitore(Giocatore giocatore, Partita partita) {
        return giocatore != null && partita != null
                && (giocatore.getMano().isEmpty()
                || partita.tuttiGliAltriGiocatoriHannoPiuDi(giocatore, sogliaCarteAvversari));
    }
}
