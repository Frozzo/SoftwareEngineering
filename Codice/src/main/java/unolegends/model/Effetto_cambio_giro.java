package unolegends.model;

public class Effetto_cambio_giro implements Ieffetto {

    public void attivaEffetto(Partita partita) {
        partita.cambioGiro();
    }
    
}
