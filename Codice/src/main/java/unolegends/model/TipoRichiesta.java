package unolegends.model;

/**
 * Tipo di informazione che un effetto chiede al giocatore.
 *
 * <p>Il model dice soltanto <i>cosa</i> serve; la frase da mostrare la scrive il
 * controller, così nel dominio non ci sono testi di interfaccia.</p>
 */
public enum TipoRichiesta {
    /** Scelta di un colore (cambia colore, +4, in futuro la roulette). */
    SCELTA_COLORE,
    /**
     * Scelta di un giocatore bersaglio. Non ancora usata: servirà alla carta 7
     * di No Mercy e alle abilità attive dei Legend (Castigatore, Gifted card,
     * Scambia mazzo).
     */
    SCELTA_GIOCATORE
}
