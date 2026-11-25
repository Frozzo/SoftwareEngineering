package unolegends.model;

import java.util.Objects;

/**
 * Fatto avvenuto durante la partita, registrato da {@link Partita} e letto dalla UI
 * tramite il controller (registro eventi "pull").
 *
 * <p>Alternativa più semplice all'Observer: la CLI è sincrona (dà un comando e
 * aspetta), quindi le basta leggere, dopo ogni comando, cosa è successo. Non
 * servono interfacce listener né callback, e non c'è il rischio che un callback
 * rientri in {@code Partita} mentre sta ancora eseguendo una giocata.</p>
 *
 * <p>Il model non produce testo: l'evento contiene solo dati (Pure Fabrication),
 * la frase la scrive il controller. I campi {@code origine} e {@code colore}
 * valgono solo per alcuni tipi e altrimenti sono {@code null}: per questo gli
 * eventi si creano con le factory statiche, che rendono esplicito quali campi
 * usa ciascun tipo. Se un giorno i tipi diventassero molti e con dati molto
 * diversi, si potrà passare a un record per evento (interfaccia sealed).</p>
 *
 * @param tipo          cosa è successo
 * @param nomeGiocatore il giocatore coinvolto ({@code null} per {@link TipoEvento#SENSO_INVERTITO})
 * @param quantita      numero di carte, per le pesche; 0 altrimenti
 * @param origine       causa della pesca forzata, solo per {@link TipoEvento#PESCA_FORZATA}
 * @param colore        colore scelto, solo per {@link TipoEvento#COLORE_SCELTO}
 */
public record EventoPartita(TipoEvento tipo, String nomeGiocatore, int quantita,
                            OriginePesca origine, Colore colore) {

    public EventoPartita {
        Objects.requireNonNull(tipo, "tipo non puo essere null");
    }

    static EventoPartita pescaVolontaria(Giocatore giocatore, int quantita) {
        return new EventoPartita(TipoEvento.PESCA_VOLONTARIA, giocatore.getNome(), quantita, null, null);
    }

    static EventoPartita pescaForzata(Giocatore giocatore, int quantita, OriginePesca origine) {
        return new EventoPartita(TipoEvento.PESCA_FORZATA, giocatore.getNome(), quantita,
                Objects.requireNonNull(origine, "origine non puo essere null"), null);
    }

    static EventoPartita turnoSaltato(Giocatore giocatore) {
        return new EventoPartita(TipoEvento.TURNO_SALTATO, giocatore.getNome(), 0, null, null);
    }

    static EventoPartita sensoInvertito() {
        return new EventoPartita(TipoEvento.SENSO_INVERTITO, null, 0, null, null);
    }

    static EventoPartita coloreScelto(Giocatore giocatore, Colore colore) {
        return new EventoPartita(TipoEvento.COLORE_SCELTO, giocatore.getNome(), 0, null,
                Objects.requireNonNull(colore, "colore non puo essere null"));
    }

    static EventoPartita unoDichiarato(Giocatore giocatore) {
        return new EventoPartita(TipoEvento.UNO_DICHIARATO, giocatore.getNome(), 0, null, null);
    }
}
