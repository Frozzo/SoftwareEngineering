package unolegends.model;

/**
 * Abilità attiva "Congela" del Freezer: un avversario a scelta salta il suo
 * prossimo turno (1 turno, decisione presa).
 *
 * <p>Il congelato è "come se non ci fosse" finché il congelamento dura: il turno lo
 * salta, non è bersagliabile, non risponde allo stacking e la carta 0 non gli passa la
 * mano. Tutto questo non lo fa l'effetto: basta segnare il giocatore come congelato
 * ({@link Giocatore#congela(int)}), e i punti di Partita che già decidono "chi viene
 * dopo" e "chi si può scegliere" lo escludono da soli.</p>
 *
 * <p>Non consuma il turno del Freezer (decisione presa): con 1 solo turno di
 * congelamento l'abilità è già abbastanza debole.</p>
 */
final class EffettoCongela extends EffettoConBersaglio {
    private final int turni;

    EffettoCongela(int turni) {
        if (turni <= 0) {
            throw new IllegalArgumentException("turni deve essere positivo");
        }
        this.turni = turni;
    }

    @Override
    protected void applicaAlBersaglio(Partita partita, Giocatore chiGioca, Giocatore bersaglio) {
        bersaglio.congela(turni);
        partita.registraEvento(EventoPartita.giocatoreCongelato(chiGioca, bersaglio));
    }
}
