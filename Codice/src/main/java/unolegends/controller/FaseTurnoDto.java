package unolegends.controller;

/**
 * Fase del turno esposta alla UI.
 *
 * <p>Replica i valori di {@code unolegends.model.FaseTurno} perché la UI non deve
 * importare classi del model (regola MVC del progetto). La conversione avviene in
 * {@link UnoLegendsGame} con uno {@code switch} esaustivo: se si aggiunge una
 * fase nel model, il compilatore segnala che va aggiunta anche qui.</p>
 */
public enum FaseTurnoDto {
    NORMALE,
    POST_PESCA,
    ATTESA_INPUT,
    ATTESA_UNO,
    TERMINATA
}
