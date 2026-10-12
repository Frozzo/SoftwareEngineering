package unolegends.model;

public class Effetto_Blocca_Turno implements Ieffetto {

    @Override
    public void attivaEffetto(Partita partita) {
        partita.segnalaSaltoTurno();
    }

}
