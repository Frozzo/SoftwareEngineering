package unolegends.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Composite di effetti: applica le strategie contenute nell'ordine ricevuto, dovrebbe esse giusto samue dammi na mano uoooooo
 */
public final class EffettoComposito implements Ieffetto {
    private final List<Ieffetto> effetti;

    public EffettoComposito(List<Ieffetto> effetti) {
        Objects.requireNonNull(effetti, "effetti non puo essere null");
        if (effetti.isEmpty()) {
            throw new IllegalArgumentException("Un effetto composto deve contenere almeno un effetto");
        }

        List<Ieffetto> copiaEffetti = new ArrayList<>(effetti.size()); //non ho ben capito la implementazione
        for (Ieffetto effetto : effetti) {
            copiaEffetti.add(Objects.requireNonNull(effetto, "un effetto non puo essere null"));
        }
        if (contaEffettiConInput(copiaEffetti) > 1) {
            throw new IllegalArgumentException("Un effetto composto puo richiedere un solo input");
        }
        this.effetti = Collections.unmodifiableList(copiaEffetti);
    }

    private static int contaEffettiConInput(List<Ieffetto> effetti) {
        int numeroRichieste = 0;
        for (Ieffetto effetto : effetti) {
            if (effetto instanceof EffettoComposito composto) {
                numeroRichieste += contaEffettiConInput(composto.effetti);
            } else if (effetto.getRichiestaInput() != null) {
                numeroRichieste++;
            }
        }
        return numeroRichieste;
    }

    public void attivaeffetto(Partita partita) { //non so se qua co override va bene che vittoria ha detto che la prof si tilta
        for (Ieffetto effetto : effetti) {
            effetto.attivaeffetto(partita);
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
    public void attivaeffetto(Partita partita, String risposta) {
        boolean rispostaInoltrata = false;
        for (Ieffetto effetto : effetti) {
            if (!rispostaInoltrata && effetto.getRichiestaInput() != null) {
                effetto.attivaeffetto(partita, risposta);
                rispostaInoltrata = true;
            } else {
                effetto.attivaeffetto(partita);
            }
        }
    }

    public static boolean contieneEffetto(Ieffetto effetto, Class<? extends Ieffetto> tipoEffetto) {
        Objects.requireNonNull(effetto, "effetto non puo essere null");
        Objects.requireNonNull(tipoEffetto, "tipoEffetto non puo essere null");

        if (tipoEffetto.isInstance(effetto)) {
            return true;
        }
        if (effetto instanceof EffettoComposito composto) {
            return composto.effetti.stream()
                    .anyMatch(effettoComponente -> contieneEffetto(effettoComponente, tipoEffetto));
        }
        return false;
    }
}
