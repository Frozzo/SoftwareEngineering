package unolegends;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Punto di avvio CLI per provare il caso d'uso GiocaTurno in una partita demo.
 */
public final class UnoLegendsCli {
    private UnoLegendsCli() {
    }

    public static void main(String[] args) {
        System.out.println("UnoLegends - CLI");
        System.out.println();

        // Menù iniziale indicizzato: scala per future opzioni (0 avvia partita)
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

            // GRASP Controller: UnoLegendsGame rimane pura facade per i comandi di gioco
            Partita partita = PartitaFactory.creaPartitaStandard();
            UnoLegendsGame gioco = new UnoLegendsGame(partita);

            System.out.println("Demo base: puoi giocare una carta dalla mano oppure pescare; dopo la pesca puoi solo giocare quella carta o passare.");
            System.out.println("Le carte in mano sono numerate da 0 in poi e vengono mostrate come 'numero colore'.");
            System.out.println("Il sistema mostra automaticamente lo stato dopo ogni scelta.");
            System.out.println();
            while (true) {
                StatoTurno stato = gioco.richiediStato();
                mostraStato(stato);

                if (stato.isDeveGiocareCartaPescata()) {
                    System.out.println("Carta pescata: " + stato.getCartaAppenaPescata().toCliString());
                    System.out.println("Scegli: indica l'indice della carta pescata oppure 'p' per passare il turno.");
                    System.out.print("> ");
                    String scelta = scanner.nextLine().trim();
                    if ("p".equalsIgnoreCase(scelta)) {
                        if (gioco.passaTurno()) {
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

                    if (gioco.giocaCarta(indiceCarta)) {
                        System.out.println("Carta giocata.");
                    } else {
                        System.out.println("Mossa non valida.");
                    }
                    continue;
                }

                System.out.println("Scegli: 0) gioca carta  1) pesca carta  2) esci");
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
                    if (gioco.giocaCarta(indiceCarta)) {
                        System.out.println("Carta giocata.");
                    } else {
                        System.out.println("Mossa non valida.");
                    }
                    continue;
                }
                if ("1".equals(scelta)) {
                    if (!gioco.pescaCarta()) {
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

    private static void mostraStato(StatoTurno stato) {
        System.out.println();
        System.out.println("Giocatore attivo: " + stato.getNomeGiocatoreAttivo());
        System.out.println("Carta in cima: " + (stato.getCartaInCima() == null ? "nessuna" : stato.getCartaInCima().toCliString()));
        System.out.println("Mano:");
        List<Carta> mano = stato.getManoGiocatoreAttivo();
        for (int i = 0; i < mano.size(); i++) {
            System.out.println(i + ") " + mano.get(i).toCliString());
        }
        System.out.println();
    }

    private static Integer parseIndice(String valore) {
        try {
            int indice = Integer.parseInt(valore);
            return indice >= 0 ? indice : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}