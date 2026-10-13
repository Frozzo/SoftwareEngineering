package unolegends.model;

/**
 * Esito di una giocata, eventualmente sospesa in attesa di un input.
 */
public final class EsitoGiocata {
    private final boolean giocata;
    private final RichiestaInput richiestaInput;
    private final boolean MancatoUno;

    private EsitoGiocata(boolean giocata, RichiestaInput richiestaInput,boolean MancatoUno) {
        this.giocata = giocata;
        this.richiestaInput = richiestaInput;
        this.MancatoUno = MancatoUno;
    }

    public static EsitoGiocata completata(boolean giocata) {
        return completata(giocata, false);
    }

    public static EsitoGiocata completata(boolean giocata, boolean MancatoUno) {
        return new EsitoGiocata(giocata, null, MancatoUno);
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

    public boolean isMancatoUno() {
        return MancatoUno;
    }
}
