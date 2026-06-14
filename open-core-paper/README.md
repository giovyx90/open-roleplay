# Open Core

`open-core-paper` e' il plugin Paper di base per Open Roleplay.

La v1 non contiene gameplay: espone servizi comuni e un lifecycle ordinato per
moduli futuri senza trascinare dentro feature private o troppo grandi.

## Cosa include

- servizio Bukkit `OpenRoleplayCore`;
- manager moduli esplicito, senza discovery riflessivo del vecchio progetto;
- comando `/opencore` con stato e reload;
- database opzionale SQLite o MySQL tramite HikariCP;
- servizio HUD temporaneo;
- invio opzionale del resource pack;
- blocco opzionale dell'esperienza vanilla.

## Configurazione

Il database e' disabilitato di default. Per una installazione piccola puoi usare
SQLite:

```yaml
database:
  enabled: true
  type: sqlite
```

Per reti o server con piu' moduli persistenti puoi usare MySQL:

```yaml
database:
  enabled: true
  type: mysql
  mysql:
    host: 127.0.0.1
    port: 3306
    database: open_roleplay
    username: open_roleplay
    password: ""
```

## Comandi

- `/opencore status`
- `/opencore reload`
- `/opencore reload <modulo>`

Alias: `/ocore`.

## Permessi

- `openrp.core.admin`
- `openrp.core.reload`
- `openrp.core.debug`
