# Open Cosmetics

Open Cosmetics e' il modulo Paper dedicato ai cosmetici arma di Open Roleplay.
Gestisce LED, colori, skin, gettoni, editor GUI e stazioni cosmetiche.

## Collegamento con Open Weapons

Il plugin pubblica `OpenCosmeticsApi` tramite Bukkit Services. Open Weapons puo'
registrare un `OpenCosmeticsWeaponBridge` per permettere a Open Cosmetics di:

- riconoscere gli item arma;
- applicare e rimuovere cosmetici;
- chiedere il refresh visuale dopo una modifica.

Se il bridge non e' disponibile, Open Cosmetics si abilita comunque ma le
funzioni che richiedono un'arma non possono applicare cambiamenti.

## Comandi

- `/opencosmetics editor`: apre l'editor skin/LED/colore.
- `/opencosmetics gui [giocatore]`: apre il banco cosmetico.
- `/opencosmetics token <led|color> <id> [quantita'] [giocatore]`: crea gettoni.
- `/opencosmetics station <create|remove|list> [id]`: gestisce stazioni.
- `/weaponcosmetic`: alias compatibile.

## Permessi

- `openrp.cosmetics.use`
- `openrp.cosmetics.admin`
- Alias compatibili: `openrp.weapons.cosmetic.use` e
  `openrp.weapons.cosmetic.admin`
