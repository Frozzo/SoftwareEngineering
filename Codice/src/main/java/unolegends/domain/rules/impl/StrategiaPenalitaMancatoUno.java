package unolegends.domain.rules.impl;
import unolegends.domain.rules.IStrategiaPenalitaUno;
import unolegends.model.Giocatore;
import unolegends.model.OriginePesca;
import unolegends.model.Partita;

/**
 * Penalita standard per mancata dichiarazione di UNO: due carte.
 * La pesca passa da {@link Partita#applicaPescaForzata}, così anche questa
 * penalità può essere modificata dalle regole (es. abilità passive).
 */
public final class StrategiaPenalitaMancatoUno implements IStrategiaPenalitaUno {
    public static final int CARTE_PENALITA_STANDARD = 2;

    private final int numeroCarte;

    public StrategiaPenalitaMancatoUno() {
        this(CARTE_PENALITA_STANDARD);
    }

    public StrategiaPenalitaMancatoUno(int numeroCarte) {
        this.numeroCarte = numeroCarte;
    }

    @Override
    public void applica(Giocatore giocatore, Partita partita) {
        partita.applicaPescaForzata(giocatore, numeroCarte, OriginePesca.PENALITA_UNO);
    }
}
