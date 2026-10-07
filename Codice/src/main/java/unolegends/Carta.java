package unolegends;

/**
 * Entita base del dominio: carta con id.
 * Classe astratta che definisce il contratto comune per tutte le carte.
 */
public abstract class Carta {

    /**
     * Determina se questa carta e' compatibile con la carta in cima agli scarti.
     * Implementazione specifica nelle sottoclassi (polimorfismo).
     */
    public abstract boolean compatibileCon(Carta cartaInCima);
    public abstract Colore getColore();
    public abstract ValoreCarta getValoreCarta();
    public abstract Ieffetto getEffetto();

    /**
     * Rappresentazione testuale per la CLI. Implementazione specifica.
     */
    public abstract String toCliString();


}
