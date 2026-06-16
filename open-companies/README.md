# Open Companies

Lingue: [Italiano](README.md) | [English](README.en.md)

Modulo per **aziende, camera di commercio e asset aziendali** roleplay su Paper,
progettato con approccio **adapter-first** e facile da integrare. Fa parte della
famiglia Open Roleplay insieme a Open Vending Machines e Open Weapons. Richiede
Java 21, Maven e Paper 1.21.x.

Open Companies fornisce il **Company Core**: identità aziendale, ruoli, membri,
inviti, stato legale, licenze base e sede (HQ), più un registro di **asset
fisici** (terminali, POS, stampanti, casseforti, ...) a cui altri moduli possono
agganciarsi. Non contiene gameplay verticale: food, real estate, vending,
boutique, hospital ecc. usano l'**API/adapter** di questo modulo invece di
reimplementare il concetto di azienda.

Lo installi e funziona subito con i default YAML; se hai sistemi proprietari
(economy, permessi, regioni, identità) li colleghi senza fare fork.

---

## Indice

1. [Perché adapter-first?](#perché-adapter-first)
2. [Installazione](#1-installazione)
3. [Modalità di creazione](#2-modalità-di-creazione)
4. [Configurazione](#3-configurazione)
5. [Ruoli e capability](#4-ruoli-e-capability)
6. [Licenze, stato e sede](#5-licenze-stato-e-sede)
7. [Asset aziendali](#6-asset-aziendali)
8. [API pubblica](#7-api-pubblica)
9. [Scrivere adapter custom](#8-scrivere-adapter-custom)
10. [Integrazione con Open Vending Machines](#9-integrazione-con-open-vending-machines)
11. [Esempi: server piccolo vs server grande](#10-esempi-server-piccolo-vs-server-grande)
- [Comandi e permessi](#comandi-e-permessi)
- [Eventi](#eventi)
- [Architettura](#architettura)
- [Sicurezza e concorrenza](#sicurezza-e-concorrenza)
- [Build da sorgente](#build-da-sorgente)

---

## Perché adapter-first?

Il core non importa mai direttamente la tua economy, il tuo sistema permessi, il
tuo plugin regioni o il tuo sistema prefissi. Dipende solo da sette interfacce,
ognuna con un default funzionante già incluso e sostituibile a runtime:

| Adapter | Responsabilità | Default incluso |
|---|---|---|
| `StorageAdapter` | persiste aziende, asset e richieste | singolo file YAML o in memoria |
| `EconomyAdapter` | costo creazione e tesoreria aziendale | wallet demo in memoria |
| `PermissionAdapter` | controlli permessi | permission node Bukkit (riflette LuckPerms) |
| `RegionAdapter` | sede dentro un'area controllata | no-op (la HQ è solo una location salvata) |
| `IdentityAdapter` | prefix/suffix/tab aziendale | no-op (nessun prefisso imposto) |
| `NotificationAdapter` | notifiche ai membri | chat |
| `LoggingAdapter` | audit trail | file, console o nessuno |

Puoi sostituirne uno con una sola chiamata; il resto del plugin non deve saperlo.
**Vault, LuckPerms, WorldGuard e OpenVendingMachines non sono dipendenze**: sono
soft-dependency opzionali agganciate via reflection/guard, quindi il plugin
compila e si avvia anche senza nessuno di essi.

---

## 1. Installazione

1. Compila (vedi [Build da sorgente](#build-da-sorgente)) o scarica
   `open-companies-<versione>.jar`.
2. Mettilo in `plugins/`.
3. Avvia il server una volta: vengono generati `config.yml`, `messages_it.yml`
   e `messages_en.yml` nella cartella `plugins/OpenCompanies/`.
4. È già giocabile con i default. Personalizza `config.yml` e ricarica con
   `/company admin reload`.

---

## 2. Modalità di creazione

Una sola chiave di config decide **come nascono le aziende**, senza cambiare
codice:

```yaml
companies:
  creation:
    mode: PLAYER_DIRECT   # PLAYER_DIRECT | PLAYER_APPLICATION | ADMIN_ONLY
```

### PLAYER_DIRECT — i player fondano liberamente
- `/company create <nome> <tipo>` crea subito l'azienda; il player diventa CEO.
- Limiti anti-spam applicati: numero massimo di aziende per player, nome unico,
  cooldown di creazione ed eventuale costo tramite l'`EconomyAdapter`.
- Nessuno staff richiesto. Ideale per server open/community.

### PLAYER_APPLICATION — richiesta + approvazione
- `/company apply <nome> <tipo> [descrizione]` invia una richiesta.
- Lo staff usa `/company admin applications`, poi `approve <id>` o
  `deny <id> [motivo]`.
- All'approvazione l'azienda viene creata con il richiedente come CEO.

### ADMIN_ONLY — solo staff
- `/company create` per i player è disabilitato con un messaggio chiaro.
- Solo lo staff crea: `/company admin create <owner> <nome> <tipo>`; l'owner
  indicato diventa CEO.
- Replica il feeling "Camera di Commercio / Admin" con nomi Open Roleplay.

---

## 3. Configurazione

Estratto di `config.yml` (valori di default):

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

Lingua: `messages.language` accetta `auto` (locale del client), `it` o `en`; i
testi sono in `messages_it.yml` / `messages_en.yml` e usano MiniMessage dove
coerente con il resto della repo.

---

## 4. Ruoli e capability

Sei ruoli, ordinati per livello di autorità:

| Ruolo | Livello | Capability principali |
|---|---|---|
| `CEO` | 6 | tutte (implica `ADMIN`) |
| `DIRECTOR` | 5 | + `MANAGE_LICENSES`, `MANAGE_FINANCE`, `MANAGE_IDENTITY` |
| `VICE_DIRECTOR` | 4 | + `FIRE`, `CHANGE_ROLE` |
| `MANAGER` | 3 | + `INVITE`, `MANAGE_ASSETS` |
| `EMPLOYEE` | 2 | `VIEW`, `USE_ASSETS` |
| `TRAINING` | 1 | `VIEW`, `USE_ASSETS` |

Le capability disponibili sono: `VIEW`, `INVITE`, `FIRE`, `CHANGE_ROLE`,
`MANAGE_IDENTITY`, `MANAGE_LICENSES`, `MANAGE_ASSETS`, `USE_ASSETS`,
`MANAGE_FINANCE`, `ADMIN`. Ogni operazione sensibile chiede
`role.grants(capability)`: per cambiare la "struttura di potere" basta modificare
la mappatura ruolo→capability in `CompanyRole`, in un punto solo.

---

## 5. Licenze, stato e sede

- **Stato** (`CompanyStatus`): `ACTIVE`, `SUSPENDED`, `DISSOLVED`. Solo le aziende
  `ACTIVE` possono operare (assumere, usare asset). Gestito da
  `/company admin setstatus`.
- **Licenze** (`CompanyLicenseType`): `GENERAL_BUSINESS`, `FOOD_SERVICE`,
  `PRIVATE_SECURITY`, `TRANSPORT`, `BANKING`, `MEDIA`, `REAL_ESTATE`,
  `MANUFACTURING`, `PRIVATE_HEALTHCARE`. Concesse/revocate dalla camera con
  `/company admin license grant|revoke`. I moduli verticali fanno gating sul
  proprio gameplay con `chamber().hasLicense(companyId, type)`.
- **Sede (HQ)**: `/company admin sethq <azienda>` salva la posizione dell'admin.
  Con il `RegionAdapter` di default la HQ è solo una location; un adapter
  WorldGuard può richiedere che stia dentro un'area controllata.

---

## 6. Asset aziendali

Il modulo tiene un registro di asset fisici, senza imporne il comportamento:

`COMPANY_TERMINAL`, `HOLOGRAM_PROJECTOR`, `LED_PANEL`, `PRINTER`, `POS`,
`CASH_REGISTER`, `STORAGE`, `SAFE`, `BADGE_READER`, `RECEPTION_KIOSK`,
`PRODUCT_DISPLAY`.

Di ogni asset il core memorizza solo posizione, azienda proprietaria, tipo e una
mappa `metadata` libera. Ogni tipo ha un ruolo minimo di **uso** e di
**gestione**. I moduli verticali risolvono l'asset con
`assets().assetAt(world, x, y, z)` e implementano la logica (un POS funzionante,
una stampante che produce documenti, un pannello LED che mostra testo, ...).

> La complessità grafica (PC/LED/ologrammi) non è inclusa qui di proposito:
> modello, API e storage sono pronti perché i moduli futuri si aggancino.

---

## 7. API pubblica

Recupera l'API dal services manager di Bukkit:

```java
OpenCompaniesApi api = Bukkit.getServicesManager().load(OpenCompaniesApi.class);

// Creare un'azienda (flusso admin/API): l'owner diventa CEO
CompanyResult result = api.companies().createCompany(ownerUuid, "Owner", "Red Spot Foods", "food");
result.company().ifPresent(c -> getLogger().info("Creata " + c.id()));

// Controllo capability
boolean canFire = api.companies().hasCapability(playerUuid, "red-spot-foods", CompanyCapability.FIRE);

// Camera di commercio
api.chamber().grantLicense("red-spot-foods", CompanyLicenseType.FOOD_SERVICE);
boolean ok = api.chamber().hasLicense("red-spot-foods", CompanyLicenseType.FOOD_SERVICE);

// Asset
api.assets().registerAsset("red-spot-foods", staffUuid, CompanyAssetType.POS, "world", 10, 64, -3);
```

Tre servizi più il registro adapter:

- `CompanyService companies()` — `createCompany`, `createCompanyForPlayer`,
  `deleteCompany`, `findById`, `findByName`, `findByPlayer`, `allCompanies`,
  `inviteMember`, `acceptInvite`, `denyInvite`, `removeMember`, `changeRole`,
  `transferOwnership`, `hasCapability`.
- `ChamberService chamber()` — `submitApplication`, `approveApplication`,
  `denyApplication`, `grantLicense`, `revokeLicense`, `setStatus`,
  `setHeadquarters`, `hasLicense`.
- `CompanyAssetService assets()` — `registerAsset`, `removeAsset`, `assetsOf`,
  `assetAt`, `canUseAsset`, `canManageAsset`.
- `AdapterRegistry adapters()` — sostituisci qui qualsiasi adapter.

Ogni metodo che muta dati passa dallo **stesso percorso validato e lockato** dei
comandi e restituisce un `CompanyResult` (esito, chiave messaggio, payload
opzionale), quindi chiamarlo da codice è sicuro quanto digitare il comando.

---

## 8. Scrivere adapter custom

Implementa l'interfaccia e registrala (di solito nel tuo `onEnable`, dopo che
Open Companies si è caricato):

```java
public final class MyEconomyAdapter implements EconomyAdapter { /* ... */ }

OpenCompaniesApi api = Bukkit.getServicesManager().load(OpenCompaniesApi.class);
api.adapters().setEconomy(new MyEconomyAdapter());
```

Gli adapter vengono creati una volta sola all'avvio e **sopravvivono a
`/company reload`** (solo config, messaggi e dati persistiti vengono riletti),
così le integrazioni di altri plugin non vengono perse.

- **LuckPerms**: non serve un adapter dedicato; i permission node Bukkit
  riflettono già i gruppi/nodi LuckPerms. Per prefissi aziendali in tab/chat
  fornisci un `IdentityAdapter`.
- **WorldGuard**: fornisci un `RegionAdapter` che validi le coordinate della HQ;
  il default no-op accetta qualsiasi posizione.
- **SQL**: implementa `StorageAdapter` con upsert/delete per riga; le operazioni
  pesanti vanno eseguite in modo asincrono nel tuo adapter.

---

## 9. Integrazione con Open Vending Machines

Open Vending Machines espone un proprio `BusinessAdapter`. Quando entrambi i
plugin sono presenti, Open Companies registra automaticamente un
`OpenCompaniesBusinessAdapter`, così i distributori possono appartenere alle
aziende e rispettarne i ruoli. Mappatura capability vending → livello ruolo:

| Capability vending | Ruolo minimo |
|---|---|
| `USE` | Employee+ |
| `RESTOCK` | Employee+ oppure Manager+ (`integration.vending.restock-requires-manager`) |
| `WITHDRAW` | Manager+ |
| `EDIT_PRICE` | Manager+ |
| `MANAGE` | Director+ |

Per collegarli:

1. Installa entrambi i plugin (l'ordine non conta: `softdepend`).
2. Tieni `integration.vending.enabled: true` in `config.yml`.
3. In Open Vending Machines imposta `adapters.business` per usare l'azienda; in
   `/ovm create <tipo> <companyId>` usa l'`id` dell'azienda (lo slug, es.
   `red-spot-foods`).

Se Open Vending Machines non è installato non succede nulla: nessuna dipendenza
forte, il modulo si avvia comunque.

---

## 10. Esempi: server piccolo vs server grande

**Server piccolo / demo**

```yaml
adapters: { economy: default, storage: yaml, logging: console }
companies:
  creation: { mode: PLAYER_DIRECT, max-owned-per-player: 1, creation-cost: 0.0 }
```
Tutto in memoria/YAML, i player fondano aziende liberamente, zero dipendenze.

**Server serio / produzione**

```yaml
adapters: { economy: vault, storage: yaml, logging: file }
companies:
  creation: { mode: PLAYER_APPLICATION, max-owned-per-player: 3, creation-cost: 2500.0, cooldown-seconds: 600 }
```
Economy Vault, audit su file, aziende per approvazione staff, costo e cooldown.
Per database/identità custom fornisci i tuoi `StorageAdapter`/`IdentityAdapter`
(es. SQL + LuckPerms) a runtime via API.

---

## Comandi e permessi

Comando principale `/company` — alias: `/opencompanies`, `/ocompanies`,
`/companies`.

| Comando | Descrizione | Permesso |
|---|---|---|
| `/company help` | lista comandi | `opencompanies.use` |
| `/company create <nome> <tipo>` | fonda un'azienda (modalità PLAYER_DIRECT) | `opencompanies.create` |
| `/company apply <nome> <tipo> [descr.]` | invia una richiesta (PLAYER_APPLICATION) | `opencompanies.apply` |
| `/company list` | elenca le aziende | `opencompanies.use` |
| `/company info [azienda]` | dettagli di un'azienda | `opencompanies.use` |
| `/company members` | membri della tua azienda | `opencompanies.use` |
| `/company invite <player> <ruolo>` | invita un membro | `opencompanies.invite` |
| `/company invite accept` \| `deny` | accetta/rifiuta un invito | `opencompanies.use` |
| `/company fire <player>` | licenzia un membro | `opencompanies.fire` |
| `/company role <player> <ruolo>` | cambia ruolo | `opencompanies.role` |
| `/company leave` | lascia l'azienda | `opencompanies.use` |
| `/company licenses` | licenze dell'azienda | `opencompanies.use` |
| `/company assets` | asset dell'azienda | `opencompanies.assets` |

Comandi staff (`/company admin ...`):

| Comando | Permesso |
|---|---|
| `admin create <owner> <nome> <tipo>` | `opencompanies.create.admin` |
| `admin delete <azienda>` | `opencompanies.delete` |
| `admin setstatus <azienda> <ACTIVE\|SUSPENDED\|DISSOLVED>` | `opencompanies.admin` |
| `admin setowner <azienda> <player>` | `opencompanies.admin` |
| `admin license grant\|revoke <azienda> <licenza>` | `opencompanies.admin` |
| `admin sethq <azienda>` | `opencompanies.admin` |
| `admin applications` / `approve <id>` / `deny <id> [motivo]` | `opencompanies.admin` |
| `admin reload` | `opencompanies.reload` |

| Permesso | Default | Significato |
|---|---|---|
| `opencompanies.*` | op | tutto |
| `opencompanies.use` | true | usare il comando e azioni read-only |
| `opencompanies.create` | true | fondare un'azienda (solo in PLAYER_DIRECT) |
| `opencompanies.apply` | true | inviare richieste (solo in PLAYER_APPLICATION) |
| `opencompanies.manage` | true | gestire la propria azienda |
| `opencompanies.invite` | true | invitare membri |
| `opencompanies.fire` | true | rimuovere membri |
| `opencompanies.role` | true | cambiare ruoli |
| `opencompanies.assets` | true | registrare/usare asset |
| `opencompanies.admin` | op | controllo completo, bypassa i controlli di ruolo |
| `opencompanies.create.admin` | op | creare aziende per altri |
| `opencompanies.delete` | op | eliminare aziende |
| `opencompanies.reload` | op | ricaricare config |

Il permission node abilita il *comando*; l'autorizzazione reale dentro l'azienda
è data dal **ruolo** (capability). La modalità di creazione fa da gate
aggiuntivo: in `ADMIN_ONLY` anche chi ha `opencompanies.create` riceve un
messaggio di rifiuto.

---

## Eventi

Eventi Bukkit per agganciarsi al ciclo di vita (i primi tre sono cancellabili):

`CompanyCreateEvent`, `CompanyDeleteEvent`, `CompanyMemberInviteEvent`,
`CompanyMemberJoinEvent`, `CompanyMemberLeaveEvent`, `CompanyRoleChangeEvent`,
`CompanyStatusChangeEvent`, `CompanyLicenseChangeEvent`,
`CompanyHeadquartersChangeEvent`.

---

## Architettura

```text
dev.openrp.companies
|-- adapter          # 7 interfacce integrazione + AdapterRegistry
|   |-- defaults     # implementazioni incluse (Bukkit, YAML, in-memory, no-op)
|   `-- vault        # esempio economy Vault opzionale via reflection
|-- api              # OpenCompaniesApi + CompanyService/ChamberService/CompanyAssetService
|-- command          # comando /company + tab completion
|-- config           # CompaniesSettings tipizzato + CreationMode
|-- core             # CompanyManager/AssetManager (puri) + servizi lockati, validator, lock, result
|-- event            # eventi Bukkit del ciclo di vita azienda
|-- integration      # registrazione OpenCore (reflection) + adapter business Open Vending
|-- message          # servizi lingua e messaggi bilingui
`-- model            # Company, CompanyMember, ruoli, capability, stato, licenze, asset, applicazione
```

Il cuore logico (`CompanyManager`, `AssetManager`, `CompanyValidator`) è **puro
Java**, senza Bukkit: regole, modalità, limiti, invite/accept e cambi ruolo sono
unit-testabili direttamente. I `Default*Service` aggiungono lock, eventi,
economy, notifiche, identità e audit. Il core dipende dalle *interfacce*, mai da
una integrazione concreta.

---

## Sicurezza e concorrenza

- Tutte le operazioni critiche sono server-side; la GUI/comando non è mai fonte
  di verità. Vengono sempre validati permesso, ruolo, stato azienda, limiti di
  config, nome, esistenza di player/azienda e cooldown.
- Ogni mutazione di membri, ruoli, stato, licenze, sede o asset gira dentro un
  **lock per `companyId`** (`CompanyLocks`), così due attori non si
  sovrappongono sulla stessa azienda.
- Il costo di creazione viene prima addebitato e **rimborsato** se la creazione
  fallisce o un evento la annulla, evitando duplicazioni di denaro.
- Le API Bukkit (eventi, notifiche) vengono usate sul main thread; uno
  `StorageAdapter` SQL può spostare l'I/O pesante in async al suo interno.

---

## Build da sorgente

```bash
# tutta la suite
mvn clean package

# solo questo modulo (con le sue dipendenze)
mvn -B -ntp -pl open-companies -am package
```

Il jar viene prodotto in `open-companies/target/open-companies-<versione>.jar`.
Richiede Java 21. Le classi di Open Vending Machines hanno scope `provided`:
servono solo a compilare l'integrazione opzionale e **non** vengono incluse nel
jar, quindi il plugin si avvia anche senza quel modulo.
