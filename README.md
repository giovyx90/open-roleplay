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
| `open-core-api` | Sperimentale, compilabile | Contratti pubblici condivisi: lifecycle moduli, registrazione moduli, database opzionale, HUD, messaggi, permessi e utility item. |
| `open-core-paper` | Sperimentale, avviabile | Plugin Paper `OpenCore`: servizio Bukkit, comando `/opencore`, DB SQLite/MySQL opzionale, resource pack ed esperienza opzionali. |
| `open-access` | Standalone iniziale | Plugin Paper per WorldGuard, profili, trust, casse, porte, blocchi interattivi e storage SQLite/MySQL. Se trova OpenCore si registra in `/opencore status`. |
| `open-cosmetics` | Standalone iniziale | Plugin Paper per cosmetici arma: LED, colori, skin, gettoni, GUI/editor e stazioni cosmetiche. Se trova OpenCore si registra in `/opencore status`. |
| `open-weapons` | Standalone iniziale, compilabile | Plugin Paper per armi, munizioni, accessori, armature, granate, C4, manette, taser, utility RP, rapine e perquisizioni. Usa bridge open/no-op per servizi opzionali non ancora pubblicati. |
| `open-vending-machines` | Standalone iniziale, compilabile | Plugin Paper adapter-first per distributori automatici roleplay: prodotti, stock, prezzi, casse, aziende opzionali e integrazioni economy/inventory sostituibili. |
| `open-companies` | Standalone iniziale, compilabile | Plugin Paper adapter-first per aziende roleplay: ruoli, membri, inviti, camera di commercio (stato, licenze, sede), asset aziendali e API pubblica. Modalita' creazione PLAYER_DIRECT/PLAYER_APPLICATION/ADMIN_ONLY, con integrazione opzionale verso OpenVendingMachines. |
| `open-fdo` | Standalone iniziale, compilabile | Plugin Paper adapter-first e neutro rispetto all'ambientazione: il sistema nervoso dello stato. Corpi, gradi e capability tutti da config; atti depositati come libri timbrati; fascicoli con catena di custodia delle prove; ricercati, detenzione via adapter e foglio di servizio. Espone `OpenFdoApi`. |
| `open-crime` | Standalone iniziale, compilabile | Plugin Paper adapter-first e neutro rispetto all'ambientazione: il sottobosco criminale. Organizzazioni illegali, gerarchia, territorio, produzione multistadio, traffico, riciclaggio ed estorsione, tutti da config. Niente "heat": le forze dell'ordine apprendono solo tramite Discovery generate da azioni RP concrete (denuncia, scoperta fisica, arresto, informatore, indagine). Economia, aziende e autorità via adapter opzionali. Espone `OpenCrimeApi`. |
| `open-gestionale` | Demo, eseguibile | Gestionale web universale (non un plugin Paper): API bridge Node.js + frontend React. Carica il layout dall'API e renderizza widget adapter-first; moduli, label e permessi vengono dalla config. Auth via OTP in gioco + JWT, sola lettura sul DB di gioco, demo con dati fittizi. |

## Stato della pubblicazione

Open Roleplay e' pubblicato come base aperta e modulare. Ogni modulo deve
restare comprensibile, compilabile e adattabile da altri server, senza richiedere
servizi proprietari per avviarsi. Le feature grandi o molto legate al gameplay
devono diventare moduli autonomi quando hanno confini chiari.

Stato attuale:

- `OpenCore`: sperimentale, avviabile, espone `OpenRoleplayCore` e la registrazione moduli pubblica.
- `OpenAccess`: plugin standalone iniziale, compatibile con OpenCore quando presente.
- `OpenCosmetics`: plugin standalone iniziale, compatibile con OpenCore quando presente.
- `OpenWeapons`: plugin standalone iniziale e compilabile; armi, utility, rapine e perquisizioni funzionano come base pubblica, mentre le integrazioni opzionali degradano tramite bridge minimali quando il relativo modulo non e' presente.
- `OpenVendingMachines`: plugin standalone iniziale e compilabile; distributori automatici configurabili con adapter sostituibili per economy, inventario, aziende, storage, notifiche e audit.
- `OpenCompanies`: plugin standalone iniziale e compilabile; aziende con ruoli/capability, inviti, camera di commercio (stato, licenze, sede) e asset aziendali, con adapter sostituibili per storage, economy, permessi, regioni, identita', notifiche e audit. Espone `OpenCompaniesApi` e si integra opzionalmente con OpenVendingMachines.
- `OpenFDO`: plugin standalone iniziale e compilabile, neutro rispetto all'ambientazione; corpi, gradi, capability, atti, reati e livelli ricercato sono tutti definiti in config. Gli atti si depositano scrivendo e firmando un libro che il plugin timbra; i fascicoli tracciano capi d'imputazione, prove con catena di custodia e sentenze; la detenzione e l'audit economico passano da adapter opzionali (assenti -> la capability sparisce). Espone `OpenFdoApi`.
- `OpenCrime`: plugin standalone iniziale e compilabile, adapter-first e neutro rispetto all'ambientazione; organizzazioni illegali, gerarchia, territorio, produzione, traffico, riciclaggio ed estorsione sono tutti definiti in config. Filosofia RP First: non esiste un "heat" nascosto, le forze dell'ordine non ricevono nulla in automatico e apprendono solo tramite Discovery generate da azioni RP concrete (denuncia, scoperta fisica, arresto, informatore, indagine). Economia (denaro sporco/pulito), aziende e autorità passano da adapter opzionali con default interni; assenti o non reali, la feature collegata degrada in silenzio. I cinque sottosistemi (syndicate, production, traffic, laundering, racket) si attivano dai toggle in `config.yml`. Espone `OpenCrimeApi`.
- `OpenGestionale`: gestionale web universale e adapter-first, non un plugin Paper. API bridge Node.js (sul VPS, sola lettura sul DB di gioco) + frontend React statico (su CDN). Il layout e i widget sono risolti dall'API in base ai moduli attivi e alle capability del giocatore; il frontend non conosce l'ambientazione. Auth senza password (OTP in gioco + JWT), demo con dati fittizi, esempi `realistico-it` e `fantasy` sullo stesso codice. Avvio con `docker compose up` o `npm` dentro `open-gestionale/`.

La priorita' dei prossimi passaggi e':

1. trasformare i bridge minimali in API open dedicate quando i moduli esistono;
2. valutare estrazioni dedicate per sistemi grandi come food, hospital,
   staffboard e interaction;
3. mantenere i sotto-pack pubblicabili in `open-weapons/assets/resource-pack/`
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

La licenza del codice non concede diritti su marchi, loghi, nomi server o asset
non inclusi esplicitamente in questa repository. Vedi `TRADEMARKS.md`.

## Requisiti previsti

- Java 21
- Maven 3.9+
- Paper API 1.21.x
- Dipendenze opzionali previste: Nexo, WorldGuard, PacketEvents, AnvilGUI

## Build

Build completa locale e CI:

```bash
mvn -B -ntp package
```

Build mirata OpenWeapons con dipendenze di reactor:

```bash
mvn -B -ntp -pl open-weapons -am package
```

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
  open-vending-machines/
    src/main/java/dev/openrp/vending/
    src/main/resources/
    src/test/java/dev/openrp/vending/
  open-companies/
    src/main/java/dev/openrp/companies/
    src/main/resources/
    src/test/java/dev/openrp/companies/
  open-fdo/
    src/main/java/dev/openrp/fdo/
    src/main/resources/
    src/test/java/dev/openrp/fdo/
    examples/
  open-crime/
    src/main/java/dev/openrp/crime/
    src/main/resources/
    src/test/java/dev/openrp/crime/
    examples/                 # realistico-it/ e fantasy/
  open-gestionale/            # progetto Node.js + React (non Maven)
    api/                      # bridge Node.js (Express, OpenAPI, SQLite)
    frontend/                 # SPA React (Vite)
    demo/                     # seed dati fittizi + config demo
    examples/                 # realistico-it/ e fantasy/
```

## Note per sviluppatori

`open-weapons` espone una base pubblica per armi, munizioni, accessori,
protezioni, utility RP, rapine e perquisizioni. Le integrazioni opzionali con
altri moduli Open Roleplay devono passare da bridge difensivi: se il modulo non
e' installato, la feature deve degradare in modo chiaro senza bloccare
l'avvio del plugin.
