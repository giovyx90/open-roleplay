# Open FDO

The **state's nervous system** for Paper roleplay servers. Adapter-first and
**setting-neutral**: the core knows no corps, rank, crime or short code. It knows only
abstract concepts — authority, act, dossier, evidence, conviction, wanted — and leaves
everything else to configuration and optional adapters.

> One rule: **if a player can do it physically in the world, the plugin does not do it for
> them.** OpenFDO *registers* what already happened, *enables* what Minecraft's physics
> cannot guarantee (timers, expiring warrants, cross-player notifications) and *executes*
> only where a 24/7 human operator would be needed (automatic release at end of sentence).

Apache 2.0. Part of the [Open Roleplay](https://github.com/giovyx90/open-roleplay) suite.

## Neutrality test

Every feature passes a test: *would it work identically on a medieval fantasy server?* If
not because of a setting assumption, it goes into **config**; if not because the concept
does not exist in that world, it goes behind an **optional adapter**. The Italian realistic
setting is just the reference configuration shipped as the default example — not the plugin.

## Three layers

1. **Core** (`dev.openrp.fdo`) — universal primitives: `Capability`, `Agent`, `Dossier`,
   `Evidence`, `CustodyEntry`, `Verdict`, `WantedEntry`, `DetentionOrder`. Never imports
   from the adapter layer.
2. **Config** — `corps.yml`, `ranks.yml`, `acts.yml`, `crimes.yml`, `wanted.yml`. This is
   where authorities, ranks, custody durations and the crime catalogue are born.
3. **Adapters** — optional interfaces to the outside world, discovered at runtime from the
   Bukkit ServicesManager. A capability tied to a missing adapter is silently disabled — no
   crash, no hard dependency.

## Capabilities

Acts are bound to **capabilities**, and capabilities are assigned to ranks from config.
Core catalogue: `DETAIN_TEMPORARY`, `ARREST`, `ADD_CHARGE`, `SEIZE_EVIDENCE`, `ISSUE_FINE`,
`FLAG_WANTED`, `OPEN_INVESTIGATION`, `REQUEST_WARRANT`, `ISSUE_WARRANT`, `ISSUE_VERDICT`,
`EXTEND_CUSTODY`, `ECONOMIC_AUDIT` (adapter), `IMPORT_EXTERNAL_RECORD` (adapter),
`MANAGE_DETENTION` (adapter), `DECLARE_ALERT`. Lower ranks' capabilities are inherited by
higher ones.

## Commands

- `/fdo` — identity: info, identifica, tesserino, servizio on/off, arruola, congeda,
  promuovi, degrada, reload.
- `/atto` — produce acts: `nuovo [type] [target]` (opens the picker or hands the book),
  `lista`.
- `/registro` — archives: fascicolo, lista, capo, wanted, servizio, prova.
- `/detenzione` — lista, info, rilascia, condanna, proroga (alias `/carcere`).
- `/allerta` — stato, dichiara, revoca.

The Bukkit permission (`openfdo.use`) only enables the command; real authority comes from
rank capabilities. `openfdo.admin` lets staff bypass capability checks.

## The act is a book

`/atto nuovo` shows only the acts the member's rank can produce and whose required adapter
is present. The member receives a writable book and **writes the content themselves** — the
plugin never composes it. On signing, the plugin stamps it (corps, rank, badge, date,
dossier id), registers the act and applies its effects (open a dossier, start custody,
seize evidence, flag wanted, ...).

## Dossier and chain of custody

A dossier has three sections: **A** heading (immutable), **B** body (charges, evidence,
notes, custody — mutable), **C** outcome (verdict, immutable once signed). Evidence keeps an
append-only **chain of custody**; a gap is what makes it contestable in roleplay.

## Adapters

`StorageAdapter`, `PermissionAdapter`, `NotificationAdapter`, `LoggingAdapter`,
`RegionAdapter` ship with working defaults. `DutyStatusAdapter` falls back to an internal
`/fdo servizio` toggle. `DetentionAdapter`, `EconomyAuditAdapter`, `ExternalRecordAdapter`,
`RadioAdapter` and `EvidenceSourceAdapter` are optional: absent, the matching capability and
act simply disappear, while a conviction is still recorded in the dossier.

## Public API

```java
OpenFdoApi api = Bukkit.getServicesManager().load(OpenFdoApi.class);
api.adapters().setDetention(myPrisonModule);   // plug in physical detention
api.reportEscape(inmate, "world", x, y, z);    // a prison module reports an escape
```

## Build

```bash
mvn -B -ntp package -pl open-fdo -am
```

## License

Apache License 2.0. See `LICENSE` at the repository root.
