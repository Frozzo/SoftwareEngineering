package unolegends.model;

/**
 * Jolly "roulette colore" della No Mercy: chi gioca la carta sceglie un colore
 * (decisione presa, diversa dalla regola ufficiale in cui lo sceglie la vittima),
 * che diventa il colore attivo. Il giocatore successivo pesca una carta alla volta
 * finché non esce una carta di quel colore, tiene tutte le carte pescate e salta il
 * turno.
 *
 * <p>Riusa {@link Effetto_Cambia_colore} per la richiesta, la validazione e
 * l'applicazione del colore (composizione), e {@link Partita#applicaPescaFinoA}
 * per la pesca, così l'effetto non scrive cicli di pesca.</p>
 *
 * <p>Regole decise:</p>
 * <ul>
 *   <li>un jolly nero pescato <b>non</b> conta come carta del colore scelto;</li>
 *   <li>la pesca si ferma se mazzo e scarti sono esauriti o se la vittima arriva
 *       a 25 carte (Mercy rule);</li>
 *   <li>Salesman non ha lo sconto: la pesca non passa da {@code calcolaCartePescaForzata};</li>
 *   <li>la roulette non si accumula con lo stacking (Step 5).</li>
 * </ul>
 */
public class EffettoRoulette implements Ieffetto {
    private final Effetto_Cambia_colore sceltaColore = new Effetto_Cambia_colore();

    @Override
    public void attivaEffetto(Partita partita) {
        throw new IllegalStateException("La roulette richiede la scelta di un colore");
    }

    @Override
    public RichiestaInput getRichiestaInput(Partita partita) {
        return sceltaColore.getRichiestaInput(partita);
    }

    @Override
    public boolean validaRispostaInput(String risposta, Partita partita) {
        return sceltaColore.validaRispostaInput(risposta, partita);
    }

    @Override
    public void attivaEffetto(Partita partita, String risposta) {
        sceltaColore.attivaEffetto(partita, risposta);
        Colore coloreScelto = partita.getColoreAttuale();
        Giocatore vittima = partita.getProssimoGiocatore();
        partita.applicaPescaFinoA(vittima, carta -> carta.getColore() == coloreScelto, OriginePesca.ROULETTE);
        partita.segnalaSaltoTurno();
    }
}
