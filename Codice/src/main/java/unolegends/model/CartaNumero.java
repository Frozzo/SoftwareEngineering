package unolegends.model;

import java.util.Locale;

/**
 * Carta numerica standard: estende `Carta` aggiungendo colore e numero.
 */
public class CartaNumero extends Carta {
    private final Colore colore;
    private final ValoreCarta valore;
    private final int id;
    private final Ieffetto effetto;

    public CartaNumero(int id, Colore colore, ValoreCarta valore) {
        this.id = id;
        this.colore = java.util.Objects.requireNonNull(colore, "colore non puo essere null");
        this.valore = java.util.Objects.requireNonNull(valore, "valore non puo essere null");
        this.effetto = new Effetto_Nullo();
        
    }

    public Colore getColore() {
        return colore;
    }

    public ValoreCarta getValoreCarta() {
        return this.valore;
    }

    @Override
    public Ieffetto getEffetto() {
        return effetto;
    }

    public boolean compatibileCon(Carta cartaInCima) {
        return cartaInCima == null
                || colore == cartaInCima.getColore()
                || valore == cartaInCima.getValoreCarta();
    }

    public String toTestoBase() {
        return valore.getValore() + " " + colore.getNome().toLowerCase(Locale.ROOT);
    }

    public String toCliString() {
        String base = toTestoBase();
        return colore.formatta(base);
    }
}
