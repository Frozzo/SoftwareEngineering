package unolegends.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import unolegends.model.AbilitaAttiva;
import unolegends.model.Accumulo;
import unolegends.model.Carta;
import unolegends.model.Colore;
import unolegends.model.ConfigurazionePartita;
import unolegends.model.ErroreConfigurazione;
import unolegends.model.EventoPartita;
import unolegends.model.FaseTurno;
import unolegends.model.Giocatore;
import unolegends.model.Legend;
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
 *
 * <p>Dallo Step 6 il setup chiede anche i Legend, sempre come stringhe:
 * {@link #richiedeLegend(Set)} dice se le opzioni scelte li prevedono,
 * {@link #getLegendDisponibili()} e {@link #descriviLegend(String)} li elencano. Nomi
 * e descrizioni di Legend, passive e abilità li scrive questa classe, come per
 * modalità e opzioni: il model contiene solo l'enum {@link Legend}.</p>
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

    /**
     * true se con queste opzioni ogni giocatore deve scegliere un Legend. La CLI lo
     * chiede qui invece di confrontare il nome dell'opzione: non sa quale opzione porta
     * i Legend.
     */
    public static boolean richiedeLegend(Set<String> opzioni) {
        return opzioni.stream().anyMatch(nome -> opzioneDaNome(nome) == OpzionePartita.LEGEND);
    }

    /** Nomi dei Legend selezionabili, nell'ordine in cui mostrarli. */
    public static List<String> getLegendDisponibili() {
        return Arrays.stream(Legend.values()).map(UnoLegendsGame::nomeLegend).toList();
    }

    /** Frase che spiega passiva, attiva e cooldown del Legend, da mostrare nel menu. */
    public static String descriviLegend(String legend) {
        Legend scelto = Objects.requireNonNull(legendDaNome(legend), "legend sconosciuto");
        return "passiva " + nomePassiva(scelto) + " (" + descrizionePassiva(scelto) + "); attiva "
                + nomeAbilita(scelto) + " (" + descrizioneAbilita(scelto) + ", ricarica "
                + scelto.getCooldown() + " turni)";
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
        return verificaImpostazioni(modalita, opzioni, nomiGiocatori, List.of());
    }

    /**
     * Come {@link #verificaImpostazioni(String, Set, List)}, con i Legend dei giocatori
     * nello stesso ordine dei nomi (lista vuota se la partita è senza Legend).
     *
     * @return la frase da mostrare se le scelte non sono valide, oppure {@code null} se sono valide
     */
    public static String verificaImpostazioni(String modalita, Set<String> opzioni, List<String> nomiGiocatori,
                                              List<String> legend) {
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
        List<Legend> legendScelti = new ArrayList<>();
        for (String nome : legend) {
            Legend scelto = legendDaNome(nome);
            if (scelto == null) {
                return "Legend sconosciuto: " + nome + ".";
            }
            legendScelti.add(scelto);
        }
        ErroreConfigurazione errore = ConfigurazionePartita.verifica(modalitaScelta, nomiGiocatori, opzioniScelte,
                legendScelti);
        return errore == null ? null : messaggioPer(errore);
    }

    /**
     * Crea e avvia la partita descritta dalle scelte del menu. Le scelte vanno
     * controllate prima con {@link #verificaImpostazioni}: se non sono valide
     * questo metodo lancia una {@link IllegalArgumentException}, perché è un errore
     * di programmazione della UI e non un caso da mostrare all'utente.
     */
    public static UnoLegendsGame avviaPartita(String modalita, Set<String> opzioni, List<String> nomiGiocatori) {
        return avviaPartita(modalita, opzioni, nomiGiocatori, List.of());
    }

    /** Come {@link #avviaPartita(String, Set, List)}, con i Legend dei giocatori. */
    public static UnoLegendsGame avviaPartita(String modalita, Set<String> opzioni, List<String> nomiGiocatori,
                                              List<String> legend) {
        String errore = verificaImpostazioni(modalita, opzioni, nomiGiocatori, legend);
        if (errore != null) {
            throw new IllegalArgumentException(errore);
        }
        Set<OpzionePartita> opzioniScelte = EnumSet.noneOf(OpzionePartita.class);
        opzioni.forEach(nome -> opzioniScelte.add(opzioneDaNome(nome)));
        List<Legend> legendScelti = legend.stream().map(UnoLegendsGame::legendDaNome).toList();
        ConfigurazionePartita config = new ConfigurazionePartita(modalitaDaNome(modalita), nomiGiocatori,
                opzioniScelte, legendScelti);

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
            case STACKING -> "Stacking";
            case LEGEND -> "Legend";
        };
    }

    private static String descrizioneOpzione(OpzionePartita opzione) {
        return switch (opzione) {
            case ENDLESS -> "chi finisce le carte esce e si continua fino all'ultimo, poi si stila la classifica"
                    + " (almeno " + ConfigurazionePartita.MIN_GIOCATORI_ENDLESS + " giocatori)";
            case STACKING -> "a una carta di pesca si puo rispondere con una carta di pesca di valore uguale"
                    + " o superiore (+2 su +2, +4 su +2 o su +4): le carte si accumulano e chi non risponde"
                    + " le pesca tutte e perde il turno";
            case LEGEND -> "ogni giocatore sceglie un Legend diverso, con un'abilita passiva sempre attiva"
                    + " e un'abilita attiva da usare a inizio turno (poi va ricaricata)";
        };
    }

    // ------------------------------------------------------------------
    // Testi dei Legend (Step 6): il model ha solo l'enum, le frasi stanno qui
    // ------------------------------------------------------------------

    private static String nomeLegend(Legend legend) {
        return switch (legend) {
            case ROCCIA -> "La Roccia";
            case MERCANTE -> "Il Mercante";
            case FANTASMA -> "Il Fantasma";
            case DALTONICO -> "Il Daltonico";
            case FREEZER -> "Il Freezer";
            case ACROBATA -> "L'Acrobata";
        };
    }

    private static String nomePassiva(Legend legend) {
        return switch (legend) {
            case ROCCIA -> "Roccia";
            case MERCANTE -> "Salesman";
            case FANTASMA -> "Ghost";
            case DALTONICO -> "Daltonismo";
            case FREEZER -> "Pelle di ghiaccio";
            case ACROBATA -> "Doppia pesca";
        };
    }

    private static String descrizionePassiva(Legend legend) {
        return switch (legend) {
            case ROCCIA -> "un +2 da solo non ha effetto su di lui: non pesca e non salta il turno;"
                    + " un accumulo dello stacking lo subisce come tutti";
            case MERCANTE -> "a ogni penalita di pesca pesca una carta in meno, tranne che con la roulette";
            case FANTASMA -> "non puo essere scelto come bersaglio da carte e abilita";
            case DALTONICO -> "puo giocare carte rosse sul verde e carte verdi sul rosso";
            case FREEZER -> "la carta Blocca non ha effetto su di lui";
            case ACROBATA -> "quando pesca pesca 2 carte, ne tiene una e rimette l'altra in fondo al mazzo;"
                    + " solo nella partita Standard";
        };
    }

    private static String nomeAbilita(Legend legend) {
        return switch (legend) {
            case ROCCIA -> "Il Castigatore";
            case MERCANTE -> "Gifted card";
            case FANTASMA -> "Scambia mazzo";
            case DALTONICO -> "Arcobaleno";
            case FREEZER -> "Congela";
            case ACROBATA -> "Double card";
        };
    }

    private static String descrizioneAbilita(Legend legend) {
        return switch (legend) {
            case ROCCIA -> "un giocatore a scelta pesca 4 carte";
            case MERCANTE -> "dai 2 carte a caso della tua mano a un giocatore a scelta; serve avere almeno 4 carte";
            case FANTASMA -> "scambi la tua mano con un giocatore a scelta che ha piu di una carta";
            case DALTONICO -> "in questo turno puoi giocare qualsiasi carta, anche se non e compatibile";
            case FREEZER -> "un giocatore a scelta salta il suo prossimo turno e intanto e come se non ci fosse";
            case ACROBATA -> "dopo questo turno ne giochi subito un altro";
        };
    }

    /** Il Legend con quel nome, oppure null se non esiste. */
    private static Legend legendDaNome(String nome) {
        return Arrays.stream(Legend.values())
                .filter(l -> nomeLegend(l).equalsIgnoreCase(nome == null ? "" : nome.strip()))
                .findFirst()
                .orElse(null);
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
            case LEGEND_MANCANTI -> "Con l'opzione Legend ogni giocatore deve scegliere un Legend.";
            case LEGEND_SENZA_OPZIONE -> "Per scegliere i Legend bisogna attivare l'opzione Legend.";
            case LEGEND_DUPLICATI -> "Due giocatori non possono scegliere lo stesso Legend.";
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
                partita.getClassifica().stream().map(Giocatore::getNome).toList(),
                convertiAccumulo(partita.getAccumulo()),
                abilitaDisponibile());
    }

    /** Nome dell'abilità che il giocatore di turno può usare ora, oppure null. */
    private String abilitaDisponibile() {
        if (!partita.isAbilitaUsabile()) {
            return null;
        }
        return nomeAbilita(partita.getGiocatori().get(partita.getIndiceGiocatoreAttivo()).getLegend());
    }

    private static StatoPartitaDto.Accumulo convertiAccumulo(Accumulo accumulo) {
        return accumulo == null
                ? null
                : new StatoPartitaDto.Accumulo(accumulo.totale(), accumulo.valoreUltimaCarta());
    }

    private List<StatoPartitaDto.Giocatore> convertiGiocatori() {
        String nomeAttivo = partita.getNomeGiocatoreAttivo();
        boolean partitaInCorso = partita.getFase() != FaseTurno.TERMINATA;
        return partita.getGiocatori().stream()
                .map(g -> new StatoPartitaDto.Giocatore(g.getNome(), g.getMano().size(), descriviStato(g),
                        partitaInCorso && g.getNome().equals(nomeAttivo),
                        g.getLegend() == null ? null : nomeLegend(g.getLegend()),
                        ricarica(g.getAbilitaAttiva())))
                .toList();
    }

    private static int ricarica(AbilitaAttiva abilita) {
        return abilita == null ? 0 : abilita.getTurniDiRicarica();
    }

    private String descriviStato(Giocatore giocatore) {
        return switch (giocatore.getStato()) {
            case IN_GIOCO -> giocatore.isCongelato() ? "congelato" : "in gioco";
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
     * Inoltra la scelta di non rispondere allo stacking: il giocatore pesca tutte
     * le carte accumulate e perde il turno. La pesca viene comunicata dagli eventi.
     *
     * @return true se il comando è stato accettato (c'era un accumulo a cui rispondere)
     */
    public boolean accettaPenalita() {
        return partita.accettaPenalita();
    }

    /**
     * Inoltra l'uso dell'abilità attiva del giocatore di turno (Step 6). Se l'abilità
     * chiede un bersaglio, la partita passa in fase di attesa e la UI risponde con
     * {@link #rispondiInput(String)}, come per una carta.
     *
     * @return true se il comando è stato accettato
     */
    public boolean usaAbilita() {
        return partita.usaAbilita();
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
            case PESCA_FORZATA -> evento.quantita() == 0
                    ? giocatore + " non pesca nessuna carta" + descriviOrigine(evento.origine()) + "."
                    : giocatore + " pesca " + descriviNumeroCarte(evento.quantita())
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
            case ACCUMULO_AUMENTATO -> "Stacking: l'accumulo sale a " + descriviNumeroCarte(evento.quantita()) + ".";
            // Le carte pescate le racconta subito dopo l'evento PESCA_FORZATA.
            case PENALITA_ACCETTATA -> giocatore + " non risponde allo stacking e accetta la penalita.";
            case ABILITA_USATA -> giocatore + " (" + nomeLegend(evento.legend()) + ") usa l'abilita "
                    + nomeAbilita(evento.legend()) + spiegazioneUso(evento.legend()) + ".";
            case PESCA_RIDOTTA -> "Passiva " + nomePassiva(evento.legend()) + ": " + giocatore + " evita "
                    + descriviNumeroCarte(evento.quantita()) + ".";
            case BLOCCO_EVITATO -> "Passiva " + nomePassiva(evento.legend()) + ": il Blocca non ha effetto su "
                    + giocatore + ".";
            case GIOCATORE_CONGELATO -> giocatore + " congela " + evento.altroGiocatore()
                    + ": saltera il suo prossimo turno.";
            case TURNO_CONGELATO -> giocatore + " e congelato e salta il turno.";
            case CARTE_REGALATE -> giocatore + " regala " + descriviNumeroCarte(evento.quantita())
                    + " a " + evento.altroGiocatore() + ".";
            case CARTA_SCELTA -> giocatore + " tiene una carta e ne rimette " + evento.quantita()
                    + " in fondo al mazzo.";
        };
    }

    /**
     * Le abilità senza bersaglio non producono altri eventi subito: qui si dice cosa
     * succede. Le altre lo raccontano i loro eventi (pesca, scambio, congelamento...).
     */
    private static String spiegazioneUso(Legend legend) {
        return switch (legend) {
            case DALTONICO -> ": in questo turno puo giocare qualsiasi carta";
            case ACROBATA -> ": dopo questo turno ne gioca subito un altro";
            case ROCCIA, MERCANTE, FANTASMA, FREEZER -> "";
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
            case ACCUMULO -> " accumulate con lo stacking";
            case CASTIGATORE -> " per Il Castigatore";
        };
    }

    private static StatoPartitaDto.Fase convertiFase(FaseTurno fase) {
        return switch (fase) {
            case NORMALE -> StatoPartitaDto.Fase.NORMALE;
            case POST_PESCA -> StatoPartitaDto.Fase.POST_PESCA;
            case ATTESA_INPUT -> StatoPartitaDto.Fase.ATTESA_INPUT;
            case ATTESA_UNO -> StatoPartitaDto.Fase.ATTESA_UNO;
            case RISPOSTA_STACKING -> StatoPartitaDto.Fase.RISPOSTA_STACKING;
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
            case SCELTA_CARTA -> "Doppia pesca: scegli la carta da tenere (l'altra torna in fondo al mazzo):";
        };
    }
}
