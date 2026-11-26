# SoftwareEngineering

## UnoLegends

La CLI avvia una partita hotseat da 2 a 6 giocatori. La UI comunica
esclusivamente con `UnoLegendsGame` e con i DTO del controller; non importa né
riceve tipi del model. Il controller coordina i casi d'uso e converte i dati
del dominio in DTO adatti alla presentazione.

Avvio da PowerShell nella cartella `Codice`:

```powershell
javac -d out (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
java -cp out unolegends.ui.UnoLegendsCli
```

Nel menu iniziale scegli `0` per una nuova partita o `1` per uscire. Il menu di
setup chiede:

1. la modalità: **Standard** oppure **No Mercy** (mazzo da 168 carte con 0 e 7 che spostano le mani,
   Scarta tutto, Salta tutti, +4 colorati, jolly +4 cambio giro, +6, +10, roulette; pesca finché trovi una
   carta giocabile; eliminazione a 25 carte; stacking sempre attivo);
2. le opzioni, con una domanda sì/no ciascuna:
   - **Endless**: chi finisce le carte esce e si continua fino all'ultimo, poi si
     stila la classifica; serve almeno 3 giocatori;
   - **Stacking** (solo nella partita Standard, in No Mercy è sempre attivo): a una
     carta di pesca si risponde con una carta di pesca di valore uguale o superiore,
     di qualsiasi colore; le carte si accumulano e chi non risponde le pesca tutte e
     perde il turno;
3. il numero di giocatori (2–6) e i loro nomi (invio = nome predefinito).

Se le scelte non sono valide, la CLI mostra il motivo e ripete il setup.
Durante la partita puoi giocare una carta, pescare, passare dopo aver pescato
oppure uscire; quando c'è uno stacking in corso puoi rispondere con una carta di
pesca oppure accettare la penalità; la CLI mostra la tabella dei giocatori (carte in mano e stato) e
lo stato del turno dopo ogni azione.

Nella partita normale vince chi gioca l'ultima carta; con Endless la partita
continua finché resta un solo giocatore. A fine partita la CLI stampa la
classifica. Il controller traduce le scelte del menu in una
`ConfigurazionePartita` e la passa a `PartitaFactory.creaPartita(config)`, che
crea mazzo, regole e giocatori.

Quando si gioca una carta avendone due in mano, la CLI permette di dichiarare
`uno`; se non viene dichiarato, la strategia di regole applica la penalità di
due carte.
