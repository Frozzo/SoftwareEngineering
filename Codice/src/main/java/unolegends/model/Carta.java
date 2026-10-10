package unolegends.model;

/**
 * Entita base del dominio: carta con id.
 * Classe astratta che definisce il contratto comune per tutte le carte.
 */
public abstract class Carta {

    public abstract Colore getColore();
    public abstract ValoreCarta getValoreCarta();
    public abstract Ieffetto getEffetto();

    /**
     * Rappresentazione testuale per la CLI. Implementazione specifica.
     */
    public abstract String toCliString();


}
