package unolegends.model;

/**
 * Esito di una giocata, eventualmente sospesa in attesa di un input.
 */
public final class EsitoGiocata {
    private final boolean giocata;
    private final RichiestaInput richiestaInput;
    private final boolean richiedeDichiarazioneUno;

    private EsitoGiocata(boolean giocata, RichiestaInput richiestaInput,
                         boolean richiedeDichiarazioneUno) {
        this.giocata = giocata;
        this.richiestaInput = richiestaInput;
        this.richiedeDichiarazioneUno = richiedeDichiarazioneUno;
    }

    public static EsitoGiocata completata(boolean giocata) {
        return new EsitoGiocata(giocata, null, false);
    }

    public static EsitoGiocata richiedeDichiarazioneUno() {
        return new EsitoGiocata(true, null, true);
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

    public boolean isRichiedeDichiarazioneUno() {
        return richiedeDichiarazioneUno;
    }
}
