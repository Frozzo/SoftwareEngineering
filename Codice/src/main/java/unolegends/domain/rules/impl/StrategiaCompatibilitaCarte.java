package unolegends.domain.rules.impl;

import unolegends.domain.rules.IStrategiaCompatibilitaCarte;
import unolegends.model.Carta;
import unolegends.model.Colore;

/**
 * Strategia standard per verificare la compatibilità delle carte.
 */
public final class StrategiaCompatibilitaCarte implements IStrategiaCompatibilitaCarte {


    public StrategiaCompatibilitaCarte() {
    }
    @Override
    public boolean puoGiocare(Carta cartaGiocata, Carta cartaInCima, Colore coloreAttivo) {
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
