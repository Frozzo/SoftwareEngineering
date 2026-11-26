package unolegends.domain.rules.impl;

import java.util.Objects;

import unolegends.domain.rules.IStrategiaAccumuloPenalita;
import unolegends.model.Accumulo;
import unolegends.model.Carta;

/**
 * Stacking "valore uguale o superiore": a un accumulo si risponde con una carta di
 * pesca il cui valore è almeno quello dell'<b>ultima</b> carta accumulata. Il
 * colore non conta (decisione presa).
 *
 * <p>Una sola strategia basta per entrambe le modalità. Con il mazzo normale, che
 * ha solo +2 e +4, la regola dà da sola "+2 ← +2/+4, +4 ← +4" (decisione per la
 * partita normale). Con il mazzo No Mercy dà la tabella ufficiale
 * (+2 ← +2/+4/+6/+10 … +10 ← +10). Non serve quindi una
 * {@code StrategiaAccumuloNoMercy} separata, come ipotizzava la ROADMAP.</p>
 *
 * <p>Il valore di pesca lo dà l'effetto della carta
 * ({@code Ieffetto.getValorePenalita()}): nessun {@code instanceof} e nessun
 * elenco di valori di carta. Una carta che non è di pesca vale 0 e non può mai
 * rispondere.</p>
 */
public class StrategiaAccumuloValoreCrescente implements IStrategiaAccumuloPenalita {

    @Override
    public boolean isAccumuloAttivo() {
        return true;
    }

    @Override
    public boolean puoRispondere(Carta candidata, Accumulo accumulo) {
        Objects.requireNonNull(candidata, "candidata non puo essere null");
        Objects.requireNonNull(accumulo, "accumulo non puo essere null");
        int valore = candidata.getEffetto().getValorePenalita();
        return valore > 0 && valore >= accumulo.valoreUltimaCarta();
    }
}
