package unolegends.model;

import java.util.List;

/**
 * Carta 7 della No Mercy e abilità attiva "Scambia mazzo" del Fantasma: chi gioca
 * sceglie un avversario e i due si scambiano la mano.
 *
 * <p><b>Perché tutto dentro l'effetto</b> (scelta 4.B): l'effetto riceve
 * {@link Partita}, che gli fornisce il giocatore di turno e i bersagli, e sposta le
 * mani con i metodi package-private di {@link Giocatore}. Partita non deve conoscere
 * questo effetto.</p>
 *
 * <p>Dallo Step 6 la scelta del bersaglio la fa {@link EffettoConBersaglio}
 * (Template Method): i bersagli sono quelli selezionabili, quindi Ghost e i giocatori
 * congelati sono esclusi senza codice qui.</p>
 *
 * <p>Le due versioni differiscono in due punti, decisi con il team:</p>
 * <ul>
 *   <li><b>carta 7</b>: se è l'ultima carta di chi la gioca, ha vinto e lo scambio non
 *       avviene (nessuna richiesta); se non c'è nessun bersaglio (per esempio l'unico
 *       avversario è Ghost) la carta si gioca senza scambio;</li>
 *   <li><b>Scambia mazzo</b>: il bersaglio deve avere <b>più di una carta</b>, così non
 *       si "ruba" una mano quasi vincente. Per questo non serve far perdere il turno a
 *       chi la usa (decisione presa: nessuna abilità consuma il turno).</li>
 * </ul>
 *
 * <p>Dopo lo scambio nessuno deve dichiarare UNO (lo garantisce
 * {@code Partita.deveDichiarareUno}; un'abilità non è una giocata).</p>
 */
public class EffettoScambiaMano extends EffettoConBersaglio {
    private final boolean comeCarta;

    /** Carta 7 della No Mercy. */
    public EffettoScambiaMano() {
        this(true);
    }

    private EffettoScambiaMano(boolean comeCarta) {
        this.comeCarta = comeCarta;
    }

    /** Abilità attiva "Scambia mazzo": il bersaglio deve avere più di una carta. */
    static EffettoScambiaMano abilitaScambiaMazzo() {
        return new EffettoScambiaMano(false);
    }

    @Override
    protected boolean ammetteBersaglio(Giocatore bersaglio) {
        return comeCarta || bersaglio.getMano().size() > 1;
    }

    /**
     * Come carta, non chiede nulla se la 7 è l'unica carta in mano (la si sta
     * giocando come ultima carta) o se nessuno è bersagliabile.
     */
    @Override
    public RichiestaInput getRichiestaInput(Partita partita) {
        if (comeCarta && (partita.getGiocatoreAttivo().getMano().size() <= 1 || getBersagli(partita).isEmpty())) {
            return null;
        }
        return super.getRichiestaInput(partita);
    }

    @Override
    protected void applicaAlBersaglio(Partita partita, Giocatore giocatore, Giocatore bersaglio) {
        if (giocatore.getMano().isEmpty()) {
            return; // ultima carta: ha vinto, niente scambio
        }
        List<Carta> manoGiocatore = giocatore.svuotaMano();
        List<Carta> manoBersaglio = bersaglio.svuotaMano();
        giocatore.riceviCarte(manoBersaglio);
        bersaglio.riceviCarte(manoGiocatore);
        partita.registraEvento(EventoPartita.maniScambiate(giocatore, bersaglio));
    }

    @Override
    public boolean isAmmessoComeCartaIniziale() {
        return false;
    }
}
