package unolegends.model;

public class Effetto_Blocca_Turno implements Ieffetto {

    public void attivaEffetto(Partita partita) {
        partita.segnalaSaltoTurno();
    }

}
