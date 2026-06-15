# Open Vending Machines

Languages: [English](README.en.md) | [Italiano](README.md)

An **adapter-first**, integration-friendly vending machine module for Paper roleplay servers, part of
the Open Roleplay family (sibling to Open Weapons). Java 21 · Maven · Paper 1.21.x.

Open Vending Machines gives you realistic, restockable, company-ownable vending machines — and
**hardcodes nothing** about economy, inventory or companies. Every external system is reached through
a small interface (an *adapter*) that ships with a working default and can be swapped at runtime from
your own plugin. Drop it in and it works out of the box; wire it to your existing systems without
forking the plugin.

---

## Table of contents

1. [Installation](#1-installation)
2. [Basic configuration](#2-basic-configuration)
3. [Creating a vending machine](#3-creating-a-vending-machine)
4. [Adding products](#4-adding-products)
5. [Configuring stock and prices](#5-configuring-stock-and-prices)
6. [Connecting a custom economy](#6-connecting-a-custom-economy)
7. [Connecting a custom inventory](#7-connecting-a-custom-inventory)
8. [Connecting a custom business/company system](#8-connecting-a-custom-businesscompany-system)
9. [Writing custom adapters](#9-writing-custom-adapters)
10. [Events](#10-events)
11. [Public API](#11-public-api)
12. [Full config example](#12-full-config-example)
13. [Security notes](#13-security-notes)
14. [Developer guide for other servers](#14-developer-guide-for-other-servers)
- [Commands & permissions](#commands--permissions)
- [Architecture](#architecture)
- [Building from source](#building-from-source)

---

## Why adapter-first?

The core never imports your economy, your inventory plugin or your company system. It depends only on
seven interfaces:

| Adapter | Responsibility | Bundled default |
|---|---|---|
| `EconomyAdapter` | charge buyers, pay out cash boxes (cash / bank / custom accounts) | in-memory demo wallet |
| `InventoryAdapter` | give/take items (buying, inventory restock) | real Bukkit inventories |
| `BusinessAdapter` | resolve companies, members, roles, limits, accounts | config-driven (`businesses` section) |
| `PermissionAdapter` | permission checks | Bukkit permission nodes |
| `StorageAdapter` | persist machines, stock, cash, state | single YAML file (or in-memory) |
| `NotificationAdapter` | deliver notifications | chat |
| `LoggingAdapter` | audit trail | file / console / none |

Replace any of them with one method call; the rest of the plugin is none the wiser. **Vault is not a
dependency** — an optional Vault economy adapter is included as an example and wired entirely through
reflection, so the core compiles and runs without Vault on the server.

---

## 1. Installation

**Requirements:** Paper 1.21.x, Java 21.

1. Build the jar (see [Building from source](#building-from-source)) or drop a release jar into your
   server's `plugins/` folder.
2. Start the server once. The plugin generates its data folder with:
   - `config.yml` — main settings and the demo company directory
   - `machines.yml` — machine types/models
   - `products.yml` — sellable products
   - `messages_en.yml`, `messages_it.yml` — translations (English + Italian, auto-selected by client locale)
3. Edit the files, then run `/ovm reload`.

That's it — with the bundled defaults the plugin is fully playable: players have a demo wallet, machines
persist to YAML, and the example `RedSpot Foods` company already exists.

## 2. Basic configuration

The most important `config.yml` knobs (see [section 12](#12-full-config-example) for the full file):

```yaml
adapters:
  economy: default     # default | vault
  storage: yaml        # yaml | memory
  business: config     # config (reads the businesses section)

economy:
  payment-account: cash    # which account buyers are charged from
  withdraw-account: bank   # where cash-outs go by default
  currency-symbol: "$"

purchase:
  max-interaction-distance: 6.0
  cooldown-millis: 750

restock:
  mode: player_inventory   # player_inventory | business_warehouse | free
  allow-price-editing: true

machines:
  default-company-limit: 5
  seed-full-on-create: true   # new machines start stocked (demo) vs empty (realistic)
```

Language: set `messages.language` to `auto`, `en` or `it`.

## 3. Creating a vending machine

1. Look at the block where the machine should stand.
2. Run `/ovm create <type> [company]`, e.g.:
   ```
   /ovm create snack-machine
   /ovm create drink-cooler redspot-foods
   ```
3. The targeted block becomes the machine. **Right-click** it to open the buy menu;
   **sneak + right-click** (as authorized staff) to open the management menu.

Machine blocks are protected from being broken — remove a machine with `/ovm remove` while looking at
it. Use `/ovm list` to see machines near you and `/ovm info` to inspect the one you are looking at.

Each machine has: a unique id, a position, a type/model, an optional owning company, its products,
per-product stock / capacity / price, an internal cash box, and a state
(`ACTIVE`, `EMPTY`, `BROKEN`, `DISABLED`).

### Choosing the look (texture / model)

A machine is bound to a **block location**. How it looks is up to you, controlled by two settings:

- **`machines.place-icon-block: true`** (default) — `/ovm create` turns the block you are looking at
  into the type's `icon` block (from machines.yml), and `/ovm remove` restores it to air. A **resource
  pack** can retexture that block/blockstate, so `icon` effectively *is* the texture selector. Place a
  throwaway block, look at it, `/ovm create` — it becomes the machine.
- **`interaction.furniture-entities: true`** (+ `place-icon-block: false`) — use a **furniture entity**
  as the model (ItemsAdder / Oraxen / Nexo furniture, or even a vanilla item frame / armor stand).
  Place the furniture, then `/ovm create` while looking at the block it sits on. Right-clicking the
  furniture entity opens the machine. No furniture-plugin dependency is required — the machine is
  matched by location.

The plugin ships no 3D models of its own on purpose: it adds the economy/stock/cash logic on top of
whatever block or furniture you choose.

## 4. Adding products

Products live in `products.yml`. Each entry describes the item a buyer receives plus default economics:

```yaml
energy-drink:
  display-name: "<green>Energy Drink</green>"   # MiniMessage
  material: DRAGON_BREATH
  amount: 1                # items handed over per unit bought
  price: 4.5               # default price (a machine may override per slot)
  max-stock: 24            # default capacity (a machine may override per slot)
  custom-model-data: 0     # optional, for resource-pack items
  lore:
    - "<gray>Restores stamina.</gray>"
```

Then reference the product id from a machine type in `machines.yml`:

```yaml
drink-cooler:
  display-name: "<aqua>Drink Cooler</aqua>"
  icon: BLAST_FURNACE        # the block players place & right-click, and the GUI icon
  slots: 9                   # max distinct products this type can hold
  default-products: [water, soda, energy-drink]
```

Run `/ovm reload` to apply. Use `/ovm giveitem <productId> [amount]` to get product items for testing
inventory-based restocking.

> Using a custom item plugin? You don't have to use `material`/`custom-model-data` — replace the
> `InventoryAdapter` and build/match items however you like, keyed off the product id (see
> [section 7](#7-connecting-a-custom-inventory)).

## 5. Configuring stock and prices

- **Defaults** come from `products.yml` (`price`, `max-stock`).
- **Per-machine** values are stored on each machine and can diverge from the defaults: prices can be
  edited in the management GUI (right-click a product: `+1`, shift-right-click: `-1`) when
  `restock.allow-price-editing: true`, or via the API `setPrice(...)`.
- **Stock** is changed by restocking (never directly trusted from a client). A machine flips to
  `EMPTY` automatically when every product hits zero, and back to `ACTIVE` when restocked.
- `machines.seed-full-on-create` controls whether new machines start full (demo) or empty (realistic).

Restocking has three modes (`restock.mode`):

| Mode | Behaviour |
|---|---|
| `player_inventory` | consumes real items from the staff member's inventory |
| `business_warehouse` | sources items through the `InventoryAdapter` — with the default adapter this consumes the staff inventory; plug in a warehouse-backed adapter to pull from company stock |
| `free` | no item cost; the management UI just sets the stock number (admin/demo) |

## 6. Connecting a custom economy

Implement `EconomyAdapter` and register it. The `account` argument lets you model cash, bank or any
custom account type; named accounts (e.g. a company treasury) are addressed by id.

```java
public final class MyEconomyAdapter implements EconomyAdapter {
    private final MyBank bank; // your system

    @Override public String id() { return "my-economy"; }

    @Override public double balance(OfflinePlayer player, String account) {
        return bank.balance(player.getUniqueId(), account);
    }
    @Override public boolean has(OfflinePlayer player, String account, double amount) {
        return bank.balance(player.getUniqueId(), account) >= amount;
    }
    @Override public boolean withdraw(OfflinePlayer player, String account, double amount) {
        return bank.tryWithdraw(player.getUniqueId(), account, amount); // atomic, false on failure
    }
    @Override public boolean deposit(OfflinePlayer player, String account, double amount) {
        return bank.deposit(player.getUniqueId(), account, amount);
    }
    @Override public double accountBalance(String accountId) { return bank.companyBalance(accountId); }
    @Override public boolean depositToAccount(String accountId, double amount) { return bank.companyDeposit(accountId, amount); }
    @Override public boolean withdrawFromAccount(String accountId, double amount) { return bank.companyWithdraw(accountId, amount); }
}
```

```java
OpenVendingMachinesApi api = Bukkit.getServicesManager().load(OpenVendingMachinesApi.class);
api.adapters().setEconomy(new MyEconomyAdapter());
```

> `withdraw`/`deposit` must be **atomic** and return `false` (with no side effect) if they cannot
> complete — the transaction services rely on that to stay duplication-free.

**Vault:** set `adapters.economy: vault` in `config.yml`. The bundled `VaultEconomyAdapter` (reflective)
activates if Vault and an economy provider are present, otherwise the plugin logs a warning and falls
back to the demo economy.

## 7. Connecting a custom inventory

Implement `InventoryAdapter` to give/take items through your own inventory plugin. You receive the full
`ProductDefinition`, so you can build or match items any way you like:

```java
public final class MyInventoryAdapter implements InventoryAdapter {
    @Override public String id() { return "my-inventory"; }

    @Override public boolean canReceive(Player player, ProductDefinition product, int amount) {
        return myInv.hasSpace(player, product.id(), amount);
    }
    @Override public boolean give(Player player, ProductDefinition product, int amount) {
        return myInv.give(player, product.id(), amount);   // all-or-nothing
    }
    @Override public boolean has(Player player, ProductDefinition product, int amount) {
        return myInv.count(player, product.id()) >= amount;
    }
    @Override public boolean take(Player player, ProductDefinition product, int amount) {
        return myInv.take(player, product.id(), amount);   // all-or-nothing
    }
}
```

```java
api.adapters().setInventory(new MyInventoryAdapter());
```

To source restocks from a **company warehouse** instead of the staff member's inventory, set
`restock.mode: business_warehouse` and implement `take(...)` to pull from the acting player's company
stock.

## 8. Connecting a custom business/company system

The core stores only a company *id* on each machine and asks the `BusinessAdapter` for everything else:

```java
public final class MyBusinessAdapter implements BusinessAdapter {
    @Override public String id() { return "my-companies"; }

    @Override public boolean companyExists(String companyId) { return companies.exists(companyId); }
    @Override public Optional<String> companyDisplayName(String companyId) { return companies.name(companyId); }
    @Override public boolean isMember(OfflinePlayer player, String companyId) { return companies.isMember(companyId, player.getUniqueId()); }
    @Override public Optional<String> roleOf(OfflinePlayer player, String companyId) { return companies.role(companyId, player.getUniqueId()); }

    @Override public boolean hasCapability(OfflinePlayer player, String companyId, BusinessCapability capability) {
        // Map your roles to USE / RESTOCK / WITHDRAW / EDIT_PRICE / MANAGE
        return companies.roleGrants(companyId, player.getUniqueId(), capability.name());
    }
    @Override public int machineLimit(String companyId) { return companies.machineLimit(companyId); } // -1 = unlimited
    @Override public Optional<String> companyAccount(String companyId) { return companies.treasuryAccount(companyId); }
}
```

```java
api.adapters().setBusiness(new MyBusinessAdapter());
```

The bundled `ConfigBusinessAdapter` reads the `businesses` section of `config.yml`, which is perfect for
small servers and demos — but swapping the adapter is how you integrate a real company plugin.

## 9. Writing custom adapters

Every adapter follows the same pattern: a tiny interface, a default implementation, and a setter on
`AdapterRegistry`. The full set:

```java
AdapterRegistry adapters = api.adapters();
adapters.setEconomy(myEconomy);          // EconomyAdapter
adapters.setInventory(myInventory);      // InventoryAdapter
adapters.setBusiness(myBusiness);        // BusinessAdapter
adapters.setPermission(myPermissions);   // PermissionAdapter  (e.g. LuckPerms contexts)
adapters.setStorage(myStorage);          // StorageAdapter     (e.g. SQLite/MySQL)
adapters.setNotification(myNotifier);    // NotificationAdapter (e.g. action bar / Discord)
adapters.setLogging(myLogger);           // LoggingAdapter     (e.g. database audit)
```

**Adding a SQL `StorageAdapter`** is a great example — the interface is CRUD-shaped on purpose:

```java
public final class SqliteStorageAdapter implements StorageAdapter {
    @Override public String id() { return "sqlite"; }
    @Override public void init() { /* open connection, CREATE TABLE IF NOT EXISTS ... */ }
    @Override public Collection<VendingMachine> loadAll() { /* SELECT * -> build VendingMachine list */ }
    @Override public void save(VendingMachine m) { /* single-row UPSERT (id, type, world, x, y, z, owner, cash, state) + products */ }
    @Override public void saveAll(Collection<VendingMachine> all) { all.forEach(this::save); }
    @Override public void delete(UUID id) { /* DELETE WHERE id = ? */ }
    @Override public void flush() { /* commit */ }
    @Override public void close() { /* close connection */ }
}
```

`VendingMachine` exposes everything you need to serialize: `id()`, `typeId()`, `location()`
(world + block x/y/z), `ownerCompanyId()`, `cashBalance()`, `state()`, and `products()` (each
`MachineProduct` has `productId()`, `price()`, `stock()`, `capacity()`).

> **Timing:** set your adapters in your plugin's `onEnable`, and add `depend: [OpenVendingMachines]`
> (or `softdepend`) to your `plugin.yml` so Open Vending Machines loads first. Adapters are preserved
> across `/ovm reload`, so you only register them once.

## 10. Events

All events are in `dev.openrp.vending.event`. Cancellable events fire *before* the
action; the success/fail pair are informational.

| Event | Cancellable | Fired when |
|---|---|---|
| `VendingMachineCreateEvent` | ✅ | before a machine is registered/persisted |
| `VendingMachineRemoveEvent` | ✅ | before a machine is removed |
| `VendingMachineRestockEvent` | ✅ | before stock is added (before items are taken) |
| `VendingMachinePurchaseAttemptEvent` | ✅ | after validation, before money/items move |
| `VendingMachinePurchaseSuccessEvent` | ❌ | after a completed sale |
| `VendingMachinePurchaseFailEvent` | ❌ | when a purchase is rejected (typed `PurchaseFailReason`) |
| `VendingMachineCashWithdrawEvent` | ✅ | before the cash box is emptied |
| `VendingMachineStateChangeEvent` | ✅ | before a state transition |

```java
@EventHandler
public void onSale(VendingMachinePurchaseSuccessEvent event) {
    getLogger().info(event.getPlayer().getName() + " bought " + event.getAmount()
            + "x " + event.getProductId() + " for " + event.getTotalPaid());
}

@EventHandler
public void onAttempt(VendingMachinePurchaseAttemptEvent event) {
    if (isBlacklisted(event.getProductId())) {
        event.setCancelled(true); // buyer gets a "denied" failure
    }
}
```

## 11. Public API

Retrieve the API from Bukkit's services manager:

```java
OpenVendingMachinesApi api = Bukkit.getServicesManager().load(OpenVendingMachinesApi.class);
```

### Adapters & hooks
```java
api.adapters();                 // AdapterRegistry – swap any integration
api.registerHook(myHook);       // influence decisions (see below)
api.unregisterHook(myHook);
```

### Queries (`api.machines()` → `VendingMachineService`)
```java
api.machines().all();
api.machines().byId(uuid);
api.machines().at(location);                 // machine on a block, or null
api.machines().nearby(location, 50.0);
api.machines().count();
api.machines().countOwnedBy("redspot-foods");
```

### Catalogue
```java
api.machineTypes(); api.machineType("snack-machine");
api.products();     api.product("water");
```

### Actions (same validated, locked paths the GUI/commands use)
```java
Optional<VendingMachine> m = api.createMachine(player, "snack-machine", location, "redspot-foods");
api.removeMachine(player, machine);
api.setState(machine, VendingMachineState.DISABLED);

PurchaseResult  p = api.purchase(player, machine, "water", 1);
RestockResult   r = api.restock(player, machine, "water", 16);
boolean         ok = api.setPrice(player, machine, "water", 2.0);
WithdrawResult  w = api.withdraw(player, machine);
```

### Callback hooks (`VendingHook`)

A lighter-weight alternative to events when you need to *influence* an action with a return value.
Every method has a permissive default — implement only what you need:

```java
api.registerHook(new VendingHook() {
    @Override
    public VendingDecision canPlayerUseMachine(Player player, VendingMachine machine) {
        return player.hasPermission("vip") ? VendingDecision.allow() : VendingDecision.deny("VIP only");
    }
    @Override
    public double resolveProductPrice(Player player, VendingMachine machine, MachineProduct product, double current) {
        return player.hasPermission("vip") ? current * 0.9 : current;   // 10% VIP discount
    }
    @Override
    public int resolveCompanyLimit(String companyId, int current) {
        return current + bonusMachines(companyId);
    }
    @Override public VendingDecision beforePayment(PurchaseContext ctx) { return VendingDecision.allow(); }
    @Override public void afterPayment(PurchaseContext ctx) { stats.record(ctx); }
});
```

Available callbacks: `canPlayerUseMachine`, `canPlayerRestockMachine`, `canPlayerWithdrawCash`,
`resolveProductPrice`, `resolveCompanyLimit`, `beforePayment`, `afterPayment`.

## 12. Full config example

```yaml
branding:
  product-name: "Open Vending Machines"
  message-prefix: "<gold>[Vending]</gold> "
  credit-line: "Open Vending Machines by Open Roleplay Workshop"

messages:
  language: auto   # auto | en | it
  fallback: en

adapters:
  economy: default        # default | vault
  inventory: bukkit
  business: config
  permission: bukkit
  storage: yaml           # yaml | memory
  notification: chat
  logging: file           # file | console | none

economy:
  payment-account: cash
  withdraw-account: bank
  currency-symbol: "$"
  demo-starting-balance: 1000.0

purchase:
  max-interaction-distance: 6.0
  cooldown-millis: 750
  require-line-of-sight: false
  fail-if-inventory-full: true

restock:
  mode: player_inventory   # player_inventory | business_warehouse | free
  max-per-action: 64
  allow-price-editing: true

cash:
  deposit-to-company-account: true
  log-withdrawals: true

machines:
  default-company-limit: 5
  random-breakdown-chance: 0.0
  seed-full-on-create: true
  place-icon-block: true        # /ovm create places the type's icon block; remove clears it

interaction:
  furniture-entities: false     # also open via right-click on furniture entities (ItemsAdder/Oraxen/Nexo)

storage:
  file: machines-data.yml

logging:
  file: transactions.log

debug: false

businesses:
  enabled: true
  companies:
    redspot-foods:
      display-name: "RedSpot Foods"
      machine-limit: 3
      account: "redspot-foods"
      members:
        "00000000-0000-0000-0000-000000000000": owner
      roles:
        owner: [use, restock, withdraw, edit_price, manage]
        manager: [use, restock, withdraw]
        clerk: [use, restock]
```

## 13. Security notes

All critical logic is **server-side**; the client is never trusted. Every purchase, restock and
withdrawal:

- **Validates** distance from the player, machine state, stock, price, permissions, company
  capabilities and the anti-spam cooldown — *before* anything changes.
- **Runs under a per-machine lock** (`MachineLocks`, reentrant), so two actors can never interleave on
  the same machine and duplicate items or money.
- **Moves money before items, and refunds on failure.** If item delivery fails after payment, the
  charge is refunded; if a cash-out's destination deposit fails, the cash box is restored. Value can
  never be created or lost.
- **Re-checks funds and space inside the lock** even after the up-front check, to close races.
- **Resolves prices server-side** (config + hooks), never from the GUI item.

The GUI and commands are thin front-ends: they only call the same validated services the API exposes,
so there is no privileged client path to exploit. Machine blocks are protected from being broken.

## 14. Developer guide for other servers

A typical integration plugin:

```java
public final class MyRpIntegration extends JavaPlugin {
    @Override
    public void onEnable() {
        OpenVendingMachinesApi api = getServer().getServicesManager().load(OpenVendingMachinesApi.class);
        if (api == null) {
            getLogger().severe("Open Vending Machines not found!");
            return;
        }
        // 1. Swap in your real systems
        api.adapters().setEconomy(new MyEconomyAdapter(myBank));
        api.adapters().setBusiness(new MyBusinessAdapter(myCompanies));
        api.adapters().setStorage(new SqliteStorageAdapter(dataSource));

        // 2. Influence decisions
        api.registerHook(new MyPricingHook());

        // 3. React to events (register a Listener as usual)
        getServer().getPluginManager().registerEvents(new MyVendingListener(), this);
    }
}
```

`plugin.yml`:
```yaml
name: MyRpIntegration
depend: [OpenVendingMachines]   # ensures load order so the API is ready in onEnable
```

Checklist:
- Add `depend: [OpenVendingMachines]` so it loads first.
- Set adapters in `onEnable` (they persist across `/ovm reload`).
- Map your company roles to the five `BusinessCapability` values.
- Make `EconomyAdapter` money moves atomic and `InventoryAdapter` give/take all-or-nothing.
- Want a custom UI? Implement `gui.VendingInterface` and call
  `((OpenVendingMachinesPlugin) Bukkit.getPluginManager().getPlugin("OpenVendingMachines")).setUserInterface(myUi)`.
  The core only ever calls `openPurchase` / `openManagement`.

To compile against the API, install this plugin to your local Maven repo (`mvn install`) and add it as
a `provided` dependency, or reference the jar directly.

---

## Commands & permissions

| Command | Description | Permission |
|---|---|---|
| `/ovm help` | list commands | — |
| `/ovm create <type> [company]` | place a machine where you are looking | `openvending.create` |
| `/ovm remove` | remove the machine you are looking at | `openvending.remove` |
| `/ovm list` | list nearby machines | `openvending.use` |
| `/ovm info` | inspect the targeted machine | `openvending.use` |
| `/ovm restock` | open the management/restock interface | `openvending.restock` |
| `/ovm withdraw` | cash out the targeted machine | `openvending.withdraw` |
| `/ovm reload` | reload configuration | `openvending.reload` |
| `/ovm giveitem <productId> [amount]` | give yourself a product item | `openvending.admin` |

Aliases: `/openvending`, `/ovm`, `/vending`.

| Permission | Default | Meaning |
|---|---|---|
| `openvending.*` | op | everything |
| `openvending.admin` | op | full control, bypasses company checks |
| `openvending.create` | op | place machines |
| `openvending.remove` | op | remove machines |
| `openvending.use` | true | buy from machines |
| `openvending.restock` | true | restock authorized machines |
| `openvending.withdraw` | true | withdraw authorized cash boxes |
| `openvending.reload` | op | reload config |

For owned machines, company capabilities (`RESTOCK`, `WITHDRAW`, `EDIT_PRICE`, `MANAGE`) are checked in
addition to the permission node; `openvending.admin` bypasses the company checks.

## Architecture

```
dev.openrp.vending
├── adapter          # the 7 integration interfaces + AdapterRegistry + BusinessCapability
│   ├── defaults     # bundled implementations (Bukkit, YAML, in-memory, config)
│   └── vault        # optional reflective Vault economy example (no compile dependency)
├── api              # OpenVendingMachinesApi (+ provider) and VendingMachineService
├── command          # /ovm command + tab completion
├── config           # machine type / product registries, typed settings, RestockMode
├── core             # manager + locked transaction services (purchase/restock/cash), locks, cooldown
├── event            # 8 Bukkit events + PurchaseFailReason
├── gui              # swappable VendingInterface + default chest GUI
├── hook             # VendingHook callback API + HookExecutor
├── listener         # block interaction / protection
├── message          # bilingual message + language services
└── model            # VendingMachine, MachineProduct, MachineType, ProductDefinition, state, location
```

The core depends on the *interfaces*, never on a concrete integration. The GUI and commands are
replaceable front-ends over the same services the API exposes.

## Building from source

```bash
mvn clean package
```

Produces `target/open-vending-machines-0.1.0-SNAPSHOT.jar`. Run `mvn test` for the unit test suite
(stock math, state transitions, hooks, cooldowns, locks, parsing — all server-free).

---

*Open Vending Machines — part of the Open Roleplay workshop. Adapter-first by design: bring your own
economy, inventory and companies.*
