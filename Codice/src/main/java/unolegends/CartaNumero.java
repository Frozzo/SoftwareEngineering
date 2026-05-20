package unolegends;

import java.util.Locale;

/**
 * Carta numerica standard: estende `Carta` aggiungendo colore e numero.
 */
public class CartaNumero extends Carta {
    private final Colore colore;
    private final int numero;

    public CartaNumero(String id, Colore colore, int numero) {
        super(id);
        this.colore = java.util.Objects.requireNonNull(colore, "colore non puo essere null");
        this.numero = numero;
    }

    public Colore getColore() {
        return colore;
    }

    public int getNumero() {
        return numero;
    }

    @Override
    public boolean compatibileCon(Carta cartaInCima) {
        if (cartaInCima == null) return true;
        if (cartaInCima instanceof CartaNumero other) {
            return this.colore == other.colore || this.numero == other.numero;
        }
        // Altri tipi di carta possono implementare proprie regole
        return false;
    }

    public String toTestoBase() {
        return numero + " " + colore.getNome().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toCliString() {
        String base = toTestoBase();
        return colore.formatta(base);
    }
}
