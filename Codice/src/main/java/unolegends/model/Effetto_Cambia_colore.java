package unolegends.model;

import java.util.List;

public class Effetto_Cambia_colore implements Ieffetto {

    @Override
    public void attivaeffetto(Partita partita) {
        throw new IllegalStateException("L'effetto richiede la scelta di un colore"); //controlla che l'effetto non venga attivato senza input
    }

    @Override
    public RichiestaInput getRichiestaInput() {
        return new RichiestaInput("Scegli il colore da assegnare alla carta:",
                List.of(Colore.ROSSO.getNome(), Colore.VERDE.getNome(),
                        Colore.BLU.getNome(), Colore.GIALLO.getNome()));
    }

    @Override
    public boolean validaRispostaInput(String risposta) {
        return coloreDaRisposta(risposta) != null;
    }

    @Override
    public void attivaeffetto(Partita partita, String risposta) { //attiva l'effetto con la risposta dell'utente
        Colore colore = coloreDaRisposta(risposta); // uguale null se la risposta non è valida
        if (colore == null) {
            throw new IllegalArgumentException("Colore non valido: " + risposta);
        }
        partita.applicaColoreScelto(colore);
    }

    private static Colore coloreDaRisposta(String risposta) { //traduce la risposta in un colore, o null se non valido
        if (risposta == null) {
            return null;
        }
        for (Colore colore : List.of(Colore.ROSSO, Colore.VERDE, Colore.BLU, Colore.GIALLO)) {
            if (colore.getNome().equalsIgnoreCase(risposta.trim())) {
                return colore;
            }
        }
        return null;
    }
}
