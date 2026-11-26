package unolegends.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import unolegends.domain.rules.IStrategiaAccumuloPenalita;
import unolegends.domain.rules.IStrategiaCondizioneSconfitta;
import unolegends.domain.rules.IStrategiaCondizioneVittoria;
import unolegends.domain.rules.IStrategiaCompatibilitaCarte;
import unolegends.domain.rules.IStrategiaFinePartita;
import unolegends.domain.rules.IStrategiaPenalitaUno;
import unolegends.domain.rules.IStrategiaPesca;
import unolegends.domain.rules.impl.StrategiaAccumuloValoreCrescente;
import unolegends.domain.rules.impl.StrategiaCompatibilitaCarte;
import unolegends.domain.rules.impl.StrategiaFinePartitaEndless;
import unolegends.domain.rules.impl.StrategiaFinePartitaStandard;
import unolegends.domain.rules.impl.StrategiaNessunAccumulo;
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
 * metodi. Con lo stacking (Step 5) le strategie sono diventate 7 e il team ha
 * confermato di restare senza Builder. Allo Step 6 le passive dei Legend <b>non</b>
 * hanno aggiunto strategie: sono del singolo giocatore, non della partita, quindi le
 * strategie restano 7 e il Builder continua a non servire.</p>
 *
 * <p><b>Passive dei Legend (Step 6).</b> Qui si combinano la regola della partita e
 * le passive del giocatore: prima si calcola il risultato "base" con la strategia,
 * poi lo si fa passare dagli hook di {@link IPassiva} del giocatore coinvolto. Le
 * regole non sanno quali passive esistono.</p>
 */
public class RegoleDiGioco {
    private final IStrategiaCondizioneVittoria strategiaVittoria;
    private final IStrategiaCondizioneSconfitta strategiaSconfitta;
    private final IStrategiaCompatibilitaCarte strategiaCompatibilitaCarte;
    private final IStrategiaPesca strategiaPescaVolontaria;
    private final IStrategiaPenalitaUno strategiaPenalitaUno;
    private final IStrategiaFinePartita strategiaFinePartita;
    private final IStrategiaAccumuloPenalita strategiaAccumulo;

    public RegoleDiGioco(IStrategiaCondizioneVittoria strategiaVittoria,
                         IStrategiaCondizioneSconfitta strategiaSconfitta,
                         IStrategiaCompatibilitaCarte strategiaCompatibilitaCarte,
                         IStrategiaPesca strategiaPescaVolontaria,
                         IStrategiaPenalitaUno strategiaPenalitaUno,
                         IStrategiaFinePartita strategiaFinePartita,
                         IStrategiaAccumuloPenalita strategiaAccumulo) {
        this.strategiaVittoria = Objects.requireNonNull(strategiaVittoria);
        this.strategiaSconfitta = Objects.requireNonNull(strategiaSconfitta);
        this.strategiaCompatibilitaCarte = Objects.requireNonNull(strategiaCompatibilitaCarte);
        this.strategiaPescaVolontaria = Objects.requireNonNull(strategiaPescaVolontaria);
        this.strategiaPenalitaUno = Objects.requireNonNull(strategiaPenalitaUno);
        this.strategiaFinePartita = Objects.requireNonNull(strategiaFinePartita);
        this.strategiaAccumulo = Objects.requireNonNull(strategiaAccumulo);
    }

    /**
     * Stabilisce se il giocatore può giocare la carta candidata. Il giocatore è
     * passato anche se la regola standard non lo usa: servirà alle abilità
     * passive che modificano la compatibilità (es. Daltonico).
     *
     * <p>Se c'è un accumulo dello stacking in corso (Step 5) la domanda cambia: non
     * conta più la compatibilità con la carta in cima, ma solo se la carta può
     * rispondere all'accumulo ({@link IStrategiaAccumuloPenalita}). Resta un unico
     * metodo "si può giocare?", così {@link Partita} non deve sapere quale delle due
     * regole vale in quel momento.</p>
     */
    public boolean isGiocabile(Giocatore giocatore, Carta candidata, Carta inCima, Partita partita) {
        Accumulo accumulo = partita == null ? null : partita.getAccumulo();
        if (accumulo != null) {
            // Con lo stacking contano solo i valori di pesca: le passive sulla
            // compatibilità (Daltonico, Arcobaleno) non valgono (decisione presa).
            return strategiaAccumulo.puoRispondere(candidata, accumulo);
        }
        Colore coloreAttivo = partita == null ? null : partita.getColoreAttuale();
        if (strategiaCompatibilitaCarte.puoGiocare(candidata, inCima, coloreAttivo)) {
            return true;
        }
        // Le passive possono solo aggiungere giocate: "base OR passive".
        return candidata != null && giocatore != null
                && giocatore.getPassive().stream().anyMatch(p -> p.consenteGiocata(candidata, inCima, coloreAttivo));
    }

    /**
     * true se le carte di pesca si accumulano (stacking) invece di far pescare
     * subito il giocatore successivo. Lo chiede {@link Partita} quando un effetto di
     * pesca applica la sua penalità.
     */
    public boolean isAccumuloAttivo() {
        return strategiaAccumulo.isAccumuloAttivo();
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
     * Calcola quante carte deve pescare davvero il bersaglio di una pesca forzata:
     * il numero richiesto, modificato una passiva dopo l'altra dalle passive del
     * bersaglio (La Roccia: 0 su un +2 singolo; Salesman: una in meno). Il risultato
     * non scende mai sotto 0.
     */
    public int calcolaCartePescaForzata(ContestoPesca contesto) {
        int carte = contesto.numeroCarte();
        for (IPassiva passiva : contesto.bersaglio().getPassive()) {
            carte = Math.max(0, passiva.modificaPescaForzata(contesto, carte));
        }
        return carte;
    }

    /**
     * Fra quante carte sceglie il giocatore quando pesca volontariamente (1 = pesca
     * normale, senza scelta). Più di 1 solo se la strategia di pesca lo ammette
     * (partita normale) e una passiva lo chiede (Doppia pesca dell'Acrobata).
     */
    public int getCarteTraCuiScegliereInPesca(Giocatore giocatore) {
        if (!strategiaPescaVolontaria.ammettePescaConScelta()) {
            return 1;
        }
        return giocatore.getPassive().stream()
                .mapToInt(IPassiva::getCarteTraCuiScegliereInPesca)
                .max()
                .orElse(1);
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
                finePartita(opzioni),
                accumulo(opzioni));
    }

    /** Numero di carte che fa scattare l'eliminazione in No Mercy (Mercy rule). */
    public static final int SOGLIA_ELIMINAZIONE_NO_MERCY = 25;

    /**
     * Regole della No Mercy, con le opzioni scelte nel menu. Rispetto alla partita
     * normale cambiano la pesca volontaria (finché trovi una carta giocabile) e la
     * sconfitta (eliminazione a 25 carte). Lo stacking è un'opzione obbligatoria
     * della modalità ({@link ModalitaDiGioco#NO_MERCY}): {@link ConfigurazionePartita}
     * la aggiunge da sola alle opzioni, quindi arriva qui come le altre.
     */
    public static RegoleDiGioco noMercy(Set<OpzionePartita> opzioni) {
        return new RegoleDiGioco(
                new StrategiaVittoriaStandard(),
                new StrategiaSconfittaNoMercy(SOGLIA_ELIMINAZIONE_NO_MERCY),
                new StrategiaCompatibilitaCarte(),
                new StrategiaPescaFinoAGiocabile(),
                new StrategiaPenalitaMancatoUno(),
                finePartita(opzioni),
                accumulo(opzioni));
    }

    /** Opzione Endless: comune alle due modalità. */
    private static IStrategiaFinePartita finePartita(Set<OpzionePartita> opzioni) {
        Objects.requireNonNull(opzioni, "opzioni non puo essere null");
        return opzioni.contains(OpzionePartita.ENDLESS)
                ? new StrategiaFinePartitaEndless()
                : new StrategiaFinePartitaStandard();
    }

    /**
     * Opzione Stacking: comune alle due modalità, come Endless. Con il mazzo
     * normale la stessa strategia dà "+2 ← +2/+4, +4 ← +4", con quello No Mercy la
     * tabella ufficiale.
     */
    private static IStrategiaAccumuloPenalita accumulo(Set<OpzionePartita> opzioni) {
        Objects.requireNonNull(opzioni, "opzioni non puo essere null");
        return opzioni.contains(OpzionePartita.STACKING)
                ? new StrategiaAccumuloValoreCrescente()
                : new StrategiaNessunAccumulo();
    }
}
