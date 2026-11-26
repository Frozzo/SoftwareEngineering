package unolegends.domain.rules.impl;

import java.util.ArrayList;
import java.util.List;

import unolegends.domain.rules.IStrategiaPesca;
import unolegends.model.Carta;
import unolegends.model.Giocatore;
import unolegends.model.Partita;

/**
 * Pesca volontaria della No Mercy: il giocatore pesca una carta alla volta finché
 * non trova una carta giocabile. L'ultima carta pescata è quella giocabile.
 *
 * <p>Dopo la pesca il turno resta in {@code POST_PESCA} come nella partita
 * normale: il giocatore può giocare una qualsiasi carta della mano oppure passare
 * (decisione presa). La pesca si ferma anche se mazzo e scarti sono esauriti o se
 * il giocatore arriva al limite della Mercy rule: l'eliminazione la esegue poi
 * Partita.</p>
 *
 * <p>Si aggancia al punto di variazione già previsto dallo Step 1
 * ({@code RegoleDiGioco.pescaVolontaria}): Partita non sa quale pesca è attiva.</p>
 */
public final class StrategiaPescaFinoAGiocabile implements IStrategiaPesca {
    @Override
    public List<Carta> pesca(Giocatore giocatore, Partita partita) {
        List<Carta> cartePescate = new ArrayList<>();
        while (true) {
            Carta carta = partita.pescaSingola(giocatore);
            if (carta == null) {
                break;
            }
            cartePescate.add(carta);
            if (partita.isCartaGiocabile(giocatore, carta) || partita.isDaEliminare(giocatore)) {
                break;
            }
        }
        return cartePescate;
    }
}
