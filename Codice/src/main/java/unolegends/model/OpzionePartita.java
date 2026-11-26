package unolegends.model;

/**
 * Regole opzionali che si possono aggiungere a una modalità di gioco dal menu.
 *
 * <p>È un enum di soli dati: cosa cambia concretamente un'opzione lo decide
 * {@link RegoleDiGioco} quando sceglie le strategie ({@link RegoleDiGioco#standard(java.util.Set)}).
 * Nella {@link ConfigurazionePartita} le opzioni stanno in un {@code Set}, quindi
 * aggiungerne una non aggiunge campi.</p>
 *
 * <p>Contiene solo le opzioni già implementate, così il menu non offre scelte che
 * non fanno nulla.</p>
 */
public enum OpzionePartita {
    /** Chi finisce le carte esce e si continua finché resta un solo giocatore: alla fine c'è la classifica. */
    ENDLESS,
    /**
     * Stacking (Step 5): a una carta di pesca si risponde con una carta di pesca di
     * valore uguale o superiore; le carte si accumulano e chi non risponde le pesca
     * tutte. Configurabile nella partita normale, obbligatoria in No Mercy.
     */
    STACKING,
    /**
     * Legend (Step 6): ogni giocatore sceglie un {@link Legend} diverso, con
     * un'abilità passiva e una attiva. Configurabile in entrambe le modalità. A
     * differenza delle altre opzioni non cambia le regole di tutti: cambia i
     * giocatori, quindi la legge {@link ConfigurazionePartita} (che chiede un Legend a
     * testa) e non {@link RegoleDiGioco}.
     */
    LEGEND
}
