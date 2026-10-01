package unolegends;

@FunctionalInterface
public interface EffettoCarta {
    /**
     * Esegue l'azione sul contesto della partita.
     * @param contesto Contiene i riferimenti a: giocatore successivo, mazzo, direzione di gioco, etc.
     */
    void applica(Partita statopartita);
}