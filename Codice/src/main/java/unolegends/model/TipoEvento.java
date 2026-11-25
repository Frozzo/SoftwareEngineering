package unolegends.model;

/**
 * Tipo di un fatto avvenuto durante la partita che la UI deve poter raccontare.
 *
 * <p>Si aggiunge un valore quando una funzionalità nuova produce un fatto da
 * mostrare (per esempio {@code PASSIVA_ATTIVATA} con i Legend o
 * {@code GIOCATORE_ELIMINATO} con la Mercy rule).</p>
 */
public enum TipoEvento {
    /** Il giocatore ha pescato volontariamente {@code quantita} carte. */
    PESCA_VOLONTARIA,
    /** Il giocatore ha subito una pesca forzata di {@code quantita} carte per {@code origine}. */
    PESCA_FORZATA,
    /** Il giocatore salta il turno. */
    TURNO_SALTATO,
    /** Il senso di gioco si è invertito. */
    SENSO_INVERTITO,
    /** Il giocatore ha scelto {@code colore} come colore attivo. */
    COLORE_SCELTO,
    /** Il giocatore ha dichiarato UNO in tempo. */
    UNO_DICHIARATO
}
