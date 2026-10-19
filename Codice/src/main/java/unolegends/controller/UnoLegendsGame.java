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
                partita.getManoGiocatoreAttivo().stream().map(Carta::getDescrizioneBreve).collect(Collectors.toList()),
                formattaCarta(partita.getPilaDegliScarti().getCartaInCima()),
                partita.isCartaPescataDaGiocare(),
                formattaCarta(partita.getCartaAppenaPescata()),
                partita.getRichiestaInputIniziale(),
                partita.isPartitaTerminata(),
                partita.getNomeVincitore());
    }

    /**
     * Inoltra al dominio il tentativo di giocata e l'eventuale risposta richiesta
     * dall'effetto. Il controller non valida la carta: restituisce alla UI l'esito
     * prodotto da Partita, che può indicare una mossa rifiutata o un input in attesa.
     */
    public EsitoGiocata giocaCarta(int indiceCarta, String rispostaInput) {
        return partita.giocaCarta(indiceCarta, rispostaInput);
    }

    public boolean completaDichiarazioneUno(boolean unoDichiarato) {
        return partita.completaDichiarazioneUno(unoDichiarato);
    }

    /**
     * Inoltra la risposta alla richiesta emessa dall'effetto della carta iniziale.
     * La validità della risposta e l'applicazione dell'effetto sono responsabilità
     * di Partita; il booleano viene restituito alla UI per segnalare l'esito.
     */
    public boolean rispondiRichiestaInputIniziale(String rispostaInput) {
        return partita.rispondiRichiestaInputIniziale(rispostaInput);
    }

    /**
     * Inoltra il comando di pesca alla partita e restituisce se è stato accettato
     * secondo lo stato corrente (per esempio, non si può pescare due volte di seguito).
     */
    public boolean pescaCarta() {
        return partita.pescaCarta();
    }

    /**
     * Inoltra la richiesta di passaggio turno; Partita controlla se il passaggio
     * è consentito e aggiorna lo stato del turno.
     */
    public boolean passaTurno() {
        return partita.passaTurno();
    }

    /**
     * Converte una carta in testo per lo snapshot della UI, preservando null
     * quando non esiste una carta da visualizzare.
     */
    private static String formattaCarta(Carta carta) {
        return carta == null ? null : carta.getDescrizioneBreve();
    }
}
