package unolegends.model;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Azione sospesa in attesa di una risposta del giocatore (pattern Command).
 *
 * <p>Unifica i casi che prima avevano un meccanismo ciascuno (colore della carta
 * iniziale, colore di un jolly giocato nel turno) e quelli futuri (bersaglio della
 * carta 7, abilità attive usate a metà turno):</p>
 * <ul>
 *   <li>{@code effetto} è l'Information Expert della richiesta: sa cosa chiedere
 *       ({@link Ieffetto#getRichiestaInput}) e se la risposta è valida
 *       ({@link Ieffetto#validaRispostaInput});</li>
 *   <li>{@code completamento} è ciò che {@link Partita} deve fare quando arriva una
 *       risposta valida (completare la giocata, avviare il turno dopo la carta
 *       iniziale...). Così {@code Partita.rispondiInput} non deve sapere che tipo di
 *       azione stava aspettando: niente {@code if} sul caso concreto.</li>
 * </ul>
 */
record AzioneInAttesa(Ieffetto effetto, Consumer<String> completamento) {
    AzioneInAttesa {
        Objects.requireNonNull(effetto, "effetto non puo essere null");
        Objects.requireNonNull(completamento, "completamento non puo essere null");
    }
}
