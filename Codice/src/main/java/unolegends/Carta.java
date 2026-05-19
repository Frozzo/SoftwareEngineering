package unolegends;

import java.util.Locale;
import java.util.Objects;

/**
 * Entita base del dominio: carta con id, colore e numero.
 */
public class Carta {
    private final String id;
    private final Colore colore;
    private final int numero;

    public Carta(String id, Colore colore, int numero) {
        this.id = Objects.requireNonNull(id, "id non puo essere null");
        this.colore = Objects.requireNonNull(colore, "colore non puo essere null");
        this.numero = numero;
    }

    public String getId() {
        return id;
    }

    public Colore getColore() {
        return colore;
    }

    public int getNumero() {
        return numero;
    }

    public boolean compatibileCon(Carta cartaInCima) {
        return this.colore == cartaInCima.colore || this.numero == cartaInCima.numero;
    }

    public String toTestoBase() {
        return numero + " " + colore.getNome().toLowerCase(Locale.ROOT);
    }

    public String toCliString() {
        String base = toTestoBase();
        return colore.formatta(base);
    }

    @Override
    public String toString() {
        return toCliString();
    }
}
