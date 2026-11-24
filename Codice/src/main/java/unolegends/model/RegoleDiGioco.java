package unolegends.model;

import java.util.List;
import java.util.Objects;

import unolegends.domain.rules.IStrategiaCondizioneSconfitta;
import unolegends.domain.rules.IStrategiaCondizioneVittoria;
import unolegends.domain.rules.IStrategiaCompatibilitaCarte;
import unolegends.domain.rules.IStrategiaPenalitaUno;
import unolegends.domain.rules.IStrategiaPesca;
import unolegends.domain.rules.impl.StrategiaCompatibilitaCarte;
import unolegends.domain.rules.impl.StrategiaPenalitaMancatoUno;
import unolegends.domain.rules.impl.StrategiaPescaSingola;
import unolegends.domain.rules.impl.StrategiaSconfittaStandard;
import unolegends.domain.rules.impl.StrategiaVittoriaStandard;

/**
 * Contenitore unico delle regole di gioco e delle relative strategie.
 *
 * <p>È il punto di variazione protetto: {@link Partita} conosce soltanto
 * questa astrazione concreta, mentre le singole regole restano sostituibili.
 * È anche il punto in cui verranno applicate le abilità passive dei Legend,
 * perché tutte le domande "si può fare?" e "quante carte?" passano da qui.</p>
 */
public class RegoleDiGioco {
    private final IStrategiaCondizioneVittoria strategiaVittoria;
    private final IStrategiaCondizioneSconfitta strategiaSconfitta;
    private final IStrategiaCompatibilitaCarte strategiaCompatibilitaCarte;
    private final IStrategiaPesca strategiaPescaVolontaria;
    private final IStrategiaPenalitaUno strategiaPenalitaUno;

    public RegoleDiGioco(IStrategiaCondizioneVittoria strategiaVittoria,
                         IStrategiaCondizioneSconfitta strategiaSconfitta,
                         IStrategiaCompatibilitaCarte strategiaCompatibilitaCarte,
                         IStrategiaPesca strategiaPescaVolontaria,
                         IStrategiaPenalitaUno strategiaPenalitaUno) {
        this.strategiaVittoria = Objects.requireNonNull(strategiaVittoria);
        this.strategiaSconfitta = Objects.requireNonNull(strategiaSconfitta);
        this.strategiaCompatibilitaCarte = Objects.requireNonNull(strategiaCompatibilitaCarte);
        this.strategiaPescaVolontaria = Objects.requireNonNull(strategiaPescaVolontaria);
        this.strategiaPenalitaUno = Objects.requireNonNull(strategiaPenalitaUno);
    }

    /**
     * Stabilisce se il giocatore può giocare la carta candidata. Il giocatore è
     * passato anche se la regola standard non lo usa: servirà alle abilità
     * passive che modificano la compatibilità (es. Daltonico).
     */
    public boolean isGiocabile(Giocatore giocatore, Carta candidata, Carta inCima, Partita partita) {
        return strategiaCompatibilitaCarte.puoGiocare(candidata, inCima,
                partita == null ? null : partita.getColoreAttuale());
    }

    public boolean isVincitore(Giocatore giocatore, Partita partita) {
        return strategiaVittoria.isVincitore(giocatore, partita);
    }

    public boolean isEliminato(Giocatore giocatore, Partita partita) {
        return strategiaSconfitta.isEliminato(giocatore, partita);
    }

    /**
     * Esegue la pesca volontaria del giocatore secondo la strategia configurata
     * (una carta nelle regole standard, fino a una carta giocabile in No Mercy).
     */
    public List<Carta> pescaVolontaria(Giocatore giocatore, Partita partita) {
        return strategiaPescaVolontaria.pesca(giocatore, partita);
    }

    /**
     * Calcola quante carte deve pescare davvero il bersaglio di una pesca forzata.
     * Oggi restituisce il numero richiesto; è il punto in cui le abilità passive
     * (es. immunità al +2, una carta in meno) modificheranno il risultato.
     */
    public int calcolaCartePescaForzata(ContestoPesca contesto) {
        return contesto.numeroCarte();
    }

    /**
     * Applica la penalità se il giocatore è rimasto con una carta senza dichiarare UNO.
     *
     * @return true se la penalità è stata applicata
     */
    public boolean mancatoUno(Giocatore giocatore, boolean unoDichiarato, Partita partita) {
        if (giocatore.getMano().size() == 1 && !unoDichiarato) {
            strategiaPenalitaUno.applica(giocatore, partita);
            return true;
        }
        return false;
    }

    public static RegoleDiGioco standard() {
        return new RegoleDiGioco(
                new StrategiaVittoriaStandard(),
                new StrategiaSconfittaStandard(),
                new StrategiaCompatibilitaCarte(),
                new StrategiaPescaSingola(),
                new StrategiaPenalitaMancatoUno());
    }
}
