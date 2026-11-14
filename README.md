# SoftwareEngineering

## UnoLegends

La demo CLI avvia una partita hotseat standard per due giocatori. La UI comunica
esclusivamente con `UnoLegendsGame` e con i DTO del controller; non importa né
riceve tipi del model. Il controller coordina i casi d'uso e converte i dati
del dominio in DTO adatti alla presentazione.

Avvio da PowerShell nella cartella `Codice`:

```powershell
javac -d out (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
java -cp out unolegends.ui.UnoLegendsCli
```

Nel menu iniziale scegli `0` per avviare la partita o `1` per uscire. Durante
la partita puoi giocare una carta, pescare, passare dopo aver pescato oppure
uscire; la CLI mostra lo stato del turno dopo ogni azione.

La regola standard dichiara vincitore chi gioca l'ultima carta. La CLI riceve
stato, richieste di input ed esiti tramite DTO del controller e non dipende
dalle classi del model. Il controller configura la partita passando alla factory
il `MazzoFactory`, il numero di giocatori e il profilo di regole:
`PartitaFactory.creaPartita(mazzoFactory, numeroGiocatori, regole)`.

Quando si gioca una carta avendone due in mano, la CLI permette di dichiarare
`uno`; se non viene dichiarato, la strategia di regole applica la penalità di
due carte.