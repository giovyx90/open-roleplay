# Open Jobs

The **basic-jobs engine** for Paper roleplay servers. Adapter-first and **setting-neutral**: the
core knows no job, material or location by name. It knows only abstract concepts — a worker doing a
physical activity at a location, producing something and being paid — and leaves everything else to
configuration and optional adapters.

> One rule: **the plugin does not pay the time, it pays the work actually done.** Standing idle in a
> mine is not working. The session tracks what you physically do — blocks broken, fish caught, items
> transformed — never the minutes spent in the region.

Apache 2.0. Part of the [Open Roleplay](https://github.com/giovyx90/open-roleplay) suite.

## The universal primitive of a basic job

A basic job is an activity anyone can do **without institutional mediation**: no exam, no hiring, no
company. Go, work, get paid. That is the line between Open Jobs and
[Open Companies](https://github.com/giovyx90/open-roleplay/tree/main/open-companies): Companies
runs structured firms with employees and contracts, Jobs the informal individual work. They do not
overlap — they coexist, and an employee can do a basic job as part of their role.

## Neutrality test

Every feature passes a test: *would it work identically on a medieval fantasy server?* "Miner" → yes
→ core (with config for name and resources). "A worker who extracts resources from a designated
location and is paid" → yes → core. The realistic Italian setting is just the reference config,
bundled as the default; see [`examples/fantasy`](examples/fantasy) for the opposite on the same code.

## Three payment models

| Model | How it pays |
| --- | --- |
| `a_produzione` | per produced unit (`payment.rates`), with a minimum below which it pays nothing. The most RP: pays real work, not time. |
| `a_sessione` | per effective active time (`rate_per_hour`), with a malus when activity drops below `activity_threshold` (`inactivity_penalty`). For work hard to measure. |
| `a_consegna` | nothing during extraction: pays per unit (`delivery_rates`) once the worker reaches the delivery point. Transport is physical and vulnerable. |

**Transformative** jobs (carpenter, smith, baker) pay per completed transformation on a designated
bench, with a `craft_time_seconds` floor that blocks abusive instant crafting.

On top of the base pay, the **progression**, **cooperative**, **tool**, **shift** and **season**
multipliers apply when configured.

## Session, progression, licences

A session pauses (clock stopped) when its worker leaves the region and resumes on return; staying out
too long abandons it (paid partially). Progression is **not XP**: the tier is derived from completed
sessions over time, per job, with optional inactivity decay. A licence is an identity, not red tape:
the DB record is authoritative, so a revoked licence is useless even if the item is still held, and a
lost item can be reissued.

## Adapters

Every adapter has a working default and is discovered at runtime via the Bukkit ServicesManager. When
absent or not real, the matching feature degrades silently — no crash, no hard dependency.

`StorageAdapter` (atomic YAML / `memory`) · `PermissionAdapter` (Bukkit) · `NotificationAdapter`
(chat) · `RegionAdapter` (per-chunk; a WorldGuard bridge adds real regions and delivery-point gating)
· `EconomyAdapter` (no-op; an Open Economy bridge delivers wages) · `CompanyEmploymentAdapter` (no-op;
an Open Companies bridge can redirect pay to the employer) · `IdentityAdapter` (no-op; an Open
Identity bridge makes licences physical items carrying the live tier).

## Commands

`/lavoro lista` · `info <job>` · `inizia` · `fine` · `stato` · `profilo` · `licenza`, plus the admin
subtree behind `openjobs.admin`: `admin licenza <emetti|revoca> <player> <job>`,
`admin sessione termina <player>`, `admin location <add|remove> ...`, `admin stats <player|job>`,
`admin reload`.

## Public API

Fetch `OpenJobsApi` from the Bukkit Services Manager. It exposes the job catalogue, a worker's active
session, lifetime records, licences and the live tier — the data the Open Gestionale widgets and an
Open Identity licence item read. Register your economy / company / identity / region adapter through
`api.adapters()`.

## What the module never does

Never pays the time (only real activity) · never hardcodes job/material/location names · never blocks
mining or crafting outside locations (they are normal Minecraft, just unpaid) · never handles careers
or company contracts (that is Open Companies) · never uses XP levels · never crashes when an adapter is
missing · never touches chat or prefixes · never assumes WorldGuard · never limits how many jobs a
player can hold, with separate progression for each.

---

*Open Jobs v1.0 — an Open Roleplay project. Apache 2.0.* · [Italiano](README.md)
