package unolegends.controller;

import java.util.List;
import java.util.Objects;

/**
 * Dati di una richiesta d'interazione esposti alla UI senza tipi del dominio.
 */
public final class RichiestaInputDto {
    private final String messaggio;
    private final List<String> opzioni;

    public RichiestaInputDto(String messaggio, List<String> opzioni) {
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
