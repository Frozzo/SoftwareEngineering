package unolegends.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Carta 0 della No Mercy: tutti i giocatori in gioco passano la propria mano al
 * giocatore successivo <b>nel senso di gioco</b> (regola ufficiale, scelta del
 * team).
 *
 * <p>Come per {@link EffettoScambiaMano}, tutto il lavoro sta nell'effetto
 * (scelta 4.B): Partita fornisce l'ordine dei giocatori in gioco, l'effetto sposta
 * le mani. I giocatori già terminati (Endless) o eliminati non partecipano, e dallo
 * Step 6 nemmeno i congelati (decisione presa), che tengono la loro mano.</p>
 *
 * <p>Regole decise: dopo il passaggio nessuno deve dichiarare UNO; se lo 0 è
 * l'ultima carta di chi lo gioca, ha vinto e le mani non si spostano.</p>
 */
public class EffettoPassaMani implements Ieffetto {

    @Override
    public void attivaEffetto(Partita partita) {
        Giocatore chiHaGiocato = partita.getGiocatoreAttivo();
        if (chiHaGiocato.getMano().isEmpty()) {
            return; // ultima carta: ha vinto, le mani non si spostano
        }
        List<Giocatore> ordine = partita.getGiocatoriPartecipantiNelSensoDiGioco();
        List<List<Carta>> mani = new ArrayList<>();
        for (Giocatore giocatore : ordine) {
            mani.add(giocatore.svuotaMano());
        }
        // La mano del giocatore i va al giocatore i + 1 (l'ultimo la passa al primo).
        for (int i = 0; i < ordine.size(); i++) {
            ordine.get((i + 1) % ordine.size()).riceviCarte(mani.get(i));
        }
        partita.registraEvento(EventoPartita.maniPassate(chiHaGiocato));
    }

    @Override
    public boolean isAmmessoComeCartaIniziale() {
        return false;
    }
}
