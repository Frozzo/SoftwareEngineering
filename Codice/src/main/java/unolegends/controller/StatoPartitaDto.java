package unolegends.controller;

import java.util.List;
import java.util.Objects;

/**
 * Snapshot immutabile dello stato di gioco esposto alla UI.
 *
 * <p>La {@link FaseTurnoDto} sostituisce i flag {@code deveGiocareCartaPescata} e
 * {@code partitaTerminata}: la UI non deduce più la fase combinando più campi, la
 * legge. {@code richiestaInput} è valorizzata solo in
 * {@link FaseTurnoDto#ATTESA_INPUT}, {@code cartaAppenaPescata} solo in
 * {@link FaseTurnoDto#POST_PESCA}, {@code nomeVincitore} solo in
 * {@link FaseTurnoDto#TERMINATA}.</p>
 */
public final class StatoPartitaDto {
    private final FaseTurnoDto fase;
    private final String nomeGiocatoreAttivo;
    private final List<String> manoGiocatoreAttivo;
    private final String cartaInCima;
    private final String cartaAppenaPescata;
    private final RichiestaInputDto richiestaInput;
    private final String nomeVincitore;

    public StatoPartitaDto(FaseTurnoDto fase, String nomeGiocatoreAttivo, List<String> manoGiocatoreAttivo,
                           String cartaInCima, String cartaAppenaPescata,
                           RichiestaInputDto richiestaInput, String nomeVincitore) {
        this.fase = Objects.requireNonNull(fase, "fase non puo essere null");
        this.nomeGiocatoreAttivo = Objects.requireNonNull(nomeGiocatoreAttivo, "nomeGiocatoreAttivo non puo essere null");
        this.manoGiocatoreAttivo = List.copyOf(
                Objects.requireNonNull(manoGiocatoreAttivo, "manoGiocatoreAttivo non puo essere null"));
        this.cartaInCima = cartaInCima;
        this.cartaAppenaPescata = cartaAppenaPescata;
        this.richiestaInput = richiestaInput;
        this.nomeVincitore = nomeVincitore;
    }

    public FaseTurnoDto getFase() {
        return fase;
    }

    public String getNomeGiocatoreAttivo() {
        return nomeGiocatoreAttivo;
    }

    public List<String> getManoGiocatoreAttivo() {
        return manoGiocatoreAttivo;
    }

    public String getCartaInCima() {
        return cartaInCima;
    }

    public String getCartaAppenaPescata() {
        return cartaAppenaPescata;
    }

    public RichiestaInputDto getRichiestaInput() {
        return richiestaInput;
    }

    public String getNomeVincitore() {
        return nomeVincitore;
    }
}
