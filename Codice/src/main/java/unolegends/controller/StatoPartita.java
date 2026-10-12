package unolegends.controller;

import java.util.List;
import java.util.Objects;

import unolegends.model.RichiestaInput;

/**
 * Snapshot immutabile dello stato di gioco e dell'eventuale richiesta d'interazione.
 */
public final class StatoPartita {
    private final String nomeGiocatoreAttivo;
    private final List<String> manoGiocatoreAttivo;
    private final boolean richiedeDichiarazioneUno;
    private final String cartaInCima;
    private final boolean deveGiocareCartaPescata;
    private final String cartaAppenaPescata;
    private final RichiestaInput richiestaInput;
    private final boolean partitaTerminata;
    private final String nomeVincitore;

    public StatoPartita(String nomeGiocatoreAttivo, List<String> manoGiocatoreAttivo,
                        boolean richiedeDichiarazioneUno,
                        String cartaInCima, boolean deveGiocareCartaPescata,
                        String cartaAppenaPescata, RichiestaInput richiestaInput,
                        boolean partitaTerminata, String nomeVincitore) {
        this.nomeGiocatoreAttivo = Objects.requireNonNull(nomeGiocatoreAttivo, "nomeGiocatoreAttivo non puo essere null");
        this.manoGiocatoreAttivo = List.copyOf(
                Objects.requireNonNull(manoGiocatoreAttivo, "manoGiocatoreAttivo non puo essere null"));
        this.richiedeDichiarazioneUno = richiedeDichiarazioneUno;
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

    public boolean isRichiedeDichiarazioneUno() {
        return richiedeDichiarazioneUno;
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

    public RichiestaInput getRichiestaInput() {
        return richiestaInput;
    }

    public boolean isPartitaTerminata() {
        return partitaTerminata;
    }

    public String getNomeVincitore() {
        return nomeVincitore;
    }
}
