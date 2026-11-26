package unolegends.model;

/**
 * Carta Blocca: il giocatore successivo salta il turno.
 *
 * <p>Dallo Step 6 chiama {@link Partita#bloccaProssimo()} e non più direttamente
 * {@code segnalaSaltoTurno()}: così Partita sa che il salto viene da un Blocca e può
 * chiedere alle passive del bersaglio se è immune (Pelle di ghiaccio del Freezer),
 * senza che gli altri salti ne siano toccati.</p>
 */
public class Effetto_Blocca_Turno implements Ieffetto {

    public void attivaEffetto(Partita partita) {
        partita.bloccaProssimo();
    }

}
