package unolegends.model;

/**
 * Passiva del Daltonico: può giocare una carta <b>rossa sul verde</b> e una
 * <b>verde sul rosso</b>, come se i due colori fossero lo stesso.
 *
 * <p>Si confronta con il colore attivo, così funziona anche quando in cima c'è un
 * jolly a cui è stato assegnato il rosso o il verde. Aggiunge giocate e non ne
 * toglie: le altre compatibilità restano quelle delle regole.</p>
 */
final class PassivaDaltonico implements IPassiva {
    @Override
    public boolean consenteGiocata(Carta candidata, Carta inCima, Colore coloreAttivo) {
        Colore attivo = coloreAttivo != null ? coloreAttivo : (inCima == null ? null : inCima.getColore());
        Colore colore = candidata.getColore();
        return (colore == Colore.ROSSO && attivo == Colore.VERDE)
                || (colore == Colore.VERDE && attivo == Colore.ROSSO);
    }
}
