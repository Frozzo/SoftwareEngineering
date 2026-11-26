package unolegends.model;

/**
 * Causa di una pesca. Permette alle regole (e in futuro alle abilità passive dei
 * Legend) di distinguere le pescate senza conoscere l'effetto o la carta che le
 * ha generate: per esempio un'immunità al solo +2 controlla {@code PIU_DUE}.
 *
 * <p>Nuove cause (abilità attive) vanno aggiunte qui quando vengono introdotte le
 * relative funzionalità. +6, +10 e roulette sono arrivati con lo Step 4,
 * l'accumulo dello stacking con lo Step 5, il Castigatore con lo Step 6.</p>
 */
public enum OriginePesca {
    VOLONTARIA,
    PIU_DUE,
    /** Sia il +4 nero della partita normale sia il +4 colorato e il +4 cambio giro della No Mercy. */
    PIU_QUATTRO,
    PENALITA_UNO,
    /** Jolly +6 (No Mercy). */
    PIU_SEI,
    /** Jolly +10 (No Mercy). */
    PIU_DIECI,
    /** Jolly roulette colore (No Mercy): non passa da calcolaCartePescaForzata, quindi niente sconti (Salesman). */
    ROULETTE,
    /**
     * Accumulo dello stacking (Step 5) formato da <b>due o più</b> carte. Se chi
     * accetta subisce una sola carta, l'origine resta quella della carta
     * ({@link Accumulo#getOriginePenalita()}): La Roccia è immune al +2 singolo,
     * non a un accumulo.
     */
    ACCUMULO,
    /**
     * Abilità attiva "Il Castigatore" de La Roccia (Step 6): un giocatore a scelta pesca
     * 4 carte. Passa da calcolaCartePescaForzata, quindi Salesman ha lo sconto.
     */
    CASTIGATORE
}
