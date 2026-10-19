package unolegends.ui;

import java.util.List;
import java.util.Scanner;

import unolegends.model.Colore;
import unolegends.model.EsitoGiocata;
import unolegends.model.RichiestaInput;
import unolegends.controller.StatoPartita;
import unolegends.controller.UnoLegendsGame;

/**
 * Punto di avvio CLI per provare il caso d'uso GiocaTurno in una partita demo.
 */
public final class UnoLegendsCli {
    private UnoLegendsCli() {
    }

    /**
     * Gestisce il menu iniziale e il ciclo interattivo della partita. Il menu
     * accetta solo l'avvio della demo o l'uscita; ogni altra scelta termina senza
     * creare una partita. Nel ciclo rilegge lo stato a ogni iterazione per
     * proporre solo le azioni consentite dalla situazione corrente.
     */
    public static void main(String[] args) {
        System.out.println("UnoLegends - CLI");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Menu iniziale:");
            System.out.println("0) Avvia partita demo standard (2 giocatori)");
            System.out.println("1) Esci");
            System.out.print("> ");
            String sceltaIniziale = scanner.nextLine().trim();
            if ("1".equals(sceltaIniziale)) {
                System.out.println("Uscita.");
                return;
            }
            if (!"0".equals(sceltaIniziale)) {
                System.out.println("Scelta non valida. Digita '0' per avviare oppure '1' per uscire.");
                return;
            }

            // GRASP Controller: UnoLegendsGame rimane pura facade per i comandi di controller
            UnoLegendsGame controller = UnoLegendsGame.avviaPartitaStandard();

            System.out.println("Demo base: puoi giocare una carta dalla mano oppure pescare; dopo la pesca puoi solo giocare quella carta o passare.");
            System.out.println("Le carte in mano sono numerate da 0 in poi e vengono mostrate come 'numero colore'.");
            System.out.println("Il sistema mostra automaticamente lo stato dopo ogni scelta.");
            System.out.println();
            while (true) {
                StatoPartita stato = controller.richiediStato();
                if (stato.isPartitaTerminata()) {
                    System.out.println("Partita terminata. Vince " + stato.getNomeVincitore() + "!");
                    return;
                }
                if (stato.getRichiestaInput() != null) { //controlla se c'è una richiesta di input in sospeso in particolare all'inizio della partita
                    String risposta = richiediInput(scanner, stato.getRichiestaInput());
                    if (!controller.rispondiRichiestaInputIniziale(risposta)) {
                        System.out.println("Risposta non valida. Riprova.");
                    }
                    continue;
                }
                mostraStato(stato);

                if (stato.isDeveGiocareCartaPescata()) {
                    System.out.println("Carta pescata: " + stato.getCartaAppenaPescata());
                    System.out.println("Scegli: indica l'indice della carta pescata oppure 'p' per passare il turno.");
                    System.out.print("> ");
                    String scelta = scanner.nextLine().trim();
                    if ("p".equalsIgnoreCase(scelta)) {
                        if (controller.passaTurno()) {
                            System.out.println("Turno passato.");
                        } else {
                            System.out.println("Passaggio turno non consentito.");
                        }
                        continue;
                    }

                    Integer indiceCarta = parseIndice(scelta);
                    if (indiceCarta == null) {
                        System.out.println("Valore non valido.");
                        continue;
                    }

                    if (gestisciGiocata(indiceCarta, controller, scanner,
                            stato.isRichiedeDichiarazioneUno())) {
                        System.out.println("Carta giocata.");
                    } else {
                        System.out.println("Mossa non valida.");
                    }
                    continue;
                }

                System.out.println("Scegli:\n 0) gioca carta\n 1) pesca carta\n 2) esci");
                System.out.print("> ");
                String scelta = scanner.nextLine().trim();

                if ("2".equals(scelta)) {
                    return;
                }
                if ("0".equals(scelta)) {
                    System.out.println("Digita l'indice della carta da giocare.");
                    System.out.print("> ");
                    Integer indiceCarta = parseIndice(scanner.nextLine().trim());
                    if (indiceCarta == null) {
                        System.out.println("Valore non valido.");
                        continue;
                    }
                    if (gestisciGiocata(indiceCarta, controller, scanner,
                            stato.isRichiedeDichiarazioneUno())) {
                        System.out.println("Carta giocata.");
                    } else {
                        System.out.println("Mossa non valida.");
                    }
                    continue;
                }
                if ("1".equals(scelta)) {
                    if (!controller.pescaCarta()) {
                        System.out.println("Mazzo esaurito.");
                    } else {
                        System.out.println("Hai pescato una carta: ora puoi solo giocare quella carta oppure passare.");
                    }
                    continue;
                }

                System.out.println("Comando non riconosciuto.");
            }
        }
    }

    /**
     * Presenta lo snapshot restituito dal controller. Stampa "nessuna" quando
     * non c'è una carta in cima agli scarti e numera la mano da zero per
     * permettere all'utente di selezionare una carta con il relativo indice.
     */
    private static void mostraStato(StatoPartita stato) {
        System.out.println();
        System.out.println("Giocatore attivo: " + stato.getNomeGiocatoreAttivo());
        System.out.println("Carta in cima: " + (stato.getCartaInCima() == null ? "nessuna" : stato.getCartaInCima()));
        System.out.println("Mano:");
        List<String> mano = stato.getManoGiocatoreAttivo();
        for (int i = 0; i < mano.size(); i++) {
            System.out.println(i + ") " + mano.get(i));
        }
        System.out.println();
    }

    /**
     * Raccoglie gli input necessari e inoltra il tentativo di giocata al controller.
     * La dichiarazione UNO viene chiesta solo quando la mano ha due carte; gli eventuali
     * input richiesti dall'effetto vengono raccolti e inviati ripetendo il comando fino
     * a ottenere un esito definitivo. La validità della mossa è decisa dal dominio.
     */
    private static boolean gestisciGiocata(int indiceCarta, UnoLegendsGame controller, Scanner scanner,boolean richiedeDichiarazioneUno) {

        boolean unoDichiarato = richiedeDichiarazioneUno && dichiaraUno(scanner);
        EsitoGiocata esito = controller.giocaCarta(indiceCarta, null, unoDichiarato);
        while (esito.getRichiestaInput() != null) {
            String risposta = richiediInput(scanner, esito.getRichiestaInput());
            esito = controller.giocaCarta(indiceCarta, risposta, unoDichiarato);
        }
        boolean giocataValida = esito.isGiocata();
        if (esito.isMancatoUno()) {
            System.out.println("UNO non dichiarato: hai pescato due carte di penalita.");
        }
        return giocataValida;
    }

    /**
     * Mostra una richiesta prodotta dal dominio e restituisce la risposta dell'utente.
     * Se sono disponibili opzioni, accetta un indice e restituisce la relativa opzione;
     * un indice non valido diventa una risposta vuota, che il dominio può rifiutare.
     * Senza opzioni, legge invece una risposta testuale libera.
     */
    private static String richiediInput(Scanner scanner, RichiestaInput richiesta) {
        System.out.println(richiesta.getMessaggio());
        if (!richiesta.getOpzioni().isEmpty()) {
            List<String> opzioni = richiesta.getOpzioni();
            for (int i = 0; i < opzioni.size(); i++) {
                System.out.println(i + ") " + opzioni.get(i));
            }
            System.out.print("> ");
            Integer indice = parseIndice(scanner.nextLine().trim());
            String scelta = indice != null && indice < opzioni.size()
                    ? opzioni.get(indice)
                    : "nessuna opzione valida";
            String sceltaColorata = scelta;
            for (Colore colore : Colore.values()) {
                if (colore.getNome().equalsIgnoreCase(scelta)) {
                    sceltaColorata = colore.formatta(scelta);
                    break;
                }
            }
            System.out.println("Hai scelto: " + sceltaColorata);
            return indice != null && indice < opzioni.size() ? opzioni.get(indice) : "";
        }
        System.out.print("> ");
        return scanner.nextLine().trim();
    }

    /**
     * Interpreta la dichiarazione UNO solo quando l'utente scrive "uno",
     * ignorando maiuscole/minuscole; qualsiasi altra risposta vale come non dichiarato.
     */
    private static boolean dichiaraUno(Scanner scanner) {
        System.out.print("Dichiara UNO insieme alla giocata digitando 'uno'; altrimenti premi Invio: ");
        return "uno".equalsIgnoreCase(scanner.nextLine().trim());
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