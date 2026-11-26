package unolegends.model;

/**
 * Abilità passiva di un Legend (Step 6): una regola che cambia <b>solo per il
 * giocatore che la possiede</b> ed è sempre attiva, senza cooldown.
 *
 * <p><b>Perché un'interfaccia con metodi {@code default}.</b> Ogni metodo è un
 * <i>hook</i>, cioè un punto del gioco in cui una passiva può intervenire. Il
 * comportamento di default "lascia tutto com'è", quindi ogni passiva concreta è
 * una classe piccola che ridefinisce soltanto l'hook che le serve. Chi interroga
 * gli hook ({@link RegoleDiGioco}, {@link Partita}, {@link Giocatore}) non sa quale
 * passiva ha davanti: chiede soltanto "qualcuno modifica questa regola?".</p>
 *
 * <p>È il pattern <b>Strategy</b> applicato al giocatore e, in termini GRASP,
 * <b>Polymorphism</b> + <b>Protected Variations</b>: il punto che varia (le
 * passive) è nascosto dietro un'interfaccia stabile, e una passiva nuova è una
 * classe nuova senza {@code instanceof} né {@code switch} sul Legend. Se una passiva
 * futura deve intervenire in un punto nuovo si aggiunge un hook qui: grazie al
 * {@code default} le passive esistenti non si toccano.</p>
 *
 * <p>Alternative scartate (vedi {@code docs/step6_sesta_iter.md}): enum di
 * "capacità" con {@code if} nelle regole, enum con un corpo per costante,
 * Decorator sulle strategie, Observer.</p>
 *
 * <p>Hook e chi li chiama:</p>
 * <ul>
 *   <li>{@link #modificaPescaForzata} — {@link RegoleDiGioco#calcolaCartePescaForzata};</li>
 *   <li>{@link #consenteGiocata} — {@link RegoleDiGioco#isGiocabile};</li>
 *   <li>{@link #isBersagliabile} — {@link Giocatore#isBersagliabile()};</li>
 *   <li>{@link #isImmuneAlBlocca} — {@link Partita#bloccaProssimo()};</li>
 *   <li>{@link #getCarteTraCuiScegliereInPesca} — {@link RegoleDiGioco#getCarteTraCuiScegliereInPesca}.</li>
 * </ul>
 */
public interface IPassiva {

    /**
     * Quante carte pesca davvero il possessore di una pesca forzata. Riceve il
     * valore già calcolato (dalle regole o da un'altra passiva) e restituisce quello
     * nuovo; di default non lo cambia.
     */
    default int modificaPescaForzata(ContestoPesca contesto, int carte) {
        return carte;
    }

    /**
     * true se la passiva permette al possessore una giocata che le regole normali
     * rifiuterebbero. Può solo <b>aggiungere</b> giocate, mai toglierne: per questo
     * di default è false e le regole fanno "base OR passive".
     *
     * @param coloreAttivo colore attivo della partita (per un jolly in cima è quello scelto)
     */
    default boolean consenteGiocata(Carta candidata, Carta inCima, Colore coloreAttivo) {
        return false;
    }

    /**
     * false se il possessore non può essere scelto come bersaglio dagli effetti in
     * cui il giocatore sceglie un avversario (carta 7, Castigatore, Gifted card,
     * Scambia mazzo, Congela).
     */
    default boolean isBersagliabile() {
        return true;
    }

    /** true se la carta Blocca giocata contro il possessore non ha effetto. */
    default boolean isImmuneAlBlocca() {
        return false;
    }

    /**
     * Fra quante carte il possessore sceglie quando pesca volontariamente (1 = nessuna
     * scelta). Vale solo se la pesca volontaria delle regole lo ammette, cioè nella
     * partita normale.
     */
    default int getCarteTraCuiScegliereInPesca() {
        return 1;
    }
}
