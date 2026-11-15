package unolegends.model;

public interface Ieffetto {

    void attivaEffetto(Partita partita);

    default RichiestaInput getRichiestaInput() {
        return null;
    }

    default boolean validaRispostaInput(String risposta) {
        if (getRichiestaInput() == null) {
            return risposta == null;
        }
        return risposta != null;
    }

    default void attivaEffetto(Partita partita, String risposta) {
        if (getRichiestaInput() != null) {
            throw new IllegalArgumentException("L'effetto richiede un input");
        }
        attivaEffetto(partita);
    }
}
