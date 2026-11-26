package unolegends.model;

import java.util.Objects;

/**
 * Abilità attiva di un giocatore (Step 6): un {@link Ieffetto} più il suo cooldown.
 *
 * <p><b>Perché riusa {@link Ieffetto}</b> (decisione presa): un'abilità fa le stesse
 * cose di una carta (chiede un input, lo valida, modifica la partita), quindi passa
 * dallo stesso meccanismo unificato ({@code Partita.mettiInAttesa} +
 * {@code rispondiInput}) e può riusare gli effetti delle carte: Scambia mazzo è
 * {@link EffettoScambiaMano}, Double card è {@link EffettoTurnoExtra}. Non serve
 * un'interfaccia {@code IAbilitaAttiva} separata.</p>
 *
 * <p>Il {@link Legend} descrive l'abilità (dati fissi, uguali per tutti), questa
 * classe tiene lo <b>stato di un giocatore</b>: quanti turni mancano prima di poterla
 * riusare. Per questo ogni {@link Giocatore} con un Legend ne ha una propria.</p>
 *
 * <p>Cooldown (decisioni prese):</p>
 * <ul>
 *   <li>all'inizio della partita l'abilità è pronta (niente cooldown iniziale);</li>
 *   <li>dopo l'uso la ricarica riparte dal cooldown e scende di 1 all'inizio di ogni
 *       turno <b>del possessore</b> (i turni saltati non contano, perché non li gioca):
 *       con cooldown 4 l'abilità torna pronta al quarto turno successivo;</li>
 *   <li>siccome il cooldown è almeno 1, non si può usare due volte nello stesso turno.</li>
 * </ul>
 */
public final class AbilitaAttiva {
    private final Ieffetto effetto;
    private final int cooldown;
    private int turniDiRicarica;

    AbilitaAttiva(Ieffetto effetto, int cooldown) {
        this.effetto = Objects.requireNonNull(effetto, "effetto non puo essere null");
        if (cooldown < 1) {
            throw new IllegalArgumentException("il cooldown deve essere almeno 1");
        }
        this.cooldown = cooldown;
    }

    Ieffetto getEffetto() {
        return effetto;
    }

    public int getCooldown() {
        return cooldown;
    }

    /** Turni (del possessore) che mancano prima di poterla riusare; 0 = pronta. */
    public int getTurniDiRicarica() {
        return turniDiRicarica;
    }

    public boolean isPronta() {
        return turniDiRicarica == 0;
    }

    /** Chiamato quando l'abilità viene usata. */
    void avviaRicarica() {
        turniDiRicarica = cooldown;
    }

    /** Chiamato all'inizio di ogni turno del possessore. */
    void scalaRicarica() {
        if (turniDiRicarica > 0) {
            turniDiRicarica--;
        }
    }
}
