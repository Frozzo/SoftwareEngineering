package unolegends;

/**
 * Entita base del dominio: carta con id.
 * Classe astratta che definisce il contratto comune per tutte le carte.
 */
public  class Carta {  //da modificare il concetto di carta che da astratta diventa concreta, in quanto non ci sono carte che non hanno colore e valore (le carte speciali le facciamo nere), quindi non ha senso avere una carta astratta senza colore e valore.
                        //tocca pero implementare bene la funzione compatibileCon, che ora e' implementata in maniera generica, ma va implementata in maniera che vada bene per ogni tipo di carta
    private final String id;

    protected Carta(String id) {
        this.id = java.util.Objects.requireNonNull(id, "id non puo essere null");
    }

    public String getId() {
        return id;
    }

    /**
     * Determina se questa carta e' compatibile con la carta in cima agli scarti.
     * Implementazione specifica nelle sottoclassi (polimorfismo).
     */
    public boolean compatibileCon(Carta cartaInCima);
            {
            // se carta jolly colore nero, puo essere giocata sempre
            //se la carta ha lo stesso colore della carta in cima agli scarti, puo essere giocata (ma se è cambio gire ad esempio come faccio)
            //se la carta ha lo stesso valore della carta in cima agli scarti, puo essere giocata    
                return true;}

    /**
     * Rappresentazione testuale per la CLI. Implementazione specifica.
     */
    public abstract String toCliString();

    @Override
    public String toString() {
        return toCliString();
    }
}
