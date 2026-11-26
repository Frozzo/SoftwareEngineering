package unolegends.model;

/**
 * Passiva del Mercante (Salesman): pesca <b>una carta in meno</b> a ogni penalità di
 * pesca (+2, +4, +6, +10, accumulo dello stacking, Castigatore, mancato UNO).
 *
 * <p>Non vale per la roulette, che non passa da
 * {@link RegoleDiGioco#calcolaCartePescaForzata} (decisione presa), né per la pesca
 * volontaria, che non è una penalità.</p>
 */
final class PassivaSalesman implements IPassiva {
    @Override
    public int modificaPescaForzata(ContestoPesca contesto, int carte) {
        return Math.max(0, carte - 1);
    }
}
