package unolegends.model;

/**
 * Abilità attiva "Arcobaleno" del Daltonico: per il resto del turno può giocare
 * qualsiasi carta, senza controllare la compatibilità.
 *
 * <p>Dà al giocatore una passiva temporanea ({@link PassivaColoreLibero}), che
 * {@code Partita.chiudiTurno()} toglie. Così si riusa il meccanismo delle passive e in
 * Partita non c'è un flag in più. Vale solo nella giocata normale: durante lo
 * stacking le abilità non si usano e la compatibilità non conta.</p>
 */
final class EffettoArcobaleno implements Ieffetto {
    @Override
    public void attivaEffetto(Partita partita) {
        partita.getGiocatoreAttivo().aggiungiPassivaTemporanea(new PassivaColoreLibero());
    }
}
