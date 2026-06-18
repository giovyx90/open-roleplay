# Open Politics

The **political engine** for Paper roleplay servers. Adapter-first and **setting-neutral**: the core
knows no Mayor, King or Senate. It knows only abstract primitives — a charge with a holder, a
government that assigns it, a signed act, a registered law — and leaves everything else to config and
optional adapters.

> One rule: **the plugin certifies, it does not govern.** Open Politics never executes the
> consequences of a political decision. It certifies that the decision was taken by whoever had the
> authority to take it, records it, and exposes it through the API. The consequences are RP — players
> live them, not the plugin.

A law that bans weapons downtown does **not** disable weapons downtown. It is a document signed by the
competent authority that players and the police use as a narrative reference. Apache 2.0. Part of the
[Open Roleplay](https://github.com/giovyx90/open-roleplay) suite.

## The universal primitive of politics

> A system of government is a set of **charges**, a **mechanism** to assign them, and a system to
> produce **binding decisions**.

The core does not know what a Mayor is. It knows charges exist, charges have holders, holders produce
acts, and acts become laws. What all of that is *called* lives in config.

## Neutrality test

Every feature passes one test: *would it work identically on a medieval fantasy server?* "Mayor" → no
→ config. "Decree" → no → config. "Democratic election" → no → config (it is a type of mechanism).
"A charge holder producing a signed act" → yes → core. "A mechanism for assigning a charge" → yes →
core. The realistic Italian municipality is just the reference config shipped as the default; see
[`examples/fantasy`](examples/fantasy) (a Kingdom) and [`examples/oligarchia`](examples/oligarchia) (a
Council of Five) for opposite settings on the same code.

## The four areas

Not separate sub-modules: aspects of the same system, toggled in `config.yml`.

| Area | Question | What it does |
| --- | --- | --- |
| **Charges** | What you are | Institutional roles with authority, holders, term and capabilities. |
| **Government** | How you got there | A set of charges with an assignment mechanism. |
| **Acts** | What you sign | Official documents produced by a holder of the right capability. |
| **Laws** | What you decided | Acts that cleared their iter: a public registry, never executed. |

## Political capabilities

Like Open FDO, the core defines *what* each capability unlocks; config decides *which charge holds it*.
A capability never produces an automatic effect: it enables a command and certifies authority.

`SIGN_ACT` · `SIGN_LAW` · `APPOINT` · `REMOVE` · `DISSOLVE` · `DECLARE_EMERGENCY` · `MANAGE_BUDGET` ·
`REVOKE_LICENSE` · `CALL_ELECTION` · `VETO`

## The four assignment mechanisms

| Mechanism | How the charge is obtained |
| --- | --- |
| `election` | Players vote. The plugin runs the campaign, candidacies, ballots, the result and the automatic assignment to the winner(s). |
| `appointment` | A unilateral appointment by a holder of `APPOINT`. |
| `hereditary` | The charge passes to the successor designated by the holder (`/politica successore`). |
| `conquest` | It belongs to whoever physically controls a region (via `RegionAdapter`, e.g. WorldGuard). |

Election and conquest are the **only** cases where the plugin assigns a charge automatically.
Everything else is signed by a holder.

## The legislative iter

An act of a type with `requires_vote` is submitted to a **collegiate body** (a charge with
`max_holders > 1`): quorum and majority decide whether it carries. If the type allows `veto_allowed`, a
**veto window** opens for a holder of `VETO`. Once the iter is complete and the type is
`can_become_law`, the act is **promulgated** into a law. The plugin **tracks** the iter, it never
forces it: a skipped step is recorded, not punished.

Active laws are public (`/politica leggi`); repealed ones stay in the **historical archive**
(`/politica archivio`), so an RP judge can apply the law that was in force when a fact occurred, even
after repeal.

## Commands

`openpolitics.use` enables the commands; `openpolitics.admin` unlocks `/politica admin`. Real authority
is **not** a permission node — it is the charge you hold.

| Command | Function |
| --- | --- |
| `/politica cariche` · `carica <id>` | active charges with holders; a charge's detail |
| `/politica leggi` · `legge <id>` · `archivio` | public registry, a law's text, historical archive |
| `/politica atti` · `governo` | recent acts; active government structure |
| `/politica atto <type> <title>` | sign an act (produces a stamped book) |
| `/politica nomina <player> <charge>` · `rimuovi` | appoint/remove holders (`APPOINT`/`REMOVE`) |
| `/politica veto <act_id>` · `vota <act_id> <yes\|no\|abstain>` | veto; collegiate ballot |
| `/politica emergenza <dichiara\|revoca>` | state of emergency (Open FDO bridge) |
| `/politica elezioni indici <charge>` · `successore <player>` · `abroga <law_id>` | call election; designate heir; repeal |
| `/voto lista` · `candidatura <id>` · `<id> <player>` · `risultati <id>` | election participation |
| `/politica admin …` · `/openpolitics <status\|reload>` | staff management, status and reload |

## Adapters

Each adapter has a working default and is discovered at runtime via the Bukkit ServicesManager. Absent
or not real, the linked feature degrades silently — no crash, no hard dependency.

| Adapter | Default | Absent / not real → |
| --- | --- | --- |
| `StorageAdapter` | atomic YAML with backup (or `memory`) | — |
| `PermissionAdapter` | Bukkit (reflects LuckPerms) | — |
| `NotificationAdapter` | chat / broadcast | — |
| `EconomyAdapter` | no-op | an Open Economy bridge exposes the public budget to `MANAGE_BUDGET` holders |
| `CompanyAdapter` | no-op | an Open Companies bridge recognises licence revocations by `REVOKE_LICENSE` holders |
| `IdentityAdapter` | no-op | an Open Identity bridge turns charges into a physical ID card that decays at term end |
| `RegionAdapter` | no-op | a WorldGuard bridge enables the `conquest` mechanism |
| `AuthorityAdapter` | no-op | an Open FDO bridge receives emergency declarations |

## Public API

Retrieve `OpenPoliticsApi` from the Bukkit Services Manager. It is the server's institutional registry:
who holds which charge, what they may do, what they signed and — above all for other modules — the
public law registry.

```java
OpenPoliticsApi api = Bukkit.getServicesManager().load(OpenPoliticsApi.class);

// Open FDO: attach violated laws to a dossier and apply the law of the time of the fact
List<Law> active = api.getActiveLaws("comune");
boolean inForce = api.wasActiveDuring(lawId, factMoment);

// Who can raise the alert? (Open FDO asks to know who triggers the state of emergency)
List<String> charges = api.chargesWithCapability("comune", PoliticalCapability.DECLARE_EMERGENCY);

api.adapters().setIdentity(myIdentityBridge);
```

## What the module never does

- Holds no charge, government, act-type or law-category name: all in config.
- Never executes the consequences of a law — it certifies and exposes them.
- Never forces the legislative iter — it tracks it and flags whether it was respected.
- Never assigns charges automatically, except in the `election` and `conquest` mechanisms.
- Never handles political violence or coups: that is free RP.
- Never exposes election ballots when `anonymous_voting: true`.
- Never crashes when an adapter is missing: it silently disables the linked capability.

## Build

```bash
mvn -pl open-politics -am package
```

Requires Java 21 and Paper 1.21.x. The JAR lands in `target/`. OpenCore is optional: the plugin is
fully standalone.
