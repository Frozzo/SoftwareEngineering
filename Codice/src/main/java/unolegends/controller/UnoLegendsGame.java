package unolegends.controller;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import unolegends.model.Carta;
import unolegends.model.Colore;
import unolegends.model.ConfigurazionePartita;
import unolegends.model.ErroreConfigurazione;
import unolegends.model.EventoPartita;
import unolegends.model.FaseTurno;
import unolegends.model.Giocatore;
import unolegends.model.ModalitaDiGioco;
import unolegends.model.OpzionePartita;
import unolegends.model.OriginePesca;
import unolegends.model.Partita;
import unolegends.model.PartitaFactory;
import unolegends.model.RichiestaInput;
import unolegends.model.TipoRichiesta;

/**
 * GRASP Controller (Facade): punto di ingresso per la UI.
 *
 * <p>Oltre a inoltrare i comandi a {@link Partita}, fa da traduttore verso la
 * presentazione: converte le carte in testo, la fase del model in
 * {@link StatoPartitaDto.Fase}, il {@link TipoRichiesta} in una frase e gli
 * {@link EventoPartita} in messaggi. Così il model non contiene testi di
 * interfaccia e la UI non importa il model.</p>
 *
 * <p>Dallo Step 3 espone anche il menu di setup, usando solo stringhe: elenca
 * modalità e opzioni, verifica le scelte ({@link #verificaImpostazioni}) e avvia
 * la partita ({@link #avviaPartita}). Non servono DTO né eccezioni dedicate: la
 * verifica restituisce la frase d'errore oppure {@code null}. La CLI non sa quali
 * opzioni sono obbligatorie in una modalità: le basta chiedere
 * {@link #getOpzioniConfigurabili(String)}.</p>
 */
public class UnoLegendsGame {
    private final Partita partita;

    // ------------------------------------------------------------------
    // Menu di setup
    // ------------------------------------------------------------------

    /** Nomi delle modalità selezionabili nel menu, nell'ordine in cui mostrarle. */
    public static List<String> getModalitaDisponibili() {
        return Arrays.stream(ModalitaDiGioco.values()).map(UnoLegendsGame::nomeModalita).toList();
    }

    /**
     * Nomi delle opzioni che l'utente può attivare o no nella modalità indicata.
     * Quelle obbligatorie per la modalità non compaiono: vengono attivate da sole.
     */
    public static List<String> getOpzioniConfigurabili(String modalita) {
        ModalitaDiGioco scelta = Objects.requireNonNull(modalitaDaNome(modalita), "modalita sconosciuta");
        return Arrays.stream(OpzionePartita.values())
                .filter(scelta::isConfigurabile)
                .map(UnoLegendsGame::nomeOpzione)
                .toList();
    }

    /** Frase che spiega l'opzione, da mostrare prima di chiedere se attivarla. */
    public static String descriviOpzione(String opzione) {
        return descrizioneOpzione(Objects.requireNonNull(opzioneDaNome(opzione), "opzione sconosciuta"));
    }

    public static int getMinimoGiocatori() {
        return ConfigurazionePartita.MIN_GIOCATORI;
    }

    public static int getMassimoGiocatori() {
        return ConfigurazionePartita.MAX_GIOCATORI;
    }

    /**
     * Controlla le scelte del menu prima di avviare la partita. La validazione la
     * fa il model ({@link ConfigurazionePartita#verifica}); qui il motivo viene
     * tradotto in una frase per l'utente.
     *
     * @return la frase da mostrare se le scelte non sono valide, oppure {@code null} se sono valide
     */
    public static String verificaImpostazioni(String modalita, Set<String> opzioni, List<String> nomiGiocatori) {
        ModalitaDiGioco modalitaScelta = modalitaDaNome(modalita);
        if (modalitaScelta == null) {
            return "Modalita sconosciuta: " + modalita + ".";
        }
        Set<OpzionePartita> opzioniScelte = EnumSet.noneOf(OpzionePartita.class);
        for (String nome : opzioni) {
            OpzionePartita opzione = opzioneDaNome(nome);
            if (opzione == null) {
                return "Opzione sconosciuta: " + nome + ".";
            }
            opzioniScelte.add(opzione);
        }
        ErroreConfigurazione errore = ConfigurazionePartita.verifica(modalitaScelta, nomiGiocatori, opzioniScelte);
        return errore == null ? null : messaggioPer(errore);
    }

    /**
     * Crea e avvia la partita descritta dalle scelte del menu. Le scelte vanno
     * controllate prima con {@link #verificaImpostazioni}: se non sono valide
     * questo metodo lancia una {@link IllegalArgumentException}, perché è un errore
     * di programmazione della UI e non un caso da mostrare all'utente.
     */
    public static UnoLegendsGame avviaPartita(String modalita, Set<String> opzioni, List<String> nomiGiocatori) {
        String errore = verificaImpostazioni(modalita, opzioni, nomiGiocatori);
        if (errore != null) {
            throw new IllegalArgumentException(errore);
        }
        Set<OpzionePartita> opzioniScelte = EnumSet.noneOf(OpzionePartita.class);
        opzioni.forEach(nome -> opzioniScelte.add(opzioneDaNome(nome)));
        ConfigurazionePartita config = new ConfigurazionePartita(modalitaDaNome(modalita), nomiGiocatori, opzioniScelte);

        Partita partita = PartitaFactory.creaPartita(config);
        partita.iniziaGioco();
        return new UnoLegendsGame(partita);
    }

    private static String nomeModalita(ModalitaDiGioco modalita) {
        return switch (modalita) {
            case NORMALE -> "Standard";
            case NO_MERCY -> "No Mercy";
        };
    }

    private static String nomeOpzione(OpzionePartita opzione) {
        return switch (opzione) {
            case ENDLESS -> "Endless";
        };
    }

    private static String descrizioneOpzione(OpzionePartita opzione) {
        return switch (opzione) {
            case ENDLESS -> "chi finisce le carte esce e si continua fino all'ultimo, poi si stila la classifica"
                    + " (almeno " + ConfigurazionePartita.MIN_GIOCATORI_ENDLESS + " giocatori)";
        };
    }

    /** La modalità con quel nome, oppure null se non esiste. */
    private static ModalitaDiGioco modalitaDaNome(String nome) {
        return Arrays.stream(ModalitaDiGioco.values())
                .filter(m -> nomeModalita(m).equalsIgnoreCase(nome))
                .findFirst()
                .orElse(null);
    }

    /** L'opzione con quel nome, oppure null se non esiste. */
    private static OpzionePartita opzioneDaNome(String nome) {
        return Arrays.stream(OpzionePartita.values())
                .filter(o -> nomeOpzione(o).equalsIgnoreCase(nome))
                .findFirst()
                .orElse(null);
    }

    private static String messaggioPer(ErroreConfigurazione errore) {
        return switch (errore) {
            case NUMERO_GIOCATORI_NON_VALIDO -> "Il numero di giocatori deve essere tra "
                    + ConfigurazionePartita.MIN_GIOCATORI + " e " + ConfigurazionePartita.MAX_GIOCATORI + ".";
            case NOME_VUOTO -> "Il nome di un giocatore non puo essere vuoto.";
            case NOMI_DUPLICATI -> "Due giocatori non possono avere lo stesso nome.";
            case GIOCATORI_INSUFFICIENTI_PER_ENDLESS -> "La modalita Endless richiede almeno "
                    + ConfigurazionePartita.MIN_GIOCATORI_ENDLESS + " giocatori.";
        };
    }

    // ------------------------------------------------------------------
    // Partita in corso
    // ------------------------------------------------------------------

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
                convertiGiocatori(),
                partita.isSensoOrario(),
                partita.getClassifica().stream().map(Giocatore::getNome).toList());
    }

    private List<StatoPartitaDto.Giocatore> convertiGiocatori() {
        String nomeAttivo = partita.getNomeGiocatoreAttivo();
        boolean partitaInCorso = partita.getFase() != FaseTurno.TERMINATA;
        return partita.getGiocatori().stream()
                .map(g -> new StatoPartitaDto.Giocatore(g.getNome(), g.getMano().size(), descriviStato(g),
                        partitaInCorso && g.getNome().equals(nomeAttivo)))
                .toList();
    }

    private String descriviStato(Giocatore giocatore) {
        return switch (giocatore.getStato()) {
            case IN_GIOCO -> "in gioco";
            case TERMINATO -> "terminato al posto n. " + (partita.getGiocatoriTerminati().indexOf(giocatore) + 1);
            case ELIMINATO -> "eliminato";
        };
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
            case GIOCATORE_TERMINATO -> giocatore + " ha finito le carte: si classifica al posto n. " + evento.quantita() + "!";
            case GIOCATORE_ELIMINATO -> giocatore + " e eliminato: le sue carte tornano nel mazzo.";
            case MANI_SCAMBIATE -> giocatore + " scambia la mano con " + evento.altroGiocatore() + ".";
            case MANI_PASSATE -> giocatore + " gioca lo 0: tutti passano la mano al giocatore successivo.";
            case CARTE_SCARTATE -> evento.quantita() == 0
                    ? giocatore + " non ha altre carte di colore " + evento.colore().getNome().toLowerCase() + " da scartare."
                    : giocatore + " scarta " + descriviNumeroCarte(evento.quantita()) + " di colore "
                            + evento.colore().formatta(evento.colore().getNome().toLowerCase()) + ".";
            case TURNO_EXTRA -> "Tutti saltano il turno: " + giocatore + " gioca ancora.";
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
            case PIU_SEI -> " per un +6";
            case PIU_DIECI -> " per un +10";
            case ROULETTE -> " con la roulette";
        };
    }

    private static StatoPartitaDto.Fase convertiFase(FaseTurno fase) {
        return switch (fase) {
            case NORMALE -> StatoPartitaDto.Fase.NORMALE;
            case POST_PESCA -> StatoPartitaDto.Fase.POST_PESCA;
            case ATTESA_INPUT -> StatoPartitaDto.Fase.ATTESA_INPUT;
            case ATTESA_UNO -> StatoPartitaDto.Fase.ATTESA_UNO;
            case TERMINATA -> StatoPartitaDto.Fase.TERMINATA;
        };
    }

    /**
     * Converte una carta in testo per lo snapshot della UI, preservando null
     * quando non esiste una carta da visualizzare.
     */
    private static String formattaCarta(Carta carta) {
        return carta == null ? null : carta.getDescrizioneBreve();
    }

    /**
     * Carta in cima agli scarti: un jolly nero viene mostrato con il colore scelto.
     * Da quando le carte sono un'unica classe basta guardare il colore, senza
     * {@code instanceof}.
     */
    private static String formattaCarta(Carta carta, Colore coloreAttuale) {
        if (carta == null) {
            return null;
        }
        if (carta.getColore() == Colore.NERO && coloreAttuale != null && coloreAttuale != Colore.NERO) {
            return carta.getDescrizioneBreve(coloreAttuale);
        }
        return carta.getDescrizioneBreve();
    }

    /**
     * Converte la richiesta del model nel DTO, scrivendo qui la frase da mostrare:
     * il model indica solo il {@link TipoRichiesta}.
     */
    private static StatoPartitaDto.Richiesta convertiRichiesta(RichiestaInput richiesta) {
        return richiesta == null
                ? null
                : new StatoPartitaDto.Richiesta(messaggioPer(richiesta.getTipo()), richiesta.getOpzioni());
    }

    private static String messaggioPer(TipoRichiesta tipo) {
        return switch (tipo) {
            case SCELTA_COLORE -> "Scegli il colore da assegnare alla carta:";
            case SCELTA_GIOCATORE -> "Scegli il giocatore bersaglio:";
        };
    }
}
