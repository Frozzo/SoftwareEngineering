package unolegends.model;

public interface Ieffetto {

    void attivaEffetto(Partita partita);

    default RichiestaInput getRichiestaInput() {
        return null;
    }

    default boolean validaRispostaInput(String risposta) {
        return getRichiestaInput() == null ? risposta == null : risposta != null;
    }

    default void attivaEffetto(Partita partita, String risposta) {
        if (getRichiestaInput() != null) {
            throw new IllegalArgumentException("L'effetto richiede un input");
        }
        attivaEffetto(partita);
    }
}
