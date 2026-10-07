package unolegends;

public class Effetto_Blocca_Turno implements Ieffetto {

    @Override
    public void attivaeffetto(Partita partita) {
        partita.aggiornaGiocatoreAttivo(); // Passa al giocatore successivo
        // Implementazione dell'effetto "Blocca Turno"
        // Logica per bloccare il turno del giocatore successivo
        System.out.println("Effetto 'Blocca Turno' attivato: il turno del giocatore successivo è bloccato.");
    }

}
