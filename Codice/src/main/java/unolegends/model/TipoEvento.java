package unolegends.model;

/**
 * Tipo di un fatto avvenuto durante la partita che la UI deve poter raccontare.
 *
 * <p>Si aggiunge un valore quando una funzionalità nuova produce un fatto da
 * mostrare (per esempio {@code PASSIVA_ATTIVATA} con i Legend).</p>
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
    UNO_DICHIARATO,
    /** Il giocatore ha finito le carte e si classifica in posizione {@code quantita}. */
    GIOCATORE_TERMINATO,
    /** Il giocatore è stato eliminato e le sue carte sono tornate nel mazzo. */
    GIOCATORE_ELIMINATO,
    /** Il giocatore ha scambiato la mano con {@code altroGiocatore} (carta 7). */
    MANI_SCAMBIATE,
    /** Tutti i giocatori in gioco hanno passato la mano al successivo (carta 0). */
    MANI_PASSATE,
    /** Il giocatore ha scartato {@code quantita} carte di colore {@code colore} (Scarta tutto). */
    CARTE_SCARTATE,
    /** Tutti gli altri saltano e il giocatore gioca un altro turno (Salta tutti). */
    TURNO_EXTRA
}
