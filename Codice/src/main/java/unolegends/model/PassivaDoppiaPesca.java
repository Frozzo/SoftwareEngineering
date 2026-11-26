package unolegends.model;

/**
 * Passiva dell'Acrobata (Doppia pesca): quando pesca volontariamente pesca 2 carte,
 * ne tiene una e rimette l'altra in fondo al mazzo.
 *
 * <p>Funziona solo nella partita normale: in No Mercy si pesca finché si trova una
 * carta giocabile e la passiva non ha senso. Non lo controlla la passiva ma la
 * strategia di pesca ({@code IStrategiaPesca#ammettePescaConScelta()}), quindi qui
 * non c'è nessun {@code if} sulla modalità.</p>
 */
final class PassivaDoppiaPesca implements IPassiva {
    static final int CARTE_PESCATE = 2;

    @Override
    public int getCarteTraCuiScegliereInPesca() {
        return CARTE_PESCATE;
    }
}
