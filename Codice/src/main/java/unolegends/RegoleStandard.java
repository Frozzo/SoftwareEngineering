package unolegends;

/**
 * Implementazione base delle regole di gioco: una carta è giocabile se è compatibile
 * con la carta in cima alla pila (o se la cima è null).
 */
public class RegoleStandard implements RegoleDiGioco {

    @Override
    public boolean isGiocabile(Carta candidata, Carta inCima, Partita partita) {
        if (candidata == null) return false;
        if (inCima == null) return true;
        return candidata.compatibileCon(inCima);
    }
}
