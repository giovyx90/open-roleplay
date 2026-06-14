# Asset Open Weapons

Questa cartella contiene gli asset pubblicabili collegati al modulo
`open-weapons`.

## Resource pack

Il sotto-pack e' in `resource-pack/` e include solo gli asset necessari alle
feature di Open Weapons:

- dispatcher item filtrati sui custom model data usati dal modulo;
- modelli e texture `custom/tools/crime` referenziati dai dispatcher;
- texture GUI collegate a crime/weapons;
- suoni `sounds/weapons` e `sounds.json` filtrato.
- strumenti RP pubblici limitati a passamontagna, C4, manette, tronchesi,
  taser, corda e forbici.

Gli asset cosmetici delle armi sono stati estratti in
`../open-cosmetics/assets/resource-pack/`. Open Weapons conserva i dispatcher e
le varianti model-data; Open Cosmetics conserva modelli, texture e suoni di
LED, colori, skin e gettoni.

Non include zip generati, hash di deploy, config VPS, log, dati giocatori o
asset globali non necessari al modulo pubblico.

Per sostituire armi, model data o texture, leggi `../DEVELOPER_GUIDE.md`.
