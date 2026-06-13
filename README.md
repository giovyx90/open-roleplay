# Open Roleplay

Open Roleplay e' una suite open source per esperienze Minecraft roleplay su
Paper. Questa repository contiene il primo modulo pubblico: **Open Weapons**.

## Moduli

| Modulo | Stato | Descrizione |
| --- | --- | --- |
| `open-weapons` | Snapshot iniziale | Sistema armi, munizioni, accessori, armature, granate, C4, manette, radio, taser e utility item. |

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
4. aggiungere esempi di configurazione e resource-pack placeholder con asset
   pubblicabili.

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
  open-weapons/
    src/main/java/dev/openrp/weapons/
    src/main/resources/
    src/test/java/dev/openrp/weapons/
```

## Note per sviluppatori

Il modulo contiene ancora feature roleplay avanzate legate a sistemi esterni
come banca, identita', polizia, staff log e GUI custom. Durante il decoupling
questi punti dovranno diventare integrazioni opzionali e degradare in modo
pulito quando il servizio esterno non e' presente.
