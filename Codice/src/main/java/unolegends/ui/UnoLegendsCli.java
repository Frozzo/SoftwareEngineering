package unolegends.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import unolegends.controller.StatoPartitaDto;
import unolegends.controller.UnoLegendsGame;

/**
 * Punto di avvio CLI (hotseat, da 2 a 6 giocatori).
 *
 * <p>Prima il menu di setup (modalità, opzioni, giocatori), poi il ciclo del
 * caso d'uso GiocaTurno. La CLI non conosce le regole: le modalità e le opzioni
 * da proporre le elenca il controller, e la validità delle scelte la verifica il
 * model.</p>
 */
public final class UnoLegendsCli {
    private UnoLegendsCli() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("UnoLegends - CLI");
        System.out.println();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Menu iniziale:");
            System.out.println("0) Nuova partita");
            System.out.println("1) Esci");
            System.out.print("> ");
            String sceltaIniziale = leggiRiga(reader);
            if ("1".equals(sceltaIniziale)) {
                System.out.println("Uscita.");
                return;
            }
            if (!"0".equals(sceltaIniziale)) {
                System.out.println("Scelta non valida. Digita '0' per iniziare oppure '1' per uscire.");
                return;
            }

            // GRASP Controller: UnoLegendsGame rimane pura facade per i comandi di controller
            UnoLegendsGame controller = configuraPartita(reader);

            System.out.println();
            System.out.println("Puoi giocare una carta dalla mano oppure pescare; dopo la pesca puoi giocare una carta o passare.");
            System.out.println("Le carte in mano sono numerate da 0 in poi e vengono mostrate come 'numero colore'.");
            System.out.println("Il sistema mostra automaticamente lo stato dopo ogni scelta.");
            System.out.println();
            boolean continua = true;
            while (continua) {
                // Prima racconta cosa è successo dall'ultimo comando (pesche, salti, colori scelti...).
                controller.prelevaMessaggi().forEach(System.out::println);
                // Rilegge lo stato a ogni iterazione: le scelte precedenti possono averlo modificato.
                StatoPartitaDto stato = controller.richiediStato();
                // La fase la decide il model: la CLI non la deduce da più flag, sceglie solo
                // come presentarla. Lo switch è esaustivo: una fase nuova va gestita qui.
                continua = switch (stato.fase()) {
                    case TERMINATA -> {
                        mostraClassifica(stato);
                        yield false;
                    }
                    case ATTESA_INPUT -> {
                        gestisciRichiestaInput(stato, controller, reader);
                        yield true;
                    }
                    case ATTESA_UNO -> {
                        controller.completaDichiarazioneUno(attendiDichiarazioneUno(reader));
                        System.out.println();
                        yield true;
                    }
                    case RISPOSTA_STACKING -> turnoRispostaStacking(stato, controller, reader);
                    case POST_PESCA -> turnoDopoPesca(stato, controller, reader);
                    case NORMALE -> turnoNormale(stato, controller, reader);
                };
            }
        }
    }

    // ------------------------------------------------------------------
    // Menu di setup
    // ------------------------------------------------------------------

    /**
     * Raccoglie le scelte del setup e avvia la partita. Ordine: modalità, opzioni
     * (una domanda sì/no ciascuna), numero e nomi dei giocatori. Se le scelte non
     * sono valide (per esempio Endless con 2 giocatori) mostra il motivo, scritto
     * dal controller, e ripete il setup. Le scelte si verificano prima di avviare
     * la partita: il controller restituisce la frase d'errore oppure null.
     */
    private static UnoLegendsGame configuraPartita(BufferedReader reader) throws IOException {
        while (true) {
            System.out.println();
            System.out.println("=== Nuova partita ===");
            String modalita = scegliModalita(reader);
            Set<String> opzioniAttive = scegliOpzioni(reader, modalita);
            List<String> nomi = scegliGiocatori(reader);
            String errore = UnoLegendsGame.verificaImpostazioni(modalita, opzioniAttive, nomi);
            if (errore == null) {
                return UnoLegendsGame.avviaPartita(modalita, opzioniAttive, nomi);
            }
            System.out.println("Impostazioni non valide: " + errore);
            System.out.println("Ripetiamo la configurazione.");
        }
    }

    private static String scegliModalita(BufferedReader reader) throws IOException {
        List<String> modalita = UnoLegendsGame.getModalitaDisponibili();
        if (modalita.size() == 1) {
            System.out.println("Modalita: " + modalita.get(0));
            return modalita.get(0);
        }
        while (true) {
            System.out.println("Scegli la modalita:");
            for (int i = 0; i < modalita.size(); i++) {
                System.out.println(i + ") " + modalita.get(i));
            }
            System.out.print("> ");
            Integer indice = parseIndice(leggiRiga(reader));
            if (indice != null && indice < modalita.size()) {
                return modalita.get(indice);
            }
            System.out.println("Scelta non valida.");
        }
    }

    private static Set<String> scegliOpzioni(BufferedReader reader, String modalita) throws IOException {
        Set<String> attive = new HashSet<>();
        for (String opzione : UnoLegendsGame.getOpzioniConfigurabili(modalita)) {
            System.out.println(opzione + ": " + UnoLegendsGame.descriviOpzione(opzione) + ".");
            if (chiediSiNo(reader, "Attivare " + opzione + "?")) {
                attive.add(opzione);
            }
        }
        return attive;
    }

    private static List<String> scegliGiocatori(BufferedReader reader) throws IOException {
        int minimo = UnoLegendsGame.getMinimoGiocatori();
        int massimo = UnoLegendsGame.getMassimoGiocatori();
        int numero;
        while (true) {
            System.out.print("Numero di giocatori (" + minimo + "-" + massimo + "): ");
            Integer letto = parseIndice(leggiRiga(reader));
            if (letto != null && letto >= minimo && letto <= massimo) {
                numero = letto;
                break;
            }
            System.out.println("Valore non valido.");
        }
        List<String> nomi = new ArrayList<>();
        for (int i = 1; i <= numero; i++) {
            String predefinito = "Giocatore" + i;
            System.out.print("Nome del giocatore " + i + " (invio = " + predefinito + "): ");
            String nome = leggiRiga(reader);
            nomi.add(nome.isEmpty() ? predefinito : nome);
        }
        return nomi;
    }

    private static boolean chiediSiNo(BufferedReader reader, String domanda) throws IOException {
        while (true) {
            System.out.print(domanda + " (s/n): ");
            String risposta = leggiRiga(reader);
            if ("s".equalsIgnoreCase(risposta)) {
                return true;
            }
            if ("n".equalsIgnoreCase(risposta)) {
                return false;
            }
            System.out.println("Rispondi 's' oppure 'n'.");
        }
    }

    /** Legge una riga senza spazi esterni; se l'input finisce (Ctrl+Z / Ctrl+D) chiude la CLI con IOException. */
    private static String leggiRiga(BufferedReader reader) throws IOException {
        String riga = reader.readLine();
        if (riga == null) {
            throw new IOException("Input terminato.");
        }
        return riga.trim();
    }

    // ------------------------------------------------------------------
    // Partita in corso
    // ------------------------------------------------------------------

    /**
     * Inizio del turno: il giocatore può giocare una carta, pescare oppure uscire.
     *
     * @return false se il giocatore ha scelto di uscire
     */
    private static boolean turnoNormale(StatoPartitaDto stato, UnoLegendsGame controller,
                                        BufferedReader reader) throws IOException {
        mostraStato(stato);
        System.out.println("Scegli:\n 0) gioca carta\n 1) pesca carta\n 2) esci");
        System.out.print("> ");
        String scelta = leggiRiga(reader);

        if ("2".equals(scelta)) {
            return false;
        }
        if ("0".equals(scelta)) {
            System.out.println("Digita l'indice della carta da giocare.");
            System.out.print("> ");
            giocaCarta(leggiRiga(reader), controller);
            return true;
        }
        if ("1".equals(scelta)) {
            if (!controller.pescaCarta()) {
                System.out.println("Pesca non consentita.");
            }
            return true;
        }
        System.out.println("Comando non riconosciuto.");
        return true;
    }

    /**
     * Stacking (Step 5): c'è un accumulo di pesca e il giocatore deve decidere se
     * rispondere con una carta di pesca di valore sufficiente oppure accettare la
     * penalità. Quali carte sono valide lo decide il model: la CLI mostra solo la
     * soglia ricevuta nel DTO.
     *
     * @return false se il giocatore ha scelto di uscire
     */
    private static boolean turnoRispostaStacking(StatoPartitaDto stato, UnoLegendsGame controller,
                                                 BufferedReader reader) throws IOException {
        mostraStato(stato);
        StatoPartitaDto.Accumulo accumulo = stato.accumulo();
        System.out.println("Stacking in corso: " + accumulo.carteDaPescare() + " carte accumulate.");
        System.out.println("Puoi rispondere con una carta +" + accumulo.valoreMinimoRisposta()
                + " o superiore (di qualsiasi colore), oppure accettare e pescarle tutte.");
        System.out.println("Scegli:\n 0) rispondi con una carta\n 1) accetta la penalita\n 2) esci");
        System.out.print("> ");
        String scelta = leggiRiga(reader);

        if ("2".equals(scelta)) {
            return false;
        }
        if ("0".equals(scelta)) {
            System.out.println("Digita l'indice della carta da giocare.");
            System.out.print("> ");
            giocaCarta(leggiRiga(reader), controller);
            return true;
        }
        if ("1".equals(scelta)) {
            if (!controller.accettaPenalita()) {
                System.out.println("Comando non consentito.");
            }
            return true;
        }
        System.out.println("Comando non riconosciuto.");
        return true;
    }

    /**
     * Dopo una pesca volontaria il giocatore può giocare una carta della mano
     * oppure passare il turno.
     */
    private static boolean turnoDopoPesca(StatoPartitaDto stato, UnoLegendsGame controller,
                                          BufferedReader reader) throws IOException {
        mostraStato(stato);
        if (stato.cartaAppenaPescata() != null) {
            System.out.println("Carta pescata: " + stato.cartaAppenaPescata());
        }
        System.out.println("Scegli: indica l'indice della carta che vuoi giocare oppure 'p' per passare il turno.");
        System.out.print("> ");
        String scelta = leggiRiga(reader);
        if ("p".equalsIgnoreCase(scelta)) {
            System.out.println(controller.passaTurno() ? "Turno passato." : "Passaggio turno non consentito.");
            return true;
        }
        giocaCarta(scelta, controller);
        return true;
    }

    /**
     * Interpreta l'indice e chiede la giocata al controller. Se la carta richiede
     * un input (per esempio un colore), la richiesta arriverà al prossimo giro del
     * ciclo, con la fase ATTESA_INPUT.
     */
    private static void giocaCarta(String testoIndice, UnoLegendsGame controller) {
        Integer indiceCarta = parseIndice(testoIndice);
        if (indiceCarta == null) {
            System.out.println("Valore non valido.");
            return;
        }
        System.out.println(controller.giocaCarta(indiceCarta) ? "Carta giocata." : "Mossa non valida.");
    }

    /**
     * Unico punto della CLI che risponde alle richieste di input, qualunque sia
     * la loro origine (carta iniziale, jolly giocato, in futuro bersagli e abilità).
     */
    private static void gestisciRichiestaInput(StatoPartitaDto stato, UnoLegendsGame controller,
                                               BufferedReader reader) throws IOException {
        System.out.println(stato.nomeGiocatoreAttivo() + ", rispondi:");
        String risposta = richiediInput(reader, stato.richiestaInput());
        if (!controller.rispondiInput(risposta)) {
            System.out.println("Risposta non valida. Riprova.");
        }
    }

    /**
     * Presenta lo snapshot restituito dal controller. Stampa "nessuna" quando
     * non c'è una carta in cima agli scarti e numera la mano da zero per
     * permettere all'utente di selezionare una carta con il relativo indice.
     */
    private static void mostraStato(StatoPartitaDto stato) {
        System.out.println();
        System.out.println("Giocatori (senso " + (stato.sensoOrario() ? "orario" : "antiorario") + "):");
        for (StatoPartitaDto.Giocatore giocatore : stato.giocatori()) {
            System.out.println((giocatore.diTurno() ? " > " : "   ") + giocatore.nome()
                    + " - " + giocatore.numeroCarte() + (giocatore.numeroCarte() == 1 ? " carta" : " carte")
                    + " (" + giocatore.stato() + ")");
        }
        System.out.println("Giocatore attivo: " + stato.nomeGiocatoreAttivo());
        System.out.println("Carta in cima: " + (stato.cartaInCima() == null ? "nessuna" : stato.cartaInCima()));
        System.out.println("Mano:");
        List<String> mano = stato.mano();
        for (int i = 0; i < mano.size(); i++) {
            System.out.println(i + ") " + mano.get(i));
        }
        System.out.println();
    }

    private static void mostraClassifica(StatoPartitaDto stato) {
        System.out.println();
        System.out.println("Partita terminata. Vince " + stato.nomeVincitore() + "!");
        System.out.println("Classifica:");
        List<String> classifica = stato.classifica();
        for (int i = 0; i < classifica.size(); i++) {
            System.out.println(" " + (i + 1) + ". " + classifica.get(i));
        }
    }

    /**
     * Mostra il messaggio della richiesta e raccoglie la risposta da tastiera.
     * Se la richiesta contiene opzioni, le stampa numerate e interpreta
     * l'inserimento come indice: restituisce l'opzione corrispondente, oppure
     * una stringa vuota se l'indice non è valido. Se tra le opzioni è stato
     * scelto un colore, ne mostra anche il nome formattato. Senza opzioni,
     * legge e restituisce direttamente la riga inserita dall'utente.
     */
    private static String richiediInput(BufferedReader reader, StatoPartitaDto.Richiesta richiesta) throws IOException {
        System.out.println(richiesta.messaggio());
        if (!richiesta.opzioni().isEmpty()) {
            List<String> opzioni = richiesta.opzioni();
            for (int i = 0; i < opzioni.size(); i++) {
                System.out.println(i + ") " + opzioni.get(i));
            }
            System.out.print("> ");
            Integer indice = parseIndice(leggiRiga(reader));
            String scelta = indice != null && indice < opzioni.size()
                    ? opzioni.get(indice)
                    : "nessuna opzione valida";
            System.out.println("Hai scelto: " + formattaOpzione(scelta));
            return indice != null && indice < opzioni.size() ? opzioni.get(indice) : "";
        }
        System.out.print("> ");
        return leggiRiga(reader);
    }

    private static String formattaOpzione(String opzione) {
        String codiceAnsi;
        if ("Rosso".equalsIgnoreCase(opzione)) {
            codiceAnsi = "\u001B[31m";
        } else if ("Verde".equalsIgnoreCase(opzione)) {
            codiceAnsi = "\u001B[32m";
        } else if ("Blu".equalsIgnoreCase(opzione)) {
            codiceAnsi = "\u001B[34m";
        } else if ("Giallo".equalsIgnoreCase(opzione)) {
            codiceAnsi = "\u001B[33m";
        } else if ("Nero".equalsIgnoreCase(opzione)) {
            codiceAnsi = "\u001B[30m";
        } else {
            return opzione;
        }
        return codiceAnsi + opzione + "\u001B[0m";
    }

    private static boolean attendiDichiarazioneUno(BufferedReader reader) throws IOException {
        System.out.print("Dichiara UNO entro 5 secondi: ");
        for (int secondo = 1; secondo <= 5; secondo++) {
            long fineAttesa = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < fineAttesa) {
                if (reader.ready()) {
                    return "uno".equalsIgnoreCase(leggiRiga(reader));
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            System.out.print(".");
        }
        return false;
    }

    /**
     * Converte l'input in un indice non negativo. Restituisce null se il testo
     * non è un intero o se il valore è negativo, così il chiamante può gestire
     * l'errore senza interrompere il ciclo della partita.
     */
    private static Integer parseIndice(String valore) {
        try {
            int indice = Integer.parseInt(valore);
            return indice >= 0 ? indice : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}