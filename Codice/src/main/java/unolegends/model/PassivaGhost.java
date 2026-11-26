package unolegends.model;

/**
 * Passiva del Fantasma (Ghost): non può essere scelto come bersaglio dagli effetti in
 * cui chi gioca sceglie un avversario.
 *
 * <p>Gli effetti automatici (carta 0, +2 al successivo, roulette) lo colpiscono
 * comunque, perché non passano da {@link Partita#getBersagliSelezionabili()}.</p>
 */
final class PassivaGhost implements IPassiva {
    @Override
    public boolean isBersagliabile() {
        return false;
    }
}
