package unolegends.controller;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import unolegends.model.Carta;
import unolegends.model.CartaSpeciale;
import unolegends.model.Colore;
import unolegends.model.EventoPartita;
import unolegends.model.FaseTurno;
import unolegends.model.OriginePesca;
import unolegends.model.Partita;
import unolegends.model.PartitaFactory;
import unolegends.model.RegoleDiGioco;
import unolegends.model.RichiestaInput;
import unolegends.model.StandardMazzoFactory;
import unolegends.model.TipoRichiesta;

/**
 * GRASP Controller (Facade): punto di ingresso per la UI.
 *
 * <p>Oltre a inoltrare i comandi a {@link Partita}, fa da traduttore verso la
 * presentazione: converte le carte in testo, la fase del model in
 * {@link FaseTurnoDto}, il {@link TipoRichiesta} in una frase e gli
 * {@link EventoPartita} in messaggi. Così il model non contiene testi di
 * interfaccia e la UI non importa il model.</p>
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
                convertiFase(partita.getFase()),
                partita.getNomeGiocatoreAttivo(),
                partita.getManoGiocatoreAttivo().stream().map(Carta::getDescrizioneBreve).collect(Collectors.toList()),
                formattaCarta(partita.getPilaDegliScarti().getCartaInCima(), partita.getColoreAttuale()),
                formattaCarta(partita.getCartaAppenaPescata()),
                convertiRichiesta(partita.getRichiestaInputInAttesa()),
                partita.getNomeVincitore());
    }

    /**
     * Inoltra al dominio il tentativo di giocata. Se la carta richiede un input,
     * la partita passa in fase di attesa: la UI lo scopre rileggendo lo stato e
     * risponde con {@link #rispondiInput(String)}.
     *
     * @return true se la giocata è stata accettata
     */
    public boolean giocaCarta(int indiceCarta) {
        return partita.giocaCarta(indiceCarta);
    }

    /**
     * Inoltra la risposta a qualunque richiesta di input in attesa (colore della
     * carta iniziale, colore di un jolly, in futuro bersagli e abilità).
     *
     * @return true se la risposta è stata accettata
     */
    public boolean rispondiInput(String risposta) {
        return partita.rispondiInput(risposta);
    }

    /**
     * Inoltra l'esito della finestra UNO. L'eventuale penalità viene comunicata
     * dagli eventi ({@link #prelevaMessaggi()}).
     *
     * @return true se il comando è stato accettato
     */
    public boolean completaDichiarazioneUno(boolean unoDichiarato) {
        return partita.completaDichiarazioneUno(unoDichiarato);
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
     * Preleva gli eventi accaduti dall'ultima chiamata e li traduce in messaggi
     * da stampare. La UI la chiama a ogni giro del suo ciclo.
     */
    public List<String> prelevaMessaggi() {
        return partita.prelevaEventi().stream()
                .map(UnoLegendsGame::descriviEvento)
                .collect(Collectors.toList());
    }

    /**
     * Traduce un evento del model in una frase. Gli {@code switch} sono
     * esaustivi: aggiungendo un {@code TipoEvento} o un'{@code OriginePesca}, il
     * compilatore obbliga a scrivere anche il relativo messaggio.
     */
    private static String descriviEvento(EventoPartita evento) {
        String giocatore = evento.nomeGiocatore();
        return switch (evento.tipo()) {
            case PESCA_VOLONTARIA -> evento.quantita() == 0
                    ? giocatore + " non pesca nulla: mazzo e scarti sono esauriti."
                    : giocatore + " pesca " + descriviNumeroCarte(evento.quantita()) + ".";
            case PESCA_FORZATA -> giocatore + " pesca " + descriviNumeroCarte(evento.quantita())
                    + descriviOrigine(evento.origine()) + ".";
            case TURNO_SALTATO -> giocatore + " salta il turno.";
            case SENSO_INVERTITO -> "Il senso di gioco si inverte.";
            case COLORE_SCELTO -> giocatore + " sceglie il colore "
                    + evento.colore().formatta(evento.colore().getNome()) + ".";
            case UNO_DICHIARATO -> giocatore + " dichiara UNO!";
        };
    }

    private static String descriviNumeroCarte(int quantita) {
        return quantita == 1 ? "1 carta" : quantita + " carte";
    }

    private static String descriviOrigine(OriginePesca origine) {
        return switch (origine) {
            case VOLONTARIA -> "";
            case PIU_DUE -> " per un +2";
            case PIU_QUATTRO -> " per un +4";
            case PENALITA_UNO -> " di penalita: UNO non dichiarato";
        };
    }

    private static FaseTurnoDto convertiFase(FaseTurno fase) {
        return switch (fase) {
            case NORMALE -> FaseTurnoDto.NORMALE;
            case POST_PESCA -> FaseTurnoDto.POST_PESCA;
            case ATTESA_INPUT -> FaseTurnoDto.ATTESA_INPUT;
            case ATTESA_UNO -> FaseTurnoDto.ATTESA_UNO;
            case TERMINATA -> FaseTurnoDto.TERMINATA;
        };
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

    /**
     * Converte la richiesta del model nel DTO, scrivendo qui la frase da mostrare:
     * il model indica solo il {@link TipoRichiesta}.
     */
    private static RichiestaInputDto convertiRichiesta(RichiestaInput richiesta) {
        return richiesta == null
                ? null
                : new RichiestaInputDto(messaggioPer(richiesta.getTipo()), richiesta.getOpzioni());
    }

    private static String messaggioPer(TipoRichiesta tipo) {
        return switch (tipo) {
            case SCELTA_COLORE -> "Scegli il colore da assegnare alla carta:";
            case SCELTA_GIOCATORE -> "Scegli il giocatore bersaglio:";
        };
    }
}
