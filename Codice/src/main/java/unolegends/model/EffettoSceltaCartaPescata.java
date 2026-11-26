package unolegends.model;

import java.util.List;
import java.util.Objects;

/**
 * Richiesta della passiva Doppia pesca (Acrobata): dopo aver pescato 2 carte il
 * giocatore sceglie quale tenere; l'altra torna <b>in fondo al mazzo</b> (decisione
 * presa: così non si ripesca subito).
 *
 * <p>Non è l'effetto di una carta: è un {@link Ieffetto} usato per passare dal
 * meccanismo unificato delle richieste ({@code Partita.mettiInAttesa} +
 * {@code rispondiInput}), come vuole la regola del progetto "niente metodi
 * rispondiXxx dedicati". Le opzioni sono le descrizioni delle carte pescate; due
 * carte con la stessa descrizione sono equivalenti, quindi basta la prima. Il
 * confronto usa {@code strip()} e non {@code trim()}: la descrizione inizia con un
 * codice colore ANSI (carattere ESC), che {@code trim()} toglierebbe.</p>
 */
final class EffettoSceltaCartaPescata implements Ieffetto {
    private final List<Carta> cartePescate;
    private Carta cartaTenuta;

    EffettoSceltaCartaPescata(List<Carta> cartePescate) {
        this.cartePescate = List.copyOf(Objects.requireNonNull(cartePescate, "cartePescate non puo essere null"));
    }

    @Override
    public void attivaEffetto(Partita partita) {
        throw new IllegalStateException("La doppia pesca richiede la scelta della carta da tenere");
    }

    @Override
    public RichiestaInput getRichiestaInput(Partita partita) {
        return new RichiestaInput(TipoRichiesta.SCELTA_CARTA,
                cartePescate.stream().map(Carta::getDescrizioneBreve).toList());
    }

    @Override
    public boolean validaRispostaInput(String risposta, Partita partita) {
        return trovaCarta(risposta) != null;
    }

    /** Tiene la carta scelta e rimette le altre in fondo al mazzo. */
    @Override
    public void attivaEffetto(Partita partita, String risposta) {
        Carta scelta = trovaCarta(risposta);
        if (scelta == null) {
            throw new IllegalArgumentException("Carta non valida: " + risposta);
        }
        Giocatore giocatore = partita.getGiocatoreAttivo();
        for (Carta carta : cartePescate) {
            if (carta != scelta) {
                giocatore.rimuoviCarta(carta);
                partita.rimettiInFondoAlMazzo(carta);
            }
        }
        cartaTenuta = scelta;
        partita.registraEvento(EventoPartita.cartaScelta(giocatore, cartePescate.size() - 1));
    }

    /** Carta tenuta, dopo la risposta. */
    Carta getCartaTenuta() {
        return cartaTenuta;
    }

    private Carta trovaCarta(String risposta) {
        if (risposta == null) {
            return null;
        }
        return cartePescate.stream()
                .filter(c -> c.getDescrizioneBreve().strip().equalsIgnoreCase(risposta.strip()))
                .findFirst()
                .orElse(null);
    }
}
