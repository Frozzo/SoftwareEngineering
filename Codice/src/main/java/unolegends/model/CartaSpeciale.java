package unolegends.model;

import java.util.Locale;
import java.util.Objects;

public class CartaSpeciale extends Carta {

    private Colore colore;
    private final ValoreCarta ValoreCarta;
    private final Ieffetto effetto;

    public CartaSpeciale(Colore colore, ValoreCarta ValoreCarta, Ieffetto effetto) {
        this.colore = Objects.requireNonNull(colore, "colore non puo essere null");
        this.ValoreCarta = Objects.requireNonNull(ValoreCarta, "valoreCarta non puo essere null");
        this.effetto = Objects.requireNonNull(effetto, "effetto non puo essere null");
    }
    
    @Override
    public Colore getColore() {
        return colore;
    }
    void setColore(Colore colore) {
        this.colore = Objects.requireNonNull(colore, "colore non puo essere null");
    }
    
    @Override
    public ValoreCarta getValoreCarta() {
        return ValoreCarta;
    }
    @Override
    public Ieffetto getEffetto() {
        return effetto;
    }

    public void attivaEffetto(Partita partita) {
        effetto.attivaeffetto(partita);
    }
        public String toTestoBase() {
        return ValoreCarta.getValore() + " " + colore.getNome().toLowerCase(Locale.ROOT);
    }

    public String toCliString() {
        String base = toTestoBase();
        return colore.formatta(base);
    }
}
