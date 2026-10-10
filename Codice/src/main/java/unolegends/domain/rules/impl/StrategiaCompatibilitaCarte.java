package unolegends.domain.rules.impl;

import unolegends.domain.rules.IStrategiaCompatibilitaCarte;
import unolegends.model.Carta;
import unolegends.model.Colore;
import unolegends.model.StatoTurno;

/**
 * Strategia standard per verificare la compatibilità delle carte.
 */
public final class StrategiaCompatibilitaCarte implements IStrategiaCompatibilitaCarte {
    private final boolean penalitaUnoAttiva;

    public StrategiaCompatibilitaCarte() {
        this(true);
    }

    public StrategiaCompatibilitaCarte(boolean penalitaUnoAttiva) {
        this.penalitaUnoAttiva = penalitaUnoAttiva;
    }

    @Override
    public boolean puoGiocare(Carta cartaGiocata, Carta cartaInCima, Colore coloreAttivo,
                              StatoTurno statoTurno) {
        if (cartaGiocata == null) {
            return false;
        }
        if (cartaInCima == null) {
            return true;
        }
        if (cartaGiocata.getColore() == Colore.NERO) {
            return true;
        }
        Colore coloreDaConfrontare = coloreAttivo != null
                ? coloreAttivo
                : cartaInCima.getColore();
        return cartaGiocata.getColore() == coloreDaConfrontare
                || cartaGiocata.getValoreCarta() == cartaInCima.getValoreCarta();
    }
}
