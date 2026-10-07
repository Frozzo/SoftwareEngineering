package unolegends;

public enum Numero {
    UNO(1),
    DUE(2),
    TRE(3),
    QUATTRO(4),
    CINQUE(5),
    SEI(6),
    SETTE(7),
    OTTO(8),
    NOVE(9),
    ZERO(0),
    NONE(-1); //per carte speciali senza numero

    public final int valore;
    
    Numero(int valore) {
        this.valore = valore;
    }

}
