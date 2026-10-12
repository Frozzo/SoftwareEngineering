package unolegends.domain.rules;

import unolegends.model.Carta;
import unolegends.model.Colore;

/**
 * Strategia atomica per stabilire se una carta è giocabile.
 */
@FunctionalInterface
public interface IStrategiaCompatibilitaCarte {
    boolean puoGiocare(Carta cartaGiocata, Carta cartaInCima, Colore coloreAttivo);
}
