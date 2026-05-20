package unolegends;

/**
 * Interfaccia che rappresenta la policy delle regole di gioco.
 * Implementazioni concrete possono fornire comportamenti diversi
 * (es. RegoleStandard, RegoleHouse).
 */
public interface RegoleDiGioco {
    /**
     * Determina se una carta candidata può essere giocata sopra la carta in cima
     * alla pila degli scarti, nel contesto della partita fornita.
     *
     * @param candidata carta proposta dal giocatore
     * @param inCima carta attualmente in cima alla pila degli scarti (può essere null)
     * @param partita contesto della partita
     * @return true se la mossa è consentita secondo le regole
     */
    boolean isGiocabile(Carta candidata, Carta inCima, Partita partita);
}
