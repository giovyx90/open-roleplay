# Open Roleplay

Trovi un bug?
Prendi il codice e sistemalo.

Non ti piace qualcosa?
Modificalo.

Vuoi adattarlo o ampliarlo per il tuo server?
Sei libero di farlo.

È gratuito, e lo sarà per sempre.

Open, perché è trasparente.
Roleplay, perché è la modalità che da tempo amiamo.

Open Roleplay e' una suite open source per esperienze Minecraft roleplay su
Paper. Questa repository contiene moduli Paper separati, pensati per essere
usati insieme ma leggibili e modificabili anche uno alla volta.

## Moduli

| Modulo | Stato | Descrizione |
| --- | --- | --- |
| `open-core-api` | Fondamenta iniziale | Contratti pubblici condivisi: lifecycle moduli, database opzionale, HUD, messaggi, permessi e utility item. |
| `open-core-paper` | Fondamenta iniziale | Plugin Paper `OpenCore`: servizio Bukkit, comando `/opencore`, DB SQLite/MySQL opzionale, resource pack ed esperienza opzionali. |
| `open-access` | Estrazione iniziale | Controllo accessi per WorldGuard, profili, trust, casse, porte, blocchi interattivi e storage SQLite/MySQL. |
| `open-weapons` | Snapshot iniziale | Sistema armi, munizioni, accessori, armature, granate, C4, manette, radio, taser e utility item. |
| `open-cosmetics` | Estrazione iniziale | Cosmetici arma: LED, colori, skin, gettoni, GUI/editor e stazioni cosmetiche. |

## Stato della pubblicazione

Questo e' uno snapshot estratto e ripulito dal progetto privato originale. Il
core pubblico non e' una copia diretta del vecchio core privato: contiene solo
l'infrastruttura riutilizzabile. Le feature grandi o molto legate al gameplay
devono diventare moduli autonomi quando hanno confini chiari.

La priorita' dei prossimi passaggi e':

1. sostituire le integrazioni interne residue con adapter opzionali;
2. rendere `open-weapons` compilabile e avviabile come plugin Paper
   indipendente;
3. valutare estrazioni dedicate per sistemi grandi come food, hospital,
   staffboard e interaction;
4. mantenere i sotto-pack pubblicabili in `open-weapons/assets/resource-pack/`
   e `open-cosmetics/assets/resource-pack/`.

## Resource pack

Ogni modulo mantiene il proprio resource pack in formato aperto dentro
`assets/resource-pack/`: la cartella contiene `pack.mcmeta` e `assets/`, quindi
puo' essere esplorata, modificata e ricompressa senza strumenti proprietari.

Per creare zip caricabili direttamente da Minecraft:

```bash
bash scripts/build-resource-packs.sh
```

Gli archivi vengono generati in `target/resource-packs/` con `pack.mcmeta` e
`assets/` alla radice dello zip. La stessa generazione viene agganciata al
phase Maven `package`, cosi' un build di release produce sempre anche i pack.

## Licenza

Il codice e' distribuito con licenza Apache License 2.0. Vedi `LICENSE`.

La licenza del codice non concede diritti su marchi, loghi, nomi server,
resource pack privati o asset non inclusi esplicitamente in questa repository.
Vedi `TRADEMARKS.md`.

## Requisiti previsti

- Java 21
- Maven 3.9+
- Paper API 1.21.x
- Dipendenze opzionali previste: Nexo, WorldGuard, PacketEvents, AnvilGUI

## Struttura

```text
open-roleplay/
  open-core-api/
    src/main/java/dev/openrp/core/api/
  open-core-paper/
    src/main/java/dev/openrp/core/
    src/main/resources/
  open-access/
    src/main/java/dev/openrp/access/
    src/main/resources/
  open-cosmetics/
    assets/resource-pack/
    src/main/java/dev/openrp/cosmetics/
    src/main/resources/
  open-weapons/
    assets/resource-pack/
    src/main/java/dev/openrp/weapons/
    src/main/resources/
    src/test/java/dev/openrp/weapons/
```

## Note per sviluppatori

Il modulo contiene ancora feature roleplay avanzate legate a sistemi esterni
come banca, identita', polizia, staff log e GUI custom. Durante il decoupling
questi punti dovranno diventare integrazioni opzionali e degradare in modo
pulito quando il servizio esterno non e' presente.
