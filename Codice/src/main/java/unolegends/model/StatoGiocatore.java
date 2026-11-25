package unolegends.model;

/**
 * Situazione di un giocatore rispetto alla partita.
 *
 * <p>Con 2 giocatori bastava sapere chi vinceva; con N giocatori, Endless ed
 * eliminazioni un giocatore può uscire di scena mentre gli altri continuano.
 * Lo scorrimento dei turni ({@code Partita.trovaProssimoIndiceInGioco}) e la
 * scelta dei bersagli considerano solo i giocatori {@link #IN_GIOCO}.</p>
 *
 * <p>Il valore si chiama {@code IN_GIOCO} e non {@code ATTIVO} perché nel
 * progetto "giocatore attivo" indica già il giocatore di turno.</p>
 */
public enum StatoGiocatore {
    /** Partecipa ancora ai turni. */
    IN_GIOCO,
    /** Ha finito le carte e ha preso posto in classifica (in Endless la partita continua senza di lui). */
    TERMINATO,
    /** È stato eliminato (Mercy rule, Step 4): finisce in fondo alla classifica. */
    ELIMINATO
}
