package unolegends.model;

public enum ValoreCarta {
    // Carte numeriche
    ZERO("0"),
    UNO("1"),
    DUE("2"),
    TRE("3"),
    QUATTRO("4"),
    CINQUE("5"),
    SEI("6"),
    SETTE("7"),
    OTTO("8"),
    NOVE("9"),
    
    // Carte azione
    PIU_DUE("+2"),
    BLOCCA_TURNO("blocca turno"),
    CAMBIA_GIRO("cambia giro"), // Aggiunta per completezza delle regole di Uno
    
    // Carte speciali (nere)
    CAMBIA_COLORE("cambia colore"),
    PIU_QUATTRO("+4");

    private final String valore;

    ValoreCarta(String valore) {
        this.valore = valore;
    }

    // Restituisce la stringa associata (es: "+2", "blocca turno")
    public String getValore() {
        return this.valore;
    }

    public static ValoreCarta fromInt(int numero) {
        return switch (numero) {
            case 0 -> ZERO;
            case 1 -> UNO;
            case 2 -> DUE;
            case 3 -> TRE;
            case 4 -> QUATTRO;
            case 5 -> CINQUE;
            case 6 -> SEI;
            case 7 -> SETTE;
            case 8 -> OTTO;
            case 9 -> NOVE;
            default -> throw new IllegalArgumentException(
                    "Il numero deve essere compreso tra 0 e 9");
        };
    }

    /**
     * Converte una stringa nel corrispondente valore dell'Enum.
     * È case-insensitive (es: "Blocca Turno" o "blocca turno" funzionano entrambi).
     */
    public static ValoreCarta fromString(String testo) {
        if (testo == null) {
            throw new IllegalArgumentException("La stringa non può essere nulla");
        }
        
        for (ValoreCarta valore : ValoreCarta.values()) {
            if (valore.getValore().equalsIgnoreCase(testo.trim())) {
                return valore;
            }
        }
        throw new IllegalArgumentException("Nessuna carta trovata per la stringa: " + testo);
    }
}