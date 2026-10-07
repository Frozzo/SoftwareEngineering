package unolegends;

public class CartaSpeciale extends Carta {

    private Ieffetto effetto;

    public CartaSpeciale(String colore, String valore, Ieffetto effetto) {
        super(colore, valore);
        this.effetto = effetto;
    }

    public void attivaEffetto(Partita partita) {
        effetto.attivaeffetto(partita);
    }
    
}
