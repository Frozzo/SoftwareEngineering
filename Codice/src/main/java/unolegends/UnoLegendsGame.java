package unolegends;

import java.util.Objects;

/**
 * GRASP Controller (Facade): punto di ingresso per la UI CLI.
 */
public class UnoLegendsGame {
    private final Partita partita;

    public UnoLegendsGame(Partita partita) {
        this.partita = Objects.requireNonNull(partita, "partita non puo essere null");
    }

    /**
     * GRASP Controller: inoltra la richiesta di stato alla radice del dominio.
     */
    public StatoTurno richiediStato() {
        return partita.getStatoTurno();
    }

    /**
     * GRASP Controller: inoltra il comando di giocata carta alla radice del dominio.
     */
    public boolean giocaCarta(int indiceCarta) {
        return partita.giocaCarta(indiceCarta);
    }

    /**
     * GRASP Controller: inoltra il comando di pesca carta alla radice del dominio.
     */
    public boolean pescaCarta() {
        return partita.pescaCarta();
    }

    /**
     * GRASP Controller: inoltra il comando di passaggio turno alla radice del dominio.
     */
    public boolean passaTurno() {
        return partita.passaTurno();
    }
}
