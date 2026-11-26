package unolegends.controller;

import java.util.List;
import java.util.Objects;

/**
 * Unico DTO che la UI riceve dal controller: uno snapshot della partita già
 * tradotto in dati di presentazione (stringhe, numeri, enum della UI).
 *
 * <p>Dallo Step 6 contiene anche i dati dei Legend: per ogni giocatore il nome del
 * Legend e la ricarica dell'abilità ({@link Giocatore}), e per il giocatore di turno
 * il nome dell'abilità che può usare adesso ({@code abilitaDisponibile}), così la CLI
 * mostra la voce di menu solo quando il comando sarebbe accettato.</p>
 *
 * @param abilitaDisponibile nome dell'abilità attiva che il giocatore di turno può usare ora,
 *                           oppure {@code null} se non può usarne nessuna
 */
public record StatoPartitaDto(Fase fase, String nomeGiocatoreAttivo, List<String> mano,
                              String cartaInCima, String cartaAppenaPescata,
                              Richiesta richiestaInput, List<Giocatore> giocatori,
                              boolean sensoOrario, List<String> classifica, Accumulo accumulo,
                              String abilitaDisponibile) {

    public StatoPartitaDto {
        Objects.requireNonNull(fase, "fase non puo essere null");
        Objects.requireNonNull(nomeGiocatoreAttivo, "nomeGiocatoreAttivo non puo essere null");
        mano = List.copyOf(Objects.requireNonNull(mano, "mano non puo essere null"));
        giocatori = List.copyOf(Objects.requireNonNull(giocatori, "giocatori non puo essere null"));
        classifica = List.copyOf(Objects.requireNonNull(classifica, "classifica non puo essere null"));
    }

    /** Primo della classifica, oppure null se la partita non è finita. */
    public String nomeVincitore() {
        return classifica.isEmpty() ? null : classifica.get(0);
    }

    public enum Fase {
        NORMALE,
        POST_PESCA,
        ATTESA_INPUT,
        ATTESA_UNO,
        /** C'è un accumulo di pesca: si risponde con una carta di pesca o si accetta la penalità. */
        RISPOSTA_STACKING,
        TERMINATA
    }

    /**
     * Accumulo dello stacking in corso, {@code null} se non ce n'è uno (Step 5).
     *
     * @param carteDaPescare        quante carte pesca chi accetta
     * @param valoreMinimoRisposta  valore di pesca minimo di una risposta (2 vuol dire "+2 o superiore")
     */
    public record Accumulo(int carteDaPescare, int valoreMinimoRisposta) {
    }

    /**
     * Richiesta di input da mostrare: la frase la scrive il controller, le opzioni
     * vengono dal model.
     */
    public record Richiesta(String messaggio, List<String> opzioni) {
        public Richiesta {
            Objects.requireNonNull(messaggio, "messaggio non puo essere null");
            opzioni = List.copyOf(Objects.requireNonNull(opzioni, "opzioni non puo essere null"));
        }
    }

    /**
     * Un giocatore come lo vede la UI.
     *
     * @param legend          nome del Legend, oppure {@code null} se la partita è senza Legend
     * @param ricaricaAbilita turni che mancano prima di poter riusare l'abilità (0 = pronta)
     */
    public record Giocatore(String nome, int numeroCarte, String stato, boolean diTurno,
                            String legend, int ricaricaAbilita) {
        public Giocatore {
            Objects.requireNonNull(nome, "nome non puo essere null");
            Objects.requireNonNull(stato, "stato non puo essere null");
        }
    }
}
