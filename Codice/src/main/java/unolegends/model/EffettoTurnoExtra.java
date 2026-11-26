package unolegends.model;

/**
 * Carta "Salta tutti" della No Mercy (e, allo Step 6, abilità attiva "Double
 * card"): tutti gli altri saltano il turno e chi l'ha giocata gioca un altro turno
 * completo, in cui può giocare o pescare come all'inizio del turno.
 *
 * <p>Non fa saltare i giocatori uno per uno: chiede a {@link Partita} un turno
 * extra, che Partita consuma al prossimo avanzamento lasciando il turno allo stesso
 * giocatore. Così funziona con qualunque numero di giocatori e con terminati ed
 * eliminati. Se la carta era l'ultima, chi l'ha giocata ha vinto e il turno extra
 * viene ignorato.</p>
 */
public class EffettoTurnoExtra implements Ieffetto {

    @Override
    public void attivaEffetto(Partita partita) {
        partita.concediTurnoExtra();
    }

    @Override
    public boolean isAmmessoComeCartaIniziale() {
        return false;
    }
}
