package unolegends.model;

public class Effetto_pescata_carte implements Ieffetto {
    private int numeroCarteDaPescare;
    public Effetto_pescata_carte(int numeroCarteDaPescare) {
        this.numeroCarteDaPescare = numeroCarteDaPescare;
    }

    public void attivaeffetto(Partita partita) {
         // nota non fa saltare il turno.
        Giocatore prossimoGiocatore = partita.getProssimoGiocatore();
        for (int i = 0; i < numeroCarteDaPescare; i++) {
            partita.pescaCartaForzata(prossimoGiocatore);
        }
    }
}