package unolegends;

import java.util.Locale;

public class CartaSpeciale extends Carta {

    private Colore colore;
    private ValoreCarta ValoreCarta;
    private Ieffetto effetto;

    public boolean compatibileCon(Carta cartaInCima) {

        if (this.colore == Colore.NERO || this.ValoreCarta == cartaInCima.getValoreCarta() || this.colore == cartaInCima.getColore()  ) { //se il colore è nero, o il ValoreCarta è uguale o il colore è uguale allora la carta è compatibile
            return true; 

        }
        else {
            return false; 
        }
    }

    public CartaSpeciale(Colore colore, ValoreCarta ValoreCarta, Ieffetto effetto) {
        this.colore = colore;
        this.ValoreCarta = ValoreCarta;
        this.effetto = effetto;
    }
    
    public Colore getColore() {
        return colore;
    }
    public void setColore(Colore colore) {
        this.colore = colore;
    }
    
    public ValoreCarta getValoreCarta() {
        return ValoreCarta;
    }
    public void setValoreCarta(ValoreCarta ValoreCarta) {
        this.ValoreCarta = ValoreCarta;
    }
    

    @Override
    public Ieffetto getEffetto() {
        return effetto;
    }
    public void setEffetto(Ieffetto effetto) {
        this.effetto = effetto;
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
