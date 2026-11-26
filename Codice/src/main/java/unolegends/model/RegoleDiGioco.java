package unolegends.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import unolegends.domain.rules.IStrategiaCondizioneSconfitta;
import unolegends.domain.rules.IStrategiaCondizioneVittoria;
import unolegends.domain.rules.IStrategiaCompatibilitaCarte;
import unolegends.domain.rules.IStrategiaFinePartita;
import unolegends.domain.rules.IStrategiaPenalitaUno;
import unolegends.domain.rules.IStrategiaPesca;
import unolegends.domain.rules.impl.StrategiaCompatibilitaCarte;
import unolegends.domain.rules.impl.StrategiaFinePartitaEndless;
import unolegends.domain.rules.impl.StrategiaFinePartitaStandard;
import unolegends.domain.rules.impl.StrategiaPenalitaMancatoUno;
import unolegends.domain.rules.impl.StrategiaPescaFinoAGiocabile;
import unolegends.domain.rules.impl.StrategiaPescaSingola;
import unolegends.domain.rules.impl.StrategiaSconfittaNoMercy;
import unolegends.domain.rules.impl.StrategiaSconfittaStandard;
import unolegends.domain.rules.impl.StrategiaVittoriaStandard;

/**
 * Contenitore unico delle regole di gioco e delle relative strategie.
 *
 * <p>È il punto di variazione protetto: {@link Partita} conosce soltanto
 * questa astrazione concreta, mentre le singole regole restano sostituibili.
 * È anche il punto in cui verranno applicate le abilità passive dei Legend,
 * perché tutte le domande "si può fare?" e "quante carte?" passano da qui.</p>
 *
 * <p>Si crea con un metodo statico per modalità ({@link #standard(Set)} e
 * {@link #noMercy(Set)}) che riceve le {@link OpzionePartita} attive e
 * sceglie le strategie concrete. Si è scelto di non introdurre un Builder: con
 * poche strategie {@code RegoleDiGioco.standard(opzioni)} è più leggibile di
 * {@code builder()...build()}, e il costruttore lungo lo chiamano solo questi
 * metodi. Il Builder va rivalutato quando le strategie aumenteranno (stacking,
 * passive dei Legend).</p>
 */
public class RegoleDiGioco {
    private final IStrategiaCondizioneVittoria strategiaVittoria;
    private final IStrategiaCondizioneSconfitta strategiaSconfitta;
    private final IStrategiaCompatibilitaCarte strategiaCompatibilitaCarte;
    private final IStrategiaPesca strategiaPescaVolontaria;
    private final IStrategiaPenalitaUno strategiaPenalitaUno;
    private final IStrategiaFinePartita strategiaFinePartita;

    public RegoleDiGioco(IStrategiaCondizioneVittoria strategiaVittoria,
                         IStrategiaCondizioneSconfitta strategiaSconfitta,
                         IStrategiaCompatibilitaCarte strategiaCompatibilitaCarte,
                         IStrategiaPesca strategiaPescaVolontaria,
                         IStrategiaPenalitaUno strategiaPenalitaUno,
                         IStrategiaFinePartita strategiaFinePartita) {
        this.strategiaVittoria = Objects.requireNonNull(strategiaVittoria);
        this.strategiaSconfitta = Objects.requireNonNull(strategiaSconfitta);
        this.strategiaCompatibilitaCarte = Objects.requireNonNull(strategiaCompatibilitaCarte);
        this.strategiaPescaVolontaria = Objects.requireNonNull(strategiaPescaVolontaria);
        this.strategiaPenalitaUno = Objects.requireNonNull(strategiaPenalitaUno);
        this.strategiaFinePartita = Objects.requireNonNull(strategiaFinePartita);
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

    /**
     * true se il giocatore ha completato la partita (nelle regole attuali: ha
     * finito le carte). In partita normale è il vincitore; in Endless prende il
     * prossimo posto in classifica e la partita continua.
     */
    public boolean isVincitore(Giocatore giocatore, Partita partita) {
        return strategiaVittoria.isVincitore(giocatore, partita);
    }

    /**
     * true se la partita deve finire. {@link Partita} lo chiede ogni volta che un
     * giocatore esce di scena (terminato o eliminato).
     */
    public boolean isPartitaTerminata(Partita partita) {
        return strategiaFinePartita.isPartitaTerminata(partita);
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

    /** Regole della partita normale senza opzioni. */
    public static RegoleDiGioco standard() {
        return standard(Set.of());
    }

    /**
     * Regole della partita normale, con le opzioni scelte nel menu.
     * Ogni opzione sostituisce una sola strategia; le altre restano standard.
     * Un'opzione nuova si aggiunge con una riga qui.
     */
    public static RegoleDiGioco standard(Set<OpzionePartita> opzioni) {
        return new RegoleDiGioco(
                new StrategiaVittoriaStandard(),
                new StrategiaSconfittaStandard(),
                new StrategiaCompatibilitaCarte(),
                new StrategiaPescaSingola(),
                new StrategiaPenalitaMancatoUno(),
                finePartita(opzioni));
    }

    /** Numero di carte che fa scattare l'eliminazione in No Mercy (Mercy rule). */
    public static final int SOGLIA_ELIMINAZIONE_NO_MERCY = 25;

    /**
     * Regole della No Mercy, con le opzioni scelte nel menu. Rispetto alla partita
     * normale cambiano la pesca volontaria (finché trovi una carta giocabile) e la
     * sconfitta (eliminazione a 25 carte). Lo stacking, obbligatorio in No Mercy,
     * arriverà con lo Step 5.
     */
    public static RegoleDiGioco noMercy(Set<OpzionePartita> opzioni) {
        return new RegoleDiGioco(
                new StrategiaVittoriaStandard(),
                new StrategiaSconfittaNoMercy(SOGLIA_ELIMINAZIONE_NO_MERCY),
                new StrategiaCompatibilitaCarte(),
                new StrategiaPescaFinoAGiocabile(),
                new StrategiaPenalitaMancatoUno(),
                finePartita(opzioni));
    }

    /** Opzione Endless: comune alle due modalità. */
    private static IStrategiaFinePartita finePartita(Set<OpzionePartita> opzioni) {
        Objects.requireNonNull(opzioni, "opzioni non puo essere null");
        return opzioni.contains(OpzionePartita.ENDLESS)
                ? new StrategiaFinePartitaEndless()
                : new StrategiaFinePartitaStandard();
    }
}
