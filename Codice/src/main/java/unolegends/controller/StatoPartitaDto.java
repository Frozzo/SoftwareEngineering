package unolegends.controller;

import java.util.List;
import java.util.Objects;

public record StatoPartitaDto(Fase fase, String nomeGiocatoreAttivo, List<String> mano,
                              String cartaInCima, String cartaAppenaPescata,
                              Richiesta richiestaInput, List<Giocatore> giocatori,
                              boolean sensoOrario, List<String> classifica) {

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
        TERMINATA
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

    public record Giocatore(String nome, int numeroCarte, String stato, boolean diTurno) {
        public Giocatore {
            Objects.requireNonNull(nome, "nome non puo essere null");
            Objects.requireNonNull(stato, "stato non puo essere null");
        }
    }
}
