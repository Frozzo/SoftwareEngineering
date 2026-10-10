package unolegends.controller;

import java.util.Objects;

import unolegends.model.Colore;
import unolegends.model.Partita;
import unolegends.model.PartitaFactory;
import unolegends.model.StatoTurno;

/**
 * GRASP Controller (Facade): punto di ingresso per la UI CLI.
 */
public class UnoLegendsGame {
    private final Partita partita;

    /**
     * Avvia una partita standard e costruisce la Facade del caso d'uso.
     * La View non deve conoscere la factory del dominio.
     */
    public static UnoLegendsGame avviaPartitaStandard() {
        return new UnoLegendsGame(PartitaFactory.creaPartitaStandard());
    }

    public UnoLegendsGame(Partita partita) {
        this.partita = Objects.requireNonNull(partita, "partita non puo essere null");
    }

    /**
     * GRASP Controller: inoltra la richiesta di stato alla radice del dominio.
     */
    public StatoTurno richiediStato() {
        return partita.getStatoTurno();
    }

    public int carteNelMazzo() {
        return partita.getMazzo().size();
    }

    /**
     * GRASP Controller: inoltra il comando di giocata carta alla radice del dominio.
     */
    public boolean giocaCarta(int indiceCarta) {
        return partita.giocaCarta(indiceCarta);
    }

    public boolean scegliColore(Colore colore) {
        return partita.scegliColore(colore);
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
