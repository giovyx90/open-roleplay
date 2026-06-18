# Open Politics

Il **motore politico** per server roleplay su Paper. Adapter-first e **neutro rispetto
all'ambientazione**: il core non conosce nessun Sindaco, Re o Senato. Conosce solo concetti astratti
— una carica con un titolare, un governo che la assegna, un atto firmato, una legge registrata — e
lascia tutto il resto alla configurazione e ad adapter opzionali.

> Regola unica: **il plugin certifica, non governa.** Open Politics non esegue mai le conseguenze di
> una decisione politica. Certifica che è stata presa da chi aveva l'autorità per prenderla, la
> registra, la espone via API. Le conseguenze sono RP — le vivono i giocatori, non il plugin.

Una legge che vieta le armi in centro **non disabilita** le armi in centro. È un documento firmato
dall'autorità competente che giocatori e FDO usano come riferimento narrativo. Apache 2.0. Parte della
suite [Open Roleplay](https://github.com/giovyx90/open-roleplay).

## Il primitivo universale della politica

> Un sistema di governo è un insieme di **cariche**, un **meccanismo** per assegnarle e un sistema per
> produrre **decisioni vincolanti**.

Il core non sa cosa sia un Sindaco. Sa che esistono cariche, che le cariche hanno titolari, che i
titolari producono atti e che gli atti diventano leggi. Come si chiama tutto questo è nella config.

## Test di neutralità

Ogni feature passa un test: *funzionerebbe identica su un server fantasy medievale?* "Sindaco" → no →
config. "Decreto Legge" → no → config. "Elezione democratica" → no → config (è un tipo di meccanismo).
"Titolare di una carica che produce un atto firmato" → sì → core. "Meccanismo di assegnazione di una
carica" → sì → core. Il realistico italiano (un Comune con elezioni) è solo la configurazione di
riferimento, inclusa come default; vedi [`examples/fantasy`](examples/fantasy) (un Regno) e
[`examples/oligarchia`](examples/oligarchia) (un Consiglio dei Cinque) per ambientazioni opposte sullo
stesso codice.

## Le quattro aree

Non sono sottomoduli separati: sono aspetti dello stesso sistema, attivabili da `config.yml`.

| Area | Domanda | Cosa fa |
| --- | --- | --- |
| **Cariche** | Cosa sei | Ruoli istituzionali con autorità, titolari, mandato e capability. |
| **Governo** | Come ci sei arrivato | Insieme di cariche con un meccanismo di assegnazione. |
| **Atti** | Cosa firmi | Documenti ufficiali prodotti da chi ne ha la capability. |
| **Leggi** | Cosa hai deciso | Atti che hanno completato l'iter: registro pubblico, non eseguito. |

## Capability politiche

Come Open FDO, il core definisce *cosa* sblocca ogni capability; la config decide *quale carica la
detiene*. Una capability non produce mai un effetto automatico: abilita un comando e certifica che chi
agisce aveva l'autorità per farlo.

`SIGN_ACT` · `SIGN_LAW` · `APPOINT` · `REMOVE` · `DISSOLVE` · `DECLARE_EMERGENCY` · `MANAGE_BUDGET` ·
`REVOKE_LICENSE` · `CALL_ELECTION` · `VETO`

## I quattro meccanismi di assegnazione

| Meccanismo | Come si ottiene la carica |
| --- | --- |
| `election` | I giocatori votano. Il plugin gestisce campagna, candidature, raccolta voti, calcolo del risultato e assegnazione automatica al/ai vincitore/i. |
| `appointment` | Nomina unilaterale da parte di chi detiene `APPOINT` su quella carica. |
| `hereditary` | La carica passa al successore designato dal titolare (`/politica successore`). |
| `conquest` | Appartiene a chi controlla fisicamente una region (via `RegionAdapter`, es. WorldGuard). |

Elezione e conquista sono gli **unici** casi in cui il plugin assegna una carica automaticamente.
Tutto il resto è firmato da un titolare.

## L'iter legislativo

Un atto di un tipo con `requires_vote` viene sottoposto a un **organo collegiale** (una carica con
`max_holders > 1`): quorum e maggioranza decidono se passa. Se il tipo prevede `veto_allowed`, si apre
una **finestra di veto** per chi detiene `VETO`. Quando l'iter è completo e il tipo è `can_become_law`,
l'atto è **promulgato** in legge. Il plugin **traccia** l'iter, non lo forza: uno step saltato resta
registrato, non punito.

Le leggi attive sono consultabili da tutti (`/politica leggi`); le abrogate restano nell'**archivio
storico** (`/politica archivio`), così un giudice in RP può applicare la legge che era in vigore al
momento del fatto, anche se oggi è abrogata.

## Comandi

Il permesso `openpolitics.use` abilita i comandi; `openpolitics.admin` sblocca `/politica admin`.
L'autorità vera **non** è un nodo permesso: è la carica che ricopri.

| Comando | Funzione |
| --- | --- |
| `/politica cariche` · `carica <id>` | cariche attive con titolari, e dettaglio di una carica |
| `/politica leggi` · `legge <id>` · `archivio` | registro pubblico, testo di una legge, archivio storico |
| `/politica atti` | atti firmati recenti |
| `/politica governo` | struttura del governo attivo |
| `/politica atto <tipo> <titolo>` | firma un atto (genera un libro timbrato) |
| `/politica nomina <player> <carica>` · `rimuovi` | nomina/rimuovi titolari (capability `APPOINT`/`REMOVE`) |
| `/politica veto <atto_id>` | poni veto su un atto nella finestra temporale |
| `/politica vota <atto_id> <yes\|no\|abstain>` | voto interno di un organo collegiale |
| `/politica emergenza <dichiara\|revoca>` | stato d'emergenza (bridge Open FDO) |
| `/politica elezioni indici <carica>` | indici un'elezione (capability `CALL_ELECTION`) |
| `/politica successore <player>` | designa il successore (meccanismo ereditario) |
| `/politica abroga <legge_id>` | abroga una legge (capability `SIGN_LAW`) |
| `/voto lista` · `candidatura <id>` · `<id> <player>` · `risultati <id>` | partecipazione alle elezioni |
| `/politica admin …` · `/openpolitics <status\|reload>` | gestione staff, stato e ricarica |

## Adapter

Ogni adapter ha un default funzionante ed è scoperto a runtime dal Bukkit ServicesManager. Assente o
non reale, la feature collegata degrada in silenzio — nessun crash, nessuna dipendenza dura.

| Adapter | Default | Assente / non reale → |
| --- | --- | --- |
| `StorageAdapter` | YAML atomico con backup (o `memory`) | — |
| `PermissionAdapter` | Bukkit (riflette LuckPerms) | — |
| `NotificationAdapter` | chat / broadcast | — |
| `EconomyAdapter` | no-op | un bridge Open Economy espone il budget pubblico ai titolari di `MANAGE_BUDGET` |
| `CompanyAdapter` | no-op | un bridge Open Companies riconosce le revoche di licenza dei titolari di `REVOKE_LICENSE` |
| `IdentityAdapter` | no-op | un bridge Open Identity rende le cariche un tesserino fisico che decade a fine mandato |
| `RegionAdapter` | no-op | un bridge WorldGuard abilita il meccanismo `conquest` |
| `AuthorityAdapter` | no-op | un bridge Open FDO riceve le dichiarazioni di emergenza |

## API pubblica

Recupera `OpenPoliticsApi` dal Services Manager di Bukkit. È il registro istituzionale del server: chi
ricopre quale carica, cosa può fare, cosa ha firmato e — soprattutto per gli altri moduli — il registro
pubblico delle leggi.

```java
OpenPoliticsApi api = Bukkit.getServicesManager().load(OpenPoliticsApi.class);

// Open FDO: aggancia le leggi violate a un fascicolo e verifica la legge del tempo del fatto
List<Law> attive = api.getActiveLaws("comune");
boolean inVigore = api.wasActiveDuring(lawId, momentoDelFatto);

// Chi puo' dichiarare l'allerta? (Open FDO lo chiede per sapere chi attiva lo stato d'emergenza)
List<String> cariche = api.chargesWithCapability("comune", PoliticalCapability.DECLARE_EMERGENCY);

// Registra il tuo bridge come adapter
api.adapters().setIdentity(myIdentityBridge);
```

`governments()` / `charges()` / `holdersOf(id)` — struttura e titolari ·
`hasCapability(uuid, cap)` — autorità live di un giocatore ·
`getActiveLaws(gov)` / `getLaw(id)` / `wasActiveDuring(id, moment)` — il bridge per le autorità ·
`recentActs(n)` / `openElections()` — atti ed elezioni · `adapters()` — sostituzione runtime.

## Cosa il modulo non fa mai

- Non contiene nomi di cariche, governi, tipi di atto o categorie di legge: tutto in config.
- Non esegue le conseguenze di una legge — le certifica e le espone.
- Non impone l'iter legislativo — lo traccia e segnala se è stato rispettato.
- Non assegna cariche automaticamente, tranne nei meccanismi `election` e `conquest`.
- Non gestisce violenza politica o colpi di stato: sono RP libero.
- Non espone i voti elettorali quando `anonymous_voting: true`.
- Non crasha se un adapter manca: disattiva in silenzio la capability collegata.

## Build

```bash
mvn -pl open-politics -am package
```

Richiede Java 21 e Paper 1.21.x. Il JAR è in `target/`. OpenCore è opzionale: il plugin è
completamente standalone.
