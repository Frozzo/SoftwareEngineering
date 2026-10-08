package unolegends;

import java.util.Objects;

public class Effetto_Cambia_colore implements Ieffetto {

    @Override
    public void attivaeffetto(Partita partita) {
        Objects.requireNonNull(partita, "partita non puo essere null");
        partita.applicaColoreScelto();
    }
}
