# Demo — dati fittizi

Tutto ciò che serve per far girare la demo senza un vero server.

- [`seed.json`](seed.json) — la forma normalizzata dei dati. 5 profili
  (cittadino, agente, politico, imprenditore, medico), transazioni, 3 fascicoli
  OpenFDO in stati diversi, 2 aziende, 1 elezione, 10 leggi, decreti, offerte di
  lavoro, notifiche e notizie. **Nessun dato reale.**
- [`gestionale.demo.yml`](gestionale.demo.yml) — config in modalità `demo`.
- [`layout.demo.yml`](layout.demo.yml) — layout che mostra tutti i moduli.

## Avvio

```bash
cd ../api && npm install && npm run start:demo
cd ../frontend && npm install && npm run dev
```

Oppure dalla radice del modulo: `docker compose up --build`.

## Migliorare la demo

Il seed è un file JSON: apri una PR per aggiungere profili o scenari. La
struttura di ogni entità è la stessa che ogni data source restituisce, quindi
quello che funziona qui funziona identico su un DB reale (lo garantisce il test
di parità in `api/test/parity.test.js`).
