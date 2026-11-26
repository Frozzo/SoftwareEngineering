package unolegends.model;

import java.util.Objects;
import java.util.Set;

/**
 * Modalità di gioco scelta nel menu: partita normale o No Mercy.
 *
 * <p>È un enum di soli dati, una "tabella" con una riga per modalità: con quale
 * mazzo si gioca ({@link MazzoFactory}, l'Abstract Factory già esistente) e quali
 * {@link OpzionePartita} sono sempre attive e quindi non vanno chieste (per
 * esempio, in No Mercy lo stacking). Le modalità sono due e lo resteranno: per
 * questo non serve una gerarchia di classi, e la scelta delle regole per
 * modalità può stare in uno {@code switch} di {@link PartitaFactory}.</p>
 *
 * <p>Sostituisce l'enum {@code TipoMazzo}, che non era usato da nessuno.</p>
 */
public enum ModalitaDiGioco {
    NORMALE(StandardMazzoFactory.getInstance(), Set.of()),
    /**
     * No Mercy (Step 4): mazzo da 168 carte e regole proprie. Lo stacking sarà la
     * sua opzione obbligatoria dallo Step 5, quando {@code OpzionePartita.STACKING}
     * esisterà.
     */
    NO_MERCY(NoMercyMazzoFactory.getInstance(), Set.of());

    private final MazzoFactory mazzoFactory;
    private final Set<OpzionePartita> opzioniObbligatorie;

    ModalitaDiGioco(MazzoFactory mazzoFactory, Set<OpzionePartita> opzioniObbligatorie) {
        this.mazzoFactory = Objects.requireNonNull(mazzoFactory);
        this.opzioniObbligatorie = Set.copyOf(opzioniObbligatorie);
    }

    public MazzoFactory getMazzoFactory() {
        return mazzoFactory;
    }

    /** Opzioni sempre attive in questa modalità: il menu non le chiede. */
    public Set<OpzionePartita> getOpzioniObbligatorie() {
        return opzioniObbligatorie;
    }

    /** true se l'utente può scegliere se attivare l'opzione. */
    public boolean isConfigurabile(OpzionePartita opzione) {
        return !opzioniObbligatorie.contains(opzione);
    }
}
