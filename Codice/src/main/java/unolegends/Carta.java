package unolegends;

/**
 * Entita base del dominio: carta con id.
 * Classe astratta che definisce il contratto comune per tutte le carte.
 */
public abstract class Carta {
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
    public abstract boolean compatibileCon(Carta cartaInCima);

    /**
     * Rappresentazione testuale per la CLI. Implementazione specifica.
     */
    public abstract String toCliString();

    @Override
    public String toString() {
        return toCliString();
    }
}
