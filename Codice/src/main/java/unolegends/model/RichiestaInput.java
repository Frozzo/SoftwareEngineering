package unolegends.model;

import java.util.List;
import java.util.Objects;

/**
 * Informazione richiesta da un effetto prima di poter essere applicato.
 */
public final class RichiestaInput {
    private final String messaggio;
    private final List<String> opzioni;

    public RichiestaInput(String messaggio, List<String> opzioni) {
        this.messaggio = Objects.requireNonNull(messaggio, "messaggio non puo essere null");
        this.opzioni = List.copyOf(Objects.requireNonNull(opzioni, "opzioni non puo essere null"));
    }

    public String getMessaggio() {
        return messaggio;
    }

    public List<String> getOpzioni() {
        return opzioni;
    }
}
