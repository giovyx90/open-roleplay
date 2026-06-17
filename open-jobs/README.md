# Open Jobs

Il **motore dei lavori base** per server roleplay su Paper. Adapter-first e
**neutro rispetto all'ambientazione**: il core non conosce nessun lavoro, materiale o location
reale. Conosce solo concetti astratti — un lavoratore che svolge un'attività fisica in una
location, produce qualcosa e viene compensato — e lascia tutto il resto alla configurazione e
ad adapter opzionali.

> Regola unica: **il plugin non paga il tempo, paga l'attività reale.** Stare fermi in miniera
> non è lavorare. La sessione traccia ciò che fai fisicamente — blocchi estratti, pesci pescati,
> item trasformati — non i minuti trascorsi nella region.

Apache 2.0. Parte della suite [Open Roleplay](https://github.com/giovyx90/open-roleplay).

## Il primitivo universale del lavoro base

Un lavoro base è un'attività accessibile a chiunque, **senza mediazione istituzionale**: niente
concorso, niente assunzione, niente azienda. Vai, lavori, guadagni. È questa la distinzione tra
Open Jobs e [Open Companies](https://github.com/giovyx90/open-roleplay/tree/main/open-companies):
Companies gestisce imprese strutturate con dipendenti e contratti, Jobs il lavoro individuale
informale — il minatore che scende in miniera, il boscaiolo che va al bosco. I due moduli non si
sovrappongono: coesistono, e un dipendente può svolgere un lavoro base come parte del suo ruolo.

## Test di neutralità

Ogni feature passa un test: *funzionerebbe identica su un server fantasy medievale?* "Minatore"
→ sì → core (con config per nome e risorse). "Raccoglitore di adamantio" → no → config.
"Lavoratore che estrae risorse da una location designata e riceve una paga" → sì → core. Il core
non sa cosa si mina, si taglia o si pesca; sa che un giocatore svolge un'attività in una location
e viene compensato. Il realistico italiano è solo la configurazione di riferimento, inclusa come
default; vedi [`examples/fantasy`](examples/fantasy) per un'ambientazione opposta sullo stesso codice.

## Tre modelli di pagamento

| Modello | Come paga |
| --- | --- |
| `a_produzione` | per unità di materiale valido prodotta nella sessione (`payment.rates`), con una soglia minima sotto la quale non paga nulla. Il più RP: paga il lavoro reale, non il tempo. |
| `a_sessione` | per durata effettiva attiva (`rate_per_hour`), con un malus se l'attività rilevata scende sotto la soglia (`activity_threshold` → `inactivity_penalty`). Adatto a lavori difficili da misurare. |
| `a_consegna` | niente durante l'estrazione: si paga per unità (`delivery_rates`) quando il lavoratore raggiunge fisicamente il punto di consegna. Il trasporto è fisico e vulnerabile. |

I lavori **trasformativi** (falegname, fabbro, fornaio) pagano per ogni trasformazione completata
su un banco designato, con un `craft_time_seconds` che impedisce il crafting istantaneo abusivo.

Sul pagamento base si applicano, quando configurati, i moltiplicatori di **progressione**,
**cooperativa**, **strumento**, **turno** e **stagione**.

## Sessione: inizia, lavora, finisci

`/lavoro inizia` parte solo se sei nella region di una location di lavoro, hai la licenza (se
richiesta), c'è capienza e non hai già una sessione attiva. Durante la sessione il core ascolta
gli eventi Bukkit del tipo di location e registra ogni azione valida. Se esci dalla region la
sessione va **in pausa** (l'orologio si ferma) e riprende al rientro; se resti fuori troppo a
lungo viene **abbandonata** e pagata parzialmente. `/lavoro fine` chiude la sessione, calcola il
payout ed eroga via adapter.

## Progressione per anzianità

Niente XP: il **grado** deriva dal numero di **sessioni reali completate nel tempo**. Un giocatore
con cento sessioni in tre mesi è esperto perché è presente e costante. I tier sono configurabili
in `progression.yml` e specifici per ogni lavoro — un mastro boscaiolo è un novizio minatore. Il
**decadimento** opzionale erode lentamente l'anzianità di chi sparisce per mesi, ma si recupera
semplicemente tornando a lavorare.

## Licenze professionali

La licenza non è burocrazia: è un'identità, il documento che riconosce un giocatore come
praticante di un mestiere. Con `requires_license: false` chiunque può iniziare. Con
`auto_issue: true` la licenza viene emessa alla prima sessione; altrimenti la rilascia un admin
(`/lavoro admin licenza emetti`). Il record nel DB è autoritativo: una licenza revocata è inutile
anche con l'item in mano, e un item perso si può riemettere. Con un Identity adapter la licenza
diventa un item NBT che mostra il grado corrente.

## Adapter

Ogni adapter ha un default funzionante ed è scoperto a runtime dal Bukkit ServicesManager. Assente
o non reale, la feature collegata degrada in silenzio — nessun crash, nessuna dipendenza dura.

| Adapter | Default | Assente / non reale → |
| --- | --- | --- |
| `StorageAdapter` | YAML atomico con backup (o `memory`) | — |
| `PermissionAdapter` | Bukkit (riflette LuckPerms) | — |
| `NotificationAdapter` | chat / action bar | — |
| `RegionAdapter` | sintetico per-chunk | con `require_region_backend` off resta usabile; un bridge WorldGuard porta regioni e tag reali e abilita il gating del punto di consegna |
| `EconomyAdapter` | no-op (`available()=false`) | un bridge Open Economy eroga le paghe; senza, la paga è registrata nel record e notificata |
| `CompanyEmploymentAdapter` | no-op | un bridge Open Companies può reindirizzare la paga al datore di lavoro |
| `IdentityAdapter` | no-op | un bridge Open Identity rende le licenze item fisici con il grado inciso |

## Comandi

Il permesso `openjobs.use` abilita l'uso del comando; `openjobs.admin` sblocca il sottocomando
`admin`.

| Comando | Funzione |
| --- | --- |
| `/lavoro lista` | tutti i lavori disponibili con location e requisiti |
| `/lavoro info <lavoro>` | dettagli: categoria, location, paga, licenza, gradi |
| `/lavoro inizia` | avvia una sessione nella location corrente |
| `/lavoro fine` | termina la sessione e ricevi il pagamento |
| `/lavoro stato` | stato della sessione: durata attiva, prodotto, paga stimata |
| `/lavoro profilo` | storico personale: gradi, sessioni, guadagni per lavoro |
| `/lavoro licenza` | le licenze possedute e il loro stato |
| `/lavoro admin licenza <emetti\|revoca> <player> <job>` | gestione licenze |
| `/lavoro admin sessione termina <player>` | termina forzatamente una sessione |
| `/lavoro admin location <add\|remove> ...` | gestione location di lavoro |
| `/lavoro admin stats <player\|job>` | statistiche di un giocatore o aggregate su un lavoro |
| `/lavoro admin reload` | ricarica la config |

## API pubblica

Recupera `OpenJobsApi` dal Services Manager di Bukkit. Espone il catalogo dei lavori, la sessione
attiva di un lavoratore, i record lifetime, le licenze e il **grado live** — i dati che i widget di
Open Gestionale e un item licenza di Open Identity leggono.

```java
OpenJobsApi api = Bukkit.getServicesManager().load(OpenJobsApi.class);

// Registra il tuo bridge (es. Open Economy) come economy adapter
api.adapters().setEconomy(myEconomyBridge);

// Lettura per i widget del gestionale o per un item licenza
api.getActiveSession(playerUuid).ifPresent(session -> { /* ... */ });
api.getTier(playerUuid, "minatore").ifPresent(tier -> { /* incidi sul tesserino */ });
```

## Configurazione

```
OpenJobs/
├── config.yml          # impostazioni globali, adapter, fallback
├── jobs.yml            # definizione lavori
├── location_types.yml  # tipi di location e rilevamento attività
├── progression.yml     # tier, soglie, decadimento
├── messages_it.yml
└── messages_en.yml
```

## Cosa il modulo non fa mai

- Non paga il tempo: paga l'attività reale rilevata durante la sessione
- Non contiene nomi di lavori, materiali o location nel codice: tutto in config
- Non impedisce mining o crafting fuori dalle location: sono attività Minecraft normali, semplicemente non pagate
- Non gestisce carriere, contratti o promozioni aziendali: quello è Open Companies
- Non ha livelli XP: la progressione è basata su sessioni reali nel tempo
- Non crasha se un adapter manca: degrada in silenzio la feature collegata
- Non tocca la chat o i prefissi: il grado è un dato interno, non un display sociale
- Non assume WorldGuard: usa il RegionAdapter
- Non limita quanti lavori un giocatore può avere, con progressione separata per ognuno

---

*Open Jobs v1.0 — un progetto Open Roleplay. Apache 2.0.* · [English](README.en.md)
