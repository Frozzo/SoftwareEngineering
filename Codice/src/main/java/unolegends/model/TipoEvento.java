package unolegends.model;

/**
 * Tipo di un fatto avvenuto durante la partita che la UI deve poter raccontare.
 *
 * <p>Si aggiunge un valore quando una funzionalità nuova produce un fatto da
 * mostrare (per esempio i Legend dello Step 6).</p>
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
    TURNO_EXTRA,
    /** Una carta di pesca è finita nell'accumulo dello stacking, che ora vale {@code quantita} carte. */
    ACCUMULO_AUMENTATO,
    /** Il giocatore non risponde allo stacking e accetta di pescare le {@code quantita} carte accumulate. */
    PENALITA_ACCETTATA,
    /** Il giocatore ha usato l'abilità attiva del suo {@code legend} (Step 6). */
    ABILITA_USATA,
    /** La passiva del {@code legend} del giocatore gli ha evitato {@code quantita} carte di una pesca forzata. */
    PESCA_RIDOTTA,
    /** La carta Blocca non ha effetto sul giocatore (Pelle di ghiaccio del Freezer). */
    BLOCCO_EVITATO,
    /** Il giocatore ha congelato {@code altroGiocatore}, che salterà il prossimo turno. */
    GIOCATORE_CONGELATO,
    /** Il giocatore è congelato e salta il turno. */
    TURNO_CONGELATO,
    /** Il giocatore ha regalato {@code quantita} carte a {@code altroGiocatore} (Gifted card). */
    CARTE_REGALATE,
    /** Il giocatore ha tenuto una delle carte pescate e ne ha rimesse {@code quantita} in fondo al mazzo (Doppia pesca). */
    CARTA_SCELTA
}
