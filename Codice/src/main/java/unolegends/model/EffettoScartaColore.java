package unolegends.model;

import java.util.List;

/**
 * Carta "Scarta tutto" della No Mercy: chi la gioca scarta tutte le carte della
 * propria mano dello stesso colore della carta, <b>senza</b> attivarne gli effetti.
 *
 * <p>Il colore è quello attivo, che {@link Partita} ha appena impostato al colore
 * della carta giocata: così basta un'unica istanza dell'effetto per tutti i
 * colori. Le carte scartate finiscono negli scarti <b>sotto</b> la Scarta tutto,
 * che resta in cima: il colore attivo non cambia.</p>
 *
 * <p>Regole decise: se il giocatore resta senza carte ha vinto; se resta con una
 * carta deve dichiarare UNO (la carta rimasta era già sua, quindi
 * {@code Partita.deveDichiarareUno} apre la finestra UNO).</p>
 */
public class EffettoScartaColore implements Ieffetto {

    @Override
    public void attivaEffetto(Partita partita) {
        Giocatore giocatore = partita.getGiocatoreAttivo();
        Colore colore = partita.getColoreAttuale();
        List<Carta> daScartare = giocatore.getMano().stream()
                .filter(carta -> carta.getColore() == colore)
                .toList();
        for (Carta carta : daScartare) {
            giocatore.rimuoviCarta(carta);
        }
        partita.scartaSottoLaCima(daScartare);
        partita.registraEvento(EventoPartita.carteScartate(giocatore, daScartare.size(), colore));
    }

    @Override
    public boolean isAmmessoComeCartaIniziale() {
        return false;
    }
}
