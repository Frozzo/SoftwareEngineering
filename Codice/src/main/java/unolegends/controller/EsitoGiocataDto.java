package unolegends.controller;

/**
 * Esito di una giocata esposto alla UI senza tipi del dominio.
 */
public final class EsitoGiocataDto {
    private final boolean giocata;
    private final RichiestaInputDto richiestaInput;
    private final boolean richiedeDichiarazioneUno;

    public EsitoGiocataDto(boolean giocata, RichiestaInputDto richiestaInput,
                           boolean richiedeDichiarazioneUno) {
        this.giocata = giocata;
        this.richiestaInput = richiestaInput;
        this.richiedeDichiarazioneUno = richiedeDichiarazioneUno;
    }

    public boolean isGiocata() {
        return giocata;
    }

    public RichiestaInputDto getRichiestaInput() {
        return richiestaInput;
    }

    public boolean isRichiedeDichiarazioneUno() {
        return richiedeDichiarazioneUno;
    }
}
