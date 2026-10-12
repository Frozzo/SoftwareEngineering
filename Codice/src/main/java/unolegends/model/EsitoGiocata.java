package unolegends.model;

/**
 * Esito di una giocata, eventualmente sospesa in attesa di un input.
 */
public final class EsitoGiocata {
    private final boolean giocata;
    private final RichiestaInput richiestaInput;

    private EsitoGiocata(boolean giocata, RichiestaInput richiestaInput) {
        this.giocata = giocata;
        this.richiestaInput = richiestaInput;
    }

    public static EsitoGiocata completata(boolean giocata) {
        return new EsitoGiocata(giocata, null);
    }

    public static EsitoGiocata inAttesa(RichiestaInput richiestaInput) {
        if (richiestaInput == null) {
            throw new IllegalArgumentException("richiestaInput non puo essere null");
        }
        return new EsitoGiocata(false, richiestaInput);
    }

    public boolean isGiocata() {
        return giocata;
    }

    public RichiestaInput getRichiestaInput() {
        return richiestaInput;
    }
}
