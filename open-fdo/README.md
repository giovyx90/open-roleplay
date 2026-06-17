# Open FDO

Il **sistema nervoso dello stato** per server roleplay su Paper. Adapter-first e
**neutro rispetto all'ambientazione**: il core non conosce nessun corpo, grado, reato o
sigla. Conosce solo concetti astratti — autorita', atto, fascicolo, prova, condanna,
ricercato — e lascia tutto il resto alla configurazione e ad adapter opzionali.

> Regola unica: **se un giocatore puo' farlo fisicamente nel mondo, il plugin non lo fa
> al suo posto.** OpenFDO *registra* cio' che e' gia' avvenuto, *abilita* cio' che la
> fisica di Minecraft non garantisce (timer, mandati a scadenza, notifiche cross-player)
> ed *esegue* solo dove servirebbe un operatore umano 24/7 (rilascio a fine pena).

Apache 2.0. Parte della suite [Open Roleplay](https://github.com/giovyx90/open-roleplay).

## Test di neutralita'

Ogni feature passa un test: *funzionerebbe identica su un server fantasy medievale?* Se
no per un'assunzione di ambientazione, finisce in **config**; se no perche' il concetto
non esiste in quel mondo, finisce dietro un **adapter opzionale**. Il realistico italiano
e' solo la configurazione di riferimento, pubblicata come esempio — non il plugin.

## Architettura a tre livelli

1. **Core** (`dev.openrp.fdo`) — primitivi universali: `Capability`, `Agent`, `Dossier`,
   `Evidence`, `CustodyEntry`, `Verdict`, `WantedEntry`, `DetentionOrder`. Non importa mai
   nulla dal livello adapter.
2. **Config** — `corps.yml`, `ranks.yml`, `acts.yml`, `crimes.yml`, `wanted.yml`. Qui
   nascono "Polizia di Stato", i gradi, la durata custodia 48h, il catalogo reati.
3. **Adapter** — interfacce verso il mondo esterno, tutte opzionali e scoperte a runtime
   dal Bukkit ServicesManager. Se un adapter manca, la capability collegata sparisce in
   silenzio: nessun crash, nessuna dipendenza dura.

## Capability

Gli atti non sono legati ai corpi: sono legati a **capability**, e le capability sono
assegnate ai gradi dalla config. Catalogo del core:

`DETAIN_TEMPORARY`, `ARREST`, `ADD_CHARGE`, `SEIZE_EVIDENCE`, `ISSUE_FINE`,
`FLAG_WANTED`, `OPEN_INVESTIGATION`, `REQUEST_WARRANT`, `ISSUE_WARRANT`, `ISSUE_VERDICT`,
`EXTEND_CUSTODY`, `ECONOMIC_AUDIT` (adapter), `IMPORT_EXTERNAL_RECORD` (adapter),
`MANAGE_DETENTION` (adapter), `DECLARE_ALERT`.

I gradi inferiori sono ereditati dai superiori (ordine crescente). Un grado apicale puo'
arruolare e promuovere nel proprio corpo.

## Comandi

| Comando | Funzione |
| --- | --- |
| `/fdo` | identita': info, identifica, tesserino, servizio on/off, arruola, congeda, promuovi, degrada, reload |
| `/atto` | produzione atti: `nuovo [tipo] [bersaglio]` (apre il menu o consegna il libro), `lista` |
| `/registro` | archivi: `fascicolo`, `lista`, `capo`, `wanted`, `servizio`, `prova` |
| `/detenzione` | `lista`, `info`, `rilascia`, `condanna`, `proroga` (alias `/carcere`) |
| `/allerta` | `stato`, `dichiara <livello>`, `revoca` |

Il permesso Bukkit (`openfdo.use`) abilita solo l'uso del comando: l'autorita' reale
viene dalle capability del grado, non dal permesso. `openfdo.admin` bypassa i controlli di
capability per lo staff.

## L'atto e' un libro

`/atto nuovo` mostra **solo** gli atti la cui capability e' posseduta dal grado E il cui
adapter (se richiesto) e' presente. Selezionato un atto, l'agente riceve un libro
scrivibile: **scrive lui il contenuto**, il plugin non lo compila mai. Alla firma il
plugin timbra (corpo, grado, matricola, data, id fascicolo), registra l'atto e applica gli
effetti previsti (apre il fascicolo, avvia la custodia, sequestra la prova, segna il
ricercato...).

## Fascicolo e catena di custodia

Il fascicolo ha tre sezioni: **A** intestazione (immutabile), **B** corpo del procedimento
(capi d'imputazione, prove, note, custodia — mutabile), **C** esito (sentenza, immutabile
una volta firmata). L'id segue il pattern di config (default `{anno}/{numero}/{sigla_corpo}`).

Le prove tracciano una **catena di custodia** append-only: raccolta sulla scena, eventuali
trasferimenti, deposito. Un buco nella catena e' esattamente cio' che rende la prova
contestabile in RP — il core registra la catena, non decide l'ammissibilita'.

## Adapter

| Adapter | Responsabilita' | Assente -> |
| --- | --- | --- |
| `StorageAdapter` | Persiste agenti, fascicoli, prove, ricercati, atti, detenzioni | default YAML/memoria |
| `PermissionAdapter` | Permessi Bukkit (riflette LuckPerms) | default bukkit |
| `NotificationAdapter` | Notifiche (evasioni, allerte) | default chat |
| `LoggingAdapter` | Audit trail | default file/console/none |
| `RegionAdapter` | Region (armadi prove, zone intercettazione) | default no-op |
| `DutyStatusAdapter` | Stato di servizio | fallback interno `/fdo servizio` |
| `DetentionAdapter` | Esecuzione **fisica** della pena | condanna registrata, esecuzione manuale |
| `EconomyAuditAdapter` | Audit economico | la capability `ECONOMIC_AUDIT` sparisce |
| `ExternalRecordAdapter` | Referti/atti esterni | la capability `IMPORT_EXTERNAL_RECORD` sparisce |
| `RadioAdapter` | Intercettazione | niente intercettazione |
| `EvidenceSourceAdapter` | Impronte/tracce sulle prove | sequestro manuale senza tracce |

La detenzione e' la riscrittura piu' importante: il core decide *che* c'e' una condanna e
gestisce il **timer della pena** e il **rilascio automatico**; l'adapter decide cosa
significa fisicamente (carcere realistico, segrete fantasy, cella criogenica). Senza
adapter la condanna resta comunque nel fascicolo.

## API pubblica

```java
OpenFdoApi api = Bukkit.getServicesManager().load(OpenFdoApi.class);

// Registra il tuo modulo carcere come esecutore della pena
api.adapters().setDetention(myPrisonModule);

// Un modulo carcere segnala un'evasione confermata
api.reportEscape(inmate, "world", x, y, z);
```

## Configurazione

Vedi `src/main/resources/` per i default (realistico italiano) e `examples/` per una
configurazione fantasy di partenza. Modifica i file YAML e ricarica con `/fdo reload`:
nessuna modifica al codice per cambiare ambientazione.

## Build

```bash
mvn -B -ntp package -pl open-fdo -am
# il jar finisce in: open-fdo/target/
```

## Licenza

Apache License 2.0. Vedi `LICENSE` nella radice della repository.
