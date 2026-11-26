package unolegends.model;

import java.util.List;

/**
 * Carta 7 della No Mercy (e, allo Step 6, abilità attiva "Scambia mazzo"): chi
 * gioca la carta sceglie un avversario in gioco e i due si scambiano la mano.
 *
 * <p><b>Perché tutto dentro l'effetto</b> (scelta 4.B): l'effetto riceve
 * {@link Partita}, che gli fornisce il giocatore di turno e gli avversari, e
 * sposta le mani con i metodi package-private di {@link Giocatore}. Partita non
 * deve conoscere questo effetto: i controlli che le spettano (eliminazione a 25
 * carte, finestra UNO, vittoria) li fa comunque dopo ogni giocata, per qualunque
 * effetto.</p>
 *
 * <p>Regole decise:</p>
 * <ul>
 *   <li>dopo lo scambio <b>nessuno</b> deve dichiarare UNO, né chi ha giocato né
 *       chi ha ricevuto la mano (lo garantisce {@code Partita.deveDichiarareUno});</li>
 *   <li>se la 7 è l'<b>ultima carta</b> di chi la gioca, ha vinto e lo scambio
 *       non avviene: per questo, in quel caso, non viene chiesto nessun bersaglio.</li>
 * </ul>
 *
 * <p>Il bersaglio è scelto fra gli avversari in gioco; allo Step 6 diventeranno
 * gli avversari "bersagliabili", così Ghost sarà escluso senza toccare questa
 * classe.</p>
 */
public class EffettoScambiaMano implements Ieffetto {

    /** Senza risposta l'effetto si attiva solo quando la 7 era l'ultima carta: non fa nulla. */
    @Override
    public void attivaEffetto(Partita partita) {
        // La carta era l'ultima della mano: chi l'ha giocata ha vinto, niente scambio.
    }

    /**
     * Chiede il bersaglio fra gli avversari in gioco. Non chiede nulla se la 7 è
     * l'unica carta in mano (la si sta giocando come ultima carta).
     */
    @Override
    public RichiestaInput getRichiestaInput(Partita partita) {
        if (partita.getGiocatoreAttivo().getMano().size() <= 1) {
            return null;
        }
        List<String> nomi = partita.getAvversariInGioco().stream().map(Giocatore::getNome).toList();
        return new RichiestaInput(TipoRichiesta.SCELTA_GIOCATORE, nomi);
    }

    @Override
    public boolean validaRispostaInput(String risposta, Partita partita) {
        return trovaBersaglio(risposta, partita) != null;
    }

    @Override
    public void attivaEffetto(Partita partita, String risposta) {
        Giocatore giocatore = partita.getGiocatoreAttivo();
        if (giocatore.getMano().isEmpty()) {
            return; // ultima carta: ha vinto, niente scambio
        }
        Giocatore bersaglio = trovaBersaglio(risposta, partita);
        if (bersaglio == null) {
            throw new IllegalArgumentException("Giocatore non valido: " + risposta);
        }
        List<Carta> manoGiocatore = giocatore.svuotaMano();
        List<Carta> manoBersaglio = bersaglio.svuotaMano();
        giocatore.riceviCarte(manoBersaglio);
        bersaglio.riceviCarte(manoGiocatore);
        partita.registraEvento(EventoPartita.maniScambiate(giocatore, bersaglio));
    }

    private static Giocatore trovaBersaglio(String risposta, Partita partita) {
        if (risposta == null) {
            return null;
        }
        return partita.getAvversariInGioco().stream()
                .filter(g -> g.getNome().equalsIgnoreCase(risposta.trim()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean isAmmessoComeCartaIniziale() {
        return false;
    }
}
