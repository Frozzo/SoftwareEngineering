package unolegends.controller;

import java.util.List;
import java.util.Objects;

/**
 * Snapshot dello stato di gioco esposto alla UI senza riferimenti al dominio.
 */
public final class StatoPartita {
    private final String nomeGiocatoreAttivo;
    private final List<String> manoGiocatoreAttivo;
    private final String cartaInCima;
    private final boolean deveGiocareCartaPescata;
    private final String cartaAppenaPescata;
    private final boolean deveScegliereColoreIniziale;

    public StatoPartita(String nomeGiocatoreAttivo, List<String> manoGiocatoreAttivo,
                        String cartaInCima, boolean deveGiocareCartaPescata,
                        String cartaAppenaPescata, boolean deveScegliereColoreIniziale) {
        this.nomeGiocatoreAttivo = Objects.requireNonNull(nomeGiocatoreAttivo, "nomeGiocatoreAttivo non puo essere null");
        this.manoGiocatoreAttivo = List.copyOf(
                Objects.requireNonNull(manoGiocatoreAttivo, "manoGiocatoreAttivo non puo essere null"));
        this.cartaInCima = cartaInCima;
        this.deveGiocareCartaPescata = deveGiocareCartaPescata;
        this.cartaAppenaPescata = cartaAppenaPescata;
        this.deveScegliereColoreIniziale = deveScegliereColoreIniziale;
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

    public boolean isDeveScegliereColoreIniziale() {
        return deveScegliereColoreIniziale;
    }
}
