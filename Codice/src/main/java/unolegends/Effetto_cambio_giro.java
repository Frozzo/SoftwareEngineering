package unolegends;

public class Effetto_cambio_giro implements Ieffetto {

    @Override
    public void attivaeffetto(Partita partita) {
        partita.cambioGiro(); // Cambia il senso di gioco
        // Implementazione dell'effetto "Cambio Giro"
        // Logica per cambiare il giro del gioco
        System.out.println("Effetto 'Cambio Giro' attivato: il giro del gioco è cambiato.");
    }
    
}
