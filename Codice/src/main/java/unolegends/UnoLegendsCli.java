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
        UnoLegendsGame gioco = creaDemo();

        System.out.println("UnoLegends - CLI");
        System.out.println("Demo base: puoi giocare una carta dalla mano oppure pescare; dopo la pesca puoi solo giocare quella carta o passare.");
        System.out.println("Le carte in mano sono numerate da 0 in poi e vengono mostrate come 'numero colore'.");
        System.out.println("Il sistema mostra automaticamente lo stato dopo ogni scelta.");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {
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

    private static UnoLegendsGame creaDemo() {
        List<Carta> carteNormali = CarteCatalogo.getCarteNormali();

        List<Carta> mano1 = new ArrayList<>();
        mano1.add(carteNormali.get(3));
        mano1.add(carteNormali.get(17));

        List<Carta> mano2 = new ArrayList<>();
        mano2.add(carteNormali.get(23));
        mano2.add(carteNormali.get(31));

        Giocatore giocatore1 = new Giocatore("Giocatore1", mano1);
        Giocatore giocatore2 = new Giocatore("Giocatore2", mano2);

        List<Carta> mazzoIniziale = new ArrayList<>();
        mazzoIniziale.add(carteNormali.get(34));
        mazzoIniziale.add(carteNormali.get(9));
        mazzoIniziale.add(carteNormali.get(11));

        List<Carta> scartiIniziali = new ArrayList<>();
        scartiIniziali.add(carteNormali.get(1));

        Partita partita = new Partita(
                List.of(giocatore1, giocatore2),
                new Mazzo(mazzoIniziale),
                new PilaDegliScarti(scartiIniziali),
                0);

        return new UnoLegendsGame(partita);
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