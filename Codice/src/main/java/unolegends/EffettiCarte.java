package unolegends;


public final class EffettiCarte {
    private EffettiCarte() {}

    // Null Object: non fa assolutamente nulla
    public static final EffettoCarta NESSUN_EFFETTO =  -> {
        // Carta numerica standard: il turno semplicemente proseguirà al prossimo giocatore
    };

    // Effetti standard di UNO
    public static final EffettoCarta SALTA_TURNO = statopartita -> {
        statopartita.saltaProssimoGiocatore(); //da implementare dentro partita funzioni che ne modificano lo stato
    };

    public static final EffettoCarta INVERTI_GIRO = statopartita -> {
        statopartita.invertiDirezione();
    };

    public static final EffettoCarta PIU_DUE = statopartita -> {
        statopartita.faiPescareAlSuccessivo(2);
        statopartita.saltaProssimoGiocatore(); // Nelle regole standard chi becca il +2 perde anche il turno
    };

    public static final EffettoCarta PIU_QUATTRO = statopartita -> {
        statopartita.faiPescareAlSuccessivo(4);
        statopartita.saltaProssimoGiocatore();
    };
}