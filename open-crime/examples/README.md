# Open Crime — esempi di configurazione

Stesso plugin, due ambientazioni opposte, **zero righe di codice diverse**. Tutto cambia
nei file YAML.

- `realistico-it/` — il setting realistico italiano. I default del plugin (i file in
  `src/main/resources/`) **sono già** questa ambientazione; qui trovi solo gli override per
  una versione più severa (regioni WorldGuard obbligatorie, banca reale richiesta, equipaggio
  di fondazione).
- `fantasy/` — un setting fantasy: veleni e pozioni proibite, gilde di assassini, giardini
  di erbe vietate, laboratori alchemici, aste clandestine.

## Come si usano

Copia il contenuto della cartella scelta nella data folder del plugin
(`plugins/OpenCrime/`), sovrascrivendo i file generati al primo avvio, poi `/opencrime
reload`. I file non presenti nell'esempio restano ai default.

Il **test di neutralità**: nessuno dei due esempi tocca il core. Se una feature funziona in
uno ma non nell'altro, è perché manca un adapter (es. il fantasy senza WorldGuard usa le
regioni sintetiche per-chunk), non perché il plugin "sappia" cos'è una gilda di assassini.
