package unolegends.model;

/**
 * Abstract factory per creare le famiglie di oggetti legate al mazzo.
 */
public abstract interface MazzoFactory {


    /**
     * Crea un Mazzo con le carte specifiche della famiglia concreta.
     * //@deprecated Usare {@link #creaMazzo()} per ottenere un oggetto Mazzo completo.
     */
    public abstract Mazzo creaMazzo();
}