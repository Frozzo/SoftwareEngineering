package unolegends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Snapshot minimale dello stato utile alla UI CLI durante il turno corrente.
 */
public class StatoTurno {
    private final String nomeGiocatoreAttivo;
    private final List<Carta> manoGiocatoreAttivo;
    private final Carta cartaInCima;
    private final boolean deveGiocareCartaPescata;
    private final Carta cartaAppenaPescata;

    public StatoTurno(String nomeGiocatoreAttivo, List<Carta> manoGiocatoreAttivo, Carta cartaInCima,
                      boolean deveGiocareCartaPescata, Carta cartaAppenaPescata) {
        this.nomeGiocatoreAttivo = Objects.requireNonNull(nomeGiocatoreAttivo, "nomeGiocatoreAttivo non puo essere null");
        this.manoGiocatoreAttivo = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(manoGiocatoreAttivo, "manoGiocatoreAttivo non puo essere null")));
        this.cartaInCima = cartaInCima;
        this.deveGiocareCartaPescata = deveGiocareCartaPescata;
        this.cartaAppenaPescata = cartaAppenaPescata;
    }

    public String getNomeGiocatoreAttivo() {
        return nomeGiocatoreAttivo;
    }

    public List<Carta> getManoGiocatoreAttivo() {
        return manoGiocatoreAttivo;
    }

    public Carta getCartaInCima() {
        return cartaInCima;
    }

    public boolean isDeveGiocareCartaPescata() {
        return deveGiocareCartaPescata;
    }

    public Carta getCartaAppenaPescata() {
        return cartaAppenaPescata;
    }
}
