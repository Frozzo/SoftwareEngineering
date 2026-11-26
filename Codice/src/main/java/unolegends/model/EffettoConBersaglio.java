package unolegends.model;

import java.util.List;

/**
 * Base degli effetti in cui chi gioca <b>sceglie un avversario</b>: carta 7, e le
 * abilità Castigatore, Gifted card, Scambia mazzo e Congela (Step 6).
 *
 * <p>È un <b>Template Method</b> (GoF): la parte comune (proporre i bersagli,
 * validare la risposta, trovare il giocatore scelto) sta qui, una volta sola; le
 * sottoclassi ridefiniscono soltanto {@link #applicaAlBersaglio} e, se serve,
 * {@link #ammetteBersaglio}. Prima la carta 7 faceva tutto da sola: con quattro
 * abilità nuove lo stesso codice si sarebbe ripetuto cinque volte.</p>
 *
 * <p>I bersagli partono sempre da {@link Partita#getBersagliSelezionabili()}, che
 * esclude chi è fuori gioco, chi è congelato e Ghost. Così nessun effetto deve
 * ricordarsi di quelle regole.</p>
 */
abstract class EffettoConBersaglio implements Ieffetto {

    /** Applica l'effetto al bersaglio scelto da chi gioca. */
    protected abstract void applicaAlBersaglio(Partita partita, Giocatore chiGioca, Giocatore bersaglio);

    /**
     * Condizione in più sul bersaglio, propria dell'effetto (per esempio Scambia
     * mazzo: più di una carta in mano). Di default ogni bersaglio selezionabile va bene.
     */
    protected boolean ammetteBersaglio(Giocatore bersaglio) {
        return true;
    }

    /** Avversari che si possono scegliere ora. */
    protected final List<Giocatore> getBersagli(Partita partita) {
        return partita.getBersagliSelezionabili().stream().filter(this::ammetteBersaglio).toList();
    }

    @Override
    public RichiestaInput getRichiestaInput(Partita partita) {
        return new RichiestaInput(TipoRichiesta.SCELTA_GIOCATORE,
                getBersagli(partita).stream().map(Giocatore::getNome).toList());
    }

    @Override
    public boolean validaRispostaInput(String risposta, Partita partita) {
        return trovaBersaglio(risposta, partita) != null;
    }

    /** Usabile come abilità solo se c'è almeno un bersaglio. */
    @Override
    public boolean isAttivabile(Partita partita) {
        return !getBersagli(partita).isEmpty();
    }

    /** Senza risposta non c'è un bersaglio: l'effetto non fa nulla. */
    @Override
    public void attivaEffetto(Partita partita) {
    }

    @Override
    public void attivaEffetto(Partita partita, String risposta) {
        if (risposta == null) {
            attivaEffetto(partita);
            return;
        }
        Giocatore bersaglio = trovaBersaglio(risposta, partita);
        if (bersaglio == null) {
            throw new IllegalArgumentException("Giocatore non valido: " + risposta);
        }
        applicaAlBersaglio(partita, partita.getGiocatoreAttivo(), bersaglio);
    }

    private Giocatore trovaBersaglio(String risposta, Partita partita) {
        if (risposta == null) {
            return null;
        }
        return getBersagli(partita).stream()
                .filter(g -> g.getNome().equalsIgnoreCase(risposta.trim()))
                .findFirst()
                .orElse(null);
    }
}
