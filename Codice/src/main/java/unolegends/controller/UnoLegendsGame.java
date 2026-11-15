package unolegends.controller;

import java.util.Objects;
import java.util.stream.Collectors;

import unolegends.model.Carta;
import unolegends.model.CartaSpeciale;
import unolegends.model.Colore;
import unolegends.model.EsitoGiocata;
import unolegends.model.Partita;
import unolegends.model.PartitaFactory;
import unolegends.model.RegoleDiGioco;
import unolegends.model.StandardMazzoFactory;
import unolegends.model.RichiestaInput;

/**
 * GRASP Controller (Facade): punto di ingresso per la UI.
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

    UnoLegendsGame(Partita partita) {
        this.partita = Objects.requireNonNull(partita, "partita non puo essere null");
    }

    /**
     * Restituisce alla UI uno snapshot già convertito in dati di presentazione.
     */
    public StatoPartitaDto richiediStato() {
        return new StatoPartitaDto(
                partita.getNomeGiocatoreAttivo(),
                partita.getManoGiocatoreAttivo().stream().map(Carta::getDescrizioneBreve).collect(Collectors.toList()),
                formattaCarta(partita.getPilaDegliScarti().getCartaInCima(), partita.getColoreAttuale()),
                partita.isCartaPescataDaGiocare(),
                formattaCarta(partita.getCartaAppenaPescata()),
                convertiRichiesta(partita.getRichiestaInputIniziale()),
                partita.isPartitaTerminata(),
                partita.getNomeVincitore());
    }

    /**
     * Inoltra al dominio il tentativo di giocata e l'eventuale risposta richiesta
     * dall'effetto. Il controller non valida la carta: restituisce alla UI l'esito
     * prodotto da Partita, che può indicare una mossa rifiutata o un input in attesa.
     */
    public EsitoGiocataDto giocaCarta(int indiceCarta, String rispostaInput) {
        EsitoGiocata esito = partita.giocaCarta(indiceCarta, rispostaInput);
        return new EsitoGiocataDto(
                esito.isGiocata(),
                convertiRichiesta(esito.getRichiestaInput()),
                esito.isRichiedeDichiarazioneUno());
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

    private static String formattaCarta(Carta carta, Colore coloreAttuale) {
        if (carta == null) {
            return null;
        }
        if (carta instanceof CartaSpeciale && carta.getColore() == Colore.NERO
                && coloreAttuale != null && coloreAttuale != Colore.NERO) {
            return ((CartaSpeciale) carta).getDescrizioneBreve(coloreAttuale);
        }
        return carta.getDescrizioneBreve();
    }

    private static RichiestaInputDto convertiRichiesta(RichiestaInput richiesta) {
        return richiesta == null
                ? null
                : new RichiestaInputDto(richiesta.getMessaggio(), richiesta.getOpzioni());
    }
}
