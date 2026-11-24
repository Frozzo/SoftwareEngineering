package unolegends.model;

import java.util.Objects;

/**
 * Fa pescare al giocatore successivo un numero fisso di carte e gli fa saltare
 * il turno. La pesca passa interamente da {@link Partita#applicaPescaForzata},
 * così le regole possono modificarne il numero in base all'origine.
 */
public class Effetto_pescata_carte implements Ieffetto {
    private final int numeroCarteDaPescare;
    private final OriginePesca origine;

    public Effetto_pescata_carte(int numeroCarteDaPescare, OriginePesca origine) {
        this.numeroCarteDaPescare = numeroCarteDaPescare;
        this.origine = Objects.requireNonNull(origine, "origine non puo essere null");
    }

    public void attivaEffetto(Partita partita) {
        Giocatore prossimoGiocatore = partita.getProssimoGiocatore();
        partita.applicaPescaForzata(prossimoGiocatore, numeroCarteDaPescare, origine);
        partita.segnalaSaltoTurno();
    }
}
