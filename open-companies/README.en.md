# Open Companies

Languages: [Italiano](README.md) | [English](README.en.md)

A roleplay module for **companies, a chamber of commerce and company assets** on
Paper, built **adapter-first** and easy to integrate. Part of the Open Roleplay
family alongside Open Vending Machines and Open Weapons. Requires Java 21, Maven
and Paper 1.21.x.

Open Companies provides the **Company Core**: company identity, roles, members,
invitations, legal status, base licenses and a headquarters (HQ), plus a registry
of **physical assets** (terminals, POS, printers, safes, ...) other modules can
hook into. It contains no vertical gameplay: food, real estate, vending,
boutique, hospital, etc. use this module's **API/adapters** instead of
re-implementing the notion of a company.

Install it and it works out of the box with YAML defaults; if you run proprietary
systems (economy, permissions, regions, identity) you wire them in without
forking.

---

## Table of contents

1. [Why adapter-first?](#why-adapter-first)
2. [Installation](#1-installation)
3. [Creation modes](#2-creation-modes)
4. [Configuration](#3-configuration)
5. [Roles and capabilities](#4-roles-and-capabilities)
6. [Licenses, status and HQ](#5-licenses-status-and-hq)
7. [Company assets](#6-company-assets)
8. [Public API](#7-public-api)
9. [Writing custom adapters](#8-writing-custom-adapters)
10. [Open Vending Machines integration](#9-open-vending-machines-integration)
11. [Examples: small vs large server](#10-examples-small-vs-large-server)
- [Commands and permissions](#commands-and-permissions)
- [Events](#events)
- [Architecture](#architecture)
- [Security and concurrency](#security-and-concurrency)
- [Build from source](#build-from-source)

---

## Why adapter-first?

The core never imports your economy, permission system, region plugin or prefix
system directly. It depends on just seven interfaces, each with a working default
included and replaceable at runtime:

| Adapter | Responsibility | Bundled default |
|---|---|---|
| `StorageAdapter` | persist companies, assets and applications | single YAML file or in-memory |
| `EconomyAdapter` | creation fee and company treasury | in-memory demo wallet |
| `PermissionAdapter` | permission checks | Bukkit permission nodes (reflects LuckPerms) |
| `RegionAdapter` | HQ inside a controlled region | no-op (HQ is just a saved location) |
| `IdentityAdapter` | company prefix/suffix/tab | no-op (no prefix imposed) |
| `NotificationAdapter` | notify members | chat |
| `LoggingAdapter` | audit trail | file, console or none |

You replace one with a single call; the rest of the plugin never knows.
**Vault, LuckPerms, WorldGuard and OpenVendingMachines are not dependencies**:
they are optional soft-dependencies wired via reflection/guards, so the plugin
compiles and starts without any of them.

---

## 1. Installation

1. Build it (see [Build from source](#build-from-source)) or download
   `open-companies-<version>.jar`.
2. Drop it into `plugins/`.
3. Start the server once: `config.yml`, `messages_it.yml` and `messages_en.yml`
   are generated under `plugins/OpenCompanies/`.
4. It is playable with defaults. Edit `config.yml` and reload with
   `/company admin reload`.

---

## 2. Creation modes

A single config key decides **how companies come into existence**, with no code
changes:

```yaml
companies:
  creation:
    mode: PLAYER_DIRECT   # PLAYER_DIRECT | PLAYER_APPLICATION | ADMIN_ONLY
```

### PLAYER_DIRECT — players found companies freely
- `/company create <name> <type>` creates the company immediately; the player
  becomes CEO.
- Anti-spam limits apply: max companies per player, unique name, creation
  cooldown and an optional fee through the `EconomyAdapter`.
- No staff required. Ideal for open/community servers.

### PLAYER_APPLICATION — request + approval
- `/company apply <name> <type> [description]` submits a request.
- Staff use `/company admin applications`, then `approve <id>` or
  `deny <id> [reason]`.
- On approval the company is created with the applicant as CEO.

### ADMIN_ONLY — staff only
- `/company create` is disabled for players with a clear message.
- Only staff create: `/company admin create <owner> <name> <type>`; the named
  owner becomes CEO.
- Reproduces the "Chamber of Commerce / Admin" feel with Open Roleplay naming.

---

## 3. Configuration

Excerpt of `config.yml` (default values):

```yaml
adapters:
  economy: default     # default | vault
  storage: yaml        # yaml | memory
  region: noop
  identity: noop
  logging: file        # file | console | none

economy:
  account: cash
  currency-symbol: "$"
  demo-starting-balance: 1000.0

companies:
  creation:
    mode: PLAYER_DIRECT
    max-owned-per-player: 1
    max-members-per-company: 30
    creation-cost: 0.0
    cooldown-seconds: 60
    allowed-types: [food, retail, media, transport, real_estate, healthcare, security, manufacturing, generic]
    name:
      min-length: 3
      max-length: 32
      allowed-regex: "^[A-Za-z0-9 _-]+$"
      reserved: [admin, staff, police, government, chamber]

integration:
  vending:
    enabled: true
    restock-requires-manager: false
    default-machine-limit: -1
```

Language: `messages.language` accepts `auto` (client locale), `it` or `en`; text
lives in `messages_it.yml` / `messages_en.yml` and uses MiniMessage where
consistent with the rest of the repo.

---

## 4. Roles and capabilities

Six roles, ordered by authority level:

| Role | Level | Key capabilities |
|---|---|---|
| `CEO` | 6 | all (implies `ADMIN`) |
| `DIRECTOR` | 5 | + `MANAGE_LICENSES`, `MANAGE_FINANCE`, `MANAGE_IDENTITY` |
| `VICE_DIRECTOR` | 4 | + `FIRE`, `CHANGE_ROLE` |
| `MANAGER` | 3 | + `INVITE`, `MANAGE_ASSETS` |
| `EMPLOYEE` | 2 | `VIEW`, `USE_ASSETS` |
| `TRAINING` | 1 | `VIEW`, `USE_ASSETS` |

Available capabilities: `VIEW`, `INVITE`, `FIRE`, `CHANGE_ROLE`,
`MANAGE_IDENTITY`, `MANAGE_LICENSES`, `MANAGE_ASSETS`, `USE_ASSETS`,
`MANAGE_FINANCE`, `ADMIN`. Every sensitive operation asks
`role.grants(capability)`: to change the power structure you only edit the
role→capability mapping in `CompanyRole`, in one place.

---

## 5. Licenses, status and HQ

- **Status** (`CompanyStatus`): `ACTIVE`, `SUSPENDED`, `DISSOLVED`. Only `ACTIVE`
  companies may operate (hire, use assets). Managed by
  `/company admin setstatus`.
- **Licenses** (`CompanyLicenseType`): `GENERAL_BUSINESS`, `FOOD_SERVICE`,
  `PRIVATE_SECURITY`, `TRANSPORT`, `BANKING`, `MEDIA`, `REAL_ESTATE`,
  `MANUFACTURING`, `PRIVATE_HEALTHCARE`. Granted/revoked by the chamber with
  `/company admin license grant|revoke`. Vertical modules gate their gameplay on
  `chamber().hasLicense(companyId, type)`.
- **Headquarters (HQ)**: `/company admin sethq <company>` saves the admin's
  position. With the default `RegionAdapter` the HQ is just a location; a
  WorldGuard adapter can require it to sit inside a controlled region.

---

## 6. Company assets

The module keeps a registry of physical assets without imposing their behaviour:

`COMPANY_TERMINAL`, `HOLOGRAM_PROJECTOR`, `LED_PANEL`, `PRINTER`, `POS`,
`CASH_REGISTER`, `STORAGE`, `SAFE`, `BADGE_READER`, `RECEPTION_KIOSK`,
`PRODUCT_DISPLAY`.

For each asset the core stores only its position, owning company, type and a free
`metadata` map. Each type carries a minimum **use** and **manage** role. Vertical
modules resolve an asset with `assets().assetAt(world, x, y, z)` and implement the
behaviour (a working POS, a printer producing documents, an LED panel rendering
text, ...).

> The heavy graphics (PC/LED/holograms) are intentionally out of scope here: the
> model, API and storage are ready for future modules to hook in.

---

## 7. Public API

Retrieve the API from Bukkit's services manager:

```java
OpenCompaniesApi api = Bukkit.getServicesManager().load(OpenCompaniesApi.class);

// Create a company (admin/API flow): the owner becomes CEO
CompanyResult result = api.companies().createCompany(ownerUuid, "Owner", "Red Spot Foods", "food");
result.company().ifPresent(c -> getLogger().info("Created " + c.id()));

// Capability check
boolean canFire = api.companies().hasCapability(playerUuid, "red-spot-foods", CompanyCapability.FIRE);

// Chamber of commerce
api.chamber().grantLicense("red-spot-foods", CompanyLicenseType.FOOD_SERVICE);
boolean ok = api.chamber().hasLicense("red-spot-foods", CompanyLicenseType.FOOD_SERVICE);

// Assets
api.assets().registerAsset("red-spot-foods", staffUuid, CompanyAssetType.POS, "world", 10, 64, -3);
```

Three services plus the adapter registry:

- `CompanyService companies()` — `createCompany`, `createCompanyForPlayer`,
  `deleteCompany`, `findById`, `findByName`, `findByPlayer`, `allCompanies`,
  `inviteMember`, `acceptInvite`, `denyInvite`, `removeMember`, `changeRole`,
  `transferOwnership`, `hasCapability`.
- `ChamberService chamber()` — `submitApplication`, `approveApplication`,
  `denyApplication`, `grantLicense`, `revokeLicense`, `setStatus`,
  `setHeadquarters`, `hasLicense`.
- `CompanyAssetService assets()` — `registerAsset`, `removeAsset`, `assetsOf`,
  `assetAt`, `canUseAsset`, `canManageAsset`.
- `AdapterRegistry adapters()` — swap any adapter here.

Every mutating method runs the **same validated, locked path** the commands use
and returns a `CompanyResult` (outcome, message key, optional payload), so calling
it from code is as safe as typing the command.

---

## 8. Writing custom adapters

Implement the interface and register it (usually in your own `onEnable`, after
Open Companies has loaded):

```java
public final class MyEconomyAdapter implements EconomyAdapter { /* ... */ }

OpenCompaniesApi api = Bukkit.getServicesManager().load(OpenCompaniesApi.class);
api.adapters().setEconomy(new MyEconomyAdapter());
```

Adapters are created once on enable and **survive `/company reload`** (only
config, messages and persisted data are re-read), so integrations registered by
other plugins are never lost.

- **LuckPerms**: no dedicated adapter needed; Bukkit permission nodes already
  reflect LuckPerms groups/nodes. For company prefixes in tab/chat provide an
  `IdentityAdapter`.
- **WorldGuard**: provide a `RegionAdapter` that validates HQ coordinates; the
  no-op default accepts any position.
- **SQL**: implement `StorageAdapter` with per-row upsert/delete; do heavy work
  asynchronously inside your adapter.

---

## 9. Open Vending Machines integration

Open Vending Machines exposes its own `BusinessAdapter`. When both plugins are
present, Open Companies automatically registers an `OpenCompaniesBusinessAdapter`
so machines can belong to companies and respect company roles. Vending capability
→ minimum role mapping:

| Vending capability | Minimum role |
|---|---|
| `USE` | Employee+ |
| `RESTOCK` | Employee+ or Manager+ (`integration.vending.restock-requires-manager`) |
| `WITHDRAW` | Manager+ |
| `EDIT_PRICE` | Manager+ |
| `MANAGE` | Director+ |

To connect them:

1. Install both plugins (order does not matter: `softdepend`).
2. Keep `integration.vending.enabled: true` in `config.yml`.
3. In Open Vending Machines use the company `id` (the slug, e.g.
   `red-spot-foods`) when assigning a machine to a company.

If Open Vending Machines is not installed nothing happens: no hard dependency,
the module still starts.

---

## 10. Examples: small vs large server

**Small / demo server**

```yaml
adapters: { economy: default, storage: yaml, logging: console }
companies:
  creation: { mode: PLAYER_DIRECT, max-owned-per-player: 1, creation-cost: 0.0 }
```
Everything in memory/YAML, players found companies freely, zero dependencies.

**Serious / production server**

```yaml
adapters: { economy: vault, storage: yaml, logging: file }
companies:
  creation: { mode: PLAYER_APPLICATION, max-owned-per-player: 3, creation-cost: 2500.0, cooldown-seconds: 600 }
```
Vault economy, file audit, staff-approved companies, fee and cooldown. For custom
database/identity provide your own `StorageAdapter`/`IdentityAdapter` (e.g. SQL +
LuckPerms) at runtime via the API.

---

## Commands and permissions

Main command `/company` — aliases: `/opencompanies`, `/ocompanies`,
`/companies`.

| Command | Description | Permission |
|---|---|---|
| `/company help` | command list | `opencompanies.use` |
| `/company create <name> <type>` | found a company (PLAYER_DIRECT mode) | `opencompanies.create` |
| `/company apply <name> <type> [desc]` | submit an application (PLAYER_APPLICATION) | `opencompanies.apply` |
| `/company list` | list companies | `opencompanies.use` |
| `/company info [company]` | a company's details | `opencompanies.use` |
| `/company members` | your company's members | `opencompanies.use` |
| `/company invite <player> <role>` | invite a member | `opencompanies.invite` |
| `/company invite accept` \| `deny` | accept/deny an invite | `opencompanies.use` |
| `/company fire <player>` | remove a member | `opencompanies.fire` |
| `/company role <player> <role>` | change a role | `opencompanies.role` |
| `/company leave` | leave the company | `opencompanies.use` |
| `/company licenses` | the company's licenses | `opencompanies.use` |
| `/company assets` | the company's assets | `opencompanies.assets` |

Staff commands (`/company admin ...`):

| Command | Permission |
|---|---|
| `admin create <owner> <name> <type>` | `opencompanies.create.admin` |
| `admin delete <company>` | `opencompanies.delete` |
| `admin setstatus <company> <ACTIVE\|SUSPENDED\|DISSOLVED>` | `opencompanies.admin` |
| `admin setowner <company> <player>` | `opencompanies.admin` |
| `admin license grant\|revoke <company> <license>` | `opencompanies.admin` |
| `admin sethq <company>` | `opencompanies.admin` |
| `admin applications` / `approve <id>` / `deny <id> [reason]` | `opencompanies.admin` |
| `admin reload` | `opencompanies.reload` |

| Permission | Default | Meaning |
|---|---|---|
| `opencompanies.*` | op | everything |
| `opencompanies.use` | true | use the command and read-only actions |
| `opencompanies.create` | true | found a company (PLAYER_DIRECT only) |
| `opencompanies.apply` | true | submit applications (PLAYER_APPLICATION only) |
| `opencompanies.manage` | true | manage your own company |
| `opencompanies.invite` | true | invite members |
| `opencompanies.fire` | true | remove members |
| `opencompanies.role` | true | change roles |
| `opencompanies.assets` | true | register/use assets |
| `opencompanies.admin` | op | full control, bypasses role checks |
| `opencompanies.create.admin` | op | create companies for others |
| `opencompanies.delete` | op | delete companies |
| `opencompanies.reload` | op | reload config |

The permission node enables the *command*; the real authorization inside a
company comes from the **role** (capabilities). The creation mode is an extra
gate: in `ADMIN_ONLY`, even a player with `opencompanies.create` gets a refusal
message.

---

## Events

Bukkit events to hook the lifecycle (the first three are cancellable):

`CompanyCreateEvent`, `CompanyDeleteEvent`, `CompanyMemberInviteEvent`,
`CompanyMemberJoinEvent`, `CompanyMemberLeaveEvent`, `CompanyRoleChangeEvent`,
`CompanyStatusChangeEvent`, `CompanyLicenseChangeEvent`,
`CompanyHeadquartersChangeEvent`.

---

## Architecture

```text
dev.openrp.companies
|-- adapter          # 7 integration interfaces + AdapterRegistry
|   |-- defaults     # bundled implementations (Bukkit, YAML, in-memory, no-op)
|   `-- vault        # optional Vault economy example via reflection
|-- api              # OpenCompaniesApi + CompanyService/ChamberService/CompanyAssetService
|-- command          # /company command + tab completion
|-- config           # typed CompaniesSettings + CreationMode
|-- core             # pure CompanyManager/AssetManager + locked services, validator, lock, result
|-- event            # company lifecycle Bukkit events
|-- integration      # OpenCore registration (reflection) + Open Vending business adapter
|-- message          # bilingual language and message services
`-- model            # Company, CompanyMember, roles, capabilities, status, licenses, asset, application
```

The logic core (`CompanyManager`, `AssetManager`, `CompanyValidator`) is **pure
Java**, no Bukkit: rules, modes, limits, invite/accept and role changes are
directly unit-testable. The `Default*Service` classes add locking, events,
economy, notifications, identity and audit. The core depends on the *interfaces*,
never on a concrete integration.

---

## Security and concurrency

- All critical operations are server-side; the GUI/command is never the source of
  truth. Permission, role, company status, config limits, name, player/company
  existence and cooldown are always validated.
- Every mutation of members, roles, status, licenses, HQ or assets runs inside a
  **per-`companyId` lock** (`CompanyLocks`), so two actors cannot interleave on
  the same company.
- The creation fee is charged first and **refunded** if creation fails or an
  event cancels it, preventing money duplication.
- Bukkit APIs (events, notifications) are used on the main thread; an SQL
  `StorageAdapter` can move heavy I/O off-thread internally.

---

## Build from source

```bash
# whole suite
mvn clean package

# this module only (with its dependencies)
mvn -B -ntp -pl open-companies -am package
```

The jar is produced at `open-companies/target/open-companies-<version>.jar`.
Requires Java 21. Open Vending Machines classes are `provided` scope: they only
serve to compile the optional integration and are **not** bundled into the jar,
so the plugin starts without that module.
