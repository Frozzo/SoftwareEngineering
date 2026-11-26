package unolegends.model;

import java.util.Objects;

/**
 * Effetto delle carte di pesca (+2, +4, +6, +10): fa pescare un numero fisso di
 * carte a chi viene dopo.
 *
 * <p>Dallo Step 5 l'effetto <b>non sa se lo stacking è attivo</b> (Protected
 * Variations): chiede a {@link Partita#applicaPenalitaPesca(int, OriginePesca)} di
 * applicare la penalità e decide Partita, tramite le regole. Senza stacking il
 * successivo pesca subito e salta il turno, come prima; con lo stacking la
 * penalità si aggiunge all'accumulo e il successivo può rispondere. Prima
 * l'effetto chiamava direttamente {@code applicaPescaForzata} e
 * {@code segnalaSaltoTurno}: così lo stesso effetto funziona in entrambi i casi.</p>
 */
public class Effetto_pescata_carte implements Ieffetto {
    private final int numeroCarteDaPescare;
    private final OriginePesca origine;

    public Effetto_pescata_carte(int numeroCarteDaPescare, OriginePesca origine) {
        if (numeroCarteDaPescare <= 0) {
            throw new IllegalArgumentException("numeroCarteDaPescare deve essere positivo");
        }
        this.numeroCarteDaPescare = numeroCarteDaPescare;
        this.origine = Objects.requireNonNull(origine, "origine non puo essere null");
    }

    public void attivaEffetto(Partita partita) {
        partita.applicaPenalitaPesca(numeroCarteDaPescare, origine);
    }

    /** Il valore di pesca è il numero di carte che fa pescare (Information Expert). */
    @Override
    public int getValorePenalita() {
        return numeroCarteDaPescare;
    }
}
