# Open Jobs — esempi di configurazione

Stesso plugin, due ambientazioni opposte, **zero righe di codice diverse**. Tutto cambia nei file YAML.

- `realistico-it/` — il setting realistico italiano. I default del plugin (i file in
  `src/main/resources/`) **sono già** questa ambientazione; qui trovi solo gli override per una
  versione più severa (regioni reali obbligatorie, stagionalità attiva).
- `fantasy/` — un setting fantasy: cercatori di minerali, taglialegna, pescatori del fiume, carpentieri
  e fabbri d'armi di gilda, stallieri e cantonieri.

## Come si usano

Copia il contenuto della cartella scelta nella data folder del plugin (`plugins/OpenJobs/`),
sovrascrivendo i file generati al primo avvio, poi `/lavoro admin reload`. I file non presenti
nell'esempio restano ai default.

Il **test di neutralità**: nessuno dei due esempi tocca il core. Se una feature funziona in uno ma
non nell'altro, è perché manca un adapter (es. il fantasy senza WorldGuard usa le regioni sintetiche
per-chunk e non può applicare il gating del punto di consegna), non perché il plugin "sappia" cos'è
un taglialegna.
