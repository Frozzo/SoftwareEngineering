package unolegends;

/**
 * Rappresenta i colori base delle carte UNO con formattazione ANSI per la CLI.
 */
public enum Colore {
    ROSSO("Rosso", "\u001B[31m"),
    VERDE("Verde", "\u001B[32m"),
    BLU("Blu", "\u001B[34m"),
    GIALLO("Giallo", "\u001B[33m");

    private static final String ANSI_RESET = "\u001B[0m";

    private final String nome;
    private final String ansiCode;

    Colore(String nome, String ansiCode) {
        this.nome = nome;
        this.ansiCode = ansiCode;
    }

    public String getNome() {
        return nome;
    }

    public String formatta(String testo) {
        return ansiCode + testo + ANSI_RESET;
    }
}
