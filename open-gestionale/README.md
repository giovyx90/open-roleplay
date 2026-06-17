# Open Gestionale

Gestionale web **universale, open source e adapter-first** per server Minecraft
roleplay su Paper. Parte della suite [Open Roleplay](../README.md), Apache 2.0.

> **Regola unica:** il gestionale non conosce nessuna ambientazione. Sa solo che
> esistono giocatori, dati e widget che li presentano. Cosa mostrano quei widget
> lo decide la config del server.

Stesso test di neutralità degli altri moduli: *funzionerebbe identico su un
server fantasy medievale?* La risposta è in [`examples/fantasy/`](examples/fantasy/):
stesso identico codice del realistico, cambiano solo `gestionale.yml` e
`layout.yml`. I widget `economy.*` e `identity.*` spariscono da soli perché i
loro moduli non sono attivi — nessuna riga di codice toccata.

## Architettura

```
FRONTEND React (statico, CDN)  ──HTTPS+JWT+CORS──▶  API BRIDGE Node.js (VPS)  ──▶  DB Open Roleplay
  carica il layout dall'API                          legge i dati (read-mostly)      (SQLite / MySQL)
  renderizza i widget                                espone widget REST
```

Due progetti **indipendenti**:

| Cartella | Cos'è | Dove gira |
| --- | --- | --- |
| [`api/`](api/) | Bridge Node.js: auth, config, widget REST, OpenAPI | sul VPS, vicino al DB |
| [`frontend/`](frontend/) | SPA React (Vite): login, dashboard, widget system | su qualsiasi CDN o nginx |
| [`demo/`](demo/) | Seed di dati fittizi + config demo | — |
| [`examples/`](examples/) | Config di riferimento `realistico-it` e `fantasy` | — |

L'API è il **contratto pubblico**: il frontend React è solo uno dei client
possibili (in futuro: app mobile, bot Discord, frontend alternativi).

## Avvio rapido — la demo

Niente DB, niente login: dati fittizi, 5 profili da esplorare.

**Con Docker (un comando):**

```bash
docker compose up --build
# API:      http://localhost:3000
# Frontend: http://localhost:8080
```

**Senza Docker (due terminali):**

```bash
# 1) API in modalità demo
cd api && npm install && npm run start:demo      # :3000

# 2) Frontend
cd frontend && npm install && npm run dev        # :5173
```

Apri il frontend e scegli un personaggio. Il **cittadino** vede solo i propri
dati; l'**agente** vede anche fascicoli gestiti, ricercati e foglio di servizio;
il **politico** vede leggi ed elezioni; l'**imprenditore** la propria azienda;
il **medico** la cartella clinica. È lo stesso meccanismo di permessi del gioco.

## Come funziona il widget system

Al login il frontend chiama `GET /config/layout` e riceve un layout **già
risolto** per quel giocatore: i widget il cui modulo non è attivo, o per cui
manca la capability, sono già stati rimossi e i tab vuoti eliminati. Il frontend
renderizza ciò che riceve e non sa quali moduli esistano.

Ogni widget è un componente autonomo che conosce solo il proprio endpoint
(`GET /widget/<id>`) e gestisce quattro stati: caricamento, errore, **non
disponibile** (modulo assente → `404 WIDGET_NOT_AVAILABLE`) e dati.

La stessa porta è chiusa anche lato server: l'endpoint di un widget controlla
modulo attivo e capability prima di rispondere, quindi un client non può
aggirare il filtro del layout.

## Configurazione

Tutto vive in due file YAML (vedi [`api/config/`](api/config/) e
[`examples/`](examples/)):

- **`gestionale.yml`** — nome server, tema, modalità (`demo`/`real`), database,
  auth (OTP/JWT), CORS, rate limit, e l'elenco dei **moduli attivi**.
- **`layout.yml`** — hero card, tab, sidebar e pagine, con label localizzate.
  Un server fantasy mette "Gilda" al posto di "Lavoro" senza cambiare codice.

## Moduli e widget

I widget disponibili crescono con i moduli Open Roleplay installati: `core`
(sempre), `economy`, `companies`, `openfdo`, `politics`, `jobs`, `health`,
`identity`, `education`. L'elenco completo dei widget è in
[`api/src/widgets/registry.js`](api/src/widgets/registry.js) e nella
[documentazione OpenAPI](api/openapi.yaml).

## Deployment

- **Frontend** → CDN (Vercel/Netlify/Cloudflare Pages) con `VITE_API_URL` che
  punta al bridge. Sono file statici: spesso gratis e serviti dall'edge.
- **API** → sul VPS del server, vicino al DB, dietro un reverse proxy TLS.

Dettagli in [`api/README.md`](api/README.md) e [`frontend/README.md`](frontend/README.md).

## Cosa il gestionale non fa mai

- Non modifica i dati di gameplay (l'unica scrittura "bonifico" è simulata in
  demo e richiede un adapter dedicato in produzione).
- Non ha un sistema di permessi proprio: legge le capability dal DB e le rispetta.
- Non assume nessuna ambientazione: nomi, label e struttura vengono dalla config.
- Non crasha se un modulo manca: mostra il widget "non disponibile".
- Non richiede un account separato: l'identità passa dal server Minecraft.
- Nessun telemetry, nessun analytics esterno.

## Licenza

Apache License 2.0. Vedi [`LICENSE`](../LICENSE).
