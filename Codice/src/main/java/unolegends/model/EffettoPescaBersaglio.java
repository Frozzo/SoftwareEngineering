package unolegends.model;

import java.util.Objects;

/**
 * Abilità attiva "Il Castigatore" de La Roccia: un avversario a scelta pesca un
 * numero fisso di carte (4).
 *
 * <p>La pesca passa da {@link Partita#applicaPescaForzata}, quindi le passive del
 * bersaglio valgono da sole (Salesman ne pesca una in meno; La Roccia non è immune,
 * perché l'origine non è un +2) e l'evento si registra da solo. Non apre uno stacking:
 * non è una carta, e il bersaglio non è "il prossimo" ma uno scelto. La Mercy rule la
 * controlla Partita dopo l'abilità.</p>
 */
final class EffettoPescaBersaglio extends EffettoConBersaglio {
    private final int numeroCarte;
    private final OriginePesca origine;

    EffettoPescaBersaglio(int numeroCarte, OriginePesca origine) {
        if (numeroCarte <= 0) {
            throw new IllegalArgumentException("numeroCarte deve essere positivo");
        }
        this.numeroCarte = numeroCarte;
        this.origine = Objects.requireNonNull(origine, "origine non puo essere null");
    }

    @Override
    protected void applicaAlBersaglio(Partita partita, Giocatore chiGioca, Giocatore bersaglio) {
        partita.applicaPescaForzata(bersaglio, numeroCarte, origine);
    }
}
