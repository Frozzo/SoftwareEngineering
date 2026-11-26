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

    /**
     * Valore di pesca dell'effetto: quante carte fa pescare una carta di pesca
     * (+2 vale 2, +4 vale 4...), 0 per tutti gli altri effetti. Lo usa lo stacking
     * (Step 5) per decidere se una carta può rispondere a un accumulo: si risponde
     * solo con un valore uguale o superiore a quello dell'ultima carta.
     *
     * <p>È un metodo polimorfico e non un attributo di {@link ValoreCarta}: il
     * numero di carte è già scritto nell'effetto che le fa pescare, e ripeterlo
     * nell'enum vorrebbe dire averlo in due punti (decisione 5.A). Evita anche
     * {@code instanceof Effetto_pescata_carte} nelle regole.</p>
     *
     * <p>La roulette vale 0: pesca un numero di carte non noto in anticipo e non si
     * accumula.</p>
     */
    default int getValorePenalita() {
        return 0;
    }

    default void attivaEffetto(Partita partita, String risposta) {
        if (getRichiestaInput(partita) != null) {
            throw new IllegalArgumentException("L'effetto richiede un input");
        }
        attivaEffetto(partita);
    }
}
