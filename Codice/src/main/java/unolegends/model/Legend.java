package unolegends.model;

import java.util.Objects;

/**
 * Catalogo dei Legend (Step 6): ogni valore è una <b>coppia fissa</b> passiva +
 * attiva, con il cooldown dell'attiva.
 *
 * <p>È un enum di soli dati, come {@link ModalitaDiGioco}: una "tabella" con una riga
 * per Legend. Il comportamento sta nelle classi che la tabella collega
 * ({@link IPassiva} e {@link Ieffetto}); nomi e descrizioni per l'utente li scrive il
 * controller. I Legend sono un elenco deciso a tavolino (6, quanti i giocatori
 * massimi), quindi un enum basta e il menu li elenca da solo; una factory o un
 * registro di classi darebbe più struttura per un elenco piccolo e chiuso.</p>
 *
 * <p>Passive ed effetti non hanno stato, quindi ogni valore ne tiene un'istanza
 * condivisa. Lo stato di un giocatore (la ricarica dell'abilità) sta in
 * {@link AbilitaAttiva}, creata per ogni giocatore da {@link #creaAbilitaAttiva()}.</p>
 *
 * <p>I cooldown sono un primo valore, da bilanciare giocando (Step 7): le abilità
 * più forti (Scambia mazzo, Congela) hanno il cooldown più lungo.</p>
 */
public enum Legend {
    /** Passiva: immune al +2 singolo. Attiva: Il Castigatore (un giocatore a scelta pesca 4). */
    ROCCIA(new PassivaRoccia(), new EffettoPescaBersaglio(4, OriginePesca.CASTIGATORE), 4),
    /** Passiva: Salesman (una carta in meno). Attiva: Gifted card (regala 2 carte a caso). */
    MERCANTE(new PassivaSalesman(), new EffettoRegalaCarte(2), 3),
    /** Passiva: Ghost (non bersagliabile). Attiva: Scambia mazzo (bersaglio con più di una carta). */
    FANTASMA(new PassivaGhost(), EffettoScambiaMano.abilitaScambiaMazzo(), 5),
    /** Passiva: rosso sul verde e viceversa. Attiva: Arcobaleno (qualsiasi carta per un turno). */
    DALTONICO(new PassivaDaltonico(), new EffettoArcobaleno(), 4),
    /** Passiva: Pelle di ghiaccio (immune al Blocca). Attiva: Congela (il bersaglio salta 1 turno). */
    FREEZER(new PassivaPelleDiGhiaccio(), new EffettoCongela(1), 5),
    /** Passiva: Doppia pesca (solo partita normale). Attiva: Double card (un turno in più). */
    ACROBATA(new PassivaDoppiaPesca(), new EffettoTurnoExtra(), 4);

    private final IPassiva passiva;
    private final Ieffetto effettoAttivo;
    private final int cooldown;

    Legend(IPassiva passiva, Ieffetto effettoAttivo, int cooldown) {
        this.passiva = Objects.requireNonNull(passiva);
        this.effettoAttivo = Objects.requireNonNull(effettoAttivo);
        this.cooldown = cooldown;
    }

    IPassiva getPassiva() {
        return passiva;
    }

    /** Turni del possessore che servono per riusare l'abilità attiva. */
    public int getCooldown() {
        return cooldown;
    }

    /** Nuova abilità attiva, pronta, per un giocatore che ha scelto questo Legend. */
    AbilitaAttiva creaAbilitaAttiva() {
        return new AbilitaAttiva(effettoAttivo, cooldown);
    }
}
