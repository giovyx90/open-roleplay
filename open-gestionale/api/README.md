# Open Gestionale — API bridge

Bridge Node.js che espone in sola lettura il DB di un server Open Roleplay come
widget REST adapter-first. Gira sul VPS, vicino al database.

## Requisiti

- Node.js **22.5+** (usa `node:sqlite`, integrato — nessuna dipendenza nativa)
- Accesso al DB di Open Roleplay (SQLite; MySQL/MariaDB previsti via adapter)

## Avvio

```bash
npm install

# Demo (dati fittizi dal seed, nessun DB):
npm run start:demo

# Produzione: prepara la config e avvia
cp config/gestionale.example.yml config/gestionale.yml
cp config/layout.example.yml   config/layout.yml
# edita gestionale.yml: database, jwt_secret, cors.allowed_origins
npm start
```

La chiave `jwt_secret` **deve** essere cambiata: con `NODE_ENV=production` e mode
`real` l'avvio fallisce se resta il placeholder.

### Override via ambiente

`GESTIONALE_CONFIG`, `GESTIONALE_LAYOUT`, `GESTIONALE_SEED`, `GESTIONALE_MODE`
(`demo`/`real`), `GESTIONALE_DB_PATH`, `GESTIONALE_JWT_SECRET`, `PORT`, `NODE_ENV`.

## Endpoint

Contratto completo in [`openapi.yaml`](openapi.yaml). In sintesi:

| Metodo | Path | Note |
| --- | --- | --- |
| POST | `/auth/login` | `{uuid, otp}` → JWT (OTP generato in-game) |
| POST | `/auth/demo` | `{uuid}` → JWT (solo modalità demo) |
| POST | `/auth/refresh` · `/auth/logout` | rinnovo / logout |
| GET | `/demo/profiles` | roster demo (solo demo) |
| GET | `/config/server` | nome, tema, modalità, moduli (pubblico) |
| GET | `/config/layout` | layout risolto per il giocatore |
| GET | `/player/me` | identità del giocatore |
| GET | `/widget/{id}` | dati del widget (es. `economy.balance`) |
| POST | `/widget/notifications.feed/{id}/read` | segna letta |
| POST | `/widget/economy.transfers` | bonifico (solo demo) |

Risposte: `{ data, meta }`. Errori: `{ error: { code, message } }`. Un modulo non
attivo risponde `404 WIDGET_NOT_AVAILABLE`; una capability mancante `403
WIDGET_FORBIDDEN`.

## Data source

Tutta la logica di dominio è in [`src/datasource/base.js`](src/datasource/base.js),
espressa su poche primitive. Due implementazioni:

- **demo** ([`demo.js`](src/datasource/demo.js)) — legge il seed in memoria.
- **sqlite** ([`sqlite.js`](src/datasource/sqlite.js)) — legge live il DB. Le
  tabelle di gioco sono mirror JSON con la forma del seed (vedi
  [`db/schema.sql`](db/schema.sql)); le tabelle proprie del gestionale (OTP,
  sessioni, notizie, stato notifiche) seguono la sezione 10 del design.

Per un DB diverso (MySQL/MariaDB) basta una nuova sottoclasse di `BaseDataSource`
che implementi le primitive: i widget non se ne accorgono.

### Importare il seed in SQLite

```bash
node db/import-seed.js ../demo/seed.json ./data/openrp.db
```

## Test

```bash
npm test        # node --test
```

Coprono OTP, risoluzione del layout (gating modulo/capability), **parità
demo↔SQLite** (stessi payload da entrambe le sorgenti) e l'API end-to-end.
