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
Paper. Questa repository contiene i primi moduli pubblici: **Open Weapons** e
**Open Cosmetics**.

## Moduli

| Modulo | Stato | Descrizione |
| --- | --- | --- |
| `open-weapons` | Snapshot iniziale | Sistema armi, munizioni, accessori, armature, granate, C4, manette, radio, taser e utility item. |
| `open-cosmetics` | Estrazione iniziale | Cosmetici arma: LED, colori, skin, gettoni, GUI/editor e stazioni cosmetiche. |

## Stato della pubblicazione

Questo e' il primo snapshot estratto dal progetto privato originale. Il codice
del modulo e' gia' stato spostato nel namespace pubblico `dev.openrp.weapons`,
ma alcune classi fanno ancora riferimento ad API interne del vecchio core
roleplay.

La priorita' dei prossimi passaggi e':

1. estrarre una piccola API pubblica `openrp-core-api`;
2. sostituire le integrazioni interne con adapter opzionali;
3. rendere `open-weapons` compilabile e avviabile come plugin Paper
   indipendente;
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
