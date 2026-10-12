package unolegends.model;

/**
 * Esito di una giocata, eventualmente sospesa in attesa di un input.
 */
public final class EsitoGiocata {
    private final boolean giocata;
    private final RichiestaInput richiestaInput;
    private final boolean penalizzataPerMancatoUno;

    private EsitoGiocata(boolean giocata, RichiestaInput richiestaInput,
                         boolean penalizzataPerMancatoUno) {
        this.giocata = giocata;
        this.richiestaInput = richiestaInput;
        this.penalizzataPerMancatoUno = penalizzataPerMancatoUno;
    }

    public static EsitoGiocata completata(boolean giocata) {
        return completata(giocata, false);
    }

    public static EsitoGiocata completata(boolean giocata, boolean penalizzataPerMancatoUno) {
        return new EsitoGiocata(giocata, null, penalizzataPerMancatoUno);
    }

    public static EsitoGiocata inAttesa(RichiestaInput richiestaInput) {
        if (richiestaInput == null) {
            throw new IllegalArgumentException("richiestaInput non puo essere null");
        }
        return new EsitoGiocata(false, richiestaInput, false);
    }

    public boolean isGiocata() {
        return giocata;
    }

    public RichiestaInput getRichiestaInput() {
        return richiestaInput;
    }

    public boolean isPenalizzataPerMancatoUno() {
        return penalizzataPerMancatoUno;
    }
}
