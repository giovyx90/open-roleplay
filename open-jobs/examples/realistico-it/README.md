# Esempio: realistico italiano

I **default del plugin** riproducono già questa ambientazione:

- **Lavori estrattivi:** Minatore (tesserino auto, a produzione, con strumento), Boscaiolo (libero, a
  consegna alla segheria), Pescatore (libero, a produzione, stagionale)
- **Lavori trasformativi:** Falegname (patente manuale, officina, trasformazioni)
- **Lavori a consegna:** Agricoltore (libero, a consegna al mercato, stagionale)
- **Lavori di servizio:** Spazzino (libero, a sessione con rilevamento pulizia strade)
- **Progressione:** 5 tier (Novizio → Apprendista → Lavoratore → Esperto → Mastro), decadimento
  attivo dopo 45 giorni inattivi

Quindi per il realistico-it **non serve copiare nulla**: installa il plugin e parti.

## Override per un setup più severo

`config.yml` in questa cartella alza l'asticella verso un server realistico maturo:

- `general.require_region_backend: true` — il lavoro richiede regioni reali (un bridge WorldGuard) con
  i tag `job_mine` / `job_forest` / `job_farm` / `job_water` / `job_workshop` / `job_street`; abilita
  anche il gating del punto di consegna per Boscaiolo e Agricoltore
- `general.seasonal_enabled: true` — attiva la stagionalità per Pescatore e Agricoltore
- `general.session_abandoned_minutes: 5` — abbandono più rapido fuori dalla region

Poi crea le location con `/lavoro admin location add <lavoro> <regione>` puntando alle regioni reali.
