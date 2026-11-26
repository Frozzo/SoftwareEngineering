package unolegends.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abilità attiva "Gifted card" del Mercante: chi la usa dà 2 carte della propria mano,
 * scelte a caso, a un avversario a scelta. I loro effetti <b>non</b> si attivano: le
 * carte passano da una mano all'altra senza essere giocate.
 *
 * <p>Si può usare solo con <b>più di 3 carte</b> in mano, cioè almeno 4 (decisione
 * presa). Così chi la usa resta con almeno 2 carte: non vince regalando le ultime
 * carte e non c'è il caso "resto con una carta, devo dichiarare UNO?".</p>
 *
 * <p>Il bersaglio può superare il limite della Mercy rule: l'eliminazione la fa
 * Partita dopo l'abilità, in un punto sicuro, come per le carte.</p>
 */
final class EffettoRegalaCarte extends EffettoConBersaglio {
    /** "Più di 3 carte in mano" (decisione presa). */
    static final int CARTE_MINIME_IN_MANO = 4;

    private final int numeroCarte;

    EffettoRegalaCarte(int numeroCarte) {
        if (numeroCarte <= 0) {
            throw new IllegalArgumentException("numeroCarte deve essere positivo");
        }
        this.numeroCarte = numeroCarte;
    }

    @Override
    public boolean isAttivabile(Partita partita) {
        return partita.getGiocatoreAttivo().getMano().size() >= CARTE_MINIME_IN_MANO && super.isAttivabile(partita);
    }

    @Override
    protected void applicaAlBersaglio(Partita partita, Giocatore chiGioca, Giocatore bersaglio) {
        List<Carta> mano = new ArrayList<>(chiGioca.getMano());
        Collections.shuffle(mano);
        List<Carta> regalate = List.copyOf(mano.subList(0, Math.min(numeroCarte, mano.size())));
        regalate.forEach(chiGioca::rimuoviCarta);
        bersaglio.riceviCarte(regalate);
        partita.registraEvento(EventoPartita.carteRegalate(chiGioca, bersaglio, regalate.size()));
    }
}
