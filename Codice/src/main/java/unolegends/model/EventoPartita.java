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
 * @param quantita      numero di carte, per le pesche; posizione in classifica, per
 *                      {@link TipoEvento#GIOCATORE_TERMINATO}; 0 altrimenti
 * @param origine       causa della pesca forzata, solo per {@link TipoEvento#PESCA_FORZATA}
 * @param colore        colore scelto ({@link TipoEvento#COLORE_SCELTO}) o scartato
 *                      ({@link TipoEvento#CARTE_SCARTATE})
 * @param altroGiocatore secondo giocatore coinvolto, solo per {@link TipoEvento#MANI_SCAMBIATE}
 */
public record EventoPartita(TipoEvento tipo, String nomeGiocatore, int quantita,
                            OriginePesca origine, Colore colore, String altroGiocatore) {

    public EventoPartita {
        Objects.requireNonNull(tipo, "tipo non puo essere null");
    }

    static EventoPartita pescaVolontaria(Giocatore giocatore, int quantita) {
        return new EventoPartita(TipoEvento.PESCA_VOLONTARIA, giocatore.getNome(), quantita, null, null, null);
    }

    static EventoPartita pescaForzata(Giocatore giocatore, int quantita, OriginePesca origine) {
        return new EventoPartita(TipoEvento.PESCA_FORZATA, giocatore.getNome(), quantita,
                Objects.requireNonNull(origine, "origine non puo essere null"), null, null);
    }

    static EventoPartita turnoSaltato(Giocatore giocatore) {
        return new EventoPartita(TipoEvento.TURNO_SALTATO, giocatore.getNome(), 0, null, null, null);
    }

    static EventoPartita sensoInvertito() {
        return new EventoPartita(TipoEvento.SENSO_INVERTITO, null, 0, null, null, null);
    }

    static EventoPartita coloreScelto(Giocatore giocatore, Colore colore) {
        return new EventoPartita(TipoEvento.COLORE_SCELTO, giocatore.getNome(), 0, null,
                Objects.requireNonNull(colore, "colore non puo essere null"), null);
    }

    static EventoPartita unoDichiarato(Giocatore giocatore) {
        return new EventoPartita(TipoEvento.UNO_DICHIARATO, giocatore.getNome(), 0, null, null, null);
    }

    static EventoPartita giocatoreTerminato(Giocatore giocatore, int posizione) {
        return new EventoPartita(TipoEvento.GIOCATORE_TERMINATO, giocatore.getNome(), posizione, null, null, null);
    }

    static EventoPartita giocatoreEliminato(Giocatore giocatore) {
        return new EventoPartita(TipoEvento.GIOCATORE_ELIMINATO, giocatore.getNome(), 0, null, null, null);
    }

    static EventoPartita maniScambiate(Giocatore giocatore, Giocatore altro) {
        return new EventoPartita(TipoEvento.MANI_SCAMBIATE, giocatore.getNome(), 0, null, null, altro.getNome());
    }

    static EventoPartita maniPassate(Giocatore giocatore) {
        return new EventoPartita(TipoEvento.MANI_PASSATE, giocatore.getNome(), 0, null, null, null);
    }

    static EventoPartita carteScartate(Giocatore giocatore, int quantita, Colore colore) {
        return new EventoPartita(TipoEvento.CARTE_SCARTATE, giocatore.getNome(), quantita, null,
                Objects.requireNonNull(colore, "colore non puo essere null"), null);
    }

    static EventoPartita turnoExtra(Giocatore giocatore) {
        return new EventoPartita(TipoEvento.TURNO_EXTRA, giocatore.getNome(), 0, null, null, null);
    }
}
