package unolegends.model;

import java.util.List;
import java.util.Objects;

/**
 * Informazione richiesta da un effetto prima di poter essere applicato.
 *
 * <p>Contiene il {@link TipoRichiesta} e le opzioni ammesse, ma nessun testo per
 * l'utente: la traduzione in una frase è compito del controller.</p>
 */
public final class RichiestaInput {
    private final TipoRichiesta tipo;
    private final List<String> opzioni;

    public RichiestaInput(TipoRichiesta tipo, List<String> opzioni) {
        this.tipo = Objects.requireNonNull(tipo, "tipo non puo essere null");
        this.opzioni = List.copyOf(Objects.requireNonNull(opzioni, "opzioni non puo essere null"));
    }

    public TipoRichiesta getTipo() {
        return tipo;
    }

    public List<String> getOpzioni() {
        return opzioni;
    }
}
