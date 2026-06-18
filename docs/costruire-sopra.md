# Costruire sopra Open Roleplay

Open Roleplay è una base: ti dà le fondamenta e ti lascia costruirci sopra il tuo
mondo (vedi [`FILOSOFIA.md`](../FILOSOFIA.md)). Questa guida spiega *come* si
costruisce sopra, in concreto, senza fare il fork di niente.

Ci sono tre livelli di estensione, dal più semplice al più tecnico:

1. **Configurare** — riscrivere l'ambientazione e le regole in YAML.
2. **Scrivere un adapter** — collegare i tuoi sistemi (economia, storage, …).
3. **Consumare le API** — leggere lo stato dei moduli da un altro plugin.

Non serve toccare il codice di Open Roleplay per nessuno dei tre. Se ti ritrovi a
forkare il core, fermati: quasi sempre la cosa che ti serve è uno dei tre punti
qui sotto, oppure una mancanza da segnalare (vedi [`CONTRIBUTING.md`](../CONTRIBUTING.md)).

## 1. Configurare: il tuo mondo vive negli YAML

Il core non conosce nessuna ambientazione. Tutto ciò che è "editoriale" — i nomi
dei lavori, le risorse, i corpi dello stato, i reati, le organizzazioni, i prezzi —
vive nei file di configurazione del modulo, non nel codice.

Il modo giusto di iniziare è partire da un esempio e riscriverlo:

- Ogni modulo che lo prevede include `examples/realistico-it/` (la config di
  riferimento) ed `examples/fantasy/` (la stessa logica, ambientazione opposta).
- Confrontare i due esempi è il modo più rapido per capire cosa è *core* (uguale
  in entrambi) e cosa è *config* (diverso): tutto ciò che cambia tra `realistico-it`
  e `fantasy` è una leva che hai in mano anche tu.

Prima di scrivere codice, chiediti sempre se quello che vuoi è già una manopola di
config. Quasi sempre lo è.

## 2. Scrivere un adapter: collega i tuoi sistemi

I sistemi trasversali (economia, storage, permessi, regioni, identità, notifiche,
audit) passano da **adapter sostituibili**. Ogni adapter ha un default funzionante;
quelli rivolti al mondo (es. economia) partono con un default il cui
`available()` è `false`, così senza un backend reale la feature degrada in modo
pulito invece di crashare.

Per fornire la tua implementazione: registri l'API del modulo tramite il
`ServicesManager` di Bukkit e sostituisci l'adapter, tipicamente nell'`onEnable`
del *tuo* plugin.

```java
// Nel tuo plugin: collega la tua economia a Open Jobs.
public final class MyBridgePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        OpenJobsApi jobs = getServer().getServicesManager().load(OpenJobsApi.class);
        if (jobs == null) {
            getLogger().warning("Open Jobs non presente: bridge economia non attivo.");
            return;
        }
        jobs.adapters().setEconomy(new EconomyAdapter() {
            @Override public String id() { return "my-economy"; }
            @Override public boolean available() { return true; }
            @Override public boolean pay(UUID player, double amount, String reason) {
                // accredita amount al player tramite la TUA economia
                return MyEconomy.deposit(player, amount, reason);
            }
            @Override public boolean has(UUID player, double amount) {
                return MyEconomy.balance(player) >= amount;
            }
        });
    }
}
```

Regole d'oro dell'adapter:

- **Non bloccare l'avvio.** Se il modulo target non c'è (`load(...)` ritorna
  `null`), logga e prosegui. Il tuo bridge è opzionale per definizione.
- **`available()` deve dire la verità.** È così che il modulo decide se usare la
  via reale o il fallback (es. paga in item fisici invece che in denaro).
- **Niente I/O pesante sul main thread.** Vale ovunque in Bukkit, vale anche qui.

Le interfacce adapter di ogni modulo stanno in `*/adapter/` (es.
`open-jobs/src/main/java/dev/openrp/jobs/adapter/`). Sono il contratto: leggile da
lì, sono brevi e documentate.

## 3. Consumare le API: leggi lo stato dei moduli

Ogni modulo pubblicabile espone un'API stabile registrata sul `ServicesManager`:
`OpenJobsApi`, `OpenCompaniesApi`, `OpenFdoApi`, `OpenCrimeApi`,
`OpenVendingMachinesApi`, `OpenAccessApi`, `OpenCosmeticsApi`. Il core espone
`OpenRoleplayCore` (lifecycle dei moduli, database opzionale, HUD).

```java
OpenJobsApi jobs = getServer().getServicesManager().load(OpenJobsApi.class);
if (jobs != null) {
    jobs.getActiveSession(playerId).ifPresent(session -> {
        // mostra la sessione attiva nel tuo HUD, pannello, Discord bot, ...
    });
}
```

È così che, ad esempio, i widget di Open Gestionale leggono lo stato di gioco, o
che un item-licenza di un modulo identità legge `jobs.hasLicense(...)`. Tu puoi
fare lo stesso per qualunque integrazione: HUD custom, bot Discord, pannelli web,
statistiche.

## Aggiungere un modulo tuo

Se la tua estensione è grande e autonoma, falla diventare un **modulo a sé** che
vive accanto a Open Roleplay, non dentro:

1. È un normale plugin Paper. Dipende dalle API che gli servono (`open-core-api`
   e/o le API dei moduli) in `provided`/`compileOnly`, non le ricompila.
2. Se trova `OpenCore`, si registra come `OpenModule` così appare in
   `/opencore status`; se non lo trova, deve comunque avviarsi da solo.
3. Espone una sua API e i suoi adapter, seguendo gli stessi pilastri.
4. Supera il **test di neutralità**: se contiene scelte d'ambientazione cablate
   nel codice, spostale in config.

In pratica, un buon modulo di terze parti è indistinguibile da uno della suite:
standalone, adapter-first, neutro, leggibile.

## Checklist prima di pubblicare la tua estensione

- [ ] Parte da config: nessun nome/risorsa/ambientazione cablata nel codice.
- [ ] Non blocca l'avvio se un modulo opzionale manca.
- [ ] Usa API pubbliche e adapter, non fork del core.
- [ ] Niente I/O pesante sul main thread; torna sul main thread prima di API
      Bukkit non thread-safe.
- [ ] Niente segreti, asset o marchi che non puoi licenziare (vedi
      [`TRADEMARKS.md`](../TRADEMARKS.md)).

## Dove guardare

- Filosofia e confini del progetto: [`FILOSOFIA.md`](../FILOSOFIA.md)
- Contratti core condivisi: `open-core-api/`
- API e adapter di un modulo: `<modulo>/src/main/java/dev/openrp/<modulo>/api/` e
  `.../adapter/`
- Configurazioni di riferimento: `<modulo>/examples/realistico-it/` e
  `<modulo>/examples/fantasy/`
- Contribuire alla base: [`CONTRIBUTING.md`](../CONTRIBUTING.md)
