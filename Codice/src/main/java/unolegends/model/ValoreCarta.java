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

    private final String Valore;

    // Costruttore
    ValoreCarta(String Valore) {
        this.Valore = Valore;
    }

    // Restituisce la stringa associata (es: "+2", "blocca turno")
    public String getValore() {
        return this.Valore;
    }

    public static ValoreCarta fromInt(int numero) {
        if (numero < 0 || numero > 9) {
            throw new IllegalArgumentException("Il numero deve essere compreso tra 0 e 9");
        }
        return values()[numero];
    }

    /**
     * Converte una stringa nel corrispondente valore dell'Enum.
     * È case-insensitive (es: "Blocca Turno" o "blocca turno" funzionano entrambi).
     */
    public static ValoreCarta getValoreAsAString(String testo) {
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