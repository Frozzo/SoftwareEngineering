# SoftwareEngineering

## UnoLegends

La demo CLI avvia una partita hotseat standard per due giocatori. La UI comunica
esclusivamente con `UnoLegendsGame`, che coordina i casi d'uso e traduce i dati
del dominio in uno snapshot adatto alla presentazione.

Avvio da PowerShell nella cartella `Codice`:

```powershell
javac -d out (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
java -cp out unolegends.ui.UnoLegendsCli
```

Nel menu iniziale scegli `0` per avviare la partita o `1` per uscire. Durante
la partita puoi giocare una carta, pescare, passare dopo aver pescato oppure
uscire; la CLI mostra lo stato del turno dopo ogni azione.

La regola standard dichiara vincitore chi gioca l'ultima carta. Con
`RegoleDiGioco.noMercy()` è valida anche la vittoria quando tutti gli avversari
hanno più di 25 carte in mano. La CLI riceve l'esito tramite il controller e
non dipende dalle classi del model. Il profilo di regole può essere passato a
`PartitaFactory.creaPartita(tipoMazzo, numeroGiocatori, regole)`; il mazzo
`NO_MERCY` in sé resta non implementato.

Quando si gioca una carta avendone due in mano, la CLI permette di dichiarare
`uno`; se non viene dichiarato, la strategia di regole applica la penalità di
due carte.