# Open Access

Open Access e' il plugin standalone di Open Roleplay per gestire accessi a
regioni WorldGuard e blocchi sensibili: casse, barili, porte, segnali e blocchi
interattivi custom.

## Funzioni

- profili collegati a regioni WorldGuard;
- preset `private`, `members`, `managers`, `public`, `custom`;
- override per singolo blocco;
- trust/untrust di player;
- GUI editor con shift-click su blocchi gestibili;
- storage SQLite predefinito o MySQL configurabile;
- API Bukkit Services per provider futuri di aziende, hotel o proprieta'.

## Comandi

```text
/openaccess
/openaccess region link <PROPERTY|COMPANY|HOTEL_ROOM|REGION> <region> <owner> [world]
/openaccess region unlink <region> [world]
/openaccess region info [region] [world]
/openaccess trust <player|uuid> [manage]
/openaccess untrust <player|uuid>
/openaccess player add <player|uuid> [open|manage|all]
/openaccess player remove <player|uuid>
/openaccess preset <private|members|managers|public|custom> [region|block]
/openaccess reload
/openaccess debug
```

Alias: `/access`, `/oa`.

## Permessi

- `openrp.access.admin`
- `openrp.access.region.manage`
- `openrp.access.reload`
- `openrp.access.debug`
- `openrp.access.bypass`

Sono accettati anche gli alias compatibili `next.access.*`.

## Dipendenze

Open Access richiede WorldGuard. WorldEdit e WorldGuard non vengono inclusi nel
jar perche' devono essere caricati dal server.

## Storage

Di default usa `plugins/OpenAccess/open_access.db`. Per MySQL:

```yaml
storage:
  type: mysql
  mysql:
    host: 127.0.0.1
    port: 3306
    database: open_access
    username: open_access
    password: ""
```
