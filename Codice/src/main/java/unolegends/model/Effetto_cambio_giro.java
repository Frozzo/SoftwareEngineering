package unolegends.model;

public class Effetto_cambio_giro implements Ieffetto {

    @Override
    public void attivaEffetto(Partita partita) {
        partita.cambioGiro();
    }
    
}
