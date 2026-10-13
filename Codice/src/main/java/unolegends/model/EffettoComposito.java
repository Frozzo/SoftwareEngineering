package unolegends.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composite di effetti: applica le strategie contenute nell'ordine ricevuto
 */
public final class EffettoComposito implements Ieffetto {
    private final List<Ieffetto> effetti;

    public EffettoComposito(List<Ieffetto> effetti) {
        Objects.requireNonNull(effetti, "effetti non puo essere null");
        this.effetti = new ArrayList<>(effetti);
    }

    public void attivaEffetto(Partita partita) {
        for (Ieffetto effetto : effetti) {
            effetto.attivaEffetto(partita);
        }
    }

    @Override
    public RichiestaInput getRichiestaInput() {
        for (Ieffetto effetto : effetti) {
            RichiestaInput richiesta = effetto.getRichiestaInput();
            if (richiesta != null) {
                return richiesta;
            }
        }
        return null;
    }

    @Override
    public boolean validaRispostaInput(String risposta) {
        for (Ieffetto effetto : effetti) {
            if (effetto.getRichiestaInput() != null) {
                return effetto.validaRispostaInput(risposta);
            }
        }
        return risposta == null;
    }

    @Override
    public void attivaEffetto(Partita partita, String risposta) {
        boolean rispostaInoltrata = false;
        for (Ieffetto effetto : effetti) {
            if (!rispostaInoltrata && effetto.getRichiestaInput() != null) {
                effetto.attivaEffetto(partita, risposta);
                rispostaInoltrata = true;
            } else {
                effetto.attivaEffetto(partita);
            }
        }
    }
}
