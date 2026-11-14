package unolegends.controller;

import java.util.List;
import java.util.Objects;

/**
 * Snapshot immutabile dello stato di gioco e dell'eventuale richiesta d'interazione.
 */
public final class StatoPartitaDto {
    private final String nomeGiocatoreAttivo;
    private final List<String> manoGiocatoreAttivo;
    private final String cartaInCima;
    private final boolean deveGiocareCartaPescata;
    private final String cartaAppenaPescata;
    private final RichiestaInputDto richiestaInput;
    private final boolean partitaTerminata;
    private final String nomeVincitore;

    public StatoPartitaDto(String nomeGiocatoreAttivo, List<String> manoGiocatoreAttivo,
                           String cartaInCima, boolean deveGiocareCartaPescata,
                           String cartaAppenaPescata, RichiestaInputDto richiestaInput,
                        boolean partitaTerminata, String nomeVincitore) {
        this.nomeGiocatoreAttivo = Objects.requireNonNull(nomeGiocatoreAttivo, "nomeGiocatoreAttivo non puo essere null");
        this.manoGiocatoreAttivo = List.copyOf(
                Objects.requireNonNull(manoGiocatoreAttivo, "manoGiocatoreAttivo non puo essere null"));
        this.cartaInCima = cartaInCima;
        this.deveGiocareCartaPescata = deveGiocareCartaPescata;
        this.cartaAppenaPescata = cartaAppenaPescata;
        this.richiestaInput = richiestaInput;
        this.partitaTerminata = partitaTerminata;
        this.nomeVincitore = nomeVincitore;
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

    public boolean isDeveGiocareCartaPescata() {
        return deveGiocareCartaPescata;
    }

    public String getCartaAppenaPescata() {
        return cartaAppenaPescata;
    }

    public RichiestaInputDto getRichiestaInput() {
        return richiestaInput;
    }

    public boolean isPartitaTerminata() {
        return partitaTerminata;
    }

    public String getNomeVincitore() {
        return nomeVincitore;
    }
}
