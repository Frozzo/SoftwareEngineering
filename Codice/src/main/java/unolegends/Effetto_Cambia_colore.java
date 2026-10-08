package unolegends;

import java.util.Objects;

public class Effetto_Cambia_colore implements Ieffetto {

    private Colore nuovoColore;

    public Effetto_Cambia_colore() {
    }

    public Effetto_Cambia_colore(Colore nuovoColore) {
        this.nuovoColore = Objects.requireNonNull(nuovoColore, "nuovoColore non puo essere null");
    }

    void setNuovoColore(Colore nuovoColore) {
        this.nuovoColore = Objects.requireNonNull(nuovoColore, "nuovoColore non puo essere null");
    }

    @Override
    public void attivaeffetto(Partita partita) {
        Objects.requireNonNull(partita, "partita non puo essere null");
        partita.applicaColoreScelto();
    }
}
