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
 * non fanno nulla: {@code STACKING} arriverà con lo Step 5, {@code LEGEND} con lo
 * Step 6.</p>
 */
public enum OpzionePartita {
    /** Chi finisce le carte esce e si continua finché resta un solo giocatore: alla fine c'è la classifica. */
    ENDLESS
}
