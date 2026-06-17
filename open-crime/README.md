# Open Crime

Il **motore della criminalità organizzata** per server roleplay su Paper. Adapter-first e
**neutro rispetto all'ambientazione**: il core non conosce nessuna sostanza, organizzazione
reale o metodo criminale. Conosce solo concetti astratti — organizzazione illegale, bene
proibito, territorio, evento criminale, scoperta — e lascia tutto il resto alla
configurazione e ad adapter opzionali.

> Regola unica: **il plugin non gioca al posto del criminale.** Registra che qualcosa di
> illegale è successo, abilita meccaniche che la fisica di Minecraft non garantisce, ed
> esegue solo ciò che richiederebbe un admin attivo 24/7. L'organizzazione la costruiscono i
> giocatori, il territorio lo conquistano fisicamente, la produzione richiede presenza e
> ingredienti reali.

Apache 2.0. Parte della suite [Open Roleplay](https://github.com/giovyx90/open-roleplay).

## RP First: niente "heat", solo scoperte

Non esiste un numero di "attenzione" nascosto che cresce nel tempo. Il core è **opaco alle
forze dell'ordine per default**: ogni evento criminale resta privato finché non viene
collegato a una **Discovery**, cioè un'azione RP concreta. Se nessuno fa nulla, il plugin
non fa nulla. I cinque tipi di scoperta:

| Tipo | Come nasce |
| --- | --- |
| `denuncia` | un civile usa `/denuncia` vicino a un agente — servono entrambi fisicamente presenti, e un crimine dev'essere avvenuto lì di recente |
| `scoperta_fisica` | un agente entra in una location di produzione attiva — notifica solo a lui, solo in quel momento |
| `arresto` | un membro arrestato con beni illegali addosso collega gli eventi recenti dell'org al fascicolo |
| `informatore` | un membro usa `/informatore` vicino a un agente abilitato; rivela gli eventi che conosce |
| `indagine` | un agente collega manualmente scoperte già esistenti (lato Open FDO) |

Un'organizzazione senza alcuna Discovery **non esiste** agli occhi delle autorità.

## Test di neutralità

Ogni feature passa un test: *funzionerebbe identica su un server fantasy medievale?* "Coca"
→ no → config. "Bene illegale prodotto in una location con ingredienti fisici in un certo
tempo" → sì → core. Il realistico italiano è solo la configurazione di riferimento, inclusa
come default; vedi `examples/fantasy` per un'ambientazione opposta sullo stesso codice.

## Architettura a tre livelli

1. **Core** (`dev.openrp.crime`) — primitivi universali: `IllegalOrg`, `OrgMember`,
   `Territory`, `IllegalGood`, `CrimeEvent`, `Discovery`. Non importa mai nulla dagli
   adapter.
2. **Config** — `goods.yml`, `syndicate.yml`, `production.yml`, `traffic.yml`,
   `laundering.yml`, `racket.yml`. Qui nascono cocaina, la gerarchia Picciotto→Boss, le
   ricette, le rotte, i metodi di riciclaggio.
3. **Adapter** — interfacce verso il mondo esterno, tutte con un default e scoperte a
   runtime dal Bukkit ServicesManager. Assenti, la feature collegata degrada in silenzio.

## Moduli (sottosistemi attivabili)

I cinque sottosistemi sono concettualmente plugin separati che dipendono dal core; qui
vengono distribuiti come un unico plugin con i toggle in `config.yml` (`modules:`). Un
modulo disattivato non registra nulla.

| Modulo | Comando | Cosa fa |
| --- | --- | --- |
| Open Syndicate | `/syndicate`, `/territory` | fondazione, gerarchia, reclutamento, chat interna, territorio |
| Open Production | `/produce` | produzione multistadio di beni illegali in location fisiche |
| Open Traffic | `/traffic` | spedizioni lungo rotte fisiche intercettabili |
| Open Laundering | `/launder` | riciclaggio di denaro sporco in pulito nel tempo |
| Open Racket | `/racket` | estorsione/pizzo sulle aziende |

Più i comandi del core: `/denuncia`, `/informatore`, `/opencrime` (status/reload).

## Gerarchia e capability

Le gerarchie non sono hardcoded: in `syndicate.yml` definisci ranghi, ordine e capability.
Le capability sono **ereditate verso l'alto** (un rango possiede ogni capability concessa a
un rango di ordine pari o inferiore); il rango apicale di solito ha `ALL`.

Catalogo: `INVITE`, `EXPEL`, `PROMOTE`, `DEMOTE`, `DISSOLVE`, `VIEW_TREASURY`,
`TERRITORY_CLAIM`, `PRODUCE`, `PRODUCE_CANCEL`, `TRAFFIC`, `TRAFFIC_AGREEMENT`,
`TRAFFIC_LOG`, `LAUNDER`, `RACKET_IMPOSE`, `RACKET_COLLECT`, `RACKET_ESCALATE`,
`RACKET_MANAGE`, `ALL`.

Il permesso Bukkit (`opencrime.use`) abilita solo l'uso dei comandi: l'autorità reale viene
dal rango. `opencrime.admin` bypassa i controlli di capability per lo staff.

## Adapter

| Adapter | Default | Assente / non reale → |
| --- | --- | --- |
| `StorageAdapter` | YAML atomico con backup (o `memory`) | — |
| `PermissionAdapter` | Bukkit (riflette LuckPerms) | — |
| `NotificationAdapter` | chat / action bar | — |
| `RegionAdapter` | sintetico per-chunk (`available=false`) | con `territory.require_worldguard` off il plugin resta usabile; un bridge WorldGuard porta regioni e tag reali |
| `EconomyAdapter` | ledger interno (sporco/pulito) | un bridge Open Bank lo sostituisce |
| `CompanyAdapter` | no-op (nessuna azienda) | il racket degrada: `/racket imponi` non trova aziende |
| `AuthorityAdapter` | per permessi (agenti = nodo permesso, fascicolo sintetico) | un bridge Open FDO apre fascicoli reali e collega le scoperte |

La **discovery** funziona anche standalone: con l'`AuthorityAdapter` di default un "agente" è
un giocatore col permesso `opencrime.authority.agent`, e il fascicolo è un id locale.

## API pubblica

Recupera `OpenCrimeApi` dal Services Manager di Bukkit. È il registro centrale
dell'illegalità, ma le autorità leggono gli eventi di un'org **solo** passando un
`dossierId` che vere scoperte hanno collegato.

```java
OpenCrimeApi api = Bukkit.getServicesManager().load(OpenCrimeApi.class);

// registra il tuo bridge (es. Open Bank) come economy adapter
api.adapters().setEconomy(myBankBridge);

boolean illegale = api.isIllegal(item);
api.getOrgByMember(playerUuid).ifPresent(org -> ...);

// le FDO vedono solo gli eventi collegati a un fascicolo reale
List<CrimeEvent> visibili = api.getDiscoveredEventsByOrg("clan", dossierId);
```

## Build

```bash
mvn -B -ntp -pl open-crime -am package
```

Richiede Java 21 e Paper API 1.21.x. Dipendenze opzionali (tutte soft): OpenCore, OpenFDO,
OpenCompanies, un bridge bancario, WorldGuard, LuckPerms.

## Configurazione

`config.yml` (toggle moduli, storage, time scale, finestre di scoperta), `goods.yml`,
`syndicate.yml`, `production.yml`, `traffic.yml`, `laundering.yml`, `racket.yml`,
`messages_it.yml` / `messages_en.yml`. Vedi `examples/` per `realistico-it` e `fantasy`.

## Cosa il core non fa mai

- Non contiene il nome di nessuna sostanza, organizzazione reale o metodo criminale
- Non espone mai automaticamente eventi alle FDO — nessuna notifica automatica, nessun heat
- Non punisce automaticamente: registra e abilita, mai esegue
- Non decide il valore di mercato: lo legge dall'adapter economy
- Non gestisce la violenza fisica — quella è RP libero
- Non crasha se un adapter manca: disattiva in silenzio la feature collegata
- Non assume WorldGuard — usa il `RegionAdapter` del core
