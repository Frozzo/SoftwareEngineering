package unolegends.model;

/**
 * Fase in cui si trova il turno corrente.
 *
 * <p>Sostituisce le variabili sparse che prima descrivevano lo stesso concetto
 * ({@code cartaPescataDaGiocare}, {@code effettoInizialeInAttesa},
 * {@code giocatoreInAttesaDichiarazioneUno}, il controllo {@code vincitore != null}).
 * Con un solo campo la partita è sempre in esattamente una fase: le combinazioni
 * senza senso (per esempio "attesa UNO" e "carta pescata" insieme) non sono più
 * rappresentabili.</p>
 *
 * <p>Ogni comando di {@link Partita} dichiara in quali fasi è <b>permesso</b>
 * (lista di ammissione, non di esclusione): una fase aggiunta in futuro viene
 * rifiutata da tutti i comandi finché non la si abilita esplicitamente dove serve.
 * È successo con {@link #RISPOSTA_STACKING} (Step 5): l'hanno abilitata solo
 * {@code giocaCarta} e il nuovo {@code accettaPenalita}.</p>
 */
public enum FaseTurno {
    /** Inizio del turno: si può giocare una carta o pescare. */
    NORMALE,
    /** Dopo una pesca volontaria: si può giocare una carta della mano o passare. */
    POST_PESCA,
    /** Un effetto aspetta una risposta: si accetta solo {@link Partita#rispondiInput(String)}. */
    ATTESA_INPUT,
    /** Il giocatore è rimasto con una carta: si accetta solo la dichiarazione UNO. */
    ATTESA_UNO,
    /**
     * C'è un accumulo di pesca (stacking, Step 5) e il giocatore di turno deve
     * rispondere: può giocare solo una carta di pesca di valore sufficiente
     * ({@link Partita#giocaCarta(int)}) oppure accettare la penalità
     * ({@link Partita#accettaPenalita()}). Non si può pescare volontariamente.
     */
    RISPOSTA_STACKING,
    /** La partita è finita: nessun comando è accettato. */
    TERMINATA
}
