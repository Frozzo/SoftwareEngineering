package unolegends.model;

/**
 * Passiva de La Roccia: immune al <b>+2 singolo</b>.
 *
 * <p>Riconosce il +2 dall'origine della pesca: un +2 senza stacking ha origine
 * {@link OriginePesca#PIU_DUE}, e un accumulo fatto da un solo +2 viene accettato
 * con la stessa origine ({@link Accumulo#getOriginePenalita()}). Un accumulo di due o
 * più carte ha origine {@link OriginePesca#ACCUMULO} e La Roccia lo subisce come
 * tutti (decisione presa).</p>
 *
 * <p>Quando la penalità viene annullata del tutto, il possessore non perde nemmeno il
 * turno: lo decide {@link Partita}, che guarda se le passive hanno portato la pesca a
 * 0 ({@code Partita.isPenalitaAnnullata}).</p>
 */
final class PassivaRoccia implements IPassiva {
    @Override
    public int modificaPescaForzata(ContestoPesca contesto, int carte) {
        return contesto.origine() == OriginePesca.PIU_DUE ? 0 : carte;
    }
}
