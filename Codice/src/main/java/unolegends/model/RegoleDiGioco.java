package unolegends.model;

import java.util.List;
import java.util.Objects;

import unolegends.domain.rules.IStrategiaCondizioneSconfitta;
import unolegends.domain.rules.IStrategiaCondizioneVittoria;
import unolegends.domain.rules.IStrategiaCompatibilitaCarte;
import unolegends.domain.rules.IStrategiaPesca;
import unolegends.domain.rules.impl.StrategiaCompatibilitaCarte;
import unolegends.domain.rules.impl.StrategiaPescaSingola;
import unolegends.domain.rules.impl.StrategiaSconfittaStandard;
import unolegends.domain.rules.impl.StrategiaVittoriaStandard;

/**
 * Contenitore unico delle regole di gioco e delle relative strategie.
 *
 * <p>È il punto di variazione protetto: {@link Partita} conosce soltanto
 * questa astrazione concreta, mentre le singole regole restano sostituibili.</p>
 */
public class RegoleDiGioco {
    private final IStrategiaCondizioneVittoria strategiaVittoria;
    private final IStrategiaCondizioneSconfitta strategiaSconfitta;
    private final IStrategiaCompatibilitaCarte strategiaCompatibilitaCarte;
    private final IStrategiaPesca strategiaPesca;

    public RegoleDiGioco(IStrategiaCondizioneVittoria strategiaVittoria,
                         IStrategiaCondizioneSconfitta strategiaSconfitta,
                         IStrategiaCompatibilitaCarte strategiaCompatibilitaCarte,
                         IStrategiaPesca strategiaPesca) {
        this.strategiaVittoria = Objects.requireNonNull(strategiaVittoria);
        this.strategiaSconfitta = Objects.requireNonNull(strategiaSconfitta);
        this.strategiaCompatibilitaCarte = Objects.requireNonNull(strategiaCompatibilitaCarte);
        this.strategiaPesca = Objects.requireNonNull(strategiaPesca);
    }

    public boolean isGiocabile(Carta candidata, Carta inCima, Partita partita) {
        StatoTurno statoTurno = partita == null ? null : partita.getStatoTurno();
        return strategiaCompatibilitaCarte.puoGiocare(candidata, inCima, null, statoTurno);
    }

    public boolean isVincitore(Giocatore giocatore, Partita partita) {
        return strategiaVittoria.isVincitore(giocatore, partita);
    }

    public boolean isEliminato(Giocatore giocatore, Partita partita) {
        return strategiaSconfitta.isEliminato(giocatore, partita);
    }

    public List<Carta> pesca(Giocatore giocatore, Partita partita) {
        return strategiaPesca.pesca(giocatore, partita);
    }

    public static RegoleDiGioco standard() {
        return new RegoleDiGioco(
                new StrategiaVittoriaStandard(),
                new StrategiaSconfittaStandard(),
                new StrategiaCompatibilitaCarte(true),
                new StrategiaPescaSingola());
    }
}
