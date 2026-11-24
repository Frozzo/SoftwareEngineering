package unolegends.model;

import java.util.Objects;

/**
 * Dati transitori di una pesca forzata, passati alle regole per calcolare quante
 * carte vanno pescate davvero. Raggrupparli in un unico oggetto immutabile evita
 * di cambiare la firma dei metodi quando serviranno nuove informazioni.
 *
 * @param bersaglio    giocatore che deve pescare
 * @param numeroCarte  numero di carte richiesto da chi ha causato la pesca
 * @param origine      causa della pesca
 */
public record ContestoPesca(Giocatore bersaglio, int numeroCarte, OriginePesca origine) {
    public ContestoPesca {
        Objects.requireNonNull(bersaglio, "bersaglio non puo essere null");
        Objects.requireNonNull(origine, "origine non puo essere null");
        if (numeroCarte < 0) {
            throw new IllegalArgumentException("numeroCarte non puo essere negativo");
        }
    }
}
