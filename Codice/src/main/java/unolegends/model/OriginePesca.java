package unolegends.model;

/**
 * Causa di una pesca. Permette alle regole (e in futuro alle abilità passive dei
 * Legend) di distinguere le pescate senza conoscere l'effetto o la carta che le
 * ha generate: per esempio un'immunità al solo +2 controlla {@code PIU_DUE}.
 *
 * <p>Nuove cause (+6, +10, accumulo dello stacking, roulette, abilità attive)
 * vanno aggiunte qui quando vengono introdotte le relative funzionalità.</p>
 */
public enum OriginePesca {
    VOLONTARIA,
    PIU_DUE,
    PIU_QUATTRO,
    PENALITA_UNO
}
