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

    /**
     * true se una carta con questo effetto può essere la prima carta scoperta
     * sugli scarti a inizio partita. Gli effetti che agiscono sulla mano o sul
     * turno di "chi ha giocato" (scambio e rotazione delle mani, Scarta tutto,
     * Salta tutti) non hanno senso senza un giocatore che li abbia giocati:
     * {@link PartitaFactory} rimette quelle carte nel mazzo e ne scopre un'altra.
     *
     * <p>È un metodo polimorfico e non un controllo sul tipo della carta:
     * {@code PartitaFactory} non sa quali effetti esistono (Protected Variations).</p>
     */
    default boolean isAmmessoComeCartaIniziale() {
        return true;
    }

    default void attivaEffetto(Partita partita, String risposta) {
        if (getRichiestaInput(partita) != null) {
            throw new IllegalArgumentException("L'effetto richiede un input");
        }
        attivaEffetto(partita);
    }
}
