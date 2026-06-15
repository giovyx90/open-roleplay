# Open Vending Machines

Lingue: [Italiano](README.md) | [English](README.en.md)

Modulo per distributori automatici roleplay su Paper, progettato con approccio
**adapter-first** e facile da integrare. Fa parte della famiglia Open Roleplay
insieme a Open Weapons. Richiede Java 21, Maven e Paper 1.21.x.

Open Vending Machines aggiunge distributori realistici, rifornibili e assegnabili
ad aziende, senza hardcodare economy, inventari o sistemi aziendali. Ogni sistema
esterno passa da una piccola interfaccia, cioe' un adapter, con un default
funzionante gia' incluso e sostituibile a runtime dal tuo plugin. Lo installi e
funziona subito; se hai sistemi proprietari, lo colleghi senza fare fork.

---

## Indice

1. [Installazione](#1-installazione)
2. [Configurazione base](#2-configurazione-base)
3. [Creare un distributore](#3-creare-un-distributore)
4. [Aggiungere prodotti](#4-aggiungere-prodotti)
5. [Configurare stock e prezzi](#5-configurare-stock-e-prezzi)
6. [Collegare una economy custom](#6-collegare-una-economy-custom)
7. [Collegare un inventario custom](#7-collegare-un-inventario-custom)
8. [Collegare un sistema aziende custom](#8-collegare-un-sistema-aziende-custom)
9. [Scrivere adapter custom](#9-scrivere-adapter-custom)
10. [Eventi](#10-eventi)
11. [API pubblica](#11-api-pubblica)
12. [Esempio completo di config](#12-esempio-completo-di-config)
13. [Note di sicurezza](#13-note-di-sicurezza)
14. [Guida sviluppatori per altri server](#14-guida-sviluppatori-per-altri-server)
- [Comandi e permessi](#comandi-e-permessi)
- [Architettura](#architettura)
- [Build da sorgente](#build-da-sorgente)

---

## Perche' adapter-first?

Il core non importa mai direttamente la tua economy, il tuo plugin inventario o
il tuo sistema aziende. Dipende solo da sette interfacce:

| Adapter | Responsabilita' | Default incluso |
|---|---|---|
| `EconomyAdapter` | addebita i compratori e versa gli incassi delle casse | wallet demo in memoria |
| `InventoryAdapter` | consegna/preleva item per acquisti e rifornimenti | inventari Bukkit reali |
| `BusinessAdapter` | risolve aziende, membri, ruoli, limiti e account | configurazione `businesses` |
| `PermissionAdapter` | controlli permessi | permission node Bukkit |
| `StorageAdapter` | persiste macchine, stock, cassa e stato | singolo file YAML o memoria |
| `NotificationAdapter` | invia notifiche | chat |
| `LoggingAdapter` | audit trail | file, console o nessuno |

Puoi sostituirne uno con una sola chiamata; il resto del plugin non deve saperlo.
**Vault non e' una dipendenza**: l'adapter Vault economy e' incluso come esempio
opzionale e viene agganciato via reflection, quindi il core compila e gira anche
senza Vault installato sul server.

---

## 1. Installazione

**Requisiti:** Paper 1.21.x, Java 21.

1. Compila il jar, vedi [Build da sorgente](#build-da-sorgente), oppure inserisci
   un jar di release nella cartella `plugins/` del server.
2. Avvia il server una volta. Il plugin genera la cartella dati con:
   - `config.yml`: impostazioni principali e directory aziende demo;
   - `machines.yml`: tipi/modelli dei distributori;
   - `products.yml`: prodotti vendibili;
   - `messages_en.yml`, `messages_it.yml`: traduzioni inglese e italiano,
     selezionate automaticamente in base alla lingua del client.
3. Modifica i file e poi esegui `/ovm reload`.

Con i default inclusi il plugin e' gia' giocabile: i player hanno un wallet demo,
le macchine persistono in YAML e l'azienda esempio `RedSpot Foods` esiste gia'.

## 2. Configurazione base

Le opzioni piu' importanti di `config.yml`, con il file completo nella
[sezione 12](#12-esempio-completo-di-config):

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

Lingua: imposta `messages.language` su `auto`, `en` o `it`.

## 3. Creare un distributore

1. Guarda il blocco dove vuoi posizionare il distributore.
2. Esegui `/ovm create <type> [company]`, per esempio:

```text
/ovm create snack-machine
/ovm create drink-cooler redspot-foods
```

3. Il blocco mirato diventa il distributore. **Click destro** apre il menu
   acquisto; **sneak + click destro**, se sei staff autorizzato, apre il menu di
   gestione.

I blocchi dei distributori sono protetti dalla rottura. Rimuovi un distributore
con `/ovm remove` mentre lo stai guardando. Usa `/ovm list` per vedere le
macchine vicine e `/ovm info` per ispezionare quella mirata.

Ogni macchina ha: id unico, posizione, tipo/modello, azienda proprietaria
opzionale, prodotti, stock/capacita/prezzo per prodotto, cassa interna e stato
(`ACTIVE`, `EMPTY`, `BROKEN`, `DISABLED`).

### Scegliere l'aspetto (texture / modello)

Una macchina e' legata a una **posizione blocco**. L'aspetto dipende da due
impostazioni:

- **`machines.place-icon-block: true`** (default): `/ovm create` trasforma il
  blocco mirato nel blocco `icon` del tipo scelto, letto da `machines.yml`, e
  `/ovm remove` lo ripristina ad aria. Un **resource pack** puo' ritexturare quel
  blocco/blockstate, quindi `icon` diventa di fatto il selettore texture. Piazza
  un blocco provvisorio, guardalo, esegui `/ovm create`: diventa la macchina.
- **`interaction.furniture-entities: true`** con `place-icon-block: false`: usa
  una **furniture entity** come modello, per esempio ItemsAdder, Oraxen, Nexo o
  anche un item frame/armor stand vanilla. Piazza il furniture, poi esegui
  `/ovm create` guardando il blocco su cui si trova. Il click destro sul
  furniture apre la macchina. Non serve dipendenza diretta da un plugin
  furniture: la macchina viene associata per posizione.

Il plugin non include modelli 3D propri intenzionalmente: aggiunge logica di
economy, stock e cassa sopra al blocco o furniture che scegli tu.

## 4. Aggiungere prodotti

I prodotti vivono in `products.yml`. Ogni voce descrive l'item ricevuto dal
compratore e i default economici:

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

Poi referenzia l'id prodotto da un tipo macchina in `machines.yml`:

```yaml
drink-cooler:
  display-name: "<aqua>Drink Cooler</aqua>"
  icon: BLAST_FURNACE
  slots: 9
  default-products: [water, soda, energy-drink]
```

Esegui `/ovm reload` per applicare le modifiche. Usa
`/ovm giveitem <productId> [amount]` per ottenere item prodotto durante i test di
rifornimento basato su inventario.

Se usi un plugin item custom non devi per forza usare `material` e
`custom-model-data`: sostituisci l'`InventoryAdapter` e costruisci/matcha gli
item come preferisci usando l'id prodotto, vedi
[sezione 7](#7-collegare-un-inventario-custom).

## 5. Configurare stock e prezzi

- I **default** arrivano da `products.yml` (`price`, `max-stock`).
- I valori **per macchina** vengono salvati sulla singola macchina e possono
  divergere dai default: i prezzi si modificano nella GUI gestione quando
  `restock.allow-price-editing: true`, con click destro `+1` e shift-click destro
  `-1`, oppure via API con `setPrice(...)`.
- Lo **stock** cambia tramite rifornimento e non viene mai fidato direttamente
  dal client. Una macchina passa automaticamente a `EMPTY` quando tutti i
  prodotti arrivano a zero, e torna `ACTIVE` dopo il rifornimento.
- `machines.seed-full-on-create` decide se le nuove macchine partono piene
  (demo) o vuote (realistico).

Modalita' di rifornimento (`restock.mode`):

| Modalita' | Comportamento |
|---|---|
| `player_inventory` | consuma item reali dall'inventario dello staff |
| `business_warehouse` | prende gli item tramite `InventoryAdapter`; col default consuma l'inventario dello staff, con un adapter warehouse puo' prelevare dallo stock aziendale |
| `free` | nessun costo item; la UI gestione imposta solo il numero stock, utile per admin/demo |

## 6. Collegare una economy custom

Implementa `EconomyAdapter` e registralo. L'argomento `account` ti permette di
modellare contanti, banca o qualsiasi tipo di conto custom; gli account nominati,
per esempio una tesoreria aziendale, vengono referenziati per id.

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

`withdraw` e `deposit` devono essere **atomici** e tornare `false`, senza effetti
collaterali, se non possono completare. I servizi transazionali dipendono da
questa regola per evitare duplicazioni.

**Vault:** imposta `adapters.economy: vault` in `config.yml`. Il
`VaultEconomyAdapter` incluso si attiva via reflection se Vault e un provider
economy sono presenti; altrimenti il plugin logga un warning e torna alla
economy demo.

## 7. Collegare un inventario custom

Implementa `InventoryAdapter` per dare/prelevare item tramite il tuo plugin
inventario. Ricevi il `ProductDefinition` completo, quindi puoi costruire o
riconoscere item come preferisci:

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

Per rifornire da un **magazzino aziendale** invece che dall'inventario dello
staff, imposta `restock.mode: business_warehouse` e implementa `take(...)` in
modo che prelevi dallo stock dell'azienda del player che agisce.

## 8. Collegare un sistema aziende custom

Il core salva solo l'*id azienda* su ogni macchina e chiede tutto il resto al
`BusinessAdapter`:

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

Il `ConfigBusinessAdapter` incluso legge la sezione `businesses` di
`config.yml`, perfetta per server piccoli e demo. Per integrare un plugin aziende
reale, sostituisci l'adapter.

## 9. Scrivere adapter custom

Ogni adapter segue lo stesso schema: interfaccia piccola, implementazione di
default e setter su `AdapterRegistry`.

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

Aggiungere uno `StorageAdapter` SQL e' un buon esempio: l'interfaccia e'
intenzionalmente CRUD.

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

`VendingMachine` espone tutto cio' che serve serializzare: `id()`, `typeId()`,
`location()` con mondo e coordinate blocco, `ownerCompanyId()`, `cashBalance()`,
`state()` e `products()`. Ogni `MachineProduct` contiene `productId()`,
`price()`, `stock()` e `capacity()`.

**Timing:** imposta gli adapter nell'`onEnable` del tuo plugin e aggiungi
`depend: [OpenVendingMachines]` o `softdepend` al tuo `plugin.yml` per caricare
Open Vending Machines prima. Gli adapter vengono preservati durante
`/ovm reload`, quindi li registri una sola volta.

## 10. Eventi

Tutti gli eventi sono in `dev.openrp.vending.event`. Gli eventi cancellabili
scattano *prima* dell'azione; la coppia success/fail e' solo informativa.

| Evento | Cancellabile | Quando scatta |
|---|---|---|
| `VendingMachineCreateEvent` | si | prima che una macchina venga registrata/persistita |
| `VendingMachineRemoveEvent` | si | prima che una macchina venga rimossa |
| `VendingMachineRestockEvent` | si | prima che lo stock venga aggiunto, quindi prima del prelievo item |
| `VendingMachinePurchaseAttemptEvent` | si | dopo la validazione, prima di muovere soldi/item |
| `VendingMachinePurchaseSuccessEvent` | no | dopo una vendita completata |
| `VendingMachinePurchaseFailEvent` | no | quando un acquisto viene rifiutato, con `PurchaseFailReason` tipizzata |
| `VendingMachineCashWithdrawEvent` | si | prima che la cassa venga svuotata |
| `VendingMachineStateChangeEvent` | si | prima di una transizione di stato |

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

## 11. API pubblica

Recupera l'API dal services manager Bukkit:

```java
OpenVendingMachinesApi api = Bukkit.getServicesManager().load(OpenVendingMachinesApi.class);
```

### Adapter e hook

```java
api.adapters();                 // AdapterRegistry - swap any integration
api.registerHook(myHook);       // influence decisions
api.unregisterHook(myHook);
```

### Query (`api.machines()` -> `VendingMachineService`)

```java
api.machines().all();
api.machines().byId(uuid);
api.machines().at(location);                 // machine on a block, or null
api.machines().nearby(location, 50.0);
api.machines().count();
api.machines().countOwnedBy("redspot-foods");
```

### Catalogo

```java
api.machineTypes(); api.machineType("snack-machine");
api.products();     api.product("water");
```

### Azioni

Sono gli stessi percorsi validati e lockati usati da GUI e comandi.

```java
Optional<VendingMachine> m = api.createMachine(player, "snack-machine", location, "redspot-foods");
api.removeMachine(player, machine);
api.setState(machine, VendingMachineState.DISABLED);

PurchaseResult  p = api.purchase(player, machine, "water", 1);
RestockResult   r = api.restock(player, machine, "water", 16);
boolean         ok = api.setPrice(player, machine, "water", 2.0);
WithdrawResult  w = api.withdraw(player, machine);
```

### Callback hook (`VendingHook`)

Alternativa piu' leggera agli eventi quando vuoi *influenzare* un'azione con un
valore di ritorno. Ogni metodo ha un default permissivo: implementa solo cio' che
ti serve.

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

Callback disponibili: `canPlayerUseMachine`, `canPlayerRestockMachine`,
`canPlayerWithdrawCash`, `resolveProductPrice`, `resolveCompanyLimit`,
`beforePayment`, `afterPayment`.

## 12. Esempio completo di config

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
  place-icon-block: true

interaction:
  furniture-entities: false

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

## 13. Note di sicurezza

Tutta la logica critica e' **server-side**; il client non viene mai fidato. Ogni
acquisto, rifornimento e prelievo:

- **Valida** distanza dal player, stato macchina, stock, prezzo, permessi,
  capability aziendali e cooldown anti-spam prima di cambiare qualsiasi cosa.
- **Esegue sotto lock per macchina** (`MachineLocks`, reentrant), quindi due
  attori non possono interleavare operazioni sulla stessa macchina e duplicare
  item o soldi.
- **Muove i soldi prima degli item e rimborsa in caso di fallimento.** Se la
  consegna item fallisce dopo il pagamento, l'addebito viene rimborsato; se il
  deposito di un cash-out fallisce, la cassa viene ripristinata. Il valore non
  puo' essere creato o perso.
- **Ricontrolla fondi e spazio dentro il lock** anche dopo il controllo iniziale,
  chiudendo race condition.
- **Risolva i prezzi server-side**, da config e hook, mai dall'item GUI.

GUI e comandi sono front-end sottili: chiamano solo gli stessi servizi validati
esposti dall'API, quindi non esiste un percorso client privilegiato da sfruttare.
I blocchi dei distributori sono protetti dalla rottura.

## 14. Guida sviluppatori per altri server

Un tipico plugin di integrazione:

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
depend: [OpenVendingMachines]
```

Checklist:

- Aggiungi `depend: [OpenVendingMachines]` per caricarlo prima.
- Imposta gli adapter in `onEnable`; persistono durante `/ovm reload`.
- Mappa i ruoli aziendali sui cinque valori `BusinessCapability`.
- Rendi atomici i movimenti di denaro di `EconomyAdapter` e all-or-nothing le
  operazioni give/take di `InventoryAdapter`.
- Vuoi una UI custom? Implementa `gui.VendingInterface` e chiama
  `((OpenVendingMachinesPlugin) Bukkit.getPluginManager().getPlugin("OpenVendingMachines")).setUserInterface(myUi)`.
  Il core chiama solo `openPurchase` e `openManagement`.

Per compilare contro l'API, installa questo plugin nel repo Maven locale con
`mvn install` e aggiungilo come dipendenza `provided`, oppure referenzia
direttamente il jar.

---

## Comandi e permessi

| Comando | Descrizione | Permesso |
|---|---|---|
| `/ovm help` | lista comandi | - |
| `/ovm create <type> [company]` | piazza una macchina dove stai guardando | `openvending.create` |
| `/ovm remove` | rimuove la macchina mirata | `openvending.remove` |
| `/ovm list` | lista macchine vicine | `openvending.use` |
| `/ovm info` | ispeziona la macchina mirata | `openvending.use` |
| `/ovm restock` | apre interfaccia gestione/rifornimento | `openvending.restock` |
| `/ovm withdraw` | preleva la cassa della macchina mirata | `openvending.withdraw` |
| `/ovm reload` | ricarica configurazione | `openvending.reload` |
| `/ovm giveitem <productId> [amount]` | consegna a te stesso un item prodotto | `openvending.admin` |

Alias: `/openvending`, `/ovm`, `/vending`.

| Permesso | Default | Significato |
|---|---|---|
| `openvending.*` | op | tutto |
| `openvending.admin` | op | controllo completo, bypass controlli azienda |
| `openvending.create` | op | piazzare macchine |
| `openvending.remove` | op | rimuovere macchine |
| `openvending.use` | true | acquistare dalle macchine |
| `openvending.restock` | true | rifornire macchine autorizzate |
| `openvending.withdraw` | true | prelevare casse autorizzate |
| `openvending.reload` | op | ricaricare config |

Per macchine possedute da aziende, le capability aziendali (`RESTOCK`,
`WITHDRAW`, `EDIT_PRICE`, `MANAGE`) vengono controllate oltre al permission node;
`openvending.admin` bypassa i controlli azienda.

## Architettura

```text
dev.openrp.vending
|-- adapter          # 7 integration interfaces + AdapterRegistry + BusinessCapability
|   |-- defaults     # implementazioni incluse (Bukkit, YAML, in-memory, config)
|   `-- vault        # esempio economy Vault opzionale via reflection
|-- api              # OpenVendingMachinesApi e VendingMachineService
|-- command          # comando /ovm + tab completion
|-- config           # registri tipi/prodotti, settings tipizzati, RestockMode
|-- core             # manager + servizi transazionali lockati, lock, cooldown
|-- event            # eventi Bukkit + PurchaseFailReason
|-- gui              # VendingInterface sostituibile + GUI chest default
|-- hook             # API callback VendingHook + HookExecutor
|-- listener         # interazione/protezione blocchi
|-- message          # servizi lingua e messaggi bilingui
`-- model            # VendingMachine, MachineProduct, MachineType, ProductDefinition, stato, posizione
```

Il core dipende dalle *interfacce*, mai da una integrazione concreta. GUI e
comandi sono front-end sostituibili sugli stessi servizi esposti dall'API.

## Build da sorgente

```bash
mvn clean package
```

Produce `target/open-vending-machines-0.1.0-SNAPSHOT.jar`. Esegui `mvn test` per
la suite unit test: calcolo stock, transizioni di stato, hook, cooldown, lock e
parsing, senza richiedere un server.

---

*Open Vending Machines fa parte dell'Open Roleplay workshop. Adapter-first per
scelta: porta la tua economy, il tuo inventario e le tue aziende.*
