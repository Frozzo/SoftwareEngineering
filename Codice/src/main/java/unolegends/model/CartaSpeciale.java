package unolegends.model;

import java.util.Locale;
import java.util.Objects;

public class CartaSpeciale extends Carta {

    private final Colore colore;
    private final ValoreCarta valoreCarta;
    private final Ieffetto effetto;

    public CartaSpeciale(Colore colore, ValoreCarta valoreCarta, Ieffetto effetto) {
        this.colore = Objects.requireNonNull(colore, "colore non puo essere null");
        this.valoreCarta = Objects.requireNonNull(valoreCarta, "valoreCarta non puo essere null");
        this.effetto = Objects.requireNonNull(effetto, "effetto non puo essere null");
    }
    
    @Override
    public Colore getColore() {
        return colore;
    }
    @Override
    public ValoreCarta getValoreCarta() {
        return valoreCarta;
    }
    @Override
    public Ieffetto getEffetto() {
        return effetto;
    }

    public void attivaEffetto(Partita partita) {
        effetto.attivaEffetto(partita);
    }
        public String toTestoBase() {
        return valoreCarta.getValore() + " " + colore.getNome().toLowerCase(Locale.ROOT);
    }

    public String getDescrizioneBreve() {
        String base = toTestoBase();
        return colore.formatta(base);
    }
}
