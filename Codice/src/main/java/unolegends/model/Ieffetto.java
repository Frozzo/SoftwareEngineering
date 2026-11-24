package unolegends.model;

/**
 * Strategy dell'effetto di una carta (o, in futuro, di un'abilità attiva).
 *
 * <p>I metodi legati all'input ricevono {@link Partita} perché alcune richieste
 * dipendono dallo stato del gioco: per esempio la scelta di un giocatore
 * bersaglio deve proporre solo gli avversari attualmente bersagliabili.</p>
 */
public interface Ieffetto {

    void attivaEffetto(Partita partita);

    default RichiestaInput getRichiestaInput(Partita partita) {
        return null;
    }

    default boolean validaRispostaInput(String risposta, Partita partita) {
        if (getRichiestaInput(partita) == null) {
            return risposta == null;
        }
        return risposta != null;
    }

    default void attivaEffetto(Partita partita, String risposta) {
        if (getRichiestaInput(partita) != null) {
            throw new IllegalArgumentException("L'effetto richiede un input");
        }
        attivaEffetto(partita);
    }
}
