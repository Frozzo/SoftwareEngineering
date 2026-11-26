package unolegends.model;

/**
 * Passiva del Freezer (Pelle di ghiaccio): la carta <b>Blocca</b> giocata contro di
 * lui non ha effetto.
 *
 * <p>Vale solo per la carta Blocca (decisione presa), non per gli altri salti
 * (cambio giro con 2 giocatori, salto dopo un +2/+4 senza stacking, roulette). Per
 * questo l'hook lo interroga {@link Partita#bloccaProssimo()}, che usa solo
 * {@link Effetto_Blocca_Turno}, e non {@code segnalaSaltoTurno}, che non sa perché si
 * salta.</p>
 */
final class PassivaPelleDiGhiaccio implements IPassiva {
    @Override
    public boolean isImmuneAlBlocca() {
        return true;
    }
}
