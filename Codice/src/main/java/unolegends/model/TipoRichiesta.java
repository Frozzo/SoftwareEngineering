package unolegends.model;

/**
 * Tipo di informazione che un effetto chiede al giocatore.
 *
 * <p>Il model dice soltanto <i>cosa</i> serve; la frase da mostrare la scrive il
 * controller, così nel dominio non ci sono testi di interfaccia.</p>
 */
public enum TipoRichiesta {
    /** Scelta di un colore (cambia colore, +4, roulette). */
    SCELTA_COLORE,
    /**
     * Scelta di un giocatore bersaglio: carta 7 della No Mercy e abilità attive dei
     * Legend (Castigatore, Gifted card, Scambia mazzo, Congela).
     */
    SCELTA_GIOCATORE,
    /**
     * Scelta della carta da tenere fra quelle pescate (passiva Doppia pesca
     * dell'Acrobata, Step 6). Le opzioni sono le descrizioni delle carte.
     */
    SCELTA_CARTA
}
