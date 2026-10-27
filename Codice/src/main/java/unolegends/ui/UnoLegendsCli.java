package unolegends.ui;

import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

    public static void main(String[] args) throws IOException {
        System.out.println("UnoLegends - CLI");
        System.out.println();

        // Menù iniziale indicizzato: scala per future opzioni (0 avvia partita)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Menu iniziale:");
            System.out.println("0) Avvia partita demo standard (2 giocatori)");
            System.out.println("1) Esci");
            System.out.print("> ");
            String sceltaIniziale = reader.readLine().trim();
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
                // Rilegge lo stato a ogni iterazione: le scelte precedenti possono averlo modificato.
                StatoPartita stato = controller.richiediStato();
                // La partita non richiede altre mosse; mostra il vincitore e termina la CLI.
                if (stato.isPartitaTerminata()) {
                    System.out.println("Partita terminata. Vince " + stato.getNomeVincitore() + "!");
                    return;
                }
                // Risolve prima eventuali richieste rimaste in sospeso (per esempio il colore iniziale).
                // Poi riparte dal controllo dello stato, senza mostrare o chiedere una mossa normale.
                if (stato.getRichiestaInput() != null) {
                    String risposta = richiediInput(reader, stato.getRichiestaInput());
                    if (!controller.rispondiRichiestaInputIniziale(risposta)) {
                        System.out.println("Risposta non valida. Riprova.");
                    }
                    continue;
                }
                mostraStato(stato);

                // Dopo una pesca. il turno è vincolato: si può giocare solo una carta
                // oppure passare. Il continue fa ripartire il ciclo per aggiornare lo stato.
                if (stato.isDeveGiocareCartaPescata()) {
                    System.out.println("Carta pescata: " + stato.getCartaAppenaPescata());
                    System.out.println("Scegli: indica l'indice della carta che vuoi giocare oppure 'p' per passare il turno.");
                    System.out.print("> ");
                    String scelta = reader.readLine().trim();
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

                    if (gestisciGiocata(indiceCarta, controller, reader)) {
                        System.out.println("Carta giocata.");
                    } else {
                        System.out.println("Mossa non valida.");
                    }
                    continue;
                }

                // Se non ci sono richieste pendenti né una carta pescata da risolvere,
                // il giocatore può giocare una carta, pescare oppure uscire.
                System.out.println("Scegli:\n 0) gioca carta\n 1) pesca carta\n 2) esci");
                System.out.print("> ");
                String scelta = reader.readLine().trim();

                if ("2".equals(scelta)) {
                    return;
                }
                if ("0".equals(scelta)) {
                    System.out.println("Digita l'indice della carta da giocare.");
                    System.out.print("> ");
                    Integer indiceCarta = parseIndice(reader.readLine().trim());
                    if (indiceCarta == null) {
                        System.out.println("Valore non valido.");
                        continue;
                    }
                    if (gestisciGiocata(indiceCarta, controller, reader)) {
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
                        System.out.println("Hai pescato una carta: ora puoi solo giocare una carta oppure passare.");
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

    private static boolean gestisciGiocata(int indiceCarta, UnoLegendsGame controller,
                                           BufferedReader reader) throws IOException {
        EsitoGiocata esito = controller.giocaCarta(indiceCarta, null);
        while (esito.getRichiestaInput() != null) {
            String risposta = richiediInput(reader, esito.getRichiestaInput());
            esito = controller.giocaCarta(indiceCarta, risposta);
        }
        boolean giocataValida = esito.isGiocata();
        if (giocataValida && esito.isRichiedeDichiarazioneUno()) {
            boolean unoDichiarato = attendiDichiarazioneUno(reader);
            if (controller.completaDichiarazioneUno(unoDichiarato)) {
                System.out.println("\nUNO non dichiarato: hai pescato due carte di penalita.");
            }
        }
        return giocataValida;
    }

    /**
     * Mostra il messaggio della richiesta e raccoglie la risposta da tastiera.
     * Se la richiesta contiene opzioni, le stampa numerate e interpreta
     * l'inserimento come indice: restituisce l'opzione corrispondente, oppure
     * una stringa vuota se l'indice non è valido. Se tra le opzioni è stato
     * scelto un colore, ne mostra anche il nome formattato. Senza opzioni,
     * legge e restituisce direttamente la riga inserita dall'utente.
     */
    private static String richiediInput(BufferedReader reader, RichiestaInput richiesta) throws IOException {
        System.out.println(richiesta.getMessaggio());
        if (!richiesta.getOpzioni().isEmpty()) {
            List<String> opzioni = richiesta.getOpzioni();
            for (int i = 0; i < opzioni.size(); i++) {
                System.out.println(i + ") " + opzioni.get(i));
            }
            System.out.print("> ");
            Integer indice = parseIndice(reader.readLine().trim());
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
        return reader.readLine().trim();
    }

    private static boolean attendiDichiarazioneUno(BufferedReader reader) throws IOException {
        System.out.print("Dichiara UNO entro 5 secondi: ");
        for (int secondo = 1; secondo <= 5; secondo++) {
            long fineAttesa = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < fineAttesa) {
                if (reader.ready()) {
                    return "uno".equalsIgnoreCase(reader.readLine().trim());
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