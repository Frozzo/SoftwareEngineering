package unolegends.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Tutte le scelte fatte nel menu di setup: modalità, nomi dei giocatori (quindi
 * anche il loro numero) e opzioni attive. {@link PartitaFactory} la traduce in
 * mazzo, regole e giocatori.
 *
 * <p>È un {@code record} immutabile e non un Builder: i campi sono pochi e non
 * cresceranno, perché le opzioni nuove finiscono nel {@code Set} e non in campi
 * nuovi.</p>
 *
 * <p><b>Validazione senza eccezioni dedicate.</b> Chi raccoglie le scelte (il
 * controller) chiama prima {@link #verifica}, che restituisce il motivo
 * dell'errore come {@link ErroreConfigurazione}, oppure {@code null} se va tutto
 * bene. È lo stesso stile dei comandi di {@link Partita}, che restituiscono
 * {@code boolean} invece di lanciare eccezioni. Il costruttore ripete il
 * controllo come rete di sicurezza e, se qualcuno lo usa con dati non validi,
 * lancia una normale {@link IllegalArgumentException}: è un errore di
 * programmazione, non un caso da mostrare all'utente.</p>
 *
 * <p>Il costruttore aggiunge da solo le opzioni obbligatorie della modalità: chi
 * legge la configurazione non deve gestire casi speciali come "No Mercy ha
 * sempre lo stacking".</p>
 *
 * <p><b>Legend (Step 6).</b> Con l'opzione {@link OpzionePartita#LEGEND} ogni giocatore
 * ha un Legend, nello stesso ordine dei nomi, e due giocatori non possono avere lo
 * stesso Legend (decisione presa). Senza l'opzione la lista è vuota. È l'unico campo
 * aggiunto dallo Step 3: i Legend sono un dato <i>per giocatore</i>, quindi non possono
 * stare nel {@code Set} delle opzioni.</p>
 *
 * @param modalita      modalità di gioco
 * @param nomiGiocatori nomi in ordine di turno (gli spazi esterni vengono tolti)
 * @param opzioni       opzioni attive, comprese quelle obbligatorie della modalità
 * @param legend        Legend di ogni giocatore, nello stesso ordine dei nomi; vuota senza l'opzione Legend
 */
public record ConfigurazionePartita(ModalitaDiGioco modalita, List<String> nomiGiocatori,
                                    Set<OpzionePartita> opzioni, List<Legend> legend) {

    public static final int MIN_GIOCATORI = 2;
    public static final int MAX_GIOCATORI = 6;
    public static final int MIN_GIOCATORI_ENDLESS = 3;

    public ConfigurazionePartita {
        Objects.requireNonNull(modalita, "modalita non puo essere null");
        nomiGiocatori = normalizzaNomi(nomiGiocatori);
        opzioni = conOpzioniObbligatorie(modalita, opzioni);
        legend = List.copyOf(Objects.requireNonNull(legend, "legend non puo essere null"));

        ErroreConfigurazione errore = verifica(modalita, nomiGiocatori, opzioni, legend);
        if (errore != null) {
            throw new IllegalArgumentException("Configurazione non valida: " + errore);
        }
    }

    /** Configurazione senza Legend. */
    public ConfigurazionePartita(ModalitaDiGioco modalita, List<String> nomiGiocatori, Set<OpzionePartita> opzioni) {
        this(modalita, nomiGiocatori, opzioni, List.of());
    }

    /** Come {@link #verifica(ModalitaDiGioco, List, Set, List)}, senza Legend. */
    public static ErroreConfigurazione verifica(ModalitaDiGioco modalita, List<String> nomiGiocatori,
                                                Set<OpzionePartita> opzioni) {
        return verifica(modalita, nomiGiocatori, opzioni, List.of());
    }

    /**
     * Controlla le scelte del menu prima di creare la configurazione.
     *
     * @return il motivo per cui le scelte non sono valide, oppure {@code null} se sono valide
     */
    public static ErroreConfigurazione verifica(ModalitaDiGioco modalita, List<String> nomiGiocatori,
                                                Set<OpzionePartita> opzioni, List<Legend> legend) {
        Objects.requireNonNull(modalita, "modalita non puo essere null");
        Objects.requireNonNull(legend, "legend non puo essere null");
        List<String> nomi = normalizzaNomi(nomiGiocatori);
        Set<OpzionePartita> opzioniAttive = conOpzioniObbligatorie(modalita, opzioni);

        if (nomi.size() < MIN_GIOCATORI || nomi.size() > MAX_GIOCATORI) {
            return ErroreConfigurazione.NUMERO_GIOCATORI_NON_VALIDO;
        }
        Set<String> nomiVisti = new HashSet<>();
        for (String nome : nomi) {
            if (nome.isEmpty()) {
                return ErroreConfigurazione.NOME_VUOTO;
            }
            if (!nomiVisti.add(nome.toLowerCase())) {
                return ErroreConfigurazione.NOMI_DUPLICATI;
            }
        }
        if (opzioniAttive.contains(OpzionePartita.ENDLESS) && nomi.size() < MIN_GIOCATORI_ENDLESS) {
            return ErroreConfigurazione.GIOCATORI_INSUFFICIENTI_PER_ENDLESS;
        }
        return verificaLegend(opzioniAttive.contains(OpzionePartita.LEGEND), nomi.size(), legend);
    }

    /** Un Legend a testa con l'opzione Legend, nessuno senza; mai lo stesso Legend due volte. */
    private static ErroreConfigurazione verificaLegend(boolean conLegend, int numeroGiocatori, List<Legend> legend) {
        if (!conLegend) {
            return legend.isEmpty() ? null : ErroreConfigurazione.LEGEND_SENZA_OPZIONE;
        }
        if (legend.size() != numeroGiocatori || legend.stream().anyMatch(Objects::isNull)) {
            return ErroreConfigurazione.LEGEND_MANCANTI;
        }
        if (Set.copyOf(legend).size() != legend.size()) {
            return ErroreConfigurazione.LEGEND_DUPLICATI;
        }
        return null;
    }

    public boolean haOpzione(OpzionePartita opzione) {
        return opzioni.contains(opzione);
    }

    public int numeroGiocatori() {
        return nomiGiocatori.size();
    }

    private static List<String> normalizzaNomi(List<String> nomi) {
        Objects.requireNonNull(nomi, "nomiGiocatori non puo essere null");
        return nomi.stream().map(String::strip).toList();
    }

    private static Set<OpzionePartita> conOpzioniObbligatorie(ModalitaDiGioco modalita, Set<OpzionePartita> opzioni) {
        Objects.requireNonNull(opzioni, "opzioni non puo essere null");
        EnumSet<OpzionePartita> tutte = EnumSet.noneOf(OpzionePartita.class);
        tutte.addAll(opzioni);
        tutte.addAll(modalita.getOpzioniObbligatorie());
        return Collections.unmodifiableSet(tutte);
    }
}
