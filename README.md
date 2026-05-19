# SoftwareEngineering

## UnoLegends - Iterazione 1

La demo CLI avvia una partita minima hotseat con stato iniziale già popolato.

Avvio da cartella `Codice`:

```powershell
javac -d out src\main\java\unolegends\*.java
java -cp out unolegends.UnoLegendsCli
```

Comandi disponibili nella demo:

- `gioca <idCarta>`: prova a giocare una carta compatibile con il colore o il numero della carta in cima.
- `pesca`: pesca una carta dal mazzo e la aggiunge alla mano del giocatore attivo.
- `passa`: passa automaticamente il turno al giocatore successivo.
- `esci`: termina la demo.

Lo stato del turno viene mostrato automaticamente dopo ogni comando.
