# Contribuire a Open Roleplay

Grazie per l'interesse nel progetto. Questa repository accetta contributi
tramite pull request guidate.

## Regole pratiche

- Mantieni le modifiche piccole e revisionabili.
- Descrivi il comportamento cambiato, non solo i file modificati.
- Non includere segreti, token, IP privati, dump database, log reali o dati
  utente.
- Non aggiungere asset, modelli, texture, suoni o marchi se non sei certo di
  poterne concedere la licenza.
- Per codice Paper/Bukkit, evita I/O pesante sul main thread e torna sul main
  thread prima di usare API Bukkit non thread-safe.

## Validazione

Quando il modulo sara' completamente separato dalle API interne, la validazione
standard sara':

```bash
mvn -B -ntp test
```

Per ora `open-weapons` e' uno snapshot iniziale in fase di separazione dal core
privato: alcune integrazioni sono ancora da sostituire con API pubbliche.
