package unolegends.controller;

import java.util.Objects;
import java.util.stream.Collectors;

import unolegends.model.Carta;
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
     * Restituisce alla UI uno snapshot già convertito in dati di presentazione.
     */
    public StatoPartita richiediStato() {
        StatoTurno stato = partita.getStatoTurno();
        return new StatoPartita(
                stato.getNomeGiocatoreAttivo(),
                stato.getManoGiocatoreAttivo().stream()
                        .map(carta -> carta.toCliString())
                        .collect(Collectors.toList()),
                formattaCarta(stato.getCartaInCima()),
                stato.isDeveGiocareCartaPescata(),
                formattaCarta(stato.getCartaAppenaPescata()),
                stato.isDeveScegliereColoreIniziale());
    }

    public int carteNelMazzo() {
        return partita.getNumeroCarteNelMazzo();
    }

    /**
     * Indica alla UI se la giocata selezionata richiede un colore.
     */
    public boolean cartaRichiedeSceltaColore(int indiceCarta) {
        return partita.cartaRichiedeSceltaColore(indiceCarta);
    }

    /**
     * Coordina la scelta opzionale del colore e la successiva giocata.
     */
    public boolean giocaCarta(int indiceCarta) {
        return giocaCarta(indiceCarta, null);
    }

    public boolean giocaCarta(int indiceCarta, Integer sceltaColore) {
        if (sceltaColore != null) {
            Colore colore = convertiColore(sceltaColore);
            if (colore == null || !partita.scegliColore(colore)) {
                return false;
            }
        }
        return partita.giocaCarta(indiceCarta);
    }

    /**
     * Accetta un indice colore della UI: 0 rosso, 1 verde, 2 blu, 3 giallo.
     */
    public boolean scegliColore(int sceltaColore) {
        Colore colore = convertiColore(sceltaColore);
        return colore != null && partita.scegliColore(colore);
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

    private static String formattaCarta(Carta carta) {
        return carta == null ? null : carta.toCliString();
    }

    private static Colore convertiColore(int sceltaColore) {
        switch (sceltaColore) {
            case 0:
                return Colore.ROSSO;
            case 1:
                return Colore.VERDE;
            case 2:
                return Colore.BLU;
            case 3:
                return Colore.GIALLO;
            default:
                return null;
        }
    }
}
