# Esempio: fantasy

Una gilda di artigiani invece di una camera di commercio moderna. **Stesso plugin, stesso codice** —
cambia solo la config.

- **Lavori estrattivi:** Cercatore di Minerali (libero, a produzione, con piccone), Taglialegna
  (libero, a produzione), Pescatore del Fiume (libero, a produzione, stagionale)
- **Lavori trasformativi:** Carpentiere di Gilda e Fabbro d'Armi (entrambi a patente manuale —
  il mestiere si impara da un maestro della gilda)
- **Lavori di servizio:** Stalliere e Cantoniere (liberi, a sessione)
- **Progressione:** 4 tier (Apprendista → Artigiano → Esperto → Maestro), nessun decadimento

## Uso

Copia i file di questa cartella in `plugins/OpenJobs/`, poi `/lavoro admin reload`. Crea le location
con `/lavoro admin location add <lavoro> <regione>`. Senza un backend di regioni reale il lavoro usa
le regioni sintetiche per-chunk; il gating del punto di consegna richiede regioni reali, ma qui nessun
lavoro usa il modello a consegna, quindi non serve.

Il **test di neutralità**: il core non sa cos'è una "gilda dei fabbri". Vede un lavoro trasformativo
che richiede una licenza emessa manualmente e paga per trasformazione — identico, concettualmente, al
falegname del realistico-it.
