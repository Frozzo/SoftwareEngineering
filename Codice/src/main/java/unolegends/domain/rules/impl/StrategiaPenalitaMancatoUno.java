package unolegends.domain.rules.impl;

import java.util.Objects;

import unolegends.domain.rules.IStrategiaPenalitaUno;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Penalita standard per mancata dichiarazione di UNO: due carte.
 */
public final class StrategiaPenalitaMancatoUno implements IStrategiaPenalitaUno {
    public static final int CARTE_PENALITA_STANDARD = 2;

    private final int numeroCarte;

    public StrategiaPenalitaMancatoUno() {
        this(CARTE_PENALITA_STANDARD);
    }

    public StrategiaPenalitaMancatoUno(int numeroCarte) {
        if (numeroCarte < 0) {
            throw new IllegalArgumentException("Il numero di carte penalita non puo essere negativo");
        }
        this.numeroCarte = numeroCarte;
    }

    @Override
    public void applica(Giocatore giocatore, Partita partita) {
        Objects.requireNonNull(giocatore, "giocatore non puo essere null");
        Objects.requireNonNull(partita, "partita non puo essere null");
        for (int i = 0; i < numeroCarte; i++) {
            if (partita.pescaSingola(giocatore) == null) {
                break;
            }
        }
    }
}
