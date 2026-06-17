# Open Crime

The **organised-crime engine** for Paper roleplay servers. Adapter-first and
**setting-neutral**: the core knows no substance, real organisation or criminal method. It
only knows abstract concepts — illegal organisation, prohibited good, territory, crime
event, discovery — and leaves everything else to configuration and optional adapters.

> One rule: **the plugin never plays for the criminal.** It records that something illegal
> happened, enables mechanics Minecraft physics can't guarantee, and only executes what
> would otherwise need a 24/7 admin. Players build the org, take territory physically, and
> production needs real presence and real ingredients.

Apache 2.0. Part of the [Open Roleplay](https://github.com/giovyx90/open-roleplay) suite.

## RP First: no "heat", only discoveries

There is no hidden attention number that grows over time. The core is **opaque to the
authorities by default**: every crime event stays private until a **Discovery** — a concrete
RP action — links it to a dossier. If nobody acts, nothing happens. The five discovery types:

| Type | How it is born |
| --- | --- |
| `denuncia` | a civilian uses `/denuncia` next to an agent — both physically present, and a crime must have happened nearby recently |
| `scoperta_fisica` | an agent enters an active production location — notified only to them, only then |
| `arresto` | a member arrested carrying illegal goods links the org's recent events to the dossier |
| `informatore` | a member uses `/informatore` next to a capable agent and reveals what they know |
| `indagine` | an agent manually links existing discoveries (on the Open FDO side) |

An organisation with no discoveries **does not exist** to the authorities.

## Three-layer architecture

1. **Core** (`dev.openrp.crime`) — universal primitives: `IllegalOrg`, `Territory`,
   `IllegalGood`, `CrimeEvent`, `Discovery`. Never imports from the adapter layer.
2. **Config** — `goods.yml`, `syndicate.yml`, `production.yml`, `traffic.yml`,
   `laundering.yml`, `racket.yml`.
3. **Adapters** — world-facing interfaces, each with a default and discovered at runtime from
   the Bukkit ServicesManager. Missing one degrades the matching feature silently.

## Toggleable subsystems

Five subsystems, conceptually separate plugins depending on the core, shipped as one plugin
toggled in `config.yml` (`modules:`):

| Module | Command | What it does |
| --- | --- | --- |
| Open Syndicate | `/syndicate`, `/territory` | founding, hierarchy, recruiting, internal chat, territory |
| Open Production | `/produce` | multi-stage production of illegal goods at physical locations |
| Open Traffic | `/traffic` | shipments along physical, interceptable routes |
| Open Laundering | `/launder` | turns dirty money clean over time |
| Open Racket | `/racket` | extortion / protection over companies |

Plus the core commands `/denuncia`, `/informatore`, `/opencrime`.

## Adapters

| Adapter | Default | Absent / not real → |
| --- | --- | --- |
| `StorageAdapter` | atomic YAML with backup (or `memory`) | — |
| `PermissionAdapter` | Bukkit (reflects LuckPerms) | — |
| `NotificationAdapter` | chat / action bar | — |
| `RegionAdapter` | synthetic per-chunk (`available=false`) | with `territory.require_worldguard` off the plugin stays usable; a WorldGuard bridge brings real regions and tags |
| `EconomyAdapter` | internal ledger (dirty/clean) | an Open Bank bridge replaces it |
| `CompanyAdapter` | no-op (no companies) | racket degrades: `/racket imponi` finds none |
| `AuthorityAdapter` | permission-based (agents = perm node, synthetic dossier) | an Open FDO bridge opens real dossiers and links discoveries |

## Public API

Get `OpenCrimeApi` from the Bukkit ServicesManager. It is the central registry of
illegality, but the authorities read an org's events **only** via a `dossierId` that real
discoveries have linked.

```java
OpenCrimeApi api = Bukkit.getServicesManager().load(OpenCrimeApi.class);
api.adapters().setEconomy(myBankBridge);
List<CrimeEvent> visible = api.getDiscoveredEventsByOrg("clan", dossierId);
```

## Build

```bash
mvn -B -ntp -pl open-crime -am package
```

Java 21, Paper API 1.21.x. Optional soft deps: OpenCore, OpenFDO, OpenCompanies, a bank
bridge, WorldGuard, LuckPerms. See `examples/` for `realistico-it` and `fantasy`.
