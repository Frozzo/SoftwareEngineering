package unolegends.controller;

import java.util.Objects;
import java.util.stream.Collectors;

import unolegends.model.Carta;
import unolegends.model.EsitoGiocata;
import unolegends.model.Partita;
import unolegends.model.PartitaFactory;
import unolegends.model.RegoleDiGioco;
import unolegends.model.StandardMazzoFactory;

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
        Partita partita = PartitaFactory.creaPartita(
                StandardMazzoFactory.getInstance(),
                2,
                RegoleDiGioco.standard());
        partita.iniziaGioco();
        return new UnoLegendsGame(partita);
    }

    public UnoLegendsGame(Partita partita) {
        this.partita = Objects.requireNonNull(partita, "partita non puo essere null");
    }

    /**
     * Restituisce alla UI uno snapshot già convertito in dati di presentazione.
     */
    public StatoPartita richiediStato() {
        return new StatoPartita(
                partita.getNomeGiocatoreAttivo(),
                partita.getManoGiocatoreAttivo().stream()
                        .map(Carta::getDescrizioneBreve)
                        .collect(Collectors.toList()),
                formattaCarta(partita.getPilaDegliScarti().getCartaInCima()),
                partita.isCartaPescataDaGiocare(),
                formattaCarta(partita.getCartaAppenaPescata()),
                partita.getRichiestaInputIniziale(),
                partita.isPartitaTerminata(),
                partita.getNomeVincitore());
    }

    public int carteNelMazzo() {
        return partita.getNumeroCarteNelMazzo();
    }

    /**
     * Inoltra al dominio il tentativo di giocata e l'eventuale risposta richiesta dall'effetto.
     */
    public EsitoGiocata giocaCarta(int indiceCarta, String rispostaInput, boolean unoDichiarato) {
        return partita.giocaCarta(indiceCarta, unoDichiarato, rispostaInput);
    }

    /**
     * Inoltra la risposta alla richiesta emessa dall'effetto della carta iniziale.
     */
    public boolean rispondiRichiestaInputIniziale(String rispostaInput) {
        return partita.rispondiRichiestaInputIniziale(rispostaInput);
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
        return carta == null ? null : carta.getDescrizioneBreve();
    }
}
