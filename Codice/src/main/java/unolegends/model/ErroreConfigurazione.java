package unolegends.model;

/**
 * Motivo per cui una {@link ConfigurazionePartita} non è valida.
 *
 * <p>Il model non produce testo per l'utente: segnala il motivo con questo enum e
 * la frase la scrive il controller, come per gli eventi e le richieste di input.</p>
 */
public enum ErroreConfigurazione {
    /** Il numero di giocatori è fuori dall'intervallo ammesso. */
    NUMERO_GIOCATORI_NON_VALIDO,
    /** Un nome è vuoto. */
    NOME_VUOTO,
    /** Due giocatori hanno lo stesso nome (senza distinguere maiuscole e minuscole). */
    NOMI_DUPLICATI,
    /** Endless richiede almeno 3 giocatori: in due, quando il primo finisce la partita è già conclusa. */
    GIOCATORI_INSUFFICIENTI_PER_ENDLESS,
    /** Con l'opzione Legend ogni giocatore deve avere esattamente un Legend. */
    LEGEND_MANCANTI,
    /** Sono stati scelti dei Legend senza attivare l'opzione Legend. */
    LEGEND_SENZA_OPZIONE,
    /** Due giocatori hanno scelto lo stesso Legend (decisione presa: ogni Legend una volta sola). */
    LEGEND_DUPLICATI
}
