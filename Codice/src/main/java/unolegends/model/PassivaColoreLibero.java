package unolegends.model;

/**
 * Passiva <b>temporanea</b> data dall'abilità attiva Arcobaleno del Daltonico: per il
 * resto del turno il giocatore può giocare qualsiasi carta, senza controllare la
 * compatibilità.
 *
 * <p>Non è un flag in {@link Partita}: riusa il meccanismo delle passive.
 * {@link EffettoArcobaleno} la aggiunge al giocatore e {@code Partita.chiudiTurno()} la
 * toglie ({@link Giocatore#terminaTurno()}). Non vale durante lo stacking, perché lì
 * {@link RegoleDiGioco#isGiocabile} non guarda la compatibilità ma solo il valore di
 * pesca (decisione presa: "solo in giocata normale").</p>
 */
final class PassivaColoreLibero implements IPassiva {
    @Override
    public boolean consenteGiocata(Carta candidata, Carta inCima, Colore coloreAttivo) {
        return true;
    }
}
